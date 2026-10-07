package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "sales")
public class Sale {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "sale_id")
    public int saleId;

    @ColumnInfo(name = "buyer_id")
    public int buyerId;

    @ColumnInfo(name = "customer_name")
    public String customerName;

    @ColumnInfo(name = "customer_phone")
    public String customerPhone;

    @ColumnInfo(name = "customer_gstin")
    public String customerGstin;

    @ColumnInfo(name = "is_walk_in", defaultValue = "0")
    public boolean isWalkIn = false;

    @ColumnInfo(name = "place_of_supply")
    public String placeOfSupply;

    @ColumnInfo(name = "place_of_supply_state_code")
    public String placeOfSupplyStateCode;

    @ColumnInfo(name = "cess_amount", defaultValue = "0.0")
    public double cessAmount = 0.0;

    @ColumnInfo(name = "invoice_id")
    public String invoiceId;

    @ColumnInfo(name = "subtotal_amount")
    public double subtotalAmount;

    @ColumnInfo(name = "cgst_amount")
    public double cgstAmount;

    @ColumnInfo(name = "sgst_amount")
    public double sgstAmount;

    @ColumnInfo(name = "igst_amount")
    public double igstAmount;

    @ColumnInfo(name = "total_gst")
    public double totalGst;

    @ColumnInfo(name = "other_charges")
    public double otherCharges;

    @ColumnInfo(name = "total_amount")
    public double totalAmount;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "created_by")
    public long createdBy;

    @ColumnInfo(name = "billing_date")
    public String billingDate;

    @ColumnInfo(name = "created_at")
    public String createdAt;

    @ColumnInfo(name = "updated_at")
    public String updatedAt;

    @ColumnInfo(name = "discount_amount")
    public double discountAmount;

    @ColumnInfo(name = "discount_percent")
    public double discountPercent;

    @ColumnInfo(name = "payment_method", defaultValue = "'Cash'")
    public String paymentMethod = "Cash";

    @ColumnInfo(name = "cancelled_by", defaultValue = "0")
    public long cancelledBy = 0;

    public Sale() {
    }

    @Ignore
    public Sale(
            int buyerId,
            String invoiceId,
            double subtotalAmount,
            double cgstAmount,
            double sgstAmount,
            double igstAmount,
            double totalGst,
            double totalAmount,
            String status,
            long createdBy,
            String billingDate,
            String createdAt,
            String updatedAt,
            double discountAmount,
            double discountPercent
    ) {
        this.buyerId = buyerId;
        this.invoiceId = invoiceId;
        this.subtotalAmount = subtotalAmount;
        this.cgstAmount = cgstAmount;
        this.sgstAmount = sgstAmount;
        this.igstAmount = igstAmount;
        this.totalGst = totalGst;
        this.otherCharges = 0.0;
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdBy = createdBy;
        this.billingDate = billingDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.discountAmount = discountAmount;
        this.discountPercent = discountPercent;
    }

    @Ignore
    public Sale(
            int buyerId,
            String invoiceId,
            double subtotalAmount,
            double cgstAmount,
            double sgstAmount,
            double igstAmount,
            double totalGst,
            double otherCharges,
            double totalAmount,
            String status,
            long createdBy,
            String billingDate,
            String createdAt,
            String updatedAt,
            double discountAmount,
            double discountPercent
    ) {
        this(buyerId, invoiceId, subtotalAmount, cgstAmount, sgstAmount, igstAmount, totalGst, totalAmount, status, createdBy, billingDate, createdAt, updatedAt, discountAmount, discountPercent);
        this.otherCharges = otherCharges;
    }

    @Ignore
    public Sale(
            int buyerId,
            String invoiceId,
            double subtotalAmount,
            double cgstAmount,
            double sgstAmount,
            double igstAmount,
            double totalGst,
            double otherCharges,
            double totalAmount,
            String status,
            long createdBy,
            String billingDate,
            String createdAt,
            String updatedAt,
            double discountAmount,
            double discountPercent,
            String paymentMethod
    ) {
        this(buyerId, invoiceId, subtotalAmount, cgstAmount, sgstAmount, igstAmount, totalGst, otherCharges, totalAmount, status, createdBy, billingDate, createdAt, updatedAt, discountAmount, discountPercent);
        this.paymentMethod = paymentMethod;
    }

    public int getSaleId() {
        return saleId;
    }

    public void setSaleId(int saleId) {
        this.saleId = saleId;
    }

    public int getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(int buyerId) {
        this.buyerId = buyerId;
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public void setInvoiceId(String invoiceId) {
        this.invoiceId = invoiceId;
    }

    public double getSubtotalAmount() {
        return subtotalAmount;
    }

    public void setSubtotalAmount(double subtotalAmount) {
        this.subtotalAmount = subtotalAmount;
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

    public double getTotalGst() {
        return totalGst;
    }

    public void setTotalGst(double totalGst) {
        this.totalGst = totalGst;
    }

    public double getOtherCharges() {
        return otherCharges;
    }

    public void setOtherCharges(double otherCharges) {
        this.otherCharges = otherCharges;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(long createdBy) {
        this.createdBy = createdBy;
    }

    public String getBillingDate() {
        return billingDate;
    }

    public void setBillingDate(String billingDate) {
        this.billingDate = billingDate;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(double discountPercent) {
        this.discountPercent = discountPercent;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public long getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(long cancelledBy) {
        this.cancelledBy = cancelledBy;
    }
}