# Wordlight

Wordlight is a Wordscapes-style word puzzle game for Android, built with **Kotlin** and **Jetpack Compose**.
Players swipe across a circular letter wheel to form words and fill a crossword grid. The game has 20 handcrafted levels, bonus words, sound and haptic feedback, and full state retention across rotation, backgrounding and process death.

**APK:** [Download the latest release](https://github.com/Roshankumr/wordlight/releases/latest)

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Architecture](#architecture)
4. [Custom Swipe Logic](#custom-swipe-logic)
5. [Level Data Structure](#level-data-structure)
6. [Navigation and State Management](#navigation-and-state-management)
7. [UI, Motion and Feedback](#ui-motion-and-feedback)
8. [Testing](#testing)
9. [Project Structure](#project-structure)
10. [Build and Run](#build-and-run)

---

## Features

- Crossword grid at the top and a circular letter wheel (3–5 letters) at the bottom
- Continuous swipe input with a connecting line that follows the finger
- Distinct animated feedback for grid words, bonus words, repeated words and invalid words
- 20 levels in two packs: **Dawn** (levels 1–10, 3–4 letters) and **Dusk** (levels 11–20, 5 letters)
- Automatic progression: when the grid is full, a completion screen counts down and moves to the next level
- Level selection with completed, current and locked states; progress is saved after every word
- Pause menu with Resume, Restart, Levels, Home, and Sound and Haptics toggles
- Shuffle button that animates the letters to their new positions around the wheel
- Portrait and landscape layouts, and a reduced-motion mode that respects the system setting

---

## Tech Stack

### Development environment

| Tool | Version |
|---|---|
| Android Studio | Android Studio \| AI-262.10968.63.2622.16840690 |
| JDK | JetBrains Runtime 21 (Gradle daemon JVM, set in `gradle/gradle-daemon-jvm.properties`) |
| Gradle | 8.11.1 (Gradle Wrapper) |
| Android Gradle Plugin | 8.7.3 |
| Kotlin | 2.1.0 |
| Compile / Target SDK | 35 |
| Minimum SDK | 24 (Android 7.0) |
| JVM bytecode target | 17 |

### Libraries

| Category | Library | Version | Purpose |
|---|---|---|---|
| UI | Jetpack Compose (BOM) | 2024.12.01 (Compose UI, Foundation, Animation 1.7.6) | Declarative UI, animation, Canvas drawing |
| UI | Material 3 + Material Icons Extended | 1.3.1 / 1.7.6 (via BOM) | Theming and icons |
| Core | AndroidX Core KTX | 1.15.0 | Kotlin extensions for Android framework APIs |
| Architecture | AndroidX Lifecycle (ViewModel, runtime-compose) | 2.8.7 | ViewModels, `collectAsStateWithLifecycle`, lifecycle effects |
| Architecture | Activity Compose | 1.9.3 | Compose host, edge-to-edge, predictive back |
| Navigation | Navigation Compose | 2.8.5 | Type-safe navigation between screens |
| Dependency injection | Hilt (Dagger) + Hilt Navigation Compose | 2.53.1 / 1.2.0 | Dependency injection, ViewModel injection |
| Code generation | KSP | 2.1.0-1.0.29 | Annotation processing for Hilt |
| Persistence | Jetpack DataStore (Preferences) | 1.1.1 | Level progress and settings |
| Serialization | kotlinx.serialization JSON | 1.7.3 | Level data parsing, type-safe navigation routes |
| Concurrency | Kotlin Coroutines + Flow | 1.9.0 | Asynchronous work and reactive state |
| Audio | Android `SoundPool` | Platform | Low-latency sound effects |
| Testing | JUnit 4, kotlinx-coroutines-test | 4.13.2 / 1.9.0 | Unit testing |
| Testing | AndroidX Test (ext-junit, runner) | 1.3.0 / 1.7.0 | Instrumented tests on a device or emulator |

**Fonts:** Fredoka SemiBold (titles, letters, buttons) and Inter Regular/Medium/SemiBold (labels and captions), from Google Fonts.
**Release build:** R8 code shrinking and resource shrinking are enabled.

---

## Architecture

The app follows **MVVM with MVI-style unidirectional data flow** and is organised in three layers:

```
UI (Compose screens + ViewModels)  →  Domain (pure Kotlin)  ←  Data (JSON assets, DataStore)
```

| Layer | Responsibility | Key classes |
|---|---|---|
| **Domain** | Game models, game rules and repository contracts. Plain Kotlin with no Android dependencies, so it is fully unit-testable. | `Level`, `WordPlacement`, `GameProgress`, `WordJudge`, `SelectionPath`, `LevelValidator`, `LevelRepository` |
| **Data** | Implements the repository contracts: loads levels from JSON, and saves progress and settings. | `LevelParser`, `AssetLevelDataSource`, `LevelRepositoryImpl`, `DataStoreProgressRepository`, `DataStoreSettingsRepository` |
| **UI** | Compose screens and their ViewModels. | `HomeViewModel`, `LevelSelectViewModel`, `GameViewModel`, `LetterWheel`, `CrosswordGrid` |

**Key decisions**

- **Single source of truth.** Each ViewModel exposes one immutable `StateFlow<UiState>`, which the UI observes with `collectAsStateWithLifecycle()`.
- **Intents in, state out.** The game screen sends every user action as a `GameIntent` (`LetterEntered`, `DragReleased`, `Shuffle`, `Pause`, `Restart`, …) to a single `onIntent()` entry point.
- **One-shot effects.** Haptics and auto-navigation go through a `Channel` and are collected only while the screen is visible.
- **Stateless composables.** Each screen is split into a `Route`, which connects the ViewModel, lifecycle and back handling, and a stateless `Screen(state, callbacks)` that is easy to preview and test.
- **Dependency inversion.** ViewModels depend on repository interfaces. Hilt binds the concrete implementations in `di/AppModule.kt`.

---

## Custom Swipe Logic

The letter wheel (`ui/game/wheel/LetterWheel.kt`) uses a low-level Compose gesture loop (`pointerInput` + `awaitEachGesture`) instead of the higher-level drag detector, which gives precise control over every touch event. The wheel disc and connecting lines are drawn on a Canvas.

| Rule | Implementation |
|---|---|
| **Single pointer** | The pointer ID from the first touch is the only one tracked. Additional fingers are ignored until it lifts. |
| **Hit detection** | A letter is selected when the touch point is within 34dp of its centre; the nearest letter wins. |
| **Fast-swipe sampling** | Touch events can be far apart during a quick flick. The segment between two events is sampled every 8dp and each sample is hit-tested, so no letter is skipped. |
| **Backtracking** | Moving back onto the previous letter removes the last selected letter (`SelectionPath.enter`). |
| **Submit vs. cancel** | Lifting the finger submits the word if it has 2 or more letters. An interrupted gesture (rotation, incoming call, pause) is cleared without submitting. |
| **Rendering performance** | The selected letter path is stored in the ViewModel. The live finger position is local UI state read only in the draw phase, so finger movement redraws the line without recomposing the screen. |

Hit testing, segment sampling and path reduction live in `WheelGeometry` and `SelectionPath` as pure functions, covered by unit tests.

---

## Level Data Structure

All 20 levels are defined in `app/src/main/assets/levels.json`:

```json
{
  "id": 4,
  "pack": "Dawn",
  "letters": "TEAR",
  "rows": 4,
  "cols": 6,
  "words": [
    { "word": "TEAR", "row": 0, "col": 0, "dir": "H" },
    { "word": "EAT",  "row": 0, "col": 1, "dir": "V" },
    { "word": "RATE", "row": 0, "col": 3, "dir": "V" },
    { "word": "EAR",  "row": 3, "col": 3, "dir": "H" }
  ],
  "bonus": ["TEA", "ART", "RAT", "TAR", "ERA", "ATE", "ETA", "ARE", "TARE"]
}
```

| Field | Description |
|---|---|
| `id` | Unique level number, which sets the play order |
| `pack` | Group name shown on Level Select (`Dawn`, `Dusk`) |
| `letters` | Letters on the wheel (3–5) |
| `rows`, `cols` | Grid size |
| `words` | Grid answers: word, start cell and direction (`H` = across, `V` = down) |
| `bonus` | Extra valid words that are not part of the grid |

**Processing pipeline**

1. `AssetLevelDataSource` reads the JSON file from assets.
2. `LevelParser` maps the JSON data objects to domain models. Grid cells and crossing letters are derived from the word placements, so they are never stored twice.
3. `LevelValidator` checks each level: every word can be spelled from the wheel, fits inside the grid, and agrees with the words it crosses; bonus words are valid and don't repeat grid words. Invalid data fails immediately.
4. `LevelRepositoryImpl` caches the parsed levels in memory (thread-safe with a `Mutex`).

A unit test runs the parser over the real JSON file, so an invalid level cannot ship.

---

## Navigation and State Management

**Navigation flow:** Home → Level Select → Game → Pause (overlay) → Complete (overlay)

| Scenario | Handling |
|---|---|
| Duplicate screens | `navigateSafely()` only navigates while the current screen is `RESUMED`, so rapid or double taps cannot open the same screen twice. |
| Next level | Replaces the finished game with `popUpTo<Game> { inclusive = true }`, so the back stack never grows. |
| Pause → Home / Levels | Returns to the existing screen on the back stack instead of creating a new one. |
| System back button | Game → opens Pause · Pause → resumes · Complete → previous screen. Predictive back is enabled. |
| Screen rotation | The ViewModel survives. Revealed tiles keep their state and do not re-animate. |
| Process death | Found words, bonus words, wheel order and pause state are restored from `SavedStateHandle`; the navigation back stack is restored by the framework. |
| Leaving a level | Progress is saved to DataStore after every word, so the level resumes where the player left off. |
| App backgrounded | The game opens the Pause menu and the auto-advance countdown is cancelled. Rotation is excluded via `isChangingConfigurations`. |
| Interruptions (calls, notifications) | An in-progress swipe is discarded on `ON_PAUSE` without submitting. |
| Memory leaks | ViewModels are scoped to their navigation entry and hold no `Context` or `View` references. |

---

## UI, Motion and Feedback

The visual design follows the provided UI/UX specification. Colours, typography and animation timings are defined as design tokens in `ui/theme/`.

| Interaction | Behaviour |
|---|---|
| Screen transitions | Horizontal slide (24dp) with fade, 280 ms |
| Letter selection | Spring scale to 1.16×, highlight colour, tick sound that rises in pitch with each letter |
| Grid word | Tiles pop in (scale 0.3 → 1 with overshoot, 440 ms), staggered 70 ms per letter |
| Bonus word | Sky-blue bubble "Bonus word +1"; the star counter animates |
| Already found | Neutral bubble, no penalty |
| Invalid word | Coral bubble with a horizontal shake (±8dp, 380 ms): "Not in this puzzle" |
| Level complete | 900 ms hold, then the completion screen with sparkles, cascading tiles, a bonus word summary and a 5-second auto-advance countdown (**Next level** to skip, **Stay** to cancel) |
| Locked level | Tile shakes with reject haptics |
| Buttons | Press-scale micro-interaction |
| Reduced motion | Animations switch to simple fades when system animations are disabled |

**Colour palette:** Night `#101B2E` · Slate `#1D2C45` · Wheel `#17243A` · Paper `#F6EEDF` · Lantern `#F2A541` · Sky `#7CC6FE` · Coral `#FF7A6B`

**Audio and haptics:** Sound effects are original tones created for this project and played through `SoundPool`. Haptics use `View.performHapticFeedback`, which needs no extra permission. Both can be turned off in the Pause menu, and the settings are saved.

---

## Testing

Unit tests cover the game logic and the level data:

| Test class | Coverage |
|---|---|
| `SelectionPathTest` | Adding letters, backtracking, and ignoring letters already in the path |
| `WordJudgeTest` | Grid words, bonus words, already-found words and invalid words |
| `WheelGeometryTest` | Letter positioning, hit testing, and fast-swipe sampling (a letter in the middle of a large jump is still detected) |
| `LevelDataTest` | All 20 shipped levels parse and pass validation; packs and letter counts are correct; conflicting data is rejected |

```bash
./gradlew testDebugUnitTest
```

---

## Project Structure

```
app/src/main/java/com/wordlight/game/
├── WordlightApp.kt                 Hilt application
├── MainActivity.kt                 Single activity, Compose host
├── di/                             Hilt modules and qualifiers
├── core/audio/                     SoundPool sound player
├── domain/
│   ├── model/                      Level, WordPlacement, GameProgress, GameSettings
│   ├── engine/                     WordJudge, SelectionPath, LevelValidator
│   └── repository/                 Repository interfaces
├── data/
│   ├── level/                      JSON DTOs, LevelParser, data source, cached repository
│   ├── progress/                   DataStore progress repository
│   └── settings/                   DataStore settings repository
└── ui/
    ├── theme/                      Colours, typography, motion tokens
    ├── common/                     Shared buttons, tiles, haptics helpers
    ├── navigation/                 Routes, NavHost, safe navigation extensions
    ├── home/                       Home screen + ViewModel
    ├── levels/                     Level Select screen + ViewModel
    └── game/
        ├── GameContract.kt         UI state, intents, effects
        ├── GameViewModel.kt        Game state holder
        ├── GameScreen.kt           Game route and screen
        ├── wheel/                  LetterWheel, WheelGeometry
        ├── grid/                   CrosswordGrid
        └── components/             WordBubble, HUD, Pause sheet, Complete screen

app/src/main/assets/
├── levels.json                     Level definitions
└── sfx/                            Sound effects (.ogg)
```

---

## Build and Run

**Prerequisites**

- Android Studio | AI-262.10968.63.2622.16840690
- JetBrains Runtime (JDK) 21, required by `gradle/gradle-daemon-jvm.properties` (*Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK*)
- Android SDK 35

**Steps**

1. Clone the repository:
   ```bash
   git clone https://github.com/Roshankumr/wordlight.git
   ```
2. Open the project in Android Studio and let Gradle sync.
3. Run the app on a device or emulator, or build from the command line:

| Task | Command |
|---|---|
| Install debug build | `./gradlew installDebug` |
| Run unit tests | `./gradlew testDebugUnitTest` |
| Build release APK | `./gradlew assembleRelease` |

The release APK is written to `app/build/outputs/apk/release/Wordlight-v1.0.0.apk`. It is signed with the debug key so it can be installed for review.
