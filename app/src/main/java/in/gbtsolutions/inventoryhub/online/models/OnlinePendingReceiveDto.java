package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class OnlinePendingReceiveDto {

    @SerializedName(value = "purchaseId", alternate = {"purchase_id", "id"})
    public int purchaseId;

    @SerializedName(value = "invoiceId", alternate = {"invoice_id", "invoice_no"})
    public String invoiceId;

    @SerializedName(value = "supplierName", alternate = {"supplier_name", "supplier"})
    public String supplierName;

    @SerializedName(value = "purchaseDate", alternate = {"purchase_date", "date"})
    public String purchaseDate;

    @SerializedName(value = "status")
    public String status;

    @SerializedName(value = "totalAmount", alternate = {"total_amount"})
    public double totalAmount;

    @SerializedName("items")
    public List<PendingItemDto> items = new ArrayList<>();

    public static class PendingItemDto {
        @SerializedName(value = "purchaseItemId", alternate = {"purchase_item_id", "item_id", "id"})
        public int purchaseItemId;

        @SerializedName(value = "productId", alternate = {"product_id"})
        public int productId;

        @SerializedName(value = "productName", alternate = {"product_name", "name"})
        public String productName;

        @SerializedName(value = "quantityOrdered", alternate = {"quantity_ordered", "quantity", "ordered_quantity"})
        public int quantityOrdered;

        @SerializedName(value = "quantityReceived", alternate = {"quantity_received", "received_quantity"})
        public int quantityReceived;

        @SerializedName(value = "unitPrice", alternate = {"unit_price", "purchase_price"})
        public double unitPrice;

        @SerializedName(value = "batchEnabled", alternate = {"batch_enabled", "is_batch_enabled"})
        public boolean batchEnabled;
    }
}
