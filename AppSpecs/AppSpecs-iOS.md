# Watch My Calories — App Specification

> Platform-agnostic behavioral specification. Describes what the app does, not how it's implemented.
> Use this spec to build or verify any platform (iOS, Android, etc.).
>
> ⚠️ **Point-in-time spec — partially stale.** Navigation was later restructured (tabs are now **Today / Log Food / Scan Menu / History**; Settings moved to a toolbar **gear** icon), and the Scan-Menu and About flows were added. Where this doc and `CLAUDE.md` / the source disagree, the code is authoritative.

---

## Table of Contents

1. [App Structure](#app-structure)
2. [Today Screen](#1-today-screen)
3. [Scan Screen](#2-scan-screen)
4. [History Screen](#3-history-screen)
5. [Settings Screen](#4-settings-screen)
6. [Onboarding Flow](#5-onboarding-flow)
7. [Cross-Cutting Behaviors](#6-cross-cutting-behaviors)
8. [Graphical Assets](#7-graphical-assets)

---

## App Structure

### Tab Navigation

The app has 4 bottom tabs displayed in this order:

| Order | Tab | Icon | Description |
|-------|-----|------|-------------|
| 1 | Today | Flame | Daily calorie dashboard |
| 2 | Log Food | plus.circle | Add food (camera / photo library / manual entry) |
| 3 | Scan Menu | doc.viewfinder | Scan a restaurant menu |
| 4 | History | Calendar | Past entries by date |

Settings is reached via the toolbar **gear** icon (not a tab).

### Launch Behavior

- **First launch:** The app shows an onboarding flow (see [Onboarding](#5-onboarding-flow))
- **Subsequent launches:** The app opens directly to the Today tab

### Unsaved Changes Guard

When the user navigates away from Settings with unsaved changes, the app shows a confirmation dialog:
- **Save** — persists changes, then navigates
- **Discard** — reverts to previously saved values, then navigates

---

## 1. Today Screen

### 1.1 Overview

Displays the user's daily calorie summary for today, with food entries grouped by meal type.

### 1.2 Hero Summary Card

A prominent card at the top showing daily progress.

**Layout:**
- **Left side:** Circular progress ring
- **Right side:** Three stat rows

**Progress ring (three layered rings, bottom to top):**

| Ring | Color | Represents |
|------|-------|------------|
| Background | Light green | Full circle baseline |
| Burned | Orange | Active calories burned (from health platform) as a fraction of effective target |
| Consumed | Green | Calories consumed as a fraction of effective target |

**Center of ring:** Consumed calorie count with "kcal" label.

**Stat rows:**

| Row | Icon | Shown | Value |
|-----|------|-------|-------|
| Goal | Flag (gray) | Always | User's target calories |
| Burned | Flame (orange) | Only when > 0 | Active calories burned today |
| Remaining | Bar chart (light green) | Always | Effective target minus consumed |

**Calculations:**

```
effectiveTarget = targetCalories + burnedCalories
progress        = min(consumed / effectiveTarget, 1.0)
remaining       = max(0, effectiveTarget - consumed)
```

- `consumed` = sum of all today's food entry calories
- `burnedCalories` = active energy burned today from health platform (HealthKit on iOS, Health Connect on Android)
- `targetCalories` = user's daily calorie goal from profile

### 1.3 Meal Sections

Today's entries are grouped by meal type. Meal type is auto-assigned based on the entry's timestamp hour:

| Meal Type | Hour Range (inclusive start, exclusive end) |
|-----------|---------------------------------------------|
| Breakfast | 7:00 – 9:59 |
| Lunch | 11:00 – 14:59 |
| Dinner | 17:00 – 20:59 |
| Snack | All other hours |

**Display order:** Breakfast → Lunch → Dinner → Snack. Only meal types with entries are shown.

Within each meal section, entries scanned from the same photo (sharing the same image reference) are grouped together as a "meal group."

### 1.4 Food Entry Display

**Single-item entry:**

```
[Thumbnail]  [Food Name]           [Calories]
             [Time • Quantity]
```

- **Thumbnail:** Circular photo thumbnail if an image exists, otherwise the first letter of the food name in a circle
- **Time:** Formatted as time only (e.g., "2:30 PM")
- **Quantity:** Converted to the user's preferred unit system at display time
- **Calories:** Bold, green

**Multi-item meal group (collapsed):**
- Representative title: user-defined meal name, or "Item + N more", or first item name
- Total calories for the group
- Chevron indicator

**Multi-item meal group (expanded):**
- Individual item rows showing name and calories
- Each item has its own context menu

### 1.5 Context Menu Actions

Long-press (or equivalent) on entries/groups reveals:

| Action | Single Item | Multi-Item Group |
|--------|-------------|------------------|
| View | Opens read-only detail view | Opens read-only group detail view |
| Edit | Opens editable detail view | Opens editable group detail view |
| Delete | Removes entry | Removes all entries in the group |

**Image cleanup on delete:** When the last entry referencing an image is deleted, the image file is also removed.

### 1.6 Empty State

When no entries exist for today:
- Camera icon with dashed border
- Title: "No meals tracked yet"
- Tapping the card navigates to the Scan tab
- An "or log manually" link opens manual entry

### 1.7 Manual Entry

Accessible from:
- A "+" button in the navigation bar
- The empty state "or log manually" link

**Manual entry form fields:**
- Food name (required)
- Calories (required)
- Quantity (required)
- Protein, carbs, fat (optional)
- Meal type (auto-selected by current time, user-editable)

The form also offers a "Scan with Camera" shortcut to switch to the Scan tab.

### 1.8 Health Platform Integration

The app reads **active energy burned** (active calories) from the device health platform:
- **iOS:** HealthKit (`activeEnergyBurned`)
- **Android:** Health Connect (equivalent metric)

**Behavior:**
- Authorization is requested when the Today screen first appears
- Data is fetched for the current day (start of day to now)
- Live updates are observed so the dashboard stays current
- Pull-to-refresh also re-fetches active calories

### 1.9 Ad Reminder Modal

If the user has not opted into ads, a modal appears once per day (on first visit to Today):
- Asks the user to enable ad support
- Dismissing suppresses the reminder for 24 hours

### 1.10 Banner Ad

A banner ad is displayed between the hero summary card and the meal sections (if ads are enabled).

---

## 2. Scan Screen

### 2.1 Overview

Captures a food photo using the device camera, sends it to the backend for AI-powered calorie estimation, and saves results.

### 2.2 Camera

**Requirements:**
- Uses the rear-facing camera
- Requires camera permission (prompts on first use)
- Requires a physical device (not available on simulators/emulators; mock behavior is acceptable for testing)

**UI elements:**
- Full-screen camera viewfinder (aspect-fill)
- Gradient overlays at top and bottom for visual framing
- Circular capture button (white ring, green fill) at the bottom
- Haptic feedback on capture

**Not included:** No flash toggle, no gallery/photo picker.

### 2.3 Photo Review

After capturing a photo:
- The captured photo is shown full-screen
- Two buttons:
  - **Retake** (↩ icon): Discards the photo and returns to the live camera
  - **Use Photo** (✓ icon): Proceeds to estimation

### 2.4 Photo Processing

- The captured photo is compressed to JPEG at 80% quality
- No explicit resize — uses native camera resolution
- One photo per capture session

### 2.5 AI Estimation

The photo is sent to a Cloud Run backend that proxies to Google Gemini AI.

**Request:**
- Image is base64-encoded and sent as JPEG
- A text prompt instructs Gemini to analyze the food and return structured JSON
- The prompt adapts to the user's unit system preference:
  - **Metric:** "Prefer metric units (g, kg, ml, L, pieces, slices)"
  - **US Customary:** "Prefer US customary units (oz, fl oz, cups, tbsp, tsp)"

**Expected response format:**
```json
{
  "items": [
    {
      "name": "Food Name",
      "quantity": "200 g",
      "calories": 150,
      "protein": 10,
      "carbs": 20,
      "fat": 5,
      "confidence": 0.95
    }
  ]
}
```

**Backend configuration:**
- Debug/dev builds connect to the dev backend
- Release/production builds connect to the prod backend

**Retry behavior:**
- Up to 3 attempts
- On authentication failure (401): re-authenticate and retry transparently
- Breaks on success or non-retryable error

**Response validation:**
- Negative macro values (protein, carbs, fat) are clamped to 0
- Markdown fences in the response text are stripped before JSON parsing

### 2.6 Device Attestation

The app uses platform-native device attestation to authenticate with the backend:
- **iOS:** App Attest (DeviceCheck framework)
- **Android:** Play Integrity API (or equivalent)

**Flow:**
1. Generate a device key (stored securely on-device)
2. Fetch a challenge from the backend
3. Attest the key with the platform attestation service
4. Register the attestation with the backend
5. For each API request, generate an assertion signed by the device key

**Fallback:** If attestation is unavailable (e.g., simulator, unsupported device), a legacy API key is used instead.

**Recovery:** If the backend rejects an assertion (401), the app clears its attestation state, re-attests, and retries.

### 2.7 Estimation Review Screen

Shows the result of the AI analysis. The screen progresses through these states:

| State | What the User Sees |
|-------|--------------------|
| **Loading** | Spinner + "Analyzing food..." + native ad (if loaded) |
| **Success (intermediate)** | Checkmark + "Analysis complete!" + "View Results" button |
| **Error** | Warning icon + "Analysis Failed" + expandable error details + "Try Again" / "Cancel" buttons |
| **No food detected** | Fork-and-knife icon + "No Food Detected" + "Try Again" / "Cancel" buttons |
| **Success (final)** | Checkmark + "Logged Successfully!" + list of food items with calories + total + "Done" button |

**AI consent check:** Before sending the photo, the app checks if the user has accepted AI consent:
- If accepted → proceed
- If not asked or declined → show a consent prompt (accept → proceed, decline → dismiss back to camera)

**Auto-save:** When estimation succeeds with items:
1. Save the photo to local storage (keyed by a unique ID)
2. Create a food entry for each item (name, calories, quantity, macros, timestamp, image reference, meal name)
3. Persist all entries to the local database

**After "Done":** The app switches to the Today tab and scrolls to the relevant meal section.

### 2.8 Error Scenarios

| Category | Error | User-Facing Message |
|----------|-------|---------------------|
| Camera | Permission denied | "Camera access was denied. Please enable it in Settings." |
| Camera | Configuration failed | "Unable to capture media." |
| Backend | Config missing | "Backend URL or Key not configured. Please check Settings." |
| Backend | Unparseable response | "Failed to parse response from Gemini." |
| Backend | HTTP error | "API Error: {server message or status code}" |
| Network | Connection failure | "Network error: {description}" |
| Attestation | Challenge fetch failed | "Could not verify device." |
| Attestation | Verification failed | "Device verification failed." |

---

## 3. History Screen

### 3.1 Overview

Displays all past food entries grouped by date, in reverse chronological order (newest first). Each date is a collapsible card.

### 3.2 Layout

```
┌──────────────────────────────────────────┐
│ History                                   │
├──────────────────────────────────────────┤
│ [Banner Ad]                               │
├──────────────────────────────────────────┤
│ ┌─ Day Card (collapsed) ──────────────┐  │
│ │ March 14               750 kcal   >  │  │
│ │ Friday                               │  │
│ └──────────────────────────────────────┘  │
│ ┌─ Day Card (collapsed) ──────────────┐  │
│ │ March 13               620 kcal   >  │  │
│ │ Thursday                             │  │
│ └──────────────────────────────────────┘  │
│ ... (scrollable)                          │
└──────────────────────────────────────────┘
```

### 3.3 Day Card

**Collapsed (always visible):**
- Date in "day month" format (e.g., "14 March")
- Day of week (e.g., "Friday")
- Total calories for that day
- Chevron that rotates when expanded
- Tap to toggle expand/collapse (animated)

**Expanded:**
- Entries grouped by meal type in fixed order: Breakfast → Lunch → Dinner → Snack
- Only meal types with entries for that day are shown
- Within each meal type, entries from the same photo are grouped together

### 3.4 Meal Sections (Expanded)

- Meal type header in uppercase, bold, primary color
- Food entry cards identical to the Today screen (same component/behavior)

### 3.5 Entry Interactions

Same as the Today screen:

| Interaction | Result |
|-------------|--------|
| Tap multi-item group | Expand/collapse to show individual items |
| Long-press → View | Read-only detail view (single item or group) |
| Long-press → Edit | Editable detail view (single item or group) |
| Long-press → Delete | Remove entry/group + clean up orphaned images |
| Tap thumbnail | Full-screen image viewer |

### 3.6 Empty State

When no entries exist at all:
- Camera icon with "No meals tracked yet"
- Subtitle: "Tap the camera tab to scan your first meal."
- Tapping navigates to the Scan tab

### 3.7 Not Included

- No calendar or date picker
- No search or text filter
- No charts or trend visualizations
- Browsing is vertical scroll only

---

## 4. Settings Screen

### 4.1 Overview

Allows the user to configure their profile, appearance preferences, calorie goal, and privacy settings.

### 4.2 Sections

#### App Header

- App icon (rounded)
- App name: "Watch My Calories"
- App version number

#### Banner Ad

- Banner ad displayed below the header (if ads enabled)

#### App Appearance

**Theme:**

| Option | Behavior |
|--------|----------|
| System | Follows device light/dark setting |
| Light | Forces light mode |
| Dark | Forces dark mode |

**Unit System:**

| Option | Default |
|--------|---------|
| US Customary | Default when device locale is US |
| Metric | Default for all other locales |

Changing the unit system immediately converts the profile pickers (height, weight) to display in the new system. Internal storage always uses metric.

#### Profile

The profile fields adapt their display based on the selected unit system:

**US Customary:**
- Height: feet (4–8) and inches (0–11) — dual picker
- Weight: pounds (50–400) — single picker

**Metric:**
- Height: centimeters (100–250) — single picker
- Weight: kilograms (20–200) — single picker

**Common fields (both unit systems):**
- Age: 1–100 (picker)
- Gender: Male, Female, Other (dropdown)
- Activity Level: Sedentary, Lightly Active, Moderately Active, Very Active (dropdown)

**Picker behavior:** Only one expandable picker is open at a time — opening one closes the others.

#### Daily Goals

- **Target Calories:** Numeric text field, placeholder "Not Set"
- **Calculate Recommended Goal:** Button that computes a recommendation using the Mifflin-St Jeor formula

**Mifflin-St Jeor Formula:**

```
BMR = (10 × weight_kg) + (6.25 × height_cm) - (5 × age)
  + 5     if male
  - 161   if female or other

Daily Target = round(BMR × activity_multiplier)
```

| Activity Level | Multiplier |
|---------------|------------|
| Sedentary | 1.2 |
| Lightly Active | 1.375 |
| Moderately Active | 1.55 |
| Very Active | 1.725 |

**Important behavior:** Changing profile fields does NOT auto-recalculate the goal. The user must explicitly tap "Calculate Recommended Goal." The user can also manually type any calorie target.

#### Privacy

- **AI Photo Analysis toggle** — enables/disables sending photos to Google Gemini
- Explanation text: "When enabled, food photos are sent to Google Gemini, a third-party AI service by Google, for calorie estimation. All other data stays on-device."
- **Privacy Policy** link (opens external URL)
- **Support** link (opens external URL)

#### Device Attestation Status

Displays the current attestation state with a shield icon:
- **Verified** — green checkmark shield
- **Not Verified** — gray slashed shield
- **Not Available** — gray slashed shield

### 4.3 Persistence

**Preferences** (lightweight key-value storage):

| Setting | Default |
|---------|---------|
| Theme | System |
| Unit System | Locale-aware (US or Metric) |
| AI Consent | Not asked |
| Onboarding Complete | false |

**Profile data** (local database):

| Field | Storage Unit |
|-------|-------------|
| Height | cm (always metric) |
| Weight | kg (always metric) |
| Age | years |
| Gender | string |
| Activity Level | string |
| Target Calories | kcal |

### 4.4 Save Behavior

Settings are saved explicitly (via a Save button or the unsaved-changes guard). The save flow:
1. Persist preferences (theme, unit system) to key-value storage
2. Convert profile values to metric if currently displayed in US Customary
3. Create or update the single user profile record in the local database

### 4.5 Unsaved Changes Detection

The app tracks whether any setting has changed from its persisted value. Detected changes include: theme, unit system, height, weight, age, gender, activity level, and target calories.

---

## 5. Onboarding Flow

### 5.1 Trigger

Shown on first launch (when onboarding has not been completed). Subsequent launches skip directly to the main app.

### 5.2 Steps

#### Step 0: Welcome

- App icon and name
- Tagline: "Track your meals with AI-powered calorie estimation"
- Privacy note: "Your data is never stored outside this device"
- **"Get Started"** button → Step 1

#### Step 1: Your Privacy

- **AI Photo Analysis toggle** — opt into sending photos to Gemini
- **Allow Ad Tracking toggle** — requests platform ad tracking permission and consent
- **Connect Health button** — requests permission to read active calories from health platform
  - Shows a checkmark after the request is made
  - Explanation: "Syncs active calories burned from Apple Health to adjust your daily goal"
- Progress indicator: 1 of 2
- **"Next"** button → Step 2

#### Step 2: Your Goal

- Profile form: height, weight, age, gender, activity level (same controls as Settings)
- Target calories field + "Calculate Recommended Goal" button
- Progress indicator: 2 of 2
- **"Start Tracking"** button → saves profile, completes onboarding, enters main app

#### Skip

A **Skip** button is available on all steps. Skipping completes onboarding without saving any profile data (app uses defaults).

---

## 6. Cross-Cutting Behaviors

### 6.1 Data Models

**User Profile** (one record per app):

| Field | Type | Notes |
|-------|------|-------|
| Height | Double | Stored in cm |
| Weight | Double | Stored in kg |
| Age | Int | |
| Gender | Enum | Male, Female, Other |
| Activity Level | Enum | Sedentary, Lightly Active, Moderately Active, Very Active |
| Target Calories | Double | kcal |
| Created At | Date | |

**Food Entry** (many records):

| Field | Type | Notes |
|-------|------|-------|
| ID | UUID | Unique identifier |
| Name | String | Food item name |
| Calories | Double | kcal |
| Quantity | String | Free-text with unit, e.g. "200 g" |
| Timestamp | Date | When the entry was created |
| Protein | Double? | Grams, optional |
| Carbs | Double? | Grams, optional |
| Fat | Double? | Grams, optional |
| Image ID | UUID? | Reference to stored photo |
| Meal Name | String? | User-visible group label |
| Meal Type | Enum | Breakfast, Lunch, Dinner, Snack (auto-assigned by hour) |

### 6.2 Image Storage

- Photos are stored locally as JPEG files named `{UUID}.jpg`
- Storage location: app-private documents/files directory
- Operations: save, load, delete
- Images are loaded lazily when an entry's thumbnail becomes visible
- Deleting the last entry referencing an image also deletes the image file

### 6.3 Unit Conversion

**Principle:** All data is stored in metric. Conversion to the user's preferred unit system happens at the display layer only.

**Profile values:**
- Height: cm ↔ ft/in
- Weight: kg ↔ lbs

**Food quantities** (display conversion of AI-returned quantity strings):
- Parses strings like "200 g", "1.5 cups", "2 pieces"
- Count-based units (piece, slice, egg, etc.) are never converted
- Weight conversions: g ↔ oz, kg ↔ lbs
- Volume conversions: ml ↔ fl oz, L ↔ cups
- Rounding adapts to magnitude (whole numbers for large values, 1 decimal for small)

### 6.4 Color Palette

Dynamic colors that adapt to light/dark mode:

| Token | Light Mode | Dark Mode |
|-------|-----------|-----------|
| Primary | `#66CC99` (medium green) | `#2D6B4F` (dark green) |
| Secondary | `#D9F2DE` (light green) | `#263328` (dark green-gray) |
| Accent | `#FFA01C` (orange) | `#FFA01C` (orange) |
| Background | `#FAFAFA` (near-white) | Platform system background |
| Surface | `#FFFFFF` (white) | Platform system secondary |
| Text Primary | `#1A1A1A` (near-black) | Platform system label |

### 6.5 Advertising

- **Ad network:** Google AdMob
- **Ad formats:**
  - **Banner ads** — shown on Today, History, and Settings screens
  - **Native ads** — shown on the estimation loading screen (Scan flow)
- **Consent:** Ads are opt-in. The user enables ads during onboarding or via the daily reminder modal
- **Tracking:** Requires platform ad tracking permission (ATT on iOS, equivalent on Android) and Google UMP consent
- **Debug builds** use test ad unit IDs

### 6.6 Backend Configuration

| Build Type | Backend |
|------------|---------|
| Debug / Dev | Dev Cloud Run service |
| Release / Production | Prod Cloud Run service |

The API key is embedded in the app (obfuscated), not user-provided.

### 6.7 View/Edit Modals

**View Food Entry (read-only):**
- Displays all fields in organized cards: name, calories, quantity, macros, meal type, timestamp
- Dismiss via "Done" button

**Edit Food Entry:**
- Editable fields: name, calories, quantity, protein, carbs, fat, meal type
- Meal name (applies to all entries sharing the same image)
- Save/Cancel buttons

**View Meal Group (read-only):**
- Group-level info (meal name, total calories)
- Per-item cards with full details
- Dismiss via "Done" button

**Edit Meal Group:**
- Editable shared meal name and type
- Per-item editing (name, calories, quantity, macros)
- Total calories shown for reference
- Save/Cancel buttons

### 6.8 Full-Screen Image Viewer

Triggered by tapping a food entry's thumbnail (on Today or History screens):
- Black background
- Image displayed at full resolution, scaled to fit
- Close button to dismiss

---

## 7. Graphical Assets

All assets are in the `assets/` directory relative to this spec.

### 7.1 App Icon

**File:** [`assets/AppIcon.png`](assets/AppIcon.png) (1024×1024)

![App Icon](assets/AppIcon.png)

A rounded-square icon with a dark green background. The central motif is a stylized swirl combining green leaves and an orange spiral, evoking natural food and healthy eating. The design uses the app's green and orange brand colors.

**Usage:**
- App Store / Play Store listing icon
- Home screen icon (platform auto-crops/rounds as needed)
- Splash screen (if applicable)

### 7.2 Mini App Icon

**File:** [`assets/MiniAppIcon.png`](assets/MiniAppIcon.png) (640×640)

![Mini App Icon](assets/MiniAppIcon.png)

A circular version of the brand mark with a flat design: dark green circle containing an orange and green swirl pattern with a white center. This is a simplified, high-contrast version of the app icon optimized for small sizes.

**Usage:**
- In-app header displays (Settings screen header, Onboarding welcome, Estimation loading screen)
- Rendered at small sizes (typically 32–100 pt) with rounded corners

### 7.3 System Icons

The app uses platform-native system icons (SF Symbols on iOS, Material Icons on Android). No custom icon files are needed — use the platform equivalent for each:

| Purpose | Icon Name | Where Used |
|---------|-----------|------------|
| Today tab | Flame (filled) | Tab bar |
| Scan tab | Camera (filled) | Tab bar |
| History tab | Calendar | Tab bar |
| Settings tab | Gear (filled) | Tab bar |
| Capture button | Circle (custom drawn) | Camera screen |
| Retake | Counter-clockwise arrow | Photo review |
| Confirm | Checkmark | Photo review |
| Add/Manual entry | Plus | Today nav bar |
| Camera viewfinder | Camera viewfinder | Empty states |
| Delete | Trash | Context menus |
| Edit | Pencil | Context menus |
| View | Eye | Context menus |
| Goal | Flag (filled) | Hero card stat row |
| Burned calories | Flame (filled) | Hero card stat row |
| Remaining | Bar chart (filled) | Hero card stat row |
| Success | Checkmark circle (filled) | Estimation result |
| Error/Warning | Exclamation triangle (filled) | Estimation error |
| No food | Fork and knife | No food detected |
| Close | X mark circle (filled) | Image viewer, modals |
| Expand/Collapse | Chevron right | Day cards, groups |
| Health | Heart (filled) | Onboarding |
| Privacy | Raised hand | Settings privacy link |
| Support | Life preserver | Settings support link |
| Attestation verified | Checkmark shield (filled) | Settings |
| Attestation unverified | Shield slash | Settings |
