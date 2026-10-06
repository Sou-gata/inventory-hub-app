package in.gbtsolutions.inventoryhub.activities;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.PurchaseHistoryAdapter;
import in.gbtsolutions.inventoryhub.helpers.BitmapHelper;
import in.gbtsolutions.inventoryhub.helpers.CommonFunctions;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseItem;
import in.gbtsolutions.inventoryhub.models.PurchaseItemWithProduct;
import in.gbtsolutions.inventoryhub.models.PurchaseWithSupplier;
import in.gbtsolutions.inventoryhub.models.ReceiveItem;
import in.gbtsolutions.inventoryhub.models.ReceiveRecord;
import in.gbtsolutions.inventoryhub.models.ReceiveRecordWithItems;
import in.gbtsolutions.inventoryhub.models.Suppliers;
import in.gbtsolutions.inventoryhub.repository.ConfigRepository;
import in.gbtsolutions.inventoryhub.repository.PurchaseRepository;

public class PurchaseHistoryActivity extends BaseActivity {

    private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final Map<String, String> cachedCompanyConfigs = new HashMap<>();
    private final Observer<List<PurchaseWithSupplier>> purchasesObserver = this::updatePurchaseList;
    // Filter views
    private View boxStartDate;
    private TextView textStartDate;
    private ImageView btnClearStartDate;
    private View boxEndDate;
    private TextView textEndDate;
    private ImageView btnClearEndDate;
    private EditText editSearchBuyer;
    private ImageView btnClearSearch;
    private View btnSearchSales;
    private TextView btnResetFilters;
    // List & summary
    private TextView textSalesCount;
    private TextView textSalesTotalSum;
    private RecyclerView recyclerHistory;
    private View layoutEmptyState;
    private Button btnEmptyReset;
    // Filter state (ISO yyyy-MM-dd for SQLite)
    @Nullable
    private String selectedStartDate = null;
    @Nullable
    private String selectedEndDate = null;
    private long startDateMillis = System.currentTimeMillis();
    private long endDateMillis = System.currentTimeMillis();
    private PurchaseHistoryAdapter adapter;
    private PurchaseRepository purchaseRepository;
    private LiveData<List<PurchaseWithSupplier>> currentLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_history);

        purchaseRepository = new PurchaseRepository(getApplication());
        ConfigRepository configRepository = new ConfigRepository(getApplication());
        configRepository.getAllConfigs().observe(this, configs -> {
            if (configs != null) {
                cachedCompanyConfigs.clear();
                for (Config cfg : configs) {
                    if (cfg != null && cfg.configKey != null) {
                        cachedCompanyConfigs.put(cfg.configKey, cfg.configValue);
                    }
                }
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        View headerContainer = findViewById(R.id.header_container);
        View contentContainer = findViewById(R.id.purchase_history_content_container);

        applyDrawerInsets(drawerLayout, headerContainer, contentContainer, navView);
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_purchase_history);

        initViews();
        setupDatePickers();
        setupSearchAndFilters();
        setupRecyclerView();
        applyFilters();
    }

    private void initViews() {
        boxStartDate = findViewById(R.id.box_start_date);
        textStartDate = findViewById(R.id.text_start_date);
        btnClearStartDate = findViewById(R.id.btn_clear_start_date);

        boxEndDate = findViewById(R.id.box_end_date);
        textEndDate = findViewById(R.id.text_end_date);
        btnClearEndDate = findViewById(R.id.btn_clear_end_date);

        editSearchBuyer = findViewById(R.id.edit_search_buyer);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        btnSearchSales = findViewById(R.id.btn_search_sales);
        btnResetFilters = findViewById(R.id.btn_reset_filters);

        textSalesCount = findViewById(R.id.text_sales_count);
        textSalesTotalSum = findViewById(R.id.text_sales_total_sum);
        recyclerHistory = findViewById(R.id.recycler_sales_history);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        btnEmptyReset = findViewById(R.id.btn_empty_reset);
    }

    private void setupDatePickers() {
        boxStartDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select Start Date").setSelection(startDateMillis).build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    startDateMillis = selection;
                    Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    utcCalendar.setTimeInMillis(selection);

                    SimpleDateFormat isoUtc = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    isoUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
                    selectedStartDate = isoUtc.format(utcCalendar.getTime());

                    SimpleDateFormat displayUtc = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    displayUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
                    textStartDate.setText(displayUtc.format(utcCalendar.getTime()));
                    textStartDate.setTextColor(ContextCompat.getColor(this, R.color.fg));
                    btnClearStartDate.setVisibility(View.VISIBLE);
                    updateResetButtonVisibility();
                }
            });

            datePicker.show(getSupportFragmentManager(), "PURCHASE_START_DATE");
        });

        btnClearStartDate.setOnClickListener(v -> {
            selectedStartDate = null;
            textStartDate.setText("Start Date");
            textStartDate.setTextColor(ContextCompat.getColor(this, R.color.fg_dim));
            btnClearStartDate.setVisibility(View.GONE);
            updateResetButtonVisibility();
        });

        boxEndDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select End Date").setSelection(endDateMillis).build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    endDateMillis = selection;
                    Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    utcCalendar.setTimeInMillis(selection);

                    SimpleDateFormat isoUtc = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    isoUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
                    selectedEndDate = isoUtc.format(utcCalendar.getTime());

                    SimpleDateFormat displayUtc = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    displayUtc.setTimeZone(TimeZone.getTimeZone("UTC"));
                    textEndDate.setText(displayUtc.format(utcCalendar.getTime()));
                    textEndDate.setTextColor(ContextCompat.getColor(this, R.color.fg));
                    btnClearEndDate.setVisibility(View.VISIBLE);
                    updateResetButtonVisibility();
                }
            });

            datePicker.show(getSupportFragmentManager(), "PURCHASE_END_DATE");
        });

        btnClearEndDate.setOnClickListener(v -> {
            selectedEndDate = null;
            textEndDate.setText("End Date");
            textEndDate.setTextColor(ContextCompat.getColor(this, R.color.fg_dim));
            btnClearEndDate.setVisibility(View.GONE);
            updateResetButtonVisibility();
        });
    }

    private void setupSearchAndFilters() {
        editSearchBuyer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                updateResetButtonVisibility();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnClearSearch.setOnClickListener(v -> {
            editSearchBuyer.setText("");
            updateResetButtonVisibility();
            applyFilters();
        });

        editSearchBuyer.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                applyFilters();
                return true;
            }
            return false;
        });

        btnSearchSales.setOnClickListener(v -> {
            hideKeyboard();
            applyFilters();
        });

        btnResetFilters.setOnClickListener(v -> resetAllFilters());
        btnEmptyReset.setOnClickListener(v -> resetAllFilters());
    }

    private void setupRecyclerView() {
        adapter = new PurchaseHistoryAdapter();
        recyclerHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerHistory.setAdapter(adapter);
        adapter.setOnPurchaseClickListener(this::showPurchaseDetailsSheet);
    }

    private void applyFilters() {
        String query = editSearchBuyer.getText() != null ? editSearchBuyer.getText().toString().trim() : "";

        if (currentLiveData != null) {
            currentLiveData.removeObserver(purchasesObserver);
        }

        currentLiveData = purchaseRepository.getFilteredPurchases(selectedStartDate, selectedEndDate, query);
        currentLiveData.observe(this, purchasesObserver);
        updateResetButtonVisibility();
    }

    private void resetAllFilters() {
        selectedStartDate = null;
        textStartDate.setText("Start Date");
        textStartDate.setTextColor(ContextCompat.getColor(this, R.color.fg_dim));
        btnClearStartDate.setVisibility(View.GONE);

        selectedEndDate = null;
        textEndDate.setText("End Date");
        textEndDate.setTextColor(ContextCompat.getColor(this, R.color.fg_dim));
        btnClearEndDate.setVisibility(View.GONE);

        editSearchBuyer.setText("");
        btnClearSearch.setVisibility(View.GONE);

        updateResetButtonVisibility();
        applyFilters();
    }

    private void updateResetButtonVisibility() {
        boolean hasStartDate = !TextUtils.isEmpty(selectedStartDate);
        boolean hasEndDate = !TextUtils.isEmpty(selectedEndDate);
        boolean hasQuery = editSearchBuyer.getText() != null && editSearchBuyer.getText().length() > 0;
        btnResetFilters.setVisibility((hasStartDate || hasEndDate || hasQuery) ? View.VISIBLE : View.GONE);
    }

    private void updatePurchaseList(@Nullable List<PurchaseWithSupplier> purchases) {
        if (purchases == null || purchases.isEmpty()) {
            adapter.setPurchases(new ArrayList<>());
            recyclerHistory.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            textSalesCount.setText("No purchases found");
            textSalesTotalSum.setText("Total: ₹ 0.00");
        } else {
            adapter.setPurchases(purchases);
            recyclerHistory.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);

            textSalesCount.setText(String.format(Locale.getDefault(), "Showing %d purchases", purchases.size()));

            double totalSum = 0.0;
            for (PurchaseWithSupplier item : purchases) {
                if (item.purchase != null) {
                    totalSum += item.purchase.totalAmount;
                }
            }
            textSalesTotalSum.setText(String.format(Locale.getDefault(), "Total: ₹ %,.2f", totalSum));
        }
    }

    private void showPurchaseDetailsSheet(@NonNull PurchaseWithSupplier item) {
        Purchase purchase = item.purchase;
        if (purchase == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_purchase_details, null);
        dialog.setContentView(view);

        // Header views
        TextView textInvoiceId = view.findViewById(R.id.text_detail_invoice_id);
        TextView textDateTime = view.findViewById(R.id.text_detail_date_time);
        View btnClose = view.findViewById(R.id.btn_close_sell_detail);
        LinearLayout layoutStatusBadge = view.findViewById(R.id.layout_status_badge);
        View viewStatusDot = view.findViewById(R.id.view_status_dot);
        TextView textStatus = view.findViewById(R.id.text_detail_status);
        TextView textSaleId = view.findViewById(R.id.text_detail_sale_id);

        // Cancellation Banner
        LinearLayout layoutCancellationBanner = view.findViewById(R.id.layout_cancellation_banner);
        TextView textCancellationTitle = view.findViewById(R.id.text_cancellation_title);
        TextView textCancellationDetails = view.findViewById(R.id.text_cancellation_details);

        // Supplier views
        TextView textBuyerName = view.findViewById(R.id.text_detail_buyer_name);
        TextView textBuyerContact = view.findViewById(R.id.text_detail_buyer_contact);
        TextView textBuyerPhone = view.findViewById(R.id.text_detail_buyer_phone);
        TextView textBuyerGst = view.findViewById(R.id.text_detail_buyer_gst);
        TextView textBuyerAddress = view.findViewById(R.id.text_detail_buyer_address);
        View btnCallBuyer = view.findViewById(R.id.btn_action_call_buyer);

        // Items
        TextView textItemsCount = view.findViewById(R.id.text_detail_items_count);
        LinearLayout containerItems = view.findViewById(R.id.container_detail_items);
        ProgressBar progressItems = view.findViewById(R.id.progress_detail_items);

        // Receiving Stages
        LinearLayout sectionStages = view.findViewById(R.id.section_receive_stages);
        TextView textStagesCount = view.findViewById(R.id.text_stages_count);
        LinearLayout containerStages = view.findViewById(R.id.container_receive_stages);
        ProgressBar progressStages = view.findViewById(R.id.progress_detail_stages);

        // Summary views
        TextView textSubtotal = view.findViewById(R.id.text_summary_subtotal);
        View rowDiscount = view.findViewById(R.id.row_summary_discount);
        TextView textDiscount = view.findViewById(R.id.text_summary_discount);
        View rowCgst = view.findViewById(R.id.row_summary_cgst);
        TextView textCgst = view.findViewById(R.id.text_summary_cgst);
        View rowSgst = view.findViewById(R.id.row_summary_sgst);
        TextView textSgst = view.findViewById(R.id.text_summary_sgst);
        View rowIgst = view.findViewById(R.id.row_summary_igst);
        TextView textIgst = view.findViewById(R.id.text_summary_igst);
        TextView textTotalGst = view.findViewById(R.id.text_summary_total_gst);
        View rowOtherCharges = view.findViewById(R.id.row_summary_other_charges);
        TextView textOtherCharges = view.findViewById(R.id.text_summary_other_charges);
        TextView textGrandTotal = view.findViewById(R.id.text_summary_grand_total);
        Button btnDone = view.findViewById(R.id.btn_detail_done);

        // Populate header
        textInvoiceId.setText(!TextUtils.isEmpty(purchase.invoiceId) ? purchase.invoiceId : String.format(Locale.getDefault(), "#PO-%d", purchase.purchaseId));
        textSaleId.setText(String.format(Locale.getDefault(), "#PO-%d", purchase.purchaseId));

        // Status badge color coding
        String status = !TextUtils.isEmpty(purchase.status) ? purchase.status : "Completed";
        textStatus.setText(status);

        int colorText;
        int colorBg;
        if ("Completed".equalsIgnoreCase(status)) {
            colorText = ContextCompat.getColor(this, R.color.status_green);
            colorBg = ContextCompat.getColor(this, R.color.status_green_bg);
        } else if ("Partially Received".equalsIgnoreCase(status) || "Partial Completed".equalsIgnoreCase(status)) {
            colorText = ContextCompat.getColor(this, R.color.status_blue);
            colorBg = ContextCompat.getColor(this, R.color.status_blue_bg);
        } else if ("Pending".equalsIgnoreCase(status)) {
            colorText = ContextCompat.getColor(this, R.color.status_orange);
            colorBg = ContextCompat.getColor(this, R.color.status_orange_bg);
        } else {
            // "Cancelled", "Partially Cancelled", etc.
            colorText = ContextCompat.getColor(this, R.color.status_red);
            colorBg = ContextCompat.getColor(this, R.color.status_red_bg);
        }

        textStatus.setTextColor(colorText);
        if (viewStatusDot != null) {
            viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(colorText));
        }
        if (layoutStatusBadge != null) {
            layoutStatusBadge.setBackgroundTintList(ColorStateList.valueOf(colorBg));
        }

        // Cancellation banner setup
        if ("Partially Cancelled".equalsIgnoreCase(status)) {
            layoutCancellationBanner.setVisibility(View.VISIBLE);
            textCancellationTitle.setText("ORDER PARTIALLY CANCELLED");
            String cancelDate = !TextUtils.isEmpty(purchase.updatedAt) ? purchase.updatedAt : purchase.billingDate;
            textCancellationDetails.setText(String.format("Cancelled on %s. Remaining unreceived units were cancelled.", cancelDate != null ? cancelDate : ""));
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            layoutCancellationBanner.setVisibility(View.VISIBLE);
            textCancellationTitle.setText("ORDER CANCELLED");
            String cancelDate = !TextUtils.isEmpty(purchase.updatedAt) ? purchase.updatedAt : purchase.billingDate;
            textCancellationDetails.setText(String.format("This purchase order was cancelled on %s. 0 units received.", cancelDate != null ? cancelDate : ""));
        } else {
            layoutCancellationBanner.setVisibility(View.GONE);
        }

        String formattedDate = purchase.billingDate;
        if (!TextUtils.isEmpty(purchase.createdAt)) {
            formattedDate = purchase.createdAt;
        } else if (!TextUtils.isEmpty(purchase.billingDate)) {
            try {
                Date date = isoDateFormat.parse(purchase.billingDate);
                if (date != null) formattedDate = displayDateFormat.format(date);
            } catch (ParseException ignored) {
            }
        }
        textDateTime.setText(String.format("Purchased on %s", formattedDate != null ? formattedDate : ""));

        // Populate supplier info
        Suppliers supplier = item.supplier;
        if (supplier != null) {
            textBuyerName.setText(!TextUtils.isEmpty(supplier.supplierName) ? supplier.supplierName : "Unknown Supplier");

            if (!TextUtils.isEmpty(supplier.contactPerson)) {
                textBuyerContact.setVisibility(View.VISIBLE);
                textBuyerContact.setText(String.format("Contact: %s", supplier.contactPerson));
            } else {
                textBuyerContact.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(supplier.phone)) {
                textBuyerPhone.setVisibility(View.VISIBLE);
                textBuyerPhone.setText(String.format("Phone: %s", supplier.phone));
                btnCallBuyer.setVisibility(View.VISIBLE);
                btnCallBuyer.setOnClickListener(v -> {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + supplier.phone));
                    startActivity(dialIntent);
                });
            } else {
                textBuyerPhone.setVisibility(View.GONE);
                btnCallBuyer.setVisibility(View.GONE);
            }

            if (!TextUtils.isEmpty(supplier.gst)) {
                textBuyerGst.setVisibility(View.VISIBLE);
                textBuyerGst.setText(String.format("GSTIN: %s", supplier.gst));
            } else {
                textBuyerGst.setVisibility(View.GONE);
            }

            StringBuilder addressBuilder = new StringBuilder();
            if (!TextUtils.isEmpty(supplier.address)) addressBuilder.append(supplier.address);
            if (!TextUtils.isEmpty(supplier.city)) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(supplier.city);
            }
            if (!TextUtils.isEmpty(supplier.stateCode)) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(supplier.stateCode);
            }
            if (!TextUtils.isEmpty(supplier.postalCode)) {
                if (addressBuilder.length() > 0) addressBuilder.append(" - ");
                addressBuilder.append(supplier.postalCode);
            }
            if (addressBuilder.length() > 0) {
                textBuyerAddress.setVisibility(View.VISIBLE);
                textBuyerAddress.setText(String.format("Address: %s", addressBuilder));
            } else {
                textBuyerAddress.setVisibility(View.GONE);
            }
        } else {
            textBuyerName.setText("Unknown Supplier");
            textBuyerContact.setVisibility(View.GONE);
            textBuyerPhone.setVisibility(View.GONE);
            textBuyerGst.setVisibility(View.GONE);
            textBuyerAddress.setVisibility(View.GONE);
            btnCallBuyer.setVisibility(View.GONE);
        }

        // Financial summary
        textSubtotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.subtotalAmount));

        if (purchase.discountAmount > 0) {
            rowDiscount.setVisibility(View.VISIBLE);
            textDiscount.setText(String.format(Locale.getDefault(), "- ₹ %,.2f", purchase.discountAmount));
        } else {
            rowDiscount.setVisibility(View.GONE);
        }

        if (purchase.cgstAmount > 0) {
            rowCgst.setVisibility(View.VISIBLE);
            textCgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.cgstAmount));
        } else {
            rowCgst.setVisibility(View.GONE);
        }

        if (purchase.sgstAmount > 0) {
            rowSgst.setVisibility(View.VISIBLE);
            textSgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.sgstAmount));
        } else {
            rowSgst.setVisibility(View.GONE);
        }

        if (purchase.igstAmount > 0) {
            rowIgst.setVisibility(View.VISIBLE);
            textIgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.igstAmount));
        } else {
            rowIgst.setVisibility(View.GONE);
        }

        textTotalGst.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.totalGst));

        if (purchase.otherCharges > 0) {
            rowOtherCharges.setVisibility(View.VISIBLE);
            textOtherCharges.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.otherCharges));
        } else {
            rowOtherCharges.setVisibility(View.GONE);
        }

        textGrandTotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.totalAmount));

        // Load items purchased
        final List<PurchaseItemWithProduct> loadedPurchaseItems = new ArrayList<>();
        progressItems.setVisibility(View.VISIBLE);
        containerItems.removeAllViews();

        purchaseRepository.getPurchaseItemsWithProduct(purchase.purchaseId).observe(this, new Observer<List<PurchaseItemWithProduct>>() {
            @Override
            public void onChanged(List<PurchaseItemWithProduct> purchaseItems) {
                progressItems.setVisibility(View.GONE);
                containerItems.removeAllViews();
                loadedPurchaseItems.clear();

                if (purchaseItems != null && !purchaseItems.isEmpty()) {
                    loadedPurchaseItems.addAll(purchaseItems);
                    textItemsCount.setText(String.format(Locale.getDefault(), "%d items", purchaseItems.size()));

                    double totalOrdered = 0.0;
                    double totalReceived = 0.0;
                    for (PurchaseItemWithProduct piwp : purchaseItems) {
                        if (piwp.purchaseItem != null) {
                            totalOrdered += piwp.purchaseItem.quantity;
                            totalReceived += piwp.purchaseItem.receivedQuantity;
                        }
                    }

                    if ("Partially Cancelled".equalsIgnoreCase(status)) {
                        double totalCancelled = Math.max(0.0, totalOrdered - totalReceived);
                        String cancelDate = !TextUtils.isEmpty(purchase.updatedAt) ? purchase.updatedAt : purchase.billingDate;
                        textCancellationDetails.setText(String.format(Locale.getDefault(), "Cancelled on %s. %s of %s units received across stages; remaining %s units were cancelled.", cancelDate != null ? cancelDate : "", CommonFunctions.formatQuantity(totalReceived), CommonFunctions.formatQuantity(totalOrdered), CommonFunctions.formatQuantity(totalCancelled)));
                    }

                    LayoutInflater inflater = LayoutInflater.from(PurchaseHistoryActivity.this);
                    for (PurchaseItemWithProduct itemWithProd : purchaseItems) {
                        PurchaseItem pItem = itemWithProd.purchaseItem;
                        if (pItem == null) continue;

                        View itemView = inflater.inflate(R.layout.item_purchase_detail_product, containerItems, false);

                        TextView textProdName = itemView.findViewById(R.id.text_item_product_name);
                        TextView textItemTotal = itemView.findViewById(R.id.text_item_total);
                        TextView textSku = itemView.findViewById(R.id.text_item_sku);
                        TextView textQtyPrice = itemView.findViewById(R.id.text_item_qty_price);
                        TextView chipOrdered = itemView.findViewById(R.id.chip_ordered_qty);
                        TextView chipReceived = itemView.findViewById(R.id.chip_received_qty);
                        TextView chipBalance = itemView.findViewById(R.id.chip_balance_qty);
                        TextView textTaxInfo = itemView.findViewById(R.id.text_item_tax_info);
                        TextView textDiscountInfo = itemView.findViewById(R.id.text_item_discount_info);

                        if (itemWithProd.product != null && !TextUtils.isEmpty(itemWithProd.product.productName)) {
                            textProdName.setText(itemWithProd.product.productName);
                        } else {
                            textProdName.setText(String.format(Locale.getDefault(), "Product #%d", pItem.productId));
                        }

                        if (itemWithProd.product != null && !TextUtils.isEmpty(itemWithProd.product.sku)) {
                            textSku.setVisibility(View.VISIBLE);
                            textSku.setText(String.format("SKU: %s", itemWithProd.product.sku));
                        } else {
                            textSku.setVisibility(View.GONE);
                        }

                        textQtyPrice.setText(String.format(Locale.getDefault(), "Cost Price: ₹ %,.2f", pItem.unitPrice));
                        textItemTotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", pItem.subtotal));

                        String uom = (itemWithProd.product != null && !TextUtils.isEmpty(itemWithProd.product.unitOfMeasure)) ? itemWithProd.product.unitOfMeasure.trim() : "";
                        chipOrdered.setText(String.format(Locale.getDefault(), "Ordered: %s %s", CommonFunctions.formatQuantity(pItem.quantity), uom).trim());
                        chipReceived.setText(String.format(Locale.getDefault(), "Received: %s %s", CommonFunctions.formatQuantity(pItem.receivedQuantity), uom).trim());

                        double remainingOrCancelled = Math.max(0.0, CommonFunctions.roundTo3Decimals(pItem.quantity - pItem.receivedQuantity));
                        if ("Partially Cancelled".equalsIgnoreCase(status) || "Cancelled".equalsIgnoreCase(status)) {
                            if (remainingOrCancelled > 0.0001) {
                                chipBalance.setVisibility(View.VISIBLE);
                                chipBalance.setText(String.format(Locale.getDefault(), "Cancelled: %s %s", CommonFunctions.formatQuantity(remainingOrCancelled), uom).trim());
                                chipBalance.setTextColor(ContextCompat.getColor(PurchaseHistoryActivity.this, R.color.status_red));
                                chipBalance.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(PurchaseHistoryActivity.this, R.color.status_red_bg)));
                            } else {
                                chipBalance.setVisibility(View.VISIBLE);
                                chipBalance.setText("✓ Fulfilled");
                                chipBalance.setTextColor(ContextCompat.getColor(PurchaseHistoryActivity.this, R.color.status_green));
                                chipBalance.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(PurchaseHistoryActivity.this, R.color.status_green_bg)));
                            }
                        } else if ("Partially Received".equalsIgnoreCase(status) || "Pending".equalsIgnoreCase(status)) {
                            if (remainingOrCancelled > 0.0001) {
                                chipBalance.setVisibility(View.VISIBLE);
                                chipBalance.setText(String.format(Locale.getDefault(), "Pending: %s %s", CommonFunctions.formatQuantity(remainingOrCancelled), uom).trim());
                                chipBalance.setTextColor(ContextCompat.getColor(PurchaseHistoryActivity.this, R.color.status_orange));
                                chipBalance.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(PurchaseHistoryActivity.this, R.color.status_orange_bg)));
                            } else {
                                chipBalance.setVisibility(View.VISIBLE);
                                chipBalance.setText("✓ Fulfilled");
                                chipBalance.setTextColor(ContextCompat.getColor(PurchaseHistoryActivity.this, R.color.status_green));
                                chipBalance.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(PurchaseHistoryActivity.this, R.color.status_green_bg)));
                            }
                        } else {
                            chipBalance.setVisibility(View.VISIBLE);
                            chipBalance.setText("✓ Fully Received");
                            chipBalance.setTextColor(ContextCompat.getColor(PurchaseHistoryActivity.this, R.color.status_green));
                            chipBalance.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(PurchaseHistoryActivity.this, R.color.status_green_bg)));
                        }

                        if (pItem.cgstRate > 0 || pItem.sgstRate > 0) {
                            textTaxInfo.setText(String.format(Locale.getDefault(), "GST: %.1f%% (₹ %,.2f)", pItem.cgstRate + pItem.sgstRate, pItem.cgstAmount + pItem.sgstAmount));
                        } else if (pItem.igstRate > 0) {
                            textTaxInfo.setText(String.format(Locale.getDefault(), "IGST: %.1f%% (₹ %,.2f)", pItem.igstRate, pItem.igstAmount));
                        } else {
                            textTaxInfo.setText("GST: 0%");
                        }

                        if (pItem.discountAmount > 0) {
                            textDiscountInfo.setVisibility(View.VISIBLE);
                            textDiscountInfo.setText(String.format(Locale.getDefault(), "Disc: ₹ %,.2f", pItem.discountAmount));
                        } else {
                            textDiscountInfo.setVisibility(View.GONE);
                        }

                        containerItems.addView(itemView);
                    }
                } else {
                    textItemsCount.setText("0 items");
                }
            }
        });

        // Load Receive Stages
        progressStages.setVisibility(View.VISIBLE);
        containerStages.removeAllViews();

        purchaseRepository.getReceiveRecordsWithItems(purchase.purchaseId).observe(this, new Observer<List<ReceiveRecordWithItems>>() {
            @Override
            public void onChanged(List<ReceiveRecordWithItems> stageRecords) {
                progressStages.setVisibility(View.GONE);
                containerStages.removeAllViews();

                if (stageRecords == null || stageRecords.isEmpty()) {
                    sectionStages.setVisibility(View.GONE);
                    return;
                }

                sectionStages.setVisibility(View.VISIBLE);
                textStagesCount.setText(String.format(Locale.getDefault(), "%d stage%s", stageRecords.size(), stageRecords.size() == 1 ? "" : "s"));

                LayoutInflater inflater = LayoutInflater.from(PurchaseHistoryActivity.this);
                SimpleDateFormat dtDisplayFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                int stageNumber = 1;
                for (ReceiveRecordWithItems recWithItems : stageRecords) {
                    ReceiveRecord record = recWithItems.record;
                    if (record == null) continue;

                    View stageView = inflater.inflate(R.layout.item_receive_stage, containerStages, false);

                    TextView textStageTitle = stageView.findViewById(R.id.text_stage_title);
                    TextView textStageDateTime = stageView.findViewById(R.id.text_stage_date_time);
                    LinearLayout containerStageItems = stageView.findViewById(R.id.container_stage_items);
                    TextView textStageNotes = stageView.findViewById(R.id.text_stage_notes);
                    TextView textStageTotalUnits = stageView.findViewById(R.id.text_stage_total_units);
                    View btnDownloadBill = stageView.findViewById(R.id.btn_download_stage_bill);

                    final int currentStageNumber = stageNumber;
                    textStageTitle.setText(String.format(Locale.getDefault(), "STAGE %d", currentStageNumber));

                    String dateDisplay = record.createdAt;
                    if (!TextUtils.isEmpty(record.createdAt)) {
                        try {
                            Date d = parseFormat.parse(record.createdAt);
                            if (d != null) dateDisplay = dtDisplayFormat.format(d);
                        } catch (ParseException ignored) {
                        }
                    } else if (!TextUtils.isEmpty(record.receiveDate)) {
                        dateDisplay = record.receiveDate;
                    }
                    textStageDateTime.setText(dateDisplay != null ? dateDisplay : "");

                    double totalStageUnits = 0.0;
                    containerStageItems.removeAllViews();

                    if (recWithItems.items != null) {
                        for (ReceiveItem rItem : recWithItems.items) {
                            totalStageUnits += rItem.quantityReceived;

                            String prodName = "Product #" + rItem.productId;
                            for (PurchaseItemWithProduct piwp : loadedPurchaseItems) {
                                if (piwp.purchaseItem != null && piwp.purchaseItem.purchaseItemId == rItem.purchaseItemId) {
                                    if (piwp.product != null && !TextUtils.isEmpty(piwp.product.productName)) {
                                        prodName = piwp.product.productName;
                                    }
                                    break;
                                }
                            }

                            TextView itemRow = new TextView(PurchaseHistoryActivity.this);
                            itemRow.setText(String.format(Locale.getDefault(), "• %s × %s", CommonFunctions.formatQuantity(rItem.quantityReceived), prodName));
                            itemRow.setTextColor(ContextCompat.getColor(PurchaseHistoryActivity.this, R.color.fg));
                            itemRow.setTextSize(12f);
                            itemRow.setPadding(0, 2, 0, 2);
                            containerStageItems.addView(itemRow);
                        }
                    }

                    textStageTotalUnits.setText(String.format(Locale.getDefault(), "Total Received: %s Unit%s", CommonFunctions.formatQuantity(totalStageUnits), totalStageUnits == 1.0 ? "" : "s"));

                    if (!TextUtils.isEmpty(record.notes)) {
                        textStageNotes.setVisibility(View.VISIBLE);
                        textStageNotes.setText(String.format("Notes: %s", record.notes));
                    } else {
                        textStageNotes.setVisibility(View.GONE);
                    }

                    // Bill Download / Save for this specific stage
                    final int recordId = record.receiveRecordId;
                    btnDownloadBill.setOnClickListener(v -> {
                        btnDownloadBill.setEnabled(false);
                        purchaseRepository.getReceiveDetailsForBill(recordId, new PurchaseRepository.ReceiveDetailsCallback() {
                            @Override
                            public void onLoaded(ReceiveRecord r, List<ReceiveItem> items, Purchase p, List<PurchaseItemWithProduct> allItems) {
                                btnDownloadBill.setEnabled(true);
                                Bitmap billBitmap = CommonFunctions.createReceiveBillBitmap(PurchaseHistoryActivity.this, r, items, p != null ? p : purchase, supplier, allItems != null ? allItems : loadedPurchaseItems, cachedCompanyConfigs);

                                if (billBitmap != null) {
                                    String fileName = "GRN_Stage" + currentStageNumber + "_" + r.receiveRecordId + "_" + System.currentTimeMillis() + ".png";
                                    BitmapHelper.saveBitmapToDownloads(billBitmap, PurchaseHistoryActivity.this, fileName);
                                    Toast.makeText(PurchaseHistoryActivity.this, String.format(Locale.getDefault(), "Stage %d GRN Bill saved to Downloads!", currentStageNumber), Toast.LENGTH_LONG).show();
                                    File pngFile = BitmapHelper.saveBitmapAsPng(PurchaseHistoryActivity.this, billBitmap, fileName);
                                    if (pngFile != null) {
                                        String title = "GRN Stage " + currentStageNumber;
                                        BillViewerActivity.start(PurchaseHistoryActivity.this, pngFile.getAbsolutePath(), title, fileName);
                                    }
                                } else {
                                    Toast.makeText(PurchaseHistoryActivity.this, "Failed to create bill bitmap.", Toast.LENGTH_SHORT).show();
                                }
                            }

                            @Override
                            public void onError(String message) {
                                btnDownloadBill.setEnabled(true);
                                Toast.makeText(PurchaseHistoryActivity.this, "Error generating bill: " + message, Toast.LENGTH_LONG).show();
                            }
                        });
                    });

                    containerStages.addView(stageView);
                    stageNumber++;
                }
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnDone.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
