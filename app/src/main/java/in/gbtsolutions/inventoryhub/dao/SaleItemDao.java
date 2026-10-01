package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.SaleItem;
import in.gbtsolutions.inventoryhub.models.SaleItemWithProduct;

@Dao
public interface SaleItemDao {

    @Insert
    long insert(SaleItem saleItem);

    @Insert
    void insertAll(List<SaleItem> saleItems);

    @Update
    void update(SaleItem saleItem);

    @Delete
    void delete(SaleItem saleItem);

    @Query("SELECT * FROM sale_items WHERE sale_item_id = :saleItemId")
    SaleItem getSaleItemById(int saleItemId);

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId")
    LiveData<List<SaleItem>> getItemsBySaleId(int saleId);

    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId")
    List<SaleItem> getItemsBySaleIdSync(int saleId);

    @Query("SELECT * FROM sale_items WHERE product_id = :productId")
    LiveData<List<SaleItem>> getItemsByProductId(int productId);

    @Query("DELETE FROM sale_items WHERE sale_id = :saleId")
    void deleteBySaleId(int saleId);

    @Query("SELECT COUNT(*) FROM sale_items")
    int count();

    @Transaction
    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId")
    LiveData<List<SaleItemWithProduct>> getSaleItemsWithProduct(int saleId);

    @Transaction
    @Query("SELECT * FROM sale_items WHERE sale_id = :saleId")
    List<in.gbtsolutions.inventoryhub.models.SaleItemWithProduct> getSaleItemsWithProductSync(int saleId);

    @Query("SELECT * FROM sale_items WHERE sale_id IN (:saleIds)")
    List<SaleItem> getItemsBySaleIds(List<Integer> saleIds);

    @Transaction
    @Query("SELECT * FROM sale_items")
    List<in.gbtsolutions.inventoryhub.models.SaleItemWithProduct> getAllSaleItemsWithProductSync();

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void upsert(SaleItem item);
}
