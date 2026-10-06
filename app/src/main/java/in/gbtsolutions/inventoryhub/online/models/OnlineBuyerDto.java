package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;

public class OnlineBuyerDto {

    @SerializedName(value = "buyer_id", alternate = {"buyerId", "id"})
    public int buyerId;

    @SerializedName(value = "buyer_name", alternate = {"buyerName", "name"})
    public String buyerName;

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

    @SerializedName(value = "billing_state_code", alternate = {"billingStateCode"})
    public String billingStateCode;

    @SerializedName(value = "shipping_state_code", alternate = {"shipping_state_code", "shippingStateCode"})
    public String shippingStateCode;

    @SerializedName(value = "is_registered", alternate = {"isRegistered"})
    public boolean isRegistered = false;

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

    public OnlineBuyerDto() {
    }
}
