# Initial Prompt to create the GCP backend

- This folder contains both the iOS version and the Andriod version of the Carolie Watcher app. The iOS version is in the folder `WatchMyCalories` and the Android version is in the folder `WatchMyCaloriesAndroid`.
- Next we will add a backend for the iOS version of the app stored in `WatchMyCalories`. Please ignore the Android version of the app stored in `WatchMyCaloriesAndroid`. We will refactor the Android version later. Please don't make any changes to the Android version until you receive an explicit instruction to do so.
- The goal of this backend is to protect the Gemini API key so that it is not exposed in the app installed on the end users' devices. 
- We will use Cloud Run on Google Cloud Platform (GCP) to host the backend. 
- Let's store the backend code in the folder `Backend`, which should be at the same level as `WatchMyCalories` and `WatchMyCaloriesAndroid`. 
- We need to make sure the communication between the app and the backend is secure. Please adopt the industry best practices when you design how the app and the backend communicate with each other.
- In order to protect end users' data and privacy, we should store all the data on the device and never store any data on the backend. 
- When the app scans a meal item, it will send the prompt and the images of the meal to the backend, and the backend will then forward the data to the Gemini service and relay the results back to the app. The Gemini API key will enable the backend to access the Gemini service. 
- The backend will only store the Gemini API key and the Gemini model name used to access the Gemini service. Please use the recommended method for Cloud Run to store the Gemini API key and the Gemini model name. 
- Let's plan to have a `.env` file in the `Backend` folder for me to pass the Gemini API key and the Gemini model name to the backend. During deployment to GCP, please retrieve the Gemini API key and the Gemini model name from there.
- You should prepare a `.env.example` file in the `Backend` folder for me to fill in the Gemini API key and the Gemini model name. Please list all the valid Gemini model names in this file so that I only copy the one I want to use.
- You should be able to deploy the backend to GCP using my account directly. Please use the `gcloud` command line tool to deploy the backend to GCP using my account. 
- Please have a subfolder called `docs` in the `Backend` folder. If I need to take any manual actions to prepare you to deploy the backend to GCP, please record the steps in this subfolder. 
