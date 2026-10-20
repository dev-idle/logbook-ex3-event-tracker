package com.comp1786.logbook.eventtracker;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.color.MaterialColors;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shows the events as cards with all their details, under a heading for each month.
 */
public class EventAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_MONTH = 0;
    private static final int TYPE_EVENT = 1;
    // Sent when only the selection changes, so the rest of each card is not bound again.
    private static final Object SELECTION_CHANGED = new Object();

    private final OnEventClickListener listener;
    private List<Row> rows = new ArrayList<>();
    private final Set<Long> selectedIds = new LinkedHashSet<>();
    private boolean selecting;

    public EventAdapter(OnEventClickListener listener) {
        this.listener = listener;
    }

    /** Shows the events, which must be in start order, as they stand at the given time. */
    public void setEvents(List<Event> events, LocalDateTime now) {
        List<Row> newRows = buildRows(events, now);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new RowDiff(rows, newRows));
        rows = newRows;
        // An event that is no longer shown cannot stay selected.
        Set<Long> shownIds = new HashSet<>();
        for (Event event : events) {
            shownIds.add(event.getId());
        }
        selectedIds.retainAll(shownIds);
        diff.dispatchUpdatesTo(this);
    }

    public boolean isSelecting() {
        return selecting;
    }

    /** Shows or hides the circles for selecting cards, and deselects all when hiding them. */
    public void setSelecting(boolean selecting) {
        this.selecting = selecting;
        if (!selecting) {
            selectedIds.clear();
        }
        notifyItemRangeChanged(0, rows.size(), SELECTION_CHANGED);
    }

    /** Selects the event, or deselects it if it is already selected. */
    public void toggleSelected(long id) {
        if (!selectedIds.remove(id)) {
            selectedIds.add(id);
        }
        notifyItemChanged(positionOf(id), SELECTION_CHANGED);
    }

    /** Selects every event shown. */
    public void selectAll() {
        for (Row row : rows) {
            if (row.event != null) {
                selectedIds.add(row.event.getId());
            }
        }
        notifyItemRangeChanged(0, rows.size(), SELECTION_CHANGED);
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyItemRangeChanged(0, rows.size(), SELECTION_CHANGED);
    }

    /** Returns true if every event shown is selected. */
    public boolean isAllSelected() {
        for (Row row : rows) {
            if (row.event != null && !selectedIds.contains(row.event.getId())) {
                return false;
            }
        }
        return !selectedIds.isEmpty();
    }

    /** Selects the shown events with these IDs, such as the ones selected before a rotation. */
    public void select(long[] ids) {
        for (long id : ids) {
            if (positionOf(id) != RecyclerView.NO_POSITION) {
                selectedIds.add(id);
            }
        }
        notifyItemRangeChanged(0, rows.size(), SELECTION_CHANGED);
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public long[] getSelectedIds() {
        long[] ids = new long[selectedIds.size()];
        int i = 0;
        for (long id : selectedIds) {
            ids[i++] = id;
        }
        return ids;
    }

    /** Returns the selected events in list order. */
    public List<Event> getSelectedEvents() {
        List<Event> events = new ArrayList<>();
        for (Row row : rows) {
            if (row.event != null && selectedIds.contains(row.event.getId())) {
                events.add(row.event);
            }
        }
        return events;
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
        EventViewHolder holder =
                new EventViewHolder(inflater.inflate(R.layout.item_event, parent, false));
        holder.itemView.setOnClickListener(v -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onEventClick(rows.get(position).event);
            }
        });
        holder.itemView.setOnLongClickListener(v -> {
            int position = holder.getBindingAdapterPosition();
            if (position != RecyclerView.NO_POSITION) {
                listener.onEventLongClick(rows.get(position).event);
            }
            return true;
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        if (row.event == null) {
            ((MonthViewHolder) holder).monthText.setText(EventDates.formatMonth(row.month));
        } else {
            bindEvent((EventViewHolder) holder, row);
            bindSelection((EventViewHolder) holder, row.event);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position,
            @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else if (holder instanceof EventViewHolder) {
            bindSelection((EventViewHolder) holder, rows.get(position).event);
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
        EventCardState state = row.state;

        holder.card.setDatePanelColor(
                MaterialColors.getColor(holder.itemView, state.getDatePanelColorAttr()));
        int datePanelTextColor =
                MaterialColors.getColor(holder.itemView, state.getDatePanelTextColorAttr());
        holder.monthText.setTextColor(datePanelTextColor);
        holder.dayText.setTextColor(datePanelTextColor);
        holder.dayOfWeekText.setTextColor(datePanelTextColor);

        holder.datePanel.setContentDescription(EventDates.formatLong(date));
        holder.monthText.setText(EventDates.formatShortMonth(date));
        holder.dayText.setText(EventDates.formatDayOfMonth(date));
        holder.dayOfWeekText.setText(EventDates.formatShortDayOfWeek(date));
        holder.nameText.setText(event.getName());
        holder.typeLabel.setText(type.getLabelRes());
        state.showStatus(holder.statusText, event, row.daysAway);
        bindTimes(holder, event);
        holder.locationText.setText(event.getLocation());
        holder.locationText.setContentDescription(
                context.getString(R.string.description_location, event.getLocation()));
        holder.organizerText.setText(event.getOrganizer());
        holder.organizerText.setContentDescription(
                context.getString(R.string.description_organizer, event.getOrganizer()));
        state.showCancelled(holder.nameText, holder.typeLabel, type,
                holder.startTimeText, holder.endTimeText);
    }

    /** Shows the circle while selecting, checked and outlined when the card is selected. */
    private void bindSelection(EventViewHolder holder, Event event) {
        boolean selected = selectedIds.contains(event.getId());
        // Selecting the view also checks the circle, outlines the card and tells screen readers.
        holder.itemView.setSelected(selected);
        holder.selectionCheck.setVisibility(selecting ? View.VISIBLE : View.GONE);
        // Screen readers say "Double tap to view details" and "Double tap and hold to select".
        Context context = holder.itemView.getContext();
        int selectRes = selected ? R.string.description_deselect : R.string.description_select;
        ViewCompat.replaceAccessibilityAction(holder.itemView,
                AccessibilityActionCompat.ACTION_CLICK,
                context.getString(selecting ? selectRes : R.string.description_view_details), null);
        ViewCompat.replaceAccessibilityAction(holder.itemView,
                AccessibilityActionCompat.ACTION_LONG_CLICK, context.getString(selectRes), null);
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

    /** Adds month headings and works out the state of each event at the given time. */
    private static List<Row> buildRows(List<Event> events, LocalDateTime now) {
        LocalDateTime nextStart = EventCardState.nextStart(events, now);
        List<Row> rows = new ArrayList<>();
        YearMonth month = null;
        for (Event event : events) {
            YearMonth eventMonth = YearMonth.from(event.getDate());
            if (!eventMonth.equals(month)) {
                month = eventMonth;
                rows.add(new Row(month, null, null, 0));
            }
            EventCardState state = EventCardState.at(event, now, nextStart);
            rows.add(new Row(month, event, state, state.daysAway(event, now.toLocalDate())));
        }
        return rows;
    }

    /** A month heading when event is null, otherwise an event in that month. */
    private static final class Row {

        private final YearMonth month;
        @Nullable
        private final Event event;
        @Nullable
        private final EventCardState state;
        private final long daysAway;

        Row(YearMonth month, @Nullable Event event, @Nullable EventCardState state, long daysAway) {
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
            // States and countdowns change as time passes, and details when an event is edited.
            return oldRow.state == newRow.state && oldRow.daysAway == newRow.daysAway
                    && Objects.equals(oldRow.event, newRow.event);
        }
    }

    /** Called when a card is tapped or long pressed. */
    public interface OnEventClickListener {
        void onEventClick(Event event);

        void onEventLongClick(Event event);
    }

    /** The view of one month heading. */
    public static final class MonthViewHolder extends RecyclerView.ViewHolder {

        private final TextView monthText;

        public MonthViewHolder(@NonNull View itemView) {
            super(itemView);
            monthText = itemView.findViewById(R.id.month_heading);
        }
    }

    /** The views of one event card. */
    public static final class EventViewHolder extends RecyclerView.ViewHolder {

        private final EventCardDrawable card;
        private final View datePanel;
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
        private final View selectionCheck;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            card = new EventCardDrawable(itemView.getContext());
            int rippleColor = MaterialColors.getColor(
                    itemView, androidx.appcompat.R.attr.colorControlHighlight);
            // Without a mask, the ripple is clipped to the card shape.
            itemView.setBackground(
                    new RippleDrawable(ColorStateList.valueOf(rippleColor), card, null));
            // Screen readers say "Double tap and hold to select".
            ViewCompat.replaceAccessibilityAction(itemView,
                    AccessibilityActionCompat.ACTION_LONG_CLICK,
                    itemView.getContext().getString(R.string.description_select), null);
            datePanel = itemView.findViewById(R.id.date_panel);
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
            selectionCheck = itemView.findViewById(R.id.selection_check);
        }
    }
}
