package hcmute.edu.vn.teeticktick.bottomsheet;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.widget.GridLayout;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import hcmute.edu.vn.teeticktick.R;

public class AddCategoryBottomSheet extends BottomSheetDialogFragment {

    private EditText etCategoryName;
    private GridLayout gridIcons;
    private GridLayout gridColors;
    private ImageView ivPreviewIcon;
    private TextView tvPreviewName;

    // Track selections
    private int selectedIconRes = R.drawable.ic_tc_icon_10;
    private String selectedColorHex = "#10B981";

    // 14 SVG icons generated previously
    private final int[] iconResources = {
            R.drawable.ic_tc_icon, R.drawable.ic_tc_icon_1, R.drawable.ic_tc_icon_2,
            R.drawable.ic_tc_icon_3, R.drawable.ic_tc_icon_4, R.drawable.ic_tc_icon_5,
            R.drawable.ic_tc_icon_6, R.drawable.ic_tc_icon_7, R.drawable.ic_tc_icon_8,
            R.drawable.ic_tc_icon_9, R.drawable.ic_tc_icon_10, R.drawable.ic_tc_icon_11,
            R.drawable.ic_tc_icon_12, R.drawable.ic_tc_icon_13
    };

    // 18 Colors matching Figma
    private final String[] colorHexes = {
            "#EF4444", "#F97316", "#F59E0B", "#EAB308", "#84CC16", "#22C55E", "#14B8A6", "#06B6D4", "#3B82F6",
            "#0EA5E9", "#6366F1", "#8B5CF6", "#A855F7", "#D946EF", "#EC4899", "#F43F5E", "#64748B", "#737373"
    };

    public static AddCategoryBottomSheet newInstance() {
        return new AddCategoryBottomSheet();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_add_category, container, false);

        etCategoryName = view.findViewById(R.id.et_category_name);
        gridIcons = view.findViewById(R.id.grid_icons);
        gridColors = view.findViewById(R.id.grid_colors);
        ivPreviewIcon = view.findViewById(R.id.iv_preview_icon);
        tvPreviewName = view.findViewById(R.id.tv_preview_name);

        view.findViewById(R.id.btn_close).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> dismiss());
        
        view.findViewById(R.id.btn_add).setOnClickListener(v -> {
            String name = etCategoryName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập tên danh mục", Toast.LENGTH_SHORT).show();
                return;
            }
            // TODO: Pass the result (name, selectedIconRes, selectedColorHex) back to parent
            Toast.makeText(getContext(), "Đã thêm: " + name, Toast.LENGTH_SHORT).show();
            dismiss();
        });

        setupNameInput();
        populateIcons();
        populateColors();

        updatePreview();

        return view;
    }

    private void setupNameInput() {
        etCategoryName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().isEmpty()) {
                    tvPreviewName.setText("Tên danh mục");
                } else {
                    tvPreviewName.setText(s.toString().trim());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void populateIcons() {
        if (getContext() == null) return;
        
        int itemSize = (int) (40 * getResources().getDisplayMetrics().density);
        int margin = (int) (6 * getResources().getDisplayMetrics().density);
        int borderSize = (int) (1 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < iconResources.length; i++) {
            final int resId = iconResources[i];

            // Container for border selection
            LinearLayout container = new LinearLayout(getContext());
            container.setGravity(Gravity.CENTER);
            container.setPadding(borderSize, borderSize, borderSize, borderSize);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = itemSize;
            params.height = itemSize;
            params.setMargins(margin, margin, margin, margin);
            // Dynamic column count adaptation
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            container.setLayoutParams(params);

            // The icon image
            ImageView icon = new ImageView(getContext());
            icon.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            icon.setImageResource(resId);
            icon.setPadding(margin, margin, margin, margin);
            // Default tint grey
            icon.setImageTintList(ColorStateList.valueOf(Color.parseColor("#10B981"))); 

            container.addView(icon);

            // Selection state visually
            container.setOnClickListener(v -> {
                selectedIconRes = resId;
                updateIconSelection();
                updatePreview();
            });

            gridIcons.addView(container);
        }
    }

    private void populateColors() {
        if (getContext() == null) return;

        int itemSize = (int) (32 * getResources().getDisplayMetrics().density);
        int margin = (int) (4 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < colorHexes.length; i++) {
            final String hex = colorHexes[i];

            // Outer container for stroke selection
            LinearLayout container = new LinearLayout(getContext());
            container.setGravity(Gravity.CENTER);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = itemSize;
            params.height = itemSize;
            params.setMargins(margin, margin, margin, margin);
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            container.setLayoutParams(params);

            // Inner circle color
            View colorCircle = new View(getContext());
            LinearLayout.LayoutParams circleParams = new LinearLayout.LayoutParams(
                    (int)(24 * getResources().getDisplayMetrics().density), 
                    (int)(24 * getResources().getDisplayMetrics().density));
            colorCircle.setLayoutParams(circleParams);
            
            // Just use an XML drawable to make it round, tinted
            colorCircle.setBackgroundResource(R.drawable.bg_button_black); // we will tint it
            colorCircle.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(hex)));

            container.addView(colorCircle);

            container.setOnClickListener(v -> {
                selectedColorHex = hex;
                updateColorSelection();
                updatePreview();
            });

            gridColors.addView(container);
        }
    }

    private void updateIconSelection() {
        for (int i = 0; i < gridIcons.getChildCount(); i++) {
            LinearLayout child = (LinearLayout) gridIcons.getChildAt(i);
            ImageView img = (ImageView) child.getChildAt(0);
            
            // Very simple visual selection indicator
            if (iconResources[i] == selectedIconRes) {
                child.setBackgroundResource(R.drawable.bg_input_gray); 
                img.setImageTintList(ColorStateList.valueOf(Color.parseColor(selectedColorHex)));
            } else {
                child.setBackground(null);
                img.setImageTintList(ColorStateList.valueOf(Color.parseColor("#10B981"))); // default color
            }
        }
    }

    private void updateColorSelection() {
        // Just refresh the entire setup since we need it dynamic
        updateIconSelection();
    }

    private void updatePreview() {
        ivPreviewIcon.setImageResource(selectedIconRes);
        ivPreviewIcon.setImageTintList(ColorStateList.valueOf(Color.parseColor(selectedColorHex)));
    }
}
