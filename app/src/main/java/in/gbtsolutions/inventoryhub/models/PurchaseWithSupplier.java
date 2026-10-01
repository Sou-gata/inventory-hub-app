package in.gbtsolutions.inventoryhub.models;

import androidx.room.Embedded;
import androidx.room.Relation;

public class PurchaseWithSupplier {

    @Embedded
    public Purchase purchase;

    @Relation(
            parentColumn = "supplier_id",
            entityColumn = "supplier_id"
    )
    public Suppliers supplier;
}
