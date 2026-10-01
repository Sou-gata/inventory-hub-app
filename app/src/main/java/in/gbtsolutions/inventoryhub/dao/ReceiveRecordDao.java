package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.ReceiveRecord;
import in.gbtsolutions.inventoryhub.models.ReceiveRecordWithItems;

@Dao
public interface ReceiveRecordDao {

    @Insert
    long insert(ReceiveRecord record);

    @Query("SELECT * FROM receive_records WHERE receive_record_id = :recordId")
    ReceiveRecord getRecordById(int recordId);

    @Query("SELECT * FROM receive_records WHERE purchase_id = :purchaseId ORDER BY receive_record_id DESC")
    LiveData<List<ReceiveRecord>> getRecordsByPurchaseId(int purchaseId);

    @Transaction
    @Query("SELECT * FROM receive_records WHERE purchase_id = :purchaseId ORDER BY receive_record_id ASC")
    LiveData<List<ReceiveRecordWithItems>> getRecordsWithItemsByPurchaseId(int purchaseId);

    @Transaction
    @Query("SELECT * FROM receive_records WHERE purchase_id = :purchaseId ORDER BY receive_record_id ASC")
    List<ReceiveRecordWithItems> getRecordsWithItemsByPurchaseIdSync(int purchaseId);

    @Query("SELECT * FROM receive_records WHERE purchase_id = :purchaseId ORDER BY receive_record_id DESC LIMIT 1")
    ReceiveRecord getLatestRecordByPurchaseId(int purchaseId);
}
