package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "receive_records")
public class ReceiveRecord {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "receive_record_id")
    public int receiveRecordId;

    @ColumnInfo(name = "purchase_id")
    public int purchaseId;

    @ColumnInfo(name = "receive_date")
    public String receiveDate;

    @ColumnInfo(name = "received_by")
    public long receivedBy;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "created_at")
    public String createdAt;

    public ReceiveRecord() {
    }

    @Ignore
    public ReceiveRecord(int purchaseId, String receiveDate, long receivedBy, String notes, String createdAt) {
        this.purchaseId = purchaseId;
        this.receiveDate = receiveDate;
        this.receivedBy = receivedBy;
        this.notes = notes;
        this.createdAt = createdAt;
    }

    public int getReceiveRecordId() { return receiveRecordId; }
    public void setReceiveRecordId(int receiveRecordId) { this.receiveRecordId = receiveRecordId; }

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public String getReceiveDate() { return receiveDate; }
    public void setReceiveDate(String receiveDate) { this.receiveDate = receiveDate; }

    public long getReceivedBy() { return receivedBy; }
    public void setReceivedBy(long receivedBy) { this.receivedBy = receivedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
