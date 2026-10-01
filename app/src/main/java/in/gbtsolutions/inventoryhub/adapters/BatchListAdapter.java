package in.gbtsolutions.inventoryhub.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.models.ProductBatch;

public class BatchListAdapter extends RecyclerView.Adapter<BatchListAdapter.BatchViewHolder> {

    public interface OnBatchActionListener {
        void onEdit(ProductBatch batch, int position);
        void onDelete(ProductBatch batch, int position);
    }

    private final List<ProductBatch> batches = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private OnBatchActionListener actionListener;
    private boolean isReadOnly = false;

    public BatchListAdapter() {
    }

    public void setBatches(List<ProductBatch> newBatches) {
        this.batches.clear();
        if (newBatches != null) {
            this.batches.addAll(newBatches);
        }
        notifyDataSetChanged();
    }

    public List<ProductBatch> getBatches() {
        return batches;
    }

    public void setActionListener(OnBatchActionListener listener) {
        this.actionListener = listener;
    }

    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_batch_row, parent, false);
        return new BatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchViewHolder holder, int position) {
        ProductBatch batch = batches.get(position);
        Context context = holder.itemView.getContext();

        holder.tvBatchNo.setText(batch.batchNo != null ? batch.batchNo : "No Batch#");
        holder.tvBatchQuantity.setText("Qty: " + batch.quantity);
        holder.tvPurchasePrice.setText(String.format(Locale.getDefault(), "Cost: ₹%.2f", batch.purchasePrice));

        if (batch.sellingPrice > 0) {
            holder.tvSellingPrice.setText(String.format(Locale.getDefault(), "Sell: ₹%.2f", batch.sellingPrice));
            holder.tvSellingPrice.setVisibility(View.VISIBLE);
        } else {
            holder.tvSellingPrice.setVisibility(View.GONE);
        }

        if (batch.expiryDate > 0) {
            holder.tvExpiry.setText("Exp: " + dateFormat.format(new Date(batch.expiryDate)));
            holder.tvExpiry.setVisibility(View.VISIBLE);
        } else {
            holder.tvExpiry.setText("No Expiry");
            holder.tvExpiry.setVisibility(View.VISIBLE);
        }

        boolean isExpired = batch.isExpired();
        boolean isZeroQty = batch.quantity <= 0;

        if (isExpired) {
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.error_border));
            holder.cardRoot.setCardBackgroundColor(Color.parseColor("#FFF5F5"));
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText("EXPIRED");
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_expired);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.error_red));
        } else if (isZeroQty) {
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.border));
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card2));
            holder.tvStatusBadge.setVisibility(View.VISIBLE);
            holder.tvStatusBadge.setText("OUT OF STOCK");
            holder.tvStatusBadge.setBackgroundColor(Color.TRANSPARENT);
            holder.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.fg_dim));
        } else {
            holder.cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.border));
            holder.cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card));
            holder.tvStatusBadge.setVisibility(View.GONE);
        }

        if (isReadOnly) {
            holder.btnEdit.setVisibility(View.GONE);
            holder.btnDelete.setVisibility(View.GONE);
        } else {
            holder.btnEdit.setVisibility(View.VISIBLE);
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> {
                if (actionListener != null) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        actionListener.onEdit(batches.get(pos), pos);
                    }
                }
            });
            holder.btnDelete.setOnClickListener(v -> {
                if (actionListener != null) {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        actionListener.onDelete(batches.get(pos), pos);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return batches.size();
    }

    static class BatchViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView tvBatchNo, tvBatchQuantity, tvExpiry, tvPurchasePrice, tvSellingPrice, tvStatusBadge;
        ImageButton btnEdit, btnDelete;

        public BatchViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_batch_row);
            tvBatchNo = itemView.findViewById(R.id.tv_batch_no);
            tvBatchQuantity = itemView.findViewById(R.id.tv_batch_quantity);
            tvExpiry = itemView.findViewById(R.id.tv_batch_expiry);
            tvPurchasePrice = itemView.findViewById(R.id.tv_batch_purchase_price);
            tvSellingPrice = itemView.findViewById(R.id.tv_batch_selling_price);
            tvStatusBadge = itemView.findViewById(R.id.tv_batch_status_badge);
            btnEdit = itemView.findViewById(R.id.btn_edit_batch);
            btnDelete = itemView.findViewById(R.id.btn_delete_batch);
        }
    }
}
