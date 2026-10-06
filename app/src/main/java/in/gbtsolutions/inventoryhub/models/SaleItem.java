package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "sale_items")
public class SaleItem {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "sale_item_id")
    public int saleItemId;

    @ColumnInfo(name = "sale_id")
    public int saleId;

    @ColumnInfo(name = "product_id")
    public int productId;

    @ColumnInfo(name = "quantity")
    public double quantity;

    @ColumnInfo(name = "unit_price")
    public double unitPrice;

    @ColumnInfo(name = "hsn_code")
    public String hsnCode;

    @ColumnInfo(name = "sac_code")
    public String sacCode;

    @ColumnInfo(name = "uqc", defaultValue = "PCS")
    public String uqc = "PCS";

    @ColumnInfo(name = "gst_rate", defaultValue = "0.0")
    public double gstRate;

    @ColumnInfo(name = "cess_rate", defaultValue = "0.0")
    public double cessRate = 0.0;

    @ColumnInfo(name = "cess_amount", defaultValue = "0.0")
    public double cessAmount = 0.0;

    @ColumnInfo(name = "taxable_value", defaultValue = "0.0")
    public double taxableValue = 0.0;

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

    @ColumnInfo(name = "batch_id")
    public Integer batchId;

    @ColumnInfo(name = "updated_at", defaultValue = "0")
    public long updatedAt = System.currentTimeMillis();

    public SaleItem() {
    }

    @Ignore
    public SaleItem(
            int saleId,
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
        this.saleId = saleId;
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
    public SaleItem(
            int saleId,
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
        this(saleId, productId, (double) quantity, unitPrice, cgstRate, sgstRate, igstRate,
                cgstAmount, sgstAmount, igstAmount, subtotal, discountPercent, discountAmount);
    }

    public int getSaleItemId() {
        return saleItemId;
    }

    public void setSaleItemId(int saleItemId) {
        this.saleItemId = saleItemId;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public double getQuantity() {
        return quantity;
    }

    public void setQuantity(double quantity) {
        this.quantity = quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = (double) quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public double getCgstRate() {
        return cgstRate;
    }

    public void setCgstRate(double cgstRate) {
        this.cgstRate = cgstRate;
    }

    public double getSgstRate() {
        return sgstRate;
    }

    public void setSgstRate(double sgstRate) {
        this.sgstRate = sgstRate;
    }

    public double getIgstRate() {
        return igstRate;
    }

    public void setIgstRate(double igstRate) {
        this.igstRate = igstRate;
    }

    public double getCgstAmount() {
        return cgstAmount;
    }

    public void setCgstAmount(double cgstAmount) {
        this.cgstAmount = cgstAmount;
    }

    public double getSgstAmount() {
        return sgstAmount;
    }

    public void setSgstAmount(double sgstAmount) {
        this.sgstAmount = sgstAmount;
    }

    public double getIgstAmount() {
        return igstAmount;
    }

    public void setIgstAmount(double igstAmount) {
        this.igstAmount = igstAmount;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public Integer getBatchId() {
        return batchId;
    }

    public void setBatchId(Integer batchId) {
        this.batchId = batchId;
    }
}
