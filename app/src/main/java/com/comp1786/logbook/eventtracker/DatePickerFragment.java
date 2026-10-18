package com.comp1786.logbook.eventtracker;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.DatePicker;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.time.LocalDate;

/**
 * Hosts the module guide's DatePickerDialog and returns the chosen date as a fragment result.
 */
public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    public static final String REQUEST_KEY = "date_picker";
    public static final String RESULT_DATE = "date";

    private static final String ARG_DATE = "date";

    /** Creates a picker that opens on the given date. */
    public static DatePickerFragment newInstance(LocalDate date) {
        // Arguments, unlike constructor parameters, survive when Android recreates the fragment.
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATE, date);
        DatePickerFragment fragment = new DatePickerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LocalDate date = requireArguments().getSerializable(ARG_DATE, LocalDate.class);
        // DatePickerDialog counts months from 0.
        return new DatePickerDialog(requireContext(), this,
                date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
    }

    @Override
    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Bundle result = new Bundle();
        result.putSerializable(RESULT_DATE, LocalDate.of(year, month + 1, dayOfMonth));
        getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
    }
}
