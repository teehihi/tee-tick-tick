package hcmute.edu.vn.teeticktick.bottomsheet;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import hcmute.edu.vn.teeticktick.R;

public class CategoryIconPickerBottomSheet extends DialogFragment {

    public interface OnIconSelectedListener {
        void onIconSelected(int iconRes, int color);
    }

    private OnIconSelectedListener listener;
    private int selectedColor = 0xFF2B7FFF;
    private int selectedIconRes = R.drawable.ic_ios_check_circle;

    private FrameLayout selectedColorView = null;
    private View selectedIconView = null;

    // 8 colors from Figma
    private static final int[] COLORS = {
        0xFF2B7FFF, 0xFFFB2C36, 0xFF00C950, 0xFFF0B100,
        0xFFAD46FF, 0xFFF6339A, 0xFFFF6900, 0xFF00BBA7
    };

    // 24 icons from Figma (6 per row × 4 rows)
    private static final int[] ICONS = {
        R.drawable.ic_ios_check_circle, R.drawable.ic_ios_briefcase,
        R.drawable.ic_ios_star,         R.drawable.ic_ios_heart,
        R.drawable.ic_ios_home,         R.drawable.ic_ios_shopping,
        R.drawable.ic_ios_target,       R.drawable.ic_ios_book,
        R.drawable.ic_ios_music,        R.drawable.ic_ios_gamepad,
        R.drawable.ic_ios_soccer,       R.drawable.ic_ios_run,
        R.drawable.ic_ios_coffee,       R.drawable.ic_ios_plane,
        R.drawable.ic_ios_car,          R.drawable.ic_ios_laptop,
        R.drawable.ic_ios_mail,         R.drawable.ic_ios_phone,
        R.drawable.ic_ios_money,        R.drawable.ic_ios_gift,
        R.drawable.ic_ios_bell,         R.drawable.ic_ios_calendar,
        R.drawable.ic_ios_flag,         R.drawable.ic_ios_chart,
    };

    public void setOnIconSelectedListener(OnIconSelectedListener listener) {
        this.listener = listener;
    }

    public void setInitialColor(int color) { this.selectedColor = color; }
    public void setInitialIcon(int iconRes) { this.selectedIconRes = iconRes; }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return inflater.inflate(R.layout.bottom_sheet_icon_picker, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog() != null ? getDialog().getWindow() : null;
        if (window != null) {
            int width = (int) (requireContext().getResources().getDisplayMetrics().widthPixels * 0.92f);
            window.setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
            window.setDimAmount(0.4f);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Close button
        view.findViewById(R.id.btn_close_picker).setOnClickListener(v -> dismiss());

        // Build color row
        LinearLayout colorRow = view.findViewById(R.id.color_row);
        for (int color : COLORS) {
            FrameLayout dot = new FrameLayout(requireContext());
            int size = dpToPx(40);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMarginEnd(dpToPx(8));
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.bg_color_dot);
            dot.getBackground().setTint(color);
            dot.setTag(color);
            dot.setOnClickListener(v -> {
                selectedColor = (int) v.getTag();
                updateColorSelection(colorRow, (FrameLayout) v);
            });
            colorRow.addView(dot);
            if (color == selectedColor) {
                dot.post(() -> updateColorSelection(colorRow, dot));
            }
        }

        // Build icon grid
        LinearLayout iconGrid = view.findViewById(R.id.icon_grid);
        LinearLayout currentRow = null;
        for (int i = 0; i < ICONS.length; i++) {
            if (i % 6 == 0) {
                currentRow = new LinearLayout(requireContext());
                currentRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                rowLp.bottomMargin = dpToPx(8);
                currentRow.setLayoutParams(rowLp);
                iconGrid.addView(currentRow);
            }
            int iconRes = ICONS[i];
            View cell = buildIconCell(iconRes);
            currentRow.addView(cell);
            if (iconRes == selectedIconRes) {
                cell.post(() -> updateIconSelection(iconGrid, cell));
            }
        }

        // Confirm button
        view.findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            if (listener != null) listener.onIconSelected(selectedIconRes, selectedColor);
            dismiss();
        });
    }

    private View buildIconCell(int iconRes) {
        LinearLayout cell = new LinearLayout(requireContext());
        // weight=1 so all 6 cells in a row share width equally
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dpToPx(52), 1f);
        lp.setMargins(dpToPx(3), dpToPx(3), dpToPx(3), dpToPx(3));
        cell.setLayoutParams(lp);
        cell.setGravity(android.view.Gravity.CENTER);
        cell.setBackgroundResource(R.drawable.bg_icon_cell);
        cell.setTag(iconRes);

        ImageView iv = new ImageView(requireContext());
        int iconSize = dpToPx(24);
        LinearLayout.LayoutParams ivLp = new LinearLayout.LayoutParams(iconSize, iconSize);
        iv.setLayoutParams(ivLp);
        iv.setImageResource(iconRes);
        iv.setColorFilter(0xFF4A5565);
        cell.addView(iv);

        cell.setOnClickListener(v -> {
            selectedIconRes = iconRes;
            ViewGroup grid = (ViewGroup) v.getParent().getParent();
            updateIconSelection(grid, v);
        });
        return cell;
    }

    private void updateColorSelection(LinearLayout row, FrameLayout selected) {
        // Reset previous dot: plain circle, no checkmark
        if (selectedColorView != null) {
            selectedColorView.removeAllViews();
            selectedColorView.setBackgroundResource(R.drawable.bg_color_dot);
            selectedColorView.getBackground().setTint((int) selectedColorView.getTag());
        }
        // Selected dot: white ring + checkmark overlay
        selected.setBackgroundResource(R.drawable.bg_color_dot_selected);
        selected.getBackground().setTint(selectedColor);
        ImageView check = new ImageView(requireContext());
        int checkSize = dpToPx(16);
        FrameLayout.LayoutParams checkLp = new FrameLayout.LayoutParams(checkSize, checkSize);
        checkLp.gravity = Gravity.CENTER;
        check.setLayoutParams(checkLp);
        check.setImageResource(R.drawable.ic_ios_check);
        check.setColorFilter(Color.WHITE);
        selected.addView(check);
        selectedColorView = selected;

        // Update selected icon cell color immediately
        if (selectedIconView != null) {
            selectedIconView.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
        }
    }

    private void updateIconSelection(ViewGroup grid, View selected) {
        // reset all
        for (int r = 0; r < grid.getChildCount(); r++) {
            ViewGroup row = (ViewGroup) grid.getChildAt(r);
            for (int c = 0; c < row.getChildCount(); c++) {
                View cell = row.getChildAt(c);
                cell.setBackgroundResource(R.drawable.bg_icon_cell);
                cell.setBackgroundTintList(null);
                if (cell instanceof LinearLayout) {
                    ImageView iv = (ImageView) ((LinearLayout) cell).getChildAt(0);
                    if (iv != null) iv.setColorFilter(0xFF4A5565);
                }
            }
        }
        // highlight selected with current selectedColor
        selected.setBackgroundResource(R.drawable.bg_icon_cell);
        selected.setBackgroundTintList(ColorStateList.valueOf(selectedColor));
        if (selected instanceof LinearLayout) {
            ImageView iv = (ImageView) ((LinearLayout) selected).getChildAt(0);
            if (iv != null) iv.setColorFilter(Color.WHITE);
        }
        selectedIconView = selected;
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
