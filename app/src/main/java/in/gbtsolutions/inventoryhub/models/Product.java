package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "products", indices = {@Index(value = "sku", unique = true)})
public class Product {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "product_id")
    public int productId;

    @ColumnInfo(name = "product_name")
    public String productName;

    @ColumnInfo(name = "category_id")
    public int categoryId;

    @ColumnInfo(name = "sku")
    public String sku;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "brand")
    public String brand;

    @ColumnInfo(name = "unit_of_measure")
    public String unitOfMeasure;

    @ColumnInfo(name = "reorder_level")
    public int reorderLevel;

    @ColumnInfo(name = "reorder_quantity")
    public int reorderQuantity;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "hsn_code")
    public String hsnCode;

    @ColumnInfo(name = "gst_percent")
    public double gstPercent;

    @ColumnInfo(name = "gst_category", defaultValue = "TAXABLE")
    public String gstCategory = "TAXABLE";

    @ColumnInfo(name = "uqc", defaultValue = "PCS")
    public String uqc = "PCS";

    @ColumnInfo(name = "cess_percent", defaultValue = "0.0")
    public double cessPercent = 0.0;

    @ColumnInfo(name = "default_markup_percent")
    public double defaultMarkupPercent;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @ColumnInfo(name = "unit_price")
    public double unitPrice;

    @ColumnInfo(name = "selling_price")
    public double sellingPrice;

    @ColumnInfo(name = "quantity")
    public int quantity;

    @ColumnInfo(name = "batch_enabled")
    public boolean batchEnabled = false;

    @Ignore
    public ProductBatch batch = null;

    @Ignore
    public java.util.List<ProductBatch> batches = new java.util.ArrayList<>();

    @Ignore
    public Product() {
    }

    @Ignore
    public Product(String productName, int categoryId, String sku, String description,
                   String brand, String unitOfMeasure, int reorderLevel, int reorderQuantity,
                   String status, String hsnCode, double gstPercent, double defaultMarkupPercent,
                   long createdAt, long updatedAt, double unitPrice, double sellingPrice,
                   int quantity) {
        this(productName, categoryId, sku, description, brand, unitOfMeasure, reorderLevel,
                reorderQuantity, status, hsnCode, gstPercent, defaultMarkupPercent, createdAt,
                updatedAt, unitPrice, sellingPrice, quantity, false);
    }

    public Product(String productName, int categoryId, String sku, String description,
                   String brand, String unitOfMeasure, int reorderLevel, int reorderQuantity,
                   String status, String hsnCode, double gstPercent, double defaultMarkupPercent,
                   long createdAt, long updatedAt, double unitPrice, double sellingPrice,
                   int quantity, boolean batchEnabled) {
        this.productName = productName;
        this.categoryId = categoryId;
        this.sku = sku;
        this.description = description;
        this.brand = brand;
        this.unitOfMeasure = unitOfMeasure;
        this.reorderLevel = reorderLevel;
        this.reorderQuantity = reorderQuantity;
        this.status = status;
        this.hsnCode = hsnCode;
        this.gstPercent = gstPercent;
        this.defaultMarkupPercent = defaultMarkupPercent;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.unitPrice = unitPrice;
        this.sellingPrice = sellingPrice;
        this.quantity = quantity;
        this.batchEnabled = batchEnabled;
    }

    public ProductBatch getBatch() {
        return batch;
    }

    public void setBatch(ProductBatch batch) {
        this.batch = batch;
        if (batch != null) {
            if (this.batches == null) {
                this.batches = new java.util.ArrayList<>();
            }
            boolean found = false;
            for (ProductBatch b : this.batches) {
                if (b.batchId == batch.batchId || (b.batchNo != null && b.batchNo.equalsIgnoreCase(batch.batchNo))) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                this.batches.add(batch);
            }
        }
    }

    public java.util.List<ProductBatch> getBatches() {
        if (batches == null) {
            batches = new java.util.ArrayList<>();
        }
        return batches;
    }

    public void setBatches(java.util.List<ProductBatch> batches) {
        this.batches = batches != null ? batches : new java.util.ArrayList<>();
        if ((this.batch == null || this.batch.batchId == 0) && !this.batches.isEmpty()) {
            this.batch = this.batches.get(0);
        }
    }
}
