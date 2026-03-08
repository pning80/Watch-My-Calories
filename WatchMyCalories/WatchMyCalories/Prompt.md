# Calorie-Watcher App Development Prompts

This document contains a sequence of prompts designed to recreate the **Calorie-Watcher** iPhone app from scratch, incorporating all iterative refinements and design decisions made during development.

## Phase 1: Foundation & Data Layer

**Prompt 1: Project Setup & Data Models**
> Create a new iOS app using SwiftUI and SwiftData. Define the following data models:
> 
> **UserProfile**:
> - Properties: `height` (Double, cm), `weight` (Double, kg), `age` (Int), `gender` (Enum), `activityLevel` (Enum), `targetCalories` (Double).
> - **Unit Strategy**: Store strictly in metric internally (kg/cm) but design the UI to accept Imperial units (lbs/in).
> 
> **FoodEntry**:
> - Properties: `id` (UUID), `name` (String), `calories` (Double), `quantity` (String), `timestamp` (Date), `imageID` (UUID?), macros (optional).
> - **MealType Logic**: Add a `MealType` enum (Breakfast, Lunch, Dinner, Snack). Implement a static `from(date:)` method to determine the meal type based on specific hour windows:
>   - Breakfast: 7:00 AM – 9:00 AM
>   - Lunch: 11:00 AM – 2:00 PM
>   - Dinner: 5:00 PM – 8:00 PM
>   - Snack: All other times.
> - Automatically set `mealType` in the `init` based on the timestamp.

**Prompt 2: Shared Components & Design System**
> Create a `Components.swift` file for shared UI elements:
> - **Design System**: Define a `Color` extension with a "Organic Modern" palette (Deep Green `cwPrimary`, Pale Mint `cwSecondary`, Orange `cwAccent`).
> - **FoodEntryCard**: A stylish row displaying food name, time, quantity, and calories. It should show a thumbnail image (circle inside a rounded square) if an `imageID` exists.
> - **HeroSummaryCard**: A dashboard card showing a circular progress ring for daily calorie consumption vs. goal.
> - **ImageStorage**: A helper to save/load images to the Documents directory using UUIDs.

---

## Phase 2: Core Feature - AI Food Scanner

**Prompt 3: Custom Camera Interface**
> Build a custom `CameraView` using `AVFoundation`.
> - **UI**: Full-screen camera with a large shutter button.
> - **Multi-shot**: Allow taking up to 3 photos.
> - **Feedback**: Display a horizontal scroll view of thumbnails for captured images at the bottom.
> - **Controls**: "Reset" button to clear images, "Done" button to proceed to analysis.

**Prompt 4: Gemini AI Integration**
> Implement `GeminiService` to analyze food images via Google's Gemini API.
> - **Function**: `estimateCalories(images: [Data], apiKey: String)`.
> - **Output**: Expect a strict JSON response with food name, estimated calories, quantity, and macros.
> - **Model Fetching**: Add a method `fetchAvailableModels(apiKey: String)` that queries the Gemini API to retrieve a list of supported models.
> - **Error Handling**: Log detailed errors to the console but throw clean error types for the UI.

**Prompt 5: Analysis Review & Graceful Error Handling**
> Create `EstimationReviewView` to handle the analysis flow.
> - **Process**: Show a loading indicator while calling the API.
> - **Success**: Automatically save the `FoodEntry` items to SwiftData and save the image to disk. Show a "Logged Successfully" success state with a "Done" button.
> - **Failure**: If the API fails, show a friendly "Analysis Failed" screen. Include a "Show Details" toggle to reveal the technical error message for debugging.

---

## Phase 3: Dashboard & Visualization

**Prompt 6: Dashboard (Today's View)**
> Build the `DashboardView` as the main landing screen.
> - **Header**: Display the date and "WatchMyCalories".
> - **Summary**: Show the `HeroSummaryCard`. If no profile exists, default to a 2000 kcal goal (do not block the user with a setup screen).
> - **Meal Grouping**: Group today's entries by `MealType` (Breakfast, Lunch, Dinner, Snack). Display them in sections.
> - **Scrolling**: When a new scan is added via the camera, automatically scroll the view to the current meal time section.
> - **Thumbnails**: Use the `FoodEntryCard` to show thumbnails of scanned food. Tapping a thumbnail should open the original image in full screen.

**Prompt 7: History View & Compact Layout**
> Create the `HistoryView` to browse past logs.
> - **Layout**: Display a list of collapsible daily cards (`HistoryDayCard`).
> - **Compact Rows**: Inside the day card, list food items using a compact version of the `FoodEntryCard` (smaller fonts/thumbnails) to fit more items.
> - **Sorting**: Ensure items are sorted chronologically (Breakfast to Dinner).
> - **Visual Polish**: Ensure there is adequate spacing between items and cards for a clean look.

---

## Phase 4: Settings & Polish

**Prompt 8: Settings Screen & Refined Inputs**
> Implement a comprehensive `SettingsView`.
> - **API Key**: Secure text field for the Gemini API key.
> - **Model Selection**: Add a Picker to select the Gemini model. Populate this picker dynamically by calling `fetchAvailableModels` when the view appears or the API key changes. Default to a "flash" model if available.
> - **Profile Inputs**:
>   - **Height**: Use a side-by-side picker for Feet (4-8) and Inches (0-11).
>   - **Weight**: Use an expandable inline wheel picker (Range: 50-400 lbs) triggered by tapping the row.
>   - **Age**: Use an expandable inline wheel picker triggered by tapping the row.
> - **Unit Conversion**: The UI must display/edit in Imperial units (lbs/in) but convert to/from Metric (kg/cm) for the `UserProfile` storage.
> - **Calorie Calculator**: Add a "Calculate Recommended Goal" button using the Mifflin-St Jeor equation.
> - **Responsiveness**: Ensure the keyboard dismisses correctly when tapping outside fields or pressing done.

**Prompt 9: Navigation & Final Assembly**
> Assemble the app in `ContentView` using a `TabView`.
> - **Tabs**: Dashboard ("Today"), Camera ("Scan"), History, Settings.
> - **Navigation State**: Pass a binding `selectedTab` to `CameraRootView` and `SettingsView` to allow programmatic navigation.
> - **Camera Reset**: Ensure the camera state clears every time the tab is opened.

