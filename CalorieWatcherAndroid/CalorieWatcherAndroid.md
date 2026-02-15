# Calorie-Watcher Android App Development Prompts

This document contains a sequence of prompts designed to create the **Calorie-Watcher** Android app from scratch. This project is a port of the existing iOS application, adapted for modern Android development standards using Kotlin and Jetpack Compose.

## Design Philosophy & Aesthetics (Frontend-Design Skill)

**Crucial**: The UI must be implemented with a **BOLD, PREMIUM, and ORGANIC** aesthetic. Do not build a generic "Material Design" app.

-   **Typography**: Use a distinctive, modern font family (e.g., *Outfit*, *Manrope*, or *Syne* from Google Fonts). Avoid default Roboto. Use large, editorial-style headings.
-   **Color Palette**:
    -   **Primary**: Deep Organic Green (e.g., `#2D4F1E`)
    -   **Secondary**: Pale Mint/Sage (e.g., `#E0F2E9`)
    -   **Accent**: Vibrant Burnt Orange (e.g., `#D97706`) for call-to-actions and progress rings.
    -   **Background**: Soft off-white or a very subtle organic texture/gradient, avoiding stark white.
-   **Shapes & Depth**: Use soft, generous corner radii (24dp+). Use subtle drop shadows and "glassmorphism" (blur effects) where appropriate to create depth.
-   **Motion**: The app must feel alive. Use `AnimatedVisibility`, `animateContentSize`, and shared element transitions for images.

---

## Phase 1: Foundation & Data Layer

**Prompt 1: Project Setup & Room Database**
> Create a new Android project using **Jetpack Compose** and **Material3**.
> Set up **Hilt** for dependency injection.
> Implement the local database using **Room**.
>
> **Data Models (Entities):**
> 1.  **UserProfile**:
>     -   Store as a DataStore preference or a single-row Room table.
>     -   Fields: `height` (Double, cm), `weight` (Double, kg), `age` (Int), `gender` (Enum), `activityLevel` (Enum), `targetCalories` (Double).
>     -   **Logic**: Store strictly in metric. UI will handle Imperial conversion.
>
> 2.  **FoodEntry**:
>     -   Entity with `@PrimaryKey id: String = UUID.randomUUID().toString()`.
>     -   Fields: `name` (String), `calories` (Double), `quantity` (String), `timestamp` (Long/Instant), `imagePath` (String?), `macros` (Embedded object or JSON).
>     -   **MealType Logic**: Create a `MealType` enum (Breakfast, Lunch, Dinner, Snack). Implement logic to auto-assign based on hour:
>         -   Breakfast: 7-9 AM
>         -   Lunch: 11 AM - 2 PM
>         -   Dinner: 5-8 PM
>         -   Snack: All other times.

**Prompt 2: Shared UI Components & Theming**
> Create a `DesignSystem.kt` file.
> -   **Theme**: Implement the "Organic Modern" color palette and custom typography.
> -   **FoodEntryCard**:
>     -   A card with a distinctive layout.
>     -   Left: Thumbnail image (circular crop) with a subtle border.
>     -   Center: Food name (Bold) and time (Small, muted).
>     -   Right: Calories (Large, Accent Color).
>     -   Background: Surface color with low elevation.
> -   **HeroSummaryCard**:
>     -   A prominent dashboard card.
>     -   Centerpiece: A custom **Circular Progress Indicator** showing consumed vs. target calories.
>     -   Animation: The ring should animate (fill up) when the view loads.
> -   **ImageHelper**: Utility to save images to the app's internal storage and return a file path.

---

## Phase 2: Core Feature - AI Food Scanner

**Prompt 3: CameraX Integration**
> Build a custom camera screen using **CameraX**.
> -   **UI**: Full-screen preview.
> -   **Controls**: Large, organic-shaped shutter button.
> -   **Feature**: Allow taking up to 3 photos in a session.
> -   **Preview**: Show a horizontal list of captured thumbnails at the bottom.
> -   **Actions**: "Reset" (icon) and "Done" (Text button).

**Prompt 4: Gemini AI Integration**
> Implement `GeminiRepository` to verify food images using the **Google AI Client SDK for Android**.
> -   **Function**: `suspend fun estimateCalories(images: List<Bitmap>, apiKey: String): FoodAnalysisResult`
> -   **Prompt**: "Analyze these food images. Identify the food, estimate calories, quantity, and macros. Return strict JSON."
> -   **Model**: Use `gemini-1.5-flash` or similar efficient model.
> -   **Error Handling**: Wrap potential errors in a sealed `Result` class.

**Prompt 5: Analysis Flow & Result Screen**
> Create `AnalysisScreen.kt`.
> -   **State**: Loading (Skeleton UI or Lotties animation), Success (List of identified items), Error.
> -   **Success State**:
>     -   Display the analyzed data in editable fields.
>     -   "Save Log" button that commits data to Room and saves images to storage.
> -   **Error State**: Friendly error message with a "Retry" button.

---

## Phase 3: Dashboard & Visualization

**Prompt 6: Dashboard (Home Screen)**
> Build `DashboardScreen.kt`.
> -   **Header**: "Today" with current date, styled elegantly.
> -   **Hero**: Display `HeroSummaryCard` at the top.
> -   **List**: Use `LazyColumn` to show daily entries.
> -   **Grouping**: Use `stickyHeader` (experimental) or grouped items to separate Breakfast, Lunch, Dinner, Snack.
> -   **Interaction**: Tapping an item opens a **Detail Dialog** or screen with the full image.
> -   **FAB**: A floating action button with a "+" icon to open the Camera.

**Prompt 7: History & Statistics**
> Create `HistoryScreen.kt`.
> -   **Calendar/List**: A list of past days.
> -   **Summary Cards**: Each day shows a summary (Total Calories, Progress Bar).
> -   **Expandable**: Tapping a day expands it to show that day's food logs (Accordion style).

---

## Phase 4: Health Connect Integration

**Prompt 8: Health Connect Setup**
> integrate **Health Connect** to read "Active Calories Burned".
> -   **Permissions**: Handle `HealthConnectClient.permissionController`. Request `READ` access for `ActiveCaloriesBurnedRecord`.
> -   **Repository**: Create `HealthRepository`.
>     -   Method: `getTodayActiveCalories()`: Query aggregate data for the current day.
> -   **Dashboard Update**:
>     -   Update `HeroSummaryCard` to include a "Burned" ring or stat.
>     -   Formula: Remaining = (Target + Burned) - Consumed.

---

## Phase 5: Settings & Polish

**Prompt 9: Settings Screen**
> Build `SettingsScreen.kt`.
> -   **API Key Management**: Secure storage for Gemini API Key.
> -   **Profile Editing**:
>     -   Height/Weight input.
>     -   Unit Toggle (Metric/Imperial).
>     -   "Calculate Goal" button (Mifflin-St Jeor equation).
> -   **Visuals**: Use grouped settings items with icons.

**Prompt 10: Navigation & Main Activity**
> Set up **Jetpack Compose Navigation**.
> -   **Routes**: `Home`, `Camera`, `Analysis`, `History`, `Settings`.
> -   **Bottom Navigation**: Create a custom bottom bar with organic icons and subtle selection animations.
> -   **Transitions**: Add slide-in/slide-out transitions between screens.
