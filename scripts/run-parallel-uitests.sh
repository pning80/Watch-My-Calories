#!/bin/bash
#
# run-parallel-uitests.sh — Parallel iOS Simulator XCUI Test Runner
#
# Detects machine resources, spins up optimal simulator count, distributes
# test classes via greedy bin-packing, runs UI tests in parallel, and reports results.
#
# Usage:
#   ./scripts/run-parallel-uitests.sh [options]
#
# Options:
#   --simulators N          Override auto-detected simulator count
#   --base-simulator NAME   Override auto-detected base simulator (name or UDID)
#   --skip-unit-tests       Skip unit test phase
#   --keep-simulators       Don't delete simulators after run (for debugging)
#   --dry-run               Print plan without executing
#
set -euo pipefail

# ── Constants ────────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
XCODE_PROJECT="$PROJECT_DIR/WatchMyCalories/WatchMyCalories.xcodeproj"
SCHEME="WatchMyCalories"
UNIT_TEST_TARGET="WatchMyCaloriesTests"
UI_TEST_TARGET="WatchMyCaloriesUITests"
DERIVED_DATA="$PROJECT_DIR/WatchMyCalories/DerivedData-ParallelTests"
RESULTS_DIR="$PROJECT_DIR/WatchMyCalories/test-results"
SIM_NAME_PREFIX="WMC-ParallelTest-Sim"
MAX_SIMULATORS=6
RAM_PER_SIM_GB=3
CORES_PER_SIM=2

# UI test classes ordered by test count (largest first for bin-packing)
declare -a UI_TEST_CLASSES=(
    "SettingsTests:19"
    "DashboardTests:15"
    "ManualEntryTests:14"
    "EndToEndFlowTests:11"
    "HistoryTests:10"
    "TabNavigationTests:6"
    "OnboardingTests:6"
)
TOTAL_UI_TESTS=81

# Unit test classes
declare -a UNIT_TEST_CLASSES=(
    "CalorieCalculatorTests"
    "EstimationModelTests"
    "FoodEntryTests"
    "MealTypeTests"
    "SettingsEnumTests"
    "UnitConversionTests"
)
TOTAL_UNIT_TESTS=44

# ── CLI flag defaults ────────────────────────────────────────────────────────

OVERRIDE_SIM_COUNT=""
OVERRIDE_BASE_SIM=""
SKIP_UNIT_TESTS=false
KEEP_SIMULATORS=false
DRY_RUN=false

# ── Color output ─────────────────────────────────────────────────────────────

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

info()    { echo -e "${BLUE}ℹ${NC}  $*"; }
success() { echo -e "${GREEN}✓${NC}  $*"; }
warn()    { echo -e "${YELLOW}⚠${NC}  $*"; }
error()   { echo -e "${RED}✗${NC}  $*"; }
header()  { echo -e "\n${BOLD}${CYAN}── $* ──${NC}\n"; }

# ── Parse CLI arguments ─────────────────────────────────────────────────────

while [[ $# -gt 0 ]]; do
    case "$1" in
        --simulators)
            OVERRIDE_SIM_COUNT="$2"; shift 2 ;;
        --base-simulator)
            OVERRIDE_BASE_SIM="$2"; shift 2 ;;
        --skip-unit-tests)
            SKIP_UNIT_TESTS=true; shift ;;
        --keep-simulators)
            KEEP_SIMULATORS=true; shift ;;
        --dry-run)
            DRY_RUN=true; shift ;;
        -h|--help)
            head -20 "$0" | tail -16; exit 0 ;;
        *)
            error "Unknown option: $1"; exit 1 ;;
    esac
done

# ── Cleanup trap ─────────────────────────────────────────────────────────────

CREATED_SIM_UDIDS=()

cleanup() {
    if [[ "$KEEP_SIMULATORS" == true ]]; then
        warn "Keeping simulators (--keep-simulators). Clean up manually:"
        for udid in "${CREATED_SIM_UDIDS[@]}"; do
            echo "  xcrun simctl delete $udid"
        done
        return
    fi
    if [[ ${#CREATED_SIM_UDIDS[@]} -gt 0 ]]; then
        info "Cleaning up ${#CREATED_SIM_UDIDS[@]} simulator(s)..."
        for udid in "${CREATED_SIM_UDIDS[@]}"; do
            xcrun simctl shutdown "$udid" 2>/dev/null || true
            xcrun simctl delete "$udid" 2>/dev/null || true
        done
        success "Simulators cleaned up."
    fi
}

trap cleanup EXIT

# ── Clean up stale simulators from previous interrupted runs ─────────────────

cleanup_stale_sims() {
    local stale_udids
    stale_udids=$(xcrun simctl list devices -j | python3 -c "
import json, sys
data = json.load(sys.stdin)
for runtime, devices in data.get('devices', {}).items():
    for d in devices:
        if d['name'].startswith('$SIM_NAME_PREFIX'):
            print(d['udid'])
" 2>/dev/null || true)

    if [[ -n "$stale_udids" ]]; then
        local count
        count=$(echo "$stale_udids" | wc -l | tr -d ' ')
        warn "Found $count stale simulator(s) from previous runs. Cleaning up..."
        while IFS= read -r udid; do
            xcrun simctl shutdown "$udid" 2>/dev/null || true
            xcrun simctl delete "$udid" 2>/dev/null || true
        done <<< "$stale_udids"
        success "Stale simulators removed."
    fi
}

# ── Resource detection ───────────────────────────────────────────────────────

detect_resources() {
    local cpu_cores ram_bytes ram_gb max_by_cores max_by_ram num_classes optimal

    cpu_cores=$(sysctl -n hw.ncpu)
    ram_bytes=$(sysctl -n hw.memsize)
    ram_gb=$((ram_bytes / 1073741824))

    max_by_cores=$((cpu_cores / CORES_PER_SIM))
    max_by_ram=$((ram_gb / RAM_PER_SIM_GB))
    num_classes=${#UI_TEST_CLASSES[@]}

    # Take the minimum of all caps
    optimal=$max_by_cores
    [[ $max_by_ram -lt $optimal ]] && optimal=$max_by_ram
    [[ $MAX_SIMULATORS -lt $optimal ]] && optimal=$MAX_SIMULATORS
    [[ $num_classes -lt $optimal ]] && optimal=$num_classes
    [[ $optimal -lt 1 ]] && optimal=1

    info "Machine: ${cpu_cores} CPU cores, ${ram_gb} GB RAM"
    info "Limits: ${max_by_cores} (by cores), ${max_by_ram} (by RAM), ${MAX_SIMULATORS} (cap), ${num_classes} (classes)"

    if [[ -n "$OVERRIDE_SIM_COUNT" ]]; then
        optimal=$OVERRIDE_SIM_COUNT
        info "Using override: $optimal simulator(s)"
    else
        info "Auto-detected: $optimal simulator(s)"
    fi

    SIM_COUNT=$optimal
}

# ── Find base simulator ─────────────────────────────────────────────────────

find_base_simulator() {
    if [[ -n "$OVERRIDE_BASE_SIM" ]]; then
        # Check if it's a UDID (contains dashes and is 36 chars)
        if [[ "$OVERRIDE_BASE_SIM" =~ ^[0-9A-Fa-f-]{36}$ ]]; then
            BASE_SIM_UDID="$OVERRIDE_BASE_SIM"
            BASE_SIM_NAME="$OVERRIDE_BASE_SIM"
        else
            # Look up by name
            BASE_SIM_UDID=$(xcrun simctl list devices -j | python3 -c "
import json, sys
data = json.load(sys.stdin)
for runtime, devices in data.get('devices', {}).items():
    for d in devices:
        if d['name'] == '$OVERRIDE_BASE_SIM' and d['isAvailable']:
            print(d['udid'])
            sys.exit(0)
print('')
" 2>/dev/null)
            if [[ -z "$BASE_SIM_UDID" ]]; then
                error "Base simulator '$OVERRIDE_BASE_SIM' not found or not available."
                exit 1
            fi
            BASE_SIM_NAME="$OVERRIDE_BASE_SIM"
        fi
        info "Using override base simulator: $BASE_SIM_NAME ($BASE_SIM_UDID)"
        return
    fi

    # Auto-detect: find the latest available iPhone simulator
    read -r BASE_SIM_UDID BASE_SIM_NAME < <(xcrun simctl list devices available -j | python3 -c "
import json, sys, re

data = json.load(sys.stdin)
best = None
best_ver = (0, 0)

for runtime, devices in data.get('devices', {}).items():
    # Extract iOS version from runtime string
    m = re.search(r'iOS[- ](\d+)[.-](\d+)', runtime)
    if not m:
        continue
    ver = (int(m.group(1)), int(m.group(2)))

    for d in devices:
        name = d['name']
        if not d.get('isAvailable', False):
            continue
        # Prefer iPhone simulators, skip Watch/iPad/TV
        if not name.startswith('iPhone'):
            continue
        # Pick the highest iOS version, then prefer Pro Max > Pro > Plus > base
        priority = 0
        if 'Pro Max' in name:
            priority = 3
        elif 'Pro' in name:
            priority = 2
        elif 'Plus' in name:
            priority = 1
        if best is None or ver > best_ver or (ver == best_ver and priority > best[2]):
            best = (d['udid'], name, priority)
            best_ver = ver

if best:
    print(best[0], best[1])
else:
    print('')
" 2>/dev/null)

    if [[ -z "$BASE_SIM_UDID" ]]; then
        error "No available iPhone simulator found. Install one via Xcode > Settings > Platforms."
        exit 1
    fi
    info "Base simulator: $BASE_SIM_NAME ($BASE_SIM_UDID)"
}

# ── Distribute tests via greedy bin-packing ──────────────────────────────────

distribute_tests() {
    # Arrays to hold per-simulator assignments
    # SIM_CLASSES[i] = "Class1,Class2,..."
    # SIM_COUNTS[i] = total test count
    SIM_CLASSES=()
    SIM_COUNTS=()

    for ((i = 0; i < SIM_COUNT; i++)); do
        SIM_CLASSES[$i]=""
        SIM_COUNTS[$i]=0
    done

    # Greedy: assign each class (already sorted largest-first) to the simulator
    # with the fewest tests so far
    for entry in "${UI_TEST_CLASSES[@]}"; do
        local class_name="${entry%%:*}"
        local class_count="${entry##*:}"

        # Find simulator with minimum load
        local min_idx=0
        local min_count=${SIM_COUNTS[0]}
        for ((i = 1; i < SIM_COUNT; i++)); do
            if [[ ${SIM_COUNTS[$i]} -lt $min_count ]]; then
                min_idx=$i
                min_count=${SIM_COUNTS[$i]}
            fi
        done

        # Assign
        if [[ -z "${SIM_CLASSES[$min_idx]}" ]]; then
            SIM_CLASSES[$min_idx]="$class_name"
        else
            SIM_CLASSES[$min_idx]="${SIM_CLASSES[$min_idx]},$class_name"
        fi
        SIM_COUNTS[$min_idx]=$((SIM_COUNTS[$min_idx] + class_count))
    done

    header "Test Distribution"
    printf "  %-12s %-50s %s\n" "Simulator" "Classes" "Tests"
    printf "  %-12s %-50s %s\n" "---------" "-------" "-----"
    for ((i = 0; i < SIM_COUNT; i++)); do
        printf "  %-12s %-50s %s\n" "Sim $((i+1))" "${SIM_CLASSES[$i]}" "${SIM_COUNTS[$i]}"
    done
    echo ""
}

# ── Build once ───────────────────────────────────────────────────────────────

build_for_testing() {
    header "Building for Testing"
    info "Building scheme '$SCHEME' for testing (build-for-testing)..."
    info "DerivedData: $DERIVED_DATA"

    # Determine destination from base simulator
    local destination="platform=iOS Simulator,id=$BASE_SIM_UDID"

    xcodebuild build-for-testing \
        -project "$XCODE_PROJECT" \
        -scheme "$SCHEME" \
        -destination "$destination" \
        -derivedDataPath "$DERIVED_DATA" \
        -quiet \
        2>&1 | while IFS= read -r line; do
            # Show errors and warnings, suppress verbose output
            if [[ "$line" == *"error:"* ]] || [[ "$line" == *"warning:"* ]]; then
                echo "  $line"
            fi
        done

    if [[ ${PIPESTATUS[0]} -ne 0 ]]; then
        error "Build failed. See output above."
        exit 1
    fi
    success "Build succeeded."
}

# ── Clone & boot simulators ─────────────────────────────────────────────────

create_simulators() {
    header "Creating Simulators"

    SIM_UDIDS=()

    for ((i = 0; i < SIM_COUNT; i++)); do
        local sim_name="${SIM_NAME_PREFIX}$((i+1))"
        info "Cloning: $sim_name from $BASE_SIM_NAME..."
        local udid
        udid=$(xcrun simctl clone "$BASE_SIM_UDID" "$sim_name")
        SIM_UDIDS[$i]="$udid"
        CREATED_SIM_UDIDS+=("$udid")
        success "Created $sim_name ($udid)"
    done

    # Boot all simulators in parallel
    info "Booting ${SIM_COUNT} simulator(s)..."
    for ((i = 0; i < SIM_COUNT; i++)); do
        xcrun simctl boot "${SIM_UDIDS[$i]}" 2>/dev/null &
    done
    wait

    # Wait for all to finish booting
    for ((i = 0; i < SIM_COUNT; i++)); do
        xcrun simctl bootstatus "${SIM_UDIDS[$i]}" -b 2>/dev/null || true
    done

    success "All simulators booted."
}

# ── Run unit tests ───────────────────────────────────────────────────────────

run_unit_tests() {
    header "Running Unit Tests"
    info "Running $TOTAL_UNIT_TESTS unit tests on ${SIM_NAME_PREFIX}1..."

    mkdir -p "$RESULTS_DIR"
    local result_bundle="$RESULTS_DIR/unit-tests.xcresult"
    local log_file="$RESULTS_DIR/unit-tests.log"

    # Remove old result bundle if present
    rm -rf "$result_bundle"

    local destination="platform=iOS Simulator,id=${SIM_UDIDS[0]}"

    # Build -only-testing flags for all unit test classes
    local only_testing_flags=""
    for class_name in "${UNIT_TEST_CLASSES[@]}"; do
        only_testing_flags="$only_testing_flags -only-testing:${UNIT_TEST_TARGET}/${class_name}"
    done

    set +e
    xcodebuild test-without-building \
        -project "$XCODE_PROJECT" \
        -scheme "$SCHEME" \
        -destination "$destination" \
        -derivedDataPath "$DERIVED_DATA" \
        -resultBundlePath "$result_bundle" \
        $only_testing_flags \
        2>&1 | tee "$log_file" | while IFS= read -r line; do
            if [[ "$line" == *"Test Case"*"passed"* ]]; then
                echo -e "  ${GREEN}✓${NC} $line"
            elif [[ "$line" == *"Test Case"*"failed"* ]]; then
                echo -e "  ${RED}✗${NC} $line"
            elif [[ "$line" == *"error:"* ]]; then
                echo -e "  ${RED}$line${NC}"
            fi
        done
    local exit_code=${PIPESTATUS[0]}
    set -e

    if [[ $exit_code -eq 0 ]]; then
        success "Unit tests passed."
    else
        error "Unit tests failed (exit code $exit_code). See: $log_file"
    fi

    return $exit_code
}

# ── Run UI tests in parallel ────────────────────────────────────────────────

run_ui_tests_parallel() {
    header "Running UI Tests in Parallel"

    mkdir -p "$RESULTS_DIR"

    declare -a PIDS=()
    declare -a SIM_EXIT_CODES=()

    for ((i = 0; i < SIM_COUNT; i++)); do
        local sim_num=$((i+1))
        local sim_udid="${SIM_UDIDS[$i]}"
        local classes="${SIM_CLASSES[$i]}"
        local result_bundle="$RESULTS_DIR/ui-tests-sim${sim_num}.xcresult"
        local log_file="$RESULTS_DIR/ui-tests-sim${sim_num}.log"

        # Remove old result bundle
        rm -rf "$result_bundle"

        local destination="platform=iOS Simulator,id=$sim_udid"

        # Build -only-testing flags
        local only_testing_flags=""
        IFS=',' read -ra CLASS_ARRAY <<< "$classes"
        for class_name in "${CLASS_ARRAY[@]}"; do
            only_testing_flags="$only_testing_flags -only-testing:${UI_TEST_TARGET}/${class_name}"
        done

        info "Sim $sim_num: ${classes} (${SIM_COUNTS[$i]} tests)"

        # Run in background
        (
            xcodebuild test-without-building \
                -project "$XCODE_PROJECT" \
                -scheme "$SCHEME" \
                -destination "$destination" \
                -derivedDataPath "$DERIVED_DATA" \
                -resultBundlePath "$result_bundle" \
                $only_testing_flags \
                > "$log_file" 2>&1
        ) &
        PIDS[$i]=$!
    done

    # Wait for all and collect exit codes
    info "Waiting for ${SIM_COUNT} parallel test runners..."
    for ((i = 0; i < SIM_COUNT; i++)); do
        set +e
        wait "${PIDS[$i]}"
        SIM_EXIT_CODES[$i]=$?
        set -e
    done

    success "All UI test runners finished."
}

# ── Report results ───────────────────────────────────────────────────────────

report_results() {
    header "Test Results Summary"

    local overall_pass=true

    if [[ "$SKIP_UNIT_TESTS" != true ]]; then
        local unit_log="$RESULTS_DIR/unit-tests.log"
        if [[ -f "$unit_log" ]]; then
            local unit_passed unit_failed
            unit_passed=$(grep -c "Test Case.*passed" "$unit_log" 2>/dev/null || echo "0")
            unit_failed=$(grep -c "Test Case.*failed" "$unit_log" 2>/dev/null || echo "0")
            if [[ $unit_failed -gt 0 ]]; then
                echo -e "  ${RED}✗${NC} Unit Tests: ${unit_passed} passed, ${unit_failed} failed"
                overall_pass=false
            else
                echo -e "  ${GREEN}✓${NC} Unit Tests: ${unit_passed} passed"
            fi
        fi
    fi

    for ((i = 0; i < SIM_COUNT; i++)); do
        local sim_num=$((i+1))
        local log_file="$RESULTS_DIR/ui-tests-sim${sim_num}.log"
        local result_bundle="$RESULTS_DIR/ui-tests-sim${sim_num}.xcresult"
        local exit_code="${SIM_EXIT_CODES[$i]}"
        local classes="${SIM_CLASSES[$i]}"

        local passed failed
        passed=$(grep -c "Test Case.*passed" "$log_file" 2>/dev/null || echo "0")
        failed=$(grep -c "Test Case.*failed" "$log_file" 2>/dev/null || echo "0")

        if [[ $exit_code -eq 0 ]]; then
            echo -e "  ${GREEN}✓${NC} Sim $sim_num [$classes]: ${passed} passed"
        else
            echo -e "  ${RED}✗${NC} Sim $sim_num [$classes]: ${passed} passed, ${failed} failed"
            overall_pass=false
        fi
        echo "    Result bundle: $result_bundle"
        echo "    Log: $log_file"
    done

    echo ""

    if [[ "$overall_pass" == true ]]; then
        success "${BOLD}All tests passed!${NC}"
        return 0
    else
        error "${BOLD}Some tests failed.${NC} Check logs above for details."
        return 1
    fi
}

# ── Dry-run output ───────────────────────────────────────────────────────────

dry_run_output() {
    header "Dry Run — Plan"
    echo "  Project:       $XCODE_PROJECT"
    echo "  Scheme:        $SCHEME"
    echo "  DerivedData:   $DERIVED_DATA"
    echo "  Results dir:   $RESULTS_DIR"
    echo "  Simulators:    $SIM_COUNT"
    echo "  Base sim:      $BASE_SIM_NAME ($BASE_SIM_UDID)"
    echo "  Skip units:    $SKIP_UNIT_TESTS"
    echo "  Keep sims:     $KEEP_SIMULATORS"
    echo ""

    distribute_tests

    if [[ "$SKIP_UNIT_TESTS" != true ]]; then
        info "Phase 1: Unit tests ($TOTAL_UNIT_TESTS tests) on Sim 1"
        for class_name in "${UNIT_TEST_CLASSES[@]}"; do
            echo "    - $UNIT_TEST_TARGET/$class_name"
        done
        echo ""
    fi

    info "Phase 2: UI tests ($TOTAL_UI_TESTS tests) across $SIM_COUNT simulators"
    for ((i = 0; i < SIM_COUNT; i++)); do
        local classes="${SIM_CLASSES[$i]}"
        IFS=',' read -ra CLASS_ARRAY <<< "$classes"
        echo "  Sim $((i+1)):"
        for class_name in "${CLASS_ARRAY[@]}"; do
            echo "    - $UI_TEST_TARGET/$class_name"
        done
    done

    echo ""
    success "Dry run complete. No simulators created, no tests executed."
}

# ── Main ─────────────────────────────────────────────────────────────────────

main() {
    header "Parallel iOS Simulator XCUI Test Runner"

    cleanup_stale_sims
    detect_resources
    find_base_simulator

    if [[ "$DRY_RUN" == true ]]; then
        dry_run_output
        exit 0
    fi

    distribute_tests
    build_for_testing
    create_simulators

    local overall_exit=0

    if [[ "$SKIP_UNIT_TESTS" != true ]]; then
        set +e
        run_unit_tests
        local unit_exit=$?
        set -e
        if [[ $unit_exit -ne 0 ]]; then
            overall_exit=1
            warn "Unit tests failed but continuing with UI tests..."
        fi
    fi

    run_ui_tests_parallel

    set +e
    report_results
    local report_exit=$?
    set -e

    [[ $report_exit -ne 0 ]] && overall_exit=1

    exit $overall_exit
}

main
