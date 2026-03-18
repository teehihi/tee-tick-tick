package hcmute.edu.vn.teeticktick.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;
import java.util.List;
import java.util.concurrent.Executors;

@Database(entities = {TaskEntity.class, CategoryEntity.class}, version = 4, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    
    private static AppDatabase instance;
    
    public abstract TaskDao taskDao();
    public abstract CategoryDao categoryDao();
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "teetick_database"
            )
            .fallbackToDestructiveMigration()
            .addCallback(roomCallback)
            .build();
        }
        return instance;
    }

    private static RoomDatabase.Callback roomCallback = new RoomDatabase.Callback() {
        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            // Pre-populate if missing
            Executors.newSingleThreadExecutor().execute(() -> {
                CategoryDao dao = instance.categoryDao();
                android.util.Log.e("ROOM_DB", "onOpen FIRED - Checking DB count");
                List<CategoryEntity> existing = dao.getAllCategoriesSync(); // Need to check count sync
                if (existing == null || existing.isEmpty()) {
                    android.util.Log.e("ROOM_DB", "DB EMPTY - Prepopulating default categories with custom sounds");
                    
                    // Inbox - Âm thanh mặc định
                    CategoryEntity inbox = new CategoryEntity("Inbox", "inbox", "Inbox");
                    inbox.setNotificationSound("default");
                    dao.insert(inbox);
                    
                    // Work - Âm thanh 1 (Nhẹ nhàng, chuyên nghiệp)
                    CategoryEntity work = new CategoryEntity("Work", "work", "Work");
                    work.setNotificationSound("sound_1");
                    dao.insert(work);
                    
                    // Personal - Âm thanh 2 (Năng động)
                    CategoryEntity personal = new CategoryEntity("Personal", "personal", "Personal");
                    personal.setNotificationSound("sound_2");
                    dao.insert(personal);
                    
                    // Shopping - Âm thanh 3 (Tập trung)
                    CategoryEntity shopping = new CategoryEntity("Shopping", "shopping", "Shopping");
                    shopping.setNotificationSound("sound_3");
                    dao.insert(shopping);
                    
                    // Learning - Âm thanh 4 (Vui vẻ)
                    CategoryEntity learning = new CategoryEntity("Learning", "learning", "Learning");
                    learning.setNotificationSound("sound_4");
                    dao.insert(learning);
                } else {
                     android.util.Log.e("ROOM_DB", "DB NOT EMPTY (" + existing.size() + ") - Skipping prepopulation");
                }
            });
        }
    };
}
