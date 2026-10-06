package in.gbtsolutions.inventoryhub.models;

import androidx.annotation.NonNull;

import java.util.Objects;

public class CartItem {
    public enum DiscountType {
        RUPEE,
        PERCENT
    }

    public Product product;
    public double quantity;
    public double sellingPrice;
    public double discountValue = 0.0;
    public DiscountType discountType = DiscountType.PERCENT;

    // Batch Inventory support
    public Integer batchId = null;
    public String batchNo = null;
    public double batchAvailableQty = Double.MAX_VALUE;

    public CartItem(@NonNull Product product) {
        this.product = product;
        this.quantity = 1.0;
        this.sellingPrice = product.sellingPrice;
        this.discountValue = 0.0;
        this.discountType = DiscountType.PERCENT;
    }

    public CartItem(@NonNull Product product, double quantity, double sellingPrice) {
        this.product = product;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;
        this.discountValue = 0.0;
        this.discountType = DiscountType.PERCENT;
    }

    public CartItem(@NonNull Product product, int quantity, double sellingPrice) {
        this(product, (double) quantity, sellingPrice);
    }

    public CartItem(@NonNull Product product, ProductBatch batch, double quantity, double sellingPrice) {
        this.product = product;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;
        this.discountValue = 0.0;
        this.discountType = DiscountType.PERCENT;
        if (batch != null) {
            this.batchId = batch.batchId;
            this.batchNo = batch.batchNo;
            this.batchAvailableQty = batch.quantity;
        }
    }

    public CartItem(@NonNull Product product, ProductBatch batch, int quantity, double sellingPrice) {
        this(product, batch, (double) quantity, sellingPrice);
    }

    public CartItem(@NonNull Product product, double quantity, double sellingPrice, double discountValue, DiscountType discountType) {
        this.product = product;
        this.quantity = quantity;
        this.sellingPrice = sellingPrice;
        this.discountValue = discountValue;
        this.discountType = (discountType != null) ? discountType : DiscountType.PERCENT;
    }

    public CartItem(@NonNull Product product, int quantity, double sellingPrice, double discountValue, DiscountType discountType) {
        this(product, (double) quantity, sellingPrice, discountValue, discountType);
    }

    public double getGrossPrice() {
        return sellingPrice * quantity;
    }

    public double getDiscountAmount() {
        double gross = getGrossPrice();
        if (discountType == DiscountType.PERCENT) {
            double percent = Math.max(0.0, Math.min(100.0, discountValue));
            return gross * (percent / 100.0);
        } else {
            return Math.max(0.0, Math.min(gross, discountValue));
        }
    }

    public double getDiscountPercent() {
        double gross = getGrossPrice();
        if (discountType == DiscountType.PERCENT) {
            return Math.max(0.0, Math.min(100.0, discountValue));
        } else {
            return gross > 0 ? (getDiscountAmount() / gross) * 100.0 : 0.0;
        }
    }

    public double getTaxableAmount() {
        return Math.max(0.0, getGrossPrice() - getDiscountAmount());
    }

    public double getGstAmount() {
        if (product != null && product.gstPercent > 0) {
            return getTaxableAmount() * (product.gstPercent / 100.0);
        }
        return 0.0;
    }

    public double getTotalPrice() {
        return getTaxableAmount();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        if (product == null || cartItem.product == null) return false;
        if (product.productId != cartItem.product.productId) return false;
        return Objects.equals(batchId, cartItem.batchId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product != null ? product.productId : 0, batchId);
    }
}
