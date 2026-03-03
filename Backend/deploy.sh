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

PROJECT_ID=$(gcloud config get-value project)
echo "Deploying to Project: $PROJECT_ID"
REGION="us-central1"

echo "Enabling necessary Google Cloud APIs..."
gcloud services enable run.googleapis.com secretmanager.googleapis.com cloudbuild.googleapis.com --project $PROJECT_ID

# Store GEMINI_API_KEY in Secret Manager
echo "Setting up Secret Manager for GEMINI_API_KEY..."
if gcloud secrets describe caloriewatcher-gemini-api-key --project $PROJECT_ID >/dev/null 2>&1; then
  echo "Updating existing secret version..."
  echo -n "$GEMINI_API_KEY" | gcloud secrets versions add caloriewatcher-gemini-api-key --data-file=- --project $PROJECT_ID
else
  echo "Creating new secret..."
  echo -n "$GEMINI_API_KEY" | gcloud secrets create caloriewatcher-gemini-api-key --data-file=- --replication-policy="automatic" --project $PROJECT_ID
fi

# Store APP_BACKEND_API_KEY in Secret Manager
echo "Setting up Secret Manager for APP_BACKEND_API_KEY..."
if gcloud secrets describe caloriewatcher-app-backend-api-key --project $PROJECT_ID >/dev/null 2>&1; then
  echo "Updating existing secret version..."
  echo -n "$APP_BACKEND_API_KEY" | gcloud secrets versions add caloriewatcher-app-backend-api-key --data-file=- --project $PROJECT_ID
else
  echo "Creating new secret..."
  echo -n "$APP_BACKEND_API_KEY" | gcloud secrets create caloriewatcher-app-backend-api-key --data-file=- --replication-policy="automatic" --project $PROJECT_ID
fi

# Grant the Cloud Run service account access to Secret Manager
SERVICE_ACCOUNT=$(gcloud run services describe caloriewatcher-backend --region $REGION --project $PROJECT_ID --format="value(spec.template.spec.serviceAccountName)" 2>/dev/null || echo "${PROJECT_NUMBER}-compute@developer.gserviceaccount.com")
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

echo "Deploying to Cloud Run..."
gcloud run deploy caloriewatcher-backend \
  --source . \
  --region $REGION \
  --project $PROJECT_ID \
  --allow-unauthenticated \
  --set-env-vars="GEMINI_MODEL_NAME=${GEMINI_MODEL_NAME}" \
  --set-secrets="GEMINI_API_KEY=caloriewatcher-gemini-api-key:latest,APP_BACKEND_API_KEY=caloriewatcher-app-backend-api-key:latest"

echo "Deployment complete."
