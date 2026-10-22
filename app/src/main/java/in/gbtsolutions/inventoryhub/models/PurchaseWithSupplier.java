package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Ignore;
import androidx.room.Relation;

import java.util.List;

public class PurchaseWithSupplier {

    @Embedded
    public Purchase purchase;

    @Relation(parentColumn = "supplier_id", entityColumn = "supplier_id")
    public Suppliers supplier;

    @ColumnInfo(name = "created_by_name")
    public String createdByName;

    @ColumnInfo(name = "created_by_role")
    public String createdByRole;

    @Ignore
    public List<PurchaseItemWithProduct> items;

    @Ignore
    public List<ReceiveRecordWithItems> stages;
}
