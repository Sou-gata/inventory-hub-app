package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class OnlineReceiveItemsRequest {

    @SerializedName("purchase_id")
    public int purchaseId;

    @SerializedName("notes")
    public String notes;

    @SerializedName("items")
    public List<ReceiveItemPayload> items = new ArrayList<>();

    public static class ReceiveItemPayload {
        @SerializedName(value = "purchase_item_id", alternate = {"purchaseItemId", "item_id"})
        public Integer purchaseItemId;

        @SerializedName(value = "product_id", alternate = {"productId"})
        public int productId;

        @SerializedName(value = "received_qty", alternate = {"receivedQty", "quantity"})
        public double receivedQty;

        @SerializedName(value = "batch_no", alternate = {"batchNo"})
        public String batchNo;

        @SerializedName(value = "expiry_date", alternate = {"expiryDate"})
        public String expiryDate;

        @SerializedName(value = "selling_price", alternate = {"sellingPrice"})
        public Double sellingPrice;

        public ReceiveItemPayload() {
        }

        public ReceiveItemPayload(int productId, double receivedQty, String batchNo, String expiryDate) {
            this(null, productId, receivedQty, batchNo, expiryDate, null);
        }

        public ReceiveItemPayload(int productId, int receivedQty, String batchNo, String expiryDate) {
            this(null, productId, (double) receivedQty, batchNo, expiryDate, null);
        }

        public ReceiveItemPayload(Integer purchaseItemId, int productId, double receivedQty, String batchNo, String expiryDate, Double sellingPrice) {
            this.purchaseItemId = purchaseItemId;
            this.productId = productId;
            this.receivedQty = receivedQty;
            this.batchNo = batchNo;
            this.expiryDate = expiryDate;
            this.sellingPrice = sellingPrice;
        }

        public ReceiveItemPayload(Integer purchaseItemId, int productId, int receivedQty, String batchNo, String expiryDate, Double sellingPrice) {
            this(purchaseItemId, productId, (double) receivedQty, batchNo, expiryDate, sellingPrice);
        }
    }
}
