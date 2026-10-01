package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.Buyer;

@Dao
public interface BuyerDao {
    @Insert
    long insert(Buyer buyer);

    @Update
    void update(Buyer buyer);

    @Query("SELECT * FROM buyers WHERE buyer_id = :buyerId")
    Buyer getBuyerById(int buyerId);

    @Query("SELECT * FROM buyers ORDER BY buyer_name ASC")
    LiveData<List<Buyer>> getAllBuyers();

    @Query("SELECT * FROM buyers WHERE is_active = 1 ORDER BY buyer_name ASC")
    LiveData<List<Buyer>> getActiveBuyers();

    @Query("SELECT * FROM buyers WHERE buyer_name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR gst LIKE '%' || :query || '%' OR contact_person LIKE '%' || :query || '%' OR city LIKE '%' || :query || '%' ORDER BY buyer_name ASC")
    LiveData<List<Buyer>> searchBuyers(String query);

    @Query("UPDATE buyers SET is_active = :isActive, updated_at = :updatedAt WHERE buyer_id = :buyerId")
    void updateStatus(int buyerId, boolean isActive, long updatedAt);

    @Query("SELECT COUNT(*) FROM buyers")
    int count();

    @Query("SELECT * FROM buyers")
    List<Buyer> getAllBuyersSync();
}
