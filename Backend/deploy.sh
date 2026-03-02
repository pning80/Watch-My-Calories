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
if gcloud secrets describe gemini-api-key --project $PROJECT_ID >/dev/null 2>&1; then
  echo "Updating existing secret version..."
  echo -n "$GEMINI_API_KEY" | gcloud secrets versions add gemini-api-key --data-file=- --project $PROJECT_ID
else
  echo "Creating new secret..."
  echo -n "$GEMINI_API_KEY" | gcloud secrets create gemini-api-key --data-file=- --replication-policy="automatic" --project $PROJECT_ID
fi

# Store APP_BACKEND_API_KEY in Secret Manager
echo "Setting up Secret Manager for APP_BACKEND_API_KEY..."
if gcloud secrets describe app-backend-api-key --project $PROJECT_ID >/dev/null 2>&1; then
  echo "Updating existing secret version..."
  echo -n "$APP_BACKEND_API_KEY" | gcloud secrets versions add app-backend-api-key --data-file=- --project $PROJECT_ID
else
  echo "Creating new secret..."
  echo -n "$APP_BACKEND_API_KEY" | gcloud secrets create app-backend-api-key --data-file=- --replication-policy="automatic" --project $PROJECT_ID
fi

echo "Deploying to Cloud Run..."
gcloud run deploy caloriewatcher-backend \
  --source . \
  --region $REGION \
  --project $PROJECT_ID \
  --allow-unauthenticated \
  --set-env-vars="GEMINI_MODEL_NAME=${GEMINI_MODEL_NAME}" \
  --set-secrets="GEMINI_API_KEY=gemini-api-key:latest,APP_BACKEND_API_KEY=app-backend-api-key:latest"

echo "Deployment complete."
