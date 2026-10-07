package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleWithBuyer;

@Dao
public interface SaleDao {

    @Insert
    long insert(Sale sale);

    @Insert
    List<Long> insertAll(List<Sale> sales);

    @Update
    void update(Sale sale);

    @Delete
    void delete(Sale sale);

    @Query("SELECT * FROM sales WHERE sale_id = :saleId")
    Sale getSaleById(int saleId);

    @Query("SELECT * FROM sales WHERE invoice_id = :invoiceId LIMIT 1")
    Sale getSaleByInvoiceId(String invoiceId);

    @Query("SELECT * FROM sales ORDER BY sale_id DESC")
    LiveData<List<Sale>> getAllSales();

    @Query("SELECT * FROM sales WHERE buyer_id = :buyerId ORDER BY sale_id DESC")
    LiveData<List<Sale>> getSalesByBuyer(int buyerId);

    @Query("SELECT * FROM sales WHERE billing_date = :billingDate ORDER BY sale_id DESC")
    LiveData<List<Sale>> getSalesByDate(String billingDate);

    @Query("SELECT * FROM sales WHERE status = :status ORDER BY sale_id DESC")
    LiveData<List<Sale>> getSalesByStatus(String status);

    @Query("SELECT * FROM sales WHERE invoice_id LIKE '%' || :query || '%' OR customer_name LIKE '%' || :query || '%' OR (customer_phone IS NOT NULL AND customer_phone LIKE '%' || :query || '%') ORDER BY sale_id DESC")
    LiveData<List<Sale>> searchSales(String query);

    @Query("SELECT COUNT(*) FROM sales")
    int count();

    @Transaction
    @Query("SELECT * FROM sales ORDER BY sale_id DESC")
    LiveData<List<SaleWithBuyer>> getAllSalesWithBuyer();

    @Transaction
    @Query("SELECT sales.* FROM sales " +
            "LEFT JOIN buyers ON sales.buyer_id = buyers.buyer_id " +
            "WHERE (:startDate IS NULL OR :startDate = '' OR sales.billing_date >= :startDate) " +
            "  AND (:endDate IS NULL OR :endDate = '' OR sales.billing_date <= :endDate) " +
            "  AND (:buyerQuery IS NULL OR :buyerQuery = '' OR buyers.buyer_name LIKE '%' || :buyerQuery || '%' OR sales.customer_name LIKE '%' || :buyerQuery || '%' OR sales.invoice_id LIKE '%' || :buyerQuery || '%' OR (sales.customer_phone IS NOT NULL AND sales.customer_phone LIKE '%' || :buyerQuery || '%')) " +
            "ORDER BY sales.sale_id DESC")
    LiveData<List<SaleWithBuyer>> getFilteredSales(String startDate, String endDate, String buyerQuery);

    @Transaction
    @Query("SELECT sales.* FROM sales " +
            "WHERE (:startDate IS NULL OR :startDate = '' OR billing_date >= :startDate) " +
            "  AND (:endDate IS NULL OR :endDate = '' OR billing_date <= :endDate) " +
            "ORDER BY billing_date ASC, sale_id ASC")
    List<SaleWithBuyer> getSalesWithBuyerForExport(String startDate, String endDate);

    @Query("SELECT * FROM sales " +
            "WHERE (:startDate IS NULL OR :startDate = '' OR billing_date >= :startDate) " +
            "  AND (:endDate IS NULL OR :endDate = '' OR billing_date <= :endDate) " +
            "ORDER BY billing_date ASC, sale_id ASC")
    List<Sale> getSalesForExport(String startDate, String endDate);

    @Query("SELECT COUNT(*) FROM sales " +
            "WHERE (:startDate IS NULL OR :startDate = '' OR billing_date >= :startDate) " +
            "  AND (:endDate IS NULL OR :endDate = '' OR billing_date <= :endDate) " +
            "  AND status = 'Cancelled'")
    int countCancelledSales(String startDate, String endDate);

    @Query("UPDATE sales SET status = :status, cancelled_by = :cancelledBy, updated_at = :updatedAt WHERE sale_id = :saleId")
    void updateCancellation(int saleId, String status, long cancelledBy, String updatedAt);

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void upsert(Sale sale);
}
