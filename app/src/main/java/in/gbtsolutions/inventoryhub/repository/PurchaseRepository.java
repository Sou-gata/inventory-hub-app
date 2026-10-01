package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.lifecycle.LiveData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.ProductBatchDao;
import in.gbtsolutions.inventoryhub.dao.ProductDao;
import in.gbtsolutions.inventoryhub.dao.PurchaseDao;
import in.gbtsolutions.inventoryhub.dao.PurchaseItemDao;
import in.gbtsolutions.inventoryhub.dao.ReceiveItemDao;
import in.gbtsolutions.inventoryhub.dao.ReceiveRecordDao;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;
import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseItem;
import in.gbtsolutions.inventoryhub.models.PurchaseItemWithProduct;
import in.gbtsolutions.inventoryhub.models.PurchaseWithSupplier;
import in.gbtsolutions.inventoryhub.models.ReceiveItem;
import in.gbtsolutions.inventoryhub.models.ReceiveRecord;
import in.gbtsolutions.inventoryhub.models.ReceiveRecordWithItems;

public class PurchaseRepository {

    private final Database db;
    private final PurchaseDao purchaseDao;
    private final PurchaseItemDao purchaseItemDao;
    private final ProductDao productDao;
    private final ProductBatchDao productBatchDao;
    private final ReceiveRecordDao receiveRecordDao;
    private final ReceiveItemDao receiveItemDao;
    private final ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Application application;
    private final in.gbtsolutions.inventoryhub.online.repository.OnlinePurchaseRepository onlinePurchaseRepository;
    private final in.gbtsolutions.inventoryhub.online.config.AppModeManager appModeManager;

    public PurchaseRepository(Application application) {
        this.application = application;
        this.db = Database.getInstance(application);
        this.purchaseDao = db.purchaseDao();
        this.purchaseItemDao = db.purchaseItemDao();
        this.productDao = db.productDao();
        this.productBatchDao = db.productBatchDao();
        this.receiveRecordDao = db.receiveRecordDao();
        this.receiveItemDao = db.receiveItemDao();
        this.executorService = Executors.newFixedThreadPool(4);
        this.onlinePurchaseRepository = new in.gbtsolutions.inventoryhub.online.repository.OnlinePurchaseRepository(application);
        this.appModeManager = in.gbtsolutions.inventoryhub.online.config.AppModeManager.getInstance(application);
    }

    public boolean isOnlineMode() {
        return appModeManager.isOnlineMode();
    }

    public in.gbtsolutions.inventoryhub.online.repository.OnlinePurchaseRepository getOnlineRepository() {
        return onlinePurchaseRepository;
    }

    public LiveData<List<PurchaseWithSupplier>> getFilteredPurchases(String startDate, String endDate, String supplierQuery) {
        return purchaseDao.getFilteredPurchases(startDate, endDate, supplierQuery);
    }

    public LiveData<List<PurchaseWithSupplier>> getPendingPurchasesWithSupplier() {
        return purchaseDao.getPendingPurchasesWithSupplier();
    }

    public LiveData<List<PurchaseWithSupplier>> getFilteredPendingPurchases(String supplierQuery) {
        return purchaseDao.getFilteredPendingPurchases(supplierQuery);
    }

    public LiveData<List<PurchaseItemWithProduct>> getPurchaseItemsWithProduct(int purchaseId) {
        return purchaseItemDao.getPurchaseItemsWithProduct(purchaseId);
    }

    public LiveData<List<ReceiveRecordWithItems>> getReceiveRecordsWithItems(int purchaseId) {
        return receiveRecordDao.getRecordsWithItemsByPurchaseId(purchaseId);
    }

    public void insertPurchaseWithItems(Purchase purchase, List<PurchaseItem> items, PurchaseInsertCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlinePurchaseRepository.createPurchase(purchase, items, new in.gbtsolutions.inventoryhub.online.repository.OnlinePurchaseRepository.PurchaseCreateCallback() {
                @Override
                public void onSuccess(int purchaseId) {
                    purchase.setPurchaseId(purchaseId);
                    if (callback != null) {
                        callback.onSuccess(purchaseId);
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
                final long[] generatedPurchaseId = new long[1];
                db.runInTransaction(() -> {
                    long purchaseId = purchaseDao.insert(purchase);
                    generatedPurchaseId[0] = purchaseId;
                    purchase.setPurchaseId((int) purchaseId);

                    if (items != null && !items.isEmpty()) {
                        for (PurchaseItem item : items) {
                            item.setPurchaseId((int) purchaseId);
                        }
                        purchaseItemDao.insertAll(items);

                        // If marked completed immediately, increment inventory stock
                        if ("Completed".equalsIgnoreCase(purchase.status)) {
                            for (PurchaseItem item : items) {
                                item.setReceivedQuantity(item.quantity);
                                Product prod = productDao.getProductById(item.productId);
                                if (prod != null) {
                                    prod.quantity += item.quantity;
                                    if (prod.quantity > 0) {
                                        prod.status = "In Stock";
                                    }
                                    productDao.update(prod);
                                }
                            }
                        }
                    }
                });
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logAddition(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_PURCHASE,
                        purchase.invoiceId != null ? purchase.invoiceId : ("PO #" + generatedPurchaseId[0]),
                        "Created purchase order " + (purchase.invoiceId != null ? purchase.invoiceId : "") + " (Total: ₹" + purchase.totalAmount + ")");
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(generatedPurchaseId[0]));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save purchase."));
                }
            }
        });
    }

    /**
     * Process receiving items against a purchase order.
     * Supports per-product batch verification and expiry checking:
     * - If batchEnabled: asks/requires batch_no and expiry_date.
     * - If existing batch matches batch_no:
     * - If expiry matches: increments batch quantity and syncs product total.
     * - If expiry does NOT match: throws exception with clear error.
     * - If batch is new: inserts new batch with expiry date.
     * - If not batchEnabled: increments product stock directly.
     */
    public void processReceive(
            int purchaseId,
            Map<Integer, Integer> receiveQtyMap,
            Map<Integer, BatchReceiveInput> batchInputMap,
            String notes,
            long userId,
            ReceiveOrderCallback callback
    ) {
        executorService.execute(() -> {
            try {
                final long[] createdRecordId = new long[1];
                final boolean[] isFullReceived = new boolean[1];
                final Purchase[] updatedPurchase = new Purchase[1];

                db.runInTransaction(() -> {
                    Purchase purchase = purchaseDao.getPurchaseById(purchaseId);
                    if (purchase == null) {
                        throw new IllegalStateException("Purchase order not found: ID " + purchaseId);
                    }

                    List<PurchaseItem> items = purchaseItemDao.getPurchaseItemsSync(purchaseId);
                    if (items == null || items.isEmpty()) {
                        throw new IllegalStateException("No items found for purchase order ID " + purchaseId);
                    }

                    String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                    // Create receive record
                    ReceiveRecord record = new ReceiveRecord(purchaseId, dateStr, userId, notes, now);
                    long recId = receiveRecordDao.insert(record);
                    createdRecordId[0] = recId;

                    boolean anyReceivedInThisBatch = false;

                    for (PurchaseItem item : items) {
                        Integer toReceive = receiveQtyMap.get(item.purchaseItemId);
                        if (toReceive != null && toReceive > 0) {
                            anyReceivedInThisBatch = true;
                            int remaining = Math.max(0, item.quantity - item.receivedQuantity);
                            int actualReceive = Math.min(toReceive, remaining);

                            int newReceivedQty = item.receivedQuantity + actualReceive;
                            purchaseItemDao.updateReceivedQuantity(item.purchaseItemId, newReceivedQty);
                            item.setReceivedQuantity(newReceivedQty);

                            // Insert receive item
                            ReceiveItem rItem = new ReceiveItem((int) recId, item.purchaseItemId, item.productId, actualReceive);
                            receiveItemDao.insert(rItem);

                            Product prod = productDao.getProductById(item.productId);
                            if (prod != null) {
                                if (prod.batchEnabled) {
                                    BatchReceiveInput batchInput = batchInputMap != null ? batchInputMap.get(item.purchaseItemId) : null;
                                    if (batchInput == null || TextUtils.isEmpty(batchInput.batchNo) || batchInput.expiryDate <= 0) {
                                        String prodName = !TextUtils.isEmpty(prod.productName) ? prod.productName : "Product #" + item.productId;
                                        throw new IllegalArgumentException("Batch number and expiry date are required for: " + prodName);
                                    }

                                    String bNo = batchInput.batchNo.trim();
                                    ProductBatch existingBatch = productBatchDao.getBatchByProductAndBatchNo(item.productId, bNo);

                                    if (existingBatch != null) {
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                        String existingExp = existingBatch.expiryDate > 0 ? sdf.format(new Date(existingBatch.expiryDate)) : "";
                                        String inputExp = batchInput.expiryDate > 0 ? sdf.format(new Date(batchInput.expiryDate)) : "";

                                        if (!existingExp.equals(inputExp)) {
                                            String prodName = !TextUtils.isEmpty(prod.productName) ? prod.productName : "Product #" + item.productId;
                                            throw new IllegalArgumentException("Batch '" + bNo + "' already exists for '" + prodName +
                                                    "' with expiry date " + existingExp + ", but entered expiry date is " + inputExp +
                                                    ". Expiry dates must match for the same batch number.");
                                        }

                                        // Expiry matches: increase batch count
                                        existingBatch.quantity += actualReceive;
                                        existingBatch.updatedAt = System.currentTimeMillis();
                                        productBatchDao.update(existingBatch);
                                    } else {
                                        // New batch
                                        ProductBatch newBatch = new ProductBatch(
                                                item.productId,
                                                bNo,
                                                actualReceive,
                                                item.unitPrice,
                                                prod.sellingPrice,
                                                batchInput.expiryDate,
                                                System.currentTimeMillis(),
                                                System.currentTimeMillis()
                                        );
                                        productBatchDao.insert(newBatch);
                                    }

                                    // Synchronize product overall quantity from batch totals
                                    int totalBatchQty = productBatchDao.sumQuantityForProduct(item.productId);
                                    prod.quantity = totalBatchQty;
                                    if (prod.quantity > 0) {
                                        prod.status = "In Stock";
                                    }
                                    productDao.update(prod);
                                } else {
                                    // Not batch enabled: increase stock count directly
                                    prod.quantity += actualReceive;
                                    if (prod.quantity > 0) {
                                        prod.status = "In Stock";
                                    }
                                    productDao.update(prod);
                                }
                            }
                        }
                    }

                    if (!anyReceivedInThisBatch) {
                        throw new IllegalArgumentException("Received quantity must be greater than 0 for at least one item.");
                    }

                    // Check total completion
                    boolean allCompleted = true;
                    boolean someReceived = false;

                    for (PurchaseItem item : items) {
                        if (item.receivedQuantity < item.quantity) {
                            allCompleted = false;
                        }
                        if (item.receivedQuantity > 0) {
                            someReceived = true;
                        }
                    }

                    String newStatus;
                    if (allCompleted) {
                        newStatus = "Completed";
                        isFullReceived[0] = true;
                    } else if (someReceived) {
                        newStatus = "Partially Received";
                        isFullReceived[0] = false;
                    } else {
                        newStatus = "Pending";
                        isFullReceived[0] = false;
                    }

                    purchaseDao.updateStatus(purchaseId, newStatus, now);
                    purchase.status = newStatus;
                    purchase.updatedAt = now;
                    updatedPurchase[0] = purchase;
                });

                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_PURCHASE,
                        "PO #" + purchaseId,
                        "Received items for purchase order #" + purchaseId);

                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(createdRecordId[0], isFullReceived[0], updatedPurchase[0]));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to process receive."));
                }
            }
        });
    }

    public void cancelPurchase(int purchaseId, PurchaseActionCallback callback) {
        executorService.execute(() -> {
            try {
                db.runInTransaction(() -> {
                    Purchase purchase = purchaseDao.getPurchaseById(purchaseId);
                    if (purchase == null) {
                        throw new IllegalStateException("Purchase order not found: ID " + purchaseId);
                    }

                    List<PurchaseItem> items = purchaseItemDao.getPurchaseItemsSync(purchaseId);
                    boolean someReceived = false;
                    if (items != null) {
                        for (PurchaseItem item : items) {
                            if (item.receivedQuantity > 0) {
                                someReceived = true;
                                break;
                            }
                        }
                    }

                    String newStatus = someReceived ? "Partially Cancelled" : "Cancelled";
                    String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
                    purchaseDao.updateStatus(purchaseId, newStatus, now);
                });

                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_PURCHASE,
                        "PO #" + purchaseId,
                        "Cancelled purchase order #" + purchaseId);

                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to cancel purchase."));
                }
            }
        });
    }

    /**
     * Load receive record details along with purchase and items for bill printing.
     */
    public void getReceiveDetailsForBill(int receiveRecordId, ReceiveDetailsCallback callback) {
        executorService.execute(() -> {
            try {
                ReceiveRecord record = receiveRecordDao.getRecordById(receiveRecordId);
                if (record == null) {
                    throw new IllegalStateException("Receive record not found: ID " + receiveRecordId);
                }
                List<ReceiveItem> items = receiveItemDao.getItemsByRecordId(receiveRecordId);
                Purchase purchase = purchaseDao.getPurchaseById(record.purchaseId);
                List<PurchaseItemWithProduct> allItems = purchaseItemDao.getPurchaseItemsWithProductSync(record.purchaseId);

                if (callback != null) {
                    mainHandler.post(() -> callback.onLoaded(record, items, purchase, allItems));
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to load receive details."));
                }
            }
        });
    }

    public void delete(Purchase purchase, PurchaseActionCallback callback) {
        executorService.execute(() -> {
            try {
                purchaseItemDao.deletePurchaseItemsByPurchaseId(purchase.purchaseId);
                purchaseDao.delete(purchase);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logDeletion(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_PURCHASE,
                        purchase.invoiceId != null ? purchase.invoiceId : ("PO #" + purchase.purchaseId),
                        "Deleted purchase order " + (purchase.invoiceId != null ? purchase.invoiceId : ""));
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to delete purchase."));
                }
            }
        });
    }

    public interface PurchaseInsertCallback {
        void onSuccess(long purchaseId);

        void onError(String message);
    }

    public interface PurchaseActionCallback {
        void onSuccess();

        void onError(String message);
    }

    public interface ReceiveOrderCallback {
        void onSuccess(long receiveRecordId, boolean isFullReceived, Purchase purchase);

        void onError(String message);
    }

    public interface ReceiveDetailsCallback {
        void onLoaded(ReceiveRecord record, List<ReceiveItem> items, Purchase purchase, List<PurchaseItemWithProduct> allItems);

        void onError(String message);
    }

    public static class BatchReceiveInput {
        public String batchNo;
        public long expiryDate;

        public BatchReceiveInput(String batchNo, long expiryDate) {
            this.batchNo = batchNo;
            this.expiryDate = expiryDate;
        }
    }
}
