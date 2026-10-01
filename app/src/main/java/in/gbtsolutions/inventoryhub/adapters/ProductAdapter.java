package in.gbtsolutions.inventoryhub.adapters;

import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import in.gbtsolutions.inventoryhub.models.Category;
import in.gbtsolutions.inventoryhub.models.Product;

public class ProductAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_ITEM = 0;
    public static final int VIEW_TYPE_FOOTER = 1;

    public interface OnProductClickListener {
        void onProductClick(@NonNull Product product);
    }

    public interface OnProductEditListener {
        void onProductEdit(@NonNull Product product);
    }

    public interface OnProductViewListener {
        void onProductView(@NonNull Product product);
    }

    private final List<Product> productList = new ArrayList<>();
    private final Map<Integer, String> categoryNames = new HashMap<>();
    private final Map<Integer, String> categoryIcons = new HashMap<>();
    private final Map<Integer, Long> earliestExpiryMap = new HashMap<>();

    private OnProductClickListener clickListener;
    private OnProductEditListener editListener;
    private OnProductViewListener viewListener;

    private boolean isFooterAdded = false;
    private boolean isRetryActive = false;
    private String retryMessage = null;
    private Runnable retryAction = null;

    public ProductAdapter() {
    }

    public void setEarliestExpiryMap(Map<Integer, Long> expiryMap) {
        this.earliestExpiryMap.clear();
        if (expiryMap != null) {
            this.earliestExpiryMap.putAll(expiryMap);
        }
        notifyDataSetChanged();
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnProductEditListener(OnProductEditListener listener) {
        this.editListener = listener;
    }

    public void setOnProductViewListener(OnProductViewListener listener) {
        this.viewListener = listener;
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

    public List<Product> getProducts() {
        return new ArrayList<>(productList);
    }

    public void clearProducts() {
        int count = getItemCount();
        productList.clear();
        isFooterAdded = false;
        isRetryActive = false;
        if (count > 0) {
            notifyItemRangeRemoved(0, count);
        }
    }

    public void addProducts(@NonNull List<Product> newBatch) {
        if (newBatch.isEmpty()) return;
        int insertPos = productList.size();
        productList.addAll(newBatch);
        notifyItemRangeInserted(insertPos, newBatch.size());
    }

    public void showLoadingFooter() {
        if (!isFooterAdded) {
            isFooterAdded = true;
            isRetryActive = false;
            notifyItemInserted(productList.size());
        } else if (isRetryActive) {
            isRetryActive = false;
            notifyItemChanged(productList.size());
        }
    }

    public void showRetryFooter(String message, Runnable retry) {
        this.retryMessage = message;
        this.retryAction = retry;
        this.isRetryActive = true;
        if (!isFooterAdded) {
            isFooterAdded = true;
            notifyItemInserted(productList.size());
        } else {
            notifyItemChanged(productList.size());
        }
    }

    public void removeFooter() {
        if (isFooterAdded) {
            isFooterAdded = false;
            isRetryActive = false;
            retryMessage = null;
            retryAction = null;
            notifyItemRemoved(productList.size());
        }
    }

    public boolean isFooterAdded() {
        return isFooterAdded;
    }

    public void setProducts(@NonNull List<Product> newProducts) {
        isFooterAdded = false;
        isRetryActive = false;
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return productList.size();
            }

            @Override
            public int getNewListSize() {
                return newProducts.size();
            }

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
                        && Double.compare(oldP.unitPrice, newP.unitPrice) == 0
                        && oldP.quantity == newP.quantity
                        && oldP.reorderLevel == newP.reorderLevel
                        && TextUtils.equals(oldP.unitOfMeasure, newP.unitOfMeasure)
                        && oldP.batchEnabled == newP.batchEnabled;
            }
        });

        productList.clear();
        productList.addAll(newProducts);
        diffResult.dispatchUpdatesTo(this);
    }

    @Override
    public int getItemViewType(int position) {
        if (isFooterAdded && position == productList.size()) {
            return VIEW_TYPE_FOOTER;
        }
        return VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_FOOTER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_loading_footer, parent, false);
            return new FooterViewHolder(view);
        }
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ProductViewHolder) {
            Product product = productList.get(position);
            ((ProductViewHolder) holder).bind(product);
        } else if (holder instanceof FooterViewHolder) {
            ((FooterViewHolder) holder).bind(isRetryActive, retryMessage, retryAction);
        }
    }

    @Override
    public int getItemCount() {
        return isFooterAdded ? productList.size() + 1 : productList.size();
    }

    public class ProductViewHolder extends RecyclerView.ViewHolder {
        private final View cardRoot;
        private final ImageView imgProductIcon;
        private final TextView textName;
        private final TextView textBrand;
        private final TextView textSku;
        private final TextView textCategory;
        private final TextView textSellingPrice;
        private final TextView textUnitPrice;
        private final LinearLayout layoutStockBadge;
        private final View viewStockDot;
        private final TextView textStockStatus;
        private final TextView chipProductExpiry;
        private final View btnView;
        private final View btnEdit;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_product_root);
            imgProductIcon = itemView.findViewById(R.id.img_product_icon);
            textName = itemView.findViewById(R.id.text_product_name);
            textBrand = itemView.findViewById(R.id.text_product_brand);
            textSku = itemView.findViewById(R.id.text_product_sku);
            textCategory = itemView.findViewById(R.id.text_product_category);
            textSellingPrice = itemView.findViewById(R.id.text_selling_price);
            textUnitPrice = itemView.findViewById(R.id.text_unit_price);
            layoutStockBadge = itemView.findViewById(R.id.layout_stock_badge);
            viewStockDot = itemView.findViewById(R.id.view_stock_dot);
            textStockStatus = itemView.findViewById(R.id.text_stock_status);
            chipProductExpiry = itemView.findViewById(R.id.chip_product_expiry);
            btnView = itemView.findViewById(R.id.btn_view_product);
            btnEdit = itemView.findViewById(R.id.btn_edit_product);
        }

        public void bind(@NonNull Product product) {
            String name = product.productName != null ? product.productName.trim() : "Unnamed Product";
            textName.setText(name);

            // Category Icon
            if (imgProductIcon != null) {
                String iconKey = categoryIcons.get(product.categoryId);
                imgProductIcon.setImageResource(CategoryIconHelper.getIconResId(itemView.getContext(), iconKey));
            }

            // Brand
            if (!TextUtils.isEmpty(product.brand)) {
                textBrand.setVisibility(View.VISIBLE);
                textBrand.setText(product.brand.trim());
            } else {
                textBrand.setVisibility(View.GONE);
            }

            // SKU
            if (!TextUtils.isEmpty(product.sku)) {
                textSku.setVisibility(View.VISIBLE);
                textSku.setText(product.sku.trim());
            } else {
                textSku.setVisibility(View.GONE);
            }

            // Category tag
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

            // Pricing
            textSellingPrice.setText(String.format(Locale.getDefault(), "₹ %,.2f", product.sellingPrice));
            if (product.unitPrice > 0) {
                textUnitPrice.setVisibility(View.VISIBLE);
                textUnitPrice.setText(String.format(Locale.getDefault(), "Cost: ₹ %,.2f", product.unitPrice));
            } else {
                textUnitPrice.setVisibility(View.GONE);
            }

            // Stock Badge
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
                textStockStatus.setText(String.format(Locale.getDefault(), "Low Stock: %d %s left", qty, uom));
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

            // Expiry Chip (Batch Mode — per product)
            if (chipProductExpiry != null) {
                Long earliestExp = earliestExpiryMap.get(product.productId);
                if (product.batchEnabled && earliestExp != null && earliestExp > 0) {
                    chipProductExpiry.setVisibility(View.VISIBLE);
                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                    chipProductExpiry.setText("Exp: " + sdf.format(new java.util.Date(earliestExp)));

                    long now = System.currentTimeMillis();
                    long daysLeft = (earliestExp - now) / (1000L * 60 * 60 * 24);

                    if (earliestExp < now) {
                        chipProductExpiry.setBackgroundResource(R.drawable.bg_badge_expired);
                        chipProductExpiry.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.error_red));
                    } else if (daysLeft <= 30) {
                        chipProductExpiry.setBackgroundResource(R.drawable.bg_stock_low);
                        chipProductExpiry.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.accent_amber));
                    } else {
                        chipProductExpiry.setBackgroundResource(R.drawable.bg_tag_sku);
                        chipProductExpiry.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.fg_muted));
                    }
                } else {
                    chipProductExpiry.setVisibility(View.GONE);
                }
            }

            // Click Handlers
            cardRoot.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onProductClick(product);
                }
            });

            btnView.setOnClickListener(v -> {
                if (viewListener != null) {
                    viewListener.onProductView(product);
                } else if (clickListener != null) {
                    clickListener.onProductClick(product);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onProductEdit(product);
                }
            });
        }

        private void tintStatusDot(View dot, int color) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(color);
            dot.setBackground(circle);
        }
    }

    public static class FooterViewHolder extends RecyclerView.ViewHolder {
        private final View containerLoading;
        private final View containerRetry;
        private final TextView textRetryMessage;
        private final TextView btnRetryLoad;

        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
            containerLoading = itemView.findViewById(R.id.container_loading);
            containerRetry = itemView.findViewById(R.id.container_retry);
            textRetryMessage = itemView.findViewById(R.id.text_retry_message);
            btnRetryLoad = itemView.findViewById(R.id.btn_retry_load);
        }

        public void bind(boolean isRetry, String message, Runnable retryAction) {
            if (isRetry) {
                if (containerLoading != null) containerLoading.setVisibility(View.GONE);
                if (containerRetry != null) {
                    containerRetry.setVisibility(View.VISIBLE);
                    if (textRetryMessage != null && message != null) {
                        textRetryMessage.setText(message);
                    }
                    if (btnRetryLoad != null) {
                        btnRetryLoad.setOnClickListener(v -> {
                            if (retryAction != null) {
                                retryAction.run();
                            }
                        });
                    }
                }
            } else {
                if (containerRetry != null) containerRetry.setVisibility(View.GONE);
                if (containerLoading != null) containerLoading.setVisibility(View.VISIBLE);
            }
        }
    }
}
