# Requirements Document

## Introduction

This document defines the requirements for a single-user, fully offline Android application that displays the personal teaching schedule of a primary school teacher. The application targets a personal Samsung Android device (sideloaded/installed manually) and requires no account or sign-in.

The application displays, for any selected calendar date, the class/group codes the Owner (the teacher whose schedule is shown) teaches during each period, together with the correct clock (bell) times for that day. The App is fully offline: it uses no network connection and no user account. Because the Owner name, the weekly class layout, and the day-to-group assignment are all user-editable and stored on the device, the same App build can be used by different teachers, where each teacher enters their own Owner name and their own schedule.

The scheduling model has two independent parts:

1. **Weekly class layout.** The classes taught on each weekday do not change from week to week; the same class/group codes always fall on the same weekday. Weekdays are organized into two groups:
   - **Parna group** days (default): Monday, Thursday, Friday.
   - **Neparna group** days (default): Tuesday, Wednesday.

   The weekly class layout and the assignment of weekdays to the Parna group or the Neparna group are user-editable and stored on the device. The App ships with the documented default class layout and group assignment as the seed values, which are used on first run and then stored. Group membership does not change week to week; it changes only when the Owner edits it.
2. **Weekly shift alternation.** What changes week to week is only the shift (morning vs afternoon). The Parna group and the Neparna group swap shifts each week. This is captured by a Week_Type (A or B) computed deterministically from the date. The Week_Type computation, the shift alternation logic, and the Bell_Times are fixed and are not user-editable.

For any given date, the application determines the weekday (which yields the fixed class list and the group), computes the Week_Type from the date (which yields whether that group is on the morning or afternoon shift that week), and displays each period with its class/group code and the correct clock times for the resulting shift.

## Glossary

- **App**: The Android application that displays the teaching schedule, installed on a single Samsung Android device.
- **Teacher**: The single user of the App, a primary school teacher who is the Owner of the currently stored schedule.
- **Owner**: The name of the teacher whose schedule the App displays. The Owner is a single text value, editable by the Teacher and stored on the device, with a default value of "Olivera".
- **Timetable**: The complete weekly layout of periods and class/group codes for all weekdays, independent of Week_Type. The Timetable is user-editable and stored on the device (see Stored_Schedule).
- **Weekday**: A day of the week from Monday through Sunday. Monday through Friday may contain scheduled periods; Saturday and Sunday do not.
- **Group**: An assignment of a weekday to one of two groups, the Parna_Group or the Neparna_Group. Group membership is user-editable and stored on the device, and does not change week to week; it changes only when the Owner edits it. By default the **Parna group** consists of Monday, Thursday, and Friday, and the **Neparna group** consists of Tuesday and Wednesday.
- **Parna_Group**: The group of weekdays assigned to the Parna group, labeled "ПАРНА" in the Schedule_Editor. Default membership: Monday, Thursday, and Friday.
- **Neparna_Group**: The group of weekdays assigned to the Neparna group, labeled "НЕПАРНА" in the Schedule_Editor. Default membership: Tuesday and Wednesday.
- **Group_Assignment**: The mapping of each weekday Monday through Friday to either the Parna_Group or the Neparna_Group. The Group_Assignment is part of the Stored_Schedule and is user-editable.
- **Week_Type**: A deterministic classification of a Monday-to-Sunday calendar week as either "A" or "B", which determines the Shift assigned to each Group for that week. In Week_Type "A", the Parna_Group is on the morning Shift and the Neparna_Group is on the afternoon Shift. In Week_Type "B", the Parna_Group is on the afternoon Shift and the Neparna_Group is on the morning Shift.
- **Shift_Pattern**: The mapping, for a given Week_Type, from each Group to its Shift (morning or afternoon).
- **Shift**: The time-of-day segment a Group operates in for a given week, either "morning" (ПРЕПОДНЕВНА СМЕНА) or "afternoon" (ПОПОДНЕВНА СМЕНА). The Shift determines the clock times of each Period.
- **Period**: An ordinal teaching slot within a day, numbered 1 through 7. Each Period has a start and end clock time determined by the Shift.
- **Bell_Times**: The fixed set of Period start and end clock times for a given Shift.
- **Class_Code** (also **Group_Code**): The code identifying the class/group the Teacher teaches during a Period, for example "5/4", "6/1", or "8/7".
- **Pause** (also **Free_Period**): A Period within a school day for which no class is taught (a free or break period).
- **Anchor_Week**: The fixed reference Monday-to-Sunday week used to compute Week_Type. The Anchor_Week begins on Monday 2026-08-31 and is defined as Week_Type "A".
- **Daily_View**: The App screen that shows the schedule for one Selected_Date, including each Period, its Class_Code, and its clock times.
- **Calendar_View**: The App screen or control that lets the Teacher select any date to display in the Daily_View.
- **Selected_Date**: The single calendar date currently displayed in the Daily_View.
- **Weekend_Day**: Saturday or Sunday, for which no Periods are scheduled.
- **Settings_View**: The App screen on which the Teacher can view and edit the Owner name.
- **Schedule_Editor**: The App screen on which the Teacher can edit the Class_Code (or Pause) of each Period for each weekday Monday through Friday and edit the Group_Assignment (which weekdays belong to the Parna_Group labeled "ПАРНА" versus the Neparna_Group labeled "НЕПАРНА").
- **Stored_Schedule**: The device-local, user-editable schedule data consisting of the Timetable (the Class_Code or Pause for each Period of each weekday Monday through Friday) and the Group_Assignment. The Stored_Schedule does not include Bell_Times or the Week_Type computation, which are fixed.
- **Default_Schedule**: The Stored_Schedule values the App ships with and uses to seed storage on first run: the Timetable of Requirement 5.2 and the Group_Assignment Parna_Group = {Monday, Thursday, Friday}, Neparna_Group = {Tuesday, Wednesday}.
- **Previous_Schedule**: The single most recent Stored_Schedule that existed immediately before the current Stored_Schedule was saved. At most one Previous_Schedule is retained at any time.
- **Revert**: The action of restoring the Stored_Schedule to the Previous_Schedule, which replaces the current Timetable and Group_Assignment with those of the Previous_Schedule.

## Requirements

### Requirement 1: Display the Daily Teaching Schedule

**User Story:** As the Teacher, I want to see my teaching schedule for a specific date, so that I know which classes I teach and at what times that day.

#### Acceptance Criteria

1. WHEN a Selected_Date that falls Monday through Friday is displayed in the Daily_View, THE App SHALL list each Period defined for that Weekday with the Period number, the Class_Code the Teacher teaches during that Period, and the Period start and end clock times for the applicable Shift.
2. WHEN the Daily_View displays a Selected_Date that falls Monday through Friday, THE App SHALL order the Periods in ascending Period number from first to last.
3. WHEN the Daily_View displays a Selected_Date that falls Monday through Friday, THE App SHALL display the Weekday and the Group (Parna_Group or Neparna_Group) to which the Selected_Date's Weekday is assigned in the current Group_Assignment.
4. WHEN the Daily_View displays a Selected_Date that falls Monday through Friday, THE App SHALL display a label indicating whether the applicable Shift is the morning Shift or the afternoon Shift.
5. WHERE a Period for the Selected_Date is a Pause, THE App SHALL display that Period with its Period number and clock times and a visible indication that no class is taught during that Period.
6. IF the Selected_Date is a Weekend_Day, THEN THE App SHALL display a message indicating that no classes are scheduled for that date and SHALL omit the Period list.
7. IF the Selected_Date falls Monday through Friday and no Period entry exists for that Weekday, THEN THE App SHALL display a message indicating that no classes are scheduled for that date.

### Requirement 2: Determine the Shift From the Week Type

**User Story:** As the Teacher, I want the app to determine whether each day is a morning or afternoon shift automatically, so that the correct clock times are shown without manual switching.

#### Acceptance Criteria

1. THE App SHALL compute the Week_Type of any date deterministically by counting the number of whole Monday-to-Sunday weeks between the Monday of that date's week and the Monday of the Anchor_Week, assigning Week_Type "A" when the count is even and Week_Type "B" when the count is odd.
2. THE App SHALL treat each week as beginning on Monday.
3. WHEN the App computes the Week_Type for a Selected_Date on any later occasion, THE App SHALL return the same Week_Type value as the initial computation for that date.
4. WHERE two dates fall in consecutive Monday-to-Sunday weeks, THE App SHALL assign those dates opposite Week_Type values.
5. IF the Week_Type of the Selected_Date is "A", THEN THE App SHALL assign the morning Shift to the Parna_Group and the afternoon Shift to the Neparna_Group.
6. IF the Week_Type of the Selected_Date is "B", THEN THE App SHALL assign the afternoon Shift to the Parna_Group and the morning Shift to the Neparna_Group.
7. WHEN the App determines the Shift for a Selected_Date, THE App SHALL select the morning or afternoon Shift based on the Group of the Selected_Date's Weekday and the Week_Type of the Selected_Date, and SHALL apply the Bell_Times of that Shift to the Periods.
8. THE App SHALL keep the Class_Code assigned to each Period of a Weekday identical across all Week_Type values, so that only the clock times differ between Week_Type "A" and Week_Type "B".

### Requirement 3: Navigate Between Days

**User Story:** As the Teacher, I want to move to other days, so that I can check another day's classes and times.

#### Acceptance Criteria

1. WHEN the App is opened, THE App SHALL set the Selected_Date to the current calendar date.
2. WHEN the Teacher requests the next day, THE App SHALL advance the Selected_Date by one calendar day and update the Daily_View, including the Periods, Class_Codes, and Shift clock times for the new Selected_Date.
3. WHEN the Teacher requests the previous day, THE App SHALL move the Selected_Date back by one calendar day and update the Daily_View, including the Periods, Class_Codes, and Shift clock times for the new Selected_Date.
4. WHEN the Selected_Date changes, THE App SHALL update the displayed Weekday, Group, and Shift label to match the new Selected_Date.
5. IF navigation sets the Selected_Date to a Weekend_Day, THEN THE App SHALL display the Daily_View for that date with a message indicating that no classes are scheduled and SHALL retain the Selected_Date.

### Requirement 4: Browse a Calendar to Select a Date

**User Story:** As the Teacher, I want to pick a date from a calendar, so that I can jump directly to any day's schedule.

#### Acceptance Criteria

1. WHEN the Teacher opens the Calendar_View, THE App SHALL display a calendar from which a date can be selected, defaulting to the month containing the current Selected_Date.
2. WHEN the Teacher selects a date in the Calendar_View, THE App SHALL set the Selected_Date to the chosen date and display its Daily_View within 1 second.
3. WHILE the Calendar_View is open, THE App SHALL visually distinguish the currently Selected_Date using a distinct visual marker.
4. WHEN the Teacher selects a date, THE App SHALL display the Weekday, Group, and Shift label that apply to the selected date.
5. IF the Teacher selects a Weekend_Day, THEN THE App SHALL display the Daily_View for that date with a message indicating that no classes are scheduled and SHALL retain the Selected_Date.

### Requirement 5: Store the Timetable and Bell Times Locally

**User Story:** As the Teacher, I want the app to hold my complete timetable and bell times on the device, so that schedules display correctly offline.

#### Acceptance Criteria

1. THE App SHALL store one weekly Timetable defining, for each of Monday through Friday, the ordered Periods and the Class_Code (or Pause) for each Period, independent of Week_Type, as part of the Stored_Schedule.
2. THE App SHALL use the following weekly Timetable data as the Default_Schedule Timetable that seeds the Stored_Schedule:
   - Monday (Parna_Group): Period 6 = 5/4, Period 7 = 5/2.
   - Tuesday (Neparna_Group): Period 2 = 6/1, Period 3 = 8/5, Period 4 = 6/3, Period 5 = 6/5, Period 6 = 7/1, Period 7 = 7/5.
   - Wednesday (Neparna_Group): Period 1 = 7/3, Period 2 = 5/3, Period 3 = 8/1, Period 4 = Pause, Period 5 = 6/7, Period 6 = 8/7.
   - Thursday (Parna_Group): Period 1 = 6/4, Period 2 = 6/2, Period 3 = 6/6, Period 4 = 8/4, Period 5 = Pause, Period 6 = 7/4, Period 7 = 7/2.
   - Friday (Parna_Group): Period 4 = 7/6, Period 5 = 8/2, Period 6 = 8/6, Period 7 = 7/7.
   - Saturday and Sunday: no Periods.
3. THE App SHALL store the following fixed Bell_Times for the morning Shift: Period 1 = 08:00–08:45, Period 2 = 08:50–09:35, Period 3 = 09:55–10:40, Period 4 = 10:45–11:30, Period 5 = 11:35–12:20, Period 6 = 12:25–13:10, Period 7 = 13:15–14:00.
4. THE App SHALL store the following fixed Bell_Times for the afternoon Shift: Period 1 = 14:00–14:45, Period 2 = 14:50–15:35, Period 3 = 15:55–16:40, Period 4 = 16:45–17:30, Period 5 = 17:35–18:20, Period 6 = 18:25–19:10, Period 7 = 19:15–20:00.
5. WHEN the device has no active network connection, THE App SHALL display the requested schedule for any date using locally stored Stored_Schedule and Bell_Times data.
6. WHEN the App is restarted after being fully closed, THE App SHALL display the same Stored_Schedule and Bell_Times data that was present before the restart, with no loss or alteration.
7. IF the Stored_Schedule or Bell_Times data cannot be read at startup, THEN THE App SHALL display an indication that the timetable data is unavailable and SHALL retain any previously stored data without overwriting it.
8. THE App SHALL treat the Bell_Times of the morning Shift and the afternoon Shift as fixed values that the Teacher cannot edit.

### Requirement 6: Installation on a Personal Device

**User Story:** As the Teacher, I want to install the app on my Samsung Android phone, so that I can use it as a standalone offline app.

#### Acceptance Criteria

1. THE App SHALL run as a standalone installable Android application on a Samsung Android device without requiring a network connection.
2. WHEN the App is launched, THE App SHALL display the Daily_View for the current system date within 3 seconds without requiring an account or sign-in.
3. IF the current system date is a Weekend_Day, THEN THE App SHALL display the Daily_View for that date with a message indicating that no classes are scheduled.
4. IF the App fails to determine the current system date on launch, THEN THE App SHALL display an error indication that the current date could not be determined and SHALL default the Daily_View to the most recent weekday.


### Requirement 7: Edit and Persist the Schedule Owner

**User Story:** As the Teacher, I want to set the owner name of the schedule, so that the App shows whose schedule it is and can be used by different teachers on the same App build.

#### Acceptance Criteria

1. WHEN the App is run for the first time with no stored Owner value, THE App SHALL use the Owner value "Olivera" and SHALL store that value in device-local storage.
2. WHEN the Teacher opens the Settings_View, THE App SHALL display the current Owner value in an editable text field.
3. WHEN the Teacher saves an Owner value in the Settings_View, THE App SHALL trim leading and trailing whitespace from the entered value before validation and storage.
4. WHEN the Teacher saves an Owner value whose trimmed length is between 1 and 50 characters inclusive, THE App SHALL store the trimmed Owner value in device-local storage, replacing the previous Owner value, and SHALL display the stored value in the editable text field within 1 second of the save action.
5. WHEN the App is restarted after being fully closed, THE App SHALL display the same Owner value that was stored before the restart.
6. THE App SHALL display the current Owner value in at least the Settings_View.
7. IF the Teacher saves an Owner value whose trimmed length is 0 characters, THEN THE App SHALL retain the previously stored Owner value unchanged and SHALL display an indication that the Owner name cannot be empty.
8. IF the Teacher saves an Owner value whose trimmed length exceeds 50 characters, THEN THE App SHALL retain the previously stored Owner value unchanged and SHALL display an indication that the Owner name must be 50 characters or fewer.
9. THE App SHALL read and write the Owner value using device-local storage only, without requiring a network connection.

### Requirement 8: Edit and Persist the Timetable and Group Assignment

**User Story:** As the Teacher, I want to edit my class codes and which weekdays are Parna or Neparna, so that the App shows my own schedule instead of a fixed built-in one.

#### Acceptance Criteria

1. WHEN the App is started and no Stored_Schedule exists on the device, THE App SHALL initialize the Stored_Schedule from the Default_Schedule and SHALL store the resulting Stored_Schedule on the device before displaying any schedule.
2. WHEN the Teacher opens the Schedule_Editor, THE App SHALL display, for each weekday Monday through Friday, exactly seven Periods numbered 1 through 7, each showing its current Class_Code or Pause from the Stored_Schedule in an editable form.
3. WHEN the Teacher opens the Schedule_Editor, THE App SHALL display the current Group_Assignment of each weekday Monday through Friday as either the Parna_Group labeled "ПАРНА" or the Neparna_Group labeled "НЕПАРНА" in an editable form.
4. WHEN the Teacher sets the Class_Code of a Period in the Schedule_Editor to a non-empty value of 1 to 20 characters after trimming leading and trailing whitespace, THE App SHALL update that Period entry in the working copy of the schedule to the trimmed Class_Code value.
5. WHEN the Teacher clears a Period in the Schedule_Editor or leaves its Class_Code empty or containing only whitespace, THE App SHALL set that Period entry in the working copy of the schedule to a Pause.
6. WHEN the Teacher changes the Group_Assignment of a weekday in the Schedule_Editor, THE App SHALL update the Group_Assignment of that weekday in the working copy of the schedule to the selected Parna_Group or Neparna_Group value.
7. WHILE the Teacher is editing in the Schedule_Editor and has not saved, THE App SHALL leave the Stored_Schedule on the device unchanged.
8. WHEN the Teacher saves the edited schedule in the Schedule_Editor and the write succeeds, THE App SHALL replace the current Stored_Schedule on the device with the edited Timetable and Group_Assignment from the working copy, including weekdays whose Periods are all Pause.
9. IF the Teacher saves the edited schedule in the Schedule_Editor and the write to device-local storage fails, THEN THE App SHALL retain the previous Stored_Schedule unchanged and SHALL display an error indication that the save did not succeed.
10. WHEN the App is restarted after being fully closed and a valid Stored_Schedule exists, THE App SHALL display the same Class_Codes, Pauses, and Group_Assignment that were stored before the restart, with no loss or alteration.
11. IF the App is started and a Stored_Schedule exists but cannot be read or parsed, THEN THE App SHALL reinitialize the Stored_Schedule from the Default_Schedule and SHALL display an error indication that the previous schedule could not be loaded.
12. WHEN the Daily_View displays a Selected_Date after the Stored_Schedule has been edited and saved, THE App SHALL display the Class_Codes and Group_Assignment from the current Stored_Schedule.
13. THE App SHALL keep the Bell_Times, the Week_Type computation, and the shift-alternation logic unchanged when the Stored_Schedule is edited.
14. WHERE the device has no network connection, THE App SHALL read and write the Stored_Schedule using device-local storage only.

### Requirement 9: Revert to the Previous Saved Schedule

**User Story:** As the Teacher, I want to undo my last saved schedule change, so that I can restore my previous schedule if I make a mistake.

#### Acceptance Criteria

1. WHEN the Teacher saves an edited schedule as the current Stored_Schedule, THE App SHALL retain the Timetable and Group_Assignment that were current immediately before the save as the Previous_Schedule.
2. THE App SHALL retain at most one Previous_Schedule at any time, replacing any earlier Previous_Schedule when a new save occurs.
3. WHEN the Teacher performs a Revert on the Schedule_Editor and a Previous_Schedule exists, THE App SHALL replace the current Stored_Schedule's Timetable and Group_Assignment with those of the Previous_Schedule and SHALL store the result as the current Stored_Schedule without creating a new Previous_Schedule.
4. WHEN a Revert completes, THE App SHALL discard the Previous_Schedule so that no Previous_Schedule is available until the next save, causing a second consecutive Revert without an intervening save to have no schedule to restore.
5. WHEN the Teacher performs a Revert, THE App SHALL leave the Owner value unchanged.
6. IF the Teacher performs a Revert when no Previous_Schedule exists, THEN THE App SHALL leave the current Stored_Schedule unchanged and SHALL display an indication that no previous schedule is available.
7. WHEN the App is restarted after being fully closed, THE App SHALL retain the Previous_Schedule that was stored before the restart, if one existed.
