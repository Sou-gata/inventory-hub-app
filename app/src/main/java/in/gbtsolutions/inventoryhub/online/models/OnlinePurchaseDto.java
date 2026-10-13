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

    @SerializedName(value = "purchaseDate", alternate = {"purchase_date", "date", "billing_date", "billingDate"})
    public String purchaseDate;

    @SerializedName(value = "totalAmount", alternate = {"total_amount"})
    public double totalAmount;

    @SerializedName(value = "subtotalAmount", alternate = {"subtotal_amount"})
    public double subtotalAmount;

    @SerializedName(value = "cgstAmount", alternate = {"cgst_amount"})
    public double cgstAmount;

    @SerializedName(value = "sgstAmount", alternate = {"sgst_amount"})
    public double sgstAmount;

    @SerializedName(value = "igstAmount", alternate = {"igst_amount"})
    public double igstAmount;

    @SerializedName(value = "totalGst", alternate = {"total_gst"})
    public double totalGst;

    @SerializedName(value = "otherCharges", alternate = {"other_charges"})
    public double otherCharges;

    @SerializedName(value = "discountAmount", alternate = {"discount_amount"})
    public double discountAmount;

    @SerializedName(value = "discountPercent", alternate = {"discount_percent"})
    public double discountPercent;

    @SerializedName("status")
    public String status;

    @SerializedName(value = "createdBy", alternate = {"created_by"})
    public long createdBy;

    @SerializedName(value = "createdByName", alternate = {"created_by_name"})
    public String createdByName;

    @SerializedName(value = "createdByRole", alternate = {"created_by_role"})
    public String createdByRole;

    @SerializedName(value = "cancelledBy", alternate = {"cancelled_by"})
    public long cancelledBy;

    @SerializedName(value = "cancelledByName", alternate = {"cancelled_by_name"})
    public String cancelledByName;

    @SerializedName(value = "cancelledByRole", alternate = {"cancelled_by_role"})
    public String cancelledByRole;

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    public String createdAt;

    @SerializedName(value = "updatedAt", alternate = {"updated_at"})
    public String updatedAt;

    @SerializedName("supplier")
    public OnlineSupplierDto supplier;

    @SerializedName("items")
    public List<OnlinePurchaseItemDto> items = new ArrayList<>();

    @SerializedName("stages")
    public List<OnlineReceiveRecordDto> stages = new ArrayList<>();

    public static class OnlinePurchaseItemDto {
        @SerializedName(value = "purchaseItemId", alternate = {"purchase_item_id", "id"})
        public Integer purchaseItemId;

        @SerializedName(value = "productId", alternate = {"product_id"})
        public int productId;

        @SerializedName(value = "productName", alternate = {"product_name", "name"})
        public String productName;

        @SerializedName(value = "quantity", alternate = {"quantity_ordered", "quantityOrdered"})
        public double quantity;

        @SerializedName(value = "receivedQuantity", alternate = {"received_quantity", "quantityReceived", "quantity_received"})
        public double receivedQuantity;

        @SerializedName(value = "unitPrice", alternate = {"unit_price", "purchase_price"})
        public double unitPrice;

        @SerializedName(value = "taxPercent", alternate = {"tax_percent", "gst_percent"})
        public double taxPercent;

        @SerializedName(value = "batchNo", alternate = {"batch_no", "expected_batch_no"})
        public String batchNo;

        @SerializedName(value = "expiryDate", alternate = {"expiry_date", "expiry"})
        public String expiryDate;

        @SerializedName(value = "unitOfMeasure", alternate = {"unit_of_measure", "uom"})
        public String unitOfMeasure;

        @SerializedName(value = "batchEnabled", alternate = {"batch_enabled"})
        public boolean batchEnabled;
    }

    public static class OnlineReceiveRecordDto {
        @SerializedName(value = "receiveRecordId", alternate = {"receive_record_id", "id"})
        public int receiveRecordId;

        @SerializedName(value = "purchaseId", alternate = {"purchase_id"})
        public int purchaseId;

        @SerializedName(value = "receiveDate", alternate = {"receive_date", "date"})
        public String receiveDate;

        @SerializedName(value = "receivedBy", alternate = {"received_by"})
        public long receivedBy;

        @SerializedName(value = "receivedByName", alternate = {"received_by_name"})
        public String receivedByName;

        @SerializedName(value = "receivedByRole", alternate = {"received_by_role"})
        public String receivedByRole;

        @SerializedName(value = "notes")
        public String notes;

        @SerializedName(value = "createdAt", alternate = {"created_at"})
        public String createdAt;

        @SerializedName("items")
        public List<OnlineReceiveItemDto> items = new ArrayList<>();
    }

    public static class OnlineReceiveItemDto {
        @SerializedName(value = "receiveItemId", alternate = {"receive_item_id", "id"})
        public int receiveItemId;

        @SerializedName(value = "purchaseItemId", alternate = {"purchase_item_id"})
        public int purchaseItemId;

        @SerializedName(value = "productId", alternate = {"product_id"})
        public int productId;

        @SerializedName(value = "productName", alternate = {"product_name", "name"})
        public String productName;

        @SerializedName(value = "quantityReceived", alternate = {"quantity_received", "quantity", "received_qty"})
        public double quantityReceived;

        @SerializedName(value = "batchNo", alternate = {"batch_no"})
        public String batchNo;

        @SerializedName(value = "expiryDate", alternate = {"expiry_date"})
        public long expiryDate;

        @SerializedName(value = "sellingPrice", alternate = {"selling_price"})
        public double sellingPrice;
    }
}
