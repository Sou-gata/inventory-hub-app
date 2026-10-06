package in.gbtsolutions.inventoryhub.views;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.BatchSelectAdapter;
import in.gbtsolutions.inventoryhub.helpers.SellCartManager;
import in.gbtsolutions.inventoryhub.models.CartItem;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;
import in.gbtsolutions.inventoryhub.repository.ProductBatchRepository;

public class BatchSelectDialog extends BottomSheetDialogFragment {

    public interface OnBatchesSelectedListener {
        void onBatchesSelected(Product product, List<BatchSelection> selections);
    }

    public static class BatchSelection {
        public ProductBatch batch;
        public double quantity;

        public BatchSelection(ProductBatch batch, double quantity) {
            this.batch = batch;
            this.quantity = quantity;
        }

        public BatchSelection(ProductBatch batch, int quantity) {
            this(batch, (double) quantity);
        }
    }

    private Product product;
    private OnBatchesSelectedListener listener;
    private ProductBatchRepository repository;

    private TextView tvTitle, tvEmpty, tvSummaryQty, tvSummaryBatches;
    private RecyclerView recyclerView;
    private MaterialButton btnConfirm;
    private BatchSelectAdapter adapter;

    public static BatchSelectDialog newInstance(Product product) {
        BatchSelectDialog dialog = new BatchSelectDialog();
        dialog.product = product;
        return dialog;
    }

    public void setOnBatchesSelectedListener(OnBatchesSelectedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_batch_select, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ProductBatchRepository(requireActivity().getApplication());

        tvTitle = view.findViewById(R.id.tv_select_dialog_title);
        tvEmpty = view.findViewById(R.id.tv_select_dialog_empty);
        tvSummaryQty = view.findViewById(R.id.tv_select_summary_qty);
        tvSummaryBatches = view.findViewById(R.id.tv_select_summary_batches);
        recyclerView = view.findViewById(R.id.recycler_select_batches);
        btnConfirm = view.findViewById(R.id.btn_confirm_batch_selection);

        if (product != null) {
            tvTitle.setText("Select Batches — " + product.productName);
        }

        adapter = new BatchSelectAdapter();
        adapter.setProduct(product);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        adapter.setQuantityChangedListener(this::updateSummary);

        // Pre-fill existing quantities in cart for this product
        Map<Integer, Double> existingInCart = new HashMap<>();
        List<CartItem> currentCart = SellCartManager.getInstance().getCartItems();
        for (CartItem ci : currentCart) {
            if (ci.product != null && ci.product.productId == (product != null ? product.productId : -1)) {
                if (ci.batchId != null) {
                    existingInCart.put(ci.batchId, ci.quantity);
                }
            }
        }
        adapter.setInitialQuantities(existingInCart);
        updateSummary();

        // Load active batches (quantity > 0)
        if (product != null) {
            repository.getActiveBatchesForProduct(product.productId).observe(getViewLifecycleOwner(), batches -> {
                if (batches == null || batches.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.setBatches(batches);
                }
                updateSummary();
            });
        }

        btnConfirm.setOnClickListener(v -> confirmSelection());
    }

    private void updateSummary() {
        if (adapter == null) return;
        double totalQty = adapter.getTotalSelectedQuantity();
        int batchCount = adapter.getSelectedBatchCount();
        if (tvSummaryQty != null) {
            String uom = (product != null && !TextUtils.isEmpty(product.unitOfMeasure)) ? " " + product.unitOfMeasure.trim() : "";
            tvSummaryQty.setText("Total Qty: " + in.gbtsolutions.inventoryhub.helpers.CommonFunctions.formatQuantity(totalQty) + uom);
        }
        if (tvSummaryBatches != null) {
            tvSummaryBatches.setText(batchCount + " batch(es) selected");
        }
    }

    private void confirmSelection() {
        if (adapter == null || product == null) {
            dismiss();
            return;
        }

        Map<Integer, Double> selectedMap = adapter.getSelectedQuantities();
        List<ProductBatch> allBatches = adapter.getBatches();
        List<BatchSelection> result = new ArrayList<>();

        for (ProductBatch b : allBatches) {
            if (selectedMap.containsKey(b.batchId)) {
                Double q = selectedMap.get(b.batchId);
                if (q != null && q > 0.0001) {
                    result.add(new BatchSelection(b, q));
                }
            }
        }

        if (listener != null) {
            listener.onBatchesSelected(product, result);
        }
        dismiss();
    }
}
