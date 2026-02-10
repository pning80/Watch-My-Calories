# Calorie-Watcher App Development Prompts

This document contains a sequence of prompts designed to recreate the **Calorie-Watcher** iPhone app, based on the `Ideation.md` requirements and the iterative refinement process used during development.

## Phase 1: Foundation & Data Layer

**Prompt 1: Project Setup & Data Models**
> Create a new iOS app using SwiftUI and SwiftData. Based on the requirements in `Ideation.md`, define the data models needed for the app.
> 
> **Requirements:**
> - **UserProfile**: Stores height, weight, age, gender, activity level, and calculated target calories.
> - **FoodEntry**: Stores individual meal logs with name, calories, quantity, timestamp, macros (protein, carbs, fat), and an image reference.
> - **Persistence**: Use SwiftData (`@Model`) for local-only storage.
> - **Container**: Configure the `ModelContainer` in the main App file.

**Prompt 2: Settings & Onboarding**
> Build a `SettingsView` to manage the user's profile and API configuration.
> 
> **Requirements:**
> - **Profile Form**: Input fields for height (cm), weight (kg), age, gender, and activity level.
> - **Calorie Calculation**: Implement the Mifflin-St Jeor equation to automatically calculate `targetCalories` based on the profile data.
> - **API Key Management**: Add a secure text field for the Google Gemini API Key. Store this key securely using the Keychain (create a helper if needed), NOT UserDefaults.
> - **Model Selection**: Add a picker to select the Gemini model (default to `gemini-1.5-flash`), saving the selection to UserDefaults.

---

## Phase 2: Core Feature - AI Food Scanner

**Prompt 3: Custom Camera Interface**
> Create a custom camera view (`CameraView`) using `AVFoundation`.
> 
> **Requirements:**
> - **Custom UI**: Do not use `UIImagePickerController`. Build a full-screen camera interface.
> - **Multi-shot**: Allow the user to take up to 3 photos of the food from different angles.
> - **Visual Feedback**: Show a counter (e.g., "1/3") and thumbnails of captured images.
> - **Controls**: Include a large shutter button, a reset button, and a "Done" button to proceed.

**Prompt 4: Gemini AI Integration**
> Implement the `GeminiService` to analyze food images.
> 
> **Requirements:**
> - **API Call**: Create a function `estimateCalories(images: [Data], apiKey: String)` that sends the images to the Google Gemini API.
> - **Prompt Engineering**: Use a system prompt that instructs Gemini to identify food items and return a strict JSON structure containing: `name`, `calories`, `quantity`, `protein`, `carbs`, `fat`.
> - **Model Fetching**: Add a method to fetch available models from the API to populate the settings picker dynamically.

**Prompt 5: Analysis Review & Auto-Save**
> Build the `EstimationReviewView` to display the AI analysis results.
> 
> **Requirements:**
> - **Loading State**: Show a progress indicator while analyzing.
> - **Result Display**: Show the detected food items and total calories in a clean list.
> - **Auto-Save**: Automatically save the results to SwiftData upon successful analysis without requiring an extra "Save" click.
> - **Feedback**: specific visual confirmation (e.g., "Logged Successfully") before dismissing the view.

---

## Phase 3: Dashboard & Visualization

**Prompt 6: Dashboard (Today's View)**
> Design the `DashboardView` as the main landing screen.
> 
> **Requirements:**
> - **Design Aesthetic**: Use a "Organic Modern" style with deep greens and card-based layouts.
> - **Summary Card**: Create a `HeroSummaryCard` showing a circular progress ring for daily calorie consumption vs. goal.
> - **Recent Meals**: Display a list of today's `FoodEntry` items using custom cards.
> - **Empty State**: Show a "No meals tracked yet" card with a call-to-action to open the camera if the list is empty.

**Prompt 7: History View**
> Create the `HistoryView` to browse past logs.
> 
> **Requirements:**
> - **Grouping**: Group `FoodEntry` items by date (descending).
> - **Expandable Cards**: Use an accordion-style card for each day (`HistoryDayCard`) that shows the total calories for that day in the header.
> - **Deletion**: Allow users to delete individual food items from the history (e.g., via swipe-to-delete or context menu).

---

## Phase 4: Polish & Refinement

**Prompt 8: Navigation & Polish**
> Integrate all views into a main `ContentView` using a `TabView`.
> 
> **Requirements:**
> - **Tabs**: Dashboard ("Today"), Camera ("Scan"), History, Settings.
> - **Camera Reset**: Ensure the camera state (captured images) resets automatically every time the user switches to the Camera tab.
> - **Readability**: Ensure all text, especially secondary labels like dates and metadata, has sufficient contrast (dark gray instead of light gray).
> - **Transitions**: Add smooth animations for state changes (e.g., expanding history cards).

