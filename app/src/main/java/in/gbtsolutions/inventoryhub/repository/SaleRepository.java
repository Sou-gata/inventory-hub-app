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
import java.util.HashSet;
import java.util.Set;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.ProductBatchDao;
import in.gbtsolutions.inventoryhub.dao.ProductDao;
import in.gbtsolutions.inventoryhub.dao.SaleDao;
import in.gbtsolutions.inventoryhub.dao.SaleItemDao;
import in.gbtsolutions.inventoryhub.helpers.UserHelper;
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleItem;
import in.gbtsolutions.inventoryhub.models.SaleItemWithProduct;
import in.gbtsolutions.inventoryhub.models.SaleWithBuyer;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.online.repository.OnlineSaleRepository;
import in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper;
import in.gbtsolutions.inventoryhub.helpers.CommonFunctions;
import in.gbtsolutions.inventoryhub.models.AuditTrail;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;
import in.gbtsolutions.inventoryhub.online.paging.EndlessRecyclerScrollListener;

public class SaleRepository {

    private final Database db;
    private final SaleDao saleDao;
    private final SaleItemDao saleItemDao;
    private final ProductDao productDao;
    private final ProductBatchDao productBatchDao;
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
    private final OnlineSaleRepository onlineSaleRepository;
    private final AppModeManager appModeManager;

    public SaleRepository(Application application) {
        this.application = application;
        this.db = Database.getInstance(application);
        this.saleDao = db.saleDao();
        this.saleItemDao = db.saleItemDao();
        this.productDao = db.productDao();
        this.productBatchDao = db.productBatchDao();
        this.allSales = saleDao.getAllSales();
        this.executorService = Executors.newFixedThreadPool(4);
        this.onlineSaleRepository = new OnlineSaleRepository(application);
        this.appModeManager = AppModeManager.getInstance(application);
    }

    public boolean isOnlineMode() {
        return appModeManager.isOnlineMode();
    }

    public OnlineSaleRepository getOnlineRepository() {
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

    public void fetchSalesOnline(int page, int limit, String startDate, String endDate, String buyerQuery, OnlineSaleRepository.SaleListCallback callback) {
        if (onlineSaleRepository != null) {
            onlineSaleRepository.fetchSales(page, limit, buyerQuery, startDate, endDate, new OnlineSaleRepository.SaleListCallback() {
                @Override
                public void onSuccess(List<SaleWithBuyer> sales, int currentPage, int totalPages, int totalRecords, boolean hasMore) {
                    executorService.execute(() -> {
                        try {
                            db.runInTransaction(() -> {
                                if (sales != null) {
                                    for (SaleWithBuyer swb : sales) {
                                        if (swb.sale != null) {
                                            saleDao.insert(swb.sale);
                                        }
                                        if (swb.buyer != null && swb.buyer.buyerId > 0) {
                                            db.buyerDao().insert(swb.buyer);
                                        }
                                    }
                                }
                            });
                        } catch (Exception ignored) {
                        }
                    });
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(sales, currentPage, totalPages, totalRecords, hasMore));
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError(errorMessage));
                    }
                }
            });
        }
    }

    public void fetchSalesOnline(String startDate, String endDate, String buyerQuery, OnlineSaleRepository.SaleListCallback callback) {
        fetchSalesOnline(1, EndlessRecyclerScrollListener.PAGE_SIZE, startDate, endDate, buyerQuery, callback);
    }

    public void fetchSaleDetailsOnline(int saleId, OnlineSaleRepository.SaleDetailCallback callback) {
        if (onlineSaleRepository != null) {
            onlineSaleRepository.fetchSaleById(saleId, callback);
        }
    }

    public LiveData<List<SaleItemWithProduct>> getSaleItemsWithProduct(int saleId) {
        return saleItemDao.getSaleItemsWithProduct(saleId);
    }

    public void insert(Sale sale) {
        if (sale.createdBy <= 0) {
            sale.createdBy = UserHelper.getCurrentUserId(application);
        }
        executorService.execute(() -> saleDao.insert(sale));
    }

    public void insert(Sale sale, SaleInsertCallback callback) {
        if (sale.createdBy <= 0) {
            sale.createdBy = UserHelper.getCurrentUserId(application);
        }
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
        if (sale.createdBy <= 0) {
            sale.createdBy = UserHelper.getCurrentUserId(application);
        }
        if (appModeManager.isOnlineMode()) {
            onlineSaleRepository.createSale(sale, items, new OnlineSaleRepository.SaleCreateCallback() {
                @Override
                public void onSuccess(int saleId, String invoiceNo) {
                    sale.saleId = saleId;
                    if (invoiceNo != null && !invoiceNo.isEmpty()) {
                        sale.invoiceId = invoiceNo;
                    }
                    executorService.execute(() -> {
                        try {
                            db.runInTransaction(() -> {
                                saleDao.insert(sale);
                                if (items != null) {
                                    for (SaleItem item : items) {
                                        item.saleId = saleId;
                                    }
                                    saleItemDao.insertAll(items);
                                }
                            });
                        } catch (Exception ignored) {
                        }
                    });
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
                        Set<Integer> affectedProductIds = new HashSet<>();
                        for (SaleItem item : items) {
                            item.setSaleId((int) saleId);
                            affectedProductIds.add(item.productId);

                            // If item has a specific batch, deduct from that batch
                            if (item.batchId != null && item.batchId > 0) {
                                ProductBatch batch = productBatchDao.getBatchById(item.batchId);
                                if (batch != null) {
                                    batch.quantity = Math.max(0.0, CommonFunctions.roundTo3Decimals(batch.quantity - item.quantity));
                                    batch.updatedAt = System.currentTimeMillis();
                                    productBatchDao.update(batch);
                                }
                            } else {
                                // Deduct from product quantity directly if no batchId
                                Product product = productDao.getProductById(item.productId);
                                if (product != null) {
                                    product.quantity = Math.max(0.0, CommonFunctions.roundTo3Decimals(product.quantity - item.quantity));
                                    if (product.quantity <= 0.0001) {
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
                                double totalQty = productBatchDao.sumQuantityForProduct(pid);
                                Product prod = productDao.getProductById(pid);
                                if (prod != null) {
                                    prod.quantity = CommonFunctions.roundTo3Decimals(totalQty);
                                    if (prod.quantity <= 0.0001) {
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
                AuditTrailHelper.logAddition(application, AuditTrail.MODULE_SALE, sale.invoiceId != null ? sale.invoiceId : ("Sale #" + generatedSaleId[0]), "Created sale " + (sale.invoiceId != null ? sale.invoiceId : "") + " (Total: ₹" + sale.totalAmount + ")");
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
        cancelSale(saleId, UserHelper.getCurrentUserId(application), callback);
    }

    public void cancelSale(int saleId, long userId, SaleActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineSaleRepository.cancelSale(saleId, new OnlineSaleRepository.SaleActionCallback() {
                @Override
                public void onSuccess() {
                    executorService.execute(() -> {
                        try {
                            Sale s = saleDao.getSaleById(saleId);
                            if (s != null) {
                                s.status = "Cancelled";
                                s.cancelledBy = userId > 0 ? userId : UserHelper.getCurrentUserId(application);
                                s.updatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                                saleDao.update(s);
                            }
                        } catch (Exception ignored) {
                        }
                    });
                    if (callback != null) {
                        mainHandler.post(callback::onSuccess);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError(errorMessage));
                    }
                }
            });
            return;
        }

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
                        Set<Integer> affectedProductIds = new HashSet<>();
                        for (SaleItem item : items) {
                            affectedProductIds.add(item.productId);

                            // Restore batch quantity if item was tied to a batch
                            if (item.batchId != null && item.batchId > 0) {
                                ProductBatch batch = productBatchDao.getBatchById(item.batchId);
                                if (batch != null) {
                                    batch.quantity = CommonFunctions.roundTo3Decimals(batch.quantity + item.quantity);
                                    batch.updatedAt = System.currentTimeMillis();
                                    productBatchDao.update(batch);
                                } else {
                                    Product product = productDao.getProductById(item.productId);
                                    if (product != null) {
                                        product.quantity = CommonFunctions.roundTo3Decimals(product.quantity + item.quantity);
                                        if (product.quantity > 0.0001 && "Out of Stock".equalsIgnoreCase(product.status)) {
                                            product.status = "Active";
                                        }
                                        product.updatedAt = System.currentTimeMillis();
                                        productDao.update(product);
                                    }
                                }
                            } else {
                                // Restore product quantity directly
                                Product product = productDao.getProductById(item.productId);
                                if (product != null) {
                                    product.quantity = CommonFunctions.roundTo3Decimals(product.quantity + item.quantity);
                                    if (product.quantity > 0.0001 && "Out of Stock".equalsIgnoreCase(product.status)) {
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
                                double totalQty = productBatchDao.sumQuantityForProduct(pid);
                                Product prod = productDao.getProductById(pid);
                                if (prod != null) {
                                    prod.quantity = CommonFunctions.roundTo3Decimals(totalQty);
                                    if (prod.quantity > 0.0001 && "Out of Stock".equalsIgnoreCase(prod.status)) {
                                        prod.status = "Active";
                                    }
                                    prod.updatedAt = System.currentTimeMillis();
                                    productDao.update(prod);
                                }
                            }
                        }
                    }

                    long finalUserId = userId > 0 ? userId : UserHelper.getCurrentUserId(application);
                    sale.status = "Cancelled";
                    sale.cancelledBy = finalUserId;
                    sale.updatedAt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    saleDao.update(sale);
                });

                AuditTrailHelper.logEdit(application, AuditTrail.MODULE_SALE, "Sale #" + saleId, "Cancelled sale #" + saleId + " and restored product stock");

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
            AuditTrailHelper.logDeletion(application, AuditTrail.MODULE_SALE, sale.invoiceId != null ? sale.invoiceId : ("Sale #" + sale.saleId), "Deleted sale " + (sale.invoiceId != null ? sale.invoiceId : ""));
        });
    }

    public void delete(Sale sale, SaleActionCallback callback) {
        executorService.execute(() -> {
            try {
                saleItemDao.deleteBySaleId(sale.saleId);
                saleDao.delete(sale);
                AuditTrailHelper.logDeletion(application, AuditTrail.MODULE_SALE, sale.invoiceId != null ? sale.invoiceId : ("Sale #" + sale.saleId), "Deleted sale " + (sale.invoiceId != null ? sale.invoiceId : ""));
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
