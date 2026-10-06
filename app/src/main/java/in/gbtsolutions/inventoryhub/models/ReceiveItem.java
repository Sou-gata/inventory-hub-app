package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "receive_items")
public class ReceiveItem {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "receive_item_id")
    public int receiveItemId;

    @ColumnInfo(name = "receive_record_id")
    public int receiveRecordId;

    @ColumnInfo(name = "purchase_item_id")
    public int purchaseItemId;

    @ColumnInfo(name = "product_id")
    public int productId;

    @ColumnInfo(name = "quantity_received")
    public double quantityReceived;

    public ReceiveItem() {
    }

    @Ignore
    public ReceiveItem(int receiveRecordId, int purchaseItemId, int productId, double quantityReceived) {
        this.receiveRecordId = receiveRecordId;
        this.purchaseItemId = purchaseItemId;
        this.productId = productId;
        this.quantityReceived = quantityReceived;
    }

    @Ignore
    public ReceiveItem(int receiveRecordId, int purchaseItemId, int productId, int quantityReceived) {
        this(receiveRecordId, purchaseItemId, productId, (double) quantityReceived);
    }

    public int getReceiveItemId() { return receiveItemId; }
    public void setReceiveItemId(int receiveItemId) { this.receiveItemId = receiveItemId; }

    public int getReceiveRecordId() { return receiveRecordId; }
    public void setReceiveRecordId(int receiveRecordId) { this.receiveRecordId = receiveRecordId; }

    public int getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(int purchaseItemId) { this.purchaseItemId = purchaseItemId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public double getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(double quantityReceived) { this.quantityReceived = quantityReceived; }
    public void setQuantityReceived(int quantityReceived) { this.quantityReceived = (double) quantityReceived; }
}
