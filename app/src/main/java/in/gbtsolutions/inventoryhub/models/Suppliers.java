package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "suppliers", indices = {
        @Index(value = "phone", unique = true),
        @Index(value = "email", unique = true),
        @Index(value = "gst", unique = true)
})
public class Suppliers {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "supplier_id")
    public int supplierId;

    @ColumnInfo(name = "supplier_name")
    public String supplierName;

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
    public Suppliers() {
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    @Ignore
    public Suppliers(String supplierName, String contactPerson, String phone, String email, String address, String city, String stateCode, String postalCode, String country, String gst, String pan, String notes) {
        this(supplierName, contactPerson, phone, email, address, city, stateCode, postalCode, country, gst, pan, notes, true);
    }

    @Ignore
    public Suppliers(String supplierName, String contactPerson, String phone, String email, String address, String city, String stateCode, String postalCode, String country, String gst, String pan, String notes, boolean isActive) {
        this.supplierName = supplierName;
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
