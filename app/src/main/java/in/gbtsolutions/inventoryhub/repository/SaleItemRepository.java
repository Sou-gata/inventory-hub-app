package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.SaleItemDao;
import in.gbtsolutions.inventoryhub.models.SaleItem;

public class SaleItemRepository {

    private final SaleItemDao saleItemDao;
    private final ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SaleItemCallback {
        void onSuccess();
        void onError(String message);
    }

    public SaleItemRepository(Application application) {
        Database db = Database.getInstance(application);
        this.saleItemDao = db.saleItemDao();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<SaleItem>> getItemsBySaleId(int saleId) {
        return saleItemDao.getItemsBySaleId(saleId);
    }

    public LiveData<List<SaleItem>> getItemsByProductId(int productId) {
        return saleItemDao.getItemsByProductId(productId);
    }

    public void insert(SaleItem item) {
        executorService.execute(() -> saleItemDao.insert(item));
    }

    public void insertAll(List<SaleItem> items) {
        executorService.execute(() -> saleItemDao.insertAll(items));
    }

    public void insertAll(List<SaleItem> items, SaleItemCallback callback) {
        executorService.execute(() -> {
            try {
                saleItemDao.insertAll(items);
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save sale items."));
                }
            }
        });
    }

    public void update(SaleItem item) {
        executorService.execute(() -> saleItemDao.update(item));
    }

    public void delete(SaleItem item) {
        executorService.execute(() -> saleItemDao.delete(item));
    }

    public void deleteBySaleId(int saleId) {
        executorService.execute(() -> saleItemDao.deleteBySaleId(saleId));
    }
}
