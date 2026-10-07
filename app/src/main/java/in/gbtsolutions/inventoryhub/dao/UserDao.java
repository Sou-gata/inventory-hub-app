package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.User;

@Dao
public interface UserDao {
    @Insert
    long insert(User user);

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void insertAll(List<User> users);

    @Query("DELETE FROM users")
    void deleteAll();

    @Update
    void update(User user);

    @Query("SELECT * FROM users WHERE username = :username OR email = :username OR contact = :username")
    User getUserForLogin(String username);

    @Query("SELECT * FROM users WHERE username LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR contact LIKE '%' || :query || '%' ORDER BY name ASC")
    LiveData<List<User>> searchUser(String query);

    @Query("SELECT * FROM users WHERE id = :id")
    User getUserById(long id);

    @Query("SELECT * FROM users ORDER BY name ASC")
    LiveData<List<User>> getAllUsers();

    @Query("SELECT * FROM users ORDER BY name ASC")
    List<User> getAllUsersSync();

    @Query("SELECT * FROM users WHERE is_active = 1 ORDER BY name ASC")
    LiveData<List<User>> getActiveUsers();

    @Query("SELECT * FROM users WHERE is_active = 0 ORDER BY name ASC")
    LiveData<List<User>> getInactiveUsers();

    @Query("SELECT COUNT(*) FROM users")
    int count();
}
