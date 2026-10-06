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
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleWithBuyer;

public class SellHistoryAdapter extends RecyclerView.Adapter<SellHistoryAdapter.SaleViewHolder> {

    private final List<SaleWithBuyer> salesList = new ArrayList<>();
    private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private OnSaleClickListener clickListener;

    public SellHistoryAdapter() {
    }

    public void setOnSaleClickListener(OnSaleClickListener listener) {
        this.clickListener = listener;
    }

    public void setSales(@NonNull List<SaleWithBuyer> newSales) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return salesList.size();
            }

            @Override
            public int getNewListSize() {
                return newSales.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                Sale oldSale = salesList.get(oldItemPosition).sale;
                Sale newSale = newSales.get(newItemPosition).sale;
                return oldSale != null && newSale != null && oldSale.saleId == newSale.saleId;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                SaleWithBuyer oldItem = salesList.get(oldItemPosition);
                SaleWithBuyer newItem = newSales.get(newItemPosition);

                if (oldItem.sale == null || newItem.sale == null) return false;

                boolean sameSale = oldItem.sale.saleId == newItem.sale.saleId && TextUtils.equals(oldItem.sale.invoiceId, newItem.sale.invoiceId) && Double.compare(oldItem.sale.totalAmount, newItem.sale.totalAmount) == 0 && TextUtils.equals(oldItem.sale.billingDate, newItem.sale.billingDate) && TextUtils.equals(oldItem.sale.status, newItem.sale.status) && TextUtils.equals(oldItem.sale.customerName, newItem.sale.customerName) && TextUtils.equals(oldItem.sale.customerPhone, newItem.sale.customerPhone);

                String oldBuyerName = oldItem.buyer != null ? oldItem.buyer.buyerName : "";
                String newBuyerName = newItem.buyer != null ? newItem.buyer.buyerName : "";
                String oldBuyerPhone = oldItem.buyer != null ? oldItem.buyer.phone : "";
                String newBuyerPhone = newItem.buyer != null ? newItem.buyer.phone : "";

                return sameSale && TextUtils.equals(oldBuyerName, newBuyerName) && TextUtils.equals(oldBuyerPhone, newBuyerPhone);
            }
        });

        salesList.clear();
        salesList.addAll(newSales);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public SaleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sell_history, parent, false);
        return new SaleViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SaleViewHolder holder, int position) {
        SaleWithBuyer item = salesList.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return salesList.size();
    }

    public interface OnSaleClickListener {
        void onSaleClick(@NonNull SaleWithBuyer item);
    }

    public class SaleViewHolder extends RecyclerView.ViewHolder {

        private final TextView textInvoiceId;
        private final TextView textSaleDate;
        private final TextView textStatus;
        private final LinearLayout layoutStatusBadge;
        private final View viewStatusDot;
        private final TextView textBuyerName;
        private final TextView textBuyerPhone;
        private final TextView textTotalAmount;

        SaleViewHolder(@NonNull View itemView) {
            super(itemView);
            textInvoiceId = itemView.findViewById(R.id.text_invoice_id);
            textSaleDate = itemView.findViewById(R.id.text_sale_date);
            textStatus = itemView.findViewById(R.id.text_status);
            layoutStatusBadge = itemView.findViewById(R.id.layout_status_badge);
            viewStatusDot = itemView.findViewById(R.id.view_status_dot);
            textBuyerName = itemView.findViewById(R.id.text_buyer_name);
            textBuyerPhone = itemView.findViewById(R.id.text_buyer_phone);
            textTotalAmount = itemView.findViewById(R.id.text_total_amount);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && clickListener != null) {
                    clickListener.onSaleClick(salesList.get(pos));
                }
            });
        }

        void bind(@NonNull SaleWithBuyer item) {
            Sale sale = item.sale;
            if (sale == null) return;

            // Invoice ID
            if (!TextUtils.isEmpty(sale.invoiceId)) {
                textInvoiceId.setText(sale.invoiceId);
            } else {
                textInvoiceId.setText(String.format(Locale.getDefault(), "#SALE-%d", sale.saleId));
            }

            // Date formatted
            String formattedDate = sale.billingDate;
            if (!TextUtils.isEmpty(sale.billingDate)) {
                try {
                    Date date = isoDateFormat.parse(sale.billingDate);
                    if (date != null) {
                        formattedDate = displayDateFormat.format(date);
                    }
                } catch (ParseException ignored) {
                }
            }
            textSaleDate.setText(formattedDate != null ? formattedDate : "");

            // Status badge styling
            String status = !TextUtils.isEmpty(sale.status) ? sale.status : "Completed";
            textStatus.setText(status);

            int colorText;
            int colorBg;
            if ("Cancelled".equalsIgnoreCase(status)) {
                colorText = ContextCompat.getColor(itemView.getContext(), R.color.status_red);
                colorBg = ContextCompat.getColor(itemView.getContext(), R.color.status_red_bg);
            } else {
                colorText = ContextCompat.getColor(itemView.getContext(), R.color.status_green);
                colorBg = ContextCompat.getColor(itemView.getContext(), R.color.status_green_bg);
            }

            textStatus.setTextColor(colorText);
            if (viewStatusDot != null) {
                viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(colorText));
            }
            if (layoutStatusBadge != null) {
                layoutStatusBadge.setBackgroundTintList(ColorStateList.valueOf(colorBg));
            }

            // Buyer Name & Phone
            String displayName;
            String displayPhone;

            if (item.buyer != null && !TextUtils.isEmpty(item.buyer.buyerName)) {
                displayName = item.buyer.buyerName;
                displayPhone = !TextUtils.isEmpty(item.buyer.phone) ? item.buyer.phone : sale.customerPhone;
            } else if (!TextUtils.isEmpty(sale.customerName)) {
                displayName = sale.customerName;
                displayPhone = sale.customerPhone;
            } else {
                displayName = "Walk-in Customer";
                displayPhone = sale.customerPhone;
            }

            textBuyerName.setText(displayName);

            if (!TextUtils.isEmpty(displayPhone)) {
                textBuyerPhone.setVisibility(View.VISIBLE);
                textBuyerPhone.setText(displayPhone);
            } else {
                textBuyerPhone.setVisibility(View.GONE);
            }

            // Total Amount
            textTotalAmount.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.totalAmount));
        }
    }
}
