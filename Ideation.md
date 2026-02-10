# Ideation Notes

## Goal of the project

The goal of this project is to develop an iPhone app that can help the user estimate the calories of the food they eat and track their daily consumption.

## Development Platform

Since XCode is the best development tool for iPhone app, we will use **XCode Intelligence Vibe Coding** to develop the app.

## High level plan

We plan to support the following use cases:

### Use case \#1: Estimate calories of food

- Assume the user has a dish with food and wants to estimate the calories of the food. The user can point the camera to the food and take multiple pictures from different angles. In the background, the app will use Gemini to estimate the calories of the food. 
- Be creative when using the camera. You can ask the user to point to the food from different angles, and you can take pictures multiple times without asking the user to click the shutter button.
- Send the pictures to Gemini, get the possible food in the picture and ask the user to confirm. Hopefully this can increase the accuracy.

### User case \#2: Track daily consumption and guide additional food consumption

- Use the user's weight, height, age, gender, activity level, and dietary goals to calculate the user's daily calorie needs
- Track daily consumption and guide additional food consumption. Make sure you check today's date and reset the daily calorie needs if necessary. 
- If the user agrees, get data from Apple Health or Apple Watch to track exercise and calories burned. This will help to adjust the daily calorie needs. Show the user the progress towards their daily calorie goal, using both the food consumption and the exercise data.
- Keep a history of all the data. Allow the user to view the summary of calories consumed and burned by date. 
- Provide a way to manually add or edit the food consumption data.
- Provide a way for the user to view the trend of calories consumed and burned by date. 

### UI Design
- When designing the UI, make sure it is user-friendly and visually appealing. 
- Look for the agent skill called frontend-design. If it's available, use it to help design the UI. Otherwise, try your best to design the UI.
- Create mock up of all screens and flows, and ask the user to review and approve before proceeding to development.

## Development Notes

- Keep all data on the device. Do not send any user data to the cloud. 
- **Working with Gemini**
  - The app will need to call Gemini API to estimate the calories of the food. Since this app is a free app, we will need the user to provide a Gemini API key. Provide an easy way for the user to get a Gemini API Key from their own Gemini account and move it to the app. When the app is first opened, it will ask the user to provide a Gemini API key, and direct the user to Google AI Studio to get the API key. Please be creative if there are other ways for the user to get the API key. Afer the user saves the API key, the app should have a way to leave the screen and go back to the main screen.
  - **Important**: Make sure you first check what models are available, and show the available models in the app. The user should be able to pick any model. When the user doesn't pick, you should choose a flash model or lite model to save the cost. 




