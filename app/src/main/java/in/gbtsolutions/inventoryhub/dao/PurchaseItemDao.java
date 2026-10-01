package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.PurchaseItem;
import in.gbtsolutions.inventoryhub.models.PurchaseItemWithProduct;

@Dao
public interface PurchaseItemDao {

    @Insert
    void insert(PurchaseItem purchaseItem);

    @Insert
    void insertAll(List<PurchaseItem> purchaseItems);

    @Query("SELECT * FROM purchase_items WHERE purchase_id = :purchaseId")
    LiveData<List<PurchaseItem>> getPurchaseItemsByPurchaseId(int purchaseId);

    @Query("SELECT * FROM purchase_items WHERE purchase_id = :purchaseId")
    List<PurchaseItem> getPurchaseItemsSync(int purchaseId);

    @Transaction
    @Query("SELECT * FROM purchase_items WHERE purchase_id = :purchaseId")
    LiveData<List<PurchaseItemWithProduct>> getPurchaseItemsWithProduct(int purchaseId);

    @Transaction
    @Query("SELECT * FROM purchase_items WHERE purchase_id = :purchaseId")
    List<PurchaseItemWithProduct> getPurchaseItemsWithProductSync(int purchaseId);

    @Query("UPDATE purchase_items SET received_quantity = :receivedQuantity WHERE purchase_item_id = :purchaseItemId")
    void updateReceivedQuantity(int purchaseItemId, int receivedQuantity);

    @Query("DELETE FROM purchase_items WHERE purchase_id = :purchaseId")
    void deletePurchaseItemsByPurchaseId(int purchaseId);

    @Query("SELECT * FROM purchase_items WHERE purchase_id IN (:purchaseIds)")
    List<PurchaseItem> getItemsByPurchaseIds(List<Integer> purchaseIds);

    @Transaction
    @Query("SELECT * FROM purchase_items")
    List<PurchaseItemWithProduct> getAllPurchaseItemsWithProductSync();

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void upsert(PurchaseItem item);
}
