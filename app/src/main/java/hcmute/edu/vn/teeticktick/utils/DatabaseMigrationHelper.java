package hcmute.edu.vn.teeticktick.utils;

import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Executors;

import hcmute.edu.vn.teeticktick.database.AppDatabase;
import hcmute.edu.vn.teeticktick.database.CategoryDao;
import hcmute.edu.vn.teeticktick.database.CategoryEntity;

/**
 * Helper class to migrate existing categories to have notification sounds
 */
public class DatabaseMigrationHelper {
    
    private static final String TAG = "DBMigrationHelper";
    
    /**
     * Update existing categories with default notification sounds
     * This should be called once when app starts if categories don't have sounds
     */
    public static void updateCategoriesWithSounds(Context context) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(context);
                CategoryDao dao = database.categoryDao();
                
                List<CategoryEntity> categories = dao.getAllCategoriesSync();
                if (categories == null || categories.isEmpty()) {
                    Log.d(TAG, "No categories found");
                    return;
                }
                
                boolean needsUpdate = false;
                for (CategoryEntity category : categories) {
                    if (category.getNotificationSound() == null) {
                        needsUpdate = true;
                        break;
                    }
                }
                
                if (!needsUpdate) {
                    Log.d(TAG, "All categories already have notification sounds");
                    return;
                }
                
                Log.d(TAG, "Updating categories with notification sounds...");
                
                for (CategoryEntity category : categories) {
                    if (category.getNotificationSound() == null) {
                        String soundId = getDefaultSoundForCategory(category.getName());
                        category.setNotificationSound(soundId);
                        dao.update(category);
                        Log.d(TAG, "Updated " + category.getName() + " with sound: " + soundId);
                    }
                }
                
                Log.d(TAG, "Migration completed successfully");
                
            } catch (Exception e) {
                Log.e(TAG, "Error updating categories: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Get default sound ID for a category based on its name
     */
    private static String getDefaultSoundForCategory(String categoryName) {
        if (categoryName == null) {
            return "default";
        }
        
        switch (categoryName.toLowerCase()) {
            case "inbox":
                return "default";
            case "work":
                return "sound_1";
            case "personal":
                return "sound_2";
            case "shopping":
                return "sound_3";
            case "learning":
                return "sound_4";
            default:
                return "default";
        }
    }
}
