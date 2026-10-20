package com.comp1786.logbook.eventtracker;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Shows one saved event in full, with a menu to edit or delete it.
 */
public class EventDetailsActivity extends AppCompatActivity {

    /** Extra with the ID of the event to show. */
    public static final String EXTRA_EVENT_ID = "event_id";
    /** Result extra holding the deleted event, so MainActivity can save it again. */
    public static final String EXTRA_EVENT = "event";
    /** Result code when the event was deleted. */
    public static final int RESULT_DELETED = RESULT_FIRST_USER;

    private static final String STATE_DELETE_SHOWN = "delete_shown";

    private MaterialToolbar toolbar;
    private TextView nameText;
    private TextView typeLabel;
    private TextView statusText;
    private View timeBlock;
    private TextView startTimeText;
    private TextView startDateText;
    private TextView lengthText;
    private TextView endTimeText;
    private TextView endDateText;
    private TextView locationText;
    private TextView organizerText;

    private EventDatabase database;
    private long eventId;
    private Event event;
    private AlertDialog deleteDialog;
    private ListPopupWindow moreMenu;

    private final ActivityResultLauncher<Intent> editEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                // onStart has already shown the saved changes.
                if (result.getResultCode() == RESULT_OK) {
                    IconSnackbar.make(findViewById(R.id.main),
                            getString(R.string.message_event_updated),
                            R.drawable.ic_check_circle, Snackbar.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_event_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        bindViews();
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        database = new EventDatabase(this);
        eventId = getIntent().getLongExtra(EXTRA_EVENT_ID, Event.NO_ID);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Reloads the event, which may have been edited or have ended since it was shown.
        showEvent();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_DELETE_SHOWN, deleteDialog != null && deleteDialog.isShowing());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getBoolean(STATE_DELETE_SHOWN)) {
            showDeleteDialog();
        }
    }

    @Override
    protected void onDestroy() {
        if (deleteDialog != null) {
            deleteDialog.dismiss();
        }
        if (moreMenu != null) {
            moreMenu.dismiss();
        }
        database.close();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_more, menu);
        MenuItem more = menu.findItem(R.id.action_more);
        // A button set as an action view does not call onOptionsItemSelected by itself.
        more.getActionView().setOnClickListener(v -> onOptionsItemSelected(more));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_more) {
            moreMenu = MoreMenu.show(this, item.getActionView(),
                    new MoreMenu.Item(R.layout.item_menu_edit, R.string.menu_edit,
                            this::openEditEvent),
                    new MoreMenu.Item(R.layout.item_menu_delete, R.string.menu_delete,
                            this::showDeleteDialog));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        nameText = findViewById(R.id.event_name);
        typeLabel = findViewById(R.id.type_label);
        statusText = findViewById(R.id.event_status);
        timeBlock = findViewById(R.id.time_block);
        startTimeText = findViewById(R.id.start_time);
        startDateText = findViewById(R.id.start_date);
        lengthText = findViewById(R.id.event_length);
        endTimeText = findViewById(R.id.end_time);
        endDateText = findViewById(R.id.end_date);
        locationText = findViewById(R.id.event_location);
        organizerText = findViewById(R.id.event_organizer);
    }

    private void openEditEvent() {
        editEventLauncher.launch(new Intent(EventDetailsActivity.this, AddEventActivity.class)
                .putExtra(AddEventActivity.EXTRA_EVENT, event));
    }

    /** Asks before deleting the event, which then returns RESULT_DELETED. */
    private void showDeleteDialog() {
        Event deleted = event;
        deleteDialog = new MaterialAlertDialogBuilder(this,
                R.style.ThemeOverlay_EventTracker_DestructiveDialog)
                .setTitle(R.string.delete_title)
                .setMessage(getString(R.string.delete_message, deleted.getName(),
                        EventDates.format(deleted.getDate())))
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_delete, (dialog, which) -> {
                    database.deleteEvent(deleted.getId());
                    setResult(RESULT_DELETED, new Intent().putExtra(EXTRA_EVENT, deleted));
                    finish();
                })
                .show();
    }

    /** Shows the saved event as its ticket would, with the same status text and colors. */
    private void showEvent() {
        // The same steps and time as the list, so the status matches the ticket.
        LocalDateTime now = LocalDateTime.now();
        database.completeEndedEvents(now);
        event = database.getEvent(eventId);
        if (event == null) {
            finish();
            return;
        }
        TicketState state =
                TicketState.at(event, now, TicketState.nextStart(database.getAllEvents(), now));
        EventType type = event.getType();
        nameText.setText(event.getName());
        typeLabel.setText(type.getLabelRes());
        statusText.setText(state.statusText(this, event, state.daysAway(event, now.toLocalDate())));
        statusText.setTextColor(MaterialColors.getColor(statusText, state.getStatusColorAttr()));
        statusText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                state == TicketState.LIVE ? R.drawable.ic_live : 0, 0, 0, 0);
        showTimes();
        locationText.setText(event.getLocation());
        organizerText.setText(event.getOrganizer());
        showCancelled(type, state == TicketState.CANCELLED);
    }

    /** Shows when the event starts and ends, with the end date in color if it is the next day. */
    private void showTimes() {
        LocalDate startDate = event.getDate();
        LocalDate endDate = event.getEnd().toLocalDate();
        String start = EventTimes.format(event.getStartTime());
        String end = EventTimes.format(event.getEndTime());
        String length = EventTimes.formatLength(
                getResources(), event.getStartTime(), event.getEndTime());
        startTimeText.setText(start);
        startDateText.setText(EventDates.formatMedium(startDate));
        lengthText.setText(length);
        endTimeText.setText(end);
        endDateText.setText(EventDates.formatMedium(endDate));
        // The same color as the +1 on the ticket.
        endDateText.setTextColor(MaterialColors.getColor(endDateText, event.endsNextDay()
                ? androidx.appcompat.R.attr.colorPrimary
                : com.google.android.material.R.attr.colorOnSurfaceVariant));
        timeBlock.setContentDescription(getString(R.string.description_starts_ends,
                start, EventDates.formatLong(startDate), end, EventDates.formatLong(endDate),
                length));
    }

    /** Grays out a cancelled event and strikes through its name, as its ticket does. */
    private void showCancelled(EventType type, boolean cancelled) {
        int textColor = MaterialColors.getColor(nameText, cancelled
                ? com.google.android.material.R.attr.colorOnSurfaceVariant
                : com.google.android.material.R.attr.colorOnSurface);
        nameText.setTextColor(textColor);
        startTimeText.setTextColor(textColor);
        endTimeText.setTextColor(textColor);
        int flags = nameText.getPaintFlags();
        nameText.setPaintFlags(cancelled
                ? flags | Paint.STRIKE_THRU_TEXT_FLAG : flags & ~Paint.STRIKE_THRU_TEXT_FLAG);
        typeLabel.setBackgroundTintList(ColorStateList.valueOf(cancelled
                ? MaterialColors.getColor(typeLabel,
                        com.google.android.material.R.attr.colorSurfaceContainerHighest)
                : getColor(type.getContainerColorRes())));
    }
}
