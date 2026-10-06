package in.gbtsolutions.inventoryhub.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.BatchListAdapter;
import in.gbtsolutions.inventoryhub.adapters.ImportErrorAdapter;
import in.gbtsolutions.inventoryhub.adapters.ProductAdapter;
import in.gbtsolutions.inventoryhub.dao.CategoryDao;
import in.gbtsolutions.inventoryhub.dao.ProductBatchDao;
import in.gbtsolutions.inventoryhub.dao.ProductDao;
import in.gbtsolutions.inventoryhub.helpers.BitmapHelper;
import in.gbtsolutions.inventoryhub.helpers.CategoryIconHelper;
import in.gbtsolutions.inventoryhub.helpers.CommonFunctions;
import in.gbtsolutions.inventoryhub.helpers.ProductCsvParser;
import in.gbtsolutions.inventoryhub.helpers.ProductQRHelper;
import in.gbtsolutions.inventoryhub.helpers.ThemeManager;
import in.gbtsolutions.inventoryhub.models.Category;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;
import in.gbtsolutions.inventoryhub.online.config.AppMode;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.online.paging.EndlessRecyclerScrollListener;
import in.gbtsolutions.inventoryhub.online.repository.OnlineCategoryRepository;
import in.gbtsolutions.inventoryhub.online.repository.OnlineProductRepository;
import in.gbtsolutions.inventoryhub.repository.CategoryRepository;
import in.gbtsolutions.inventoryhub.repository.ProductBatchRepository;
import in.gbtsolutions.inventoryhub.repository.ProductRepository;

public class InventoryActivity extends BaseActivity {

    private final List<Product> allProducts = new ArrayList<>();
    private final Map<Integer, String> categoryNames = new HashMap<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private ActivityResultLauncher<Intent> csvPickerLauncher;
    private BottomSheetDialog currentImportDialog;
    private Uri selectedCsvUri;
    private View layoutSelectFile;
    private View layoutFileSelected;
    private TextView textSelectedFileName;
    private TextView textSelectedFileSize;
    private View layoutValidationSuccess;
    private TextView textValidationSuccessDetails;
    private View layoutValidationError;
    private TextView textValidationErrorDetails;
    private View layoutImportProgress;
    private TextView textImportProgressStatus;
    private View layoutImportReport;
    private View layoutReportBanner;
    private ImageView imageReportStatus;
    private TextView textReportTitle;
    private TextView textReportSubtitle;
    private TextView textStatProductsCount;
    private TextView textStatCategoriesCount;
    private TextView textStatBatchesCount;
    private TextView textStatErrorsCount;
    private View containerErrorsList;
    private TextView textErrorsHeader;
    private RecyclerView recyclerImportErrors;
    private ImportErrorAdapter importErrorAdapter;
    private TextView btnStartImport;
    private View layoutReportActions;
    private TextView btnImportAnother;
    private TextView btnDoneImport;
    private NavigationView navView;
    private RecyclerView recyclerInventory;
    private ProductAdapter productAdapter;
    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private ProductBatchRepository productBatchRepository;
    private View layoutEmptyState;
    private View layoutListContainer;
    private TextView textInventoryCount;
    private TextView textMetaLabel;
    private EditText editSearchInventory;
    private ImageView btnClearSearch;
    private View layoutSearchEmpty;
    private TextView textSearchEmptyQuery;
    private View btnResetSearch;
    private TextView chipFilterAll;
    private TextView chipFilterInStock;
    private TextView chipFilterLowStock;
    private TextView chipFilterOutOfStock;
    private String currentSearchQuery = "";
    private FilterType currentFilter = FilterType.ALL;

    private AppModeManager appModeManager;
    private EndlessRecyclerScrollListener endlessScrollListener;
    private Runnable searchRunnable;
    private int onlineCurrentPage = 1;
    private boolean onlineHasMore = true;
    private boolean onlineIsLoading = false;
    private Toolbar toolbar;
    private AppModeManager.OnModeChangeListener modeChangeListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inventory);

        appModeManager = AppModeManager.getInstance(this);
        productRepository = new ProductRepository(getApplication());
        categoryRepository = new CategoryRepository(getApplication());
        productBatchRepository = new ProductBatchRepository(getApplication());

        toolbar = findViewById(R.id.toolbar);

        modeChangeListener = newMode -> {
            if (newMode == AppMode.ONLINE) {
                loadOnlineProducts(1, true);
            } else {
                productAdapter.clearProducts();
                productAdapter.setProducts(allProducts);
                applyFilters();
            }
        };
        appModeManager.addOnModeChangeListener(modeChangeListener);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        View headerContainer = findViewById(R.id.header_container);
        View contentContainer = findViewById(R.id.inventory_content_container);

        // Apply edge-to-edge system insets (status bar & nav bar)
        applyDrawerInsets(drawerLayout, headerContainer, contentContainer, navView);

        // Standardized sidebar and header navigation
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_inventory);

        setupCsvPickerLauncher();
        initViews();
        setupRecyclerView();
        setupSearch();
        setupFilterChips();
        handleIntentFilter(getIntent());
        observeData();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntentFilter(intent);
    }

    private void handleIntentFilter(Intent intent) {
        if (intent != null && intent.hasExtra("filter")) {
            String filter = intent.getStringExtra("filter");
            if ("low_stock".equalsIgnoreCase(filter)) {
                selectFilterChip(FilterType.LOW_STOCK);
            } else if ("in_stock".equalsIgnoreCase(filter)) {
                selectFilterChip(FilterType.IN_STOCK);
            } else if ("out_of_stock".equalsIgnoreCase(filter)) {
                selectFilterChip(FilterType.OUT_OF_STOCK);
            } else if ("all".equalsIgnoreCase(filter)) {
                selectFilterChip(FilterType.ALL);
            }
        }
    }

    private void setupCsvPickerLauncher() {
        csvPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            handleCsvFileSelected(uri);
                        }
                    }
                }
        );
    }

    private void initViews() {
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        layoutListContainer = findViewById(R.id.layout_list_container);
        textInventoryCount = findViewById(R.id.text_inventory_count);
        textMetaLabel = findViewById(R.id.text_meta_label);
        recyclerInventory = findViewById(R.id.recycler_inventory);

        editSearchInventory = findViewById(R.id.edit_search_inventory);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        layoutSearchEmpty = findViewById(R.id.layout_search_empty);
        textSearchEmptyQuery = findViewById(R.id.text_search_empty_query);
        btnResetSearch = findViewById(R.id.btn_reset_search);

        chipFilterAll = findViewById(R.id.chip_filter_all);
        chipFilterInStock = findViewById(R.id.chip_filter_in_stock);
        chipFilterLowStock = findViewById(R.id.chip_filter_low_stock);
        chipFilterOutOfStock = findViewById(R.id.chip_filter_out_of_stock);

        View btnImportCsv = findViewById(R.id.btn_import_csv);
        if (btnImportCsv != null) {
            btnImportCsv.setOnClickListener(v -> showImportCsvBottomSheet());
        }


        FloatingActionButton fabAddItem = findViewById(R.id.fab_add_item);
        if (fabAddItem != null) {
            fabAddItem.setOnClickListener(v -> openAddProduct());
        }

        View btnEmptyAddProduct = findViewById(R.id.btn_empty_add_product);
        if (btnEmptyAddProduct != null) {
            btnEmptyAddProduct.setOnClickListener(v -> openAddProduct());
        }
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerInventory.setLayoutManager(layoutManager);
        recyclerInventory.setAdapter(productAdapter);

        endlessScrollListener = new EndlessRecyclerScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (appModeManager != null && appModeManager.isOnlineMode() && onlineHasMore && !onlineIsLoading) {
                    loadOnlineProducts(page, false);
                }
            }
        };
        recyclerInventory.addOnScrollListener(endlessScrollListener);

        productAdapter.setOnProductClickListener(this::showProductDetailsSheet);
        productAdapter.setOnProductViewListener(this::showProductDetailsSheet);
        productAdapter.setOnProductEditListener(this::openEditProduct);
    }

    private void setupSearch() {
        if (editSearchInventory != null) {
            editSearchInventory.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s != null ? s.toString() : "";
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    currentSearchQuery = query.trim();

                    if (appModeManager != null && appModeManager.isOnlineMode()) {
                        if (searchRunnable != null) {
                            searchHandler.removeCallbacks(searchRunnable);
                        }
                        searchRunnable = () -> loadOnlineProducts(1, true);
                        searchHandler.postDelayed(searchRunnable, 400);
                    } else {
                        applyFilters();
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            editSearchInventory.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard(v);
                    editSearchInventory.clearFocus();
                    return true;
                }
                return false;
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (editSearchInventory != null) {
                    editSearchInventory.setText("");
                }
            });
        }

        if (btnResetSearch != null) {
            btnResetSearch.setOnClickListener(v -> {
                if (editSearchInventory != null) {
                    editSearchInventory.setText("");
                }
                selectFilterChip(FilterType.ALL);
            });
        }
    }

    private void setupFilterChips() {
        if (chipFilterAll != null) {
            chipFilterAll.setOnClickListener(v -> selectFilterChip(FilterType.ALL));
        }
        if (chipFilterInStock != null) {
            chipFilterInStock.setOnClickListener(v -> selectFilterChip(FilterType.IN_STOCK));
        }
        if (chipFilterLowStock != null) {
            chipFilterLowStock.setOnClickListener(v -> selectFilterChip(FilterType.LOW_STOCK));
        }
        if (chipFilterOutOfStock != null) {
            chipFilterOutOfStock.setOnClickListener(v -> selectFilterChip(FilterType.OUT_OF_STOCK));
        }
    }

    private void selectFilterChip(FilterType filterType) {
        currentFilter = filterType;

        updateChipState(chipFilterAll, filterType == FilterType.ALL);
        updateChipState(chipFilterInStock, filterType == FilterType.IN_STOCK);
        updateChipState(chipFilterLowStock, filterType == FilterType.LOW_STOCK);
        updateChipState(chipFilterOutOfStock, filterType == FilterType.OUT_OF_STOCK);

        if (appModeManager != null && appModeManager.isOnlineMode()) {
            loadOnlineProducts(1, true);
        } else {
            applyFilters();
        }
    }

    private void updateChipState(TextView chip, boolean isSelected) {
        if (chip == null) return;
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_button);
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_card);
            chip.setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
        }
    }

    private void observeData() {
        if (appModeManager != null && appModeManager.isOnlineMode()) {
            categoryRepository.fetchCategoriesOnline(new OnlineCategoryRepository.CategoryListCallback() {
                @Override
                public void onSuccess(List<Category> categories) {
                    categoryNames.clear();
                    if (categories != null) {
                        for (Category category : categories) {
                            if (category.categoryName != null) {
                                categoryNames.put(category.categoryId, category.categoryName);
                            }
                        }
                    }
                    if (productAdapter != null) {
                        productAdapter.setCategories(categories);
                    }
                }

                @Override
                public void onError(String errorMessage) {
                }
            });
            loadOnlineProducts(1, true);
            return;
        }

        categoryRepository.getAllCategories().observe(this, categories -> {
            categoryNames.clear();
            if (categories != null) {
                for (Category category : categories) {
                    if (category.categoryName != null) {
                        categoryNames.put(category.categoryId, category.categoryName);
                    }
                }
            }
            if (productAdapter != null) {
                productAdapter.setCategories(categories);
            }
            applyFilters();
        });

        productRepository.getAllProducts().observe(this, products -> {
            allProducts.clear();
            if (products != null) {
                allProducts.addAll(products);
            }
            applyFilters();
        });

        productBatchRepository.getAllEarliestExpiries().observe(this, tuples -> {
            Map<Integer, Long> expiryMap = new HashMap<>();
            if (tuples != null) {
                for (ProductBatchDao.ProductExpiryTuple tuple : tuples) {
                    expiryMap.put(tuple.product_id, tuple.earliest_expiry);
                }
            }
            if (productAdapter != null) {
                productAdapter.setEarliestExpiryMap(expiryMap);
            }
        });
    }

    private void applyFilters() {
        if (allProducts.isEmpty()) {
            layoutListContainer.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.GONE);
            }
            textInventoryCount.setText("0 Items");
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        layoutListContainer.setVisibility(View.VISIBLE);

        List<Product> filteredList = new ArrayList<>();
        String lowerQuery = currentSearchQuery.toLowerCase(Locale.getDefault());

        for (Product product : allProducts) {
            // 1. Stock Status Filter
            boolean matchesStock;
            switch (currentFilter) {
                case IN_STOCK:
                    matchesStock = product.quantity > product.reorderLevel;
                    break;
                case LOW_STOCK:
                    matchesStock = product.quantity <= product.reorderLevel && product.quantity > 0;
                    break;
                case OUT_OF_STOCK:
                    matchesStock = product.quantity <= 0;
                    break;
                case ALL:
                default:
                    matchesStock = true;
                    break;
            }

            if (!matchesStock) {
                continue;
            }

            // 2. Search Query Filter
            if (lowerQuery.isEmpty()) {
                filteredList.add(product);
            } else {
                boolean matchesName = product.productName != null &&
                        product.productName.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesSku = product.sku != null &&
                        product.sku.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesBrand = product.brand != null &&
                        product.brand.toLowerCase(Locale.getDefault()).contains(lowerQuery);

                String catName = categoryNames.get(product.categoryId);
                boolean matchesCategory = catName != null &&
                        catName.toLowerCase(Locale.getDefault()).contains(lowerQuery);

                if (matchesName || matchesSku || matchesBrand || matchesCategory) {
                    filteredList.add(product);
                }
            }
        }

        // Display results
        if (filteredList.isEmpty()) {
            recyclerInventory.setVisibility(View.GONE);
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.VISIBLE);
            }
            if (textSearchEmptyQuery != null) {
                if (!currentSearchQuery.isEmpty()) {
                    textSearchEmptyQuery.setText("No products found matching \"" + currentSearchQuery + "\"");
                } else {
                    textSearchEmptyQuery.setText("No products match the selected stock filter.");
                }
            }
            if (textMetaLabel != null) {
                textMetaLabel.setText("FILTERED RESULTS");
            }
            textInventoryCount.setText("0 Found");
        } else {
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.GONE);
            }
            recyclerInventory.setVisibility(View.VISIBLE);
            productAdapter.setProducts(filteredList);

            if (currentSearchQuery.isEmpty() && currentFilter == FilterType.ALL) {
                if (textMetaLabel != null) {
                    textMetaLabel.setText("ALL INVENTORY");
                }
                String countText = filteredList.size() + (filteredList.size() == 1 ? " Item" : " Items");
                textInventoryCount.setText(countText);
            } else {
                if (textMetaLabel != null) {
                    textMetaLabel.setText(currentSearchQuery.isEmpty() ? getFilterTitle() : "SEARCH RESULTS");
                }
                String countText = filteredList.size() + " of " + allProducts.size() + " Found";
                textInventoryCount.setText(countText);
            }
        }
    }

    private String getFilterTitle() {
        switch (currentFilter) {
            case IN_STOCK:
                return "IN STOCK ITEMS";
            case LOW_STOCK:
                return "LOW STOCK ITEMS";
            case OUT_OF_STOCK:
                return "OUT OF STOCK ITEMS";
            default:
                return "ALL INVENTORY";
        }
    }


    private void loadOnlineProducts(int page, boolean isNewSearch) {
        if (onlineIsLoading && !isNewSearch) return;
        onlineIsLoading = true;

        if (isNewSearch) {
            onlineCurrentPage = 1;
            onlineHasMore = true;
            if (endlessScrollListener != null) {
                endlessScrollListener.resetState();
            }
            productAdapter.clearProducts();
            textInventoryCount.setText("Loading...");
        }

        productAdapter.showLoadingFooter();

        String stockFilterParam;
        switch (currentFilter) {
            case IN_STOCK:
                stockFilterParam = "in_stock";
                break;
            case LOW_STOCK:
                stockFilterParam = "low_stock";
                break;
            case OUT_OF_STOCK:
                stockFilterParam = "out_of_stock";
                break;
            case ALL:
            default:
                stockFilterParam = "all";
                break;
        }

        productRepository.fetchProductsPaged(page, EndlessRecyclerScrollListener.PAGE_SIZE, currentSearchQuery, stockFilterParam, null,
                new OnlineProductRepository.PagedProductCallback() {
                    @Override
                    public void onSuccess(List<Product> products, int currentPage, int totalPages, int totalRecords, boolean hasMore) {
                        onlineIsLoading = false;
                        onlineCurrentPage = currentPage;
                        onlineHasMore = hasMore;
                        if (endlessScrollListener != null) {
                            endlessScrollListener.setHasMore(hasMore);
                            endlessScrollListener.setLoading(false);
                        }

                        productAdapter.removeFooter();
                        productAdapter.addProducts(products);

                        int totalLoaded = productAdapter.getProducts().size();
                        textInventoryCount.setText(String.format(Locale.getDefault(), "%d / %d Items", totalLoaded, totalRecords));

                        if (totalLoaded == 0) {
                            if (!currentSearchQuery.isEmpty()) {
                                layoutListContainer.setVisibility(View.GONE);
                                layoutEmptyState.setVisibility(View.GONE);
                                if (layoutSearchEmpty != null) {
                                    layoutSearchEmpty.setVisibility(View.VISIBLE);
                                    if (textSearchEmptyQuery != null) {
                                        textSearchEmptyQuery.setText("No products found matching \"" + currentSearchQuery + "\"");
                                    }
                                }
                            } else {
                                layoutListContainer.setVisibility(View.GONE);
                                layoutEmptyState.setVisibility(View.VISIBLE);
                                if (layoutSearchEmpty != null)
                                    layoutSearchEmpty.setVisibility(View.GONE);
                            }
                        } else {
                            layoutEmptyState.setVisibility(View.GONE);
                            if (layoutSearchEmpty != null)
                                layoutSearchEmpty.setVisibility(View.GONE);
                            layoutListContainer.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(String errorMessage, boolean sessionExpired) {
                        onlineIsLoading = false;
                        if (endlessScrollListener != null) {
                            endlessScrollListener.setLoading(false);
                        }
                        productAdapter.showRetryFooter("Error: " + errorMessage, () -> loadOnlineProducts(page, isNewSearch));
                        Toast.makeText(InventoryActivity.this, "Cloud: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void openAddProduct() {
        navigateTo(AddProductActivity.class, false);
    }

    private void openEditProduct(@NonNull Product product) {
        Intent intent = new Intent(this, AddProductActivity.class);
        intent.putExtra("product_id", product.productId);
        intent.putExtra("product_name", product.productName);
        intent.putExtra("sku", product.sku);
        intent.putExtra("brand", product.brand);
        intent.putExtra("category_id", product.categoryId);
        intent.putExtra("unit_price", product.unitPrice);
        intent.putExtra("selling_price", product.sellingPrice);
        intent.putExtra("quantity", product.quantity);
        intent.putExtra("unit_of_measure", product.unitOfMeasure);
        intent.putExtra("reorder_level", product.reorderLevel);
        intent.putExtra("reorder_quantity", product.reorderQuantity);
        intent.putExtra("hsn_code", product.hsnCode);
        intent.putExtra("gst_percent", product.gstPercent);
        intent.putExtra("default_markup_percent", product.defaultMarkupPercent);
        intent.putExtra("status", product.status);
        intent.putExtra("description", product.description);
        intent.putExtra("created_at", product.createdAt);
        intent.putExtra("batch_enabled", product.batchEnabled);
        startActivity(intent);
        applyTransition(this);
    }

    private void showProductDetailsSheet(@NonNull Product product) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_product_details, null);
        dialog.setContentView(view);

        TextView textName = view.findViewById(R.id.text_detail_name);
        TextView textBrand = view.findViewById(R.id.text_detail_brand);
        View btnClose = view.findViewById(R.id.btn_close_detail);
        TextView textSku = view.findViewById(R.id.text_detail_sku);
        TextView textCategory = view.findViewById(R.id.text_detail_category);
        View stockBadge = view.findViewById(R.id.layout_detail_stock_badge);
        View stockDot = view.findViewById(R.id.view_detail_stock_dot);
        TextView textStockStatus = view.findViewById(R.id.text_detail_stock_status);
        TextView textSellingPrice = view.findViewById(R.id.text_detail_selling_price);
        TextView textUnitPrice = view.findViewById(R.id.text_detail_unit_price);
        TextView textQuantity = view.findViewById(R.id.text_detail_quantity);
        TextView textReorder = view.findViewById(R.id.text_detail_reorder);
        TextView textTaxHsn = view.findViewById(R.id.text_detail_tax_hsn);
        TextView textStatus = view.findViewById(R.id.text_detail_status);
        View containerDescription = view.findViewById(R.id.container_detail_description);
        TextView textDescription = view.findViewById(R.id.text_detail_description);
        View btnEdit = view.findViewById(R.id.btn_detail_edit_product);

        textName.setText(product.productName != null ? product.productName : "Unnamed Product");

        if (!TextUtils.isEmpty(product.brand)) {
            textBrand.setVisibility(View.VISIBLE);
            textBrand.setText(String.format("Brand: %s", product.brand));
        } else {
            textBrand.setVisibility(View.GONE);
        }

        textSku.setText(!TextUtils.isEmpty(product.sku) ? product.sku : "No SKU");

        String categoryName = categoryNames.get(product.categoryId);
        if (!TextUtils.isEmpty(categoryName)) {
            textCategory.setVisibility(View.VISIBLE);
            textCategory.setText(categoryName);
        } else if (product.categoryId > 0) {
            textCategory.setVisibility(View.VISIBLE);
            textCategory.setText(String.format(Locale.getDefault(), "Cat #%d", product.categoryId));
        } else {
            textCategory.setVisibility(View.GONE);
        }

        String uom = !TextUtils.isEmpty(product.unitOfMeasure) ? product.unitOfMeasure : "pcs";
        double qty = product.quantity;
        int reorder = product.reorderLevel;

        if (qty <= 0) {
            stockBadge.setBackgroundResource(R.drawable.bg_stock_out);
            textStockStatus.setText("Out of Stock");
            int redColor = ContextCompat.getColor(this, R.color.error_red);
            textStockStatus.setTextColor(redColor);
            tintCircle(stockDot, redColor);
        } else if (qty <= reorder) {
            stockBadge.setBackgroundResource(R.drawable.bg_stock_low);
            textStockStatus.setText(String.format(Locale.getDefault(), "Low Stock: %s %s left", CommonFunctions.formatQuantity(qty), uom));
            int amberColor = ContextCompat.getColor(this, R.color.accent_amber);
            textStockStatus.setTextColor(amberColor);
            tintCircle(stockDot, amberColor);
        } else {
            stockBadge.setBackgroundResource(R.drawable.bg_stock_in);
            textStockStatus.setText("In Stock");
            int inStockColor = ContextCompat.getColor(this, R.color.status_green);
            textStockStatus.setTextColor(inStockColor);
            tintCircle(stockDot, inStockColor);
        }

        textSellingPrice.setText(String.format(Locale.getDefault(), "₹ %,.2f", product.sellingPrice));
        textUnitPrice.setText(String.format(Locale.getDefault(), "₹ %,.2f", product.unitPrice));
        textQuantity.setText(String.format(Locale.getDefault(), "%s %s", CommonFunctions.formatQuantity(qty), uom));

        textReorder.setText(String.format(Locale.getDefault(), "Alert at %d %s • Reorder %d %s",
                product.reorderLevel, uom, product.reorderQuantity, uom));

        String hsn = !TextUtils.isEmpty(product.hsnCode) ? product.hsnCode : "N/A";
        textTaxHsn.setText(String.format(Locale.getDefault(), "%.1f%% • HSN %s", product.gstPercent, hsn));

        textStatus.setText(!TextUtils.isEmpty(product.status) ? product.status : "Active");

        if (!TextUtils.isEmpty(product.description)) {
            containerDescription.setVisibility(View.VISIBLE);
            textDescription.setText(product.description);
        } else {
            containerDescription.setVisibility(View.GONE);
        }

        // Batch Inventory Section in Bottom Sheet
        View containerDetailBatches = view.findViewById(R.id.container_detail_batches);
        com.google.android.material.tabs.TabLayout tabLayoutBatches = view.findViewById(R.id.tab_layout_batches);
        RecyclerView recyclerDetailBatches = view.findViewById(R.id.recycler_detail_batches);
        TextView tvDetailBatchesEmpty = view.findViewById(R.id.tv_detail_batches_empty);

        boolean isBatchMode = product.batchEnabled;
        if (isBatchMode && containerDetailBatches != null) {
            containerDetailBatches.setVisibility(View.VISIBLE);
            BatchListAdapter detailBatchAdapter = new BatchListAdapter();
            detailBatchAdapter.setReadOnly(true);
            recyclerDetailBatches.setLayoutManager(new LinearLayoutManager(this));
            recyclerDetailBatches.setAdapter(detailBatchAdapter);

            tabLayoutBatches.removeAllTabs();
            tabLayoutBatches.addTab(tabLayoutBatches.newTab().setText("Active Batches"));
            tabLayoutBatches.addTab(tabLayoutBatches.newTab().setText("Stock History"));

            List<ProductBatch> allBatchesList = new ArrayList<>();
            List<ProductBatch> activeBatchesList = new ArrayList<>();

            if (appModeManager != null && appModeManager.isOnlineMode()) {
                if (product.getBatches() != null && !product.getBatches().isEmpty()) {
                    allBatchesList.addAll(product.getBatches());
                    for (ProductBatch b : product.getBatches()) {
                        if (b != null && b.quantity > 0) {
                            activeBatchesList.add(b);
                        }
                    }
                } else if (product.getBatch() != null) {
                    allBatchesList.add(product.getBatch());
                    if (product.getBatch().quantity > 0) {
                        activeBatchesList.add(product.getBatch());
                    }
                }
                int selectedTab = tabLayoutBatches.getSelectedTabPosition();
                List<ProductBatch> currentDisplay = (selectedTab == 1) ? allBatchesList : activeBatchesList;
                detailBatchAdapter.setBatches(currentDisplay);
                if (currentDisplay.isEmpty()) {
                    tvDetailBatchesEmpty.setVisibility(View.VISIBLE);
                    tvDetailBatchesEmpty.setText(selectedTab == 1 ? "No stock history" : "No active batches");
                } else {
                    tvDetailBatchesEmpty.setVisibility(View.GONE);
                }

                // Query server for latest batch/stock details
                productRepository.getProductById(product.productId, new OnlineProductRepository.ProductActionCallback() {
                    @Override
                    public void onSuccess(Product cloudProd) {
                        if (cloudProd != null) {
                            List<ProductBatch> cloudBatches = cloudProd.getBatches();
                            if (cloudBatches != null && !cloudBatches.isEmpty()) {
                                allBatchesList.clear();
                                activeBatchesList.clear();
                                allBatchesList.addAll(cloudBatches);
                                for (ProductBatch b : cloudBatches) {
                                    if (b != null && b.quantity > 0) {
                                        activeBatchesList.add(b);
                                    }
                                }
                            } else if (cloudProd.getBatch() != null) {
                                allBatchesList.clear();
                                activeBatchesList.clear();
                                allBatchesList.add(cloudProd.getBatch());
                                if (cloudProd.getBatch().quantity > 0) {
                                    activeBatchesList.add(cloudProd.getBatch());
                                }
                            }
                            int curTab = tabLayoutBatches.getSelectedTabPosition();
                            List<ProductBatch> disp = (curTab == 1) ? allBatchesList : activeBatchesList;
                            detailBatchAdapter.setBatches(disp);
                            if (disp.isEmpty()) {
                                tvDetailBatchesEmpty.setVisibility(View.VISIBLE);
                                tvDetailBatchesEmpty.setText(curTab == 1 ? "No stock history" : "No active batches");
                            } else {
                                tvDetailBatchesEmpty.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {}
                });
            } else {
                productBatchRepository.getAllBatchesForProduct(product.productId).observe(this, batches -> {
                    allBatchesList.clear();
                    activeBatchesList.clear();
                    if (batches != null) {
                        allBatchesList.addAll(batches);
                        for (ProductBatch b : batches) {
                            if (b.quantity > 0) {
                                activeBatchesList.add(b);
                            }
                        }
                    }
                    int selectedTab = tabLayoutBatches.getSelectedTabPosition();
                    List<ProductBatch> currentDisplay = (selectedTab == 1) ? allBatchesList : activeBatchesList;
                    detailBatchAdapter.setBatches(currentDisplay);
                    if (currentDisplay.isEmpty()) {
                        tvDetailBatchesEmpty.setVisibility(View.VISIBLE);
                        tvDetailBatchesEmpty.setText(selectedTab == 1 ? "No stock history" : "No active batches");
                    } else {
                        tvDetailBatchesEmpty.setVisibility(View.GONE);
                    }
                });
            }

            tabLayoutBatches.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                    int pos = tab.getPosition();
                    List<ProductBatch> currentDisplay = (pos == 1) ? allBatchesList : activeBatchesList;
                    detailBatchAdapter.setBatches(currentDisplay);
                    if (currentDisplay.isEmpty()) {
                        tvDetailBatchesEmpty.setVisibility(View.VISIBLE);
                        tvDetailBatchesEmpty.setText(pos == 1 ? "No stock history" : "No active batches");
                    } else {
                        tvDetailBatchesEmpty.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {
                }

                @Override
                public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {
                }
            });
        } else if (containerDetailBatches != null) {
            containerDetailBatches.setVisibility(View.GONE);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            openEditProduct(product);
        });

        View btnGenQr = view.findViewById(R.id.btn_gen_qr);
        if (btnGenQr != null) {
            btnGenQr.setOnClickListener(v -> {
                dialog.dismiss();
                showGenerateQrDialog(product);
            });
        }

        dialog.show();
    }

    private void showGenerateQrDialog(@NonNull Product product) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_generate_qr, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvProdName = dialogView.findViewById(R.id.tv_gen_qr_product_name);
        TextView tvProdSku = dialogView.findViewById(R.id.tv_gen_qr_product_sku);
        TextView tvProdPrice = dialogView.findViewById(R.id.tv_gen_qr_product_price);

        if (tvProdName != null) {
            tvProdName.setText(product.productName != null ? product.productName : "Product");
        }
        if (tvProdSku != null) {
            tvProdSku.setText(!TextUtils.isEmpty(product.sku) ? product.sku : "No SKU");
        }
        if (tvProdPrice != null) {
            tvProdPrice.setText(String.format(Locale.getDefault(), "₹ %,.2f", product.sellingPrice));
        }

        TextInputEditText etCopies = dialogView.findViewById(R.id.et_copies);
        View btnMinus = dialogView.findViewById(R.id.btn_copies_minus);
        View btnPlus = dialogView.findViewById(R.id.btn_copies_plus);

        if (btnMinus != null && etCopies != null) {
            btnMinus.setOnClickListener(v -> {
                int cur = parseCopies(etCopies.getText() != null ? etCopies.getText().toString() : "1");
                if (cur > 1) {
                    etCopies.setText(String.valueOf(cur - 1));
                }
            });
        }

        if (btnPlus != null && etCopies != null) {
            btnPlus.setOnClickListener(v -> {
                int cur = parseCopies(etCopies.getText() != null ? etCopies.getText().toString() : "1");
                if (cur < 999) {
                    etCopies.setText(String.valueOf(cur + 1));
                }
            });
        }

        int[] chipIds = {R.id.chip_copy_1, R.id.chip_copy_5, R.id.chip_copy_10, R.id.chip_copy_20, R.id.chip_copy_50};
        int[] chipVals = {1, 5, 10, 20, 50};
        for (int i = 0; i < chipIds.length; i++) {
            int val = chipVals[i];
            View chip = dialogView.findViewById(chipIds[i]);
            if (chip != null && etCopies != null) {
                chip.setOnClickListener(v -> etCopies.setText(String.valueOf(val)));
            }
        }

        View containerBatch = dialogView.findViewById(R.id.container_select_batch);
        AutoCompleteTextView actvBatch = dialogView.findViewById(R.id.actv_select_batch);
        TextView tvNoBatches = dialogView.findViewById(R.id.tv_no_batches_warning);

        boolean isBatchMode = product.batchEnabled;
        final ProductBatch[] selectedBatch = new ProductBatch[]{null};
        final List<ProductBatch> batchList = new ArrayList<>();

        if (isBatchMode && containerBatch != null) {
            containerBatch.setVisibility(View.VISIBLE);
            productBatchRepository.getAllBatchesForProduct(product.productId).observe(this, batches -> {
                batchList.clear();
                if (batches != null) {
                    batchList.addAll(batches);
                }

                if (batchList.isEmpty()) {
                    if (tvNoBatches != null) tvNoBatches.setVisibility(View.VISIBLE);
                    if (actvBatch != null) {
                        actvBatch.setText("No batches found (Default used)", false);
                        actvBatch.setEnabled(false);
                    }
                    selectedBatch[0] = null;
                } else {
                    if (tvNoBatches != null) tvNoBatches.setVisibility(View.GONE);
                    if (actvBatch != null) {
                        actvBatch.setEnabled(true);

                        List<String> displayStrings = new ArrayList<>();
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        int defaultIndex = 0;

                        for (int i = 0; i < batchList.size(); i++) {
                            ProductBatch b = batchList.get(i);
                            StringBuilder sb = new StringBuilder();
                            sb.append(b.batchNo != null ? b.batchNo : "Batch #" + b.batchId);
                            sb.append(" • Stock: ").append(b.quantity);
                            if (b.expiryDate > 0) {
                                sb.append(" • Exp: ").append(sdf.format(new Date(b.expiryDate)));
                            }
                            double price = (b.sellingPrice > 0) ? b.sellingPrice : product.sellingPrice;
                            sb.append(String.format(Locale.getDefault(), " • ₹ %,.2f", price));
                            displayStrings.add(sb.toString());

                            if (b.quantity > 0 && defaultIndex == 0) {
                                defaultIndex = i;
                            }
                        }

                        ArrayAdapter<String> batchAdapter = new ArrayAdapter<>(this,
                                android.R.layout.simple_dropdown_item_1line, displayStrings);
                        actvBatch.setAdapter(batchAdapter);

                        selectedBatch[0] = batchList.get(defaultIndex);
                        actvBatch.setText(displayStrings.get(defaultIndex), false);

                        actvBatch.setOnItemClickListener((parent, v, position, id) -> {
                            if (position >= 0 && position < batchList.size()) {
                                selectedBatch[0] = batchList.get(position);
                            }
                        });
                    }
                }
            });
        } else if (containerBatch != null) {
            containerBatch.setVisibility(View.GONE);
            selectedBatch[0] = null;
        }

        View btnCancel = dialogView.findViewById(R.id.btn_cancel_generate_qr);
        View btnConfirm = dialogView.findViewById(R.id.btn_confirm_generate_qr);

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(v -> {
                int copies = parseCopies(etCopies != null && etCopies.getText() != null ? etCopies.getText().toString() : "1");
                if (copies <= 0) {
                    if (etCopies != null) etCopies.setError("Enter at least 1 copy");
                    return;
                }

                ProductBatch batchToUse = isBatchMode ? selectedBatch[0] : null;

                Product prodForQr = new Product();
                prodForQr.productId = product.productId;
                prodForQr.productName = product.productName;
                prodForQr.sku = product.sku;
                prodForQr.unitOfMeasure = product.unitOfMeasure;
                prodForQr.brand = product.brand;
                prodForQr.categoryId = product.categoryId;

                if (batchToUse != null) {
                    prodForQr.sellingPrice = (batchToUse.sellingPrice > 0) ? batchToUse.sellingPrice : product.sellingPrice;
                    prodForQr.setBatch(batchToUse);
                } else {
                    prodForQr.sellingPrice = product.sellingPrice;
                    // If batch is disabled or none: batchId will be 0 and expiry date will be 00000
                    prodForQr.setBatch(null);
                }

                Bitmap qrBitmap;
                try {
                    qrBitmap = ProductQRHelper.generateProductQR(prodForQr, 512);
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to generate QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

                // Inflate custom XML layout to generate the full printable QR code card/label
                View labelView = getLayoutInflater().inflate(R.layout.layout_product_qr_label, null);

                TextView tvLabelName = labelView.findViewById(R.id.tv_label_product_name);
                TextView tvLabelSku = labelView.findViewById(R.id.tv_label_product_sku);
                TextView tvLabelPrice = labelView.findViewById(R.id.tv_label_product_price);
                TextView tvLabelDate = labelView.findViewById(R.id.tv_label_date);
                ImageView ivLabelQr = labelView.findViewById(R.id.iv_label_qr_code);

                View containerLabelBatch = labelView.findViewById(R.id.container_label_batch);
                TextView tvLabelBatchNo = labelView.findViewById(R.id.tv_label_batch_no);
                TextView tvLabelExpiry = labelView.findViewById(R.id.tv_label_expiry_date);

                SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                if (tvLabelDate != null) {
                    tvLabelDate.setText(sdfDate.format(new Date()));
                }
                if (tvLabelName != null) {
                    tvLabelName.setText(product.productName != null ? product.productName : "Product");
                }
                if (tvLabelSku != null) {
                    tvLabelSku.setText(!TextUtils.isEmpty(product.sku) ? product.sku : "No SKU");
                }
                if (tvLabelPrice != null) {
                    tvLabelPrice.setText(String.format(Locale.getDefault(), "₹ %,.2f", prodForQr.sellingPrice));
                }
                if (ivLabelQr != null) {
                    ivLabelQr.setImageBitmap(qrBitmap);
                }

                // Show batch row ONLY if batch is present
                if (batchToUse != null && !TextUtils.isEmpty(batchToUse.batchNo)) {
                    if (containerLabelBatch != null)
                        containerLabelBatch.setVisibility(View.VISIBLE);
                    if (tvLabelBatchNo != null) {
                        tvLabelBatchNo.setText("Batch: " + batchToUse.batchNo);
                    }
                    if (tvLabelExpiry != null) {
                        if (batchToUse.expiryDate > 0) {
                            tvLabelExpiry.setVisibility(View.VISIBLE);
                            tvLabelExpiry.setText("Exp: " + sdfDate.format(new Date(batchToUse.expiryDate)));
                        } else {
                            tvLabelExpiry.setVisibility(View.GONE);
                        }
                    }
                } else {
                    if (containerLabelBatch != null) containerLabelBatch.setVisibility(View.GONE);
                }

                // Render custom layout to bitmap using BitmapHelper
                int labelWidthPx = BitmapHelper.convertDpToPx(this, 380f);
                Bitmap customLayoutBitmap = BitmapHelper.createBitmapFromView(labelView, labelWidthPx, 0);

                // Save copies of custom layout to Downloads folder using BitmapHelper
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                String skuClean = !TextUtils.isEmpty(product.sku)
                        ? product.sku.replaceAll("[^a-zA-Z0-9_-]", "") : ("PROD" + product.productId);
                String batchClean = (batchToUse != null && !TextUtils.isEmpty(batchToUse.batchNo))
                        ? batchToUse.batchNo.replaceAll("[^a-zA-Z0-9_-]", "") : "NOBATCH";

                int savedCount = 0;
                for (int i = 1; i <= copies; i++) {
                    String fileName;
                    if (copies == 1) {
                        fileName = String.format(Locale.US, "QR_%s_%s_%s.png", skuClean, batchClean, timeStamp);
                    } else {
                        fileName = String.format(Locale.US, "QR_%s_%s_%s_copy%d.png", skuClean, batchClean, timeStamp, i);
                    }
                    Uri uri = BitmapHelper.saveBitmapToDownloads(customLayoutBitmap, this, fileName);
                    if (uri != null) {
                        savedCount++;
                    }
                }

                if (savedCount > 0) {
                    Toast.makeText(this,
                            String.format(Locale.US, "Saved %d QR label%s to Downloads folder", savedCount, savedCount > 1 ? "s" : ""),
                            Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to save QR label(s) to Downloads", Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
                showQrPreviewDialog(prodForQr, batchToUse, customLayoutBitmap, copies);
            });
        }

        dialog.show();
    }

    private void showQrPreviewDialog(Product product, ProductBatch batch, Bitmap labelBitmap, int copies) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_qr_preview, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvSub = dialogView.findViewById(R.id.tv_preview_subtitle);
        TextView tvName = dialogView.findViewById(R.id.tv_preview_product_name);
        TextView tvSku = dialogView.findViewById(R.id.tv_preview_product_sku);
        TextView tvPrice = dialogView.findViewById(R.id.tv_preview_product_price);
        View containerBatch = dialogView.findViewById(R.id.container_preview_batch_info);
        TextView tvBatch = dialogView.findViewById(R.id.tv_preview_batch_details);
        TextView tvCopies = dialogView.findViewById(R.id.tv_preview_copies_count);

        if (tvSub != null) {
            tvSub.setText(String.format(Locale.getDefault(), "Saved %d copy%s to Downloads folder", copies, copies > 1 ? "ies" : ""));
        }
        if (tvName != null) {
            tvName.setText(product.productName != null ? product.productName : "Product");
        }
        if (tvSku != null) {
            tvSku.setText(!TextUtils.isEmpty(product.sku) ? product.sku : "No SKU");
        }
        if (tvPrice != null) {
            tvPrice.setText(String.format(Locale.getDefault(), "₹ %,.2f", product.sellingPrice));
        }

        // Only show batch row if batch is present
        if (batch != null && !TextUtils.isEmpty(batch.batchNo)) {
            if (containerBatch != null) containerBatch.setVisibility(View.VISIBLE);
            if (tvBatch != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("Batch: ").append(batch.batchNo);
                if (batch.expiryDate > 0) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                    sb.append(" • Exp: ").append(sdf.format(new Date(batch.expiryDate)));
                }
                tvBatch.setText(sb.toString());
            }
        } else {
            if (containerBatch != null) containerBatch.setVisibility(View.GONE);
        }

        if (tvCopies != null) {
            tvCopies.setText(String.format(Locale.getDefault(), "Copies: %d", copies));
        }

        View btnShare = dialogView.findViewById(R.id.btn_share_qr);
        View btnDone = dialogView.findViewById(R.id.btn_done_qr);

        if (btnShare != null) {
            btnShare.setOnClickListener(v -> BitmapHelper.shareBitmap(this, labelBitmap, "Share QR Code", "QR Code for " + product.productName));
        }
        if (btnDone != null) {
            btnDone.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private int parseCopies(String text) {
        if (TextUtils.isEmpty(text)) return 1;
        try {
            int val = Integer.parseInt(text.trim());
            return val > 0 ? val : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void tintCircle(View view, int color) {
        android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
        circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        circle.setColor(color);
        view.setBackground(circle);
    }

    private void focusSearchInput() {
        if (editSearchInventory != null) {
            editSearchInventory.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editSearchInventory, InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }

    private void hideKeyboard(View view) {
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_inventory);
        }
        if (appModeManager != null && appModeManager.isOnlineMode()) {
            loadOnlineProducts(1, true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_inventory, menu);

        MenuItem importItem = menu.findItem(R.id.action_import_csv);
        if (importItem != null && importItem.getIcon() != null) {
            importItem.getIcon().setTint(ContextCompat.getColor(this, R.color.white));
        }

        MenuItem themeItem = menu.findItem(R.id.action_theme_toggle);
        if (themeItem != null) {
            boolean isDark = ThemeManager.isDarkMode(this);
            themeItem.setIcon(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);
            if (themeItem.getIcon() != null) {
                themeItem.getIcon().setTint(ContextCompat.getColor(this, R.color.white));
            }
            themeItem.setTitle(isDark ? "Switch to Light Mode" : "Switch to Dark Mode");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_import_csv) {
            showImportCsvBottomSheet();
            return true;
        } else if (item.getItemId() == R.id.action_theme_toggle) {
            ThemeManager.toggleTheme(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showImportCsvBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_import_csv, null);
        dialog.setContentView(view);
        currentImportDialog = dialog;

        layoutSelectFile = view.findViewById(R.id.layout_select_file);
        layoutFileSelected = view.findViewById(R.id.layout_file_selected);
        textSelectedFileName = view.findViewById(R.id.text_selected_file_name);
        textSelectedFileSize = view.findViewById(R.id.text_selected_file_size);
        View btnChangeFile = view.findViewById(R.id.btn_change_file);

        layoutValidationSuccess = view.findViewById(R.id.layout_validation_success);
        textValidationSuccessDetails = view.findViewById(R.id.text_validation_success_details);
        layoutValidationError = view.findViewById(R.id.layout_validation_error);
        textValidationErrorDetails = view.findViewById(R.id.text_validation_error_details);

        layoutImportProgress = view.findViewById(R.id.layout_import_progress);
        textImportProgressStatus = view.findViewById(R.id.text_import_progress_status);

        layoutImportReport = view.findViewById(R.id.layout_import_report);
        layoutReportBanner = view.findViewById(R.id.layout_report_banner);
        imageReportStatus = view.findViewById(R.id.image_report_status);
        textReportTitle = view.findViewById(R.id.text_report_title);
        textReportSubtitle = view.findViewById(R.id.text_report_subtitle);

        textStatProductsCount = view.findViewById(R.id.text_stat_products_count);
        textStatCategoriesCount = view.findViewById(R.id.text_stat_categories_count);
        textStatBatchesCount = view.findViewById(R.id.text_stat_batches_count);
        textStatErrorsCount = view.findViewById(R.id.text_stat_errors_count);

        containerErrorsList = view.findViewById(R.id.container_errors_list);
        textErrorsHeader = view.findViewById(R.id.text_errors_header);
        recyclerImportErrors = view.findViewById(R.id.recycler_import_errors);
        recyclerImportErrors.setLayoutManager(new LinearLayoutManager(this));
        importErrorAdapter = new ImportErrorAdapter();
        recyclerImportErrors.setAdapter(importErrorAdapter);

        btnStartImport = view.findViewById(R.id.btn_start_import);
        layoutReportActions = view.findViewById(R.id.layout_report_actions);
        btnImportAnother = view.findViewById(R.id.btn_import_another);
        btnDoneImport = view.findViewById(R.id.btn_done_import);

        View btnClose = view.findViewById(R.id.btn_close_import);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (layoutSelectFile != null) {
            layoutSelectFile.setOnClickListener(v -> launchCsvFilePicker());
        }
        if (btnChangeFile != null) {
            btnChangeFile.setOnClickListener(v -> launchCsvFilePicker());
        }

        if (btnStartImport != null) {
            btnStartImport.setOnClickListener(v -> executeCsvImport());
        }

        if (btnDoneImport != null) {
            btnDoneImport.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnImportAnother != null) {
            btnImportAnother.setOnClickListener(v -> resetImportSheetForNewFile());
        }

        dialog.setOnDismissListener(d -> {
            if (currentImportDialog == dialog) {
                currentImportDialog = null;
            }
            selectedCsvUri = null;
        });

        dialog.show();

        if (selectedCsvUri != null) {
            processSelectedCsv(selectedCsvUri);
        } else {
            resetImportSheetForNewFile();
        }
    }

    private void launchCsvFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimeTypes = new String[]{
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "text/plain",
                "application/vnd.ms-excel"
        };
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        csvPickerLauncher.launch(Intent.createChooser(intent, "Select CSV File"));
    }

    private void resetImportSheetForNewFile() {
        selectedCsvUri = null;
        if (layoutSelectFile != null) layoutSelectFile.setVisibility(View.VISIBLE);
        if (layoutFileSelected != null) layoutFileSelected.setVisibility(View.GONE);
        if (layoutValidationSuccess != null) layoutValidationSuccess.setVisibility(View.GONE);
        if (layoutValidationError != null) layoutValidationError.setVisibility(View.GONE);
        if (layoutImportProgress != null) layoutImportProgress.setVisibility(View.GONE);
        if (layoutImportReport != null) layoutImportReport.setVisibility(View.GONE);
        if (layoutReportActions != null) layoutReportActions.setVisibility(View.GONE);
        if (btnStartImport != null) {
            btnStartImport.setVisibility(View.VISIBLE);
            btnStartImport.setEnabled(false);
            btnStartImport.setText("Start Import");
        }
        if (importErrorAdapter != null) {
            importErrorAdapter.setItems(null);
        }
    }

    private void handleCsvFileSelected(@NonNull Uri uri) {
        selectedCsvUri = uri;
        if (currentImportDialog == null || !currentImportDialog.isShowing()) {
            showImportCsvBottomSheet();
        } else {
            processSelectedCsv(uri);
        }
    }

    private void processSelectedCsv(@NonNull Uri uri) {
        selectedCsvUri = uri;

        String displayName = getFileNameFromUri(uri);
        long fileSize = getFileSizeFromUri(uri);

        if (layoutSelectFile != null) layoutSelectFile.setVisibility(View.GONE);
        if (layoutFileSelected != null) layoutFileSelected.setVisibility(View.VISIBLE);
        if (textSelectedFileName != null) textSelectedFileName.setText(displayName);
        if (textSelectedFileSize != null) textSelectedFileSize.setText(formatFileSize(fileSize));
        if (layoutImportProgress != null) layoutImportProgress.setVisibility(View.GONE);
        if (layoutImportReport != null) layoutImportReport.setVisibility(View.GONE);
        if (layoutReportActions != null) layoutReportActions.setVisibility(View.GONE);

        // Check if ends with .csv
        if (displayName != null && !displayName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
            if (layoutValidationSuccess != null) layoutValidationSuccess.setVisibility(View.GONE);
            if (layoutValidationError != null) {
                layoutValidationError.setVisibility(View.VISIBLE);
                if (textValidationErrorDetails != null) {
                    textValidationErrorDetails.setText("The selected file does not have a .csv extension. Please select a valid CSV file.");
                }
            }
            if (btnStartImport != null) {
                btnStartImport.setVisibility(View.VISIBLE);
                btnStartImport.setEnabled(false);
            }
            return;
        }

        // Validate headers using ProductCsvParser
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) {
                showValidationError("Unable to open selected file for reading.");
                return;
            }

            ProductCsvParser.HeaderValidationResult validation = ProductCsvParser.validateHeaders(is);
            if (validation.isValid) {
                if (layoutValidationError != null) layoutValidationError.setVisibility(View.GONE);
                if (layoutValidationSuccess != null) {
                    layoutValidationSuccess.setVisibility(View.VISIBLE);
                    if (textValidationSuccessDetails != null) {
                        textValidationSuccessDetails.setText("All 20 headers validated successfully (" + validation.detectedHeaders.size() + " columns detected).");
                    }
                }
                if (btnStartImport != null) {
                    btnStartImport.setVisibility(View.VISIBLE);
                    btnStartImport.setEnabled(true);
                    btnStartImport.setText("Start Import");
                }
            } else {
                showValidationError(validation.errorMessage != null ? validation.errorMessage : "CSV header validation failed.");
            }
        } catch (Exception e) {
            showValidationError("Error validating CSV headers: " + e.getMessage());
        }
    }

    private void showValidationError(String message) {
        if (layoutValidationSuccess != null) layoutValidationSuccess.setVisibility(View.GONE);
        if (layoutValidationError != null) {
            layoutValidationError.setVisibility(View.VISIBLE);
            if (textValidationErrorDetails != null) {
                textValidationErrorDetails.setText(message);
            }
        }
        if (btnStartImport != null) {
            btnStartImport.setVisibility(View.VISIBLE);
            btnStartImport.setEnabled(false);
        }
    }

    private void executeCsvImport() {
        if (selectedCsvUri == null) {
            Toast.makeText(this, "Please select a CSV file first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (btnStartImport != null) btnStartImport.setVisibility(View.GONE);
        if (layoutImportProgress != null) layoutImportProgress.setVisibility(View.VISIBLE);
        if (textImportProgressStatus != null) {
            textImportProgressStatus.setText("Parsing CSV and updating database...");
        }

        Executors.newSingleThreadExecutor().execute(() -> {
            Database db = Database.getInstance(getApplication());
            CategoryDao categoryDao = db.categoryDao();
            ProductDao productDao = db.productDao();
            ProductBatchDao batchDao = db.productBatchDao();

            List<ImportErrorAdapter.ImportErrorItem> errorList = new ArrayList<>();
            final int[] categoriesCreated = {0};
            final int[] successfulProducts = {0};
            final int[] batchesInserted = {0};

            // Preload existing categories into a cache map
            Map<String, Integer> categoryCache = new HashMap<>();
            try {
                List<Category> allCats = categoryDao.getAllCategoriesSync();
                if (allCats != null) {
                    for (Category c : allCats) {
                        if (c.categoryName != null) {
                            categoryCache.put(c.categoryName.trim().toLowerCase(Locale.ROOT), c.categoryId);
                        }
                    }
                }
            } catch (Exception ignored) {
            }

            // Category resolver that automatically inserts missing categories
            ProductCsvParser.CategoryResolver resolver = new ProductCsvParser.CategoryResolver() {
                @Override
                public synchronized int getOrCreateCategoryId(String categoryName) throws Exception {
                    if (categoryName == null || categoryName.trim().isEmpty()) {
                        return -1;
                    }
                    String key = categoryName.trim().toLowerCase(Locale.ROOT);
                    Integer cached = categoryCache.get(key);
                    if (cached != null) return cached;

                    Category existing = categoryDao.getCategoryByNameSync(categoryName.trim());
                    if (existing != null) {
                        categoryCache.put(key, existing.categoryId);
                        return existing.categoryId;
                    }

                    // Auto-insert new category
                    Category newCat = new Category(
                            categoryName.trim(),
                            "Auto-created from CSV import",
                            CategoryIconHelper.DEFAULT_ICON_KEY
                    );
                    long newId = categoryDao.insert(newCat);
                    int catId = (int) newId;
                    categoryCache.put(key, catId);
                    categoriesCreated[0]++;
                    return catId;
                }
            };

            ProductCsvParser parser = new ProductCsvParser(resolver);
            String mandatorySetting = GlobalStore.getInstance().getCsvMandatoryField();
            parser.setMandatoryField(mandatorySetting);

            ProductCsvParser.ParseResult parseResult = null;
            try (InputStream is = getContentResolver().openInputStream(selectedCsvUri)) {
                if (is == null) {
                    throw new IOException("Cannot open stream for selected CSV file.");
                }
                parseResult = parser.parse(is);
            } catch (Exception e) {
                errorList.add(new ImportErrorAdapter.ImportErrorItem(
                        "-",
                        null,
                        null,
                        "CSV parsing error: " + e.getMessage()
                ));
            }

            if (parseResult != null) {
                // Collect row parse errors
                for (ProductCsvParser.RowError err : parseResult.errors) {
                    errorList.add(new ImportErrorAdapter.ImportErrorItem(
                            String.valueOf(err.lineNumber),
                            err.sku,
                            err.hsnCode,
                            err.reason
                    ));
                }

                Set<String> sessionSkus = new HashSet<>();
                Set<String> sessionHsns = new HashSet<>();

                // Insert valid products and batches
                for (ProductCsvParser.ProductGroup group : parseResult.groups) {
                    Product product = group.product;
                    if (product == null) continue;

                    String lines = TextUtils.join(", ", group.sourceLines);

                    // Check duplicate SKU in DB or current import session
                    if (product.sku != null && !product.sku.trim().isEmpty()) {
                        String skuClean = product.sku.trim();
                        String skuKey = skuClean.toUpperCase(Locale.ROOT);
                        Product existing = productDao.getProductBySku(skuClean);
                        if (existing != null || sessionSkus.contains(skuKey)) {
                            String conflictMsg = existing != null
                                    ? "Product with SKU '" + skuClean + "' already exists in database (conflicts with '" + existing.productName + "')."
                                    : "Duplicate SKU '" + skuClean + "' already imported in this session.";
                            errorList.add(new ImportErrorAdapter.ImportErrorItem(
                                    lines,
                                    product.sku,
                                    product.hsnCode,
                                    conflictMsg
                            ));
                            continue;
                        }
                    }

                    // Check duplicate HSN in DB or current import session
                    if (product.hsnCode != null && !product.hsnCode.trim().isEmpty()) {
                        String hsnClean = product.hsnCode.trim();
                        String hsnKey = hsnClean.toUpperCase(Locale.ROOT);
                        Product existingHsn = productDao.getProductByHsn(hsnClean);
                        if (existingHsn != null || sessionHsns.contains(hsnKey)) {
                            String conflictMsg = existingHsn != null
                                    ? "Product with HSN '" + hsnClean + "' already exists in database (conflicts with '" + existingHsn.productName + "')."
                                    : "Duplicate HSN '" + hsnClean + "' already imported in this session.";
                            errorList.add(new ImportErrorAdapter.ImportErrorItem(
                                    lines,
                                    product.sku,
                                    product.hsnCode,
                                    conflictMsg
                            ));
                            continue;
                        }
                    }

                    try {
                        long productId = productDao.insert(product);
                        product.productId = (int) productId;

                        if (product.sku != null && !product.sku.trim().isEmpty()) {
                            sessionSkus.add(product.sku.trim().toUpperCase(Locale.ROOT));
                        }
                        if (product.hsnCode != null && !product.hsnCode.trim().isEmpty()) {
                            sessionHsns.add(product.hsnCode.trim().toUpperCase(Locale.ROOT));
                        }

                        for (ProductBatch batch : group.batches) {
                            batch.productId = (int) productId;
                            batchDao.insert(batch);
                            batchesInserted[0]++;
                        }
                        successfulProducts[0]++;
                    } catch (Exception e) {
                        errorList.add(new ImportErrorAdapter.ImportErrorItem(
                                lines,
                                product.sku,
                                product.hsnCode,
                                "Database insert error: " + e.getMessage()
                        ));
                    }
                }
            }

            // Post results to UI thread
            runOnUiThread(() -> {
                if (layoutImportProgress != null) layoutImportProgress.setVisibility(View.GONE);
                if (layoutImportReport != null) layoutImportReport.setVisibility(View.VISIBLE);
                if (layoutReportActions != null) layoutReportActions.setVisibility(View.VISIBLE);

                if (textStatProductsCount != null) {
                    textStatProductsCount.setText(String.valueOf(successfulProducts[0]));
                }
                if (textStatCategoriesCount != null) {
                    textStatCategoriesCount.setText(String.valueOf(categoriesCreated[0]));
                }
                if (textStatBatchesCount != null) {
                    textStatBatchesCount.setText(String.valueOf(batchesInserted[0]));
                }
                if (textStatErrorsCount != null) {
                    textStatErrorsCount.setText(String.valueOf(errorList.size()));
                }

                // Setup banner presentation
                if (errorList.isEmpty()) {
                    if (layoutReportBanner != null) {
                        layoutReportBanner.setBackgroundResource(R.drawable.bg_success_banner);
                    }
                    if (imageReportStatus != null) {
                        imageReportStatus.setImageResource(R.drawable.ic_check);
                        imageReportStatus.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_green)));
                    }
                    if (textReportTitle != null) {
                        textReportTitle.setTextColor(ContextCompat.getColor(this, R.color.status_green));
                        textReportTitle.setText(successfulProducts[0] + " Products Imported Successfully");
                    }
                    if (textReportSubtitle != null) {
                        textReportSubtitle.setText("All rows processed and saved to database with zero errors.");
                    }
                } else if (successfulProducts[0] > 0) {
                    if (layoutReportBanner != null) {
                        layoutReportBanner.setBackgroundResource(R.drawable.bg_warning_banner);
                    }
                    if (imageReportStatus != null) {
                        imageReportStatus.setImageResource(R.drawable.ic_error);
                        imageReportStatus.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.accent_amber)));
                    }
                    if (textReportTitle != null) {
                        textReportTitle.setTextColor(ContextCompat.getColor(this, R.color.accent_amber));
                        textReportTitle.setText(successfulProducts[0] + " Successful, " + errorList.size() + " Error(s)");
                    }
                    if (textReportSubtitle != null) {
                        textReportSubtitle.setText("Some products were saved, but some rows were skipped due to errors.");
                    }
                } else {
                    if (layoutReportBanner != null) {
                        layoutReportBanner.setBackgroundResource(R.drawable.bg_error_banner);
                    }
                    if (imageReportStatus != null) {
                        imageReportStatus.setImageResource(R.drawable.ic_error);
                        imageReportStatus.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error_red)));
                    }
                    if (textReportTitle != null) {
                        textReportTitle.setTextColor(ContextCompat.getColor(this, R.color.error_red));
                        textReportTitle.setText("Import Failed (0 Successful, " + errorList.size() + " Errors)");
                    }
                    if (textReportSubtitle != null) {
                        textReportSubtitle.setText("No products could be added. Review the error details below.");
                    }
                }

                // Error list
                if (errorList.isEmpty()) {
                    if (containerErrorsList != null) containerErrorsList.setVisibility(View.GONE);
                } else {
                    if (containerErrorsList != null)
                        containerErrorsList.setVisibility(View.VISIBLE);
                    if (textErrorsHeader != null) {
                        textErrorsHeader.setText("Error Details / Skipped Rows (" + errorList.size() + ")");
                    }
                    if (importErrorAdapter != null) {
                        importErrorAdapter.setItems(errorList);
                    }
                }

                if (successfulProducts[0] > 0) {
                    Toast.makeText(this, "Imported " + successfulProducts[0] + " product(s)", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIdx >= 0) {
                        result = cursor.getString(nameIdx);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (result == null && uri.getPath() != null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result != null ? result : "products.csv";
    }

    private long getFileSizeFromUri(Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIdx >= 0) {
                        return cursor.getLong(sizeIdx);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return -1;
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "Unknown size";
        if (bytes < 1024) return bytes + " B";
        int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
        return String.format(Locale.getDefault(), "%.1f %sB", (double) bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
    }

    @Override
    protected void onDestroy() {
        if (modeChangeListener != null && appModeManager != null) {
            appModeManager.removeOnModeChangeListener(modeChangeListener);
        }
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        if (currentImportDialog != null && currentImportDialog.isShowing()) {
            currentImportDialog.dismiss();
            currentImportDialog = null;
        }
        super.onDestroy();
    }

    public enum FilterType {
        ALL,
        IN_STOCK,
        LOW_STOCK,
        OUT_OF_STOCK
    }
}