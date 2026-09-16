# Implementation Plan: School Timetable

## Overview

This plan builds the school-timetable Android app from an empty project. It starts by scaffolding a native Android Gradle project (Kotlin, Jetpack Compose, Material 3, single-Activity MVVM, `minSdk 26`, `java.time`) with Kotest and kotest-property for testing. It then builds the pure domain layer bottom-up — data models and enums, the hardcoded `TimetableData` constants, the repository with a startup self-check, then `WeekTypeCalculator`, `ShiftResolver`, and `ScheduleService` — validating each with property-based and unit/table tests. Finally it adds the presentation layer (`ScheduleViewModel` with an injected `Clock`), the Compose UI (`DailyScreen`, `CalendarScreen`), wires them into a single Activity, adds Compose UI tests, and produces an installable debug APK.

Each task builds on the previous ones and ends by integrating its output into the running app. Sub-tasks marked with `*` are optional tests and can be skipped for a faster MVP.

## Tasks

- [x] 1. Scaffold the Android Gradle project
  - Create the Gradle project structure: root `build.gradle.kts` / `settings.gradle.kts`, `gradle.properties`, Gradle wrapper, and an `:app` module with `app/build.gradle.kts`.
  - Configure Kotlin, Jetpack Compose (Compose BOM) and Material 3 dependencies; set `minSdk 26`, `targetSdk` latest stable, `compileSdk` latest stable; enable Compose build features.
  - Add test dependencies: `io.kotest:kotest-runner-junit5`, `io.kotest:kotest-assertions-core`, `io.kotest:kotest-property`, and `androidx.compose.ui:ui-test-junit4` (+ manifest/debug test dependencies); configure the test task to use JUnit Platform and set Kotest global property iteration default to ≥100.
  - Create `AndroidManifest.xml` declaring the single `MainActivity` as launcher, with NO `INTERNET` / network permission and no account/sign-in requirement.
  - Add a minimal placeholder `MainActivity` and Serbian string resources scaffold (day/shift/message labels) so the project compiles and assembles.
  - _Requirements: 6.1, 5.5_

- [x] 2. Implement domain data models, enums, and fixed group membership
  - [x] 2.1 Create enums and core data models
    - Define `WeekType { A, B }`, `DayGroup { PARNA, NEPARNA }`, `Shift { MORNING, AFTERNOON }`, and `DailyMessage { WEEKEND, NO_CLASSES, DATA_UNAVAILABLE, DATE_UNDETERMINED }`.
    - Define `Lesson(period, classCode: String?)` with `isPause`, `BellTime(period, start, end)`, `ResolvedLesson(period, classCode, start, end)` with `isPause`, and `DaySchedule(date, dayOfWeek, isWeekend, group, weekType, shift, lessons)` using `java.time`.
    - Define the fixed `DAY_GROUP: Map<DayOfWeek, DayGroup>` (Mon/Thu/Fri → PARNA, Tue/Wed → NEPARNA).
    - _Requirements: 1.5, 5.1, 1.3_

  - [ ]* 2.2 Write unit tests for data models and DAY_GROUP
    - Assert `Lesson`/`ResolvedLesson` `isPause` is true exactly when `classCode == null`; assert `DAY_GROUP` maps the five weekdays as specified and has no weekend entries.
    - _Requirements: 1.5, 1.3, 5.1_

- [x] 3. Implement the hardcoded TimetableData constants
  - [x] 3.1 Create the TimetableData object
    - Implement `TimetableData.LESSONS` for Mon–Fri exactly per the spec (Mon: 6=5/4,7=5/2; Tue: 2=6/1,3=8/5,4=6/3,5=6/5,6=7/1,7=7/5; Wed: 1=7/3,2=5/3,3=8/1,4=Pause,5=6/7,6=8/7; Thu: 1=6/4,2=6/2,3=6/6,4=8/4,5=Pause,6=7/4,7=7/2; Fri: 4=7/6,5=8/2,6=8/6,7=7/7), with empty lists for Sat/Sun.
    - Implement `MORNING_BELLS` and `AFTERNOON_BELLS` maps (periods 1–7) with the exact clock times per the spec.
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

  - [ ]* 3.2 Write table-driven unit tests for the constant data
    - Assert `LESSONS`, `MORNING_BELLS`, and `AFTERNOON_BELLS` match the specified values exactly, including the Wed period-4 and Thu period-5 pauses and empty weekend lists.
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [x] 4. Implement TimetableRepository with startup self-check
  - [x] 4.1 Create the repository interface and constant-backed implementation
    - Define `TimetableRepository` with `lessonsFor(dayOfWeek)`, `bellTimesFor(shift)`, and `load(): TimetableLoadResult`; define `sealed interface TimetableLoadResult { Available; Unavailable(reason) }`.
    - Implement a constant-backed repository reading from `TimetableData`; `load()` runs a lightweight self-check (every non-weekend day has ≥1 lesson within periods 1–7; every referenced period has a bell time in both shifts) returning `Unavailable` on failure, else `Available`.
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.7_

  - [ ]* 4.2 Write unit tests for the repository and self-check
    - Assert `lessonsFor`/`bellTimesFor` return the constant data; assert `load()` returns `Available` for the real data and `Unavailable` for a deliberately inconsistent stub.
    - _Requirements: 5.7_

- [x] 5. Implement WeekTypeCalculator
  - [x] 5.1 Create the WeekTypeCalculator object
    - Set `ANCHOR_MONDAY = LocalDate.of(2026, 8, 31)`; implement `weeksFromAnchor(date)` (Monday-of-week via `previousOrSame(MONDAY)`, `floorDiv(days,7)`, absolute value) and `weekTypeFor(date)` returning `A` for even counts and `B` for odd.
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 6. Implement ShiftResolver
  - [x] 6.1 Create the ShiftResolver object
    - Implement `shiftFor(group, weekType)`: WeekType.A → PARNA=MORNING, NEPARNA=AFTERNOON; WeekType.B → PARNA=AFTERNOON, NEPARNA=MORNING.
    - _Requirements: 2.5, 2.6_

- [x] 7. Implement ScheduleService
  - [x] 7.1 Create the ScheduleService class
    - Implement `scheduleFor(date)`: resolve weekday and `WeekType`; for weekends return a `DaySchedule` with `isWeekend=true`, null group/shift, empty lessons; otherwise resolve group via `DAY_GROUP`, shift via `ShiftResolver`, fetch bells for the shift and lessons for the weekday, sort ascending by period, and map to `ResolvedLesson`s applying the shift's bell times.
    - Ensure class codes come only from `lessonsFor` (WeekType-independent) so only times vary by shift.
    - _Requirements: 1.1, 1.2, 2.7, 2.8_

- [ ] 8. Property-based and unit tests for the domain layer
  - [ ]* 8.1 Write property test for Property 1 (WeekType determinism & parity)
    - Kotest property test over `Arb<LocalDate>` (wide range incl. dates before the anchor), ≥100 iterations; assert `weekTypeFor` is `A` iff `weeksFromAnchor` is even, is stable across repeated/freshly-constructed dates, and the anchor week is `A`.
    - **Property 1: WeekType is deterministic and follows even/odd week parity**
    - Tag: `// Feature: school-timetable, Property 1: WeekType is deterministic and follows even/odd week parity`
    - **Validates: Requirements 2.1, 2.3**

  - [ ]* 8.2 Write property test for Property 2 (same week shares WeekType)
    - Property test: for any date and offset `0..6` staying within the same Mon–Sun week, both dates yield the same `WeekType`; ≥100 iterations.
    - **Property 2: Dates in the same Monday–Sunday week share one WeekType**
    - Tag: `// Feature: school-timetable, Property 2: Dates in the same Monday–Sunday week share one WeekType`
    - **Validates: Requirements 2.2**

  - [ ]* 8.3 Write property test for Property 3 (consecutive weeks alternate)
    - Property test: `weekTypeFor(date)` and `weekTypeFor(date.plusWeeks(1))` always differ; ≥100 iterations.
    - **Property 3: Consecutive weeks alternate WeekType**
    - Tag: `// Feature: school-timetable, Property 3: Consecutive weeks alternate WeekType`
    - **Validates: Requirements 2.4**

  - [ ]* 8.4 Write property test for Property 4 (shift resolution & bell-time application)
    - Property test over Mon–Fri dates: resolved shift is MORNING iff (PARNA & A) or (NEPARNA & B) else AFTERNOON; each `ResolvedLesson` start/end equal that shift's `BellTime` for the period; ≥100 iterations.
    - **Property 4: Shift resolution and bell-time application are correct**
    - Tag: `// Feature: school-timetable, Property 4: Shift resolution and bell-time application are correct`
    - **Validates: Requirements 2.5, 2.6, 2.7, 1.1**

  - [ ]* 8.5 Write property test for Property 5 (class codes invariant across WeekTypes)
    - Property test over Mon–Fri weekdays: ordered `classCode` sequence identical for an A-week date and a B-week date of the same weekday, while clock times differ; ≥100 iterations.
    - **Property 5: Class codes are invariant across WeekTypes; only times change**
    - Tag: `// Feature: school-timetable, Property 5: Class codes are invariant across WeekTypes; only times change`
    - **Validates: Requirements 2.8**

  - [ ]* 8.6 Write property test for Property 6 (group membership is fixed)
    - Property test over Mon–Fri dates: `DaySchedule.group` equals `DAY_GROUP` for the weekday regardless of week; ≥100 iterations.
    - **Property 6: Group membership is fixed**
    - Tag: `// Feature: school-timetable, Property 6: Group membership is fixed`
    - **Validates: Requirements 1.3**

  - [ ]* 8.7 Write property test for Property 7 (ascending order & pauses preserved)
    - Property test over Mon–Fri dates: resolved lessons strictly ascending by period; every lesson with source `classCode==null` has `isPause==true` and valid start/end; ≥100 iterations.
    - **Property 7: Resolved lessons are ascending and preserve pauses**
    - Tag: `// Feature: school-timetable, Property 7: Resolved lessons are ascending and preserve pauses`
    - **Validates: Requirements 1.2, 1.5**

  - [ ]* 8.8 Write property test for Property 8 (weekends yield no lessons)
    - Property test over Sat/Sun dates: `isWeekend==true`, empty lessons, null group and shift; ≥100 iterations.
    - **Property 8: Weekend days yield no lessons**
    - Tag: `// Feature: school-timetable, Property 8: Weekend days yield no lessons`
    - **Validates: Requirements 1.6, 3.5, 4.5, 6.3**

  - [ ]* 8.9 Write property test for Property 9 (day navigation round-trips)
    - Property test over any date: `+1 then -1` (and reverse) returns the original date's schedule; a single step yields exactly the schedule of `date ± 1` with matching weekday/group/shift; ≥100 iterations.
    - **Property 9: Day navigation round-trips to the same schedule**
    - Tag: `// Feature: school-timetable, Property 9: Day navigation round-trips to the same schedule`
    - **Validates: Requirements 3.2, 3.3, 3.4, 4.2**

  - [ ]* 8.10 Write property test for Property 10 (data reads idempotent)
    - Property test over `DayOfWeek` and `Shift`: repeated `lessonsFor`/`bellTimesFor` calls, including across freshly constructed repositories, return equal results; ≥100 iterations.
    - **Property 10: Timetable data reads are idempotent**
    - Tag: `// Feature: school-timetable, Property 10: Timetable data reads are idempotent`
    - **Validates: Requirements 5.6**

  - [ ]* 8.11 Write example/unit tests for domain branches
    - Weekday-with-no-periods via a stub repository returning an empty list → produces the `NO_CLASSES` path; startup data-unavailable via a stub returning `Unavailable`, constants untouched.
    - _Requirements: 1.7, 5.7_

- [ ] 9. Checkpoint - Ensure all domain tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 10. Implement ScheduleViewModel with injected Clock and navigation
  - [x] 10.1 Create ScheduleViewModel and DailyUiState mapping
    - Implement `ScheduleViewModel(scheduleService, clock)` exposing `StateFlow<DailyUiState>`; map `DaySchedule` → `DailyUiState` (localized `headerLabel`, `groupLabel`, `shiftLabel`, lessons, and `message` for weekend/no-classes).
    - Implement `initToToday()` (sets date to `LocalDate.now(clock)`; on clock failure set `DATE_UNDETERMINED` and default to most recent weekday), `nextDay()`, `previousDay()`, `selectDate(date)`; surface `DATA_UNAVAILABLE` when `repository.load()` is `Unavailable`.
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.2, 6.2, 6.3, 6.4, 5.7, 1.6, 1.7_

  - [ ]* 10.2 Write unit tests for ViewModel launch and navigation
    - Injected fixed `Clock`: today selected/rendered; weekend clock → `WEEKEND`; throwing clock → `DATE_UNDETERMINED` defaulting to most recent weekday; `nextDay`/`previousDay`/`selectDate` update state; `Unavailable` load → `DATA_UNAVAILABLE`.
    - _Requirements: 3.1, 3.2, 3.3, 6.2, 6.3, 6.4, 5.7_

- [x] 11. Implement the DailyScreen composable
  - [x] 11.1 Create DailyScreen
    - Render `DailyUiState`: header (weekday + group), shift label, ordered period rows (number, class code, start–end times), pause rows with a visible no-class indication; weekend/no-classes messages that omit the period list; prev/next controls calling the ViewModel.
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7, 3.2, 3.3_

- [x] 12. Implement the CalendarScreen composable
  - [x] 12.1 Create CalendarScreen
    - Material 3 `DatePicker` defaulting to the selected date's month, visually marking the selected date, and calling `selectDate(date)` to return to the Daily view; weekend selections show the no-classes message and retain the date.
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

- [x] 13. Wire the single-Activity navigation
  - [x] 13.1 Assemble MainActivity, navigation, and dependency wiring
    - In `MainActivity`, set up a single-Activity Compose navigation graph with Daily and Calendar destinations sharing one `ScheduleViewModel`; construct `TimetableRepository` → `ScheduleService` → `ScheduleViewModel` with a real `Clock`; call `initToToday()` on start; navigate to Calendar and back on date selection.
    - _Requirements: 3.1, 4.1, 4.2, 6.1, 6.2_

- [ ] 14. Compose UI tests
  - [ ]* 14.1 Write Compose UI tests for DailyScreen and CalendarScreen
    - Using `createComposeRule`: Daily renders period rows (number/class code/times), weekday+group label, shift label, and pause indication; prev/next change the displayed date; Calendar defaults to the selected month, marks the selected date, and returns the chosen date to the Daily view.
    - _Requirements: 1.1, 1.3, 1.4, 1.5, 3.2, 3.3, 4.1, 4.2, 4.3, 4.4_

- [x] 15. Final assembly and verification
  - [x] 15.1 Build, run tests, and produce the installable debug APK
    - Run the full test suite (`test`) and confirm the app compiles; run `assembleDebug` to produce the installable debug APK for sideloading; confirm the manifest declares no network permission.
    - _Requirements: 6.1, 5.5, 6.2_

- [ ] 16. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP.
- Each task references specific requirements (and correctness properties) for traceability.
- Checkpoints ensure incremental validation of the domain layer and the full app.
- Property tests (task 8) validate the 10 universal correctness properties from the design, each as its own test with the required `// Feature: school-timetable, Property N: ...` tag and ≥100 iterations.
- Unit/table tests validate the fixed constant data and specific error branches; Compose UI tests validate rendering and navigation.
- All time-dependent behavior is driven by an injected `Clock` for deterministic tests.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1"] },
    { "id": 1, "tasks": ["2.1"] },
    { "id": 2, "tasks": ["2.2", "3.1"] },
    { "id": 3, "tasks": ["3.2", "4.1", "5.1", "6.1"] },
    { "id": 4, "tasks": ["4.2", "7.1", "8.1", "8.2", "8.3", "8.10"] },
    { "id": 5, "tasks": ["8.4", "8.5", "8.6", "8.7", "8.8", "8.9", "8.11", "10.1"] },
    { "id": 6, "tasks": ["10.2", "11.1", "12.1"] },
    { "id": 7, "tasks": ["13.1"] },
    { "id": 8, "tasks": ["14.1", "15.1"] }
  ]
}
```
