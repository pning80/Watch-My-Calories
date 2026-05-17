#!/bin/bash
set -e

ENV="${1:-}"
if [ "$ENV" != "prod" ] && [ "$ENV" != "dev" ]; then
  echo "Usage: ./deploy.sh <prod|dev>"
  exit 1
fi

if [ "$ENV" = "prod" ]; then
  ENV_FILE=".env.prod"
  SERVICE_NAME="watchmycalories-backend"
  SECRET_PREFIX="watchmycalories"
  ATTESTED_KEYS_COLLECTION="attestedKeys-prod"
else
  ENV_FILE=".env.dev"
  SERVICE_NAME="watchmycalories-backend-dev"
  SECRET_PREFIX="watchmycalories-dev"
  ATTESTED_KEYS_COLLECTION="attestedKeys-dev"
fi

# Load environment variables
if [ -f "$ENV_FILE" ]; then
  set -a
  source "$ENV_FILE"
  set +a
else
  echo "Error: $ENV_FILE not found."
  exit 1
fi

if [ -z "$APPLE_TEAM_ID" ]; then
  echo "Error: APPLE_TEAM_ID is not set in $ENV_FILE"
  exit 1
fi

# Play Integrity (Android) — both default to project-derived values so a deploy works
# even before .env.* are updated. Override in .env.* if the values ever change.
: "${PLAY_INTEGRITY_PROJECT_NUMBER:=657698311127}"
: "${PLAY_INTEGRITY_PACKAGE_NAME:=com.pning80.watchmycalories}"

# Prod safety prompt
if [ "$ENV" = "prod" ]; then
  echo "WARNING: Deploying to PRODUCTION ($SERVICE_NAME)."
  read -p "Continue? (y/N) " confirm
  [ "$confirm" = "y" ] || [ "$confirm" = "Y" ] || { echo "Cancelled."; exit 0; }
fi

PROJECT_ID=$(gcloud config get-value project)
echo "Deploying $ENV to Project: $PROJECT_ID (service: $SERVICE_NAME)"
REGION="us-central1"

echo "Enabling necessary Google Cloud APIs..."
gcloud services enable run.googleapis.com secretmanager.googleapis.com cloudbuild.googleapis.com firestore.googleapis.com --project $PROJECT_ID

# Upsert a secret: push to Secret Manager if provided locally, otherwise verify it exists.
# Usage: upsert_secret <VAR_VALUE> <SECRET_NAME> <VAR_NAME>
upsert_secret() {
  local value="$1" secret_name="$2" var_name="$3"
  if [ -n "$value" ]; then
    echo "Setting up Secret Manager for $var_name..."
    if gcloud secrets describe "$secret_name" --project $PROJECT_ID >/dev/null 2>&1; then
      echo "Updating existing secret version..."
      echo -n "$value" | gcloud secrets versions add "$secret_name" --data-file=- --project $PROJECT_ID
    else
      echo "Creating new secret..."
      echo -n "$value" | gcloud secrets create "$secret_name" --data-file=- --replication-policy="automatic" --project $PROJECT_ID
    fi
  else
    echo "$var_name not in $ENV_FILE — checking Secret Manager..."
    if ! gcloud secrets describe "$secret_name" --project $PROJECT_ID >/dev/null 2>&1; then
      echo "Error: $secret_name not found in Secret Manager. Set $var_name in $ENV_FILE for initial setup."
      exit 1
    fi
    echo "$secret_name already exists in Secret Manager."
  fi
}

upsert_secret "$GEMINI_API_KEY"      "${SECRET_PREFIX}-gemini-api-key"       "GEMINI_API_KEY"
if [ "$ENV" = "dev" ]; then
  upsert_secret "$APP_BACKEND_API_KEY" "${SECRET_PREFIX}-app-backend-api-key"  "APP_BACKEND_API_KEY"
fi

# Ensure the HMAC secret resource exists with at least one version
HMAC_SECRET_NAME="${SECRET_PREFIX}-attest-hmac-secret"
if ! gcloud secrets describe "$HMAC_SECRET_NAME" --project $PROJECT_ID >/dev/null 2>&1; then
  echo "Creating HMAC secret resource ($HMAC_SECRET_NAME)..."
  gcloud secrets create "$HMAC_SECRET_NAME" --replication-policy="automatic" --project $PROJECT_ID
fi
# Auto-generate a value if no version exists yet
if ! gcloud secrets versions access latest --secret="$HMAC_SECRET_NAME" --project $PROJECT_ID >/dev/null 2>&1; then
  echo "Generating HMAC secret value..."
  node -e "process.stdout.write(require('crypto').randomBytes(32).toString('hex'))" \
    | gcloud secrets versions add "$HMAC_SECRET_NAME" --data-file=- --project $PROJECT_ID
fi

SERVICE_ACCOUNT="watchmycalories-backend@${PROJECT_ID}.iam.gserviceaccount.com"

# Grant the Cloud Run service account access to Secret Manager
echo "Granting Secret Manager access to $SERVICE_ACCOUNT..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/secretmanager.secretAccessor" \
  --condition=None \
  --quiet

# Grant Cloud Run service account access to Firestore
echo "Granting Firestore access to $SERVICE_ACCOUNT..."
gcloud projects add-iam-policy-binding $PROJECT_ID \
  --member="serviceAccount:${SERVICE_ACCOUNT}" \
  --role="roles/datastore.user" \
  --condition=None \
  --quiet

echo "Deploying to Cloud Run ($SERVICE_NAME)..."

# Build env vars and secrets conditionally — legacy key auth is disabled in prod
ENV_VARS="BACKEND_ENV=${ENV},GEMINI_MODEL_NAME=${GEMINI_MODEL_NAME},APPLE_TEAM_ID=${APPLE_TEAM_ID},ATTESTED_KEYS_COLLECTION=${ATTESTED_KEYS_COLLECTION},ATTEST_HMAC_SECRET_NAME=${HMAC_SECRET_NAME},PLAY_INTEGRITY_PROJECT_NUMBER=${PLAY_INTEGRITY_PROJECT_NUMBER},PLAY_INTEGRITY_PACKAGE_NAME=${PLAY_INTEGRITY_PACKAGE_NAME},RATE_LIMIT_GLOBAL_WINDOW_MS=${RATE_LIMIT_GLOBAL_WINDOW_MS:-900000},RATE_LIMIT_GLOBAL_MAX=${RATE_LIMIT_GLOBAL_MAX:-100},RATE_LIMIT_GEMINI_WINDOW_MS=${RATE_LIMIT_GEMINI_WINDOW_MS:-900000},RATE_LIMIT_GEMINI_MAX=${RATE_LIMIT_GEMINI_MAX:-100},RATE_LIMIT_ATTEST_WINDOW_MS=${RATE_LIMIT_ATTEST_WINDOW_MS:-900000},RATE_LIMIT_ATTEST_MAX=${RATE_LIMIT_ATTEST_MAX:-30}"
SECRETS="GEMINI_API_KEY=${SECRET_PREFIX}-gemini-api-key:latest"

if [ "$ENV" = "dev" ]; then
  ENV_VARS="${ENV_VARS},RATE_LIMIT_LEGACY_KEY_WINDOW_MS=${RATE_LIMIT_LEGACY_KEY_WINDOW_MS:-900000},RATE_LIMIT_LEGACY_KEY_MAX=${RATE_LIMIT_LEGACY_KEY_MAX:-15}"
  SECRETS="${SECRETS},APP_BACKEND_API_KEY=${SECRET_PREFIX}-app-backend-api-key:latest"
fi

gcloud run deploy $SERVICE_NAME \
  --source . \
  --region $REGION \
  --project $PROJECT_ID \
  --allow-unauthenticated \
  --service-account="$SERVICE_ACCOUNT" \
  --set-env-vars="$ENV_VARS" \
  --set-secrets="$SECRETS"

echo "Deployment complete ($ENV → $SERVICE_NAME)."
