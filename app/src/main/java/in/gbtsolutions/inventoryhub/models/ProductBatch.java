package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "product_batches",
        foreignKeys = @ForeignKey(
                entity = Product.class,
                parentColumns = "product_id",
                childColumns = "product_id",
                onDelete = ForeignKey.CASCADE
        ),
        indices = {
                @Index(value = "product_id"),
                @Index(value = {"product_id", "batch_no"})
        }
)
public class ProductBatch {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "batch_id")
    public int batchId;

    @ColumnInfo(name = "product_id")
    public int productId;

    @ColumnInfo(name = "batch_no")
    public String batchNo;

    @ColumnInfo(name = "quantity")
    public int quantity;

    @ColumnInfo(name = "purchase_price")
    public double purchasePrice;

    @ColumnInfo(name = "selling_price")
    public double sellingPrice;

    @ColumnInfo(name = "expiry_date")
    public long expiryDate; // 0 if no expiry date

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public ProductBatch() {
    }

    @androidx.room.Ignore
    public ProductBatch(int productId, String batchNo, int quantity,
                        double purchasePrice, double sellingPrice,
                        long expiryDate, long createdAt, long updatedAt) {
        this.productId = productId;
        this.batchNo = batchNo;
        this.quantity = quantity;
        this.purchasePrice = purchasePrice;
        this.sellingPrice = sellingPrice;
        this.expiryDate = expiryDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isExpired() {
        return expiryDate > 0 && expiryDate < System.currentTimeMillis();
    }
}
