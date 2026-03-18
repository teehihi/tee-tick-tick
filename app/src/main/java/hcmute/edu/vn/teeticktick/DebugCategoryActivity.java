package hcmute.edu.vn.teeticktick;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;
import java.util.concurrent.Executors;

import hcmute.edu.vn.teeticktick.database.AppDatabase;
import hcmute.edu.vn.teeticktick.database.CategoryEntity;
import hcmute.edu.vn.teeticktick.utils.DatabaseMigrationHelper;

public class DebugCategoryActivity extends AppCompatActivity {

    private TextView debugText;
    private Button refreshButton;
    private Button migrateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Create simple layout programmatically
        android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
        layout.setOrientation(android.widget.LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        
        debugText = new TextView(this);
        debugText.setTextSize(14);
        debugText.setPadding(16, 16, 16, 16);
        
        refreshButton = new Button(this);
        refreshButton.setText("Refresh Categories");
        refreshButton.setOnClickListener(v -> loadCategories());
        
        migrateButton = new Button(this);
        migrateButton.setText("Run Migration");
        migrateButton.setOnClickListener(v -> runMigration());
        
        layout.addView(refreshButton);
        layout.addView(migrateButton);
        layout.addView(debugText);
        
        setContentView(layout);
        
        loadCategories();
    }

    private void loadCategories() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                AppDatabase database = AppDatabase.getInstance(this);
                List<CategoryEntity> categories = database.categoryDao().getAllCategoriesSync();
                
                StringBuilder sb = new StringBuilder();
                sb.append("=== CATEGORIES DEBUG ===\n\n");
                sb.append("Total categories: ").append(categories.size()).append("\n\n");
                
                for (CategoryEntity category : categories) {
                    sb.append("Name: ").append(category.getName()).append("\n");
                    sb.append("Emoji: ").append(category.getEmoji()).append("\n");
                    sb.append("Sound: ").append(category.getNotificationSound()).append("\n");
                    sb.append("---\n");
                }
                
                runOnUiThread(() -> debugText.setText(sb.toString()));
                
            } catch (Exception e) {
                runOnUiThread(() -> {
                    debugText.setText("Error: " + e.getMessage());
                    Toast.makeText(this, "Error loading categories", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void runMigration() {
        Toast.makeText(this, "Running migration...", Toast.LENGTH_SHORT).show();
        DatabaseMigrationHelper.updateCategoriesWithSounds(this);
        
        // Wait a bit then refresh
        new android.os.Handler().postDelayed(() -> {
            loadCategories();
            Toast.makeText(this, "Migration completed! Check results above.", Toast.LENGTH_LONG).show();
        }, 2000);
    }
}
