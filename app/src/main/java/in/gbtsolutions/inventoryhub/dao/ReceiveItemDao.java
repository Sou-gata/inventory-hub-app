package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.ReceiveItem;

@Dao
public interface ReceiveItemDao {

    @Insert
    long insert(ReceiveItem item);

    @Insert
    void insertAll(List<ReceiveItem> items);

    @Query("SELECT * FROM receive_items WHERE receive_record_id = :recordId")
    List<ReceiveItem> getItemsByRecordId(int recordId);

    @Query("SELECT * FROM receive_items WHERE receive_record_id = :recordId")
    LiveData<List<ReceiveItem>> getItemsByRecordIdLive(int recordId);
}
