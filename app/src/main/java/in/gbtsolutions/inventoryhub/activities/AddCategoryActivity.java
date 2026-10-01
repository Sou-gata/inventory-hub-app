package in.gbtsolutions.inventoryhub.activities;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.CategoryIconPickerAdapter;
import in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper;
import in.gbtsolutions.inventoryhub.helpers.CategoryIconHelper;
import in.gbtsolutions.inventoryhub.models.AuditTrail;
import in.gbtsolutions.inventoryhub.models.Category;
import in.gbtsolutions.inventoryhub.repository.CategoryRepository;

public class AddCategoryActivity extends BaseActivity {

    private EditText inputCategoryName;
    private EditText inputCategoryDesc;
    private View bannerError;
    private TextView textError;
    private View btnSaveCategory;
    private TextView textSaveButton;
    private View containerSaveIdle;
    private ProgressBar progressSaving;

    // Category Icon views
    private View cardSelectIcon;
    private ImageView imgSelectedCategoryIcon;
    private TextView textSelectedIconLabel;
    private String selectedIconKey = CategoryIconHelper.DEFAULT_ICON_KEY;

    private int categoryId = -1;
    private long createdAt = 0;

    private CategoryRepository categoryRepository;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_category);

        categoryRepository = new CategoryRepository(getApplication());
        categoryId = getIntent().getIntExtra("category_id", -1);
        boolean isEditMode = categoryId > 0;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, isEditMode ? "Edit Category" : "Add Category", true);

        View headerContainer = findViewById(R.id.header_container);
        View scrollContainer = findViewById(R.id.scroll_container);
        applyEdgeToEdgeInsets(headerContainer, scrollContainer);

        initViews(isEditMode);
        setupChips();
    }

    private void initViews(boolean isEditMode) {
        inputCategoryName = findViewById(R.id.input_category_name);
        inputCategoryDesc = findViewById(R.id.input_category_desc);
        bannerError = findViewById(R.id.banner_error);
        textError = findViewById(R.id.text_error);
        btnSaveCategory = findViewById(R.id.btn_save_category);
        textSaveButton = findViewById(R.id.text_save_button);
        containerSaveIdle = findViewById(R.id.container_save_idle);
        progressSaving = findViewById(R.id.progress_saving);

        // Icon picker views
        cardSelectIcon = findViewById(R.id.card_select_icon);
        imgSelectedCategoryIcon = findViewById(R.id.img_selected_category_icon);
        textSelectedIconLabel = findViewById(R.id.text_selected_icon_label);

        if (cardSelectIcon != null) {
            cardSelectIcon.setOnClickListener(v -> showIconPickerDialog());
        }

        if (isEditMode) {
            textSaveButton.setText("Update Category");
            String existingName = getIntent().getStringExtra("category_name");
            String existingDesc = getIntent().getStringExtra("category_desc");
            String existingIcon = getIntent().getStringExtra("category_icon");
            createdAt = getIntent().getLongExtra("created_at", System.currentTimeMillis());

            if (existingName != null) {
                inputCategoryName.setText(existingName);
                inputCategoryName.setSelection(inputCategoryName.getText().length());
            }
            if (existingDesc != null) {
                inputCategoryDesc.setText(existingDesc);
            }
            updateIconPreview(existingIcon);
        } else {
            updateIconPreview(CategoryIconHelper.DEFAULT_ICON_KEY);
        }

        btnSaveCategory.setOnClickListener(v -> saveCategory());
    }

    private void updateIconPreview(String iconKey) {
        selectedIconKey = CategoryIconHelper.sanitizeIconKey(iconKey);
        if (imgSelectedCategoryIcon != null) {
            imgSelectedCategoryIcon.setImageResource(CategoryIconHelper.getIconResId(this, selectedIconKey));
        }
        if (textSelectedIconLabel != null) {
            textSelectedIconLabel.setText(CategoryIconHelper.getIconLabel(selectedIconKey));
        }
    }

    private void showIconPickerDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_category_icon_picker, null);
        dialog.setContentView(sheetView);

        RecyclerView recyclerIcons = sheetView.findViewById(R.id.recycler_icons);
        EditText editSearchIcon = sheetView.findViewById(R.id.edit_search_icon);
        ImageView btnClearSearch = sheetView.findViewById(R.id.btn_clear_search_icon);
        View btnClose = sheetView.findViewById(R.id.btn_close_picker);
        View layoutEmptySearch = sheetView.findViewById(R.id.layout_empty_search);
        TextView textEmptyQuery = sheetView.findViewById(R.id.text_empty_search_query);
        View btnResetSearch = sheetView.findViewById(R.id.btn_reset_icon_search);

        CategoryIconPickerAdapter adapter = new CategoryIconPickerAdapter(selectedIconKey, item -> {
            updateIconPreview(item.key);
            dialog.dismiss();
        });

        recyclerIcons.setLayoutManager(new GridLayoutManager(this, 4));
        recyclerIcons.setAdapter(adapter);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        editSearchIcon.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s != null ? s.toString().trim() : "";
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                }

                List<CategoryIconHelper.IconItem> filtered = CategoryIconHelper.filterIcons(query);
                adapter.setIcons(filtered);

                if (filtered.isEmpty()) {
                    recyclerIcons.setVisibility(View.GONE);
                    if (layoutEmptySearch != null) {
                        layoutEmptySearch.setVisibility(View.VISIBLE);
                    }
                    if (textEmptyQuery != null) {
                        textEmptyQuery.setText("No icons matching \"" + query + "\"");
                    }
                } else {
                    recyclerIcons.setVisibility(View.VISIBLE);
                    if (layoutEmptySearch != null) {
                        layoutEmptySearch.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> editSearchIcon.setText(""));
        }

        if (btnResetSearch != null) {
            btnResetSearch.setOnClickListener(v -> editSearchIcon.setText(""));
        }

        dialog.show();
    }

    private void setupChips() {
        int[] chipIds = new int[]{
                R.id.chip_electronics,
                R.id.chip_groceries,
                R.id.chip_apparel,
                R.id.chip_hardware,
                R.id.chip_stationery,
                R.id.chip_furniture,
                R.id.chip_healthcare
        };

        String[] chipIconKeys = new String[]{
                "electronics",
                "groceries",
                "apparel",
                "hardware",
                "stationery",
                "furniture",
                "healthcare"
        };

        for (int i = 0; i < chipIds.length; i++) {
            TextView chip = findViewById(chipIds[i]);
            final String iconKey = chipIconKeys[i];
            if (chip != null) {
                chip.setOnClickListener(v -> {
                    inputCategoryName.setText(chip.getText());
                    inputCategoryName.setSelection(inputCategoryName.getText().length());
                    updateIconPreview(iconKey);
                    hideError();
                });
            }
        }
    }

    private void saveCategory() {
        hideError();

        String name = inputCategoryName.getText() != null ? inputCategoryName.getText().toString().trim() : "";
        String desc = inputCategoryDesc.getText() != null ? inputCategoryDesc.getText().toString().trim() : "";
        String iconKey = CategoryIconHelper.sanitizeIconKey(selectedIconKey);

        if (TextUtils.isEmpty(name)) {
            showError("Please enter a category name.");
            inputCategoryName.requestFocus();
            return;
        }

        if (name.length() < 2) {
            showError("Category name must be at least 2 characters.");
            inputCategoryName.requestFocus();
            return;
        }

        setLoading(true);

        Category category = new Category(name, desc, iconKey);
        if (categoryId > 0) {
            category.categoryId = categoryId;
            category.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
            category.updatedAt = System.currentTimeMillis();
            categoryRepository.update(category, new CategoryRepository.CategoryActionCallback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    showToast("Category \"" + name + "\" updated successfully");
                    finish();
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    showError(message != null ? message : "Failed to update category.");
                }
            });
        } else {
            categoryRepository.insert(category, new CategoryRepository.CategoryActionCallback() {
                @Override
                public void onSuccess() {
                    setLoading(false);
                    showToast("Category \"" + name + "\" added successfully");
                    finish();
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    showError(message != null ? message : "Failed to save category.");
                }
            });
        }
    }

    private void setLoading(boolean loading) {
        btnSaveCategory.setEnabled(!loading);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
