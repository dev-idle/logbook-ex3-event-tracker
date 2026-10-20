# Event Tracker

Android app for COMP1786 Logbook Exercise 3. It builds on the Exercise 2 event
manager. Events are now saved in SQLite, so they are still there when the app
is opened again. Users can also edit and delete events and give each one a
status.

## Features

- Events saved in SQLite and loaded again each time the list is shown.
- Tap a ticket to edit its event in the same form used to add one.
- Delete an event from the edit screen's More options menu, with a
  confirmation dialog and an Undo action.
- Select several events, from the list's More options menu or by long pressing
  a ticket, and delete them together.
- A status for each event: Upcoming, Completed or Cancelled, chosen from a
  dropdown.
- Upcoming events become Completed once they end.
- Filter chips [1] that show only the events with one status. The chosen
  filter is kept between launches.
- Snackbars [2] that confirm when an event is added, updated or deleted.
- From Exercise 2: tickets in start order under month headings, the date
  picker and time lists, light and dark themes from seed #2E6FD8 [3], two
  columns in landscape, and screen reader text for each ticket.

## Validation

- Name: required, and not already used on the same date, ignoring case. When
  editing, the event's own name does not count as a duplicate.
- Date: required, DD/MM/YYYY, a real date, and from 01/01/1900 to 31/12/2100,
  the range of Android's date picker [4].
- Start and end time: required. The end must differ from the start.
- Location, organizer and type: required.
- Status: Completed only once the event has ended, and Upcoming only before
  then. Cancelled is always allowed.

Error messages follow the GOV.UK Design System guidance [5].

## Event status

- New events start as Upcoming.
- Before the list loads, one UPDATE marks Upcoming events that have ended as
  Completed. It uses the same time as the tickets, so a ticket never shows an
  ended event as upcoming.
- Each ticket shows its status after the type, such as "Upcoming in 4 days" or
  "Completed 5 days ago". Only "Cancelled" is red, and a cancelled ticket is
  grayed out with its name struck through. It is the only status that does not
  follow from the time.

## Edit and delete

- Leaving the edit form asks first only if a value has changed.
- Undo saves deleted events again under the same IDs.
- While selecting, tapping a ticket selects it, Select all selects every event
  the filter shows, and Back or the close button stops selecting.
- If the filter hides an event that was just saved, the list shows every event
  again and scrolls to it.

## Database

The `Event` table in `events.db`:

| Column      | Type                              | Example      |
| ----------- | --------------------------------- | ------------ |
| `ID`        | INTEGER PRIMARY KEY AUTOINCREMENT | 7            |
| `Name`      | TEXT NOT NULL                     | Alumni talk  |
| `Date`      | TEXT NOT NULL                     | 2026-11-05   |
| `StartTime` | TEXT NOT NULL                     | 19:00        |
| `EndTime`   | TEXT NOT NULL                     | 21:30        |
| `Location`  | TEXT NOT NULL                     | Queen Anne   |
| `Organizer` | TEXT NOT NULL                     | Careers Team |
| `Type`      | TEXT NOT NULL                     | SOCIAL       |
| `Status`    | TEXT NOT NULL                     | UPCOMING     |

- Dates are stored as YYYY-MM-DD and times as HH:MM, so text order is time
  order and `ORDER BY Date, StartTime` lists events in start order.
- Version 2 added the Status column. Upgrading from version 1 keeps every saved
  event. Those events start as Upcoming, and the ones that have ended become
  Completed when the list loads.
- Every statement binds its values, so text such as a quote in a name cannot
  break or change a query.
- Deleting several events, and undoing it, runs in one transaction, so either
  every event changes or none does.
- Database calls run on the main thread, as in the module brief, because the
  table stays small. A larger app would run them on a background thread.

## Module guide

The app follows the module's Android Studio brief. Exercise 2 already used its
explicit intents with a Serializable event, an ActivityResultLauncher and a
DatePickerDialog in a DialogFragment [6]. RecyclerView [7] replaces ListView,
and Material exposed dropdown menus [8] replace Spinners.

The database follows the brief's SQLite section: a SQLiteOpenHelper subclass,
SQLiteStatement bindings for inserts and updates, and rawQuery for reads [9].
It differs from the brief in three places:

- UPDATE and DELETE use `executeUpdateDelete`, where the brief's update calls
  `executeInsert` [10].
- Values are always bound, instead of going through the brief's `Execute`
  method, which runs any SQL string with `execSQL`.
- Cursors and statements are closed with try-with-resources.

The chosen filter is saved with SharedPreferences, as in the brief [11].

## Build and run

- Android Studio with its bundled JDK, AGP 9.4.1 and Gradle 9.6.0.
- minSdk 33, targetSdk 37, Java 11.
- Tested on a Pixel 7 Pro emulator running API 33.

Open the project in Android Studio and run the `app` configuration, or build
from the command line:

```
./gradlew assembleDebug
```

## Project structure

| File                      | Purpose                                         |
| ------------------------- | ----------------------------------------------- |
| `MainActivity.java`       | Event list, filter, selection and messages      |
| `AddEventActivity.java`   | Add and edit form, delete menu and errors       |
| `EventDatabase.java`      | SQLite table, queries and the status update     |
| `EventAdapter.java`       | Month headings, ticket states and statuses      |
| `TicketDrawable.java`     | Ticket shape with the date stub and perforation |
| `Event.java`              | Event model                                     |
| `EventType.java`          | Event types and their label colors              |
| `EventStatus.java`        | Event statuses and their filter texts           |
| `EventValidator.java`     | Validation, status rules and duplicate check    |
| `EventDates.java`         | Date parsing and formatting                     |
| `EventTimes.java`         | Time slots, lengths and formatting              |
| `DropdownField.java`      | Menu field that opens on the chosen item        |
| `MoreMenu.java`           | One-item menu only as wide as its item          |
| `DatePickerFragment.java` | Date picker dialog hosted in a DialogFragment   |

## Sources

1. Material Design 3. Chips. https://m3.material.io/components/chips/overview
2. Material Design 3. Snackbar.
   https://m3.material.io/components/snackbar/overview
3. Material Theme Builder.
   https://material-foundation.github.io/material-theme-builder/
4. Android Open Source Project. DatePickerCalendarDelegate.java,
   DEFAULT_START_YEAR and DEFAULT_END_YEAR.
   https://android.googlesource.com/platform/frameworks/base/+/refs/heads/main/core/java/android/widget/DatePickerCalendarDelegate.java
5. GOV.UK Design System. Error message.
   https://design-system.service.gov.uk/components/error-message/
6. Android Developers. Add pickers to your app.
   https://developer.android.com/develop/ui/views/components/pickers
7. Android Developers. Create dynamic lists with RecyclerView.
   https://developer.android.com/develop/ui/views/layout/recyclerview
8. Material Design 3. Menus. https://m3.material.io/components/menus/overview
9. Android Developers. Save data using SQLite.
   https://developer.android.com/training/data-storage/sqlite
10. Android Developers. SQLiteStatement.
    https://developer.android.com/reference/android/database/sqlite/SQLiteStatement
11. Android Developers. Save simple data with SharedPreferences.
    https://developer.android.com/training/data-storage/shared-preferences
