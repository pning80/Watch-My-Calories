# Ideation Notes

## Goal of the project

The goal of this project is to develop an iPhone app that can help the user estimate the calories of the food they eat and track their daily consumption.

## High level plan

### Use case \#1: Estimate calories of food

Assume the user has a dish with food and wants to estimate the calories of the food. The user can point the camera to the food and take multiple pictures from different angles. In the background, the app will use Gemini to estimate the calories of the food. 

Some user inputs can help improve the accuracy:
- Optionally tell the weight (some user may have a scale to measure the weight)
- Optionally describe what’s in the food

Send the pictures to Gemini, get the possible food in the picture and ask the user to confirm. Hopefully this can increase the accuracy.

### User case \#2: Track daily consumption and guide additional food consumption

- Use the user's weight, height, age, gender, activity level, and dietary goals to calculate the user's daily calorie needs
- Track daily consumption and guide additional food consumption 
- If the user agrees, get data from Apple Health or Apple Watch to track exercise and calories burned. This will help to adjust the daily calorie needs. Show the user the progress towards their daily calorie goal, using both the food consumption and the exercise data.

## Development Notes

- Keep all data on the device. Do not send any user data to the cloud. 
- Use the agent skill called frontend-design to help design the UI. 
- Be creative when using the camera. You can ask the user to point to the food from different angles, and you can take pictures multiple times without asking the user to click the shutter button.
- Use the content of `ui_mockup.html` as the initial UI design for the app. Refine it to make it more user-friendly and visually appealing. 
- **Working with XCode**
  - Provide clear instructions on how to import the code into XCode and run the app. Assume the user has a Mac with XCode installed and can run the app on a simulator or a physical device. Assume the user has no prior experience with XCode.
  - Since the iPhone app needs to be built with XCode, please package the code in a way that can be easily imported into XCode. Use Swift Package Manager to manage dependencies.




