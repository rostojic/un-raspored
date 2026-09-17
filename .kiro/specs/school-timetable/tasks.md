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

## Feature 2 & 3 Tasks (editable Owner, Timetable, Group Assignment, and Revert)

> Tasks 17+ implement Feature 2 (edit/persist Owner and the Stored_Schedule with group assignment) and Feature 3 (single-previous Revert), per Requirements 7, 8, and 9 and the revised design (DataStore + kotlinx.serialization persistence). They build on the completed domain/UI work (tasks 1–15). Feature 1 (Cyrillic header) is already implemented and needs no task. Sub-tasks marked with `*` are optional tests, following the conventions of tasks 8 and 14.

- [ ] 17. Add persistence dependencies and version catalog entries
  - Add the kotlinx-serialization Kotlin plugin and its version to `gradle/libs.versions.toml` (`[versions] kotlinxSerialization`, `[plugins] kotlin-serialization`), and library entries for `androidx.datastore:datastore-preferences` and `org.jetbrains.kotlinx:kotlinx-serialization-json`.
  - Apply the `kotlin-serialization` plugin in `app/build.gradle.kts` and add the `datastore-preferences` and `kotlinx-serialization-json` implementation dependencies.
  - _Requirements: 5.5, 6.1, 8.14, 7.9_

- [ ] 18. Add serializable persisted models and the Default_Schedule/DEFAULT_OWNER seeds
  - [ ] 18.1 Create the `StoredSchedule` and `PersistedState` serializable models
    - Mark `Lesson` `@Serializable`; add `@Serializable data class StoredSchedule(lessonsByDay: Map<DayOfWeek, List<Lesson>>, groupByDay: Map<DayOfWeek, DayGroup>)` and `@Serializable data class PersistedState(owner: String, current: StoredSchedule, previous: StoredSchedule? = null)` in the domain layer (`DayOfWeek`/`DayGroup` serialize by name; `classCode == null` => Pause).
    - _Requirements: 8.1, 8.2, 8.3, 9.1, 9.2_

  - [ ] 18.2 Add the `DEFAULT_SCHEDULE` and `DEFAULT_OWNER` seeds
    - Build `DEFAULT_SCHEDULE: StoredSchedule` from `TimetableData.LESSONS` (excluding Sat/Sun) + the existing `DAY_GROUP` map, and define `DEFAULT_OWNER = "Olivera"`; keep `DAY_GROUP` as the seed source only.
    - _Requirements: 8.1, 8.11, 7.1, 5.2_

  - [ ]* 18.3 Write property test for Property 12 (persisted state round-trip)
    - Kotest property test over generated `PersistedState` values (owner length 1..50, arbitrary `current`, `previous` absent or present); assert JSON encode-then-decode yields an equal `PersistedState`, ≥100 iterations.
    - **Property 12: Persisted state round-trips through storage unchanged**
    - Tag: `// Feature: school-timetable, Property 12: Persisted state round-trips through storage unchanged`
    - **Validates: Requirements 7.5, 8.10, 9.7**

- [ ] 19. Implement SchedulePreferencesStore (DataStore + JSON I/O)
  - [ ] 19.1 Create the SchedulePreferencesStore
    - Wrap a `DataStore<Preferences>` with a single `persisted_state` string key; expose `val state: Flow<PersistedState?>` that decodes the JSON (mapping a parse failure to a null/read-failure signal) and `suspend fun write(state: PersistedState)` that serializes and writes atomically, throwing on write failure. This is the only device-local I/O point.
    - _Requirements: 5.5, 7.9, 8.14, 8.9_

- [ ] 20. Implement SettingsRepository (seed/reseed, owner, save, revert)
  - [ ] 20.1 Create the SettingsRepository and result types
    - Implement `SettingsRepository(store, snapshotSink)` exposing `StateFlow<PersistedState>`; define `InitOutcome`, `OwnerSaveResult`, `ScheduleSaveResult`, `RevertResult` sealed types.
    - Implement `initialize()`: seed `DEFAULT_SCHEDULE`/`DEFAULT_OWNER` on first run (`SeededDefault`), reseed on unreadable/unparseable stored data with an error indication (`ReseededAfterCorruption`), else `Ok`; push each current `StoredSchedule` into `TimetableRepository`'s snapshot via `snapshotSink`.
    - _Requirements: 8.1, 8.11, 5.6, 5.7_

  - [ ] 20.2 Implement saveOwner, saveSchedule, and revert
    - `saveOwner(raw)`: trim, validate 1..50, store trimmed value replacing prior owner or return `Empty`/`TooLong` leaving stored owner unchanged.
    - `saveSchedule(edited)`: build new `PersistedState(current = edited, previous = oldCurrent)` retaining exactly one previous; on write failure return `WriteFailed` leaving stored state unchanged.
    - `revert()`: if `previous != null` set `current = previous`, `previous = null` in one write leaving `owner` unchanged; else `NoPrevious` writing nothing.
    - _Requirements: 7.3, 7.4, 7.7, 7.8, 8.8, 8.9, 9.1, 9.2, 9.3, 9.4, 9.5, 9.6_

  - [ ]* 20.3 Write property test for Property 11 (owner validation)
    - Property test over arbitrary input strings: trimmed length 1..50 stores the trimmed value and reports success; trimmed length 0 reports empty and leaves the owner unchanged; trimmed length >50 reports too-long and leaves the owner unchanged; ≥100 iterations.
    - **Property 11: Owner validation trims and enforces the 1–50 length bound**
    - Tag: `// Feature: school-timetable, Property 11: Owner validation trims and enforces the 1–50 length bound`
    - **Validates: Requirements 7.3, 7.4, 7.7, 7.8**

  - [ ]* 20.4 Write property test for Property 14 (editing leaves stored schedule untouched until save)
    - Property test: for any initial stored `StoredSchedule` and any sequence of working-copy edits without a save, the persisted `StoredSchedule` stays equal to the initial value; ≥100 iterations.
    - **Property 14: Editing leaves the stored schedule untouched until save**
    - Tag: `// Feature: school-timetable, Property 14: Editing leaves the stored schedule untouched until save`
    - **Validates: Requirements 8.7**

  - [ ]* 20.5 Write property test for Property 15 (save retains one previous; revert restores and discards)
    - Property test: after `saveSchedule(E)`, `current == E` (including all-Pause weekdays) and `previous` equals the pre-save current, with at most one previous across any save sequence; for a state with non-null previous `P`, `revert` sets `current == P`, `previous == null`, leaves `owner` unchanged, and a second consecutive revert finds no previous; ≥100 iterations.
    - **Property 15: Save retains one previous; revert restores and discards it; owner is unaffected**
    - Tag: `// Feature: school-timetable, Property 15: Save retains one previous; revert restores and discards it; owner is unaffected`
    - **Validates: Requirements 8.8, 9.1, 9.2, 9.3, 9.4, 9.5**

  - [ ]* 20.6 Write property test for Property 16 (seeding is idempotent)
    - Property test: running `initialize()` when a valid `StoredSchedule` already exists leaves the stored owner and schedule unchanged (seeds only when nothing readable is stored); ≥100 iterations.
    - **Property 16: Seeding is idempotent**
    - Tag: `// Feature: school-timetable, Property 16: Seeding is idempotent`
    - **Validates: Requirements 8.1, 8.11**

  - [ ]* 20.7 Write unit tests for SettingsRepository seed/reseed/save/revert branches
    - Test first-run seeding, reseed-on-corruption with error indication, owner empty/too-long/valid branches, save write-failure retaining prior state, and revert `NoPrevious` no-op.
    - _Requirements: 8.1, 8.9, 8.11, 9.6, 7.7, 7.8_

- [ ] 21. Evolve TimetableRepository to snapshot + groupFor and update ScheduleService
  - [ ] 21.1 Back TimetableRepository reads with a StoredSchedule snapshot and add groupFor
    - Change `ConstantTimetableRepository` to read `lessonsFor(dayOfWeek)` and a new `groupFor(dayOfWeek): DayGroup?` from a mutable in-memory `StoredSchedule` snapshot (defaulting to `DEFAULT_SCHEDULE`, refreshed via the `SettingsRepository` snapshot sink); keep `bellTimesFor(shift)` from the bell constants and retain the `load()` self-check.
    - _Requirements: 5.1, 5.3, 5.4, 8.2, 8.6, 8.13_

  - [ ] 21.2 Update ScheduleService to resolve group via repository.groupFor
    - Change `scheduleFor(date)` to read the weekday's group from `repository.groupFor(dayOfWeek)` instead of the `DAY_GROUP` constant, so the Daily view reflects the current stored assignment; keep bell times, WeekType, and shift-alternation logic unchanged.
    - _Requirements: 8.6, 8.12, 8.13, 1.3, 2.7_

  - [ ]* 21.3 Write property test for reframed Property 6 (group follows stored assignment)
    - Property test over any current `StoredSchedule` and any Mon–Fri date: resolved `DaySchedule.group` equals `storedSchedule.groupByDay[dayOfWeek]` and is independent of the date's week; ≥100 iterations.
    - **Property 6: Group membership follows the current stored assignment and is fixed within a week**
    - Tag: `// Feature: school-timetable, Property 6: Group membership follows the current stored assignment and is fixed within a week`
    - **Validates: Requirements 1.3, 8.6, 8.12**

  - [ ]* 21.4 Write unit tests for snapshot refresh and groupFor
    - Assert `lessonsFor`/`groupFor` reflect the snapshot after the sink pushes a new `StoredSchedule`, and default to `DEFAULT_SCHEDULE` before any push.
    - _Requirements: 8.2, 8.6, 8.12_

- [ ] 22. Wire ScheduleViewModel to SettingsRepository
  - [ ] 22.1 Recompute DailyUiState when the stored schedule changes
    - Add the `SettingsRepository` dependency to `ScheduleViewModel`; collect `SettingsRepository.state`, refresh the in-memory snapshot on each emission, and recompute the current `DailyUiState` so an edit/save/revert is reflected automatically.
    - _Requirements: 8.12, 3.1, 6.2_

  - [ ]* 22.2 Write unit tests for schedule-change recomputation
    - With an injected fixed `Clock`, assert the Daily state recomputes to the new class codes/group after the `SettingsRepository` emits an edited schedule.
    - _Requirements: 8.12_

- [ ] 23. Implement SettingsViewModel and SettingsScreen
  - [ ] 23.1 Create SettingsViewModel and SettingsUiState
    - Implement `SettingsViewModel(settingsRepository)` exposing `StateFlow<SettingsUiState>` (`storedOwner`, `editingText`, `error: OwnerError?`); implement `onOwnerTextChanged` and `saveOwner()` mapping `OwnerSaveResult` to `SettingsUiState.error`; define `enum OwnerError { EMPTY, TOO_LONG }`.
    - _Requirements: 7.2, 7.3, 7.4, 7.6, 7.7, 7.8_

  - [ ] 23.2 Create the SettingsScreen composable
    - Render the current Owner in an editable text field, a Save action, inline validation messages for empty (Req 7.7) and over-50 (Req 7.8), and show the stored value on success.
    - _Requirements: 7.2, 7.4, 7.6, 7.7, 7.8_

  - [ ]* 23.3 Write Compose UI tests for SettingsScreen
    - Using `createComposeRule`: the field shows the current owner; saving a valid name shows the stored value; empty and >50-char inputs show the corresponding error and leave the shown owner unchanged.
    - _Requirements: 7.2, 7.4, 7.7, 7.8_

- [ ] 24. Implement ScheduleEditorViewModel and ScheduleEditorScreen
  - [ ] 24.1 Create ScheduleEditorViewModel and EditorUiState (working copy)
    - Implement `ScheduleEditorViewModel(settingsRepository)` holding a working-copy `StoredSchedule` initialized from the current stored value; implement `onClassCodeChanged` (trim; blank => Pause; flag `TOO_LONG` when >20), `onGroupToggled`, `save()`, and `revert()`; define `EditorUiState(working, hasPrevious, classCodeErrors, saveError, notice)`, `enum ClassCodeError { TOO_LONG }`, `enum EditorNotice { SAVED, REVERTED, NO_PREVIOUS }`.
    - _Requirements: 8.2, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 9.3, 9.6_

  - [ ] 24.2 Create the ScheduleEditorScreen composable
    - For each weekday Mon–Fri render seven period rows (1..7) with an editable class-code field (blank = Pause) and a ПАРНА/НЕПАРНА group toggle; add Save and Revert buttons; show save error (Req 8.9), the no-previous notice (Req 9.6), and edit only the working copy until Save.
    - _Requirements: 8.2, 8.3, 8.4, 8.5, 8.6, 8.7, 8.8, 8.9, 9.3, 9.6_

  - [ ]* 24.3 Write property test for Property 13 (editor edits produce expected working copy)
    - Property test over any working `StoredSchedule`, weekday Mon–Fri, and period 1..7: a trimmed 1..20 class code sets that period's `classCode`; a blank/whitespace field sets it to a Pause; toggling the group sets `groupByDay[weekday]`; all other entries unchanged; ≥100 iterations.
    - **Property 13: Editor edits produce the expected working copy**
    - Tag: `// Feature: school-timetable, Property 13: Editor edits produce the expected working copy`
    - **Validates: Requirements 8.4, 8.5, 8.6**

  - [ ]* 24.4 Write Compose UI tests for ScheduleEditorScreen
    - Using `createComposeRule`: seven period rows per weekday with current codes/pauses; editing a field then Save reflects in stored data; clearing a field marks a Pause; group toggle switches ПАРНА/НЕПАРНА; Revert with no previous shows the no-previous notice.
    - _Requirements: 8.2, 8.3, 8.4, 8.5, 8.6, 8.8, 9.6_

- [ ] 25. Add the Daily overflow menu, navigation routes, and DI wiring
  - [ ] 25.1 Add the Daily top-bar overflow menu (⋮)
    - Add an overflow menu to `DailyScreen`'s top app bar with items "Подешавања" (Settings) and "Измени распоред" (Edit schedule) that navigate to the `settings` and `editor` routes.
    - _Requirements: 7.2, 8.2_

  - [ ] 25.2 Add settings and editor routes and wire dependencies in MainActivity
    - Extend the `NavHost` with `settings` and `editor` destinations (with up/back to Daily); construct `SchedulePreferencesStore` → `SettingsRepository` (wired to the `TimetableRepository` snapshot sink), call `initialize()` on start, and scope `SettingsViewModel`/`ScheduleEditorViewModel` via `viewModelFactory` sharing the single `SettingsRepository`.
    - _Requirements: 6.1, 6.2, 7.2, 8.1, 8.11, 8.12_

  - [ ]* 25.3 Write Compose UI test for the overflow menu navigation
    - Using `createComposeRule`: opening the ⋮ menu shows both items and selecting each navigates to the Settings and Editor screens.
    - _Requirements: 7.2, 8.2_

- [ ] 26. Add Serbian strings for Settings, Editor, and the overflow menu
  - Add string resources for "Подешавања", "Измени распоред", ПАРНА/НЕПАРНА labels, Save/Revert actions, owner-empty and owner-too-long messages, the save-failed message, and the no-previous-schedule notice.
  - _Requirements: 7.7, 7.8, 8.3, 8.9, 9.6_

- [ ] 27. Checkpoint - Build and verify Features 2 & 3
  - [ ] 27.1 Run tests and assemble the debug APK
    - Run the full test suite (`test`) and confirm the app compiles; run `assembleDebug` to produce the installable debug APK; confirm the manifest still declares no network permission.
    - _Requirements: 6.1, 5.5, 8.14_

- [ ] 28. Final checkpoint - Ensure all Features 2 & 3 tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for a faster MVP.
- Each task references specific requirements (and correctness properties) for traceability.
- Checkpoints ensure incremental validation of the domain layer and the full app.
- Property tests (task 8) validate the 10 universal correctness properties from the design, each as its own test with the required `// Feature: school-timetable, Property N: ...` tag and ≥100 iterations.
- Unit/table tests validate the fixed constant data and specific error branches; Compose UI tests validate rendering and navigation.
- All time-dependent behavior is driven by an injected `Clock` for deterministic tests.
- Tasks 17–28 are the Feature 2 & 3 tasks (edit/persist Owner, edit/persist the timetable + group assignment with seed/reseed and working copy, and single-previous Revert) covering Requirements 7, 8, and 9. They build on the completed tasks 1–15 and follow the same conventions. Property tests for the new editable state (Properties 11–16, plus reframed Property 6) carry the required `// Feature: school-timetable, Property N: ...` tag and run ≥100 iterations, matching task 8.

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
    { "id": 8, "tasks": ["14.1", "15.1"] },
    { "id": 9, "tasks": ["17"] },
    { "id": 10, "tasks": ["18.1"] },
    { "id": 11, "tasks": ["18.2", "18.3"] },
    { "id": 12, "tasks": ["19.1"] },
    { "id": 13, "tasks": ["20.1"] },
    { "id": 14, "tasks": ["20.2", "20.6", "20.7"] },
    { "id": 15, "tasks": ["20.3", "20.4", "20.5", "21.1"] },
    { "id": 16, "tasks": ["21.2", "21.4"] },
    { "id": 17, "tasks": ["21.3", "22.1"] },
    { "id": 18, "tasks": ["22.2", "23.1", "24.1"] },
    { "id": 19, "tasks": ["23.2", "24.2", "24.3"] },
    { "id": 20, "tasks": ["23.3", "24.4", "25.1", "25.2", "26"] },
    { "id": 21, "tasks": ["25.3", "27.1"] }
  ]
}
```
