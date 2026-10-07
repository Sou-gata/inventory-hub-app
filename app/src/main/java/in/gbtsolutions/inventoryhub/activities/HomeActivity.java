package in.gbtsolutions.inventoryhub.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.BatchListAdapter;
import in.gbtsolutions.inventoryhub.helpers.BitmapHelper;
import in.gbtsolutions.inventoryhub.helpers.CommonFunctions;
import in.gbtsolutions.inventoryhub.helpers.ProductQRHelper;
import in.gbtsolutions.inventoryhub.helpers.ThemeManager;
import in.gbtsolutions.inventoryhub.models.Buyer;
import in.gbtsolutions.inventoryhub.models.Category;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;
import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseWithSupplier;
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleWithBuyer;
import in.gbtsolutions.inventoryhub.models.Suppliers;
import in.gbtsolutions.inventoryhub.models.User;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;

public class HomeActivity extends BaseActivity {

    private NavigationView navView;

    private final ActivityResultLauncher<Intent> qrCodeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    String scanResult = result.getData().getStringExtra(ScannerActivity.SCAN_RESULT);
                    if (scanResult != null && !scanResult.trim().isEmpty()) {
                        handleScannedProduct(scanResult.trim());
                    }
                }
            }
    );

    // Hero Header Views
    private TextView tvDashboardDate;
    private TextView tvInventoryHealthStatus;
    private FrameLayout btnHeroScanQr;

    // Quick Action Buttons
    private View btnActionNewSale;
    private View btnActionNewPurchase;
    private View btnActionAddProduct;
    private View btnActionReceive;

    // Executive KPI Cards & Texts
    private View cardKpiValuation;
    private TextView tvKpiValuation;
    private TextView tvKpiUnits;

    private View cardKpiSales;
    private TextView tvKpiRevenue;
    private TextView tvKpiSalesCount;

    private View cardKpiLowStock;
    private TextView tvKpiLowStock;
    private TextView tvKpiLowStockMeta;

    private View cardKpiPendingReceives;
    private TextView tvKpiPendingReceive;

    // Critical Stock Alerts
    private TextView badgeLowStockCount;
    private View btnViewAllLowStock;
    private LinearLayout containerLowStockItems;
    private View layoutStockOptimal;

    // Recent Activity Hub
    private View btnViewAllActivity;
    private FrameLayout tabSales;
    private TextView tvTabSales;
    private FrameLayout tabPurchases;
    private TextView tvTabPurchases;
    private LinearLayout containerSalesTab;
    private LinearLayout layoutSalesList;
    private TextView tvSalesEmpty;
    private LinearLayout containerPurchasesTab;
    private LinearLayout layoutPurchasesList;
    private TextView tvPurchasesEmpty;
    private boolean isSalesTabActive = true;

    // Quick Directories
    private View btnHubInventory;
    private TextView tvHubProductsCount;
    private View btnHubCategory;
    private TextView tvHubCategoriesCount;
    private View btnHubBuyer;
    private TextView tvHubBuyersCount;
    private View btnHubSupplier;
    private TextView tvHubSuppliersCount;
    private View btnHubReports;
    private View btnHubSettings;

    // Cached Categories
    private final Map<Integer, String> categoryNames = new HashMap<>();

    // Date Formatters
    private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        GlobalStore.getInstance().loadSettings(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        View headerContainer = findViewById(R.id.header_container);
        View dashboardScroll = findViewById(R.id.dashboard_scroll);

        // Apply edge-to-edge system insets (status bar & nav bar) via BaseActivity helper
        applyDrawerInsets(drawerLayout, headerContainer, dashboardScroll, navView);

        // Standardized sidebar and header navigation
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_home);

        initViews();
        setupClickListeners();
        updateDashboardDate();
        observeDatabaseData();
    }

    private void initViews() {
        // Hero
        tvDashboardDate = findViewById(R.id.tv_dashboard_date);
        tvInventoryHealthStatus = findViewById(R.id.tv_inventory_health_status);
        btnHeroScanQr = findViewById(R.id.btn_hero_scan_qr);

        // Quick Actions
        btnActionNewSale = findViewById(R.id.btn_action_new_sale);
        btnActionNewPurchase = findViewById(R.id.btn_action_new_purchase);
        btnActionAddProduct = findViewById(R.id.btn_action_add_product);
        btnActionReceive = findViewById(R.id.btn_action_receive);

        // KPIs
        cardKpiValuation = findViewById(R.id.card_kpi_valuation);
        tvKpiValuation = findViewById(R.id.tv_kpi_valuation);
        tvKpiUnits = findViewById(R.id.tv_kpi_units);

        cardKpiSales = findViewById(R.id.card_kpi_sales);
        tvKpiRevenue = findViewById(R.id.tv_kpi_revenue);
        tvKpiSalesCount = findViewById(R.id.tv_kpi_sales_count);

        cardKpiLowStock = findViewById(R.id.card_kpi_low_stock);
        tvKpiLowStock = findViewById(R.id.tv_kpi_low_stock);
        tvKpiLowStockMeta = findViewById(R.id.tv_kpi_low_stock_meta);

        cardKpiPendingReceives = findViewById(R.id.card_kpi_pending_receives);
        tvKpiPendingReceive = findViewById(R.id.tv_kpi_pending_receive);

        // Critical Stock Alerts
        badgeLowStockCount = findViewById(R.id.badge_low_stock_count);
        btnViewAllLowStock = findViewById(R.id.btn_view_all_low_stock);
        containerLowStockItems = findViewById(R.id.container_low_stock_items);
        layoutStockOptimal = findViewById(R.id.layout_stock_optimal);

        // Recent Activity
        btnViewAllActivity = findViewById(R.id.btn_view_all_activity);
        tabSales = findViewById(R.id.tab_sales);
        tvTabSales = findViewById(R.id.tv_tab_sales);
        tabPurchases = findViewById(R.id.tab_purchases);
        tvTabPurchases = findViewById(R.id.tv_tab_purchases);
        containerSalesTab = findViewById(R.id.container_sales_tab);
        layoutSalesList = findViewById(R.id.layout_sales_list);
        tvSalesEmpty = findViewById(R.id.tv_sales_empty);
        containerPurchasesTab = findViewById(R.id.container_purchases_tab);
        layoutPurchasesList = findViewById(R.id.layout_purchases_list);
        tvPurchasesEmpty = findViewById(R.id.tv_purchases_empty);

        // Quick Directories
        btnHubInventory = findViewById(R.id.btn_hub_inventory);
        tvHubProductsCount = findViewById(R.id.tv_hub_products_count);
        btnHubCategory = findViewById(R.id.btn_hub_category);
        tvHubCategoriesCount = findViewById(R.id.tv_hub_categories_count);
        btnHubBuyer = findViewById(R.id.btn_hub_buyer);
        tvHubBuyersCount = findViewById(R.id.tv_hub_buyers_count);
        btnHubSupplier = findViewById(R.id.btn_hub_supplier);
        tvHubSuppliersCount = findViewById(R.id.tv_hub_suppliers_count);
        btnHubReports = findViewById(R.id.btn_hub_reports);
        btnHubSettings = findViewById(R.id.btn_hub_settings);
    }

    private void setupClickListeners() {
        // QR Scanner
        if (btnHeroScanQr != null) {
            btnHeroScanQr.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, ScannerActivity.class);
                qrCodeLauncher.launch(intent);
                applySlideTransition(HomeActivity.this);
            });
        }

        // Quick Actions
        if (btnActionNewSale != null) {
            btnActionNewSale.setOnClickListener(v -> navigateTo(SellActivity.class, false));
        }
        if (btnActionNewPurchase != null) {
            btnActionNewPurchase.setOnClickListener(v -> navigateTo(PurchaseActivity.class, false));
        }
        if (btnActionAddProduct != null) {
            btnActionAddProduct.setOnClickListener(v -> navigateTo(AddProductActivity.class, false));
        }
        if (btnActionReceive != null) {
            btnActionReceive.setOnClickListener(v -> navigateTo(PendingReceiveActivity.class, false));
        }

        // KPI Cards
        if (cardKpiValuation != null) {
            cardKpiValuation.setOnClickListener(v -> navigateTo(InventoryActivity.class, false));
        }
        if (cardKpiSales != null) {
            cardKpiSales.setOnClickListener(v -> navigateTo(SellHistoryActivity.class, false));
        }
        if (cardKpiLowStock != null) {
            cardKpiLowStock.setOnClickListener(v -> openInventoryWithFilter("low_stock"));
        }
        if (btnViewAllLowStock != null) {
            btnViewAllLowStock.setOnClickListener(v -> openInventoryWithFilter("low_stock"));
        }
        if (cardKpiPendingReceives != null) {
            cardKpiPendingReceives.setOnClickListener(v -> navigateTo(PendingReceiveActivity.class, false));
        }

        // Activity Tab Switcher
        if (tabSales != null) {
            tabSales.setOnClickListener(v -> selectActivityTab(true));
        }
        if (tabPurchases != null) {
            tabPurchases.setOnClickListener(v -> selectActivityTab(false));
        }
        if (btnViewAllActivity != null) {
            btnViewAllActivity.setOnClickListener(v -> {
                if (isSalesTabActive) {
                    navigateTo(SellHistoryActivity.class, false);
                } else {
                    navigateTo(PurchaseHistoryActivity.class, false);
                }
            });
        }

        // Quick Directories
        if (btnHubInventory != null) {
            btnHubInventory.setOnClickListener(v -> navigateTo(InventoryActivity.class, false));
        }
        if (btnHubCategory != null) {
            btnHubCategory.setOnClickListener(v -> navigateTo(CategoryActivity.class, false));
        }
        if (btnHubBuyer != null) {
            btnHubBuyer.setOnClickListener(v -> navigateTo(BuyerActivity.class, false));
        }
        if (btnHubSupplier != null) {
            btnHubSupplier.setOnClickListener(v -> navigateTo(SupplierActivity.class, false));
        }
        if (btnHubReports != null) {
            btnHubReports.setOnClickListener(v -> showReportsSelectionDialog());
        }
        if (btnHubSettings != null) {
            btnHubSettings.setOnClickListener(v -> navigateTo(SettingsActivity.class, false));
        }
    }

    private void selectActivityTab(boolean showSales) {
        isSalesTabActive = showSales;
        if (showSales) {
            tabSales.setBackgroundResource(R.drawable.bg_tab_active);
            tvTabSales.setTextColor(ContextCompat.getColor(this, R.color.white));
            tabPurchases.setBackgroundResource(R.drawable.bg_tab_inactive);
            tvTabPurchases.setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
            containerSalesTab.setVisibility(View.VISIBLE);
            containerPurchasesTab.setVisibility(View.GONE);
        } else {
            tabPurchases.setBackgroundResource(R.drawable.bg_tab_active);
            tvTabPurchases.setTextColor(ContextCompat.getColor(this, R.color.white));
            tabSales.setBackgroundResource(R.drawable.bg_tab_inactive);
            tvTabSales.setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
            containerSalesTab.setVisibility(View.GONE);
            containerPurchasesTab.setVisibility(View.VISIBLE);
        }
    }

    private void openInventoryWithFilter(@NonNull String filter) {
        Intent intent = new Intent(this, InventoryActivity.class);
        intent.putExtra("filter", filter);
        startActivity(intent);
        applyTransition(this);
    }

    private void showReportsSelectionDialog() {
        String[] options = {"GSTR-1 Summary Report", "Sales Register", "Purchase Register", "User Wise Report", "Audit Trail"};
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Select Report")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        navigateTo(ReportsActivity.class, false);
                    } else if (which == 1) {
                        navigateTo(SalesRegisterActivity.class, false);
                    } else if (which == 2) {
                        navigateTo(PurchaseRegisterActivity.class, false);
                    } else if (which == 3) {
                        navigateTo(UserWiseReportActivity.class, false);
                    } else if (which == 4) {
                        navigateTo(AuditTrailActivity.class, false);
                    }
                })
                .show();
    }

    private void updateDashboardDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault());
        if (tvDashboardDate != null) {
            tvDashboardDate.setText(dateFormat.format(calendar.getTime()));
        }
    }

    private void observeDatabaseData() {
        Database db = Database.getInstance(this);

        // 1. Categories
        db.categoryDao().getAllCategories().observe(this, categories -> {
            categoryNames.clear();
            int count = categories != null ? categories.size() : 0;
            if (categories != null) {
                for (Category category : categories) {
                    if (category.categoryName != null) {
                        categoryNames.put(category.categoryId, category.categoryName);
                    }
                }
            }
            if (tvHubCategoriesCount != null) {
                tvHubCategoriesCount.setText(count + (count == 1 ? " Category" : " Categories"));
            }
        });

        // 2. Products (Valuation & Total Units)
        db.productDao().getAllProducts().observe(this, products -> {
            int productCount = products != null ? products.size() : 0;
            double totalUnits = 0.0;
            double totalValuation = 0.0;

            if (products != null) {
                for (Product p : products) {
                    totalUnits += p.quantity;
                    totalValuation += (p.quantity * p.unitPrice);
                }
            }

            if (tvKpiValuation != null) {
                tvKpiValuation.setText(formatCurrency(totalValuation));
            }
            if (tvKpiUnits != null) {
                tvKpiUnits.setText(String.format(Locale.getDefault(), "%s units in stock", CommonFunctions.formatQuantity(totalUnits)));
            }
            if (tvHubProductsCount != null) {
                tvHubProductsCount.setText(productCount + (productCount == 1 ? " Product" : " Products"));
            }
        });

        // 3. Products Needing Reorder (Low Stock Watchlist)
        db.productDao().getProductsNeedingReorder().observe(this, lowStockProducts -> {
            int count = lowStockProducts != null ? lowStockProducts.size() : 0;
            if (badgeLowStockCount != null) {
                badgeLowStockCount.setText(String.valueOf(count));
            }
            if (tvKpiLowStock != null) {
                tvKpiLowStock.setText(count + (count == 1 ? " Item" : " Items"));
            }

            if (count > 0) {
                if (tvInventoryHealthStatus != null) {
                    tvInventoryHealthStatus.setText("⚠️ " + count + " Low Stock");
                }
                if (tvKpiLowStockMeta != null) {
                    tvKpiLowStockMeta.setText("Reorder required");
                }
                if (layoutStockOptimal != null) {
                    layoutStockOptimal.setVisibility(View.GONE);
                }
                if (containerLowStockItems != null) {
                    containerLowStockItems.setVisibility(View.VISIBLE);
                    populateLowStockAlerts(lowStockProducts);
                }
            } else {
                if (tvInventoryHealthStatus != null) {
                    tvInventoryHealthStatus.setText("● Stock Optimal");
                }
                if (tvKpiLowStockMeta != null) {
                    tvKpiLowStockMeta.setText("All stocks optimal");
                }
                if (containerLowStockItems != null) {
                    containerLowStockItems.removeAllViews();
                    containerLowStockItems.setVisibility(View.GONE);
                }
                if (layoutStockOptimal != null) {
                    layoutStockOptimal.setVisibility(View.VISIBLE);
                }
            }
        });

        // 4. Sales Orders & Revenue
        db.saleDao().getAllSalesWithBuyer().observe(this, sales -> {
            double totalRevenue = 0.0;
            int activeSalesCount = 0;

            if (sales != null) {
                for (SaleWithBuyer swb : sales) {
                    Sale sale = swb.sale;
                    if (sale != null && !"Cancelled".equalsIgnoreCase(sale.status)) {
                        totalRevenue += sale.totalAmount;
                        activeSalesCount++;
                    }
                }
            }

            if (tvKpiRevenue != null) {
                tvKpiRevenue.setText(formatCurrency(totalRevenue));
            }
            if (tvKpiSalesCount != null) {
                tvKpiSalesCount.setText(activeSalesCount + (activeSalesCount == 1 ? " sale order" : " sales orders"));
            }

            populateRecentSales(sales);
        });

        // 5. Purchases (History)
        db.purchaseDao().getAllPurchasesWithSupplier().observe(this, purchases -> {
            populateRecentPurchases(purchases);
        });

        // 6. Pending Purchase Orders (Awaiting Receipt)
        db.purchaseDao().getPendingPurchasesWithSupplier().observe(this, pendingPurchases -> {
            int pendingCount = pendingPurchases != null ? pendingPurchases.size() : 0;
            if (tvKpiPendingReceive != null) {
                tvKpiPendingReceive.setText(pendingCount + (pendingCount == 1 ? " Order" : " Orders"));
            }
        });

        // 7. Buyers
        db.buyerDao().getAllBuyers().observe(this, buyers -> {
            int count = buyers != null ? buyers.size() : 0;
            if (tvHubBuyersCount != null) {
                tvHubBuyersCount.setText(count + (count == 1 ? " Buyer" : " Buyers"));
            }
        });

        // 8. Suppliers
        db.supplierDao().getAllSuppliers().observe(this, suppliers -> {
            int count = suppliers != null ? suppliers.size() : 0;
            if (tvHubSuppliersCount != null) {
                tvHubSuppliersCount.setText(count + (count == 1 ? " Supplier" : " Suppliers"));
            }
        });
    }

    private void populateLowStockAlerts(@NonNull List<Product> lowStockList) {
        if (containerLowStockItems == null) return;
        containerLowStockItems.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(this);
        int maxItems = Math.min(lowStockList.size(), 4);

        for (int i = 0; i < maxItems; i++) {
            Product product = lowStockList.get(i);
            View alertRow = inflater.inflate(R.layout.item_dashboard_alert, containerLowStockItems, false);

            TextView tvName = alertRow.findViewById(R.id.tv_alert_product_name);
            TextView tvMeta = alertRow.findViewById(R.id.tv_alert_meta);
            TextView tvBadge = alertRow.findViewById(R.id.tv_alert_stock_badge);
            TextView tvReorder = alertRow.findViewById(R.id.tv_alert_reorder_level);
            ImageView ivIcon = alertRow.findViewById(R.id.iv_alert_icon);
            FrameLayout iconContainer = alertRow.findViewById(R.id.container_alert_icon);

            tvName.setText(product.productName != null ? product.productName : "Unknown Product");

            String categoryName = categoryNames.get(product.categoryId);
            StringBuilder meta = new StringBuilder();
            if (!TextUtils.isEmpty(product.sku)) {
                meta.append("SKU: ").append(product.sku);
            }
            if (!TextUtils.isEmpty(categoryName)) {
                if (meta.length() > 0) meta.append(" • ");
                meta.append(categoryName);
            }
            tvMeta.setText(meta.toString());

            if (product.quantity <= 0) {
                tvBadge.setText("Out of stock");
                tvBadge.setBackgroundResource(R.drawable.bg_stock_out);
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.status_red));
                iconContainer.setBackgroundResource(R.drawable.bg_stock_out);
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_red));
            } else {
                tvBadge.setText(CommonFunctions.formatQuantity(product.quantity) + " left");
                tvBadge.setBackgroundResource(R.drawable.bg_stock_low);
                tvBadge.setTextColor(ContextCompat.getColor(this, R.color.status_orange));
                iconContainer.setBackgroundResource(R.drawable.bg_stock_low);
                ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_orange));
            }

            tvReorder.setText("Reorder level: " + product.reorderLevel);

            alertRow.setOnClickListener(v -> openInventoryWithFilter("low_stock"));
            containerLowStockItems.addView(alertRow);

            if (i < maxItems - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border));
                containerLowStockItems.addView(divider);
            }
        }
    }

    private void populateRecentSales(List<SaleWithBuyer> sales) {
        if (layoutSalesList == null || tvSalesEmpty == null) return;
        layoutSalesList.removeAllViews();

        if (sales == null || sales.isEmpty()) {
            tvSalesEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvSalesEmpty.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        int maxItems = Math.min(sales.size(), 4);

        for (int i = 0; i < maxItems; i++) {
            SaleWithBuyer swb = sales.get(i);
            Sale sale = swb.sale;
            if (sale == null) continue;

            View row = inflater.inflate(R.layout.item_dashboard_transaction, layoutSalesList, false);

            TextView tvPartyName = row.findViewById(R.id.tv_tx_party_name);
            TextView tvMeta = row.findViewById(R.id.tv_tx_meta);
            TextView tvAmount = row.findViewById(R.id.tv_tx_amount);
            TextView tvStatus = row.findViewById(R.id.tv_tx_status);
            ImageView ivIcon = row.findViewById(R.id.iv_tx_icon);
            FrameLayout containerIcon = row.findViewById(R.id.container_tx_icon);

            containerIcon.setBackgroundResource(R.drawable.bg_stock_in);
            ivIcon.setImageResource(R.drawable.ic_nav_sell);
            ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_green));

            String partyName;
            if (swb.buyer != null && !TextUtils.isEmpty(swb.buyer.buyerName)) {
                partyName = swb.buyer.buyerName;
            } else if (!TextUtils.isEmpty(sale.customerName)) {
                partyName = sale.customerName;
            } else {
                partyName = "Walk-in Customer";
            }
            tvPartyName.setText(partyName);

            String invoice = !TextUtils.isEmpty(sale.invoiceId)
                    ? sale.invoiceId
                    : String.format(Locale.getDefault(), "#SALE-%d", sale.saleId);
            String dateFormatted = formatDate(sale.billingDate);
            tvMeta.setText(invoice + " • " + dateFormatted);

            tvAmount.setText(formatCurrency(sale.totalAmount));

            String status = !TextUtils.isEmpty(sale.status) ? sale.status : "Completed";
            tvStatus.setText(status);
            applyStatusStyle(tvStatus, status);

            row.setOnClickListener(v -> navigateTo(SellHistoryActivity.class, false));
            layoutSalesList.addView(row);

            if (i < maxItems - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border));
                layoutSalesList.addView(divider);
            }
        }
    }

    private void populateRecentPurchases(List<PurchaseWithSupplier> purchases) {
        if (layoutPurchasesList == null || tvPurchasesEmpty == null) return;
        layoutPurchasesList.removeAllViews();

        if (purchases == null || purchases.isEmpty()) {
            tvPurchasesEmpty.setVisibility(View.VISIBLE);
            return;
        }

        tvPurchasesEmpty.setVisibility(View.GONE);
        LayoutInflater inflater = LayoutInflater.from(this);
        int maxItems = Math.min(purchases.size(), 4);

        for (int i = 0; i < maxItems; i++) {
            PurchaseWithSupplier pws = purchases.get(i);
            Purchase purchase = pws.purchase;
            if (purchase == null) continue;

            View row = inflater.inflate(R.layout.item_dashboard_transaction, layoutPurchasesList, false);

            TextView tvPartyName = row.findViewById(R.id.tv_tx_party_name);
            TextView tvMeta = row.findViewById(R.id.tv_tx_meta);
            TextView tvAmount = row.findViewById(R.id.tv_tx_amount);
            TextView tvStatus = row.findViewById(R.id.tv_tx_status);
            ImageView ivIcon = row.findViewById(R.id.iv_tx_icon);
            FrameLayout containerIcon = row.findViewById(R.id.container_tx_icon);

            containerIcon.setBackgroundResource(R.drawable.bg_icon_badge_blue);
            ivIcon.setImageResource(R.drawable.ic_nav_purchase);
            ivIcon.setColorFilter(ContextCompat.getColor(this, R.color.material_blue));

            String partyName = pws.supplier != null && !TextUtils.isEmpty(pws.supplier.supplierName)
                    ? pws.supplier.supplierName
                    : "Supplier";
            tvPartyName.setText(partyName);

            String invoice = !TextUtils.isEmpty(purchase.invoiceId)
                    ? purchase.invoiceId
                    : String.format(Locale.getDefault(), "#PO-%d", purchase.purchaseId);
            String dateFormatted = formatDate(purchase.billingDate);
            tvMeta.setText(invoice + " • " + dateFormatted);

            tvAmount.setText(formatCurrency(purchase.totalAmount));

            String status = !TextUtils.isEmpty(purchase.status) ? purchase.status : "Completed";
            tvStatus.setText(status);
            applyStatusStyle(tvStatus, status);

            row.setOnClickListener(v -> navigateTo(PurchaseHistoryActivity.class, false));
            layoutPurchasesList.addView(row);

            if (i < maxItems - 1) {
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border));
                layoutPurchasesList.addView(divider);
            }
        }
    }

    private void applyStatusStyle(@NonNull TextView tvStatus, @NonNull String status) {
        if ("Completed".equalsIgnoreCase(status) || "Received".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundResource(R.drawable.bg_stock_in);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_green));
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            tvStatus.setBackgroundResource(R.drawable.bg_stock_out);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_red));
        } else {
            tvStatus.setBackgroundResource(R.drawable.bg_stock_low);
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.status_orange));
        }
    }

    private String formatDate(String rawDate) {
        if (TextUtils.isEmpty(rawDate)) return "";
        try {
            Date date = isoDateFormat.parse(rawDate);
            if (date != null) {
                return displayDateFormat.format(date);
            }
        } catch (ParseException ignored) {
        }
        return rawDate;
    }

    public static String formatCurrency(double amount) {
        try {
            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
            return nf.format(amount);
        } catch (Exception e) {
            return String.format(Locale.getDefault(), "₹%,.2f", amount);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        GlobalStore.getInstance().loadSettings(this);
        updateDashboardDate();
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_home);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
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
        if (item.getItemId() == R.id.action_theme_toggle) {
            ThemeManager.toggleTheme(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleScannedProduct(@NonNull String scanResult) {
        Executors.newSingleThreadExecutor().execute(() -> {
            Product product = null;

            // 1. Try parsing encrypted QR payload generated by the app
            try {
                product = ProductQRHelper.parseScannedQR(HomeActivity.this, scanResult);
            } catch (Exception ignored) {}

            Database db = Database.getInstance(HomeActivity.this);

            // 2. Try JSON payload if scanned code is JSON
            if (product == null && scanResult.startsWith("{") && scanResult.endsWith("}")) {
                try {
                    org.json.JSONObject obj = new org.json.JSONObject(scanResult);
                    if (obj.has("productId")) {
                        product = db.productDao().getProductById(obj.getInt("productId"));
                    } else if (obj.has("product_id")) {
                        product = db.productDao().getProductById(obj.getInt("product_id"));
                    } else if (obj.has("sku")) {
                        product = db.productDao().getProductBySku(obj.getString("sku"));
                    }
                } catch (Exception ignored) {}
            }

            // 3. Fallback: try numeric product ID
            if (product == null) {
                try {
                    int prodId = Integer.parseInt(scanResult);
                    product = db.productDao().getProductById(prodId);
                } catch (NumberFormatException ignored) {}
            }

            // 4. Fallback: try SKU matching
            if (product == null) {
                product = db.productDao().getProductBySku(scanResult);
            }

            // 5. Fallback: try HSN / Barcode matching
            if (product == null) {
                product = db.productDao().getProductByHsn(scanResult);
            }

            final Product finalProduct = product;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (finalProduct != null) {
                    showProductDetailsSheet(finalProduct);
                } else {
                    showToast("No product found matching code: " + scanResult);
                }
            });
        });
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

        if (textName != null) {
            textName.setText(product.productName != null ? product.productName : "Unnamed Product");
        }

        if (textBrand != null) {
            if (!TextUtils.isEmpty(product.brand)) {
                textBrand.setVisibility(View.VISIBLE);
                textBrand.setText(String.format("Brand: %s", product.brand));
            } else {
                textBrand.setVisibility(View.GONE);
            }
        }

        if (textSku != null) {
            textSku.setText(!TextUtils.isEmpty(product.sku) ? product.sku : "No SKU");
        }

        if (textCategory != null) {
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
        }

        String uom = !TextUtils.isEmpty(product.unitOfMeasure) ? product.unitOfMeasure : "pcs";
        double qty = product.quantity;
        int reorder = product.reorderLevel;

        if (stockBadge != null && textStockStatus != null && stockDot != null) {
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
        }

        if (textSellingPrice != null) {
            textSellingPrice.setText(String.format(Locale.getDefault(), "₹ %,.2f", product.sellingPrice));
        }
        if (textUnitPrice != null) {
            textUnitPrice.setText(String.format(Locale.getDefault(), "₹ %,.2f", product.unitPrice));
        }
        if (textQuantity != null) {
            textQuantity.setText(String.format(Locale.getDefault(), "%s %s", CommonFunctions.formatQuantity(qty), uom));
        }

        if (textReorder != null) {
            textReorder.setText(String.format(Locale.getDefault(), "Alert at %d %s • Reorder %d %s",
                    product.reorderLevel, uom, product.reorderQuantity, uom));
        }

        if (textTaxHsn != null) {
            String hsn = !TextUtils.isEmpty(product.hsnCode) ? product.hsnCode : "N/A";
            textTaxHsn.setText(String.format(Locale.getDefault(), "%.1f%% • HSN %s", product.gstPercent, hsn));
        }

        if (textStatus != null) {
            textStatus.setText(!TextUtils.isEmpty(product.status) ? product.status : "Active");
        }

        if (containerDescription != null && textDescription != null) {
            if (!TextUtils.isEmpty(product.description)) {
                containerDescription.setVisibility(View.VISIBLE);
                textDescription.setText(product.description);
            } else {
                containerDescription.setVisibility(View.GONE);
            }
        }

        // Batch Inventory Section in Bottom Sheet
        View containerDetailBatches = view.findViewById(R.id.container_detail_batches);
        TabLayout tabLayoutBatches = view.findViewById(R.id.tab_layout_batches);
        RecyclerView recyclerDetailBatches = view.findViewById(R.id.recycler_detail_batches);
        TextView tvDetailBatchesEmpty = view.findViewById(R.id.tv_detail_batches_empty);

        boolean isBatchMode = product.batchEnabled;
        if (isBatchMode && containerDetailBatches != null && tabLayoutBatches != null && recyclerDetailBatches != null) {
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

            if (AppModeManager.getInstance(this).isOnlineMode()) {
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
                if (tvDetailBatchesEmpty != null) {
                    if (currentDisplay.isEmpty()) {
                        tvDetailBatchesEmpty.setVisibility(View.VISIBLE);
                        tvDetailBatchesEmpty.setText(selectedTab == 1 ? "No stock history" : "No active batches");
                    } else {
                        tvDetailBatchesEmpty.setVisibility(View.GONE);
                    }
                }
            } else {
                Database.getInstance(this).productBatchDao().getAllBatchesForProduct(product.productId).observe(this, batches -> {
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
                    if (tvDetailBatchesEmpty != null) {
                        if (currentDisplay.isEmpty()) {
                            tvDetailBatchesEmpty.setVisibility(View.VISIBLE);
                            tvDetailBatchesEmpty.setText(selectedTab == 1 ? "No stock history" : "No active batches");
                        } else {
                            tvDetailBatchesEmpty.setVisibility(View.GONE);
                        }
                    }
                });
            }

            tabLayoutBatches.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    int pos = tab.getPosition();
                    List<ProductBatch> currentDisplay = (pos == 1) ? allBatchesList : activeBatchesList;
                    detailBatchAdapter.setBatches(currentDisplay);
                    if (tvDetailBatchesEmpty != null) {
                        if (currentDisplay.isEmpty()) {
                            tvDetailBatchesEmpty.setVisibility(View.VISIBLE);
                            tvDetailBatchesEmpty.setText(pos == 1 ? "No stock history" : "No active batches");
                        } else {
                            tvDetailBatchesEmpty.setVisibility(View.GONE);
                        }
                    }
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {}

                @Override
                public void onTabReselected(TabLayout.Tab tab) {}
            });
        } else if (containerDetailBatches != null) {
            containerDetailBatches.setVisibility(View.GONE);
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> {
                dialog.dismiss();
                openEditProduct(product);
            });
        }

        View btnGenQr = view.findViewById(R.id.btn_gen_qr);
        if (btnGenQr != null) {
            btnGenQr.setOnClickListener(v -> {
                dialog.dismiss();
                showGenerateQrDialog(product);
            });
        }

        dialog.show();
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
        applySlideTransition(this);
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
            java.util.function.Consumer<List<ProductBatch>> batchUiPopulator = batches -> {
                batchList.clear();
                if (batches != null) {
                    batchList.addAll(batches);
                }

                if (batchList.isEmpty()) {
                    if (tvNoBatches != null) tvNoBatches.setVisibility(View.VISIBLE);
                    if (actvBatch != null) {
                        actvBatch.setEnabled(false);
                        actvBatch.setText("No batches available", false);
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
            };

            if (AppModeManager.getInstance(this).isOnlineMode()) {
                List<ProductBatch> onlineBatches = product.getBatches();
                if ((onlineBatches == null || onlineBatches.isEmpty()) && product.getBatch() != null) {
                    onlineBatches = new ArrayList<>();
                    onlineBatches.add(product.getBatch());
                }
                batchUiPopulator.accept(onlineBatches);
            } else {
                Database.getInstance(this).productBatchDao().getAllBatchesForProduct(product.productId).observe(this, batches -> {
                    batchUiPopulator.accept(batches);
                });
            }
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
                    prodForQr.setBatch(null);
                }

                Bitmap qrBitmap;
                try {
                    qrBitmap = ProductQRHelper.generateProductQR(prodForQr, 512);
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to generate QR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    return;
                }

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

                if (batchToUse != null && !TextUtils.isEmpty(batchToUse.batchNo)) {
                    if (containerLabelBatch != null) containerLabelBatch.setVisibility(View.VISIBLE);
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

                int labelWidthPx = BitmapHelper.convertDpToPx(this, 380f);
                Bitmap customLayoutBitmap = BitmapHelper.createBitmapFromView(labelView, labelWidthPx, 0);

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
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(color);
        view.setBackground(circle);
    }
}