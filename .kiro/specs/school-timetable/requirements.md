# Requirements Document

## Introduction

This document defines the requirements for a single-user, fully offline Android application that displays the personal teaching schedule of a primary school teacher (teacher name "Ostojic O."). The application targets a personal Samsung Android device (sideloaded/installed manually) and requires no account or sign-in.

The application displays, for any selected calendar date, the class/group codes the teacher teaches during each period, together with the correct clock (bell) times for that day.

The scheduling model has two independent parts:

1. **Fixed weekly class layout.** The classes taught on each weekday never change from week to week. The same class/group codes always fall on the same weekday. Weekdays are organized into two fixed groups:
   - **Parna group** days: Monday, Thursday, Friday.
   - **Neparna group** days: Tuesday, Wednesday.
2. **Weekly shift alternation.** What changes week to week is only the shift (morning vs afternoon). The Parna group and the Neparna group swap shifts each week. This is captured by a Week_Type (A or B) computed deterministically from the date.

For any given date, the application determines the weekday (which yields the fixed class list and the group), computes the Week_Type from the date (which yields whether that group is on the morning or afternoon shift that week), and displays each period with its class/group code and the correct clock times for the resulting shift.

## Glossary

- **App**: The Android application that displays the teaching schedule, installed on a single Samsung Android device.
- **Teacher**: The single user of the App, a primary school teacher identified as "Ostojic O.".
- **Timetable**: The complete fixed weekly layout of periods and class/group codes for all weekdays, independent of Week_Type.
- **Weekday**: A day of the week from Monday through Sunday. Monday through Friday may contain scheduled periods; Saturday and Sunday do not.
- **Group**: A fixed assignment of a weekday to one of two groups. The **Parna group** consists of Monday, Thursday, and Friday. The **Neparna group** consists of Tuesday and Wednesday. Group membership is fixed and never changes week to week.
- **Parna_Group**: The fixed group of weekdays Monday, Thursday, and Friday.
- **Neparna_Group**: The fixed group of weekdays Tuesday and Wednesday.
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

## Requirements

### Requirement 1: Display the Daily Teaching Schedule

**User Story:** As the Teacher, I want to see my teaching schedule for a specific date, so that I know which classes I teach and at what times that day.

#### Acceptance Criteria

1. WHEN a Selected_Date that falls Monday through Friday is displayed in the Daily_View, THE App SHALL list each Period defined for that Weekday with the Period number, the Class_Code the Teacher teaches during that Period, and the Period start and end clock times for the applicable Shift.
2. WHEN the Daily_View displays a Selected_Date that falls Monday through Friday, THE App SHALL order the Periods in ascending Period number from first to last.
3. WHEN the Daily_View displays a Selected_Date that falls Monday through Friday, THE App SHALL display the Weekday and the Group (Parna_Group or Neparna_Group) to which the Selected_Date belongs.
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

### Requirement 5: Store the Fixed Timetable and Bell Times Locally

**User Story:** As the Teacher, I want the app to hold my complete fixed timetable and bell times on the device, so that schedules display correctly offline.

#### Acceptance Criteria

1. THE App SHALL store one fixed weekly Timetable defining, for each of Monday through Friday, the ordered Periods and the Class_Code (or Pause) for each Period, independent of Week_Type.
2. THE App SHALL store the following fixed weekly Timetable data:
   - Monday (Parna_Group): Period 6 = 5/4, Period 7 = 5/2.
   - Tuesday (Neparna_Group): Period 2 = 6/1, Period 3 = 8/5, Period 4 = 6/3, Period 5 = 6/5, Period 6 = 7/1, Period 7 = 7/5.
   - Wednesday (Neparna_Group): Period 1 = 7/3, Period 2 = 5/3, Period 3 = 8/1, Period 4 = Pause, Period 5 = 6/7, Period 6 = 8/7.
   - Thursday (Parna_Group): Period 1 = 6/4, Period 2 = 6/2, Period 3 = 6/6, Period 4 = 8/4, Period 5 = Pause, Period 6 = 7/4, Period 7 = 7/2.
   - Friday (Parna_Group): Period 4 = 7/6, Period 5 = 8/2, Period 6 = 8/6, Period 7 = 7/7.
   - Saturday and Sunday: no Periods.
3. THE App SHALL store the following fixed Bell_Times for the morning Shift: Period 1 = 08:00–08:45, Period 2 = 08:50–09:35, Period 3 = 09:55–10:40, Period 4 = 10:45–11:30, Period 5 = 11:35–12:20, Period 6 = 12:25–13:10, Period 7 = 13:15–14:00.
4. THE App SHALL store the following fixed Bell_Times for the afternoon Shift: Period 1 = 14:00–14:45, Period 2 = 14:50–15:35, Period 3 = 15:55–16:40, Period 4 = 16:45–17:30, Period 5 = 17:35–18:20, Period 6 = 18:25–19:10, Period 7 = 19:15–20:00.
5. WHEN the device has no active network connection, THE App SHALL display the requested schedule for any date using locally stored Timetable and Bell_Times data.
6. WHEN the App is restarted after being fully closed, THE App SHALL display the same stored Timetable and Bell_Times data that was present before the restart, with no loss or alteration.
7. IF stored Timetable or Bell_Times data cannot be read at startup, THEN THE App SHALL display an indication that the timetable data is unavailable and SHALL retain any previously stored data without overwriting it.

### Requirement 6: Installation on a Personal Device

**User Story:** As the Teacher, I want to install the app on my Samsung Android phone, so that I can use it as a standalone offline app.

#### Acceptance Criteria

1. THE App SHALL run as a standalone installable Android application on a Samsung Android device without requiring a network connection.
2. WHEN the App is launched, THE App SHALL display the Daily_View for the current system date within 3 seconds without requiring an account or sign-in.
3. IF the current system date is a Weekend_Day, THEN THE App SHALL display the Daily_View for that date with a message indicating that no classes are scheduled.
4. IF the App fails to determine the current system date on launch, THEN THE App SHALL display an error indication that the current date could not be determined and SHALL default the Daily_View to the most recent weekday.
