package com.comp1786.logbook.eventtracker;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves events in SQLite, as the module brief's database helper does.
 */
public class EventDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "events.db";
    private static final int DATABASE_VERSION = 2;

    // Dates are stored as YYYY-MM-DD and times as HH:MM, so text order is time order.
    private static final String CREATE_TABLE = "CREATE TABLE Event ("
            + "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "Name TEXT NOT NULL, "
            + "Date TEXT NOT NULL, "
            + "StartTime TEXT NOT NULL, "
            + "EndTime TEXT NOT NULL, "
            + "Location TEXT NOT NULL, "
            + "Organizer TEXT NOT NULL, "
            + "Type TEXT NOT NULL, "
            + "Status TEXT NOT NULL)";
    private static final String INSERT = "INSERT INTO Event "
            + "(Name, Date, StartTime, EndTime, Location, Organizer, Type, Status) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE = "UPDATE Event SET Name = ?, Date = ?, StartTime = ?, "
            + "EndTime = ?, Location = ?, Organizer = ?, Type = ?, Status = ? WHERE ID = ?";
    // The ID comes last, as in UPDATE, so the details bind the same way.
    private static final String RESTORE = "INSERT INTO Event "
            + "(Name, Date, StartTime, EndTime, Location, Organizer, Type, Status, ID) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String DELETE = "DELETE FROM Event WHERE ID = ?";
    private static final String SELECT = "SELECT ID, Name, Date, StartTime, EndTime, "
            + "Location, Organizer, Type, Status FROM Event ";
    private static final String START_ORDER = "ORDER BY Date, StartTime, ID";
    private static final String SELECT_ALL = SELECT + START_ORDER;
    private static final String SELECT_BY_STATUS = SELECT + "WHERE Status = ? " + START_ORDER;
    private static final String SELECT_BY_ID = SELECT + "WHERE ID = ?";
    // An event ends on the next day when its end time is not after its start time.
    private static final String COMPLETE_ENDED = "UPDATE Event SET Status = ? "
            + "WHERE Status = ? AND (CASE WHEN EndTime > StartTime THEN Date "
            + "ELSE date(Date, '+1 day') END) || ' ' || EndTime <= ?";

    public EventDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Version 2 adds the status, and events saved before it count as upcoming.
            db.execSQL("ALTER TABLE Event ADD COLUMN Status TEXT NOT NULL DEFAULT 'UPCOMING'");
        }
    }

    /** Saves a new event and returns its ID. */
    public long insertEvent(Event event) {
        try (SQLiteStatement statement = getWritableDatabase().compileStatement(INSERT)) {
            bindDetails(statement, event);
            return statement.executeInsert();
        }
    }

    /** Saves the changes to an event that is already saved. */
    public void updateEvent(Event event) {
        try (SQLiteStatement statement = getWritableDatabase().compileStatement(UPDATE)) {
            bindDetails(statement, event);
            statement.bindLong(9, event.getId());
            statement.executeUpdateDelete();
        }
    }

    /** Deletes the event with this ID. */
    public void deleteEvent(long id) {
        try (SQLiteStatement statement = getWritableDatabase().compileStatement(DELETE)) {
            statement.bindLong(1, id);
            statement.executeUpdateDelete();
        }
    }

    /** Deletes these events in one transaction, so either all of them go or none do. */
    public void deleteEvents(List<Event> events) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try (SQLiteStatement statement = db.compileStatement(DELETE)) {
            for (Event event : events) {
                statement.bindLong(1, event.getId());
                statement.executeUpdateDelete();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Saves deleted events again under their IDs, to undo a delete, in one transaction. */
    public void restoreEvents(List<Event> events) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try (SQLiteStatement statement = db.compileStatement(RESTORE)) {
            for (Event event : events) {
                bindDetails(statement, event);
                statement.bindLong(9, event.getId());
                statement.executeInsert();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Marks upcoming events that have ended by the given time as completed. */
    public void completeEndedEvents(LocalDateTime now) {
        try (SQLiteStatement statement =
                getWritableDatabase().compileStatement(COMPLETE_ENDED)) {
            statement.bindString(1, EventStatus.COMPLETED.name());
            statement.bindString(2, EventStatus.UPCOMING.name());
            statement.bindString(3,
                    now.toLocalDate() + " " + EventTimes.format(now.toLocalTime()));
            statement.executeUpdateDelete();
        }
    }

    /** Returns every saved event in start order. */
    public List<Event> getAllEvents() {
        return getEvents(null);
    }

    /** Returns the saved events with this status, or all of them if it is null, in start order. */
    public List<Event> getEvents(@Nullable EventStatus status) {
        List<Event> events = new ArrayList<>();
        try (Cursor cursor = status == null
                ? getReadableDatabase().rawQuery(SELECT_ALL, null)
                : getReadableDatabase().rawQuery(SELECT_BY_STATUS, new String[] {status.name()})) {
            while (cursor.moveToNext()) {
                events.add(readEvent(cursor));
            }
        }
        return events;
    }

    /** Returns the saved event with this ID, or null if there is none. */
    @Nullable
    public Event getEvent(long id) {
        try (Cursor cursor = getReadableDatabase()
                .rawQuery(SELECT_BY_ID, new String[] {String.valueOf(id)})) {
            return cursor.moveToFirst() ? readEvent(cursor) : null;
        }
    }

    /** Returns how many events are saved. */
    public long countEvents() {
        return DatabaseUtils.queryNumEntries(getReadableDatabase(), "Event");
    }

    /** Reads the event in the cursor's current row, whose columns are in the order of SELECT. */
    private static Event readEvent(Cursor cursor) {
        return new Event(cursor.getLong(0), cursor.getString(1),
                LocalDate.parse(cursor.getString(2)), EventTimes.parse(cursor.getString(3)),
                EventTimes.parse(cursor.getString(4)), cursor.getString(5), cursor.getString(6),
                EventType.valueOf(cursor.getString(7)), EventStatus.valueOf(cursor.getString(8)));
    }

    /** Binds the event's details to parameters 1 to 8, in the order of the Event columns. */
    private static void bindDetails(SQLiteStatement statement, Event event) {
        statement.bindString(1, event.getName());
        statement.bindString(2, event.getDate().toString());
        statement.bindString(3, EventTimes.format(event.getStartTime()));
        statement.bindString(4, EventTimes.format(event.getEndTime()));
        statement.bindString(5, event.getLocation());
        statement.bindString(6, event.getOrganizer());
        statement.bindString(7, event.getType().name());
        statement.bindString(8, event.getStatus().name());
    }
}
