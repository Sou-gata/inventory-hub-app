package in.gbtsolutions.inventoryhub.adapters;

import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseWithSupplier;

public class PurchaseHistoryAdapter extends RecyclerView.Adapter<PurchaseHistoryAdapter.PurchaseViewHolder> {

    public interface OnPurchaseClickListener {
        void onPurchaseClick(@NonNull PurchaseWithSupplier item);
    }

    private final List<PurchaseWithSupplier> purchaseList = new ArrayList<>();
    private OnPurchaseClickListener clickListener;

    private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    public PurchaseHistoryAdapter() {
    }

    public void setOnPurchaseClickListener(OnPurchaseClickListener listener) {
        this.clickListener = listener;
    }

    public void setPurchases(@NonNull List<PurchaseWithSupplier> newPurchases) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() { return purchaseList.size(); }

            @Override
            public int getNewListSize() { return newPurchases.size(); }

            @Override
            public boolean areItemsTheSame(int oldPos, int newPos) {
                Purchase oldP = purchaseList.get(oldPos).purchase;
                Purchase newP = newPurchases.get(newPos).purchase;
                return oldP != null && newP != null && oldP.purchaseId == newP.purchaseId;
            }

            @Override
            public boolean areContentsTheSame(int oldPos, int newPos) {
                PurchaseWithSupplier oldItem = purchaseList.get(oldPos);
                PurchaseWithSupplier newItem = newPurchases.get(newPos);
                if (oldItem.purchase == null || newItem.purchase == null) return false;

                boolean samePurchase = oldItem.purchase.purchaseId == newItem.purchase.purchaseId
                        && TextUtils.equals(oldItem.purchase.invoiceId, newItem.purchase.invoiceId)
                        && Double.compare(oldItem.purchase.totalAmount, newItem.purchase.totalAmount) == 0
                        && TextUtils.equals(oldItem.purchase.billingDate, newItem.purchase.billingDate)
                        && TextUtils.equals(oldItem.purchase.status, newItem.purchase.status);

                String oldName = oldItem.supplier != null ? oldItem.supplier.supplierName : "";
                String newName = newItem.supplier != null ? newItem.supplier.supplierName : "";
                return samePurchase && TextUtils.equals(oldName, newName);
            }
        });

        purchaseList.clear();
        purchaseList.addAll(newPurchases);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public PurchaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase_history, parent, false);
        return new PurchaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PurchaseViewHolder holder, int position) {
        holder.bind(purchaseList.get(position));
    }

    @Override
    public int getItemCount() {
        return purchaseList.size();
    }

    public class PurchaseViewHolder extends RecyclerView.ViewHolder {

        private final TextView textInvoiceId;
        private final TextView textPurchaseDate;
        private final LinearLayout layoutStatusBadge;
        private final View viewStatusDot;
        private final TextView textStatus;
        private final TextView textSupplierName;
        private final TextView textSupplierPhone;
        private final TextView textTotalAmount;

        PurchaseViewHolder(@NonNull View itemView) {
            super(itemView);
            textInvoiceId = itemView.findViewById(R.id.text_invoice_id);
            textPurchaseDate = itemView.findViewById(R.id.text_sale_date);
            layoutStatusBadge = itemView.findViewById(R.id.layout_status_badge);
            viewStatusDot = itemView.findViewById(R.id.view_status_dot);
            textStatus = itemView.findViewById(R.id.text_status);
            textSupplierName = itemView.findViewById(R.id.text_buyer_name);
            textSupplierPhone = itemView.findViewById(R.id.text_buyer_phone);
            textTotalAmount = itemView.findViewById(R.id.text_total_amount);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_ID && clickListener != null) {
                    clickListener.onPurchaseClick(purchaseList.get(pos));
                }
            });
        }

        void bind(@NonNull PurchaseWithSupplier item) {
            Purchase purchase = item.purchase;
            if (purchase == null) return;

            // Invoice ID (PO number)
            if (!TextUtils.isEmpty(purchase.invoiceId)) {
                textInvoiceId.setText(purchase.invoiceId);
            } else {
                textInvoiceId.setText(String.format(Locale.getDefault(), "#PO-%d", purchase.purchaseId));
            }

            // Date formatted
            String formattedDate = purchase.billingDate;
            if (!TextUtils.isEmpty(purchase.billingDate)) {
                try {
                    Date date = isoDateFormat.parse(purchase.billingDate);
                    if (date != null) {
                        formattedDate = displayDateFormat.format(date);
                    }
                } catch (ParseException ignored) {
                }
            }
            textPurchaseDate.setText(formattedDate != null ? formattedDate : "");

            // Status styling with badge colors
            String status = !TextUtils.isEmpty(purchase.status) ? purchase.status : "Completed";
            textStatus.setText(status);

            int colorText;
            int colorBg;
            if ("Completed".equalsIgnoreCase(status)) {
                colorText = ContextCompat.getColor(itemView.getContext(), R.color.status_green);
                colorBg = ContextCompat.getColor(itemView.getContext(), R.color.status_green_bg);
            } else if ("Partially Received".equalsIgnoreCase(status) || "Partial Completed".equalsIgnoreCase(status)) {
                colorText = ContextCompat.getColor(itemView.getContext(), R.color.status_blue);
                colorBg = ContextCompat.getColor(itemView.getContext(), R.color.status_blue_bg);
            } else if ("Pending".equalsIgnoreCase(status)) {
                colorText = ContextCompat.getColor(itemView.getContext(), R.color.status_orange);
                colorBg = ContextCompat.getColor(itemView.getContext(), R.color.status_orange_bg);
            } else {
                // "Cancelled", "Partially Cancelled", etc.
                colorText = ContextCompat.getColor(itemView.getContext(), R.color.status_red);
                colorBg = ContextCompat.getColor(itemView.getContext(), R.color.status_red_bg);
            }

            textStatus.setTextColor(colorText);
            if (viewStatusDot != null) {
                viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(colorText));
            }
            if (layoutStatusBadge != null) {
                layoutStatusBadge.setBackgroundTintList(ColorStateList.valueOf(colorBg));
            }

            // Supplier info
            if (item.supplier != null && !TextUtils.isEmpty(item.supplier.supplierName)) {
                textSupplierName.setText(item.supplier.supplierName);
                if (!TextUtils.isEmpty(item.supplier.phone)) {
                    textSupplierPhone.setVisibility(View.VISIBLE);
                    textSupplierPhone.setText(item.supplier.phone);
                } else {
                    textSupplierPhone.setVisibility(View.GONE);
                }
            } else {
                textSupplierName.setText("Unknown Supplier");
                textSupplierPhone.setVisibility(View.GONE);
            }

            // Total Amount
            textTotalAmount.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.totalAmount));
        }
    }
}
