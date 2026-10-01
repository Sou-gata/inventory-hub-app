package in.gbtsolutions.inventoryhub.adapters;

import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.CategoryIconHelper;
import in.gbtsolutions.inventoryhub.helpers.PurchaseCartManager;
import in.gbtsolutions.inventoryhub.models.Category;
import in.gbtsolutions.inventoryhub.models.Product;

/**
 * Adapter for the product list in the Purchase flow.
 * Uses PurchaseCartManager (not SellCartManager).
 * Stock badge shows for reference only — adding to purchase cart is always allowed.
 */
public class PurchaseProductAdapter extends RecyclerView.Adapter<PurchaseProductAdapter.PurchaseProductViewHolder> {

    public interface OnAddToCartClickListener {
        void onAddToCartClick(@NonNull Product product);
    }

    private final List<Product> productList = new ArrayList<>();
    private final Map<Integer, String> categoryNames = new HashMap<>();
    private final Map<Integer, String> categoryIcons = new HashMap<>();
    private OnAddToCartClickListener addToCartListener;

    public PurchaseProductAdapter() {
    }

    public void setOnAddToCartClickListener(OnAddToCartClickListener listener) {
        this.addToCartListener = listener;
    }

    public void setCategories(List<Category> categories) {
        categoryNames.clear();
        categoryIcons.clear();
        if (categories != null) {
            for (Category category : categories) {
                if (category.categoryName != null) {
                    categoryNames.put(category.categoryId, category.categoryName);
                }
                categoryIcons.put(category.categoryId, category.icon);
            }
        }
        notifyItemRangeChanged(0, productList.size());
    }

    public void setProducts(@NonNull List<Product> newProducts) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return productList.size(); }

            @Override
            public int getNewListSize() { return newProducts.size(); }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return productList.get(oldItemPosition).productId == newProducts.get(newItemPosition).productId;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Product oldP = productList.get(oldItemPosition);
                Product newP = newProducts.get(newItemPosition);
                return TextUtils.equals(oldP.productName, newP.productName)
                        && TextUtils.equals(oldP.sku, newP.sku)
                        && TextUtils.equals(oldP.brand, newP.brand)
                        && oldP.categoryId == newP.categoryId
                        && Double.compare(oldP.sellingPrice, newP.sellingPrice) == 0
                        && oldP.quantity == newP.quantity;
            }
        });

        productList.clear();
        productList.addAll(newProducts);
        diffResult.dispatchUpdatesTo(this);
    }

    public void addProducts(@NonNull List<Product> moreProducts) {
        int startPos = productList.size();
        productList.addAll(moreProducts);
        notifyItemRangeInserted(startPos, moreProducts.size());
    }

    public void clearProducts() {
        int size = productList.size();
        productList.clear();
        notifyDataSetChanged();
    }

    public List<Product> getProducts() {
        return productList;
    }

    @NonNull
    @Override
    public PurchaseProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sell_product, parent, false);
        return new PurchaseProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PurchaseProductViewHolder holder, int position) {
        holder.bind(productList.get(position));
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public class PurchaseProductViewHolder extends RecyclerView.ViewHolder {
        private final View cardRoot;
        private final ImageView imgProductIcon;
        private final TextView textName;
        private final TextView textBrand;
        private final TextView textSku;
        private final TextView textCategory;
        private final TextView textSellingPrice;
        private final FrameLayout btnToggleCart;
        private final ImageView imageCartIcon;
        private final LinearLayout layoutStockBadge;
        private final View viewStockDot;
        private final TextView textStockStatus;

        public PurchaseProductViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_product_root);
            imgProductIcon = itemView.findViewById(R.id.img_product_icon);
            textName = itemView.findViewById(R.id.text_product_name);
            textBrand = itemView.findViewById(R.id.text_product_brand);
            textSku = itemView.findViewById(R.id.text_product_sku);
            textCategory = itemView.findViewById(R.id.text_product_category);
            textSellingPrice = itemView.findViewById(R.id.text_selling_price);
            btnToggleCart = itemView.findViewById(R.id.btn_toggle_cart);
            imageCartIcon = itemView.findViewById(R.id.image_cart_icon);
            layoutStockBadge = itemView.findViewById(R.id.layout_stock_badge);
            viewStockDot = itemView.findViewById(R.id.view_stock_dot);
            textStockStatus = itemView.findViewById(R.id.text_stock_status);
        }

        public void bind(@NonNull Product product) {
            textName.setText(product.productName != null ? product.productName.trim() : "Unnamed Product");

            // Category Icon
            if (imgProductIcon != null) {
                String iconKey = categoryIcons.get(product.categoryId);
                imgProductIcon.setImageResource(CategoryIconHelper.getIconResId(itemView.getContext(), iconKey));
            }

            if (!TextUtils.isEmpty(product.brand)) {
                textBrand.setVisibility(View.VISIBLE);
                textBrand.setText(product.brand.trim());
            } else {
                textBrand.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(product.sku)) {
                textSku.setVisibility(View.VISIBLE);
                textSku.setText(product.sku.trim());
            } else {
                textSku.setVisibility(View.GONE);
            }

            String categoryName = categoryNames.get(product.categoryId);
            if (!TextUtils.isEmpty(categoryName)) {
                textCategory.setVisibility(View.VISIBLE);
                textCategory.setText(categoryName);
            } else if (product.categoryId > 0) {
                textCategory.setVisibility(View.VISIBLE);
                textCategory.setText(String.format(Locale.getDefault(), "Cat #%d", product.categoryId));
            } else {
                textCategory.setVisibility(View.GONE);
            }

            // Show unit cost price (purchase price) if available, otherwise selling price
            double displayPrice = product.unitPrice > 0 ? product.unitPrice : product.sellingPrice;
            textSellingPrice.setText(String.format(Locale.getDefault(), "₹ %,.2f", displayPrice));

            // Stock badge — informational only for purchases
            String uom = !TextUtils.isEmpty(product.unitOfMeasure) ? product.unitOfMeasure.trim() : "pcs";
            int qty = product.quantity;
            int reorder = product.reorderLevel;

            if (qty <= 0) {
                layoutStockBadge.setBackgroundResource(R.drawable.bg_stock_out);
                textStockStatus.setText(String.format(Locale.getDefault(), "Out of Stock (0 %s)", uom));
                int redColor = ContextCompat.getColor(itemView.getContext(), R.color.error_red);
                textStockStatus.setTextColor(redColor);
                tintStatusDot(viewStockDot, redColor);
            } else if (qty <= reorder) {
                layoutStockBadge.setBackgroundResource(R.drawable.bg_stock_low);
                textStockStatus.setText(String.format(Locale.getDefault(), "Low Stock: %d %s", qty, uom));
                int amberColor = ContextCompat.getColor(itemView.getContext(), R.color.accent_amber);
                textStockStatus.setTextColor(amberColor);
                tintStatusDot(viewStockDot, amberColor);
            } else {
                layoutStockBadge.setBackgroundResource(R.drawable.bg_stock_in);
                textStockStatus.setText(String.format(Locale.getDefault(), "In Stock (%d %s)", qty, uom));
                int inStockColor = ContextCompat.getColor(itemView.getContext(), R.color.status_green);
                textStockStatus.setTextColor(inStockColor);
                tintStatusDot(viewStockDot, inStockColor);
            }

            // Cart state via PurchaseCartManager
            boolean inCart = PurchaseCartManager.getInstance().isInCart(product.productId);
            if (inCart) {
                btnToggleCart.setBackgroundResource(R.drawable.bg_cart_button_added);
                imageCartIcon.setImageResource(R.drawable.ic_cart_check);
                imageCartIcon.setContentDescription(itemView.getContext().getString(R.string.btn_remove_from_cart));
            } else {
                btnToggleCart.setBackgroundResource(R.drawable.bg_cart_button);
                imageCartIcon.setImageResource(R.drawable.ic_cart_add);
                imageCartIcon.setContentDescription(itemView.getContext().getString(R.string.btn_add_to_cart));
            }

            btnToggleCart.setOnClickListener(v -> {
                if (addToCartListener != null) addToCartListener.onAddToCartClick(product);
            });

            cardRoot.setOnClickListener(v -> {
                if (addToCartListener != null) addToCartListener.onAddToCartClick(product);
            });
        }

        private void tintStatusDot(View dot, int color) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(color);
            dot.setBackground(circle);
        }
    }
}
