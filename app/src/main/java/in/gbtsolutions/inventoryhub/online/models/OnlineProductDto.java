package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;

public class OnlineProductDto {

    @SerializedName(value = "productId", alternate = {"product_id", "id", "_id"})
    public int productId;

    @SerializedName(value = "uuid", alternate = {"product_uuid"})
    public String uuid;

    @SerializedName(value = "productName", alternate = {"product_name", "name", "title"})
    public String productName;

    @SerializedName(value = "categoryId", alternate = {"category_id", "cat_id"})
    public int categoryId;

    @SerializedName(value = "categoryName", alternate = {"category_name", "category"})
    public String categoryName;

    @SerializedName(value = "sku", alternate = {"product_sku", "code"})
    public String sku;

    @SerializedName(value = "description", alternate = {"desc", "product_description"})
    public String description;

    @SerializedName(value = "brand", alternate = {"brand_name"})
    public String brand;

    @SerializedName(value = "unitOfMeasure", alternate = {"unit_of_measure", "unit", "uom"})
    public String unitOfMeasure = "PCS";

    @SerializedName(value = "reorderLevel", alternate = {"reorder_level", "min_stock"})
    public int reorderLevel = 0;

    @SerializedName(value = "reorderQuantity", alternate = {"reorder_quantity"})
    public int reorderQuantity = 0;

    @SerializedName(value = "status", alternate = {"product_status"})
    public String status = "ACTIVE";

    @SerializedName(value = "hsnCode", alternate = {"hsn_code", "hsn"})
    public String hsnCode;

    @SerializedName(value = "gstPercent", alternate = {"gst_percent", "tax_rate", "tax_percent", "gst"})
    public double gstPercent = 0.0;

    @SerializedName(value = "gstCategory", alternate = {"gst_category"})
    public String gstCategory = "TAXABLE";

    @SerializedName(value = "uqc")
    public String uqc = "PCS";

    @SerializedName(value = "cessPercent", alternate = {"cess_percent", "cess"})
    public double cessPercent = 0.0;

    @SerializedName(value = "defaultMarkupPercent", alternate = {"default_markup_percent", "markup"})
    public double defaultMarkupPercent = 0.0;

    @SerializedName(value = "unitPrice", alternate = {"unit_price", "purchase_price", "cost_price", "cost"})
    public double unitPrice = 0.0;

    @SerializedName(value = "sellingPrice", alternate = {"selling_price", "sale_price", "price"})
    public double sellingPrice = 0.0;

    @SerializedName(value = "quantity", alternate = {"stock", "qty", "current_stock"})
    public double quantity = 0;

    @SerializedName(value = "batchEnabled", alternate = {"batch_enabled", "is_batch_enabled"})
    public boolean batchEnabled = false;

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    public Long createdAt;

    @SerializedName(value = "updatedAt", alternate = {"updated_at"})
    public Long updatedAt;

    @SerializedName(value = "batchId", alternate = {"batch_id"})
    public Integer batchId;

    @SerializedName(value = "batchNo", alternate = {"batch_no"})
    public String batchNo;

    @SerializedName(value = "batchQuantity", alternate = {"batch_quantity", "batch_qty"})
    public Double batchQuantity;

    @SerializedName(value = "expiryDate", alternate = {"expiry_date", "expiry"})
    public Long expiryDate;

    @SerializedName(value = "batches", alternate = {"batchList", "productBatches", "batch_list"})
    public java.util.List<OnlineBatchDto> batches;

    public static class OnlineBatchDto {
        @SerializedName(value = "batchId", alternate = {"batch_id", "id"})
        public Integer batchId;

        @SerializedName(value = "productId", alternate = {"product_id"})
        public Integer productId;

        @SerializedName(value = "batchNo", alternate = {"batch_no", "batch_number"})
        public String batchNo;

        @SerializedName(value = "quantity", alternate = {"qty", "stock", "batchQuantity", "batch_quantity"})
        public double quantity;

        @SerializedName(value = "purchasePrice", alternate = {"purchase_price", "cost_price", "unit_price", "unitPrice"})
        public double purchasePrice;

        @SerializedName(value = "sellingPrice", alternate = {"selling_price", "sale_price"})
        public double sellingPrice;

        @SerializedName(value = "expiryDate", alternate = {"expiry_date", "expiry"})
        public Long expiryDate;

        @SerializedName(value = "status")
        public String status = "ACTIVE";

        public OnlineBatchDto() {}

        public OnlineBatchDto(Integer batchId, Integer productId, String batchNo, double quantity,
                              double purchasePrice, double sellingPrice, Long expiryDate) {
            this.batchId = batchId;
            this.productId = productId;
            this.batchNo = batchNo;
            this.quantity = quantity;
            this.purchasePrice = purchasePrice;
            this.sellingPrice = sellingPrice;
            this.expiryDate = expiryDate;
        }
    }

    public OnlineProductDto() {
    }
}
