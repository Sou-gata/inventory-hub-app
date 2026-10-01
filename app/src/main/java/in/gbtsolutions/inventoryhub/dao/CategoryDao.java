package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.Category;

@Dao
public interface CategoryDao {
    @Insert
    long insert(Category category);

    @Insert
    void insertAll(List<Category> categories);

    @Update
    void update(Category category);

    @Delete
    void delete(Category category);

    @Query("SELECT * FROM categories WHERE category_id = :categoryId")
    Category getCategoryById(int categoryId);

    @Query("SELECT * FROM categories ORDER BY category_name ASC")
    LiveData<List<Category>> getAllCategories();

    @Query("SELECT * FROM categories WHERE category_name LIKE '%' || :query || '%'")
    LiveData<List<Category>> searchCategory(String query);

    @Query("SELECT COUNT(*) FROM categories")
    int count();

    @Query("SELECT * FROM categories WHERE LOWER(category_name) = LOWER(:categoryName) LIMIT 1")
    Category getCategoryByNameSync(String categoryName);

    @Query("SELECT * FROM categories")
    List<Category> getAllCategoriesSync();

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void upsert(Category category);
}
