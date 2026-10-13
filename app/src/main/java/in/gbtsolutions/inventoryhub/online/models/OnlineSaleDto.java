package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class OnlineSaleDto {

    @SerializedName(value = "saleId", alternate = {"sale_id", "id"})
    public int saleId;

    @SerializedName(value = "buyerId", alternate = {"buyer_id"})
    public Integer buyerId;

    @SerializedName(value = "buyerName", alternate = {"buyer_name"})
    public String buyerName;

    @SerializedName(value = "customerName", alternate = {"customer_name"})
    public String customerName;

    @SerializedName(value = "customerPhone", alternate = {"customer_phone", "phone"})
    public String customerPhone;

    @SerializedName(value = "customerGstin", alternate = {"customer_gstin", "gstin"})
    public String customerGstin;

    @SerializedName(value = "isWalkIn", alternate = {"is_walk_in"})
    public boolean isWalkIn = false;

    @SerializedName(value = "placeOfSupply", alternate = {"place_of_supply"})
    public String placeOfSupply;

    @SerializedName(value = "placeOfSupplyStateCode", alternate = {"place_of_supply_state_code"})
    public String placeOfSupplyStateCode;

    @SerializedName(value = "invoiceNo", alternate = {"invoice_no", "invoice_id", "invoiceId", "invoice_number", "bill_no"})
    public String invoiceNo;

    @SerializedName(value = "saleDate", alternate = {"sale_date", "billing_date", "billingDate", "date"})
    public String saleDate;

    @SerializedName(value = "subtotalAmount", alternate = {"subtotal_amount", "subtotal"})
    public double subtotalAmount;

    @SerializedName(value = "cgstAmount", alternate = {"cgst_amount"})
    public double cgstAmount;

    @SerializedName(value = "sgstAmount", alternate = {"sgst_amount"})
    public double sgstAmount;

    @SerializedName(value = "igstAmount", alternate = {"igst_amount"})
    public double igstAmount;

    @SerializedName(value = "taxAmount", alternate = {"tax_amount", "tax", "total_gst", "totalGst"})
    public double taxAmount;

    @SerializedName(value = "cessAmount", alternate = {"cess_amount"})
    public double cessAmount;

    @SerializedName(value = "otherCharges", alternate = {"other_charges"})
    public double otherCharges;

    @SerializedName(value = "discountAmount", alternate = {"discount_amount", "discount"})
    public double discountAmount;

    @SerializedName(value = "discountPercent", alternate = {"discount_percent"})
    public double discountPercent;

    @SerializedName(value = "totalAmount", alternate = {"total_amount"})
    public double totalAmount;

    @SerializedName(value = "netAmount", alternate = {"net_amount", "grand_total"})
    public double netAmount;

    @SerializedName(value = "paymentType", alternate = {"payment_type", "payment_method", "paymentMethod", "payment_mode"})
    public String paymentType = "Cash";

    @SerializedName(value = "paymentStatus", alternate = {"payment_status", "status"})
    public String paymentStatus = "Completed";

    @SerializedName(value = "createdBy", alternate = {"created_by"})
    public long createdBy;

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    public String createdAt;

    @SerializedName(value = "updatedAt", alternate = {"updated_at"})
    public String updatedAt;

    @SerializedName(value = "cancelledBy", alternate = {"cancelled_by"})
    public long cancelledBy;

    @SerializedName(value = "buyer")
    public OnlineBuyerDto buyer;

    @SerializedName("items")
    public List<OnlineSaleItemDto> items = new ArrayList<>();

    public static class OnlineSaleItemDto {
        @SerializedName(value = "saleItemId", alternate = {"sale_item_id"})
        public int saleItemId;

        @SerializedName(value = "saleId", alternate = {"sale_id"})
        public int saleId;

        @SerializedName(value = "productId", alternate = {"product_id"})
        public int productId;

        @SerializedName(value = "productName", alternate = {"product_name", "name"})
        public String productName;

        @SerializedName(value = "sku")
        public String sku;

        @SerializedName(value = "batchId", alternate = {"batch_id"})
        public Integer batchId;

        @SerializedName(value = "batchNo", alternate = {"batch_no"})
        public String batchNo;

        @SerializedName("quantity")
        public double quantity;

        @SerializedName(value = "unitPrice", alternate = {"unit_price", "purchase_price"})
        public double unitPrice;

        @SerializedName(value = "sellingPrice", alternate = {"selling_price", "price"})
        public double sellingPrice;

        @SerializedName(value = "hsnCode", alternate = {"hsn_code"})
        public String hsnCode;

        @SerializedName(value = "sacCode", alternate = {"sac_code"})
        public String sacCode;

        @SerializedName(value = "uqc")
        public String uqc = "PCS";

        @SerializedName(value = "taxPercent", alternate = {"tax_percent", "gst_percent", "gst_rate", "gstRate"})
        public double taxPercent;

        @SerializedName(value = "cgstRate", alternate = {"cgst_rate"})
        public double cgstRate;

        @SerializedName(value = "sgstRate", alternate = {"sgst_rate"})
        public double sgstRate;

        @SerializedName(value = "igstRate", alternate = {"igst_rate"})
        public double igstRate;

        @SerializedName(value = "cgstAmount", alternate = {"cgst_amount"})
        public double cgstAmount;

        @SerializedName(value = "sgstAmount", alternate = {"sgst_amount"})
        public double sgstAmount;

        @SerializedName(value = "igstAmount", alternate = {"igst_amount"})
        public double igstAmount;

        @SerializedName(value = "cessRate", alternate = {"cess_rate"})
        public double cessRate;

        @SerializedName(value = "cessAmount", alternate = {"cess_amount"})
        public double cessAmount;

        @SerializedName(value = "taxableValue", alternate = {"taxable_value"})
        public double taxableValue;

        @SerializedName(value = "subtotal")
        public double subtotal;

        @SerializedName(value = "discountPercent", alternate = {"discount_percent"})
        public double discountPercent;

        @SerializedName(value = "discountAmount", alternate = {"discount_amount"})
        public double discountAmount;
    }
}
