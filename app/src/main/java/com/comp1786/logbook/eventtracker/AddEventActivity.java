package com.comp1786.logbook.eventtracker;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The form for adding an event. The new event is returned to MainActivity as a result.
 */
public class AddEventActivity extends AppCompatActivity {

    /** Result extra holding the new Event. */
    public static final String EXTRA_EVENT = "event";
    /** Intent extra holding the existing events, used to find duplicates. */
    public static final String EXTRA_EVENTS = "events";

    private static final String STATE_TYPE = "type";
    private static final String STATE_DISCARD_SHOWN = "discard_shown";
    private static final String STATE_START_TIME = "start_time";
    private static final String STATE_END_TIME = "end_time";

    // Where both time lists open while no start time is selected.
    private static final LocalTime DEFAULT_START_TIME = LocalTime.of(6, 0);
    private static final Duration DEFAULT_LENGTH = Duration.ofHours(1);

    private MaterialToolbar toolbar;
    private TextInputLayout nameLayout;
    private TextInputLayout dateLayout;
    private TextInputLayout startTimeLayout;
    private TextInputLayout endTimeLayout;
    private TextInputLayout locationLayout;
    private TextInputLayout organizerLayout;
    private TextInputLayout typeLayout;
    private EditText nameInput;
    private EditText dateInput;
    private DropdownField startTimeInput;
    private DropdownField endTimeInput;
    private EditText locationInput;
    private EditText organizerInput;
    private DropdownField typeInput;

    private List<Event> existingEvents;
    private EventType selectedType;
    private LocalTime selectedStartTime;
    private AlertDialog discardDialog;

    // Enabled only once something is entered, so an empty form keeps the system back animation.
    private final OnBackPressedCallback discardCallback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            showDiscardDialog();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_event);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        bindViews();
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getOnBackPressedDispatcher().addCallback(this, discardCallback);
        Event[] events = getIntent().getSerializableExtra(EXTRA_EVENTS, Event[].class);
        existingEvents = events != null ? Arrays.asList(events) : Collections.emptyList();
        buildTypeMenu();
        buildTimeMenus();

        dateLayout.setEndIconOnClickListener(v -> showDatePicker());
        getSupportFragmentManager().setFragmentResultListener(DatePickerFragment.REQUEST_KEY,
                this, (key, result) -> setTextAtEnd(dateInput, EventDates.format(
                        result.getSerializable(DatePickerFragment.RESULT_DATE, LocalDate.class))));
        findViewById(R.id.save_button).setOnClickListener(v -> save());
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Added after the views restore their text, so restoring is not treated as an edit.
        for (TextInputLayout layout : allLayouts()) {
            layout.getEditText().addTextChangedListener(new InputWatcher(layout));
        }
        showLength();
        discardCallback.setEnabled(hasInput());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedType != null) {
            outState.putString(STATE_TYPE, selectedType.name());
        }
        outState.putString(STATE_START_TIME, textOf(startTimeInput));
        outState.putString(STATE_END_TIME, textOf(endTimeInput));
        outState.putBoolean(STATE_DISCARD_SHOWN,
                discardDialog != null && discardDialog.isShowing());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String type = savedInstanceState.getString(STATE_TYPE);
        if (type != null) {
            selectedType = EventType.valueOf(type);
            // Set without filtering, which would leave only this type in the menu.
            typeInput.setText(getString(selectedType.getLabelRes()), false);
        }
        // The times are set the same way, after the end list is rebuilt for the start.
        String start = savedInstanceState.getString(STATE_START_TIME, "");
        startTimeInput.setText(start, false);
        selectedStartTime = EventTimes.parse(start);
        buildEndMenu(selectedStartTime);
        endTimeInput.setText(savedInstanceState.getString(STATE_END_TIME, ""), false);
        if (savedInstanceState.getBoolean(STATE_DISCARD_SHOWN)) {
            showDiscardDialog();
        }
    }

    @Override
    protected void onDestroy() {
        if (discardDialog != null) {
            discardDialog.dismiss();
        }
        super.onDestroy();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar);
        nameLayout = findViewById(R.id.name_layout);
        dateLayout = findViewById(R.id.date_layout);
        startTimeLayout = findViewById(R.id.start_time_layout);
        endTimeLayout = findViewById(R.id.end_time_layout);
        locationLayout = findViewById(R.id.location_layout);
        organizerLayout = findViewById(R.id.organizer_layout);
        typeLayout = findViewById(R.id.type_layout);
        nameInput = findViewById(R.id.name_input);
        dateInput = findViewById(R.id.date_input);
        startTimeInput = findViewById(R.id.start_time_input);
        endTimeInput = findViewById(R.id.end_time_input);
        locationInput = findViewById(R.id.location_input);
        organizerInput = findViewById(R.id.organizer_input);
        typeInput = findViewById(R.id.type_input);
    }

    private TextInputLayout[] allLayouts() {
        return new TextInputLayout[] {nameLayout, dateLayout, startTimeLayout, endTimeLayout,
                locationLayout, organizerLayout, typeLayout};
    }

    private void buildTypeMenu() {
        EventType[] types = EventType.values();
        String[] labels = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            labels[i] = getString(types[i].getLabelRes());
        }
        // Simple items keep the full list after a type is picked.
        typeInput.setSimpleItems(labels);
        typeInput.setOnItemClickListener((parent, view, position, id) ->
                selectedType = types[position]);
    }

    private void buildTimeMenus() {
        List<LocalTime> startTimes = EventTimes.allSlots();
        startTimeInput.setSimpleItems(labelsOf(startTimes));
        startTimeInput.setPreferredItem(EventTimes.format(DEFAULT_START_TIME));
        startTimeInput.setOnItemClickListener((parent, view, position, id) ->
                changeStartTime(startTimes.get(position)));
        buildEndMenu(null);
    }

    /** Offers end times after the start, running past midnight, and opens an hour after it. */
    private void buildEndMenu(@Nullable LocalTime start) {
        LocalTime from = start != null ? start : DEFAULT_START_TIME;
        endTimeInput.setSimpleItems(labelsOf(EventTimes.slotsAfter(from)));
        endTimeInput.setPreferredItem(EventTimes.format(from.plus(DEFAULT_LENGTH)));
    }

    /** Moves the end with a new start so the length stays, or sets it an hour later. */
    private void changeStartTime(LocalTime start) {
        LocalTime end = EventTimes.parse(textOf(endTimeInput));
        if (end != null && selectedStartTime != null) {
            end = start.plusMinutes(EventTimes.minutesBetween(selectedStartTime, end));
        } else if (end == null || end.equals(start)) {
            end = start.plus(DEFAULT_LENGTH);
        }
        selectedStartTime = start;
        buildEndMenu(start);
        endTimeInput.setText(EventTimes.format(end), false);
    }

    /** Opens the picker on the date typed so far, or on today if there is none. */
    private void showDatePicker() {
        EventValidator.Result<LocalDate> typed = EventValidator.validateDate(textOf(dateInput));
        LocalDate date = typed.isValid() ? typed.getValue() : LocalDate.now();
        DatePickerFragment.newInstance(date)
                .show(getSupportFragmentManager(), DatePickerFragment.REQUEST_KEY);
    }

    private boolean hasInput() {
        for (TextInputLayout layout : allLayouts()) {
            if (!textOf(layout.getEditText()).trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private void showDiscardDialog() {
        discardDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.discard_title)
                .setMessage(R.string.discard_message)
                .setNegativeButton(R.string.button_keep_editing, null)
                .setPositiveButton(R.string.button_discard, (dialog, which) -> finish())
                .show();
    }

    private void save() {
        EventValidator.Result<String> name = EventValidator.validateText(textOf(nameInput));
        EventValidator.Result<LocalDate> date = EventValidator.validateDate(textOf(dateInput));
        EventValidator.Result<LocalTime> startTime =
                EventValidator.validateTime(textOf(startTimeInput));
        EventValidator.Result<LocalTime> endTime =
                EventValidator.validateEndTime(textOf(endTimeInput), startTime.getValue());
        EventValidator.Result<String> location =
                EventValidator.validateText(textOf(locationInput));
        EventValidator.Result<String> organizer =
                EventValidator.validateText(textOf(organizerInput));
        EventValidator.Result<EventType> type = EventValidator.validateType(selectedType);

        nameLayout.setError(name.isValid() ? null : getString(R.string.error_name_required));
        dateLayout.setError(date.isValid() ? null : dateError(date.getError()));
        startTimeLayout.setError(
                startTime.isValid() ? null : getString(R.string.error_start_time_required));
        endTimeLayout.setError(endTime.isValid() ? null : endTimeError(endTime.getError()));
        locationLayout.setError(
                location.isValid() ? null : getString(R.string.error_location_required));
        organizerLayout.setError(
                organizer.isValid() ? null : getString(R.string.error_organizer_required));
        typeLayout.setError(type.isValid() ? null : getString(R.string.error_type_required));
        if (name.isValid() && date.isValid()
                && EventValidator.isDuplicate(name.getValue(), date.getValue(), existingEvents)) {
            nameLayout.setError(getString(R.string.error_name_duplicate));
        }

        for (TextInputLayout layout : allLayouts()) {
            if (layout.getError() != null) {
                layout.getEditText().requestFocus();
                return;
            }
        }
        Event event = new Event(name.getValue(), date.getValue(), startTime.getValue(),
                endTime.getValue(), location.getValue(), organizer.getValue(), type.getValue());
        setResult(RESULT_OK, new Intent().putExtra(EXTRA_EVENT, event));
        finish();
    }

    private String dateError(EventValidator.Error error) {
        switch (error) {
            case WRONG_DATE_FORMAT:
                return getString(R.string.error_date_format);
            case NOT_A_REAL_DATE:
                return getString(R.string.error_date_not_real);
            case DATE_OUT_OF_RANGE:
                return getString(R.string.error_date_out_of_range,
                        EventDates.format(EventDates.MINIMUM),
                        EventDates.format(EventDates.MAXIMUM));
            case EMPTY:
            default:
                return getString(R.string.error_date_required);
        }
    }

    private String endTimeError(EventValidator.Error error) {
        switch (error) {
            case END_SAME_AS_START:
                return getString(R.string.error_end_time_same);
            case EMPTY:
            default:
                return getString(R.string.error_end_time_required);
        }
    }

    /** Shows how long the event lasts under the end time once both times are valid. */
    private void showLength() {
        EventValidator.Result<LocalTime> start =
                EventValidator.validateTime(textOf(startTimeInput));
        EventValidator.Result<LocalTime> end =
                EventValidator.validateEndTime(textOf(endTimeInput), start.getValue());
        if (!start.isValid() || !end.isValid()) {
            endTimeLayout.setHelperText(null);
            return;
        }
        String length = EventTimes.formatLength(getResources(), start.getValue(), end.getValue());
        endTimeLayout.setHelperText(EventTimes.endsNextDay(start.getValue(), end.getValue())
                ? getString(R.string.end_time_next_day, length) : length);
    }

    private static String[] labelsOf(List<LocalTime> times) {
        String[] labels = new String[times.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = EventTimes.format(times.get(i));
        }
        return labels;
    }

    private static String textOf(EditText input) {
        return input.getText() == null ? "" : input.getText().toString();
    }

    private static void setTextAtEnd(EditText input, CharSequence text) {
        input.setText(text);
        input.setSelection(input.length());
    }

    /** Clears a field's error once the user edits it, and updates the event length. */
    private final class InputWatcher implements TextWatcher {

        private final TextInputLayout layout;
        private String textBefore = "";

        InputWatcher(TextInputLayout layout) {
            this.layout = layout;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            textBefore = s.toString();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Some keyboards report changes that leave the text as it was.
            if (s.toString().equals(textBefore)) {
                return;
            }
            layout.setError(null);
            discardCallback.setEnabled(hasInput());
            if (layout == startTimeLayout || layout == endTimeLayout) {
                showLength();
            }
        }
    }
}
