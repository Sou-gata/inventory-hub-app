package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.CategoryDao;
import in.gbtsolutions.inventoryhub.models.Category;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.online.repository.OnlineCategoryRepository;

public class CategoryRepository {
    private final CategoryDao categoryDao;
    private final LiveData<List<Category>> allCategory;
    private final ExecutorService executorService;
    private final Application application;
    private final OnlineCategoryRepository onlineCategoryRepository;
    private final AppModeManager appModeManager;

    public interface CategoryActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public CategoryRepository(Application application) {
        this.application = application;
        Database db = Database.getInstance(application);
        categoryDao = db.categoryDao();
        allCategory = categoryDao.getAllCategories();
        executorService = Executors.newFixedThreadPool(4);
        onlineCategoryRepository = new OnlineCategoryRepository(application);
        appModeManager = AppModeManager.getInstance(application);
    }

    public boolean isOnlineMode() {
        return appModeManager.isOnlineMode();
    }

    public OnlineCategoryRepository getOnlineRepository() {
        return onlineCategoryRepository;
    }

    public LiveData<List<Category>> getAllCategories() {
        return allCategory;
    }

    public void fetchCategoriesOnline(OnlineCategoryRepository.CategoryListCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineCategoryRepository.fetchCategories(callback);
        }
    }

    public void insert(Category category) {
        insert(category, null);
    }

    public void insert(Category category, CategoryActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineCategoryRepository.createCategory(category, new OnlineCategoryRepository.CategoryActionCallback() {
                @Override
                public void onSuccess(Category cat) {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) callback.onError(errorMessage);
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                long id = categoryDao.insert(category);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logAddition(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_CATEGORY,
                        category.categoryName,
                        "Added category: " + category.categoryName);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to add category."));
                }
            }
        });
    }

    public void update(Category category) {
        update(category, null);
    }

    public void update(Category category, CategoryActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineCategoryRepository.updateCategory(category, new OnlineCategoryRepository.CategoryActionCallback() {
                @Override
                public void onSuccess(Category cat) {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) callback.onError(errorMessage);
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                categoryDao.update(category);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_CATEGORY,
                        category.categoryName,
                        "Updated category: " + category.categoryName);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to update category."));
                }
            }
        });
    }

    public void delete(Category category) {
        delete(category, null);
    }

    public void delete(Category category, CategoryActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineCategoryRepository.deleteCategory(category.categoryId, new OnlineCategoryRepository.CategoryDeleteCallback() {
                @Override
                public void onSuccess() {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) callback.onError(errorMessage);
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                categoryDao.delete(category);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logDeletion(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_CATEGORY,
                        category.categoryName,
                        "Deleted category: " + category.categoryName);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to delete category."));
                }
            }
        });
    }

    public LiveData<List<Category>> searchCategory(String query) {
        return categoryDao.searchCategory(query);
    }
}
