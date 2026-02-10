# Calorie Watcher App

This folder contains the source code for the Calorie Watcher iOS App.

## 🚀 Getting Started

Since this is a SwiftUI application, you need to create an Xcode project to run it.

### 1. Create a New Xcode Project
1. Open Xcode.
2. Select **Create a new Xcode project**.
3. Choose **iOS** -> **App**.
4. Click **Next**.
5. **Product Name**: `CalorieWatcher`
6. **Interface**: SwiftUI
7. **Language**: Swift
8. **Storage**: Core Data (Check the box!)
   - *Note: If you forget to check Core Data, you can copy the `Persistence.swift` file provided here.*
9. Click **Next** and save it to a location of your choice.

### 2. Import Source Files
1. In Finder, navigate to this `CalorieWatcherApp/Sources` directory.
2. Drag and drop the `Views`, `Models`, `Services`, `ViewModels`, and `Utils` folders into your Xcode project navigator (the left sidebar).
   - Make sure **"Copy items if needed"** is checked.
   - Make sure **"Create groups"** is selected.
   - Make sure your App Target is checked in "Add to targets".

### 3. Configure Info.plist
You need to add permissions for the Camera and HealthKit.
1. Select your project in the navigator.
2. Select the **Target** (CalorieWatcher).
3. Go to the **Info** tab.
4. Add the following keys:
   - **Privacy - Camera Usage Description**: "We need camera access to scan your food."
   - **Privacy - Health Share Usage Description**: "We need to read your active energy burned to adjust your calorie goals."
   - **Privacy - Health Update Usage Description**: "We need to save your dietary energy consumed to HealthKit."

### 5. Add Your Gemini API Key
1. Get an API Key from [Google AI Studio](https://aistudio.google.com/).
2. When you run the app, go to Settings and enter your key.
