package com.comp1786.logbook.eventtracker;

import android.content.Intent;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

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
    private TextView dateText;
    private View timeRow;
    private TextView timeText;
    private TextView nextDayText;
    private TextView lengthText;
    private TextView locationText;
    private TextView organizerText;
    private View content;

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
                    | WindowInsetsCompat.Type.displayCutout());
            // The card scrolls under the navigation bar and ends clear of it.
            v.setPadding(bars.left, bars.top, bars.right, 0);
            content.setPadding(content.getPaddingLeft(), content.getPaddingTop(),
                    content.getPaddingRight(),
                    getResources().getDimensionPixelSize(R.dimen.screen_padding) + bars.bottom);
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
        dateText = findViewById(R.id.event_date);
        timeRow = findViewById(R.id.time_row);
        timeText = findViewById(R.id.event_time);
        nextDayText = findViewById(R.id.next_day);
        lengthText = findViewById(R.id.event_length);
        locationText = findViewById(R.id.event_location);
        organizerText = findViewById(R.id.event_organizer);
        content = findViewById(R.id.content);
    }

    private void openEditEvent() {
        editEventLauncher.launch(new Intent(EventDetailsActivity.this, AddEditEventActivity.class)
                .putExtra(AddEditEventActivity.EXTRA_EVENT, event));
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

    /** Shows the saved event as its card would, with the same status text and colors. */
    private void showEvent() {
        // The same steps and time as the list, so the status matches the card.
        LocalDateTime now = LocalDateTime.now();
        database.completeEndedEvents(now);
        event = database.getEvent(eventId);
        if (event == null) {
            finish();
            return;
        }
        EventCardState state = EventCardState.at(
                event, now, EventCardState.nextStart(database.getAllEvents(), now));
        EventType type = event.getType();
        nameText.setText(event.getName());
        typeLabel.setText(type.getLabelRes());
        state.showStatus(statusText, event, state.daysAway(event, now.toLocalDate()));
        showTimes();
        // Without labels on screen, screen readers hear what each row is.
        locationText.setText(event.getLocation());
        locationText.setContentDescription(
                getString(R.string.description_location, event.getLocation()));
        organizerText.setText(event.getOrganizer());
        organizerText.setContentDescription(
                getString(R.string.description_organizer, event.getOrganizer()));
        state.showCancelled(nameText, typeLabel, type, dateText, timeText);
    }

    /** Shows the date, then the times and length, with a +1 when the event ends the next day. */
    private void showTimes() {
        String start = EventTimes.format(event.getStartTime());
        String end = EventTimes.format(event.getEndTime());
        String length = EventTimes.formatLength(
                getResources(), event.getStartTime(), event.getEndTime());
        dateText.setText(EventDates.formatLong(event.getDate()));
        timeText.setText(getString(R.string.time_range, start, end));
        lengthText.setText(length);
        nextDayText.setVisibility(event.endsNextDay() ? View.VISIBLE : View.GONE);
        timeRow.setContentDescription(getString(event.endsNextDay()
                ? R.string.description_time_range_next_day : R.string.description_time_range,
                start, end, length));
    }
}
