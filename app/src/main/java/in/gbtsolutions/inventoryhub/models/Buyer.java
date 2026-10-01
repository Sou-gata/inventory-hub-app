package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "buyers", indices = {
        @Index(value = "phone"),
        @Index(value = "email"),
        @Index(value = "gst")
})
public class Buyer {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "buyer_id")
    public int buyerId;

    @ColumnInfo(name = "buyer_name")
    public String buyerName;

    @ColumnInfo(name = "contact_person")
    public String contactPerson;

    @ColumnInfo(name = "phone")
    public String phone;

    @ColumnInfo(name = "email")
    public String email;

    @ColumnInfo(name = "address")
    public String address;

    @ColumnInfo(name = "city")
    public String city;

    @ColumnInfo(name = "state_code")
    public String stateCode;

    @ColumnInfo(name = "billing_state_code")
    public String billingStateCode;

    @ColumnInfo(name = "shipping_state_code")
    public String shippingStateCode;

    @ColumnInfo(name = "is_registered", defaultValue = "0")
    public boolean isRegistered = false;

    @ColumnInfo(name = "postal_code")
    public String postalCode;

    @ColumnInfo(name = "country")
    public String country;

    @ColumnInfo(name = "gst")
    public String gst;

    @ColumnInfo(name = "pan")
    public String pan;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "is_active", defaultValue = "1")
    public boolean isActive = true;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;
    public Buyer() {
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    @Ignore
    public Buyer(String buyerName, String contactPerson, String phone, String email, String address, String city, String stateCode, String postalCode, String country, String gst, String pan, String notes) {
        this(buyerName, contactPerson, phone, email, address, city, stateCode, postalCode, country, gst, pan, notes, true);
    }

    @Ignore
    public Buyer(String buyerName, String contactPerson, String phone, String email, String address, String city, String stateCode, String postalCode, String country, String gst, String pan, String notes, boolean isActive) {
        this.buyerName = buyerName;
        this.contactPerson = contactPerson;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.city = city;
        this.stateCode = stateCode;
        this.postalCode = postalCode;
        this.country = country;
        this.gst = gst;
        this.pan = pan;
        this.notes = notes;
        this.isActive = isActive;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
