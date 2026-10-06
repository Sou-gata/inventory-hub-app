package in.gbtsolutions.inventoryhub.helpers;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import in.gbtsolutions.inventoryhub.models.CartItem;
import in.gbtsolutions.inventoryhub.models.Product;

/**
 * Standalone cart manager for the Purchase flow.
 * Key differences vs SellCartManager:
 *   - No stock availability checks (you can purchase any quantity)
 *   - No batch dialog support (batch/expiry not tracked during purchase)
 *   - Uses cost price (stored in CartItem.sellingPrice field, displayed as "Cost Price")
 */
public class PurchaseCartManager {

    public interface OnCartChangedListener {
        void onCartChanged();
    }

    private static PurchaseCartManager instance;

    // Composite key: "productId_default" (no batch variant for purchases)
    private final Map<String, CartItem> cartItems = new LinkedHashMap<>();
    private final List<OnCartChangedListener> listeners = new ArrayList<>();

    private PurchaseCartManager() {
    }

    public static synchronized PurchaseCartManager getInstance() {
        if (instance == null) {
            instance = new PurchaseCartManager();
        }
        return instance;
    }

    public static String buildKey(int productId) {
        return productId + "_default";
    }

    public CartItem getCartItem(int productId) {
        return cartItems.get(buildKey(productId));
    }

    public void addProduct(@NonNull Product product) {
        String key = buildKey(product.productId);
        CartItem existing = cartItems.get(key);
        if (existing != null) {
            existing.quantity = Math.round((existing.quantity + 1.0) * 1000.0) / 1000.0;
            // Keep current cost price (user may have edited it)
        } else {
            // Use unitPrice (cost price) as default; fallback to sellingPrice
            CartItem item = new CartItem(product);
            if (product.unitPrice > 0) {
                item.sellingPrice = product.unitPrice;
            }
            cartItems.put(key, item);
        }
        notifyListeners();
    }

    public void removeProduct(int productId) {
        if (cartItems.remove(buildKey(productId)) != null) {
            notifyListeners();
        }
    }

    public void removeCartItem(CartItem item) {
        if (item == null || item.product == null) return;
        String key = buildKey(item.product.productId);
        if (cartItems.remove(key) != null) {
            notifyListeners();
        }
    }

    public void toggleProduct(@NonNull Product product) {
        if (isInCart(product.productId)) {
            removeProduct(product.productId);
        } else {
            addProduct(product);
        }
    }

    public boolean isInCart(int productId) {
        return cartItems.containsKey(buildKey(productId));
    }

    public double getProductQuantityInCart(int productId) {
        CartItem item = cartItems.get(buildKey(productId));
        return item != null ? item.quantity : 0;
    }

    public void updateItemQuantity(CartItem item, double quantity) {
        if (item == null || item.product == null) return;
        CartItem target = cartItems.get(buildKey(item.product.productId));
        if (target != null) {
            target.quantity = Math.round(quantity * 1000.0) / 1000.0;
        }
    }

    public void updateItemQuantity(CartItem item, int quantity) {
        updateItemQuantity(item, (double) quantity);
    }

    public void updateItemSellingPrice(CartItem item, double price) {
        if (item == null || item.product == null) return;
        CartItem target = cartItems.get(buildKey(item.product.productId));
        if (target != null) {
            target.sellingPrice = price;
        }
    }

    public void updateItemDiscount(CartItem item, double discountValue, CartItem.DiscountType discountType) {
        if (item == null || item.product == null) return;
        CartItem target = cartItems.get(buildKey(item.product.productId));
        if (target != null) {
            target.discountValue = discountValue;
            if (discountType != null) {
                target.discountType = discountType;
            }
        }
    }

    public List<CartItem> getCartItems() {
        return new ArrayList<>(cartItems.values());
    }

    public int getItemCount() {
        return cartItems.size();
    }

    public double getGrossSubtotal() {
        double subtotal = 0.0;
        for (CartItem item : cartItems.values()) {
            subtotal += item.getGrossPrice();
        }
        return subtotal;
    }

    public double getTotalDiscount() {
        double totalDiscount = 0.0;
        for (CartItem item : cartItems.values()) {
            totalDiscount += item.getDiscountAmount();
        }
        return totalDiscount;
    }

    public double getTaxableSubtotal() {
        double taxable = 0.0;
        for (CartItem item : cartItems.values()) {
            taxable += item.getTaxableAmount();
        }
        return taxable;
    }

    public double getTotalGst() {
        double totalGst = 0.0;
        for (CartItem item : cartItems.values()) {
            totalGst += item.getGstAmount();
        }
        return totalGst;
    }

    public double getGrandTotal(double otherCharges) {
        return Math.max(0.0, getTaxableSubtotal() + getTotalGst() + otherCharges);
    }

    public void clear() {
        if (!cartItems.isEmpty()) {
            cartItems.clear();
            notifyListeners();
        }
    }

    public void addListener(OnCartChangedListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(OnCartChangedListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners() {
        for (OnCartChangedListener listener : new ArrayList<>(listeners)) {
            listener.onCartChanged();
        }
    }
}
