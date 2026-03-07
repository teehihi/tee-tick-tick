package hcmute.edu.vn.teeticktick.utils;

import hcmute.edu.vn.teeticktick.R;
import android.graphics.Color;

public class IconHelper {

    // Helper to get drawable ID from string key
    public static int getIconDrawable(String iconKey) {
        if (iconKey == null) return R.drawable.ic_ios_inbox;

        switch (iconKey.toLowerCase()) {
            case "inbox":
                return R.drawable.ic_ios_inbox;
            case "work":
                return R.drawable.ic_ios_briefcase;
            case "personal":
                return R.drawable.ic_ios_home;
            case "shopping":
                return R.drawable.ic_ios_shopping;
            case "learning":
            case "book":
                return R.drawable.ic_ios_book;
            case "star":
                return R.drawable.ic_ios_star;
            case "clock":
                return R.drawable.ic_ios_clock;
            case "user":
                return R.drawable.ic_ios_user;
            case "check_circle":
                return R.drawable.ic_ios_check_circle;
            case "calendar":
                return R.drawable.ic_ios_calendar;
            case "hand_wave":
                return R.drawable.ic_ios_hand_wave;
            default:
                return R.drawable.ic_ios_inbox; // Fallback
        }
    }

    // Helper to get color for icon
    public static int getIconColor(String iconKey) {
        if (iconKey == null) return Color.parseColor("#2196F3"); // Default Blue

        switch (iconKey.toLowerCase()) {
            case "inbox":
                return Color.parseColor("#2196F3"); // Blue
            case "work":
                return Color.parseColor("#9C27B0"); // Purple
            case "personal":
                return Color.parseColor("#FF9800"); // Orange
            case "shopping":
                return Color.parseColor("#E91E63"); // Pink
            case "learning":
            case "book":
                return Color.parseColor("#3F51B5"); // Indigo
            case "star":
                return Color.parseColor("#FFC107"); // Amber
            case "clock":
                return Color.parseColor("#00BCD4"); // Cyan
            case "user":
                return Color.parseColor("#607D8B"); // Blue Grey
            case "check_circle":
                return Color.parseColor("#4CAF50"); // Green
            case "calendar":
                return Color.parseColor("#F44336"); // Red
            case "hand_wave":
                return Color.parseColor("#FF9800"); // Orange
            default:
                return Color.parseColor("#2196F3"); // Blue fallback
        }
    }

    // List of available keys for the picker
    public static String[] getAvailableIconKeys() {
        return new String[]{
            "inbox", "work", "personal", "shopping", "learning",
            "star", "clock", "user", "check_circle", "calendar", "hand_wave"
        };
    }
}
