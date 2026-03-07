package hcmute.edu.vn.teeticktick.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import hcmute.edu.vn.teeticktick.database.CategoryEntity;
import hcmute.edu.vn.teeticktick.repository.CategoryRepository;

public class CategoryViewModel extends AndroidViewModel {

    private CategoryRepository repository;
    private LiveData<List<CategoryEntity>> allCategories;

    public CategoryViewModel(@NonNull Application application) {
        super(application);
        repository = new CategoryRepository(application);
        allCategories = repository.getAllCategories();
    }

    public void insert(CategoryEntity category) {
        repository.insert(category);
    }

    public void update(CategoryEntity category) {
        repository.update(category);
    }

    public void delete(CategoryEntity category) {
        repository.delete(category);
    }

    public LiveData<List<CategoryEntity>> getAllCategories() {
        return allCategories;
    }
}
