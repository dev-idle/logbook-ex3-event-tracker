package com.comp1786.logbook.eventtracker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows the events as tickets with all their details, under a heading for each month.
 */
public class EventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MONTH = 0;
    private static final int TYPE_EVENT = 1;

    private List<Row> rows = new ArrayList<>();

    /** Shows the events, which must be in start order, as they stand at the given time. */
    public void setEvents(List<Event> events, LocalDateTime now) {
        List<Row> newRows = buildRows(events, now);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new RowDiff(rows, newRows));
        rows = newRows;
        diff.dispatchUpdatesTo(this);
    }

    /** Returns the position of the event with this ID, or NO_POSITION if it is not shown. */
    public int positionOf(long id) {
        for (int i = 0; i < rows.size(); i++) {
            Event event = rows.get(i).event;
            if (event != null && event.getId() == id) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    public boolean isMonthHeading(int position) {
        return rows.get(position).event == null;
    }

    @Override
    public int getItemViewType(int position) {
        return isMonthHeading(position) ? TYPE_MONTH : TYPE_EVENT;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_MONTH) {
            return new MonthViewHolder(inflater.inflate(R.layout.item_month, parent, false));
        }
        return new EventViewHolder(inflater.inflate(R.layout.item_event, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (row.event == null) {
            ((MonthViewHolder) holder).monthText.setText(EventDates.formatMonth(row.month));
        } else {
            bindEvent((EventViewHolder) holder, row);
        }
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    private static void bindEvent(EventViewHolder holder, Row row) {
        Context context = holder.itemView.getContext();
        Event event = row.event;
        LocalDate date = event.getDate();
        EventType type = event.getType();
        TicketState state = row.state;

        holder.ticket.setStubColor(MaterialColors.getColor(holder.itemView, state.stubColorAttr));
        int stubTextColor = MaterialColors.getColor(holder.itemView, state.stubTextColorAttr);
        holder.monthText.setTextColor(stubTextColor);
        holder.dayText.setTextColor(stubTextColor);
        holder.dayOfWeekText.setTextColor(stubTextColor);
        holder.statusText.setTextColor(
                MaterialColors.getColor(holder.itemView, state.statusColorAttr));
        holder.statusText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                state == TicketState.LIVE ? R.drawable.ic_live : 0, 0, 0, 0);

        holder.dateStub.setContentDescription(EventDates.formatLong(date));
        holder.monthText.setText(EventDates.formatShortMonth(date));
        holder.dayText.setText(EventDates.formatDayOfMonth(date));
        holder.dayOfWeekText.setText(EventDates.formatShortDayOfWeek(date));
        holder.nameText.setText(event.getName());
        holder.typeLabel.setText(type.getLabelRes());
        holder.statusText.setText(statusText(context, row));
        bindTimes(holder, event);
        holder.locationText.setText(event.getLocation());
        holder.organizerText.setText(event.getOrganizer());
        bindCancelled(holder, type, state == TicketState.CANCELLED);
    }

    /** Grays out a cancelled ticket and strikes through its name, or shows it as usual. */
    private static void bindCancelled(EventViewHolder holder, EventType type, boolean cancelled) {
        Context context = holder.itemView.getContext();
        int textColor = MaterialColors.getColor(holder.itemView, cancelled
                ? com.google.android.material.R.attr.colorOnSurfaceVariant
                : com.google.android.material.R.attr.colorOnSurface);
        holder.nameText.setTextColor(textColor);
        holder.startTimeText.setTextColor(textColor);
        holder.endTimeText.setTextColor(textColor);
        int flags = holder.nameText.getPaintFlags();
        holder.nameText.setPaintFlags(cancelled
                ? flags | Paint.STRIKE_THRU_TEXT_FLAG : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        holder.typeLabel.setBackgroundTintList(ColorStateList.valueOf(cancelled
                ? MaterialColors.getColor(holder.itemView,
                        com.google.android.material.R.attr.colorSurfaceContainerHighest)
                : context.getColor(type.getContainerColorRes())));
    }

    /** Shows the start, end and length like a boarding pass, read out as one sentence. */
    private static void bindTimes(EventViewHolder holder, Event event) {
        Context context = holder.itemView.getContext();
        String start = EventTimes.format(event.getStartTime());
        String end = EventTimes.format(event.getEndTime());
        String length = EventTimes.formatLength(
                context.getResources(), event.getStartTime(), event.getEndTime());
        holder.startTimeText.setText(start);
        holder.endTimeText.setText(end);
        holder.lengthText.setText(length);
        holder.nextDayText.setVisibility(event.endsNextDay() ? View.VISIBLE : View.GONE);
        holder.timeRow.setContentDescription(context.getString(event.endsNextDay()
                ? R.string.description_time_range_next_day : R.string.description_time_range,
                start, end, length));
    }

    /** Describes the status with when the event is, such as "Upcoming in 6 days". */
    private static String statusText(Context context, Row row) {
        if (row.state == TicketState.LIVE) {
            return context.getString(R.string.status_live);
        } else if (row.state == TicketState.CANCELLED) {
            return context.getString(R.string.status_cancelled);
        }
        long days = row.daysAway;
        LocalDate date = countdownDate(row.event, row.state);
        LocalDate today = date.minusDays(days);
        if (days == 0) {
            return context.getString(row.state == TicketState.PAST
                    ? R.string.status_completed_today : R.string.status_upcoming_today);
        } else if (days == 1) {
            return context.getString(R.string.status_upcoming_tomorrow);
        } else if (days == -1) {
            return context.getString(R.string.status_completed_yesterday);
        }
        Period period = days > 0 ? Period.between(today, date) : Period.between(date, today);
        Resources res = context.getResources();
        String duration;
        if (period.getYears() > 0) {
            duration = res.getQuantityString(
                    R.plurals.duration_years, period.getYears(), period.getYears());
        } else if (period.getMonths() > 0) {
            duration = res.getQuantityString(
                    R.plurals.duration_months, period.getMonths(), period.getMonths());
        } else {
            duration = res.getQuantityString(
                    R.plurals.duration_days, period.getDays(), period.getDays());
        }
        return context.getString(days > 0
                ? R.string.status_upcoming_in : R.string.status_completed_ago, duration);
    }

    /** Adds month headings and works out the state of each event at the given time. */
    private static List<Row> buildRows(List<Event> events, LocalDateTime now) {
        // Events happening now stand out; when none is, the next ones to start do.
        boolean anyLive = false;
        LocalDateTime nextStart = null;
        for (Event event : events) {
            TicketState state = stateAt(event, now);
            if (state == TicketState.LIVE) {
                anyLive = true;
            } else if (state == TicketState.UPCOMING && nextStart == null) {
                nextStart = event.getStart();
            }
        }
        List<Row> rows = new ArrayList<>();
        YearMonth month = null;
        for (Event event : events) {
            YearMonth eventMonth = YearMonth.from(event.getDate());
            if (!eventMonth.equals(month)) {
                month = eventMonth;
                rows.add(new Row(month, null, null, 0));
            }
            TicketState state = stateAt(event, now);
            if (state == TicketState.UPCOMING && !anyLive && event.getStart().equals(nextStart)) {
                state = TicketState.NEXT;
            }
            long daysAway =
                    ChronoUnit.DAYS.between(now.toLocalDate(), countdownDate(event, state));
            rows.add(new Row(month, event, state, daysAway));
        }
        return rows;
    }

    /** Returns the day a countdown counts to: the end for past events, otherwise the start. */
    private static LocalDate countdownDate(Event event, TicketState state) {
        return state == TicketState.PAST ? event.getEnd().toLocalDate() : event.getDate();
    }

    private static TicketState stateAt(Event event, LocalDateTime now) {
        if (event.getStatus() == EventStatus.CANCELLED) {
            return TicketState.CANCELLED;
        } else if (!event.getEnd().isAfter(now)) {
            return TicketState.PAST;
        }
        return event.getStart().isAfter(now) ? TicketState.UPCOMING : TicketState.LIVE;
    }

    /** Where an event is in time, or that it was cancelled, with the ticket colors that show it. */
    private enum TicketState {

        LIVE(androidx.appcompat.R.attr.colorPrimary,
                com.google.android.material.R.attr.colorOnPrimary,
                androidx.appcompat.R.attr.colorPrimary),
        NEXT(androidx.appcompat.R.attr.colorPrimary,
                com.google.android.material.R.attr.colorOnPrimary,
                androidx.appcompat.R.attr.colorPrimary),
        UPCOMING(com.google.android.material.R.attr.colorPrimaryContainer,
                com.google.android.material.R.attr.colorOnPrimaryContainer,
                com.google.android.material.R.attr.colorOnSurfaceVariant),
        PAST(com.google.android.material.R.attr.colorSurfaceContainerHighest,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                com.google.android.material.R.attr.colorOnSurfaceVariant),
        // The only status in color, as the one that does not follow from the time.
        CANCELLED(com.google.android.material.R.attr.colorSurfaceContainerHighest,
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                androidx.appcompat.R.attr.colorError);

        @AttrRes
        private final int stubColorAttr;
        @AttrRes
        private final int stubTextColorAttr;
        @AttrRes
        private final int statusColorAttr;

        TicketState(@AttrRes int stubColorAttr, @AttrRes int stubTextColorAttr,
                @AttrRes int statusColorAttr) {
            this.stubColorAttr = stubColorAttr;
            this.stubTextColorAttr = stubTextColorAttr;
            this.statusColorAttr = statusColorAttr;
        }
    }

    /** A month heading when event is null, otherwise an event in that month. */
    private static final class Row {

        private final YearMonth month;
        @Nullable
        private final Event event;
        @Nullable
        private final TicketState state;
        private final long daysAway;

        Row(YearMonth month, @Nullable Event event, @Nullable TicketState state, long daysAway) {
            this.month = month;
            this.event = event;
            this.state = state;
            this.daysAway = daysAway;
        }
    }

    /** Finds the rows that an update adds, removes or changes, so only those are redrawn. */
    private static final class RowDiff extends DiffUtil.Callback {

        private final List<Row> oldRows;
        private final List<Row> newRows;

        RowDiff(List<Row> oldRows, List<Row> newRows) {
            this.oldRows = oldRows;
            this.newRows = newRows;
        }

        @Override
        public int getOldListSize() {
            return oldRows.size();
        }

        @Override
        public int getNewListSize() {
            return newRows.size();
        }

        @Override
        public boolean areItemsTheSame(int oldPosition, int newPosition) {
            Row oldRow = oldRows.get(oldPosition);
            Row newRow = newRows.get(newPosition);
            if (oldRow.event == null || newRow.event == null) {
                return oldRow.event == newRow.event && oldRow.month.equals(newRow.month);
            }
            // Events are read from the database as new objects, so they are matched by ID.
            return oldRow.event.getId() == newRow.event.getId();
        }

        @Override
        public boolean areContentsTheSame(int oldPosition, int newPosition) {
            Row oldRow = oldRows.get(oldPosition);
            Row newRow = newRows.get(newPosition);
            // States and countdowns change as time passes and as earlier events are added.
            return oldRow.state == newRow.state && oldRow.daysAway == newRow.daysAway;
        }
    }

    /** The view of one month heading. */
    public static final class MonthViewHolder extends RecyclerView.ViewHolder {

        private final TextView monthText;

        public MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            monthText = itemView.findViewById(R.id.month_heading);
        }
    }

    /** The views of one event ticket. */
    public static final class EventViewHolder extends RecyclerView.ViewHolder {

        private final TicketDrawable ticket;
        private final View dateStub;
        private final TextView monthText;
        private final TextView dayText;
        private final TextView dayOfWeekText;
        private final TextView nameText;
        private final TextView typeLabel;
        private final TextView statusText;
        private final View timeRow;
        private final TextView startTimeText;
        private final TextView endTimeText;
        private final TextView lengthText;
        private final TextView nextDayText;
        private final TextView locationText;
        private final TextView organizerText;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            ticket = new TicketDrawable(itemView.getContext());
            itemView.setBackground(ticket);
            dateStub = itemView.findViewById(R.id.date_stub);
            monthText = itemView.findViewById(R.id.event_month);
            dayText = itemView.findViewById(R.id.event_day);
            dayOfWeekText = itemView.findViewById(R.id.event_day_of_week);
            nameText = itemView.findViewById(R.id.event_name);
            typeLabel = itemView.findViewById(R.id.type_label);
            statusText = itemView.findViewById(R.id.event_status);
            timeRow = itemView.findViewById(R.id.time_row);
            startTimeText = itemView.findViewById(R.id.start_time);
            endTimeText = itemView.findViewById(R.id.end_time);
            lengthText = itemView.findViewById(R.id.event_length);
            nextDayText = itemView.findViewById(R.id.next_day);
            locationText = itemView.findViewById(R.id.event_location);
            organizerText = itemView.findViewById(R.id.event_organizer);
        }
    }
}
