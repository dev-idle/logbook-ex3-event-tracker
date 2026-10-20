package com.comp1786.logbook.eventtracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDateTime;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String PREFERENCES_NAME = "settings";
    private static final String KEY_STATUS_FILTER = "status_filter";

    private MaterialToolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private View filterBar;
    private ChipGroup filterGroup;
    private RecyclerView eventList;
    private View emptyView;
    private TextView emptyTitle;
    private TextView emptyMessage;
    private ExtendedFloatingActionButton addButton;

    private EventDatabase database;
    private EventAdapter adapter;
    private SharedPreferences preferences;
    // The status the list shows, or null for every event.
    @Nullable
    private EventStatus filter;

    private final ActivityResultLauncher<Intent> addEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> showSavedEvent(result, R.string.message_event_added));
    private final ActivityResultLauncher<Intent> editEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == AddEventActivity.RESULT_DELETED) {
                    showDeletedEvent(result);
                } else {
                    showSavedEvent(result, R.string.message_event_updated);
                }
            });

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
        database = new EventDatabase(this);
        adapter = new EventAdapter(this::openEditEvent);
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
        preferences = getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE);
        String savedFilter = preferences.getString(KEY_STATUS_FILTER, "");
        filter = savedFilter.isEmpty() ? null : EventStatus.valueOf(savedFilter);
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
    protected void onDestroy() {
        database.close();
        super.onDestroy();
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
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        filterBar = findViewById(R.id.filter_bar);
        filterGroup = findViewById(R.id.filter_group);
        eventList = findViewById(R.id.event_list);
        emptyView = findViewById(R.id.empty_view);
        emptyTitle = findViewById(R.id.empty_title);
        emptyMessage = findViewById(R.id.empty_message);
        addButton = findViewById(R.id.add_button);
    }

    /** Adds a chip for each status, with the saved filter chosen. No chosen chip shows all. */
    private void buildFilterChips() {
        for (EventStatus status : EventStatus.values()) {
            addFilterChip(status);
        }
        filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            filter = checkedIds.isEmpty()
                    ? null : (EventStatus) group.findViewById(checkedIds.get(0)).getTag();
            preferences.edit()
                    .putString(KEY_STATUS_FILTER, filter == null ? "" : filter.name())
                    .apply();
            showEvents();
        });
    }

    private void addFilterChip(EventStatus status) {
        Chip chip = (Chip) getLayoutInflater()
                .inflate(R.layout.item_filter_chip, filterGroup, false);
        chip.setId(View.generateViewId());
        chip.setChipIconResource(status.getIconRes());
        chip.setText(status.getLabelRes());
        chip.setTag(status);
        // The check takes the place of the icon while chosen, as in Material 3.
        chip.setOnCheckedChangeListener((button, checked) -> chip.setChipIconVisible(!checked));
        chip.setChecked(status == filter);
        filterGroup.addView(chip);
    }

    private void openAddEvent() {
        addEventLauncher.launch(new Intent(MainActivity.this, AddEventActivity.class));
    }

    private void openEditEvent(Event event) {
        editEventLauncher.launch(new Intent(MainActivity.this, AddEventActivity.class)
                .putExtra(AddEventActivity.EXTRA_EVENT, event));
    }

    /** Scrolls to an event that was just saved and confirms it with the message. */
    private void showSavedEvent(ActivityResult result, @StringRes int messageRes) {
        Intent data = result.getData();
        if (result.getResultCode() != RESULT_OK || data == null) {
            return;
        }
        scrollToEvent(data.getLongExtra(AddEventActivity.EXTRA_EVENT_ID, Event.NO_ID));
        makeSnackbar(messageRes, R.drawable.ic_check_circle, Snackbar.LENGTH_SHORT).show();
    }

    /** Scrolls to an event, first showing every event if the filter hides it. */
    private void scrollToEvent(long id) {
        if (adapter.positionOf(id) == RecyclerView.NO_POSITION && filter != null) {
            filterGroup.clearCheck();
        }
        eventList.scrollToPosition(adapter.positionOf(id));
    }

    /** Confirms that an event was deleted, with an action that saves it again. */
    private void showDeletedEvent(ActivityResult result) {
        Intent data = result.getData();
        Event event = data == null
                ? null : data.getSerializableExtra(AddEventActivity.EXTRA_EVENT, Event.class);
        if (event == null) {
            return;
        }
        Snackbar snackbar = makeSnackbar(
                R.string.message_event_deleted, R.drawable.ic_delete, Snackbar.LENGTH_LONG);
        snackbar.setAction(R.string.button_undo, v -> {
            database.restoreEvent(event);
            showEvents();
            scrollToEvent(event.getId());
        });
        snackbar.show();
    }

    /** Returns a message at the bottom of the screen, with an icon before its text. */
    private Snackbar makeSnackbar(@StringRes int messageRes, @DrawableRes int iconRes,
            int duration) {
        // Without an anchor, the Add event button moves up while the message shows.
        Snackbar snackbar = Snackbar.make(eventList, messageRes, duration);
        TextView text =
                snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        text.setCompoundDrawablesRelativeWithIntrinsicBounds(iconRes, 0, 0, 0);
        text.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.space_medium));
        TextViewCompat.setCompoundDrawableTintList(text, text.getTextColors());
        return snackbar;
    }

    /** Shows the events the filter keeps and their number, or the empty state if there are none. */
    private void showEvents() {
        // Upcoming events that have ended become completed before they are shown.
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
        collapsingToolbar.setSubtitle(empty ? null : getResources().getQuantityString(
                filter == null ? R.plurals.event_count : filter.getCountRes(),
                events.size(), events.size()));
        // With no events there is nothing to scroll, so the app bar stays expanded.
        AppBarLayout.LayoutParams params =
                (AppBarLayout.LayoutParams) collapsingToolbar.getLayoutParams();
        int flags = params.getScrollFlags();
        params.setScrollFlags(empty ? flags & ~AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                : flags | AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
        // Applies the flags and places the subtitle, which the app bar does only during layout.
        collapsingToolbar.requestLayout();
    }
}
