package in.gbtsolutions.inventoryhub.models;

import androidx.room.Embedded;
import androidx.room.Relation;

public class PurchaseItemWithProduct {

    @Embedded
    public PurchaseItem purchaseItem;

    @Relation(
            parentColumn = "product_id",
            entityColumn = "product_id"
    )
    public Product product;
}
