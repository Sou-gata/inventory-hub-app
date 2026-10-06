package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;

public class OnlineSupplierDto {

    @SerializedName(value = "supplier_id", alternate = {"supplierId", "id"})
    public int supplierId;

    @SerializedName(value = "supplier_name", alternate = {"supplierName", "name"})
    public String supplierName;

    @SerializedName(value = "contact_person", alternate = {"contactPerson"})
    public String contactPerson;

    @SerializedName(value = "phone", alternate = {"mobile", "phone_number"})
    public String phone;

    @SerializedName("email")
    public String email;

    @SerializedName("address")
    public String address;

    @SerializedName("city")
    public String city;

    @SerializedName(value = "state_code", alternate = {"stateCode"})
    public String stateCode;

    @SerializedName(value = "postal_code", alternate = {"postalCode", "pincode", "zip"})
    public String postalCode;

    @SerializedName("country")
    public String country = "India";

    @SerializedName(value = "gst", alternate = {"gstin", "gst_number"})
    public String gst;

    @SerializedName("pan")
    public String pan;

    @SerializedName("notes")
    public String notes;

    @SerializedName(value = "is_active", alternate = {"isActive"})
    public boolean isActive = true;

    @SerializedName(value = "created_at", alternate = {"createdAt"})
    public Long createdAt;

    @SerializedName(value = "updated_at", alternate = {"updatedAt"})
    public Long updatedAt;

    public OnlineSupplierDto() {
    }
}
