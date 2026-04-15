package hcmute.edu.vn.teeticktick;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class AddTransactionFragment extends Fragment {

    private GridLayout gridCategories;

    // We emulate the category items
    private static class CategoryItem {
        int iconRes;
        String name;
        String colorHex;

        CategoryItem(int iconRes, String name, String colorHex) {
            this.iconRes = iconRes;
            this.name = name;
            this.colorHex = colorHex;
        }
    }

    // Mapping icons to names based on the design
    private final CategoryItem[] categories = {
            new CategoryItem(R.drawable.ic_tc_icon_10, "Ăn uống", "#FF5252"),
            new CategoryItem(R.drawable.ic_tc_icon_11, "Di chuyển", "#FF9800"),
            new CategoryItem(R.drawable.ic_tc_icon_12, "Mua sắm", "#E91E63"),
            new CategoryItem(R.drawable.ic_tc_icon_2, "Giải trí", "#9C27B0"),
            new CategoryItem(R.drawable.ic_tc_icon_3, "Hóa đơn", "#2196F3"),
            new CategoryItem(R.drawable.ic_tc_icon_4, "Y tế", "#F44336"),
            new CategoryItem(R.drawable.ic_tc_icon_6, "Giáo dục", "#3F51B5"),
            new CategoryItem(R.drawable.ic_tc_icon_7, "Nhà ở", "#4CAF50"),
            new CategoryItem(R.drawable.ic_tc_icon_8, "Khác", "#9E9E9E")
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_transaction, container, false);

        gridCategories = view.findViewById(R.id.grid_categories);
        populateCategories();

        // Setup the expense/income toggle
        TextView btnExpense = view.findViewById(R.id.btn_expense);
        TextView btnIncome = view.findViewById(R.id.btn_income);

        btnExpense.setOnClickListener(v -> {
            btnExpense.setBackgroundResource(R.drawable.bg_white_rounded_shadow);
            btnExpense.setTextColor(Color.parseColor("#000000"));
            btnIncome.setBackground(null);
            btnIncome.setTextColor(Color.parseColor("#666666"));
        });

        btnIncome.setOnClickListener(v -> {
            btnIncome.setBackgroundResource(R.drawable.bg_white_rounded_shadow);
            btnIncome.setTextColor(Color.parseColor("#000000"));
            btnExpense.setBackground(null);
            btnExpense.setTextColor(Color.parseColor("#666666"));
        });

        return view;
    }

    private void populateCategories() {
        if (getContext() == null) return;

        // Convert 80dp dimension to pixels
        int itemSize = (int) (80 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);
        
        // Ensure 3 columns
        gridCategories.setColumnCount(3);

        for (int i = 0; i < categories.length; i++) {
            CategoryItem item = categories[i];

            // Create linear layout as container
            LinearLayout layout = new LinearLayout(getContext());
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setGravity(Gravity.CENTER);
            layout.setBackgroundResource(R.drawable.bg_category_item);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = itemSize;
            params.height = itemSize;
            params.setMargins(margin, margin, margin, margin);
            // Calculate row and column spans to distribute evenly
            params.rowSpec = GridLayout.spec(i / 3, 1f);
            params.columnSpec = GridLayout.spec(i % 3, 1f);
            layout.setLayoutParams(params);

            // Create Icon
            ImageView icon = new ImageView(getContext());
            int iconSize = (int) (32 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(iconSize, iconSize);
            icon.setLayoutParams(iconParams);
            icon.setImageResource(item.iconRes);
            icon.setImageTintList(ColorStateList.valueOf(Color.parseColor(item.colorHex)));

            // Create Text
            TextView text = new TextView(getContext());
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            textParams.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
            text.setLayoutParams(textParams);
            text.setText(item.name);
            text.setTextSize(12);
            text.setTextColor(Color.parseColor("#333333"));
            text.setGravity(Gravity.CENTER);

            layout.addView(icon);
            layout.addView(text);

            // Simple selection state mockup
            layout.setOnClickListener(v -> {
                // reset all backgrounds
                for(int j = 0; j < gridCategories.getChildCount(); j++) {
                    View child = gridCategories.getChildAt(j);
                    child.setBackgroundResource(R.drawable.bg_category_item);
                }
                // highlight selected
                layout.setBackgroundResource(R.drawable.bg_white_rounded_shadow);
            });

            gridCategories.addView(layout);
        }
    }
}
