package com.comp1786.logbook.eventtracker;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * An event entry, Serializable so activities can pass it in an Intent.
 */
public final class Event implements Serializable {

    /** The ID of an event that has not been saved yet. */
    public static final long NO_ID = 0;

    private static final long serialVersionUID = 1L;

    private final long id;
    private final String name;
    private final LocalDate date;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String location;
    private final String organizer;
    private final EventType type;
    private final EventStatus status;

    public Event(long id, String name, LocalDate date, LocalTime startTime, LocalTime endTime,
            String location, String organizer, EventType type, EventStatus status) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.organizer = organizer;
        this.type = type;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public LocalDateTime getStart() {
        return date.atTime(startTime);
    }

    /** Returns when the event ends, on the next day if it runs past midnight. */
    public LocalDateTime getEnd() {
        return EventTimes.endOn(date, startTime, endTime);
    }

    /** Returns true when the event runs past midnight and ends on the next day. */
    public boolean endsNextDay() {
        return EventTimes.endsNextDay(startTime, endTime);
    }

    public String getLocation() {
        return location;
    }

    public String getOrganizer() {
        return organizer;
    }

    public EventType getType() {
        return type;
    }

    public EventStatus getStatus() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Event)) {
            return false;
        }
        Event other = (Event) o;
        return id == other.id && name.equals(other.name) && date.equals(other.date)
                && startTime.equals(other.startTime) && endTime.equals(other.endTime)
                && location.equals(other.location) && organizer.equals(other.organizer)
                && type == other.type && status == other.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, date, startTime, endTime, location, organizer, type, status);
    }
}
