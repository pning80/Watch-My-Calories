# Deployment Preparation

Before deploying the Cloud Run backend to your GCP account, complete these manual steps. `deploy.sh <dev|prod>` reads `Backend/.env.dev` or `Backend/.env.prod` (not `.env`) and will **fail fast** if a required value is missing.

### Step 1: Create your environment file

1. In the `Backend` folder, copy `.env.example` to `.env.dev` (and/or `.env.prod`).
2. Set the **required** values:
   * `GEMINI_API_KEY` — your Google AI Studio Gemini key, no surrounding quotes (e.g. `GEMINI_API_KEY=AIzaSy...`).
   * `APPLE_TEAM_ID` — your 10-character Apple Developer Team ID (required for App Attest verification; `deploy.sh` aborts without it).
   * `PLAY_INTEGRITY_PROJECT_NUMBER` — your GCP **project number** (the Play Integrity audience; `deploy.sh` aborts without it).
3. Confirm `GEMINI_MODEL_NAME` suits your needs (default `gemini-3.1-flash-lite`).
4. `APP_BACKEND_API_KEY` is **optional and dev-only** — it's the legacy `x-backend-key` fallback for simulators/emulators that can't attest. It is **disabled in production** (`BACKEND_ENV=prod` rejects it); prod is protected by App Attest (iOS) and Play Integrity (Android), not this key.

`ATTESTED_KEYS_COLLECTION` and `ATTEST_HMAC_SECRET` are handled automatically by `deploy.sh` — you do not set them.

### Step 2: Firestore database must exist

Attestation persists keys to Firestore. Enabling the API does **not** create a database — ensure a Firestore **Native-mode** `(default)` database exists in the project (e.g. `gcloud firestore databases create --location=<region> --type=firestore-native`). Without it, attestation writes fail with `5 NOT_FOUND` and devices get 401s.

Once your `.env.<env>` file is saved and the Firestore database exists, run `./deploy.sh dev` (or `prod`).

*Note: `.env.dev` / `.env.prod` are gitignored, so your keys are never committed.*
