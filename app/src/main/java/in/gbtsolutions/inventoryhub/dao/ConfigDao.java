package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.Config;

@Dao
public interface ConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Config config);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Config> configs);

    @Update
    void update(Config config);

    @Delete
    void delete(Config config);

    @Query("SELECT * FROM configs WHERE config_key = :key LIMIT 1")
    Config getConfigByKey(String key);

    @Query("SELECT config_value FROM configs WHERE config_key = :key LIMIT 1")
    String getValueByKey(String key);

    @Query("SELECT config_value FROM configs WHERE config_key = :key LIMIT 1")
    LiveData<String> getValueLiveData(String key);

    @Query("SELECT * FROM configs ORDER BY config_key ASC")
    LiveData<List<Config>> getAllConfigs();

    @Query("SELECT * FROM configs ORDER BY config_key ASC")
    List<Config> getAllConfigsSync();

    @Query("DELETE FROM configs WHERE config_key = :key")
    void deleteByKey(String key);

    @Query("SELECT COUNT(*) FROM configs")
    int count();
}
