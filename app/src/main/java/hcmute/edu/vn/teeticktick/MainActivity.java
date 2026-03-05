package hcmute.edu.vn.teeticktick;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import hcmute.edu.vn.teeticktick.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Apply saved language
        SharedPreferences appPrefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        String language = appPrefs.getString("language", "vi");
        setLocale(language);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Enable immersive mode - hide navigation bar
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR  // Make status bar icons dark
            );
        }
        
        // Set status bar color
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(0xFFF5F5F5); // Light gray #F5F5F5
        }

        prefs = getSharedPreferences("SmartListPrefs", MODE_PRIVATE);

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
        }

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        
        appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.FirstFragment)
                .setOpenableLayout(binding.drawerLayout)
                .build();
        
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.bottomNav, navController);

        setupCustomDrawer();
        setupDrawerListener();
        
        // Check if first launch
        boolean isFirstLaunch = appPrefs.getBoolean("isFirstLaunch", true);
        if (isFirstLaunch) {
            // First launch - show Welcome
            handleMenuClick("Welcome");
            appPrefs.edit().putBoolean("isFirstLaunch", false).apply();
        } else {
            // Not first launch - show Today
            handleMenuClick("Today");
        }
        
        binding.fab.setOnClickListener(view -> showAddTaskDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateSmartListVisibility();
    }

    private void setupDrawerListener() {
        binding.drawerLayout.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(@NonNull View drawerView, float slideOffset) {
                // Gradually change status bar color as drawer opens
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    int startColor = 0xFFF5F5F5; // Light gray
                    int endColor = 0xFFFFFFFF;   // White
                    int color = interpolateColor(startColor, endColor, slideOffset);
                    getWindow().setStatusBarColor(color);
                }
            }

            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(0xFFFFFFFF); // White
                }
            }

            @Override
            public void onDrawerClosed(@NonNull View drawerView) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    getWindow().setStatusBarColor(0xFFF5F5F5); // Light gray
                }
            }

            @Override
            public void onDrawerStateChanged(int newState) {
            }
        });
    }

    private int interpolateColor(int startColor, int endColor, float fraction) {
        int startA = (startColor >> 24) & 0xff;
        int startR = (startColor >> 16) & 0xff;
        int startG = (startColor >> 8) & 0xff;
        int startB = startColor & 0xff;

        int endA = (endColor >> 24) & 0xff;
        int endR = (endColor >> 16) & 0xff;
        int endG = (endColor >> 8) & 0xff;
        int endB = endColor & 0xff;

        return ((startA + (int) (fraction * (endA - startA))) << 24) |
               ((startR + (int) (fraction * (endR - startR))) << 16) |
               ((startG + (int) (fraction * (endG - startG))) << 8) |
               ((startB + (int) (fraction * (endB - startB))));
    }

    private void updateSmartListVisibility() {
        View drawerView = binding.customDrawer;
        
        drawerView.findViewById(R.id.smart_all).setVisibility(
            prefs.getBoolean("smart_all", true) ? View.VISIBLE : View.GONE);
        drawerView.findViewById(R.id.smart_today).setVisibility(
            prefs.getBoolean("smart_today", true) ? View.VISIBLE : View.GONE);
        drawerView.findViewById(R.id.smart_tomorrow).setVisibility(
            prefs.getBoolean("smart_tomorrow", true) ? View.VISIBLE : View.GONE);
        drawerView.findViewById(R.id.smart_next_7_days).setVisibility(
            prefs.getBoolean("smart_next_7_days", true) ? View.VISIBLE : View.GONE);
        drawerView.findViewById(R.id.smart_assigned).setVisibility(
            prefs.getBoolean("smart_assigned_to_me", true) ? View.VISIBLE : View.GONE);
        drawerView.findViewById(R.id.smart_inbox).setVisibility(
            prefs.getBoolean("smart_inbox", true) ? View.VISIBLE : View.GONE);
        drawerView.findViewById(R.id.smart_completed).setVisibility(
            prefs.getBoolean("smart_completed", true) ? View.VISIBLE : View.GONE);
        drawerView.findViewById(R.id.smart_wont_do).setVisibility(
            prefs.getBoolean("smart_won't_do", false) ? View.VISIBLE : View.GONE);
    }

    private void setupCustomDrawer() {
        View drawerView = binding.customDrawer;
        
        // Close button
        drawerView.findViewById(R.id.close_drawer).setOnClickListener(v -> 
            binding.drawerLayout.closeDrawer(GravityCompat.START));
        
        // Main Lists
        drawerView.findViewById(R.id.menu_welcome).setOnClickListener(v -> handleMenuClick("Welcome"));
        drawerView.findViewById(R.id.menu_today).setOnClickListener(v -> handleMenuClick("Today"));
        drawerView.findViewById(R.id.menu_inbox).setOnClickListener(v -> handleMenuClick("Inbox"));
        drawerView.findViewById(R.id.menu_work).setOnClickListener(v -> handleMenuClick("Work"));
        drawerView.findViewById(R.id.menu_personal).setOnClickListener(v -> handleMenuClick("Personal"));
        drawerView.findViewById(R.id.menu_shopping).setOnClickListener(v -> handleMenuClick("Shopping"));
        drawerView.findViewById(R.id.menu_learning).setOnClickListener(v -> handleMenuClick("Learning"));
        
        // Smart Lists
        drawerView.findViewById(R.id.smart_all).setOnClickListener(v -> handleMenuClick("All"));
        drawerView.findViewById(R.id.smart_today).setOnClickListener(v -> handleMenuClick("Today"));
        drawerView.findViewById(R.id.smart_tomorrow).setOnClickListener(v -> handleMenuClick("Tomorrow"));
        drawerView.findViewById(R.id.smart_next_7_days).setOnClickListener(v -> handleMenuClick("Next 7 Days"));
        drawerView.findViewById(R.id.smart_assigned).setOnClickListener(v -> handleMenuClick("Assigned to Me"));
        drawerView.findViewById(R.id.smart_inbox).setOnClickListener(v -> handleMenuClick("Inbox"));
        drawerView.findViewById(R.id.smart_completed).setOnClickListener(v -> handleMenuClick("Completed"));
        drawerView.findViewById(R.id.smart_wont_do).setOnClickListener(v -> handleMenuClick("Won't Do"));
        
        // Customize button
        drawerView.findViewById(R.id.customize_button).setOnClickListener(v -> {
            Intent intent = new Intent(this, SmartListSettingsActivity.class);
            startActivity(intent);
        });
        
        updateSmartListVisibility();
    }

    private void handleMenuClick(String menuItem) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
            if (currentFragment instanceof FirstFragment) {
                FirstFragment firstFragment = (FirstFragment) currentFragment;
                
                // Use English keys for filtering
                switch (menuItem) {
                    case "Welcome":
                        firstFragment.filterByList("Welcome");
                        break;
                    case "Today":
                        firstFragment.filterByToday();
                        break;
                    case "Tomorrow":
                        firstFragment.filterByTomorrow();
                        break;
                    case "Next 7 Days":
                        firstFragment.filterByNext7Days();
                        break;
                    case "Inbox":
                        firstFragment.filterByInbox();
                        break;
                    case "All":
                        firstFragment.filterByAll();
                        break;
                    case "Assigned to Me":
                        firstFragment.filterByAssignedToMe();
                        break;
                    case "Completed":
                        firstFragment.filterByCompleted();
                        break;
                    case "Won't Do":
                        firstFragment.filterByWontDo();
                        break;
                    default:
                        // For list-based filters (Work, Personal, Shopping, Learning)
                        firstFragment.filterByList(menuItem);
                        break;
                }
            }
        }
        
        binding.drawerLayout.closeDrawer(GravityCompat.START);
    }

    private void showAddTaskDialog() {
        AddTaskBottomSheet bottomSheet = new AddTaskBottomSheet();
        bottomSheet.setOnTaskAddedListener((title, description, emoji, listName, dueDate) -> 
            addTaskToFragment(title, description, emoji, listName, dueDate)
        );
        bottomSheet.show(getSupportFragmentManager(), "AddTaskBottomSheet");
    }

    private void addTaskToFragment(String title, String description, String emoji, String listName, Long dueDate) {
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_content_main);
        if (navHostFragment != null) {
            Fragment currentFragment = navHostFragment.getChildFragmentManager().getFragments().get(0);
            if (currentFragment instanceof FirstFragment) {
                ((FirstFragment) currentFragment).addTask(title, description, emoji, listName, dueDate);
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
    
    private void setLocale(String languageCode) {
        java.util.Locale locale = new java.util.Locale(languageCode);
        java.util.Locale.setDefault(locale);
        
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
}
