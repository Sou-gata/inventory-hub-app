package in.gbtsolutions.inventoryhub.models;

import androidx.room.Embedded;
import androidx.room.Relation;

public class SaleWithBuyer {

    @Embedded
    public Sale sale;

    @Relation(
            parentColumn = "buyer_id",
            entityColumn = "buyer_id"
    )
    public Buyer buyer;

    public SaleWithBuyer() {
    }

    public Sale getSale() {
        return sale;
    }

    public void setSale(Sale sale) {
        this.sale = sale;
    }

    public Buyer getBuyer() {
        return buyer;
    }

    public void setBuyer(Buyer buyer) {
        this.buyer = buyer;
    }
}
