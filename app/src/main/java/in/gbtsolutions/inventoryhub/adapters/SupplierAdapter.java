package in.gbtsolutions.inventoryhub.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.models.Suppliers;

public class SupplierAdapter extends RecyclerView.Adapter<SupplierAdapter.SupplierViewHolder> {

    public interface OnSupplierClickListener {
        void onSupplierClick(@NonNull Suppliers supplier);
    }

    public interface OnSupplierEditListener {
        void onSupplierEdit(@NonNull Suppliers supplier);
    }

    public interface OnSupplierStatusToggleListener {
        void onSupplierStatusToggle(@NonNull Suppliers supplier);
    }

    private final List<Suppliers> supplierList = new ArrayList<>();
    private OnSupplierClickListener clickListener;
    private OnSupplierEditListener editListener;
    private OnSupplierStatusToggleListener statusToggleListener;

    public SupplierAdapter() {
    }

    public void setOnSupplierClickListener(OnSupplierClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnSupplierEditListener(OnSupplierEditListener listener) {
        this.editListener = listener;
    }

    public void setOnSupplierStatusToggleListener(OnSupplierStatusToggleListener listener) {
        this.statusToggleListener = listener;
    }

    public void setSuppliers(@NonNull List<Suppliers> newSuppliers) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return supplierList.size();
            }

            @Override
            public int getNewListSize() {
                return newSuppliers.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return supplierList.get(oldItemPosition).supplierId == newSuppliers.get(newItemPosition).supplierId;
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                Suppliers oldSup = supplierList.get(oldItemPosition);
                Suppliers newSup = newSuppliers.get(newItemPosition);
                return TextUtils.equals(oldSup.supplierName, newSup.supplierName)
                        && TextUtils.equals(oldSup.contactPerson, newSup.contactPerson)
                        && TextUtils.equals(oldSup.phone, newSup.phone)
                        && TextUtils.equals(oldSup.email, newSup.email)
                        && TextUtils.equals(oldSup.city, newSup.city)
                        && TextUtils.equals(oldSup.gst, newSup.gst)
                        && oldSup.isActive == newSup.isActive;
            }
        });

        supplierList.clear();
        supplierList.addAll(newSuppliers);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public SupplierViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_supplier, parent, false);
        return new SupplierViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SupplierViewHolder holder, int position) {
        Suppliers supplier = supplierList.get(position);
        holder.bind(supplier);
    }

    @Override
    public int getItemCount() {
        return supplierList.size();
    }

    public class SupplierViewHolder extends RecyclerView.ViewHolder {
        private final View cardRoot;
        private final TextView textName;
        private final TextView textStatus;
        private final TextView textContact;
        private final TextView textSubtext;
        private final FrameLayout btnToggleStatus;
        private final ImageView iconToggleStatus;
        private final FrameLayout btnEdit;

        public SupplierViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_supplier_root);
            textName = itemView.findViewById(R.id.text_supplier_name);
            textStatus = itemView.findViewById(R.id.text_supplier_status);
            textContact = itemView.findViewById(R.id.text_supplier_contact);
            textSubtext = itemView.findViewById(R.id.text_supplier_subtext);
            btnToggleStatus = itemView.findViewById(R.id.btn_toggle_status_supplier);
            iconToggleStatus = itemView.findViewById(R.id.icon_toggle_status);
            btnEdit = itemView.findViewById(R.id.btn_edit_supplier);
        }

        public void bind(@NonNull Suppliers supplier) {
            String name = supplier.supplierName != null ? supplier.supplierName.trim() : "";
            textName.setText(name.isEmpty() ? "Unnamed Supplier" : name);

            // Active / Deactivated status styling
            if (supplier.isActive) {
                textStatus.setText("Active");
                textStatus.setBackgroundResource(R.drawable.bg_status_chip);
                textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.accent_teal));
                iconToggleStatus.setImageResource(R.drawable.ic_power);
                iconToggleStatus.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.accent_teal));
                cardRoot.setAlpha(1.0f);
            } else {
                textStatus.setText("Deactivated");
                textStatus.setBackgroundResource(R.drawable.bg_field_normal);
                textStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.fg_dim));
                iconToggleStatus.setImageResource(R.drawable.ic_power);
                iconToggleStatus.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.fg_dim));
                cardRoot.setAlpha(0.78f);
            }

            // Contact line: Person + Phone
            StringBuilder contactBuilder = new StringBuilder();
            if (!TextUtils.isEmpty(supplier.contactPerson)) {
                contactBuilder.append(supplier.contactPerson.trim());
            }
            if (!TextUtils.isEmpty(supplier.phone)) {
                if (contactBuilder.length() > 0) {
                    contactBuilder.append(" • ");
                }
                contactBuilder.append(supplier.phone.trim());
            }
            if (contactBuilder.length() > 0) {
                textContact.setVisibility(View.VISIBLE);
                textContact.setText(contactBuilder.toString());
            } else {
                textContact.setVisibility(View.GONE);
            }

            // Subtext line: City / State + GST
            StringBuilder subtextBuilder = new StringBuilder();
            if (!TextUtils.isEmpty(supplier.city)) {
                subtextBuilder.append(supplier.city.trim());
                if (!TextUtils.isEmpty(supplier.stateCode)) {
                    subtextBuilder.append(", ").append(supplier.stateCode.trim());
                }
            } else if (!TextUtils.isEmpty(supplier.stateCode)) {
                subtextBuilder.append(supplier.stateCode.trim());
            }

            if (!TextUtils.isEmpty(supplier.gst)) {
                if (subtextBuilder.length() > 0) {
                    subtextBuilder.append(" • GST: ");
                } else {
                    subtextBuilder.append("GST: ");
                }
                subtextBuilder.append(supplier.gst.trim());
            }

            if (subtextBuilder.length() > 0) {
                textSubtext.setVisibility(View.VISIBLE);
                textSubtext.setText(subtextBuilder.toString());
            } else if (!TextUtils.isEmpty(supplier.email)) {
                textSubtext.setVisibility(View.VISIBLE);
                textSubtext.setText(supplier.email.trim());
            } else {
                textSubtext.setVisibility(View.GONE);
            }

            // Clicks
            cardRoot.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onSupplierClick(supplier);
                }
            });

            btnEdit.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onSupplierEdit(supplier);
                }
            });

            btnToggleStatus.setOnClickListener(v -> {
                if (statusToggleListener != null) {
                    statusToggleListener.onSupplierStatusToggle(supplier);
                }
            });
        }
    }
}
