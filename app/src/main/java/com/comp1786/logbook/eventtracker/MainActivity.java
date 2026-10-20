package com.comp1786.logbook.eventtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ListPopupWindow;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Lists the saved events as tickets, with a status filter and a way to delete several at once.
 */
public class MainActivity extends AppCompatActivity implements EventAdapter.OnEventClickListener {

    private static final String STATE_FILTER = "filter";
    private static final String STATE_SELECTED_IDS = "selected_ids";
    private static final String STATE_DELETE_SHOWN = "delete_shown";

    private MaterialToolbar toolbar;
    private TextView eventCount;
    private View filterBar;
    private ChipGroup filterGroup;
    private RecyclerView eventList;
    private View emptyView;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private ExtendedFloatingActionButton addButton;

    private EventDatabase database;
    private EventAdapter adapter;
    // The status the list shows, or null for every event.
    @Nullable
    private EventStatus filter;
    private AlertDialog deleteDialog;
    private ListPopupWindow moreMenu;

    private final ActivityResultLauncher<Intent> addEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::showAddedEvent);
    private final ActivityResultLauncher<Intent> detailsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == EventDetailsActivity.RESULT_DELETED) {
                    showDeletedEvent(result);
                }
            });
    // Back leaves selecting before it leaves the app.
    private final OnBackPressedCallback selectionCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            stopSelection();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            // The app bar adds the status bar height itself, so its color fills that area.
            v.setPadding(bars.left, 0, bars.right, 0);
            // Padding the screen itself would throw off where CoordinatorLayout moves the button.
            showAboveNavigationBar(bars.bottom);
            return insets;
        });

        bindViews();
        setSupportActionBar(toolbar);
        // The close button only shows while selecting.
        toolbar.setNavigationOnClickListener(v -> stopSelection());
        getOnBackPressedDispatcher().addCallback(this, selectionCallback);
        database = new EventDatabase(this);
        adapter = new EventAdapter(this);
        GridLayoutManager layoutManager = new GridLayoutManager(this,
                getResources().getInteger(R.integer.event_list_columns));
        // Month headings span every column.
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                return adapter.isMonthHeading(position) ? layoutManager.getSpanCount() : 1;
            }
        });
        eventList.setLayoutManager(layoutManager);
        eventList.setAdapter(adapter);
        // Kept across rotation but not saved, so a new launch shows every saved event.
        String savedFilter = savedInstanceState == null
                ? null : savedInstanceState.getString(STATE_FILTER);
        filter = savedFilter == null ? null : EventStatus.valueOf(savedFilter);
        buildFilterChips();

        // Shrinks to the icon while scrolling down, so it covers less of the list.
        eventList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy > 0) {
                    addButton.shrink();
                } else if (dy < 0) {
                    addButton.extend();
                }
            }
        });
        addButton.setOnClickListener(v -> openAddEvent());
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Reloads the saved events, whose states and countdowns change as time passes.
        showEvents();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (filter != null) {
            outState.putString(STATE_FILTER, filter.name());
        }
        if (adapter.isSelecting()) {
            outState.putLongArray(STATE_SELECTED_IDS, adapter.getSelectedIds());
        }
        outState.putBoolean(STATE_DELETE_SHOWN, deleteDialog != null && deleteDialog.isShowing());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Called after onStart has shown the events, so the selected ones are in the list.
        long[] selectedIds = savedInstanceState.getLongArray(STATE_SELECTED_IDS);
        if (selectedIds != null) {
            adapter.select(selectedIds);
            startSelection();
        }
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
        getMenuInflater().inflate(
                adapter.isSelecting() ? R.menu.menu_selection : R.menu.menu_more, menu);
        MenuItem more = menu.findItem(R.id.action_more);
        if (more != null) {
            // A button set as an action view does not call onOptionsItemSelected by itself.
            more.getActionView().setOnClickListener(v -> onOptionsItemSelected(more));
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem more = menu.findItem(R.id.action_more);
        if (more != null) {
            more.setVisible(adapter.getItemCount() > 0);
        }
        MenuItem selectAll = menu.findItem(R.id.action_select_all);
        if (selectAll != null) {
            // Like the check box over a table: empty, partly filled, or checked once all are.
            boolean all = adapter.isAllSelected();
            boolean some = adapter.getSelectedCount() > 0;
            selectAll.setIcon(all ? R.drawable.ic_check_box
                    : some ? R.drawable.ic_indeterminate_check_box
                    : R.drawable.ic_check_box_outline_blank);
            selectAll.setTitle(all ? R.string.menu_deselect_all : R.string.menu_select_all);
        }
        MenuItem delete = menu.findItem(R.id.action_delete);
        if (delete != null) {
            delete.setVisible(adapter.getSelectedCount() > 0);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_more) {
            moreMenu = MoreMenu.show(this, item.getActionView(), new MoreMenu.Item(
                    R.layout.item_menu_select, R.string.menu_select_events, this::startSelection));
            return true;
        } else if (id == R.id.action_select_all) {
            if (adapter.isAllSelected()) {
                adapter.clearSelection();
            } else {
                adapter.selectAll();
            }
            showSelection();
            return true;
        } else if (id == R.id.action_delete) {
            showDeleteDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onEventClick(Event event) {
        if (adapter.isSelecting()) {
            adapter.toggleSelected(event.getId());
            showSelection();
        } else {
            openEventDetails(event);
        }
    }

    /** Starts selecting with this ticket, or selects it like a tap while already selecting. */
    @Override
    public void onEventLongClick(Event event) {
        startSelection();
        onEventClick(event);
    }

    /** Ends the list above the navigation bar and lifts the button and empty state over it. */
    private void showAboveNavigationBar(int barHeight) {
        eventList.setPadding(eventList.getPaddingLeft(), eventList.getPaddingTop(),
                eventList.getPaddingRight(),
                getResources().getDimensionPixelSize(R.dimen.list_padding_bottom) + barHeight);
        ViewGroup.MarginLayoutParams button =
                (ViewGroup.MarginLayoutParams) addButton.getLayoutParams();
        button.bottomMargin =
                getResources().getDimensionPixelSize(R.dimen.screen_padding) + barHeight;
        addButton.setLayoutParams(button);
        ViewGroup.MarginLayoutParams empty =
                (ViewGroup.MarginLayoutParams) emptyView.getLayoutParams();
        empty.bottomMargin = barHeight;
        emptyView.setLayoutParams(empty);
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        eventCount = findViewById(R.id.event_count);
        filterBar = findViewById(R.id.filter_bar);
        filterGroup = findViewById(R.id.filter_group);
        eventList = findViewById(R.id.event_list);
        emptyView = findViewById(R.id.empty_view);
        emptyTitle = findViewById(R.id.empty_title);
        emptyMessage = findViewById(R.id.empty_message);
        addButton = findViewById(R.id.add_button);
    }

    /** Adds a chip per status with the current filter chosen; none chosen shows every event. */
    private void buildFilterChips() {
        for (EventStatus status : EventStatus.values()) {
            addFilterChip(status);
        }
        filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filter = checkedIds.isEmpty()
                    ? null : (EventStatus) group.findViewById(checkedIds.get(0)).getTag();
            showEvents();
        });
    }

    private void addFilterChip(EventStatus status) {
        Chip chip = (Chip) getLayoutInflater()
                .inflate(R.layout.item_filter_chip, filterGroup, false);
        chip.setId(View.generateViewId());
        chip.setText(status.getLabelRes());
        chip.setTag(status);
        chip.setChecked(status == filter);
        filterGroup.addView(chip);
    }

    private void openAddEvent() {
        addEventLauncher.launch(new Intent(MainActivity.this, AddEventActivity.class));
    }

    private void openEventDetails(Event event) {
        detailsLauncher.launch(new Intent(MainActivity.this, EventDetailsActivity.class)
                .putExtra(EventDetailsActivity.EXTRA_EVENT_ID, event.getId()));
    }

    /** Scrolls to an event that was just added and confirms it with a message. */
    private void showAddedEvent(ActivityResult result) {
        Intent data = result.getData();
        if (result.getResultCode() != RESULT_OK || data == null) {
            return;
        }
        scrollToEvent(data.getLongExtra(AddEventActivity.EXTRA_EVENT_ID, Event.NO_ID));
        IconSnackbar.make(eventList, getString(R.string.message_event_added),
                R.drawable.ic_check_circle, Snackbar.LENGTH_SHORT).show();
    }

    /** Scrolls to an event, first showing every event if the filter hides it. */
    private void scrollToEvent(long id) {
        if (adapter.positionOf(id) == RecyclerView.NO_POSITION && filter != null) {
            filterGroup.clearCheck();
        }
        eventList.scrollToPosition(adapter.positionOf(id));
    }

    /** Confirms that the event deleted from its details screen is gone. */
    private void showDeletedEvent(ActivityResult result) {
        Intent data = result.getData();
        Event event = data == null
                ? null : data.getSerializableExtra(EventDetailsActivity.EXTRA_EVENT, Event.class);
        if (event != null) {
            showDeletedEvents(Collections.singletonList(event));
        }
    }

    /** Confirms that events were deleted, with an action that saves them again. */
    private void showDeletedEvents(List<Event> events) {
        int count = events.size();
        Snackbar snackbar = IconSnackbar.make(eventList,
                getResources().getQuantityString(R.plurals.message_events_deleted, count, count),
                R.drawable.ic_delete, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.button_undo, v -> {
            // A second tap before the bar closes would save the events twice.
            v.setEnabled(false);
            database.restoreEvents(events);
            showEvents();
            scrollToEvent(events.get(0).getId());
        });
        snackbar.show();
    }

    /** Turns the app bar into a selection bar, where tapping a ticket selects it. */
    private void startSelection() {
        if (adapter.isSelecting()) {
            return;
        }
        adapter.setSelecting(true);
        selectionCallback.setEnabled(true);
        toolbar.setNavigationIcon(R.drawable.ic_close);
        toolbar.setNavigationContentDescription(R.string.description_close_selection);
        setFiltersEnabled(false);
        addButton.hide();
        showSelection();
    }

    /** Shows how many tickets are selected, with the matching Select all and delete buttons. */
    private void showSelection() {
        int count = adapter.getSelectedCount();
        toolbar.setTitle(count == 0 ? getString(R.string.title_select_events)
                : getResources().getQuantityString(R.plurals.selected_count, count, count));
        invalidateOptionsMenu();
    }

    /** Turns the selection bar back into the app bar and deselects every ticket. */
    private void stopSelection() {
        adapter.setSelecting(false);
        selectionCallback.setEnabled(false);
        toolbar.setNavigationIcon(null);
        toolbar.setTitle(R.string.app_name);
        setFiltersEnabled(true);
        addButton.show();
        invalidateOptionsMenu();
    }

    /** Locks the filter chips while selecting, so the events to select stay the same. */
    private void setFiltersEnabled(boolean enabled) {
        for (int i = 0; i < filterGroup.getChildCount(); i++) {
            filterGroup.getChildAt(i).setEnabled(enabled);
        }
    }

    /** Asks before deleting the selected events, which Undo can then bring back. */
    private void showDeleteDialog() {
        List<Event> events = adapter.getSelectedEvents();
        int count = events.size();
        deleteDialog = new MaterialAlertDialogBuilder(this,
                R.style.ThemeOverlay_EventTracker_DestructiveDialog)
                .setTitle(getResources().getQuantityString(
                        R.plurals.delete_events_title, count, count))
                .setMessage(getResources().getQuantityString(
                        R.plurals.delete_events_message, count))
                .setNegativeButton(R.string.button_cancel, null)
                .setPositiveButton(R.string.button_delete, (dialog, which) -> {
                    database.deleteEvents(events);
                    stopSelection();
                    showEvents();
                    showDeletedEvents(events);
                })
                .show();
    }

    /** Shows the events the filter keeps and their number, or the empty state if there are none. */
    private void showEvents() {
        LocalDateTime now = LocalDateTime.now();
        database.completeEndedEvents(now);
        boolean nothingSaved = database.countEvents() == 0;
        if (nothingSaved && filter != null) {
            // The chips hide while nothing is saved, so the list goes back to every event.
            filterGroup.clearCheck();
            return;
        }
        List<Event> events = database.getEvents(filter);
        // The same time, so every ticket shown as ended is also completed.
        adapter.setEvents(events, now);
        filterBar.setVisibility(nothingSaved ? View.GONE : View.VISIBLE);
        boolean empty = events.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        emptyTitle.setText(filter == null ? R.string.empty_title : filter.getEmptyRes());
        emptyMessage.setText(
                filter == null ? R.string.empty_message : R.string.empty_filtered_message);
        if (empty) {
            // The button may still be shrunk from scrolling, and the empty state needs its label.
            addButton.extend();
        }
        eventCount.setVisibility(empty ? View.GONE : View.VISIBLE);
        eventCount.setText(getResources().getQuantityString(
                filter == null ? R.plurals.event_count : filter.getCountRes(),
                events.size(), events.size()));
        if (adapter.isSelecting()) {
            // Events no longer shown were deselected, so the number selected may have changed.
            showSelection();
        } else {
            // The More options button shows only while there are tickets to select.
            invalidateOptionsMenu();
        }
    }
}
