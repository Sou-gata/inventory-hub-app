package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.UnitOfMeasure;

@Dao
public interface UnitOfMeasureDao {

    @Query("SELECT * FROM units_of_measure ORDER BY is_default DESC, name COLLATE NOCASE ASC")
    LiveData<List<UnitOfMeasure>> getAllUnitsOfMeasure();

    @Query("SELECT * FROM units_of_measure ORDER BY is_default DESC, name COLLATE NOCASE ASC")
    List<UnitOfMeasure> getAllUnitsOfMeasureSync();

    @Query("SELECT * FROM units_of_measure WHERE uom_id = :id LIMIT 1")
    UnitOfMeasure getById(int id);

    @Query("SELECT * FROM units_of_measure WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    UnitOfMeasure getByName(String name);

    @Query("SELECT * FROM units_of_measure WHERE LOWER(name) = LOWER(:name) AND uom_id != :excludeId LIMIT 1")
    UnitOfMeasure getByNameExcluding(String name, int excludeId);

    @Query("SELECT COUNT(*) FROM units_of_measure")
    int count();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(UnitOfMeasure uom);

    @Update
    void update(UnitOfMeasure uom);

    @Delete
    void delete(UnitOfMeasure uom);

    @Query("DELETE FROM units_of_measure WHERE uom_id = :id AND is_default = 0")
    int deleteByIdIfNotDefault(int id);

    @Query("SELECT * FROM units_of_measure WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY is_default DESC, name COLLATE NOCASE ASC")
    LiveData<List<UnitOfMeasure>> search(String query);
}
