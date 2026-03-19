package hcmute.edu.vn.teeticktick;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.List;

import hcmute.edu.vn.teeticktick.adapter.SmartListSettingsAdapter;
import hcmute.edu.vn.teeticktick.model.SmartListItem;

public class SmartListSettingsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SmartListSettingsAdapter adapter;
    private ImageView backButton;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        getWindow().setDecorFitsSystemWindows(false);
        
        setContentView(R.layout.activity_smart_list_settings);
        
        // Force light gray status bar with direct hex color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFFF5F5F5); // Light gray #F5F5F5
        }

        prefs = getSharedPreferences("SmartListPrefs", MODE_PRIVATE);
        backButton = findViewById(R.id.back_button);
        recyclerView = findViewById(R.id.smart_list_recyclerview);

        backButton.setOnClickListener(v -> finish());

        setupRecyclerView();
    }

    private void setupRecyclerView() {
        List<SmartListItem> items = Arrays.asList(
            new SmartListItem(R.drawable.ic_ios_star, 0xFFFFC107, getString(R.string.smart_list_all), getString(R.string.visible_in_menu), prefs.getBoolean("smart_all", true)),
            new SmartListItem(R.drawable.ic_ios_clock, 0xFF4CAF50, getString(R.string.smart_list_today), getString(R.string.visible_in_menu), prefs.getBoolean("smart_today", true)),
            new SmartListItem(R.drawable.ic_ios_calendar, 0xFFFF9800, getString(R.string.smart_list_tomorrow), getString(R.string.visible_in_menu), prefs.getBoolean("smart_tomorrow", true)),
            new SmartListItem(R.drawable.ic_ios_calendar_days, 0xFF2196F3, getString(R.string.smart_list_next_7_days), getString(R.string.visible_in_menu), prefs.getBoolean("smart_next_7_days", true)),
            new SmartListItem(R.drawable.ic_ios_user, 0xFF9C27B0, getString(R.string.smart_list_assigned_to_me), getString(R.string.visible_in_menu), prefs.getBoolean("smart_assigned_to_me", true)),
            new SmartListItem(R.drawable.ic_ios_inbox, 0xFF2196F3, getString(R.string.smart_list_inbox), getString(R.string.visible_in_menu), prefs.getBoolean("smart_inbox", true)),
            new SmartListItem(R.drawable.ic_ios_check_circle, 0xFF4CAF50, getString(R.string.smart_list_completed), getString(R.string.visible_in_menu), prefs.getBoolean("smart_completed", true)),
            new SmartListItem(R.drawable.ic_ios_x_circle, 0xFFF44336, getString(R.string.smart_list_wont_do), getString(R.string.visible_in_menu), prefs.getBoolean("smart_won't_do", false))
        );

        adapter = new SmartListSettingsAdapter(items, (item, isChecked) -> {
            item.setVisible(isChecked);
            savePreference(item.getTitle(), isChecked);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void savePreference(String title, boolean isVisible) {
        // Map title to fixed key (title may be localized, keys must be stable)
        String key;
        // Match by title string against known smart list names
        if (title.equals(getString(R.string.smart_list_all)))           key = "smart_all";
        else if (title.equals(getString(R.string.smart_list_today)))    key = "smart_today";
        else if (title.equals(getString(R.string.smart_list_tomorrow))) key = "smart_tomorrow";
        else if (title.equals(getString(R.string.smart_list_next_7_days))) key = "smart_next_7_days";
        else if (title.equals(getString(R.string.smart_list_assigned_to_me))) key = "smart_assigned_to_me";
        else if (title.equals(getString(R.string.smart_list_inbox)))    key = "smart_inbox";
        else if (title.equals(getString(R.string.smart_list_completed))) key = "smart_completed";
        else if (title.equals(getString(R.string.smart_list_wont_do)))  key = "smart_won't_do";
        else key = "smart_" + title.toLowerCase().replace(" ", "_"); // fallback
        prefs.edit().putBoolean(key, isVisible).apply();
    }
}
