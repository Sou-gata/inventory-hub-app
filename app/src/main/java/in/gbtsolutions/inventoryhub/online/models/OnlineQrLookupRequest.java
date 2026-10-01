package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;

public class OnlineQrLookupRequest {

    @SerializedName("decrypted_qr")
    public String decryptedQr;

    @SerializedName("product_id")
    public int productId;

    @SerializedName("batch_id")
    public int batchId;

    @SerializedName("selling_price")
    public double sellingPrice;

    @SerializedName("expiry_julian")
    public String expiryJulian;

    public OnlineQrLookupRequest() {
    }

    public OnlineQrLookupRequest(String decryptedQr, int productId, int batchId, double sellingPrice, String expiryJulian) {
        this.decryptedQr = decryptedQr;
        this.productId = productId;
        this.batchId = batchId;
        this.sellingPrice = sellingPrice;
        this.expiryJulian = expiryJulian;
    }
}
