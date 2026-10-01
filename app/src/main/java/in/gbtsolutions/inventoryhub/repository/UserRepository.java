package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.UserDao;
import in.gbtsolutions.inventoryhub.models.User;

public class UserRepository {
    private final UserDao userDao;
    private final LiveData<List<User>> allUsers;
    private final ExecutorService executorService;
    private final Application application;

    public UserRepository(Application application) {
        this.application = application;
        Database database = Database.getInstance(application);
        userDao = database.userDao();
        allUsers = userDao.getAllUsers();
        executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<User>> getAllUsers() {
        return allUsers;
    }

    public void insert(User user) {
        executorService.execute(() -> {
            userDao.insert(user);
            in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logAddition(application,
                    in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_USER,
                    user.username,
                    "Created user '" + user.username + "' with role '" + user.role + "'");
        });
    }

    public void update(User user) {
        executorService.execute(() -> {
            userDao.update(user);
            in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                    in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_USER,
                    user.username,
                    "Updated user '" + user.username + "'");
        });
    }

    public User getUserForLogin(String username) {
        return userDao.getUserForLogin(username);
    }

    public LiveData<List<User>> getActiveUsers() {
        return userDao.getActiveUsers();
    }

    public LiveData<List<User>> getInactiveUsers() {
        return userDao.getInactiveUsers();
    }

    public User getUserById(int userId) {
        return userDao.getUserById(userId);
    }

    public LiveData<List<User>> searchUsers(String query) {
        return userDao.searchUser(query);
    }
}
