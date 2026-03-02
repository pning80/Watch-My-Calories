# Deployment Preparation

Before I (the AI) can securely deploy the Cloud Run backend to your GCP account, please complete the following manual step to provide the Gemini API Key.

### Step 1: Configure your `.env` file

1. Navigate to the `Backend` folder.
2. Duplicate the file named `.env.example` and rename the copy to `.env`.
3. Open `.env` and paste your actual, valid Google AI Studio Gemini API Key beside `GEMINI_API_KEY=`.
    * Ensure there are no surrounding quotes (e.g., `GEMINI_API_KEY=AIzaSyA...`)
4. Confirm that the `GEMINI_MODEL_NAME` suits your needs (the default is `gemini-2.0-flash-exp`).
5. (Optional) You may change the `APP_BACKEND_API_KEY` to any custom password of your choosing. This key secures your Cloud Run endpoint from public abuse.

Once you have saved your `.env` file, let me know, and I will execute the deployment!

*Note: The `.env` file structure is excluded from git via your `.gitignore` file, so your API key will never be committed to your repository.*
