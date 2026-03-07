package hcmute.edu.vn.teeticktick.repository;

import android.app.Application;
import androidx.lifecycle.LiveData;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.teeticktick.database.AppDatabase;
import hcmute.edu.vn.teeticktick.database.CategoryDao;
import hcmute.edu.vn.teeticktick.database.CategoryEntity;

public class CategoryRepository {
    
    private CategoryDao categoryDao;
    private ExecutorService executorService;
    private LiveData<List<CategoryEntity>> allCategories;
    
    public CategoryRepository(Application application) {
        AppDatabase database = AppDatabase.getInstance(application);
        categoryDao = database.categoryDao();
        executorService = Executors.newSingleThreadExecutor();
        allCategories = categoryDao.getAllCategories();
    }
    
    public void insert(CategoryEntity category) {
        executorService.execute(() -> categoryDao.insert(category));
    }
    
    public void update(CategoryEntity category) {
        executorService.execute(() -> categoryDao.update(category));
    }
    
    public void delete(CategoryEntity category) {
        executorService.execute(() -> categoryDao.delete(category));
    }
    
    public LiveData<List<CategoryEntity>> getAllCategories() {
        return allCategories;
    }
}
