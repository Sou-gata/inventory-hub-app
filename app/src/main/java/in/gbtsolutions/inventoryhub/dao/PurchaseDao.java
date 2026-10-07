package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseWithSupplier;

@Dao
public interface PurchaseDao {

    @Insert
    long insert(Purchase purchase);

    @Insert
    List<Long> insertAll(List<Purchase> purchases);

    @Update
    void update(Purchase purchase);

    @Delete
    void delete(Purchase purchase);

    @Query("SELECT * FROM purchases WHERE purchase_id = :purchaseId")
    Purchase getPurchaseById(int purchaseId);

    @Query("SELECT * FROM purchases WHERE invoice_id = :invoiceId LIMIT 1")
    Purchase getPurchaseByInvoiceId(String invoiceId);

    @Query("SELECT * FROM purchases ORDER BY purchase_id DESC")
    LiveData<List<Purchase>> getAllPurchases();

    @Query("SELECT * FROM purchases WHERE supplier_id = :supplierId ORDER BY purchase_id DESC")
    LiveData<List<Purchase>> getPurchasesBySupplier(int supplierId);

    @Query("SELECT COUNT(*) FROM purchases")
    int count();

    @Transaction
    @Query("SELECT * FROM purchases ORDER BY purchase_id DESC")
    LiveData<List<PurchaseWithSupplier>> getAllPurchasesWithSupplier();

    @Transaction
    @Query("SELECT purchases.* FROM purchases " +
            "LEFT JOIN suppliers ON purchases.supplier_id = suppliers.supplier_id " +
            "WHERE (:startDate IS NULL OR :startDate = '' OR purchases.billing_date >= :startDate) " +
            "  AND (:endDate IS NULL OR :endDate = '' OR purchases.billing_date <= :endDate) " +
            "  AND (:supplierQuery IS NULL OR :supplierQuery = '' OR suppliers.supplier_name LIKE '%' || :supplierQuery || '%' OR purchases.invoice_id LIKE '%' || :supplierQuery || '%') " +
            "ORDER BY purchases.purchase_id DESC")
    LiveData<List<PurchaseWithSupplier>> getFilteredPurchases(String startDate, String endDate, String supplierQuery);

    @Transaction
    @Query("SELECT purchases.* FROM purchases " +
            "LEFT JOIN suppliers ON purchases.supplier_id = suppliers.supplier_id " +
            "WHERE purchases.status IN ('Pending', 'Partially Received') " +
            "ORDER BY purchases.purchase_id DESC")
    LiveData<List<PurchaseWithSupplier>> getPendingPurchasesWithSupplier();

    @Transaction
    @Query("SELECT purchases.* FROM purchases " +
            "LEFT JOIN suppliers ON purchases.supplier_id = suppliers.supplier_id " +
            "WHERE purchases.status IN ('Pending', 'Partially Received') " +
            "  AND (:supplierQuery IS NULL OR :supplierQuery = '' OR suppliers.supplier_name LIKE '%' || :supplierQuery || '%' OR purchases.invoice_id LIKE '%' || :supplierQuery || '%') " +
            "ORDER BY purchases.purchase_id DESC")
    LiveData<List<PurchaseWithSupplier>> getFilteredPendingPurchases(String supplierQuery);

    @Query("UPDATE purchases SET status = :status, updated_at = :updatedAt WHERE purchase_id = :purchaseId")
    void updateStatus(int purchaseId, String status, String updatedAt);

    @Query("UPDATE purchases SET status = :status, cancelled_by = :cancelledBy, updated_at = :updatedAt WHERE purchase_id = :purchaseId")
    void updateCancellation(int purchaseId, String status, long cancelledBy, String updatedAt);

    @Query("UPDATE purchases SET created_by = :userId WHERE purchase_id = :purchaseId")
    void updateCreatedBy(int purchaseId, long userId);

    @Transaction
    @Query("SELECT purchases.* FROM purchases " +
            "WHERE (:startDate IS NULL OR :startDate = '' OR billing_date >= :startDate) " +
            "  AND (:endDate IS NULL OR :endDate = '' OR billing_date <= :endDate) " +
            "ORDER BY billing_date ASC, purchase_id ASC")
    List<PurchaseWithSupplier> getPurchasesWithSupplierForExport(String startDate, String endDate);

    @Query("SELECT * FROM purchases " +
            "WHERE (:startDate IS NULL OR :startDate = '' OR billing_date >= :startDate) " +
            "  AND (:endDate IS NULL OR :endDate = '' OR billing_date <= :endDate) " +
            "ORDER BY billing_date ASC, purchase_id ASC")
    List<Purchase> getPurchasesForExport(String startDate, String endDate);

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void upsert(Purchase purchase);
}
