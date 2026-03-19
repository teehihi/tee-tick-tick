package hcmute.edu.vn.teeticktick.bottomsheet;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

import hcmute.edu.vn.teeticktick.R;
import hcmute.edu.vn.teeticktick.database.CategoryEntity;
import hcmute.edu.vn.teeticktick.utils.IconHelper;
import hcmute.edu.vn.teeticktick.viewmodel.CategoryViewModel;

public class ExtraInfoBottomSheet extends DialogFragment {

    public interface OnConfirmListener {
        void onConfirm(String categoryKey, String categoryName, List<String> assignees);
    }

    private static final int REQUEST_CONTACT = 2001;

    private OnConfirmListener confirmListener;
    private String selectedCategoryKey = null;
    private String selectedCategoryName = null;
    private final List<String> assignees = new ArrayList<>();

    private LinearLayout categoryList;
    private ChipGroup chipGroupAssignees;
    private View selectedCategoryRow = null;
    private android.widget.Button btnConfirm;

    public void setConfirmListener(OnConfirmListener listener) { this.confirmListener = listener; }
    public void setSelectedCategory(String key, String name) {
        this.selectedCategoryKey = key;
        this.selectedCategoryName = name;
    }
    public void setAssignees(List<String> list) { this.assignees.addAll(list); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        }
        return inflater.inflate(R.layout.bottom_sheet_extra_info, container, false);
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

        categoryList = view.findViewById(R.id.category_list);
        chipGroupAssignees = view.findViewById(R.id.chip_group_assignees);
        btnConfirm = view.findViewById(R.id.btn_confirm);

        // Initially disable confirm until category is selected
        btnConfirm.setEnabled(false);
        btnConfirm.setAlpha(0.4f);

        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_add_person).setOnClickListener(v -> pickContact());
        btnConfirm.setOnClickListener(v -> {
            if (confirmListener != null)
                confirmListener.onConfirm(selectedCategoryKey, selectedCategoryName, new ArrayList<>(assignees));
            dismiss();
        });

        // Restore existing assignee chips
        for (String name : assignees) addChip(name, false);

        // Load categories
        CategoryViewModel vm = new ViewModelProvider(this).get(CategoryViewModel.class);
        vm.getAllCategories().observe(getViewLifecycleOwner(), this::buildCategoryList);
    }

    private void buildCategoryList(List<CategoryEntity> categories) {
        categoryList.removeAllViews();
        selectedCategoryRow = null;

        for (CategoryEntity entity : categories) {
            String key = entity.getEmoji() != null ? entity.getEmoji() : "inbox";
            String displayName = entity.getName();
            if (entity.getSystemId() != null) {
                switch (entity.getSystemId()) {
                    case "Inbox":    displayName = getString(R.string.list_inbox); break;
                    case "Work":     displayName = getString(R.string.list_work); break;
                    case "Personal": displayName = getString(R.string.list_personal); break;
                    case "Shopping": displayName = getString(R.string.list_shopping); break;
                    case "Learning": displayName = getString(R.string.list_learning); break;
                }
            }

            View row = buildCategoryRow(key, displayName, entity.getName());
            categoryList.addView(row);

            // Mark initially selected
            if (selectedCategoryKey != null && (entity.getName().equalsIgnoreCase(selectedCategoryKey)
                    || key.equalsIgnoreCase(selectedCategoryKey))) {
                markSelected(row, true);
                selectedCategoryRow = row;
                // Already had a selection — enable confirm
                if (btnConfirm != null) {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setAlpha(1f);
                }
            }
        }
    }

    private View buildCategoryRow(String iconKey, String displayName, String entityName) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(52));
        row.setLayoutParams(rowLp);
        // iOS style: no persistent highlight, just a subtle ripple on tap
        android.util.TypedValue ripple = new android.util.TypedValue();
        requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        row.setBackgroundResource(ripple.resourceId);
        row.setTag(new String[]{iconKey, entityName, displayName});

        // Icon box
        LinearLayout iconBox = new LinearLayout(requireContext());
        int boxSize = dpToPx(36);
        LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(boxSize, boxSize);
        boxLp.setMarginEnd(dpToPx(12));
        iconBox.setLayoutParams(boxLp);
        iconBox.setGravity(android.view.Gravity.CENTER);
        iconBox.setBackgroundResource(R.drawable.bg_color_dot);
        iconBox.getBackground().setTint(IconHelper.getIconColor(iconKey));

        ImageView iv = new ImageView(requireContext());
        int iconSize = dpToPx(18);
        iv.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        iv.setImageResource(IconHelper.getIconDrawable(iconKey));
        iv.setColorFilter(Color.WHITE);
        iconBox.addView(iv);
        row.addView(iconBox);

        // Name
        TextView tvName = new TextView(requireContext());
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        tvName.setLayoutParams(nameLp);
        tvName.setText(displayName);
        tvName.setTextSize(15f);
        tvName.setTextColor(0xFF0A0A0A);
        row.addView(tvName);

        // Checkmark
        ImageView check = new ImageView(requireContext());
        int checkSize = dpToPx(20);
        check.setLayoutParams(new LinearLayout.LayoutParams(checkSize, checkSize));
        check.setImageResource(R.drawable.ic_ios_check);
        check.setColorFilter(0xFF2B7FFF);
        check.setVisibility(View.GONE);
        check.setTag("check");
        row.addView(check);

        row.setOnClickListener(v -> {
            // Deselect previous
            if (selectedCategoryRow != null) markSelected(selectedCategoryRow, false);
            markSelected(row, true);
            selectedCategoryRow = row;
            String[] tags = (String[]) row.getTag();
            selectedCategoryKey = tags[1];
            selectedCategoryName = tags[2];
            // Enable confirm button
            if (btnConfirm != null) {
                btnConfirm.setEnabled(true);
                btnConfirm.setAlpha(1f);
            }
        });

        return row;
    }

    private void markSelected(View row, boolean selected) {
        if (!(row instanceof LinearLayout)) return;
        LinearLayout ll = (LinearLayout) row;
        for (int i = 0; i < ll.getChildCount(); i++) {
            View child = ll.getChildAt(i);
            if ("check".equals(child.getTag())) {
                child.setVisibility(selected ? View.VISIBLE : View.GONE);
            }
        }
    }

    private void pickContact() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        startActivityForResult(intent, REQUEST_CONTACT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONTACT && resultCode == Activity.RESULT_OK && data != null) {
            try (android.database.Cursor cursor = requireContext().getContentResolver()
                    .query(data.getData(),
                            new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                            null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    addChip(cursor.getString(0), true);
                }
            } catch (Exception ignored) {}
        }
    }

    private void addChip(String name, boolean addToList) {
        if (addToList) {
            if (assignees.contains(name)) return;
            assignees.add(name);
        }
        Chip chip = new Chip(requireContext());
        chip.setText(name);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(android.R.color.white);
        chip.setChipStrokeWidth(1.5f);
        chip.setChipStrokeColor(ColorStateList.valueOf(0xFF2B7FFF));
        chip.setTextColor(0xFF2B7FFF);
        chip.setOnCloseIconClickListener(v -> {
            assignees.remove(name);
            chipGroupAssignees.removeView(chip);
        });
        chipGroupAssignees.addView(chip);
    }

    private int dpToPx(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
