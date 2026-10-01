package in.gbtsolutions.inventoryhub.activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import java.io.File;
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

import android.content.res.ColorStateList;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

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
import in.gbtsolutions.inventoryhub.adapters.SellHistoryAdapter;
import in.gbtsolutions.inventoryhub.helpers.BitmapHelper;
import in.gbtsolutions.inventoryhub.helpers.CommonFunctions;
import in.gbtsolutions.inventoryhub.models.Buyer;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleItem;
import in.gbtsolutions.inventoryhub.models.SaleItemWithProduct;
import in.gbtsolutions.inventoryhub.models.SaleWithBuyer;
import in.gbtsolutions.inventoryhub.repository.ConfigRepository;
import in.gbtsolutions.inventoryhub.repository.SaleRepository;

public class SellHistoryActivity extends BaseActivity {

    private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
    private final Map<String, String> cachedCompanyConfigs = new HashMap<>();
    // Views - Filters
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
    // Views - Summary & List
    private TextView textSalesCount;
    private TextView textSalesTotalSum;
    private RecyclerView recyclerSalesHistory;
    private View layoutEmptyState;
    private Button btnEmptyReset;
    // Filter State (stored in ISO yyyy-MM-dd format for SQLite string comparison)
    @Nullable
    private String selectedStartDate = null;
    @Nullable
    private String selectedEndDate = null;
    private long startDateMillis = System.currentTimeMillis();
    private long endDateMillis = System.currentTimeMillis();
    // Adapter & Repository
    private SellHistoryAdapter adapter;
    private final Observer<List<SaleWithBuyer>> salesObserver = this::updateSalesList;
    private SaleRepository saleRepository;
    private LiveData<List<SaleWithBuyer>> currentSalesLiveData;
    private ConfigRepository configRepository;
    private Bitmap lastGeneratedBillBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sell_history);

        saleRepository = new SaleRepository(getApplication());
        configRepository = new ConfigRepository(getApplication());
        configRepository.getAllConfigs().observe(this, configs -> {
            if (configs != null) {
                cachedCompanyConfigs.clear();
                for (Config c : configs) {
                    cachedCompanyConfigs.put(c.getConfigKey(), c.getConfigValue());
                }
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        View headerContainer = findViewById(R.id.header_container);
        View contentContainer = findViewById(R.id.sell_history_content_container);

        applyDrawerInsets(drawerLayout, headerContainer, contentContainer, navView);
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_sell_history);

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
        recyclerSalesHistory = findViewById(R.id.recycler_sales_history);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        btnEmptyReset = findViewById(R.id.btn_empty_reset);
    }

    private void setupDatePickers() {
        boxStartDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Start Date")
                    .setSelection(startDateMillis)
                    .build();

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

            datePicker.show(getSupportFragmentManager(), "START_DATE_PICKER");
        });

        btnClearStartDate.setOnClickListener(v -> {
            selectedStartDate = null;
            textStartDate.setText("Start Date");
            textStartDate.setTextColor(ContextCompat.getColor(this, R.color.fg_dim));
            btnClearStartDate.setVisibility(View.GONE);
            updateResetButtonVisibility();
        });

        boxEndDate.setOnClickListener(v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select End Date")
                    .setSelection(endDateMillis)
                    .build();

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

            datePicker.show(getSupportFragmentManager(), "END_DATE_PICKER");
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
        adapter = new SellHistoryAdapter();
        recyclerSalesHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerSalesHistory.setAdapter(adapter);

        adapter.setOnSaleClickListener(this::showSaleDetailsSheet);
    }

    private void applyFilters() {
        String query = editSearchBuyer.getText() != null ? editSearchBuyer.getText().toString().trim() : "";

        if (currentSalesLiveData != null) {
            currentSalesLiveData.removeObserver(salesObserver);
        }

        currentSalesLiveData = saleRepository.getFilteredSales(selectedStartDate, selectedEndDate, query);
        currentSalesLiveData.observe(this, salesObserver);

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

    private void updateSalesList(@Nullable List<SaleWithBuyer> sales) {
        if (sales == null || sales.isEmpty()) {
            adapter.setSales(new ArrayList<>());
            recyclerSalesHistory.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            textSalesCount.setText("No sales found");
            textSalesTotalSum.setText("Total: ₹ 0.00");
        } else {
            adapter.setSales(sales);
            recyclerSalesHistory.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);

            double totalSum = 0.0;
            int cancelledCount = 0;
            for (SaleWithBuyer item : sales) {
                if (item.sale != null) {
                    if ("Cancelled".equalsIgnoreCase(item.sale.status)) {
                        cancelledCount++;
                    } else {
                        totalSum += item.sale.totalAmount;
                    }
                }
            }

            if (cancelledCount > 0) {
                textSalesCount.setText(String.format(Locale.getDefault(), "Showing %d sales (%d cancelled)", sales.size(), cancelledCount));
            } else {
                textSalesCount.setText(String.format(Locale.getDefault(), "Showing %d sales", sales.size()));
            }
            textSalesTotalSum.setText(String.format(Locale.getDefault(), "Total: ₹ %,.2f", totalSum));
        }
    }

    private void showSaleDetailsSheet(@NonNull SaleWithBuyer item) {
        Sale sale = item.sale;
        if (sale == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_sell_details, null);
        dialog.setContentView(view);

        // Header Views
        TextView textInvoiceId = view.findViewById(R.id.text_detail_invoice_id);
        TextView textDateTime = view.findViewById(R.id.text_detail_date_time);
        View btnClose = view.findViewById(R.id.btn_close_sell_detail);
        LinearLayout layoutStatusBadge = view.findViewById(R.id.layout_detail_status_badge);
        View viewStatusDot = view.findViewById(R.id.view_detail_status_dot);
        TextView textStatus = view.findViewById(R.id.text_detail_status);
        TextView textSaleId = view.findViewById(R.id.text_detail_sale_id);

        // Cancellation Banner
        LinearLayout layoutCancellationBanner = view.findViewById(R.id.layout_sell_cancellation_banner);
        TextView textCancellationTitle = view.findViewById(R.id.text_sell_cancellation_title);
        TextView textCancellationDetails = view.findViewById(R.id.text_sell_cancellation_details);

        // Buyer Views
        TextView textBuyerName = view.findViewById(R.id.text_detail_buyer_name);
        TextView textBuyerContact = view.findViewById(R.id.text_detail_buyer_contact);
        TextView textBuyerPhone = view.findViewById(R.id.text_detail_buyer_phone);
        TextView textBuyerGst = view.findViewById(R.id.text_detail_buyer_gst);
        TextView textBuyerAddress = view.findViewById(R.id.text_detail_buyer_address);
        View btnCallBuyer = view.findViewById(R.id.btn_action_call_buyer);

        // Items Sold Views
        TextView textItemsCount = view.findViewById(R.id.text_detail_items_count);
        LinearLayout containerItems = view.findViewById(R.id.container_detail_items);
        ProgressBar progressItems = view.findViewById(R.id.progress_detail_items);

        // Summary Views
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
        Button btnCancelBill = view.findViewById(R.id.btn_cancel_sell_bill);
        Button btnReissueBill = view.findViewById(R.id.btn_reissue_sell_bill);
        Button btnDone = view.findViewById(R.id.btn_detail_done);
        Button btnPrintBill = view.findViewById(R.id.btn_print_sell_bill);

        // Populate Header & Status
        final String invoiceDisplayId = !TextUtils.isEmpty(sale.invoiceId)
                ? sale.invoiceId
                : String.format(Locale.getDefault(), "#SALE-%d", sale.saleId);
        textInvoiceId.setText(invoiceDisplayId);
        textSaleId.setText(String.format(Locale.getDefault(), "#SALE-%d", sale.saleId));

        boolean isCancelled = "Cancelled".equalsIgnoreCase(sale.status);
        String displayStatus = !TextUtils.isEmpty(sale.status) ? sale.status : "Completed";
        textStatus.setText(displayStatus);

        int statusTextColor = ContextCompat.getColor(this, isCancelled ? R.color.status_red : R.color.status_green);
        int statusBgColor = ContextCompat.getColor(this, isCancelled ? R.color.status_red_bg : R.color.status_green_bg);
        textStatus.setTextColor(statusTextColor);
        if (viewStatusDot != null) {
            viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(statusTextColor));
        }
        if (layoutStatusBadge != null) {
            layoutStatusBadge.setBackgroundTintList(ColorStateList.valueOf(statusBgColor));
        }

        if (isCancelled) {
            if (layoutCancellationBanner != null) {
                layoutCancellationBanner.setVisibility(View.VISIBLE);
                String cancelDate = !TextUtils.isEmpty(sale.updatedAt) ? sale.updatedAt : sale.billingDate;
                if (textCancellationDetails != null) {
                    textCancellationDetails.setText(String.format("Cancelled on %s. All items were returned to inventory.", cancelDate != null ? cancelDate : ""));
                }
            }
            if (btnCancelBill != null) btnCancelBill.setVisibility(View.GONE);
            if (btnReissueBill != null) btnReissueBill.setVisibility(View.VISIBLE);
        } else {
            if (layoutCancellationBanner != null) {
                layoutCancellationBanner.setVisibility(View.GONE);
            }
            if (btnCancelBill != null) btnCancelBill.setVisibility(View.VISIBLE);
            if (btnReissueBill != null) btnReissueBill.setVisibility(View.GONE);
        }

        if (btnCancelBill != null) {
            btnCancelBill.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(SellHistoryActivity.this)
                        .setTitle("Cancel Sell Bill")
                        .setMessage(String.format("Are you sure you want to cancel bill %s?\n\n• All sold items will be returned to inventory.\n• The bill will be marked as 'Cancelled'.\n• This action cannot be undone.", invoiceDisplayId))
                        .setPositiveButton("Yes, Cancel Bill", (confirmDialog, which) -> {
                            confirmDialog.dismiss();
                            btnCancelBill.setEnabled(false);
                            btnCancelBill.setText("Cancelling...");

                            saleRepository.cancelSale(sale.saleId, new SaleRepository.SaleActionCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(SellHistoryActivity.this, "Bill " + invoiceDisplayId + " cancelled and inventory restored.", Toast.LENGTH_SHORT).show();
                                    dialog.dismiss();

                                    new MaterialAlertDialogBuilder(SellHistoryActivity.this)
                                            .setTitle("Bill Cancelled")
                                            .setMessage("Bill " + invoiceDisplayId + " has been cancelled and products returned to inventory.\n\nWould you like to re-issue a new bill now?")
                                            .setPositiveButton("Re-issue New Bill", (d, w) -> {
                                                d.dismiss();
                                                Intent intent = new Intent(SellHistoryActivity.this, SellActivity.class);
                                                startActivity(intent);
                                            })
                                            .setNegativeButton("Close", null)
                                            .show();
                                }

                                @Override
                                public void onError(String message) {
                                    btnCancelBill.setEnabled(true);
                                    btnCancelBill.setText("Cancel Bill");
                                    Toast.makeText(SellHistoryActivity.this, "Failed to cancel bill: " + message, Toast.LENGTH_LONG).show();
                                }
                            });
                        })
                        .setNegativeButton("Keep Bill", null)
                        .show();
            });
        }

        if (btnReissueBill != null) {
            btnReissueBill.setOnClickListener(v -> {
                dialog.dismiss();
                Intent intent = new Intent(SellHistoryActivity.this, SellActivity.class);
                startActivity(intent);
            });
        }

        String formattedDate = sale.billingDate;
        if (!TextUtils.isEmpty(sale.createdAt)) {
            formattedDate = sale.createdAt;
        } else if (!TextUtils.isEmpty(sale.billingDate)) {
            try {
                Date date = isoDateFormat.parse(sale.billingDate);
                if (date != null) {
                    formattedDate = displayDateFormat.format(date);
                }
            } catch (ParseException ignored) {
            }
        }
        textDateTime.setText(String.format("Billed on %s", formattedDate != null ? formattedDate : ""));

        // Populate Buyer Info
        Buyer buyer = item.buyer;
        String custName;
        String custContact = null;
        String custPhone;
        String custGst;
        String custAddress = null;

        if (buyer != null) {
            custName = !TextUtils.isEmpty(buyer.buyerName) ? buyer.buyerName : (!TextUtils.isEmpty(sale.customerName) ? sale.customerName : "Unnamed Customer");
            custContact = buyer.contactPerson;
            custPhone = !TextUtils.isEmpty(buyer.phone) ? buyer.phone : sale.customerPhone;
            custGst = !TextUtils.isEmpty(buyer.gst) ? buyer.gst : sale.customerGstin;

            StringBuilder addressBuilder = new StringBuilder();
            if (!TextUtils.isEmpty(buyer.address)) addressBuilder.append(buyer.address);
            if (!TextUtils.isEmpty(buyer.city)) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(buyer.city);
            }
            if (!TextUtils.isEmpty(buyer.stateCode)) {
                if (addressBuilder.length() > 0) addressBuilder.append(", ");
                addressBuilder.append(buyer.stateCode);
            }
            if (!TextUtils.isEmpty(buyer.postalCode)) {
                if (addressBuilder.length() > 0) addressBuilder.append(" - ");
                addressBuilder.append(buyer.postalCode);
            }

            if (addressBuilder.length() > 0) {
                custAddress = addressBuilder.toString();
            }
        } else {
            custName = !TextUtils.isEmpty(sale.customerName) ? sale.customerName : "Walk-in Customer";
            custPhone = sale.customerPhone;
            custGst = sale.customerGstin;
            if (!TextUtils.isEmpty(sale.placeOfSupply)) {
                custAddress = "POS: " + sale.placeOfSupply;
            }
        }

        textBuyerName.setText(custName);

        if (!TextUtils.isEmpty(custContact)) {
            textBuyerContact.setVisibility(View.VISIBLE);
            textBuyerContact.setText(String.format("Contact: %s", custContact));
        } else {
            textBuyerContact.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(custPhone)) {
            final String phoneToCall = custPhone;
            textBuyerPhone.setVisibility(View.VISIBLE);
            textBuyerPhone.setText(String.format("Phone: %s", custPhone));
            btnCallBuyer.setVisibility(View.VISIBLE);
            btnCallBuyer.setOnClickListener(v -> {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneToCall));
                startActivity(dialIntent);
            });
        } else {
            textBuyerPhone.setVisibility(View.GONE);
            btnCallBuyer.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(custGst)) {
            textBuyerGst.setVisibility(View.VISIBLE);
            textBuyerGst.setText(String.format("GSTIN: %s", custGst));
        } else {
            textBuyerGst.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(custAddress)) {
            textBuyerAddress.setVisibility(View.VISIBLE);
            textBuyerAddress.setText(custAddress.startsWith("Address:") || custAddress.startsWith("POS:") ? custAddress : String.format("Address: %s", custAddress));
        } else {
            textBuyerAddress.setVisibility(View.GONE);
        }

        // Populate Financial Summary
        textSubtotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.subtotalAmount));

        if (sale.discountAmount > 0) {
            rowDiscount.setVisibility(View.VISIBLE);
            textDiscount.setText(String.format(Locale.getDefault(), "- ₹ %,.2f", sale.discountAmount));
        } else {
            rowDiscount.setVisibility(View.GONE);
        }

        if (sale.cgstAmount > 0) {
            rowCgst.setVisibility(View.VISIBLE);
            textCgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.cgstAmount));
        } else {
            rowCgst.setVisibility(View.GONE);
        }

        if (sale.sgstAmount > 0) {
            rowSgst.setVisibility(View.VISIBLE);
            textSgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.sgstAmount));
        } else {
            rowSgst.setVisibility(View.GONE);
        }

        if (sale.igstAmount > 0) {
            rowIgst.setVisibility(View.VISIBLE);
            textIgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.igstAmount));
        } else {
            rowIgst.setVisibility(View.GONE);
        }

        textTotalGst.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.totalGst));

        if (sale.otherCharges > 0) {
            rowOtherCharges.setVisibility(View.VISIBLE);
            textOtherCharges.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.otherCharges));
        } else {
            rowOtherCharges.setVisibility(View.GONE);
        }

        textGrandTotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.totalAmount));

        // Load and populate Items Sold
        progressItems.setVisibility(View.VISIBLE);
        containerItems.removeAllViews();
        final List<SaleItemWithProduct> currentSaleItems = new ArrayList<>();

        saleRepository.getSaleItemsWithProduct(sale.saleId).observe(this, new Observer<List<SaleItemWithProduct>>() {
            @Override
            public void onChanged(List<SaleItemWithProduct> saleItems) {
                progressItems.setVisibility(View.GONE);
                containerItems.removeAllViews();
                currentSaleItems.clear();
                if (saleItems != null) {
                    currentSaleItems.addAll(saleItems);
                }

                if (saleItems != null && !saleItems.isEmpty()) {
                    textItemsCount.setText(String.format(Locale.getDefault(), "%d items", saleItems.size()));

                    LayoutInflater inflater = LayoutInflater.from(SellHistoryActivity.this);
                    for (SaleItemWithProduct itemWithProd : saleItems) {
                        SaleItem sItem = itemWithProd.saleItem;
                        if (sItem == null) continue;

                        View itemView = inflater.inflate(R.layout.item_sell_detail_product, containerItems, false);

                        TextView textProdName = itemView.findViewById(R.id.text_item_product_name);
                        TextView textItemTotal = itemView.findViewById(R.id.text_item_total);
                        TextView textSku = itemView.findViewById(R.id.text_item_sku);
                        TextView textBatchNo = itemView.findViewById(R.id.text_item_batch_no);
                        TextView textQtyPrice = itemView.findViewById(R.id.text_item_qty_price);
                        TextView textTaxInfo = itemView.findViewById(R.id.text_item_tax_info);
                        TextView textDiscountInfo = itemView.findViewById(R.id.text_item_discount_info);

                        if (itemWithProd.product != null && !TextUtils.isEmpty(itemWithProd.product.productName)) {
                            textProdName.setText(itemWithProd.product.productName);
                        } else {
                            textProdName.setText(String.format(Locale.getDefault(), "Product #%d", sItem.productId));
                        }

                        if (itemWithProd.product != null && !TextUtils.isEmpty(itemWithProd.product.sku)) {
                            textSku.setVisibility(View.VISIBLE);
                            textSku.setText(String.format("SKU: %s", itemWithProd.product.sku));
                        } else {
                            textSku.setVisibility(View.GONE);
                        }

                        if (textBatchNo != null) {
                            if (itemWithProd.batch != null && !TextUtils.isEmpty(itemWithProd.batch.batchNo)) {
                                textBatchNo.setVisibility(View.VISIBLE);
                                textBatchNo.setText(String.format("Batch: %s", itemWithProd.batch.batchNo));
                            } else if (sItem.batchId != null && sItem.batchId > 0) {
                                textBatchNo.setVisibility(View.VISIBLE);
                                textBatchNo.setText(String.format("Batch #%d", sItem.batchId));
                            } else {
                                textBatchNo.setVisibility(View.GONE);
                            }
                        }

                        textQtyPrice.setText(String.format(Locale.getDefault(), "Qty: %d × ₹ %,.2f", sItem.quantity, sItem.unitPrice));
                        textItemTotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", sItem.subtotal));

                        // Tax display
                        if (sItem.cgstRate > 0 || sItem.sgstRate > 0) {
                            textTaxInfo.setText(String.format(Locale.getDefault(), "GST: %.1f%% (₹ %,.2f)", sItem.cgstRate + sItem.sgstRate, sItem.cgstAmount + sItem.sgstAmount));
                        } else if (sItem.igstRate > 0) {
                            textTaxInfo.setText(String.format(Locale.getDefault(), "IGST: %.1f%% (₹ %,.2f)", sItem.igstRate, sItem.igstAmount));
                        } else {
                            textTaxInfo.setText("GST: 0%");
                        }

                        // Discount display
                        if (sItem.discountAmount > 0) {
                            textDiscountInfo.setVisibility(View.VISIBLE);
                            textDiscountInfo.setText(String.format(Locale.getDefault(), "Disc: ₹ %,.2f", sItem.discountAmount));
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

        // Close actions
        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnDone.setOnClickListener(v -> dialog.dismiss());
        btnPrintBill.setOnClickListener(v -> {
            if (currentSaleItems.isEmpty() && progressItems.getVisibility() == View.VISIBLE) {
                Toast.makeText(SellHistoryActivity.this, "Loading items, please wait...", Toast.LENGTH_SHORT).show();
                return;
            }
            Buyer buyerForBill = item.buyer;
            if (buyerForBill == null) {
                buyerForBill = new Buyer();
                buyerForBill.buyerName = !TextUtils.isEmpty(sale.customerName) ? sale.customerName : "Walk-in Customer";
                buyerForBill.phone = sale.customerPhone != null ? sale.customerPhone : "";
                buyerForBill.gst = sale.customerGstin != null ? sale.customerGstin : "";
                buyerForBill.stateCode = sale.placeOfSupplyStateCode;
                buyerForBill.city = sale.placeOfSupply;
            }
            lastGeneratedBillBitmap = CommonFunctions.createSellBillBitmap(SellHistoryActivity.this, sale, buyerForBill, currentSaleItems, cachedCompanyConfigs);
            if (lastGeneratedBillBitmap != null) {
                String fileName = "invoice_" + (sale.invoiceId != null ? sale.invoiceId : System.currentTimeMillis()) + ".png";
                BitmapHelper.saveBitmapToDownloads(lastGeneratedBillBitmap, SellHistoryActivity.this, fileName);
                Toast.makeText(SellHistoryActivity.this, "Bill bitmap created and saved to Downloads", Toast.LENGTH_SHORT).show();
                File pngFile = BitmapHelper.saveBitmapAsPng(SellHistoryActivity.this, lastGeneratedBillBitmap, fileName);
                if (pngFile != null) {
                    String title = "Invoice #" + (!TextUtils.isEmpty(sale.invoiceId) ? sale.invoiceId : String.valueOf(sale.saleId));
                    BillViewerActivity.start(SellHistoryActivity.this, pngFile.getAbsolutePath(), title, fileName);
                }
            } else {
                Toast.makeText(SellHistoryActivity.this, "Failed to create bill", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}