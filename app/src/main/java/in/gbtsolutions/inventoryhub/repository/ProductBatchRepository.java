package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.ProductBatchDao;
import in.gbtsolutions.inventoryhub.dao.ProductDao;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;

public class ProductBatchRepository {

    private final Database db;
    private final ProductBatchDao productBatchDao;
    private final ProductDao productDao;
    private final ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface BatchActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface BatchInsertCallback {
        void onSuccess(long batchId);
        void onError(String message);
    }

    public interface CountCallback {
        void onCount(int count);
    }

    public interface SyncCallback {
        void onComplete(int newTotalQty);
    }

    public ProductBatchRepository(Application application) {
        this.db = Database.getInstance(application);
        this.productBatchDao = db.productBatchDao();
        this.productDao = db.productDao();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<ProductBatch>> getAllBatchesForProduct(int productId) {
        return productBatchDao.getAllBatchesForProduct(productId);
    }

    public LiveData<List<ProductBatch>> getActiveBatchesForProduct(int productId) {
        return productBatchDao.getActiveBatchesForProduct(productId);
    }

    public LiveData<List<ProductBatchDao.ProductExpiryTuple>> getAllEarliestExpiries() {
        return productBatchDao.getAllEarliestExpiries();
    }

    public void insertBatch(ProductBatch batch, BatchInsertCallback callback) {
        executorService.execute(() -> {
            try {
                final long[] id = new long[1];
                db.runInTransaction(() -> {
                    id[0] = productBatchDao.insert(batch);
                    syncProductQuantityInternal(batch.productId);
                });
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(id[0]));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to add batch."));
                }
            }
        });
    }

    public void updateBatch(ProductBatch batch, BatchActionCallback callback) {
        executorService.execute(() -> {
            try {
                db.runInTransaction(() -> {
                    productBatchDao.update(batch);
                    syncProductQuantityInternal(batch.productId);
                });
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to update batch."));
                }
            }
        });
    }

    public void deleteBatch(ProductBatch batch, BatchActionCallback callback) {
        executorService.execute(() -> {
            try {
                db.runInTransaction(() -> {
                    productBatchDao.delete(batch);
                    syncProductQuantityInternal(batch.productId);
                });
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to delete batch."));
                }
            }
        });
    }

    public void saveBatchesForProduct(int productId, List<ProductBatch> batches, BatchActionCallback callback) {
        executorService.execute(() -> {
            try {
                db.runInTransaction(() -> {
                    if (batches != null) {
                        for (ProductBatch b : batches) {
                            b.productId = productId;
                            if (b.batchId > 0) {
                                productBatchDao.update(b);
                            } else {
                                productBatchDao.insert(b);
                            }
                        }
                    }
                    syncProductQuantityInternal(productId);
                });
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save batches."));
                }
            }
        });
    }

    public void syncProductQuantity(int productId, SyncCallback callback) {
        executorService.execute(() -> {
            int newTotal = syncProductQuantityInternal(productId);
            if (callback != null) {
                mainHandler.post(() -> callback.onComplete(newTotal));
            }
        });
    }

    private int syncProductQuantityInternal(int productId) {
        int totalQty = productBatchDao.sumQuantityForProduct(productId);
        Product product = productDao.getProductById(productId);
        if (product != null) {
            product.quantity = totalQty;
            if (product.quantity <= 0) {
                product.status = "Out of Stock";
            } else if ("Out of Stock".equalsIgnoreCase(product.status)) {
                product.status = "Active";
            }
            product.updatedAt = System.currentTimeMillis();
            productDao.update(product);
        }
        return totalQty;
    }

    public void countBatchesCreatedToday(CountCallback callback) {
        executorService.execute(() -> {
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long startOfDay = cal.getTimeInMillis();

            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            long endOfDay = cal.getTimeInMillis();

            int count = productBatchDao.countBatchesCreatedBetween(startOfDay, endOfDay);
            if (callback != null) {
                mainHandler.post(() -> callback.onCount(count));
            }
        });
    }
}
