# Event Tracker

Android app for COMP1786 Logbook Exercise 3. It builds on the Exercise 2 event
manager. Events are now saved in SQLite, so they are still there when the app
is opened again. Users can also edit and delete events and give each one a
status.

## Features

- Events saved in SQLite and loaded again each time the list is shown.
- Tap an event card to see the event in full, as in Exercise 2. Its More options
  menu edits the event in the same form used to add one, or deletes it with a
  confirmation dialog and an Undo action.
- Select several events, from the list's More options menu or by long pressing
  a card, and delete them together.
- A status for each event: Upcoming, Completed or Cancelled, chosen from a
  dropdown.
- Upcoming events become Completed once they end.
- Filter chips [1] that show only the events with one status. The filter is
  not saved, so a new launch shows every saved event.
- Snackbars [2] that confirm when an event is added, updated or deleted.
- From Exercise 2: event cards in start order under month headings, the date
  picker and time lists, light and dark themes from seed #2E6FD8 [3], two
  columns in landscape, and screen reader text for each card.

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
  Completed. It uses the same time as the cards, so a card never shows an
  ended event as upcoming.
- Each card shows its status after the type, such as "Upcoming in 4 days" or
  "Completed 5 days ago". Only "Cancelled" is red, and a cancelled card is
  grayed out with its name struck through. It is the only status that does not
  follow from the time.

## Edit and delete

- Leaving the edit form asks first only if a value has changed. After saving,
  the details screen shows the changes.
- Undo saves deleted events again under the same IDs.
- While selecting, tapping a card selects it, Select all selects every event
  the filter shows, and Back or the close button stops selecting.
- If the filter hides an event that was just added, the list shows every event
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
  order and `ORDER BY Date, StartTime, ID` lists events in start order.
- Version 2 added the Status column. Upgrading from version 1 keeps every saved
  event. Those events start as Upcoming, and the ones that have ended become
  Completed when the list loads.
- Every statement binds its values, so text such as a quote in a name cannot
  break or change a query.
- Deleting several events, and undoing it, runs in one transaction, so either
  every event changes or none does.
- Database calls run on the main thread, as in the brief's SQLite example,
  because the table stays small. A larger app would run them on a background
  thread.

## Module brief

The app follows the module's Android Studio brief. Exercise 2 already used
its explicit intents with a Serializable event, an ActivityResultLauncher and a
DatePickerDialog in a DialogFragment [6]. `DatePickerFragment` is adapted from
the brief: it takes the date as a fragment argument, so Android can recreate
it, and returns the chosen date as a fragment result. RecyclerView [7] replaces
ListView, Material exposed dropdown menus [8] replace Spinners, and snackbars
replace Toasts so a delete can offer Undo.

The database follows the brief's SQLite section: a SQLiteOpenHelper subclass
[9], SQLiteStatement bindings for inserts and updates, and rawQuery for reads,
with `DatabaseUtils.queryNumEntries` for the event count. It differs from the
brief in three places:

- UPDATE and DELETE use `executeUpdateDelete` [10], where the brief's update
  calls `executeInsert`.
- Values are always bound, instead of going through the brief's `Execute`
  method, which runs any SQL string with `execSQL`.
- Cursors and statements are closed with try-with-resources.

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

| File                        | Purpose                                         |
| --------------------------- | ----------------------------------------------- |
| `MainActivity.java`         | Event list, filter, selection and messages      |
| `EventDetailsActivity.java` | One event in full, with Edit and Delete         |
| `AddEditEventActivity.java` | Add and edit form, time lists and errors        |
| `EventDatabase.java`        | SQLite table, queries and the status update     |
| `EventAdapter.java`         | Month headings, cards and selection             |
| `EventCardState.java`       | Card states, colors and status phrases          |
| `EventCardDrawable.java`    | Card shape with the date panel and divider      |
| `Event.java`                | Event model                                     |
| `EventType.java`            | Event types and their label colors              |
| `EventStatus.java`          | Event statuses and their filter texts           |
| `EventValidator.java`       | Validation, status rules and duplicate check    |
| `EventDates.java`           | Date parsing and formatting                     |
| `EventTimes.java`           | Time slots, lengths and formatting              |
| `DropdownField.java`        | Menu field that opens on the chosen item        |
| `MoreMenu.java`             | Compact menu as wide as its widest item         |
| `IconSnackbar.java`         | Messages with an icon before their text         |
| `DatePickerFragment.java`   | Date picker dialog hosted in a DialogFragment   |

Icons such as Add, Edit and Delete are from Material Symbols and Icons [11].

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
11. Google. Material Symbols and Icons. https://fonts.google.com/icons
