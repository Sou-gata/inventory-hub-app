package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "purchase_items")
public class PurchaseItem {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "purchase_item_id")
    public int purchaseItemId;

    @ColumnInfo(name = "purchase_id")
    public int purchaseId;

    @ColumnInfo(name = "product_id")
    public int productId;

    @ColumnInfo(name = "quantity")
    public double quantity;

    @ColumnInfo(name = "unit_price")
    public double unitPrice;

    @ColumnInfo(name = "cgst_rate")
    public double cgstRate;

    @ColumnInfo(name = "sgst_rate")
    public double sgstRate;

    @ColumnInfo(name = "igst_rate")
    public double igstRate;

    @ColumnInfo(name = "cgst_amount")
    public double cgstAmount;

    @ColumnInfo(name = "sgst_amount")
    public double sgstAmount;

    @ColumnInfo(name = "igst_amount")
    public double igstAmount;

    @ColumnInfo(name = "subtotal")
    public double subtotal;

    @ColumnInfo(name = "discount_percent")
    public double discountPercent;

    @ColumnInfo(name = "discount_amount")
    public double discountAmount;

    @ColumnInfo(name = "received_quantity", defaultValue = "0")
    public double receivedQuantity;

    @ColumnInfo(name = "updated_at", defaultValue = "0")
    public long updatedAt = System.currentTimeMillis();

    public PurchaseItem() {
    }

    @Ignore
    public PurchaseItem(
            int purchaseId,
            int productId,
            double quantity,
            double unitPrice,
            double cgstRate,
            double sgstRate,
            double igstRate,
            double cgstAmount,
            double sgstAmount,
            double igstAmount,
            double subtotal,
            double discountPercent,
            double discountAmount
    ) {
        this.purchaseId = purchaseId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.cgstRate = cgstRate;
        this.sgstRate = sgstRate;
        this.igstRate = igstRate;
        this.cgstAmount = cgstAmount;
        this.sgstAmount = sgstAmount;
        this.igstAmount = igstAmount;
        this.subtotal = subtotal;
        this.discountPercent = discountPercent;
        this.discountAmount = discountAmount;
    }

    @Ignore
    public PurchaseItem(
            int purchaseId,
            int productId,
            int quantity,
            double unitPrice,
            double cgstRate,
            double sgstRate,
            double igstRate,
            double cgstAmount,
            double sgstAmount,
            double igstAmount,
            double subtotal,
            double discountPercent,
            double discountAmount
    ) {
        this(purchaseId, productId, (double) quantity, unitPrice, cgstRate, sgstRate, igstRate,
                cgstAmount, sgstAmount, igstAmount, subtotal, discountPercent, discountAmount);
    }

    public int getPurchaseItemId() { return purchaseItemId; }
    public void setPurchaseItemId(int purchaseItemId) { this.purchaseItemId = purchaseItemId; }

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public int getProductId() { return productId; }
    public void setProductId(int productId) { this.productId = productId; }

    public double getQuantity() { return quantity; }
    public void setQuantity(double quantity) { this.quantity = quantity; }
    public void setQuantity(int quantity) { this.quantity = (double) quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public double getCgstRate() { return cgstRate; }
    public void setCgstRate(double cgstRate) { this.cgstRate = cgstRate; }

    public double getSgstRate() { return sgstRate; }
    public void setSgstRate(double sgstRate) { this.sgstRate = sgstRate; }

    public double getIgstRate() { return igstRate; }
    public void setIgstRate(double igstRate) { this.igstRate = igstRate; }

    public double getCgstAmount() { return cgstAmount; }
    public void setCgstAmount(double cgstAmount) { this.cgstAmount = cgstAmount; }

    public double getSgstAmount() { return sgstAmount; }
    public void setSgstAmount(double sgstAmount) { this.sgstAmount = sgstAmount; }

    public double getIgstAmount() { return igstAmount; }
    public void setIgstAmount(double igstAmount) { this.igstAmount = igstAmount; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { this.discountPercent = discountPercent; }

    public double getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(double discountAmount) { this.discountAmount = discountAmount; }

    public double getReceivedQuantity() { return receivedQuantity; }
    public void setReceivedQuantity(double receivedQuantity) { this.receivedQuantity = receivedQuantity; }
    public void setReceivedQuantity(int receivedQuantity) { this.receivedQuantity = (double) receivedQuantity; }
}
