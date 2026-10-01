package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class OnlineSaleDto {

    @SerializedName(value = "saleId", alternate = {"sale_id", "id"})
    public int saleId;

    @SerializedName(value = "buyerId", alternate = {"buyer_id"})
    public Integer buyerId;

    @SerializedName(value = "buyerName", alternate = {"buyer_name", "customer_name"})
    public String buyerName;

    @SerializedName(value = "invoiceNo", alternate = {"invoice_no", "invoice_number", "bill_no"})
    public String invoiceNo;

    @SerializedName(value = "saleDate", alternate = {"sale_date", "date"})
    public String saleDate;

    @SerializedName(value = "totalAmount", alternate = {"total_amount", "subtotal"})
    public double totalAmount;

    @SerializedName(value = "taxAmount", alternate = {"tax_amount", "tax"})
    public double taxAmount;

    @SerializedName(value = "discountAmount", alternate = {"discount_amount", "discount"})
    public double discountAmount;

    @SerializedName(value = "netAmount", alternate = {"net_amount", "grand_total"})
    public double netAmount;

    @SerializedName(value = "paymentType", alternate = {"payment_type", "payment_mode"})
    public String paymentType;

    @SerializedName(value = "paymentStatus", alternate = {"payment_status", "status"})
    public String paymentStatus;

    @SerializedName("items")
    public List<OnlineSaleItemDto> items = new ArrayList<>();

    public static class OnlineSaleItemDto {
        @SerializedName(value = "productId", alternate = {"product_id"})
        public int productId;

        @SerializedName(value = "batchId", alternate = {"batch_id"})
        public Integer batchId;

        @SerializedName(value = "batchNo", alternate = {"batch_no"})
        public String batchNo;

        @SerializedName("quantity")
        public int quantity;

        @SerializedName(value = "unitPrice", alternate = {"unit_price", "purchase_price"})
        public double unitPrice;

        @SerializedName(value = "sellingPrice", alternate = {"selling_price", "price"})
        public double sellingPrice;

        @SerializedName(value = "taxPercent", alternate = {"tax_percent", "gst_percent"})
        public double taxPercent;
    }
}
