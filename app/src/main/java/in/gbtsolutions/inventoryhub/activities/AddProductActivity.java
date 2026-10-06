package in.gbtsolutions.inventoryhub.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.core.content.ContextCompat;

import java.util.Locale;


import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.materialswitch.MaterialSwitch;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.Configurations;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.BatchListAdapter;
import in.gbtsolutions.inventoryhub.adapters.NoFilterArrayAdapter;
import in.gbtsolutions.inventoryhub.models.Category;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;
import in.gbtsolutions.inventoryhub.models.UnitOfMeasure;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.repository.CategoryRepository;
import in.gbtsolutions.inventoryhub.repository.ProductBatchRepository;
import in.gbtsolutions.inventoryhub.repository.UnitOfMeasureRepository;
import in.gbtsolutions.inventoryhub.repository.ProductRepository;
import in.gbtsolutions.inventoryhub.views.BatchEntryBottomSheet;

public class AddProductActivity extends BaseActivity {

    private EditText inputProductName;
    private TextView labelSku;
    private EditText inputSku;
    private TextView labelHsnCode;
    private EditText inputBrand;
    private AutoCompleteTextView dropdownCategory;
    private View containerCategory;
    private EditText inputUnitPrice;
    private EditText inputSellingPrice;
    private View containerInputQuantity;
    private EditText inputQuantity;
    private AutoCompleteTextView dropdownUnitOfMeasure;
    private View containerUnitOfMeasure;
    private final List<UnitOfMeasure> uomList = new ArrayList<>();
    private final List<String> uomNames = new ArrayList<>();
    private ArrayAdapter<String> uomAdapter;
    private EditText inputReorderLevel;
    private EditText inputReorderQuantity;
    private EditText inputHsnCode;
    private AutoCompleteTextView dropdownGst;
    private View containerGst;
    private EditText inputDefaultMarkupPercent;
    private AutoCompleteTextView dropdownStatus;
    private View containerStatus;
    private EditText inputDescription;

    private View bannerError;
    private TextView textError;

    private View btnSaveProduct;
    private TextView textSaveButton;
    private View containerSaveIdle;
    private ProgressBar progressSaving;

    // Batch Inventory Views
    private View sectionBatches;
    private TextView tvBatchTotalSummary;
    private View btnAddBatch;
    private androidx.recyclerview.widget.RecyclerView recyclerBatches;
    private BatchListAdapter batchListAdapter;
    private final List<ProductBatch> pendingBatches = new ArrayList<>();
    private final List<ProductBatch> deletedBatches = new ArrayList<>();
    private boolean isBatchEnabled = false; // per-product batch flag
    private MaterialSwitch swBatchEnabled;
    private ProductBatchRepository productBatchRepository;

    private int editProductId = -1;
    private int editCategoryId = -1;
    private long createdAt = 0;

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private final List<Category> categoryList = new ArrayList<>();
    private final List<String> categoryNames = new ArrayList<>();
    private ArrayAdapter<String> categoryAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_product);

        editProductId = getIntent().getIntExtra("product_id", -1);
        boolean isEditMode = editProductId > 0;

        productRepository = new ProductRepository(getApplication());
        categoryRepository = new CategoryRepository(getApplication());
        productBatchRepository = new ProductBatchRepository(getApplication());
        // Batch mode is per-product; will be loaded from intent extra in edit mode
        isBatchEnabled = getIntent().getBooleanExtra("batch_enabled", false);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, isEditMode ? "Edit Product" : "Add Product", true);

        View headerContainer = findViewById(R.id.header_container);
        View scrollContainer = findViewById(R.id.scroll_container);
        applyEdgeToEdgeInsets(headerContainer, scrollContainer);

        initViews(isEditMode);
        setupDropdowns();
        observeCategories();
        observeUnitsOfMeasure();
        setupBatchSection(isEditMode);
    }

    private void initViews(boolean isEditMode) {
        inputProductName = findViewById(R.id.input_product_name);
        labelSku = findViewById(R.id.label_sku);
        inputSku = findViewById(R.id.input_sku);
        inputBrand = findViewById(R.id.input_brand);
        dropdownCategory = findViewById(R.id.dropdown_category);
        containerCategory = findViewById(R.id.container_category);
        inputUnitPrice = findViewById(R.id.input_unit_price);
        inputSellingPrice = findViewById(R.id.input_selling_price);
        containerInputQuantity = findViewById(R.id.container_input_quantity);
        inputQuantity = findViewById(R.id.input_quantity);
        dropdownUnitOfMeasure = findViewById(R.id.dropdown_unit_of_measure);
        containerUnitOfMeasure = findViewById(R.id.container_unit_of_measure);
        inputReorderLevel = findViewById(R.id.input_reorder_level);
        inputReorderQuantity = findViewById(R.id.input_reorder_quantity);
        labelHsnCode = findViewById(R.id.label_hsn_code);
        inputHsnCode = findViewById(R.id.input_hsn_code);
        dropdownGst = findViewById(R.id.dropdown_gst);
        containerGst = findViewById(R.id.container_gst);
        inputDefaultMarkupPercent = findViewById(R.id.input_default_markup_percent);
        dropdownStatus = findViewById(R.id.dropdown_status);
        containerStatus = findViewById(R.id.container_status);
        inputDescription = findViewById(R.id.input_description);
        setupMandatoryIndicators();

        sectionBatches = findViewById(R.id.section_batches);
        tvBatchTotalSummary = findViewById(R.id.tv_batch_total_summary);
        btnAddBatch = findViewById(R.id.btn_add_batch);
        recyclerBatches = findViewById(R.id.recycler_batches);
        swBatchEnabled = findViewById(R.id.sw_batch_enabled);

        bannerError = findViewById(R.id.banner_error);
        textError = findViewById(R.id.text_error);

        btnSaveProduct = findViewById(R.id.btn_save_product);
        textSaveButton = findViewById(R.id.text_save_button);
        containerSaveIdle = findViewById(R.id.container_save_idle);
        progressSaving = findViewById(R.id.progress_saving);

        // Initialise toggle and batch section visibility from the per-product flag
        swBatchEnabled.setChecked(isBatchEnabled);
        applyBatchToggleState(isBatchEnabled);

        swBatchEnabled.setOnCheckedChangeListener((btn, checked) -> {
            isBatchEnabled = checked;
            applyBatchToggleState(checked);
        });

        if (isEditMode) {
            if (textSaveButton != null) {
                textSaveButton.setText("Update Product");
            }
            createdAt = getIntent().getLongExtra("created_at", System.currentTimeMillis());
            editCategoryId = getIntent().getIntExtra("category_id", -1);

            String name = getIntent().getStringExtra("product_name");
            if (name != null) inputProductName.setText(name);

            String sku = getIntent().getStringExtra("sku");
            if (sku != null) inputSku.setText(sku);

            String brand = getIntent().getStringExtra("brand");
            if (brand != null) inputBrand.setText(brand);

            double unitPrice = getIntent().getDoubleExtra("unit_price", 0.0);
            if (unitPrice > 0) inputUnitPrice.setText(String.valueOf(unitPrice));

            double sellingPrice = getIntent().getDoubleExtra("selling_price", 0.0);
            if (sellingPrice > 0) inputSellingPrice.setText(String.valueOf(sellingPrice));

            int qty = getIntent().getIntExtra("quantity", 0);
            inputQuantity.setText(String.valueOf(qty));

            String uom = getIntent().getStringExtra("unit_of_measure");
            if (uom != null) dropdownUnitOfMeasure.setText(uom, false);

            int reorderLvl = getIntent().getIntExtra("reorder_level", 5);
            inputReorderLevel.setText(String.valueOf(reorderLvl));

            int reorderQty = getIntent().getIntExtra("reorder_quantity", 10);
            inputReorderQuantity.setText(String.valueOf(reorderQty));

            String hsn = getIntent().getStringExtra("hsn_code");
            if (hsn != null) inputHsnCode.setText(hsn);

            double gst = getIntent().getDoubleExtra("gst_percent", 18.0);
            dropdownGst.setText(((int) gst) + "%", false);

            double markup = getIntent().getDoubleExtra("default_markup_percent", 0.0);
            if (markup > 0) inputDefaultMarkupPercent.setText(String.valueOf(markup));

            String status = getIntent().getStringExtra("status");
            if (status != null) dropdownStatus.setText(status, false);

            String desc = getIntent().getStringExtra("description");
            if (desc != null) inputDescription.setText(desc);
        }

        btnSaveProduct.setOnClickListener(v -> saveProduct());
    }

    private void setupDropdowns() {
        // Category Dropdown
        categoryAdapter = new NoFilterArrayAdapter<>(this, R.layout.item_dropdown, categoryNames);
        dropdownCategory.setAdapter(categoryAdapter);
        dropdownCategory.setDropDownBackgroundResource(R.drawable.bg_card);

        View.OnClickListener showCategoryDropdown = v -> {
            if (categoryList.isEmpty()) {
                if (AppModeManager.getInstance(this).isOnlineMode()) {
                    showToast("Loading categories from server, please wait...");
                    observeCategories();
                } else {
                    showToast("No categories found in database. Please add a category first.");
                }
                return;
            }
            dropdownCategory.showDropDown();
        };
        dropdownCategory.setOnClickListener(showCategoryDropdown);
        if (containerCategory != null) {
            containerCategory.setOnClickListener(showCategoryDropdown);
        }
        dropdownCategory.setOnItemClickListener((parent, view, position, id) -> hideError());

        // GST Percent Dropdown
        String[] gstOptions = getResources().getStringArray(R.array.gst_slabs);
        NoFilterArrayAdapter<String> gstAdapter = new NoFilterArrayAdapter<>(this, R.layout.item_dropdown, gstOptions);
        dropdownGst.setAdapter(gstAdapter);
        dropdownGst.setDropDownBackgroundResource(R.drawable.bg_card);
        dropdownGst.setOnClickListener(v -> dropdownGst.showDropDown());
        if (containerGst != null) {
            containerGst.setOnClickListener(v -> dropdownGst.showDropDown());
        }

        // Status Dropdown
        String[] statusOptions = getResources().getStringArray(R.array.product_status);
        NoFilterArrayAdapter<String> statusAdapter = new NoFilterArrayAdapter<>(this, R.layout.item_dropdown, statusOptions);
        dropdownStatus.setAdapter(statusAdapter);
        dropdownStatus.setDropDownBackgroundResource(R.drawable.bg_card);
        dropdownStatus.setOnClickListener(v -> dropdownStatus.showDropDown());
        if (containerStatus != null) {
            containerStatus.setOnClickListener(v -> dropdownStatus.showDropDown());
        }

        // Unit of Measure Dropdown
        uomAdapter = new NoFilterArrayAdapter<>(this, R.layout.item_dropdown, uomNames);
        dropdownUnitOfMeasure.setAdapter(uomAdapter);
        dropdownUnitOfMeasure.setDropDownBackgroundResource(R.drawable.bg_card);

        View.OnClickListener showUomDropdown = v -> {
            if (uomNames.isEmpty()) {
                showToast("No units of measure found. Please add units first.");
                return;
            }
            dropdownUnitOfMeasure.showDropDown();
        };
        dropdownUnitOfMeasure.setOnClickListener(showUomDropdown);
        if (containerUnitOfMeasure != null) {
            containerUnitOfMeasure.setOnClickListener(showUomDropdown);
        }
        dropdownUnitOfMeasure.setOnItemClickListener((parent, view, position, id) -> hideError());
    }

    private void observeCategories() {
        if (AppModeManager.getInstance(this).isOnlineMode()) {
            categoryRepository.fetchCategoriesOnline(new in.gbtsolutions.inventoryhub.online.repository.OnlineCategoryRepository.CategoryListCallback() {
                @Override
                public void onSuccess(List<Category> categories) {
                    populateCategories(categories);
                }

                @Override
                public void onError(String errorMessage) {
                    showToast("Failed to load online categories: " + errorMessage);
                }
            });
            return;
        }

        categoryRepository.getAllCategories().observe(this, this::populateCategories);
    }

    private void populateCategories(List<Category> categories) {
        categoryList.clear();
        categoryNames.clear();
        if (categories != null && !categories.isEmpty()) {
            categoryList.addAll(categories);
            for (Category category : categories) {
                if (category.categoryName != null) {
                    categoryNames.add(category.categoryName);
                }
            }
        }
        categoryAdapter.notifyDataSetChanged();

        String currentText = dropdownCategory.getText().toString().trim();
        boolean hasMatch = false;

        if (editCategoryId > 0) {
            for (Category category : categoryList) {
                if (category.categoryId == editCategoryId && category.categoryName != null) {
                    dropdownCategory.setText(category.categoryName, false);
                    hasMatch = true;
                    break;
                }
            }
        }

        if (!hasMatch) {
            for (Category category : categoryList) {
                if (category.categoryName != null && category.categoryName.equalsIgnoreCase(currentText)) {
                    hasMatch = true;
                    if (!currentText.equals(category.categoryName)) {
                        dropdownCategory.setText(category.categoryName, false);
                    }
                    break;
                }
            }
        }

        if (!hasMatch) {
            if (!categoryList.isEmpty()) {
                dropdownCategory.setText(categoryList.get(0).categoryName, false);
            } else {
                dropdownCategory.setText("", false);
            }
        }
    }

    private void observeUnitsOfMeasure() {
        UnitOfMeasureRepository uomRepo = new UnitOfMeasureRepository(getApplication());
        uomRepo.getAllUnitsOfMeasure().observe(this, this::populateUnitsOfMeasure);
    }

    private void populateUnitsOfMeasure(List<UnitOfMeasure> units) {
        uomList.clear();
        uomNames.clear();
        if (units != null && !units.isEmpty()) {
            uomList.addAll(units);
            for (UnitOfMeasure u : units) {
                if (u.name != null && !u.name.trim().isEmpty()) {
                    uomNames.add(u.name.trim());
                }
            }
        }
        if (uomAdapter != null) {
            uomAdapter.notifyDataSetChanged();
        }

        String currentText = dropdownUnitOfMeasure != null ? dropdownUnitOfMeasure.getText().toString().trim() : "";
        boolean hasMatch = false;

        for (String name : uomNames) {
            if (name.equalsIgnoreCase(currentText)) {
                hasMatch = true;
                if (!currentText.equals(name)) {
                    dropdownUnitOfMeasure.setText(name, false);
                }
                break;
            }
        }

        if (!hasMatch && dropdownUnitOfMeasure != null) {
            if (editProductId > 0) {
                String existingUom = getIntent().getStringExtra("unit_of_measure");
                if (existingUom != null && !existingUom.trim().isEmpty()) {
                    for (String itemUom : uomNames) {
                        if (itemUom.equalsIgnoreCase(existingUom.trim())) {
                            dropdownUnitOfMeasure.setText(itemUom, false);
                            hasMatch = true;
                            break;
                        }
                    }
                    if (!hasMatch) {
                        dropdownUnitOfMeasure.setText(existingUom.trim(), false);
                        hasMatch = true;
                    }
                }
            }
            if (!hasMatch && !uomNames.isEmpty()) {
                if (uomNames.contains("Pics")) {
                    dropdownUnitOfMeasure.setText("Pics", false);
                } else if (uomNames.contains("KG")) {
                    dropdownUnitOfMeasure.setText("KG", false);
                } else {
                    dropdownUnitOfMeasure.setText(uomNames.get(0), false);
                }
            }
        }
    }

    /**
     * Applies the batch toggle state: shows/hides the batch section and quantity input.
     */
    private void applyBatchToggleState(boolean enabled) {
        if (containerInputQuantity != null) {
            containerInputQuantity.setVisibility(enabled ? View.GONE : View.VISIBLE);
        }
        if (sectionBatches != null) {
            sectionBatches.setVisibility(enabled ? View.VISIBLE : View.GONE);
        }
        if (enabled && batchListAdapter == null) {
            // Lazy-init the batch recycler when first enabled
            setupBatchRecycler();
        }
    }

    private void setupBatchSection(boolean isEditMode) {
        setupBatchRecycler();

        if (isEditMode && editProductId > 0 && isBatchEnabled) {
            productBatchRepository.getAllBatchesForProduct(editProductId).observe(this, batches -> {
                if (batches != null) {
                    pendingBatches.clear();
                    pendingBatches.addAll(batches);
                    batchListAdapter.setBatches(pendingBatches);
                    updateBatchSummary();
                }
            });
        } else {
            if (batchListAdapter != null) {
                batchListAdapter.setBatches(pendingBatches);
            }
            updateBatchSummary();
        }
    }

    private void setupBatchRecycler() {
        if (batchListAdapter != null) return; // already set up
        batchListAdapter = new BatchListAdapter();
        recyclerBatches.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        recyclerBatches.setAdapter(batchListAdapter);

        batchListAdapter.setActionListener(new BatchListAdapter.OnBatchActionListener() {
            @Override
            public void onEdit(ProductBatch batch, int position) {
                double unitCost = parseDoubleSafe(inputUnitPrice.getText().toString().trim());
                double sellPrice = parseDoubleSafe(inputSellingPrice.getText().toString().trim());
                BatchEntryBottomSheet dialog = BatchEntryBottomSheet.newInstance(batch, position, unitCost, sellPrice);
                dialog.setOnBatchSavedListener((updatedBatch, editPos) -> {
                    if (editPos >= 0 && editPos < pendingBatches.size()) {
                        pendingBatches.set(editPos, updatedBatch);
                        batchListAdapter.notifyItemChanged(editPos);
                        updateBatchSummary();
                    }
                });
                dialog.show(getSupportFragmentManager(), "BatchEntryBottomSheet");
            }

            @Override
            public void onDelete(ProductBatch batch, int position) {
                if (position >= 0 && position < pendingBatches.size()) {
                    ProductBatch removed = pendingBatches.remove(position);
                    if (removed.batchId > 0) {
                        deletedBatches.add(removed);
                    }
                    batchListAdapter.notifyItemRemoved(position);
                    updateBatchSummary();
                }
            }
        });

        btnAddBatch.setOnClickListener(v -> {
            double unitCost = parseDoubleSafe(inputUnitPrice.getText().toString().trim());
            double sellPrice = parseDoubleSafe(inputSellingPrice.getText().toString().trim());
            BatchEntryBottomSheet dialog = BatchEntryBottomSheet.newInstance(null, -1, unitCost, sellPrice);
            dialog.setOnBatchSavedListener((newBatch, editPos) -> {
                pendingBatches.add(newBatch);
                batchListAdapter.notifyItemInserted(pendingBatches.size() - 1);
                updateBatchSummary();
            });
            dialog.show(getSupportFragmentManager(), "BatchEntryBottomSheet");
        });
    }

    private void updateBatchSummary() {
        int totalQty = 0;
        for (ProductBatch b : pendingBatches) {
            totalQty += b.quantity;
        }
        if (tvBatchTotalSummary != null) {
            tvBatchTotalSummary.setText(String.format(java.util.Locale.getDefault(),
                    "Total stock: %d pcs across %d batch(es)", totalQty, pendingBatches.size()));
        }
    }

    private double parseDoubleSafe(String s) {
        if (TextUtils.isEmpty(s)) return 0;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String getMandatoryFieldSetting() {
        String val = GlobalStore.getInstance().getCsvMandatoryField();
        if (val == null || val.trim().isEmpty()) {
            SharedPreferences prefs = getSharedPreferences(Configurations.PREF_NAME, MODE_PRIVATE);
            val = prefs.getString(Configurations.KEY_CSV_MANDATORY_FIELD, Configurations.MANDATORY_FIELD_HSN);
        }
        return val != null ? val.trim().toLowerCase(Locale.ROOT) : Configurations.MANDATORY_FIELD_HSN;
    }

    private CharSequence formatRequired(String label) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(label);
        ssb.append(" ");
        int start = ssb.length();
        ssb.append("*");
        ssb.setSpan(new ForegroundColorSpan(ContextCompat.getColor(this, R.color.error_red)),
                start, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

    private void setupMandatoryIndicators() {
        String mandatory = getMandatoryFieldSetting();
        if (Configurations.MANDATORY_FIELD_HSN.equalsIgnoreCase(mandatory)) {
            if (labelSku != null) labelSku.setText("SKU / Barcode");
            if (labelHsnCode != null) labelHsnCode.setText(formatRequired("HSN / SAC Code"));
        } else if (Configurations.MANDATORY_FIELD_BOTH.equalsIgnoreCase(mandatory)) {
            if (labelSku != null) labelSku.setText(formatRequired("SKU / Barcode"));
            if (labelHsnCode != null) labelHsnCode.setText(formatRequired("HSN / SAC Code"));
        } else {
            if (labelSku != null) labelSku.setText(formatRequired("SKU / Barcode"));
            if (labelHsnCode != null) labelHsnCode.setText("HSN / SAC Code");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupMandatoryIndicators();
    }

    private void saveProduct() {
        hideError();

        String name = inputProductName.getText().toString().trim();
        String sku = inputSku.getText().toString().trim();
        String brand = inputBrand.getText().toString().trim();
        String unitOfMeasure = dropdownUnitOfMeasure != null ? dropdownUnitOfMeasure.getText().toString().trim() : "";
        String description = inputDescription.getText().toString().trim();
        String unitPriceStr = inputUnitPrice.getText().toString().trim();
        String sellingPriceStr = inputSellingPrice.getText().toString().trim();
        String quantityStr = inputQuantity.getText().toString().trim();
        String reorderLevelStr = inputReorderLevel.getText().toString().trim();
        String reorderQuantityStr = inputReorderQuantity.getText().toString().trim();

        // Validation
        if (TextUtils.isEmpty(name)) {
            showError("Please enter product name.");
            inputProductName.requestFocus();
            return;
        }

        String mandatory = getMandatoryFieldSetting();
        String hsnCode = inputHsnCode.getText().toString().trim();
        boolean skuBlank = TextUtils.isEmpty(sku);
        boolean hsnBlank = TextUtils.isEmpty(hsnCode);

        if (Configurations.MANDATORY_FIELD_HSN.equalsIgnoreCase(mandatory)) {
            if (hsnBlank) {
                showError("Please enter HSN / SAC Code.");
                inputHsnCode.requestFocus();
                return;
            }
        } else if (Configurations.MANDATORY_FIELD_BOTH.equalsIgnoreCase(mandatory)) {
            if (skuBlank && hsnBlank) {
                showError("Please enter both SKU / Barcode and HSN / SAC Code.");
                inputSku.requestFocus();
                return;
            } else if (skuBlank) {
                showError("Please enter SKU / Barcode.");
                inputSku.requestFocus();
                return;
            } else if (hsnBlank) {
                showError("Please enter HSN / SAC Code.");
                inputHsnCode.requestFocus();
                return;
            }
        } else {
            // Default: SKU is mandatory
            if (skuBlank) {
                showError("Please enter SKU / Barcode.");
                inputSku.requestFocus();
                return;
            }
        }

        if (TextUtils.isEmpty(sellingPriceStr)) {
            showError("Please enter selling price.");
            inputSellingPrice.requestFocus();
            return;
        }

        double sellingPrice;
        try {
            sellingPrice = Double.parseDouble(sellingPriceStr);
            if (sellingPrice < 0) {
                showError("Selling price cannot be negative.");
                inputSellingPrice.requestFocus();
                return;
            }
        } catch (NumberFormatException e) {
            showError("Please enter a valid selling price.");
            inputSellingPrice.requestFocus();
            return;
        }

        int quantity = 0;
        if (isBatchEnabled) {
            for (ProductBatch b : pendingBatches) {
                quantity += b.quantity;
            }
        } else {
            if (TextUtils.isEmpty(quantityStr)) {
                showError("Please enter stock quantity.");
                inputQuantity.requestFocus();
                return;
            }

            try {
                quantity = Integer.parseInt(quantityStr);
                if (quantity < 0) {
                    showError("Quantity cannot be negative.");
                    inputQuantity.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Please enter a valid integer for quantity.");
                inputQuantity.requestFocus();
                return;
            }
        }

        double unitPrice = 0.0;
        if (!TextUtils.isEmpty(unitPriceStr)) {
            try {
                unitPrice = Double.parseDouble(unitPriceStr);
            } catch (NumberFormatException e) {
                showError("Please enter a valid cost/unit price.");
                inputUnitPrice.requestFocus();
                return;
            }
        }

        int reorderLevel = 5;
        if (!TextUtils.isEmpty(reorderLevelStr)) {
            try {
                reorderLevel = Integer.parseInt(reorderLevelStr);
            } catch (NumberFormatException ignored) {
            }
        }

        int reorderQuantity = 10;
        if (!TextUtils.isEmpty(reorderQuantityStr)) {
            try {
                reorderQuantity = Integer.parseInt(reorderQuantityStr);
            } catch (NumberFormatException ignored) {
            }
        }

        String markupPercentStr = inputDefaultMarkupPercent.getText().toString().trim();

        // Category selection
        String categoryName = dropdownCategory.getText().toString().trim();
        Category selectedCategory = getCategoryByName(categoryName);
        if (selectedCategory == null) {
            if (categoryList.isEmpty()) {
                showError("No categories found. Please add a category first.");
            } else {
                showError("Please select a category.");
            }
            dropdownCategory.requestFocus();
            return;
        }
        int categoryId = selectedCategory.categoryId;

        String status = dropdownStatus.getText().toString().trim();
        if (TextUtils.isEmpty(status)) {
            status = "Active";
        }

        String gstPercentStr = dropdownGst.getText().toString().trim().replace("%", "");
        double gstPercent = 18.0;
        if (!TextUtils.isEmpty(gstPercentStr)) {
            try {
                gstPercent = Double.parseDouble(gstPercentStr);
            } catch (NumberFormatException ignored) {
            }
        }

        double defaultMarkupPercent = 0.0;
        if (!TextUtils.isEmpty(markupPercentStr)) {
            try {
                defaultMarkupPercent = Double.parseDouble(markupPercentStr);
                if (defaultMarkupPercent < 0) {
                    showError("Default markup percentage cannot be negative.");
                    inputDefaultMarkupPercent.requestFocus();
                    return;
                }
            } catch (NumberFormatException e) {
                showError("Please enter a valid markup percentage.");
                inputDefaultMarkupPercent.requestFocus();
                return;
            }
        }

        if (TextUtils.isEmpty(unitOfMeasure)) {
            showError("Please select a unit of measure.");
            if (dropdownUnitOfMeasure != null) dropdownUnitOfMeasure.requestFocus();
            return;
        }

        boolean validUnit = false;
        for (String itemUom : uomNames) {
            if (itemUom.equalsIgnoreCase(unitOfMeasure)) {
                unitOfMeasure = itemUom;
                validUnit = true;
                break;
            }
        }

        if (!validUnit && !uomNames.isEmpty()) {
            showError("Please select a valid unit of measure from the available list.");
            return;
        }

        long now = System.currentTimeMillis();

        String finalSku = skuBlank ? null : sku;
        String finalHsnCode = hsnBlank ? null : hsnCode;

        Product product = new Product(
                name,
                categoryId,
                finalSku,
                description,
                brand,
                unitOfMeasure,
                reorderLevel,
                reorderQuantity,
                status,
                finalHsnCode,
                gstPercent,
                defaultMarkupPercent,
                now,
                now,
                unitPrice,
                sellingPrice,
                quantity,
                isBatchEnabled
        );

        if (isBatchEnabled && !pendingBatches.isEmpty()) {
            product.setBatches(new ArrayList<>(pendingBatches));
        }

        setLoading(true);

        if (editProductId > 0) {
            product.productId = editProductId;
            product.createdAt = createdAt > 0 ? createdAt : now;
            product.updatedAt = now;

            productRepository.update(product, new ProductRepository.ProductUpdateCallback() {
                @Override
                public void onSuccess() {
                    if (isBatchEnabled) {
                        if (AppModeManager.getInstance(AddProductActivity.this).isOnlineMode()) {
                            List<ProductBatch> newBatches = new ArrayList<>();
                            for (ProductBatch b : pendingBatches) {
                                if (b.batchId <= 0) {
                                    newBatches.add(b);
                                }
                            }
                            if (!newBatches.isEmpty()) {
                                saveNewOnlineBatches(editProductId, newBatches, 0);
                                return;
                            }
                            setLoading(false);
                            showToast("Product updated successfully!");
                            finish();
                            return;
                        }

                        for (ProductBatch d : deletedBatches) {
                            productBatchRepository.deleteBatch(d, null);
                        }
                        productBatchRepository.saveBatchesForProduct(editProductId, pendingBatches, new ProductBatchRepository.BatchActionCallback() {
                            @Override
                            public void onSuccess() {
                                setLoading(false);
                                showToast("Product updated successfully!");
                                finish();
                            }

                            @Override
                            public void onError(String message) {
                                setLoading(false);
                                showToast("Product updated, but failed to save some batches: " + message);
                                finish();
                            }
                        });
                    } else {
                        setLoading(false);
                        showToast("Product updated successfully!");
                        finish();
                    }
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    showError(message);
                }
            });
        } else {
            productRepository.insert(product, new ProductRepository.ProductInsertCallback() {
                @Override
                public void onSuccess(long rowId) {
                    int newProductId = (int) rowId;
                    if (isBatchEnabled && !pendingBatches.isEmpty()) {
                        if (AppModeManager.getInstance(AddProductActivity.this).isOnlineMode()) {
                            setLoading(false);
                            showToast("Product and batches saved successfully!");
                            finish();
                            return;
                        }

                        productBatchRepository.saveBatchesForProduct(newProductId, pendingBatches, new ProductBatchRepository.BatchActionCallback() {
                            @Override
                            public void onSuccess() {
                                setLoading(false);
                                showToast("Product and batches saved successfully!");
                                finish();
                            }

                            @Override
                            public void onError(String message) {
                                setLoading(false);
                                showToast("Product saved, but failed to save batches: " + message);
                                finish();
                            }
                        });
                    } else {
                        setLoading(false);
                        showToast("Product saved successfully!");
                        finish();
                    }
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    showError(message);
                }
            });
        }
    }

    private void saveNewOnlineBatches(int productId, List<ProductBatch> newBatches, int index) {
        if (index >= newBatches.size()) {
            setLoading(false);
            showToast("Product and batches updated successfully!");
            finish();
            return;
        }
        ProductBatch b = newBatches.get(index);
        productRepository.getOnlineRepository().createBatchForProduct(productId, b, new in.gbtsolutions.inventoryhub.online.repository.OnlineProductRepository.ProductActionCallback() {
            @Override
            public void onSuccess(Product product) {
                saveNewOnlineBatches(productId, newBatches, index + 1);
            }

            @Override
            public void onError(String errorMessage) {
                saveNewOnlineBatches(productId, newBatches, index + 1);
            }
        });
    }

    private void setLoading(boolean loading) {
        btnSaveProduct.setEnabled(!loading);
        containerSaveIdle.setVisibility(loading ? View.GONE : View.VISIBLE);
        progressSaving.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        if (textError != null) {
            textError.setText(message);
        }
        if (bannerError != null) {
            bannerError.setVisibility(View.VISIBLE);
        }
    }

    private void hideError() {
        if (bannerError != null) {
            bannerError.setVisibility(View.GONE);
        }
    }

    private Category getCategoryByName(String name) {
        if (TextUtils.isEmpty(name)) {
            return null;
        }
        for (Category category : categoryList) {
            if (category.categoryName != null && category.categoryName.trim().equalsIgnoreCase(name.trim())) {
                return category;
            }
        }
        return null;
    }
}

