package hcmute.edu.vn.teeticktick.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CategoryDao {
    
    @Insert
    long insert(CategoryEntity category);
    
    @Insert
    void insertAll(List<CategoryEntity> categories);
    
    @Update
    void update(CategoryEntity category);
    
    @Delete
    void delete(CategoryEntity category);
    
    @Query("SELECT * FROM categories ORDER BY id ASC")
    LiveData<List<CategoryEntity>> getAllCategories();
    
    @Query("SELECT * FROM categories")
    List<CategoryEntity> getAllCategoriesSync();

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    CategoryEntity getCategoryByName(String name);
}
