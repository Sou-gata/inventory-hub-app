package in.gbtsolutions.inventoryhub.models;

import androidx.room.Embedded;
import androidx.room.Relation;

public class SaleItemWithProduct {

    @Embedded
    public SaleItem saleItem;

    @Relation(
            parentColumn = "product_id",
            entityColumn = "product_id"
    )
    public Product product;

    @Relation(
            parentColumn = "batch_id",
            entityColumn = "batch_id"
    )
    public ProductBatch batch;

    public SaleItemWithProduct() {
    }

    public SaleItem getSaleItem() {
        return saleItem;
    }

    public void setSaleItem(SaleItem saleItem) {
        this.saleItem = saleItem;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }
}
