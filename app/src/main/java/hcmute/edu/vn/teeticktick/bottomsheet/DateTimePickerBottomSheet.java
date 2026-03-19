package hcmute.edu.vn.teeticktick.bottomsheet;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;
import java.util.Locale;

import hcmute.edu.vn.teeticktick.R;

public class DateTimePickerBottomSheet extends DialogFragment {

    public interface OnDateTimeSelectedListener {
        void onDateTimeSelected(long timeMillis);
    }

    private OnDateTimeSelectedListener listener;
    private long initialMillis = System.currentTimeMillis();

    private Calendar displayCal;   // month being shown
    private Calendar selectedCal;  // currently selected date+time

    private TextView tvMonthYear;
    private LinearLayout rowDow;
    private LinearLayout calendarGrid;
    private NumberPicker pickerHour;
    private NumberPicker pickerMinute;

    private static final String[] DOW = {"Th 2", "Th 3", "Th 4", "Th 5", "Th 6", "Th 7", "CN"};

    public void setListener(OnDateTimeSelectedListener listener) {
        this.listener = listener;
    }

    public void setInitialMillis(long millis) {
        this.initialMillis = millis;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return inflater.inflate(R.layout.bottom_sheet_date_picker, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            int width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.92f);
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            // Semi-transparent dim so content behind is visible (like Figma)
            window.setDimAmount(0.4f);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        selectedCal = Calendar.getInstance();
        selectedCal.setTimeInMillis(initialMillis);
        displayCal = (Calendar) selectedCal.clone();

        tvMonthYear = view.findViewById(R.id.tv_month_year);
        rowDow = view.findViewById(R.id.row_dow);
        calendarGrid = view.findViewById(R.id.calendar_grid);
        pickerHour = view.findViewById(R.id.picker_hour);
        pickerMinute = view.findViewById(R.id.picker_minute);

        // Setup NumberPickers
        pickerHour.setMinValue(0);
        pickerHour.setMaxValue(23);
        pickerHour.setFormatter(i -> String.format(Locale.getDefault(), "%02d", i));
        pickerHour.setValue(selectedCal.get(Calendar.HOUR_OF_DAY));
        pickerHour.setWrapSelectorWheel(true);

        pickerMinute.setMinValue(0);
        pickerMinute.setMaxValue(59);
        pickerMinute.setFormatter(i -> String.format(Locale.getDefault(), "%02d", i));
        pickerMinute.setValue(selectedCal.get(Calendar.MINUTE));
        pickerMinute.setWrapSelectorWheel(true);

        styleNumberPicker(pickerHour);
        styleNumberPicker(pickerMinute);

        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_prev_month).setOnClickListener(v -> {
            displayCal.add(Calendar.MONTH, -1);
            renderCalendar();
        });
        view.findViewById(R.id.btn_next_month).setOnClickListener(v -> {
            displayCal.add(Calendar.MONTH, 1);
            renderCalendar();
        });

        view.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            // Force commit any in-progress scroll
            pickerHour.clearFocus();
            pickerMinute.clearFocus();
            selectedCal.set(Calendar.HOUR_OF_DAY, pickerHour.getValue());
            selectedCal.set(Calendar.MINUTE, pickerMinute.getValue());
            selectedCal.set(Calendar.SECOND, 0);
            if (listener != null) listener.onDateTimeSelected(selectedCal.getTimeInMillis());
            dismiss();
        });

        buildDowHeaders();
        renderCalendar();
    }

    private void buildDowHeaders() {
        rowDow.removeAllViews();
        for (String d : DOW) {
            TextView tv = makeDayLabel(d, false);
            tv.setTextColor(0xFF9CA3AF);
            tv.setTextSize(12f);
            rowDow.addView(tv);
        }
    }

    private void renderCalendar() {
        // Month/year label
        String[] months = {"Tháng Một", "Tháng Hai", "Tháng Ba", "Tháng Tư",
                "Tháng Năm", "Tháng Sáu", "Tháng Bảy", "Tháng Tám",
                "Tháng Chín", "Tháng Mười", "Tháng Mười Một", "Tháng Mười Hai"};
        tvMonthYear.setText(months[displayCal.get(Calendar.MONTH)] + " " + displayCal.get(Calendar.YEAR));

        calendarGrid.removeAllViews();

        // First day of month (Mon=0 ... Sun=6)
        Calendar first = (Calendar) displayCal.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int startDow = first.get(Calendar.DAY_OF_WEEK); // Sun=1, Mon=2...
        int offset = (startDow == Calendar.SUNDAY) ? 6 : startDow - Calendar.MONDAY;

        int daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        // Previous month fill
        Calendar prevCal = (Calendar) displayCal.clone();
        prevCal.add(Calendar.MONTH, -1);
        int daysInPrev = prevCal.getActualMaximum(Calendar.DAY_OF_MONTH);

        int totalCells = offset + daysInMonth;
        int rows = (int) Math.ceil(totalCells / 7.0);

        int cellIndex = 0;
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44));
            row.setLayoutParams(rowLp);
            calendarGrid.addView(row);

            for (int c = 0; c < 7; c++, cellIndex++) {
                int day;
                boolean isCurrentMonth;
                if (cellIndex < offset) {
                    day = daysInPrev - offset + cellIndex + 1;
                    isCurrentMonth = false;
                } else if (cellIndex < offset + daysInMonth) {
                    day = cellIndex - offset + 1;
                    isCurrentMonth = true;
                } else {
                    day = cellIndex - offset - daysInMonth + 1;
                    isCurrentMonth = false;
                }

                boolean isSelected = isCurrentMonth
                        && day == selectedCal.get(Calendar.DAY_OF_MONTH)
                        && displayCal.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH)
                        && displayCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR);

                TextView tv = makeDayLabel(String.valueOf(day), isSelected);
                if (!isCurrentMonth) tv.setTextColor(0xFFD1D5DB);

                if (isCurrentMonth) {
                    final int d = day;
                    tv.setOnClickListener(v -> {
                        selectedCal.set(Calendar.YEAR, displayCal.get(Calendar.YEAR));
                        selectedCal.set(Calendar.MONTH, displayCal.get(Calendar.MONTH));
                        selectedCal.set(Calendar.DAY_OF_MONTH, d);
                        renderCalendar();
                    });
                }
                row.addView(tv);
            }
        }
    }

    private TextView makeDayLabel(String text, boolean selected) {
        TextView tv = new TextView(requireContext());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
        tv.setLayoutParams(lp);
        tv.setGravity(Gravity.CENTER);
        tv.setText(text);
        tv.setTextSize(14f);

        if (selected) {
            tv.setBackgroundResource(R.drawable.bg_day_selected);
            tv.setTextColor(Color.WHITE);
        } else {
            tv.setTextColor(0xFF0A0A0A);
        }
        return tv;
    }

    private void styleNumberPicker(NumberPicker picker) {
        try {
            java.lang.reflect.Field paintField = NumberPicker.class.getDeclaredField("mSelectorWheelPaint");
            paintField.setAccessible(true);
            android.graphics.Paint paint = (android.graphics.Paint) paintField.get(picker);
            if (paint != null) {
                paint.setTextSize(dpToPx(22));
                paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                paint.setColor(0xFF0A0A0A);
            }
            // Also update the EditText inside NumberPicker
            for (int i = 0; i < picker.getChildCount(); i++) {
                android.view.View child = picker.getChildAt(i);
                if (child instanceof android.widget.EditText) {
                    android.widget.EditText et = (android.widget.EditText) child;
                    et.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 22);
                    et.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                    et.setTextColor(0xFF0A0A0A);
                }
            }
            picker.invalidate();
        } catch (Exception ignored) {}
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
