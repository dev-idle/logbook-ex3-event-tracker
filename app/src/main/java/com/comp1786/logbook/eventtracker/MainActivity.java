package com.comp1786.logbook.eventtracker;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.time.LocalDateTime;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private RecyclerView eventList;
    private View emptyView;
    private ExtendedFloatingActionButton addButton;

    private EventDatabase database;
    private EventAdapter adapter;

    private final ActivityResultLauncher<Intent> addEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() == RESULT_OK && data != null) {
                    showSavedEvent(
                            data.getLongExtra(AddEventActivity.EXTRA_EVENT_ID, Event.NO_ID));
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
            v.setPadding(bars.left, 0, bars.right, bars.bottom);
            return insets;
        });

        bindViews();
        setSupportActionBar(toolbar);
        database = new EventDatabase(this);
        adapter = new EventAdapter();
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

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        eventList = findViewById(R.id.event_list);
        emptyView = findViewById(R.id.empty_view);
        addButton = findViewById(R.id.add_button);
    }

    private void openAddEvent() {
        addEventLauncher.launch(new Intent(MainActivity.this, AddEventActivity.class));
    }

    /** Scrolls to an event that was just saved and confirms it. */
    private void showSavedEvent(long id) {
        eventList.scrollToPosition(adapter.positionOf(id));
        Toast.makeText(this, R.string.message_event_added, Toast.LENGTH_SHORT).show();
    }

    /** Shows the events and their number, or the empty state when there are none. */
    private void showEvents() {
        // Upcoming events that have ended become completed before they are shown.
        LocalDateTime now = LocalDateTime.now();
        database.completeEndedEvents(now);
        List<Event> events = database.getAllEvents();
        // The same time, so every ticket shown as ended is also completed.
        adapter.setEvents(events, now);
        boolean empty = events.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        collapsingToolbar.setSubtitle(empty ? null : getResources().getQuantityString(
                R.plurals.event_count, events.size(), events.size()));
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
