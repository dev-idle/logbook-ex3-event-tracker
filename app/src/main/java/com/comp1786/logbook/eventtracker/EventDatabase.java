package com.comp1786.logbook.eventtracker;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves events in SQLite, as the module guide's database helper does.
 */
public class EventDatabase extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "events.db";
    private static final int DATABASE_VERSION = 1;

    // Dates are stored as YYYY-MM-DD and times as HH:MM, so text order is time order.
    private static final String CREATE_TABLE = "CREATE TABLE Event ("
            + "ID INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "Name TEXT NOT NULL, "
            + "Date TEXT NOT NULL, "
            + "StartTime TEXT NOT NULL, "
            + "EndTime TEXT NOT NULL, "
            + "Location TEXT NOT NULL, "
            + "Organizer TEXT NOT NULL, "
            + "Type TEXT NOT NULL)";
    private static final String INSERT = "INSERT INTO Event "
            + "(Name, Date, StartTime, EndTime, Location, Organizer, Type) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_ALL = "SELECT ID, Name, Date, StartTime, EndTime, "
            + "Location, Organizer, Type FROM Event ORDER BY Date, StartTime, ID";

    public EventDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the first schema, so there is nothing to migrate yet.
    }

    /** Saves a new event and returns its ID. */
    public long insertEvent(Event event) {
        try (SQLiteStatement statement = getWritableDatabase().compileStatement(INSERT)) {
            bindDetails(statement, event);
            return statement.executeInsert();
        }
    }

    /** Returns every saved event in start order. */
    public List<Event> getAllEvents() {
        List<Event> events = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(SELECT_ALL, null)) {
            while (cursor.moveToNext()) {
                events.add(new Event(cursor.getLong(0), cursor.getString(1),
                        LocalDate.parse(cursor.getString(2)), EventTimes.parse(cursor.getString(3)),
                        EventTimes.parse(cursor.getString(4)), cursor.getString(5),
                        cursor.getString(6), EventType.valueOf(cursor.getString(7))));
            }
        }
        return events;
    }

    /** Binds the event's details to parameters 1 to 7, in the order of the Event columns. */
    private static void bindDetails(SQLiteStatement statement, Event event) {
        statement.bindString(1, event.getName());
        statement.bindString(2, event.getDate().toString());
        statement.bindString(3, EventTimes.format(event.getStartTime()));
        statement.bindString(4, EventTimes.format(event.getEndTime()));
        statement.bindString(5, event.getLocation());
        statement.bindString(6, event.getOrganizer());
        statement.bindString(7, event.getType().name());
    }
}
