package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.SaleDao;
import in.gbtsolutions.inventoryhub.dao.SaleItemDao;
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleItem;
import in.gbtsolutions.inventoryhub.models.SaleItemWithProduct;
import in.gbtsolutions.inventoryhub.models.SaleWithBuyer;

public class SaleRepository {

    private final Database db;
    private final SaleDao saleDao;
    private final SaleItemDao saleItemDao;
    private final in.gbtsolutions.inventoryhub.dao.ProductDao productDao;
    private final in.gbtsolutions.inventoryhub.dao.ProductBatchDao productBatchDao;
    private final LiveData<List<Sale>> allSales;
    private final ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface SaleInsertCallback {
        void onSuccess(long saleId);
        void onError(String message);
    }

    public interface SaleActionCallback {
        void onSuccess();
        void onError(String message);
    }

    private final Application application;
    private final in.gbtsolutions.inventoryhub.online.repository.OnlineSaleRepository onlineSaleRepository;
    private final in.gbtsolutions.inventoryhub.online.config.AppModeManager appModeManager;

    public SaleRepository(Application application) {
        this.application = application;
        this.db = Database.getInstance(application);
        this.saleDao = db.saleDao();
        this.saleItemDao = db.saleItemDao();
        this.productDao = db.productDao();
        this.productBatchDao = db.productBatchDao();
        this.allSales = saleDao.getAllSales();
        this.executorService = Executors.newFixedThreadPool(4);
        this.onlineSaleRepository = new in.gbtsolutions.inventoryhub.online.repository.OnlineSaleRepository(application);
        this.appModeManager = in.gbtsolutions.inventoryhub.online.config.AppModeManager.getInstance(application);
    }

    public boolean isOnlineMode() {
        return appModeManager.isOnlineMode();
    }

    public in.gbtsolutions.inventoryhub.online.repository.OnlineSaleRepository getOnlineRepository() {
        return onlineSaleRepository;
    }

    public LiveData<List<Sale>> getAllSales() {
        return allSales;
    }

    public LiveData<List<Sale>> getSalesByBuyer(int buyerId) {
        return saleDao.getSalesByBuyer(buyerId);
    }

    public LiveData<List<Sale>> getSalesByDate(String billingDate) {
        return saleDao.getSalesByDate(billingDate);
    }

    public LiveData<List<Sale>> getSalesByStatus(String status) {
        return saleDao.getSalesByStatus(status);
    }

    public LiveData<List<Sale>> searchSales(String query) {
        return saleDao.searchSales(query);
    }

    public LiveData<List<SaleWithBuyer>> getAllSalesWithBuyer() {
        return saleDao.getAllSalesWithBuyer();
    }

    public LiveData<List<SaleWithBuyer>> getFilteredSales(String startDate, String endDate, String buyerQuery) {
        return saleDao.getFilteredSales(startDate, endDate, buyerQuery);
    }

    public LiveData<List<SaleItemWithProduct>> getSaleItemsWithProduct(int saleId) {
        return saleItemDao.getSaleItemsWithProduct(saleId);
    }

    public void insert(Sale sale) {
        executorService.execute(() -> saleDao.insert(sale));
    }

    public void insert(Sale sale, SaleInsertCallback callback) {
        executorService.execute(() -> {
            try {
                long saleId = saleDao.insert(sale);
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(saleId));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to record sale."));
                }
            }
        });
    }

    public void insertSaleWithItems(Sale sale, List<SaleItem> items, SaleInsertCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineSaleRepository.createSale(sale, items, new in.gbtsolutions.inventoryhub.online.repository.OnlineSaleRepository.SaleCreateCallback() {
                @Override
                public void onSuccess(int saleId, String invoiceNo) {
                    sale.saleId = saleId;
                    if (invoiceNo != null && !invoiceNo.isEmpty()) {
                        sale.invoiceId = invoiceNo;
                    }
                    if (callback != null) {
                        callback.onSuccess(saleId);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) {
                        callback.onError(errorMessage);
                    }
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                final long[] generatedSaleId = new long[1];
                db.runInTransaction(() -> {
                    long saleId = saleDao.insert(sale);
                    generatedSaleId[0] = saleId;
                    sale.setSaleId((int) saleId);
                    if (items != null && !items.isEmpty()) {
                        java.util.Set<Integer> affectedProductIds = new java.util.HashSet<>();
                        for (SaleItem item : items) {
                            item.setSaleId((int) saleId);
                            affectedProductIds.add(item.productId);

                            // If item has a specific batch, deduct from that batch
                            if (item.batchId != null && item.batchId > 0) {
                                in.gbtsolutions.inventoryhub.models.ProductBatch batch = productBatchDao.getBatchById(item.batchId);
                                if (batch != null) {
                                    batch.quantity = Math.max(0, batch.quantity - item.quantity);
                                    batch.updatedAt = System.currentTimeMillis();
                                    productBatchDao.update(batch);
                                }
                            } else {
                                // Deduct from product quantity directly if no batchId
                                in.gbtsolutions.inventoryhub.models.Product product = productDao.getProductById(item.productId);
                                if (product != null) {
                                    product.quantity = Math.max(0, product.quantity - item.quantity);
                                    if (product.quantity <= 0) {
                                        product.status = "Out of Stock";
                                    }
                                    product.updatedAt = System.currentTimeMillis();
                                    productDao.update(product);
                                }
                            }
                        }

                        // Sync products where batch deduction happened
                        for (Integer pid : affectedProductIds) {
                            int batchCount = productBatchDao.getAllBatchesForProductSync(pid).size();
                            if (batchCount > 0) {
                                int totalQty = productBatchDao.sumQuantityForProduct(pid);
                                in.gbtsolutions.inventoryhub.models.Product prod = productDao.getProductById(pid);
                                if (prod != null) {
                                    prod.quantity = totalQty;
                                    if (prod.quantity <= 0) {
                                        prod.status = "Out of Stock";
                                    } else if ("Out of Stock".equalsIgnoreCase(prod.status)) {
                                        prod.status = "Active";
                                    }
                                    prod.updatedAt = System.currentTimeMillis();
                                    productDao.update(prod);
                                }
                            }
                        }

                        saleItemDao.insertAll(items);
                    }
                });
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logAddition(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_SALE,
                        sale.invoiceId != null ? sale.invoiceId : ("Sale #" + generatedSaleId[0]),
                        "Created sale " + (sale.invoiceId != null ? sale.invoiceId : "") + " (Total: ₹" + sale.totalAmount + ")");
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(generatedSaleId[0]));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save sale and items."));
                }
            }
        });
    }

    public void cancelSale(int saleId, SaleActionCallback callback) {
        executorService.execute(() -> {
            try {
                db.runInTransaction(() -> {
                    Sale sale = saleDao.getSaleById(saleId);
                    if (sale == null) {
                        throw new IllegalStateException("Sale not found: ID " + saleId);
                    }
                    if ("Cancelled".equalsIgnoreCase(sale.status)) {
                        throw new IllegalStateException("Sale is already cancelled.");
                    }

                    List<SaleItem> items = saleItemDao.getItemsBySaleIdSync(saleId);
                    if (items != null && !items.isEmpty()) {
                        java.util.Set<Integer> affectedProductIds = new java.util.HashSet<>();
                        for (SaleItem item : items) {
                            affectedProductIds.add(item.productId);

                            // Restore batch quantity if item was tied to a batch
                            if (item.batchId != null && item.batchId > 0) {
                                in.gbtsolutions.inventoryhub.models.ProductBatch batch = productBatchDao.getBatchById(item.batchId);
                                if (batch != null) {
                                    batch.quantity += item.quantity;
                                    batch.updatedAt = System.currentTimeMillis();
                                    productBatchDao.update(batch);
                                } else {
                                    in.gbtsolutions.inventoryhub.models.Product product = productDao.getProductById(item.productId);
                                    if (product != null) {
                                        product.quantity += item.quantity;
                                        if (product.quantity > 0 && "Out of Stock".equalsIgnoreCase(product.status)) {
                                            product.status = "Active";
                                        }
                                        product.updatedAt = System.currentTimeMillis();
                                        productDao.update(product);
                                    }
                                }
                            } else {
                                // Restore product quantity directly
                                in.gbtsolutions.inventoryhub.models.Product product = productDao.getProductById(item.productId);
                                if (product != null) {
                                    product.quantity += item.quantity;
                                    if (product.quantity > 0 && "Out of Stock".equalsIgnoreCase(product.status)) {
                                        product.status = "Active";
                                    }
                                    product.updatedAt = System.currentTimeMillis();
                                    productDao.update(product);
                                }
                            }
                        }

                        // Re-sync products where batch restoration happened
                        for (Integer pid : affectedProductIds) {
                            int batchCount = productBatchDao.getAllBatchesForProductSync(pid).size();
                            if (batchCount > 0) {
                                int totalQty = productBatchDao.sumQuantityForProduct(pid);
                                in.gbtsolutions.inventoryhub.models.Product prod = productDao.getProductById(pid);
                                if (prod != null) {
                                    prod.quantity = totalQty;
                                    if (prod.quantity > 0 && "Out of Stock".equalsIgnoreCase(prod.status)) {
                                        prod.status = "Active";
                                    }
                                    prod.updatedAt = System.currentTimeMillis();
                                    productDao.update(prod);
                                }
                            }
                        }
                    }

                    sale.status = "Cancelled";
                    sale.updatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    saleDao.update(sale);
                });

                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_SALE,
                        "Sale #" + saleId,
                        "Cancelled sale #" + saleId + " and restored product stock");

                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to cancel sale."));
                }
            }
        });
    }

    public void update(Sale sale) {
        executorService.execute(() -> saleDao.update(sale));
    }

    public void update(Sale sale, SaleActionCallback callback) {
        executorService.execute(() -> {
            try {
                saleDao.update(sale);
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to update sale."));
                }
            }
        });
    }

    public void delete(Sale sale) {
        executorService.execute(() -> {
            saleItemDao.deleteBySaleId(sale.saleId);
            saleDao.delete(sale);
            in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logDeletion(application,
                    in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_SALE,
                    sale.invoiceId != null ? sale.invoiceId : ("Sale #" + sale.saleId),
                    "Deleted sale " + (sale.invoiceId != null ? sale.invoiceId : ""));
        });
    }

    public void delete(Sale sale, SaleActionCallback callback) {
        executorService.execute(() -> {
            try {
                saleItemDao.deleteBySaleId(sale.saleId);
                saleDao.delete(sale);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logDeletion(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_SALE,
                        sale.invoiceId != null ? sale.invoiceId : ("Sale #" + sale.saleId),
                        "Deleted sale " + (sale.invoiceId != null ? sale.invoiceId : ""));
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to delete sale."));
                }
            }
        });
    }
}
