package in.gbtsolutions.inventoryhub.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.R;

public class ImportErrorAdapter extends RecyclerView.Adapter<ImportErrorAdapter.ErrorViewHolder> {

    private final List<ImportErrorItem> items = new ArrayList<>();

    public void setItems(List<ImportErrorItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ErrorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_import_error, parent, false);
        return new ErrorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ErrorViewHolder holder, int position) {
        ImportErrorItem item = items.get(position);
        holder.textLine.setText(item.lineDisplay != null ? "Line " + item.lineDisplay : "Error");

        if (item.sku != null && !item.sku.trim().isEmpty()) {
            holder.textSku.setVisibility(View.VISIBLE);
            holder.textSku.setText("SKU: " + item.sku.trim());
        } else {
            holder.textSku.setVisibility(View.GONE);
        }

        if (item.hsnCode != null && !item.hsnCode.trim().isEmpty()) {
            holder.textHsn.setVisibility(View.VISIBLE);
            holder.textHsn.setText("HSN: " + item.hsnCode.trim());
        } else {
            holder.textHsn.setVisibility(View.GONE);
        }

        holder.textReason.setText(item.reason != null ? item.reason : "Unknown error");
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ImportErrorItem {
        public final String lineDisplay;
        public final String sku;
        public final String hsnCode;
        public final String reason;

        public ImportErrorItem(String lineDisplay, String sku, String hsnCode, String reason) {
            this.lineDisplay = lineDisplay;
            this.sku = sku;
            this.hsnCode = hsnCode;
            this.reason = reason;
        }
    }

    static class ErrorViewHolder extends RecyclerView.ViewHolder {
        final TextView textLine;
        final TextView textSku;
        final TextView textHsn;
        final TextView textReason;

        ErrorViewHolder(@NonNull View itemView) {
            super(itemView);
            textLine = itemView.findViewById(R.id.text_error_line);
            textSku = itemView.findViewById(R.id.text_error_sku);
            textHsn = itemView.findViewById(R.id.text_error_hsn);
            textReason = itemView.findViewById(R.id.text_error_reason);
        }
    }
}
