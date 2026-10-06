package in.gbtsolutions.inventoryhub.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.models.UnitOfMeasure;
import in.gbtsolutions.inventoryhub.repository.UnitOfMeasureRepository;

public class AddUnitOfMeasureActivity extends BaseActivity {

    private EditText inputUomName;
    private EditText inputUomDesc;
    private View bannerError;
    private TextView textError;
    private View bannerDefaultLocked;
    private View layoutQuickChips;

    private View btnSaveUom;
    private TextView textSaveButton;
    private View containerSaveIdle;
    private ProgressBar progressSaving;

    private View btnDeleteUomAction;

    private int uomId = -1;
    private boolean isDefault = false;
    private long createdAt = 0;

    private UnitOfMeasureRepository uomRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_unit_of_measure);

        uomRepository = new UnitOfMeasureRepository(getApplication());
        uomId = getIntent().getIntExtra("uom_id", -1);
        isDefault = getIntent().getBooleanExtra("is_default", false);
        boolean isEditMode = uomId > 0;

        Toolbar toolbar = findViewById(R.id.toolbar);
        String screenTitle = isEditMode
                ? (isDefault ? "Unit of Measure Details" : "Edit Unit of Measure")
                : "Add Unit of Measure";
        setupToolbar(toolbar, screenTitle, true);

        View headerContainer = findViewById(R.id.header_container);
        View scrollContainer = findViewById(R.id.scroll_container);
        applyEdgeToEdgeInsets(headerContainer, scrollContainer);

        initViews(isEditMode);
        setupChips();
    }

    private void initViews(boolean isEditMode) {
        inputUomName = findViewById(R.id.input_uom_name);
        inputUomDesc = findViewById(R.id.input_uom_desc);
        bannerError = findViewById(R.id.banner_error);
        textError = findViewById(R.id.text_error);
        bannerDefaultLocked = findViewById(R.id.banner_default_locked);
        layoutQuickChips = findViewById(R.id.layout_quick_chips);

        btnSaveUom = findViewById(R.id.btn_save_uom);
        textSaveButton = findViewById(R.id.text_save_button);
        containerSaveIdle = findViewById(R.id.container_save_idle);
        progressSaving = findViewById(R.id.progress_saving);
        btnDeleteUomAction = findViewById(R.id.btn_delete_uom_action);

        if (isEditMode) {
            String existingName = getIntent().getStringExtra("uom_name");
            String existingDesc = getIntent().getStringExtra("uom_desc");
            createdAt = getIntent().getLongExtra("created_at", System.currentTimeMillis());

            if (existingName != null) {
                inputUomName.setText(existingName);
                inputUomName.setSelection(inputUomName.getText().length());
            }
            if (existingDesc != null) {
                inputUomDesc.setText(existingDesc);
            }

            if (isDefault) {
                // Default units cannot be edited or deleted
                bannerDefaultLocked.setVisibility(View.VISIBLE);
                inputUomName.setEnabled(false);
                inputUomDesc.setEnabled(false);
                btnSaveUom.setVisibility(View.GONE);
                btnDeleteUomAction.setVisibility(View.GONE);
                if (layoutQuickChips != null) {
                    layoutQuickChips.setVisibility(View.GONE);
                }
            } else {
                bannerDefaultLocked.setVisibility(View.GONE);
                textSaveButton.setText("Update Unit of Measure");
                btnDeleteUomAction.setVisibility(View.VISIBLE);
                btnDeleteUomAction.setOnClickListener(v -> confirmDelete());
            }
        } else {
            bannerDefaultLocked.setVisibility(View.GONE);
            btnDeleteUomAction.setVisibility(View.GONE);
        }

        btnSaveUom.setOnClickListener(v -> saveUom());
    }

    private void setupChips() {
        int[] chipIds = new int[]{
                R.id.chip_box,
                R.id.chip_dozen,
                R.id.chip_bundle,
                R.id.chip_meter,
                R.id.chip_roll,
                R.id.chip_carton,
                R.id.chip_pair,
                R.id.chip_set
        };

        for (int chipId : chipIds) {
            TextView chip = findViewById(chipId);
            if (chip != null) {
                chip.setOnClickListener(v -> {
                    if (!isDefault) {
                        inputUomName.setText(chip.getText());
                        inputUomName.setSelection(inputUomName.getText().length());
                        hideError();
                    }
                });
            }
        }
    }

    private void saveUom() {
        if (isDefault) {
            showError("Default units of measure cannot be modified.");
            return;
        }

        hideError();

        String name = inputUomName.getText() != null ? inputUomName.getText().toString().trim() : "";
        String desc = inputUomDesc.getText() != null ? inputUomDesc.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            showError("Please enter a unit name.");
            inputUomName.requestFocus();
            return;
        }

        setLoading(true);

        UnitOfMeasure uom = new UnitOfMeasure(name, desc, false);
        if (uomId > 0) {
            uom.uomId = uomId;
            uom.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
            uom.updatedAt = System.currentTimeMillis();
            uomRepository.update(uom, new UnitOfMeasureRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    showToast("Unit of measure \"" + name + "\" updated successfully");
                    finish();
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    showError(message != null ? message : "Failed to update unit of measure.");
                }
            });
        } else {
            uomRepository.insert(uom, new UnitOfMeasureRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    showToast("Unit of measure \"" + name + "\" added successfully");
                    finish();
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    showError(message != null ? message : "Failed to save unit of measure.");
                }
            });
        }
    }

    private void confirmDelete() {
        if (isDefault) {
            showToast("Default units of measure cannot be deleted.");
            return;
        }

        final String name = inputUomName.getText() != null ? inputUomName.getText().toString().trim() : "this unit";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Delete Unit of Measure")
                .setMessage("Are you sure you want to delete \"" + name + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    UnitOfMeasure uom = new UnitOfMeasure();
                    uom.uomId = uomId;
                    uom.name = name;
                    uom.isDefault = false;

                    setLoading(true);
                    uomRepository.delete(uom, new UnitOfMeasureRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            setLoading(false);
                            showToast("Unit of measure \"" + name + "\" deleted successfully");
                            finish();
                        }

                        @Override
                        public void onError(String message) {
                            setLoading(false);
                            showError(message != null ? message : "Failed to delete unit of measure.");
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setLoading(boolean loading) {
        btnSaveUom.setEnabled(!loading);
        if (btnDeleteUomAction != null) {
            btnDeleteUomAction.setEnabled(!loading);
        }
        containerSaveIdle.setVisibility(loading ? View.GONE : View.VISIBLE);
        progressSaving.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        textError.setText(message);
        bannerError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        bannerError.setVisibility(View.GONE);
    }
}
