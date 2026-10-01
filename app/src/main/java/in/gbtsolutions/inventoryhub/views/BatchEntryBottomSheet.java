package in.gbtsolutions.inventoryhub.views;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.BatchNumberGenerator;
import in.gbtsolutions.inventoryhub.models.ProductBatch;
import in.gbtsolutions.inventoryhub.repository.ProductBatchRepository;

public class BatchEntryBottomSheet extends BottomSheetDialogFragment {

    public interface OnBatchSavedListener {
        void onBatchSaved(ProductBatch batch, int editPosition);
    }

    private ProductBatch existingBatch;
    private int editPosition = -1;
    private double defaultUnitPrice = 0;
    private double defaultSellingPrice = 0;
    private long selectedExpiryEpoch = 0;
    private OnBatchSavedListener listener;
    private ProductBatchRepository repository;

    private TextInputEditText etBatchNo, etQty, etPurchasePrice, etSellingPrice, etExpiry;
    private TextView tvTitle;
    private MaterialButton btnSave, btnCancel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public static BatchEntryBottomSheet newInstance(@Nullable ProductBatch batch, int position,
                                                    double defaultUnitCost, double defaultSellPrice) {
        BatchEntryBottomSheet fragment = new BatchEntryBottomSheet();
        fragment.existingBatch = batch;
        fragment.editPosition = position;
        fragment.defaultUnitPrice = defaultUnitCost;
        fragment.defaultSellingPrice = defaultSellPrice;
        return fragment;
    }

    public void setOnBatchSavedListener(OnBatchSavedListener listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_batch_entry, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        repository = new ProductBatchRepository(requireActivity().getApplication());

        tvTitle = view.findViewById(R.id.dialog_batch_title);
        etBatchNo = view.findViewById(R.id.et_batch_no);
        etQty = view.findViewById(R.id.et_batch_qty);
        etPurchasePrice = view.findViewById(R.id.et_batch_purchase_price);
        etSellingPrice = view.findViewById(R.id.et_batch_selling_price);
        etExpiry = view.findViewById(R.id.et_batch_expiry);
        btnSave = view.findViewById(R.id.btn_save_batch);
        btnCancel = view.findViewById(R.id.btn_cancel_batch);

        btnCancel.setOnClickListener(v -> dismiss());

        etExpiry.setOnClickListener(v -> showDatePicker());

        if (existingBatch != null) {
            tvTitle.setText("Edit Batch");
            btnSave.setText("Update Batch");
            etBatchNo.setText(existingBatch.batchNo);
            etQty.setText(String.valueOf(existingBatch.quantity));
            etPurchasePrice.setText(existingBatch.purchasePrice > 0 ? String.valueOf(existingBatch.purchasePrice) : "");
            etSellingPrice.setText(existingBatch.sellingPrice > 0 ? String.valueOf(existingBatch.sellingPrice) : "");
            if (existingBatch.expiryDate > 0) {
                selectedExpiryEpoch = existingBatch.expiryDate;
                etExpiry.setText(dateFormat.format(new Date(selectedExpiryEpoch)));
            }
        } else {
            tvTitle.setText("Add Batch");
            btnSave.setText("Add Batch");
            if (defaultUnitPrice > 0) etPurchasePrice.setText(String.valueOf(defaultUnitPrice));
            if (defaultSellingPrice > 0) etSellingPrice.setText(String.valueOf(defaultSellingPrice));

            // Auto-generate batch number BN-YYDDD-NNN
            BatchNumberGenerator.generate(repository, generatedNo -> {
                if (isAdded() && etBatchNo != null && TextUtils.isEmpty(etBatchNo.getText())) {
                    etBatchNo.setText(generatedNo);
                }
            });
        }

        btnSave.setOnClickListener(v -> saveBatch());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        if (selectedExpiryEpoch > 0) {
            cal.setTimeInMillis(selectedExpiryEpoch);
        }
        DatePickerDialog dpd = new DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            Calendar chosen = Calendar.getInstance();
            chosen.set(year, month, dayOfMonth, 23, 59, 59);
            selectedExpiryEpoch = chosen.getTimeInMillis();
            etExpiry.setText(dateFormat.format(chosen.getTime()));
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    private void saveBatch() {
        String batchNo = etBatchNo.getText() != null ? etBatchNo.getText().toString().trim() : "";
        String qtyStr = etQty.getText() != null ? etQty.getText().toString().trim() : "";
        String costStr = etPurchasePrice.getText() != null ? etPurchasePrice.getText().toString().trim() : "";
        String sellStr = etSellingPrice.getText() != null ? etSellingPrice.getText().toString().trim() : "";

        if (TextUtils.isEmpty(qtyStr)) {
            etQty.setError("Quantity is required");
            etQty.requestFocus();
            return;
        }

        int qty;
        try {
            qty = Integer.parseInt(qtyStr);
        } catch (NumberFormatException e) {
            etQty.setError("Invalid quantity");
            return;
        }

        double cost = 0;
        if (!TextUtils.isEmpty(costStr)) {
            try {
                cost = Double.parseDouble(costStr);
            } catch (NumberFormatException ignored) {}
        }

        double sell = 0;
        if (!TextUtils.isEmpty(sellStr)) {
            try {
                sell = Double.parseDouble(sellStr);
            } catch (NumberFormatException ignored) {}
        }

        ProductBatch batch;
        if (existingBatch != null) {
            batch = existingBatch;
            batch.batchNo = batchNo;
            batch.quantity = qty;
            batch.purchasePrice = cost;
            batch.sellingPrice = sell;
            batch.expiryDate = selectedExpiryEpoch;
            batch.updatedAt = System.currentTimeMillis();
        } else {
            long now = System.currentTimeMillis();
            batch = new ProductBatch(0, batchNo, qty, cost, sell, selectedExpiryEpoch, now, now);
        }

        if (listener != null) {
            listener.onBatchSaved(batch, editPosition);
        }
        dismiss();
    }
}
