package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.ProductBatch;

@Dao
public interface ProductBatchDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(ProductBatch batch);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProductBatch> batches);

    @Update
    void update(ProductBatch batch);

    @Delete
    void delete(ProductBatch batch);

    @Query("SELECT * FROM product_batches WHERE batch_id = :batchId LIMIT 1")
    ProductBatch getBatchById(int batchId);

    @Query("SELECT * FROM product_batches WHERE product_id = :productId ORDER BY expiry_date ASC, batch_id ASC")
    LiveData<List<ProductBatch>> getAllBatchesForProduct(int productId);

    @Query("SELECT * FROM product_batches WHERE product_id = :productId ORDER BY expiry_date ASC, batch_id ASC")
    List<ProductBatch> getAllBatchesForProductSync(int productId);

    @Query("SELECT * FROM product_batches WHERE product_id = :productId AND UPPER(TRIM(batch_no)) = UPPER(TRIM(:batchNo)) LIMIT 1")
    ProductBatch getBatchByProductAndBatchNo(int productId, String batchNo);

    @Query("SELECT * FROM product_batches WHERE product_id = :productId AND quantity > 0 ORDER BY CASE WHEN expiry_date > 0 THEN expiry_date ELSE 9223372036854775807 END ASC, batch_id ASC")
    LiveData<List<ProductBatch>> getActiveBatchesForProduct(int productId);

    @Query("SELECT * FROM product_batches WHERE product_id = :productId AND quantity > 0 ORDER BY CASE WHEN expiry_date > 0 THEN expiry_date ELSE 9223372036854775807 END ASC, batch_id ASC")
    List<ProductBatch> getActiveBatchesForProductSync(int productId);

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM product_batches WHERE product_id = :productId")
    int sumQuantityForProduct(int productId);

    @Query("SELECT MIN(expiry_date) FROM product_batches WHERE product_id = :productId AND quantity > 0 AND expiry_date > 0")
    Long getEarliestExpiryForProduct(int productId);

    static class ProductExpiryTuple {
        public int product_id;
        public long earliest_expiry;
    }

    @Query("SELECT product_id, MIN(expiry_date) as earliest_expiry FROM product_batches WHERE quantity > 0 AND expiry_date > 0 GROUP BY product_id")
    LiveData<List<ProductExpiryTuple>> getAllEarliestExpiries();

    @Query("SELECT COUNT(*) FROM product_batches WHERE created_at >= :startOfDay AND created_at <= :endOfDay")
    int countBatchesCreatedBetween(long startOfDay, long endOfDay);

    @Query("DELETE FROM product_batches WHERE product_id = :productId")
    void deleteAllForProduct(int productId);

    @Query("SELECT COUNT(*) FROM product_batches")
    int count();
}
