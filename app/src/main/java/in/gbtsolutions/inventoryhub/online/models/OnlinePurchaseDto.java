package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class OnlinePurchaseDto {

    @SerializedName(value = "purchaseId", alternate = {"purchase_id", "id"})
    public int purchaseId;

    @SerializedName(value = "supplierId", alternate = {"supplier_id"})
    public Integer supplierId;

    @SerializedName(value = "supplierName", alternate = {"supplier_name"})
    public String supplierName;

    @SerializedName(value = "invoiceId", alternate = {"invoice_id", "invoice_no"})
    public String invoiceId;

    @SerializedName(value = "purchaseDate", alternate = {"purchase_date", "date"})
    public String purchaseDate;

    @SerializedName(value = "totalAmount", alternate = {"total_amount"})
    public double totalAmount;

    @SerializedName("status")
    public String status;

    @SerializedName("items")
    public List<OnlinePurchaseItemDto> items = new ArrayList<>();

    public static class OnlinePurchaseItemDto {
        @SerializedName(value = "purchaseItemId", alternate = {"purchase_item_id", "id"})
        public Integer purchaseItemId;

        @SerializedName(value = "productId", alternate = {"product_id"})
        public int productId;

        @SerializedName("quantity")
        public int quantity;

        @SerializedName(value = "unitPrice", alternate = {"unit_price", "purchase_price"})
        public double unitPrice;

        @SerializedName(value = "taxPercent", alternate = {"tax_percent", "gst_percent"})
        public double taxPercent;

        @SerializedName(value = "batchNo", alternate = {"batch_no", "expected_batch_no"})
        public String batchNo;

        @SerializedName(value = "expiryDate", alternate = {"expiry_date", "expiry"})
        public String expiryDate;
    }
}
