package in.gbtsolutions.inventoryhub.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.CommonFunctions;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;

public class BatchSelectAdapter extends RecyclerView.Adapter<BatchSelectAdapter.BatchSelectViewHolder> {

    private final List<ProductBatch> batches = new ArrayList<>();
    private final Map<Integer, Double> selectedQuantities = new HashMap<>(); // batchId -> qty
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
    private Product product;
    private OnQuantityChangedListener quantityChangedListener;

    public BatchSelectAdapter() {
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public void setInitialQuantities(Map<Integer, Double> initQtys) {
        this.selectedQuantities.clear();
        if (initQtys != null) {
            this.selectedQuantities.putAll(initQtys);
        }
        notifyDataSetChanged();
    }

    public void setQuantityChangedListener(OnQuantityChangedListener listener) {
        this.quantityChangedListener = listener;
    }

    public Map<Integer, Double> getSelectedQuantities() {
        return selectedQuantities;
    }

    public List<ProductBatch> getBatches() {
        return batches;
    }

    public void setBatches(List<ProductBatch> newBatches) {
        this.batches.clear();
        if (newBatches != null) {
            this.batches.addAll(newBatches);
        }
        notifyDataSetChanged();
    }

    public double getTotalSelectedQuantity() {
        double total = 0.0;
        for (double q : selectedQuantities.values()) {
            total += q;
        }
        return CommonFunctions.roundTo3Decimals(total);
    }

    public int getSelectedBatchCount() {
        int count = 0;
        for (double q : selectedQuantities.values()) {
            if (q > 0.0001) count++;
        }
        return count;
    }

    @NonNull
    @Override
    public BatchSelectViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_batch_select_row, parent, false);
        return new BatchSelectViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BatchSelectViewHolder holder, int position) {
        ProductBatch batch = batches.get(position);
        holder.bind(batch);
    }

    @Override
    public int getItemCount() {
        return batches.size();
    }

    public interface OnQuantityChangedListener {
        void onQuantityChanged();
    }

    class BatchSelectViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardRoot;
        TextView tvBatchNo, tvExpiredTag, tvAvail, tvExpiry, tvPrice;
        ImageButton btnMinus, btnPlus;
        EditText etQty;
        private TextWatcher currentWatcher;

        public BatchSelectViewHolder(@NonNull View itemView) {
            super(itemView);
            cardRoot = itemView.findViewById(R.id.card_batch_select_row);
            tvBatchNo = itemView.findViewById(R.id.tv_select_batch_no);
            tvExpiredTag = itemView.findViewById(R.id.tv_select_batch_expired_tag);
            tvAvail = itemView.findViewById(R.id.tv_select_batch_avail);
            tvExpiry = itemView.findViewById(R.id.tv_select_batch_expiry);
            tvPrice = itemView.findViewById(R.id.tv_select_batch_price);
            btnMinus = itemView.findViewById(R.id.btn_qty_minus);
            btnPlus = itemView.findViewById(R.id.btn_qty_plus);
            etQty = itemView.findViewById(R.id.et_select_qty);
        }

        public void bind(ProductBatch batch) {
            Context context = itemView.getContext();
            tvBatchNo.setText(batch.batchNo != null ? batch.batchNo : "Batch #" + batch.batchId);
            tvAvail.setText("Avail: " + CommonFunctions.formatQuantity(batch.quantity));

            double price = batch.sellingPrice > 0 ? batch.sellingPrice : (product != null ? product.sellingPrice : 0);
            tvPrice.setText(String.format(Locale.getDefault(), "₹%.2f / unit", price));

            if (batch.expiryDate > 0) {
                tvExpiry.setText("Exp: " + dateFormat.format(new Date(batch.expiryDate)));
                tvExpiry.setVisibility(View.VISIBLE);
            } else {
                tvExpiry.setVisibility(View.GONE);
            }

            boolean isExpired = batch.isExpired();
            if (isExpired) {
                tvExpiredTag.setVisibility(View.VISIBLE);
                cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.error_border));
                cardRoot.setCardBackgroundColor(Color.parseColor("#FFF5F5"));
            } else {
                tvExpiredTag.setVisibility(View.GONE);
                cardRoot.setStrokeColor(ContextCompat.getColor(context, R.color.border));
                cardRoot.setCardBackgroundColor(ContextCompat.getColor(context, R.color.card));
            }

            boolean isDecimal = product != null && CommonFunctions.isDecimalUnit(product.unitOfMeasure);
            if (isDecimal) {
                etQty.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                etQty.setFilters(new InputFilter[]{new CommonFunctions.DecimalDigitsInputFilter(6, 3)});
            } else {
                etQty.setInputType(InputType.TYPE_CLASS_NUMBER);
                etQty.setFilters(new InputFilter[]{new InputFilter.LengthFilter(6)});
            }

            double currentQty = selectedQuantities.containsKey(batch.batchId) ? selectedQuantities.get(batch.batchId) : 0.0;

            if (currentWatcher != null) {
                etQty.removeTextChangedListener(currentWatcher);
            }

            etQty.setText(CommonFunctions.formatQuantity(currentQty));

            btnPlus.setEnabled(currentQty < batch.quantity);
            btnMinus.setEnabled(currentQty > 0.0001);

            btnPlus.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                double q = selectedQuantities.containsKey(batch.batchId) ? selectedQuantities.get(batch.batchId) : 0.0;
                double step = 1.0;
                if (q + step <= batch.quantity + 0.0001) {
                    q = CommonFunctions.roundTo3Decimals(Math.min(batch.quantity, q + step));
                    selectedQuantities.put(batch.batchId, q);
                    etQty.setText(CommonFunctions.formatQuantity(q));
                    btnPlus.setEnabled(q < batch.quantity);
                    btnMinus.setEnabled(q > 0.0001);
                    if (quantityChangedListener != null)
                        quantityChangedListener.onQuantityChanged();
                } else {
                    Toast.makeText(context, "Maximum available in batch is " + CommonFunctions.formatQuantity(batch.quantity), Toast.LENGTH_SHORT).show();
                }
            });

            btnMinus.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                double q = selectedQuantities.containsKey(batch.batchId) ? selectedQuantities.get(batch.batchId) : 0.0;
                double step = 1.0;
                if (q > 0.0001) {
                    q = CommonFunctions.roundTo3Decimals(Math.max(0.0, q - step));
                    if (q <= 0.0001) {
                        selectedQuantities.remove(batch.batchId);
                        q = 0.0;
                    } else {
                        selectedQuantities.put(batch.batchId, q);
                    }
                    etQty.setText(CommonFunctions.formatQuantity(q));
                    btnPlus.setEnabled(q < batch.quantity);
                    btnMinus.setEnabled(q > 0.0001);
                    if (quantityChangedListener != null)
                        quantityChangedListener.onQuantityChanged();
                }
            });

            currentWatcher = new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    String str = s != null ? s.toString().trim() : "";
                    double val = 0.0;
                    if (!TextUtils.isEmpty(str)) {
                        try {
                            val = Double.parseDouble(str);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                    val = CommonFunctions.roundTo3Decimals(val);
                    if (val > batch.quantity) {
                        val = batch.quantity;
                        Toast.makeText(context, "Maximum available in batch is " + CommonFunctions.formatQuantity(batch.quantity), Toast.LENGTH_SHORT).show();
                        etQty.setText(CommonFunctions.formatQuantity(val));
                        etQty.setSelection(etQty.getText().length());
                    }
                    if (val <= 0.0001) {
                        selectedQuantities.remove(batch.batchId);
                    } else {
                        selectedQuantities.put(batch.batchId, val);
                    }
                    btnPlus.setEnabled(val < batch.quantity);
                    btnMinus.setEnabled(val > 0.0001);
                    if (quantityChangedListener != null)
                        quantityChangedListener.onQuantityChanged();
                }
            };
            etQty.addTextChangedListener(currentWatcher);
        }
    }
}
