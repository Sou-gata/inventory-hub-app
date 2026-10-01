package in.gbtsolutions.inventoryhub.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.models.CartItem;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;

public class SellCartManager {

    public interface OnCartChangedListener {
        void onCartChanged();
    }

    private static SellCartManager instance;

    // Composite key: "productId" or "productId_batchId"
    private final Map<String, CartItem> cartItems = new LinkedHashMap<>();
    private final List<OnCartChangedListener> listeners = new ArrayList<>();

    private SellCartManager() {
    }

    public static synchronized SellCartManager getInstance() {
        if (instance == null) {
            instance = new SellCartManager();
        }
        return instance;
    }

    public static String buildKey(int productId, Integer batchId) {
        return productId + "_" + (batchId != null ? batchId : "default");
    }

    public CartItem getCartItem(int productId, Integer batchId) {
        return cartItems.get(buildKey(productId, batchId));
    }

    public void addProduct(@NonNull Product product) {
        String key = buildKey(product.productId, null);
        CartItem existing = cartItems.get(key);
        if (existing != null) {
            existing.quantity += 1;
            if (product.sellingPrice > 0) {
                existing.sellingPrice = product.sellingPrice;
            }
        } else {
            cartItems.put(key, new CartItem(product));
        }
        notifyListeners();
    }

    public void addBatchItem(@Nullable Product product, @Nullable ProductBatch batch, int quantity) {
        if (product == null) {
            return;
        }
        if (batch == null) {
            addProduct(product);
            return;
        }
        String key = buildKey(product.productId, batch.batchId);
        double price = product.sellingPrice > 0 ? product.sellingPrice : (batch.sellingPrice > 0 ? batch.sellingPrice : product.sellingPrice);
        CartItem existing = cartItems.get(key);
        if (existing != null) {
            boolean allowOutOfStock = GlobalStore.getInstance().isAllowOutOfStockSell();
            if (!allowOutOfStock && batch.quantity > 0) {
                existing.quantity = Math.min(batch.quantity, existing.quantity + quantity);
            } else {
                existing.quantity += quantity;
            }
            existing.sellingPrice = price;
        } else {
            CartItem newItem = new CartItem(product, batch, quantity, price);
            cartItems.put(key, newItem);
        }
        notifyListeners();
    }

    public void removeProduct(int productId) {
        // Removes all items with this productId
        boolean removedAny = false;
        List<String> keysToRemove = new ArrayList<>();
        for (Map.Entry<String, CartItem> entry : cartItems.entrySet()) {
            if (entry.getValue().product != null && entry.getValue().product.productId == productId) {
                keysToRemove.add(entry.getKey());
            }
        }
        for (String k : keysToRemove) {
            cartItems.remove(k);
            removedAny = true;
        }
        if (removedAny) {
            notifyListeners();
        }
    }

    public void removeCartItem(CartItem item) {
        if (item == null || item.product == null) return;
        String key = buildKey(item.product.productId, item.batchId);
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
        for (CartItem item : cartItems.values()) {
            if (item.product != null && item.product.productId == productId) {
                return true;
            }
        }
        return false;
    }

    public int getProductQuantityInCart(int productId) {
        int total = 0;
        for (CartItem item : cartItems.values()) {
            if (item.product != null && item.product.productId == productId) {
                total += item.quantity;
            }
        }
        return total;
    }

    public void updateItemQuantity(CartItem item, int quantity) {
        if (item == null || item.product == null) return;
        String key = buildKey(item.product.productId, item.batchId);
        CartItem target = cartItems.get(key);
        if (target != null) {
            target.quantity = quantity;
        }
    }

    public void updateQuantity(int productId, int quantity) {
        String key = buildKey(productId, null);
        CartItem item = cartItems.get(key);
        if (item != null) {
            item.quantity = quantity;
        }
    }

    public void updateItemSellingPrice(CartItem item, double sellingPrice) {
        if (item == null || item.product == null) return;
        String key = buildKey(item.product.productId, item.batchId);
        CartItem target = cartItems.get(key);
        if (target != null) {
            target.sellingPrice = sellingPrice;
        }
    }

    public void updateSellingPrice(int productId, double sellingPrice) {
        String key = buildKey(productId, null);
        CartItem item = cartItems.get(key);
        if (item != null) {
            item.sellingPrice = sellingPrice;
        }
    }

    public void updateItemDiscount(CartItem item, double discountValue, CartItem.DiscountType discountType) {
        if (item == null || item.product == null) return;
        String key = buildKey(item.product.productId, item.batchId);
        CartItem target = cartItems.get(key);
        if (target != null) {
            target.discountValue = discountValue;
            if (discountType != null) {
                target.discountType = discountType;
            }
        }
    }

    public void updateDiscount(int productId, double discountValue, CartItem.DiscountType discountType) {
        String key = buildKey(productId, null);
        CartItem item = cartItems.get(key);
        if (item != null) {
            item.discountValue = discountValue;
            if (discountType != null) {
                item.discountType = discountType;
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

    public double getSubtotal() {
        return getGrossSubtotal();
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
