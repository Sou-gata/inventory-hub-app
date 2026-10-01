package in.gbtsolutions.inventoryhub.adapters;

import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.PurchaseCartManager;
import in.gbtsolutions.inventoryhub.models.CartItem;

/**
 * Cart item adapter for the Purchase flow.
 * Differences from CartItemAdapter:
 *   - Uses PurchaseCartManager (not SellCartManager)
 *   - No stock cap on the + button (can purchase any quantity)
 *   - Price field label is "Cost Price" (displayed via hint)
 *   - Batch number row always hidden
 */
public class PurchaseCartItemAdapter extends RecyclerView.Adapter<PurchaseCartItemAdapter.PurchaseCartItemViewHolder> {

    private final List<CartItem> cartItems = new ArrayList<>();
    private OnRemoveCartItemListener removeListener;
    private OnCartItemUpdateListener updateListener;

    public PurchaseCartItemAdapter() {
    }

    public void setOnRemoveCartItemListener(OnRemoveCartItemListener listener) {
        this.removeListener = listener;
    }

    public void setOnCartItemUpdateListener(OnCartItemUpdateListener listener) {
        this.updateListener = listener;
    }

    public void setCartItems(@NonNull List<CartItem> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return cartItems.size(); }

            @Override
            public int getNewListSize() { return newItems.size(); }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                CartItem oldI = cartItems.get(oldItemPosition);
                CartItem newI = newItems.get(newItemPosition);
                if (oldI.product == null || newI.product == null) return false;
                return oldI.product.productId == newI.product.productId;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                CartItem oldI = cartItems.get(oldItemPosition);
                CartItem newI = newItems.get(newItemPosition);
                return oldI.quantity == newI.quantity
                        && Double.compare(oldI.sellingPrice, newI.sellingPrice) == 0
                        && Double.compare(oldI.discountValue, newI.discountValue) == 0
                        && oldI.discountType == newI.discountType;
            }
        });

        cartItems.clear();
        cartItems.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public PurchaseCartItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart_product, parent, false);
        return new PurchaseCartItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PurchaseCartItemViewHolder holder, int position) {
        holder.bind(cartItems.get(position));
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    public interface OnCartItemUpdateListener {
        void onCartItemUpdated();
    }

    public interface OnRemoveCartItemListener {
        void onRemoveItem(@NonNull CartItem item);
    }

    public class PurchaseCartItemViewHolder extends RecyclerView.ViewHolder {
        private final TextView textName;
        private final TextView textBrand;
        private final TextView textSku;
        private final TextView textBatchNo;
        private final FrameLayout btnQtyMinus;
        private final EditText editQuantity;
        private final FrameLayout btnQtyPlus;
        private final EditText editSellingPrice;
        private final TextView textDiscountPrefix;
        private final EditText editDiscount;
        private final TextView btnDiscountPercent;
        private final TextView btnDiscountRupee;
        private final TextView textItemTotal;
        private final FrameLayout btnRemove;

        private TextWatcher qtyWatcher;
        private TextWatcher priceWatcher;
        private TextWatcher discountWatcher;

        public PurchaseCartItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_cart_product_name);
            textBrand = itemView.findViewById(R.id.text_cart_product_brand);
            textSku = itemView.findViewById(R.id.text_cart_product_sku);
            textBatchNo = itemView.findViewById(R.id.text_cart_batch_no);
            btnQtyMinus = itemView.findViewById(R.id.btn_qty_minus);
            editQuantity = itemView.findViewById(R.id.edit_cart_quantity);
            btnQtyPlus = itemView.findViewById(R.id.btn_qty_plus);
            editSellingPrice = itemView.findViewById(R.id.edit_cart_selling_price);
            textDiscountPrefix = itemView.findViewById(R.id.text_cart_discount_prefix);
            editDiscount = itemView.findViewById(R.id.edit_cart_discount);
            btnDiscountPercent = itemView.findViewById(R.id.btn_cart_discount_mode_percent);
            btnDiscountRupee = itemView.findViewById(R.id.btn_cart_discount_mode_rupee);
            textItemTotal = itemView.findViewById(R.id.text_cart_item_total);
            btnRemove = itemView.findViewById(R.id.btn_remove_cart_item);
        }

        public void bind(@NonNull CartItem item) {
            // Remove old watchers to avoid stale callbacks
            if (qtyWatcher != null) editQuantity.removeTextChangedListener(qtyWatcher);
            if (priceWatcher != null) editSellingPrice.removeTextChangedListener(priceWatcher);
            if (discountWatcher != null) editDiscount.removeTextChangedListener(discountWatcher);

            // Batch row always hidden for purchases
            if (textBatchNo != null) textBatchNo.setVisibility(View.GONE);

            String name = (item.product != null && item.product.productName != null)
                    ? item.product.productName.trim() : "Unnamed Product";
            textName.setText(name);

            if (item.product != null && !TextUtils.isEmpty(item.product.brand)) {
                textBrand.setVisibility(View.VISIBLE);
                textBrand.setText(item.product.brand.trim());
            } else {
                textBrand.setVisibility(View.GONE);
            }

            if (item.product != null && !TextUtils.isEmpty(item.product.sku)) {
                textSku.setVisibility(View.VISIBLE);
                textSku.setText(item.product.sku.trim());
            } else {
                textSku.setVisibility(View.GONE);
            }

            editQuantity.setText(String.valueOf(item.quantity));
            // Show the cost price (stored in sellingPrice field)
            editSellingPrice.setText(String.format(Locale.getDefault(), "%.2f", item.sellingPrice));
            editSellingPrice.setHint("Cost Price");
            updateTotalDisplay(item);

            // No stock cap for purchases — can order any quantity
            btnQtyMinus.setOnClickListener(v -> {
                if (item.quantity > 1) {
                    item.quantity--;
                    editQuantity.setText(String.valueOf(item.quantity));
                    updateTotalDisplay(item);
                    PurchaseCartManager.getInstance().updateItemQuantity(item, item.quantity);
                    if (updateListener != null) updateListener.onCartItemUpdated();
                }
            });

            btnQtyPlus.setOnClickListener(v -> {
                item.quantity++;
                editQuantity.setText(String.valueOf(item.quantity));
                updateTotalDisplay(item);
                PurchaseCartManager.getInstance().updateItemQuantity(item, item.quantity);
                if (updateListener != null) updateListener.onCartItemUpdated();
            });

            qtyWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s != null && s.length() > 0) {
                        try {
                            int q = Integer.parseInt(s.toString().trim());
                            if (q > 0) {
                                item.quantity = q;
                                updateTotalDisplay(item);
                                PurchaseCartManager.getInstance().updateItemQuantity(item, item.quantity);
                                if (updateListener != null) updateListener.onCartItemUpdated();
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }

                @Override public void afterTextChanged(Editable s) {}
            };
            editQuantity.addTextChangedListener(qtyWatcher);

            editQuantity.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    String str = editQuantity.getText().toString().trim();
                    if (TextUtils.isEmpty(str) || "0".equals(str)) {
                        item.quantity = 1;
                        editQuantity.setText("1");
                        updateTotalDisplay(item);
                        PurchaseCartManager.getInstance().updateItemQuantity(item, 1);
                        if (updateListener != null) updateListener.onCartItemUpdated();
                    }
                }
            });

            priceWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s != null && s.length() > 0) {
                        try {
                            double p = Double.parseDouble(s.toString().trim());
                            if (p >= 0) {
                                item.sellingPrice = p;
                                updateTotalDisplay(item);
                                PurchaseCartManager.getInstance().updateItemSellingPrice(item, item.sellingPrice);
                                if (updateListener != null) updateListener.onCartItemUpdated();
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }

                @Override public void afterTextChanged(Editable s) {}
            };
            editSellingPrice.addTextChangedListener(priceWatcher);

            editSellingPrice.setOnFocusChangeListener((v, hasFocus) -> {
                if (!hasFocus) {
                    String str = editSellingPrice.getText().toString().trim();
                    if (TextUtils.isEmpty(str)) {
                        item.sellingPrice = 0.0;
                        editSellingPrice.setText("0.00");
                        updateTotalDisplay(item);
                        PurchaseCartManager.getInstance().updateItemSellingPrice(item, 0.0);
                        if (updateListener != null) updateListener.onCartItemUpdated();
                    }
                }
            });

            // Discount setup
            updateDiscountModeUI(item);
            if (item.discountValue > 0) {
                if (item.discountValue == (long) item.discountValue) {
                    editDiscount.setText(String.format(Locale.getDefault(), "%d", (long) item.discountValue));
                } else {
                    editDiscount.setText(String.format(Locale.getDefault(), "%.2f", item.discountValue));
                }
            } else {
                editDiscount.setText("");
            }

            btnDiscountPercent.setOnClickListener(v -> {
                if (item.discountType != CartItem.DiscountType.PERCENT) {
                    item.discountType = CartItem.DiscountType.PERCENT;
                    updateDiscountModeUI(item);
                    updateTotalDisplay(item);
                    PurchaseCartManager.getInstance().updateItemDiscount(item, item.discountValue, item.discountType);
                    if (updateListener != null) updateListener.onCartItemUpdated();
                }
            });

            btnDiscountRupee.setOnClickListener(v -> {
                if (item.discountType != CartItem.DiscountType.RUPEE) {
                    item.discountType = CartItem.DiscountType.RUPEE;
                    updateDiscountModeUI(item);
                    updateTotalDisplay(item);
                    PurchaseCartManager.getInstance().updateItemDiscount(item, item.discountValue, item.discountType);
                    if (updateListener != null) updateListener.onCartItemUpdated();
                }
            });

            discountWatcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    double val = 0.0;
                    if (s != null && s.length() > 0) {
                        try { val = Double.parseDouble(s.toString().trim()); }
                        catch (NumberFormatException ignored) {}
                    }
                    item.discountValue = Math.max(0.0, val);
                    updateTotalDisplay(item);
                    PurchaseCartManager.getInstance().updateItemDiscount(item, item.discountValue, item.discountType);
                    if (updateListener != null) updateListener.onCartItemUpdated();
                }

                @Override public void afterTextChanged(Editable s) {}
            };
            editDiscount.addTextChangedListener(discountWatcher);

            btnRemove.setOnClickListener(v -> {
                if (removeListener != null) removeListener.onRemoveItem(item);
            });
        }

        private void updateDiscountModeUI(CartItem item) {
            Context context = itemView.getContext();
            if (item.discountType == CartItem.DiscountType.PERCENT) {
                btnDiscountPercent.setBackgroundResource(R.drawable.bg_toggle_button_active);
                btnDiscountPercent.setTextColor(ContextCompat.getColor(context, R.color.white));
                btnDiscountRupee.setBackgroundResource(R.drawable.bg_toggle_button_compact);
                btnDiscountRupee.setTextColor(ContextCompat.getColor(context, R.color.fg_muted));
                textDiscountPrefix.setText("%");
            } else {
                btnDiscountRupee.setBackgroundResource(R.drawable.bg_toggle_button_active);
                btnDiscountRupee.setTextColor(ContextCompat.getColor(context, R.color.white));
                btnDiscountPercent.setBackgroundResource(R.drawable.bg_toggle_button_compact);
                btnDiscountPercent.setTextColor(ContextCompat.getColor(context, R.color.fg_muted));
                textDiscountPrefix.setText("₹");
            }
        }

        private void updateTotalDisplay(CartItem item) {
            textItemTotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", item.getTotalPrice()));
        }
    }
}
