#!/bin/bash
set -e

# Load environment variables
if [ -f .env ]; then
  set -a
  source .env
  set +a
else
  echo "Error: .env file not found."
  exit 1
fi

if [ -z "$GEMINI_API_KEY" ]; then
  echo "Error: GEMINI_API_KEY is not set in .env"
  exit 1
fi

if [ -z "$APP_BACKEND_API_KEY" ]; then
  echo "Error: APP_BACKEND_API_KEY is not set in .env"
  exit 1
fi

if [ -z "$APPLE_TEAM_ID" ]; then
  echo "Error: APPLE_TEAM_ID is not set in .env"
  exit 1
fi

if [ -z "$ATTEST_HMAC_SECRET" ]; then
  echo "Error: ATTEST_HMAC_SECRET is not set in .env"
  exit 1
fi

PROJECT_ID=$(gcloud config get-value project)
echo "Deploying to Project: $PROJECT_ID"
REGION="us-central1"

echo "Enabling necessary Google Cloud APIs..."
gcloud services enable run.googleapis.com secretmanager.googleapis.com cloudbuild.googleapis.com firestore.googleapis.com --project $PROJECT_ID

# Store GEMINI_API_KEY in Secret Manager
echo "Setting up Secret Manager for GEMINI_API_KEY..."
if gcloud secrets describe watchmycalories-gemini-api-key --project $PROJECT_ID >/dev/null 2>&1; then
  echo "Updating existing secret version..."
  echo -n "$GEMINI_API_KEY" | gcloud secrets versions add watchmycalories-gemini-api-key --data-file=- --project $PROJECT_ID
else
  echo "Creating new secret..."
  echo -n "$GEMINI_API_KEY" | gcloud secrets create watchmycalories-gemini-api-key --data-file=- --replication-policy="automatic" --project $PROJECT_ID
fi

# Store APP_BACKEND_API_KEY in Secret Manager
echo "Setting up Secret Manager for APP_BACKEND_API_KEY..."
if gcloud secrets describe watchmycalories-app-backend-api-key --project $PROJECT_ID >/dev/null 2>&1; then
  echo "Updating existing secret version..."
  echo -n "$APP_BACKEND_API_KEY" | gcloud secrets versions add watchmycalories-app-backend-api-key --data-file=- --project $PROJECT_ID
else
  echo "Creating new secret..."
  echo -n "$APP_BACKEND_API_KEY" | gcloud secrets create watchmycalories-app-backend-api-key --data-file=- --replication-policy="automatic" --project $PROJECT_ID
fi

# Store ATTEST_HMAC_SECRET in Secret Manager
echo "Setting up Secret Manager for ATTEST_HMAC_SECRET..."
if gcloud secrets describe watchmycalories-attest-hmac-secret --project $PROJECT_ID >/dev/null 2>&1; then
  echo "Updating existing secret version..."
  echo -n "$ATTEST_HMAC_SECRET" | gcloud secrets versions add watchmycalories-attest-hmac-secret --data-file=- --project $PROJECT_ID
else
  echo "Creating new secret..."
  echo -n "$ATTEST_HMAC_SECRET" | gcloud secrets create watchmycalories-attest-hmac-secret --data-file=- --replication-policy="automatic" --project $PROJECT_ID
fi

# Grant the Cloud Run service account access to Secret Manager
SERVICE_ACCOUNT=$(gcloud run services describe watchmycalories-backend --region $REGION --project $PROJECT_ID --format="value(spec.template.spec.serviceAccountName)" 2>/dev/null || echo "${PROJECT_NUMBER}-compute@developer.gserviceaccount.com")
if [ -z "$SERVICE_ACCOUNT" ]; then
  PROJECT_NUMBER=$(gcloud projects describe $PROJECT_ID --format="value(projectNumber)")
  SERVICE_ACCOUNT="${PROJECT_NUMBER}-compute@developer.gserviceaccount.com"
fi
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

echo "Deploying to Cloud Run..."
gcloud run deploy watchmycalories-backend \
  --source . \
  --region $REGION \
  --project $PROJECT_ID \
  --allow-unauthenticated \
  --set-env-vars="GEMINI_MODEL_NAME=${GEMINI_MODEL_NAME},APPLE_TEAM_ID=${APPLE_TEAM_ID}" \
  --set-secrets="GEMINI_API_KEY=watchmycalories-gemini-api-key:latest,APP_BACKEND_API_KEY=watchmycalories-app-backend-api-key:latest,ATTEST_HMAC_SECRET=watchmycalories-attest-hmac-secret:latest"

echo "Deployment complete."
