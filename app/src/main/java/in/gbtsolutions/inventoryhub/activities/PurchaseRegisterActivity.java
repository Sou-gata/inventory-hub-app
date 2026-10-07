package in.gbtsolutions.inventoryhub.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.BitmapHelper;
import in.gbtsolutions.inventoryhub.helpers.CommonFunctions;
import in.gbtsolutions.inventoryhub.models.CartItem;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseItemWithProduct;
import in.gbtsolutions.inventoryhub.models.Suppliers;
import in.gbtsolutions.inventoryhub.reports.purchaseregister.PurchaseRegisterCalculator;
import in.gbtsolutions.inventoryhub.reports.purchaseregister.PurchaseRegisterCsvExporter;
import in.gbtsolutions.inventoryhub.reports.purchaseregister.PurchaseRegisterExcelExporter;
import in.gbtsolutions.inventoryhub.reports.purchaseregister.PurchaseRegisterModels;
import in.gbtsolutions.inventoryhub.reports.purchaseregister.PurchaseRegisterPdfExporter;

public class PurchaseRegisterActivity extends BaseActivity {

    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    static {
        DB_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        DISPLAY_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private File pendingFileToSave;
    private String pendingMimeTypeToSave;

    private final ActivityResultLauncher<String> storagePermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
        if (isGranted) {
            if (pendingFileToSave != null) {
                saveFileToDownloads(pendingFileToSave, pendingMimeTypeToSave);
            }
        } else {
            Toast.makeText(this, "Storage permission is required to save reports to Downloads.", Toast.LENGTH_SHORT).show();
        }
    });
    private NavigationView navView;
    // Filters
    private TextView tvStartDate;
    private TextView tvEndDate;
    private TextView chipThisMonth;
    private TextView chipLastMonth;
    private TextView chipThisQuarter;
    private TextView chipAllTime;
    private TextView chipToday;
    private EditText inputSearch;
    private ImageView btnClearSearch;
    private TextView chipFilterAll;
    private TextView chipFilterB2b;
    private TextView chipFilterB2c;
    private MaterialButton btnGenerate;
    private MaterialButton btnExport;
    // KPI Cards
    private TextView kpiGrossSales;
    private TextView kpiInvoicesCount;
    private TextView kpiTaxableValue;
    private TextView kpiTotalTax;
    private TextView kpiCgstSgst;
    private TextView kpiIgstCess;
    private TextView kpiB2bB2cSplit;
    // Table
    private TableLayout tablePurchaseRegister;
    private TextView tvTableRecordsCount;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;
    // State
    private String selectedStartDate; // "yyyy-MM-dd"
    private String selectedEndDate;   // "yyyy-MM-dd"
    private int currentTypeFilter = 0; // 0: All, 1: Registered (B2B), 2: Unregistered
    private String currentSearchQuery = "";
    private PurchaseRegisterModels.ReportData currentReportData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase_register);

        GlobalStore.getInstance().loadSettings(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        View headerContainer = findViewById(R.id.header_container);
        View scrollView = findViewById(R.id.purchase_register_scroll_view);

        applyDrawerInsets(drawerLayout, headerContainer, scrollView, navView);
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_reports_purchase_register);

        initViews();
        initListeners();
        setDefaultDateRangeThisMonth();
        loadReport();
    }

    private void initViews() {
        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);
        chipThisMonth = findViewById(R.id.chip_this_month);
        chipLastMonth = findViewById(R.id.chip_last_month);
        chipThisQuarter = findViewById(R.id.chip_this_quarter);
        chipAllTime = findViewById(R.id.chip_all_time);
        chipToday = findViewById(R.id.chip_today);

        inputSearch = findViewById(R.id.input_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        chipFilterAll = findViewById(R.id.chip_filter_all);
        chipFilterB2b = findViewById(R.id.chip_filter_b2b);
        chipFilterB2c = findViewById(R.id.chip_filter_b2c);

        btnGenerate = findViewById(R.id.btn_generate_report);
        btnExport = findViewById(R.id.btn_export_report);

        kpiGrossSales = findViewById(R.id.kpi_gross_sales);
        kpiInvoicesCount = findViewById(R.id.kpi_invoices_count);
        kpiTaxableValue = findViewById(R.id.kpi_taxable_value);
        kpiTotalTax = findViewById(R.id.kpi_total_tax);
        kpiCgstSgst = findViewById(R.id.kpi_cgst_sgst);
        kpiIgstCess = findViewById(R.id.kpi_igst_cess);
        kpiB2bB2cSplit = findViewById(R.id.kpi_b2b_b2c_split);

        tablePurchaseRegister = findViewById(R.id.table_purchase_register);
        tvTableRecordsCount = findViewById(R.id.tv_table_records_count);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
    }

    private void initListeners() {
        findViewById(R.id.btn_pick_start_date).setOnClickListener(v -> showDatePicker(true));
        findViewById(R.id.btn_pick_end_date).setOnClickListener(v -> showDatePicker(false));

        chipThisMonth.setOnClickListener(v -> {
            highlightPresetChip(chipThisMonth);
            setDefaultDateRangeThisMonth();
            loadReport();
        });

        chipLastMonth.setOnClickListener(v -> {
            highlightPresetChip(chipLastMonth);
            setDateRangeLastMonth();
            loadReport();
        });

        chipThisQuarter.setOnClickListener(v -> {
            highlightPresetChip(chipThisQuarter);
            setDateRangeThisQuarter();
            loadReport();
        });

        chipAllTime.setOnClickListener(v -> {
            highlightPresetChip(chipAllTime);
            selectedStartDate = "2020-01-01";
            selectedEndDate = "2099-12-31";
            tvStartDate.setText("All Time");
            tvEndDate.setText("All Time");
            loadReport();
        });

        chipToday.setOnClickListener(v -> {
            highlightPresetChip(chipToday);
            setDateRangeToday();
            loadReport();
        });

        btnGenerate.setOnClickListener(v -> loadReport());
        btnExport.setOnClickListener(v -> showExportDialog());

        // Search Filter
        inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                btnClearSearch.setVisibility(!TextUtils.isEmpty(currentSearchQuery) ? View.VISIBLE : View.GONE);
                renderFilteredTable();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnClearSearch.setOnClickListener(v -> inputSearch.setText(""));

        // Type Filter Chips
        chipFilterAll.setOnClickListener(v -> {
            currentTypeFilter = 0;
            updateTypeChips();
            renderFilteredTable();
        });

        chipFilterB2b.setOnClickListener(v -> {
            currentTypeFilter = 1;
            updateTypeChips();
            renderFilteredTable();
        });

        chipFilterB2c.setOnClickListener(v -> {
            currentTypeFilter = 2;
            updateTypeChips();
            renderFilteredTable();
        });
    }

    private void updateTypeChips() {
        chipFilterAll.setBackgroundResource(currentTypeFilter == 0 ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        chipFilterAll.setTextColor(ContextCompat.getColor(this, currentTypeFilter == 0 ? R.color.white : R.color.fg_muted));

        chipFilterB2b.setBackgroundResource(currentTypeFilter == 1 ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        chipFilterB2b.setTextColor(ContextCompat.getColor(this, currentTypeFilter == 1 ? R.color.white : R.color.fg_muted));

        chipFilterB2c.setBackgroundResource(currentTypeFilter == 2 ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        chipFilterB2c.setTextColor(ContextCompat.getColor(this, currentTypeFilter == 2 ? R.color.white : R.color.fg_muted));
    }

    private void setDefaultDateRangeThisMonth() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date start = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date end = cal.getTime();

        selectedStartDate = DB_DATE_FORMAT.format(start);
        selectedEndDate = DB_DATE_FORMAT.format(end);

        tvStartDate.setText(DISPLAY_DATE_FORMAT.format(start));
        tvEndDate.setText(DISPLAY_DATE_FORMAT.format(end));
    }

    private void setDateRangeToday() {
        Calendar cal = Calendar.getInstance();
        Date today = cal.getTime();
        selectedStartDate = DB_DATE_FORMAT.format(today);
        selectedEndDate = DB_DATE_FORMAT.format(today);

        tvStartDate.setText(DISPLAY_DATE_FORMAT.format(today));
        tvEndDate.setText(DISPLAY_DATE_FORMAT.format(today));
    }

    private void setDateRangeLastMonth() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date start = cal.getTime();
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date end = cal.getTime();

        selectedStartDate = DB_DATE_FORMAT.format(start);
        selectedEndDate = DB_DATE_FORMAT.format(end);

        tvStartDate.setText(DISPLAY_DATE_FORMAT.format(start));
        tvEndDate.setText(DISPLAY_DATE_FORMAT.format(end));
    }

    private void setDateRangeThisQuarter() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH);
        int quarterStartMonth = (month / 3) * 3;

        cal.set(Calendar.MONTH, quarterStartMonth);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date start = cal.getTime();

        cal.set(Calendar.MONTH, quarterStartMonth + 2);
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
        Date end = cal.getTime();
        selectedStartDate = DB_DATE_FORMAT.format(start);
        selectedEndDate = DB_DATE_FORMAT.format(end);
        tvStartDate.setText(DISPLAY_DATE_FORMAT.format(start));
        tvEndDate.setText(DISPLAY_DATE_FORMAT.format(end));
    }

    private void highlightPresetChip(TextView activeChip) {
        TextView[] chips = {chipThisMonth, chipLastMonth, chipThisQuarter, chipAllTime, chipToday};
        for (TextView c : chips) {
            if (c == activeChip) {
                c.setBackgroundResource(R.drawable.bg_tab_active);
                c.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                c.setBackgroundResource(R.drawable.bg_tab_inactive);
                c.setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
            }
        }
    }

    private void showDatePicker(boolean isStart) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setTitleText(isStart ? "Select Start Date" : "Select End Date").setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            cal.setTimeInMillis(selection);
            Date pickedDate = cal.getTime();

            if (isStart) {
                selectedStartDate = DB_DATE_FORMAT.format(pickedDate);
                tvStartDate.setText(DISPLAY_DATE_FORMAT.format(pickedDate));
            } else {
                selectedEndDate = DB_DATE_FORMAT.format(pickedDate);
                tvEndDate.setText(DISPLAY_DATE_FORMAT.format(pickedDate));
            }
        });

        picker.show(getSupportFragmentManager(), isStart ? "PICK_START_DATE" : "PICK_END_DATE");
    }

    private void loadReport() {
        btnGenerate.setEnabled(false);
        btnGenerate.setText("Generating...");

        Executors.newSingleThreadExecutor().execute(() -> {
            PurchaseRegisterModels.ReportData report = PurchaseRegisterCalculator.generateReport(PurchaseRegisterActivity.this, selectedStartDate, selectedEndDate);

            new Handler(Looper.getMainLooper()).post(() -> {
                btnGenerate.setEnabled(true);
                btnGenerate.setText("Generate");
                currentReportData = report;
                renderReport();
            });
        });
    }

    @SuppressLint("SetTextI18n")
    private void renderReport() {
        if (currentReportData == null) return;

        // Update KPI Cards
        PurchaseRegisterModels.PurchaseRegisterSummary s = currentReportData.summary;
        kpiGrossSales.setText(formatCurrency(s.grossPurchases.doubleValue()));
        kpiInvoicesCount.setText(s.totalInvoices + " Bills (" + s.b2bInvoices + " Reg / " + s.b2cInvoices + " Unreg)");
        kpiTaxableValue.setText(formatCurrency(s.taxableValue.doubleValue()));
        kpiTotalTax.setText(formatCurrency(s.totalTax.doubleValue()));
        kpiCgstSgst.setText("CGST ITC: " + formatCurrency(s.cgst.doubleValue()) + " | SGST ITC: " + formatCurrency(s.sgst.doubleValue()));
        kpiIgstCess.setText("IGST ITC: " + formatCurrency(s.igst.doubleValue()));
        kpiB2bB2cSplit.setText(s.b2bInvoices + " Registered • " + s.b2cInvoices + " Unregistered");

        renderFilteredTable();
    }

    private void renderFilteredTable() {
        tablePurchaseRegister.removeAllViews();
        if (currentReportData == null || currentReportData.rows.isEmpty()) {
            tablePurchaseRegister.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("No purchase records found for selected period.");
            tvTableRecordsCount.setText("0 Bills");
            return;
        }

        // Filter rows based on search query and type filter
        List<PurchaseRegisterModels.PurchaseRegisterRow> filtered = new ArrayList<>();
        BigDecimal sumTaxable = BigDecimal.ZERO;
        BigDecimal sumCgst = BigDecimal.ZERO;
        BigDecimal sumSgst = BigDecimal.ZERO;
        BigDecimal sumIgst = BigDecimal.ZERO;
        BigDecimal sumTotalTax = BigDecimal.ZERO;
        BigDecimal sumTotalAmount = BigDecimal.ZERO;

        for (PurchaseRegisterModels.PurchaseRegisterRow row : currentReportData.rows) {
            // Type filter
            if (currentTypeFilter == 1 && !row.isB2B) continue;
            if (currentTypeFilter == 2 && row.isB2B) continue;

            // Search query filter
            if (!TextUtils.isEmpty(currentSearchQuery)) {
                boolean match = (row.supplierName != null && row.supplierName.toLowerCase(Locale.getDefault()).contains(currentSearchQuery)) || (row.invoiceId != null && row.invoiceId.toLowerCase(Locale.getDefault()).contains(currentSearchQuery)) || (row.supplierPhone != null && row.supplierPhone.contains(currentSearchQuery)) || (row.supplierGstin != null && row.supplierGstin.toLowerCase(Locale.getDefault()).contains(currentSearchQuery)) || (row.placeOfSupply != null && row.placeOfSupply.toLowerCase(Locale.getDefault()).contains(currentSearchQuery));
                if (!match) continue;
            }

            filtered.add(row);
            if (!"Cancelled".equalsIgnoreCase(row.status)) {
                sumTaxable = sumTaxable.add(row.taxableValue);
                sumCgst = sumCgst.add(row.cgstAmount);
                sumSgst = sumSgst.add(row.sgstAmount);
                sumIgst = sumIgst.add(row.igstAmount);
                sumTotalTax = sumTotalTax.add(row.totalTax);
                sumTotalAmount = sumTotalAmount.add(row.totalAmount);
            }
        }

        tvTableRecordsCount.setText(filtered.size() + " Bills");

        if (filtered.isEmpty()) {
            tablePurchaseRegister.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("No bills matched current search & type filters.");
            return;
        }

        tablePurchaseRegister.setVisibility(View.VISIBLE);
        layoutEmptyState.setVisibility(View.GONE);

        // Header Row
        TableRow headerRow = createHeaderRow();
        tablePurchaseRegister.addView(headerRow);

        // Data Rows
        int sl = 1;
        for (PurchaseRegisterModels.PurchaseRegisterRow row : filtered) {
            TableRow tr = createDataRow(sl++, row);
            tr.setOnClickListener(v -> showInvoiceDetailDialog(row));
            tablePurchaseRegister.addView(tr);
        }

        // Summary Total Row
        TableRow totalRow = createTotalRow(filtered.size(), sumTaxable, sumCgst, sumSgst, sumIgst, sumTotalTax, sumTotalAmount);
        tablePurchaseRegister.addView(totalRow);
    }

    private TableRow createHeaderRow() {
        TableRow tr = new TableRow(this);
        tr.setBackgroundColor(ContextCompat.getColor(this, R.color.material_blue));
        tr.setPadding(0, dpToPx(8), 0, dpToPx(8));

        String[] headers = {"#", "Invoice / PO #", "Date", "Supplier Name", "GSTIN", "Type", "POS", "Taxable (₹)", "CGST (₹)", "SGST (₹)", "IGST (₹)", "Total Tax (₹)", "Total (₹)", "Terms", "Status"};

        for (String h : headers) {
            TextView tv = new TextView(this);
            tv.setText(h);
            tv.setTextColor(android.graphics.Color.WHITE);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
            tr.addView(tv);
        }
        return tr;
    }

    private TableRow createDataRow(int sl, PurchaseRegisterModels.PurchaseRegisterRow row) {
        TableRow tr = new TableRow(this);
        tr.setBackgroundColor(sl % 2 == 0 ? android.graphics.Color.parseColor("#F8F9FD") : android.graphics.Color.WHITE);
        tr.setClickable(true);
        tr.setFocusable(true);
        tr.setPadding(0, dpToPx(8), 0, dpToPx(8));

        // #
        tr.addView(createCellText(String.valueOf(sl), Gravity.CENTER, false, false));
        // Invoice / PO #
        tr.addView(createCellText(row.invoiceId, Gravity.CENTER, true, true));
        // Date
        tr.addView(createCellText(row.formattedDate, Gravity.CENTER, false, false));
        // Supplier Name
        tr.addView(createCellText(row.supplierName, Gravity.START, false, false));
        // GSTIN
        tr.addView(createCellText(!TextUtils.isEmpty(row.supplierGstin) ? row.supplierGstin : "Unregistered", Gravity.CENTER, false, false));
        // Type
        tr.addView(createCellBadge(row.isB2B ? "Registered" : "Unregistered", row.isB2B));
        // POS
        tr.addView(createCellText(row.placeOfSupply, Gravity.START, false, false));
        // Taxable Value
        tr.addView(createCellText(row.taxableValue.toPlainString(), Gravity.END, false, false));
        // CGST
        tr.addView(createCellText(row.cgstAmount.toPlainString(), Gravity.END, false, false));
        // SGST
        tr.addView(createCellText(row.sgstAmount.toPlainString(), Gravity.END, false, false));
        // IGST
        tr.addView(createCellText(row.igstAmount.toPlainString(), Gravity.END, false, false));
        // Total Tax
        tr.addView(createCellText(row.totalTax.toPlainString(), Gravity.END, true, false));
        // Total
        tr.addView(createCellText(row.totalAmount.toPlainString(), Gravity.END, true, true));
        // Terms
        tr.addView(createCellText(row.paymentMethod, Gravity.CENTER, false, false));
        // Status
        tr.addView(createCellStatus(row.status));

        return tr;
    }

    private TableRow createTotalRow(int count, BigDecimal sumTaxable, BigDecimal sumCgst, BigDecimal sumSgst, BigDecimal sumIgst, BigDecimal sumTotalTax, BigDecimal sumTotalAmount) {

        TableRow tr = new TableRow(this);
        tr.setBackgroundColor(android.graphics.Color.parseColor("#EEF2F6"));
        tr.setPadding(0, dpToPx(10), 0, dpToPx(10));

        // 0: #
        tr.addView(createCellText("", Gravity.CENTER, true, false));
        // 1: Invoice / PO #
        tr.addView(createCellText("TOTAL", Gravity.CENTER, true, false));
        // 2: Date
        tr.addView(createCellText("(" + count + " Bills)", Gravity.CENTER, true, false));
        // 3: Supplier
        tr.addView(createCellText("", Gravity.START, true, false));
        // 4: GSTIN
        tr.addView(createCellText("", Gravity.CENTER, true, false));
        // 5: Type
        tr.addView(createCellText("", Gravity.CENTER, true, false));
        // 6: POS
        tr.addView(createCellText("", Gravity.START, true, false));
        // 7: Taxable
        tr.addView(createCellText(sumTaxable.toPlainString(), Gravity.END, true, false));
        // 8: CGST
        tr.addView(createCellText(sumCgst.toPlainString(), Gravity.END, true, false));
        // 9: SGST
        tr.addView(createCellText(sumSgst.toPlainString(), Gravity.END, true, false));
        // 10: IGST
        tr.addView(createCellText(sumIgst.toPlainString(), Gravity.END, true, false));
        // 11: Total Tax
        tr.addView(createCellText(sumTotalTax.toPlainString(), Gravity.END, true, false));
        // 12: Total
        tr.addView(createCellText(sumTotalAmount.toPlainString(), Gravity.END, true, true));
        // 13: Terms
        tr.addView(createCellText("", Gravity.CENTER, false, false));
        // 14: Status
        tr.addView(createCellText("", Gravity.CENTER, false, false));

        return tr;
    }

    private TextView createCellText(String text, int gravity, boolean bold, boolean isAccent) {
        TextView tv = new TextView(this);
        tv.setText(text != null ? text : "");
        tv.setGravity(gravity);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
        if (bold) {
            tv.setTypeface(null, Typeface.BOLD);
        }
        if (isAccent) {
            tv.setTextColor(ContextCompat.getColor(this, R.color.material_blue));
        } else {
            tv.setTextColor(ContextCompat.getColor(this, R.color.fg));
        }
        tv.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
        return tv;
    }

    private View createCellBadge(String text, boolean isB2B) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setGravity(Gravity.CENTER);
        wrapper.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4));

        TextView badge = new TextView(this);
        badge.setText(text);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setBackgroundResource(R.drawable.bg_badge_edit);
        badge.setTextColor(ContextCompat.getColor(this, isB2B ? R.color.material_blue : R.color.fg_muted));
        badge.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));

        wrapper.addView(badge);
        return wrapper;
    }

    private View createCellStatus(String status) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setGravity(Gravity.CENTER);
        wrapper.setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4));

        TextView tv = new TextView(this);
        tv.setText(status != null ? status : "Completed");
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 9.5f);
        tv.setTypeface(null, Typeface.BOLD);

        if ("Cancelled".equalsIgnoreCase(status)) {
            tv.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        } else if ("Pending".equalsIgnoreCase(status)) {
            tv.setTextColor(android.graphics.Color.parseColor("#E65100"));
        } else {
            tv.setTextColor(ContextCompat.getColor(this, R.color.status_green));
        }

        wrapper.addView(tv);
        return wrapper;
    }

    private void showInvoiceDetailDialog(PurchaseRegisterModels.PurchaseRegisterRow row) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_purchase_register_detail, null);

        TextView tvInvId = dialogView.findViewById(R.id.tv_detail_invoice_id);
        TextView tvDateMode = dialogView.findViewById(R.id.tv_detail_date_mode);
        TextView tvTypeBadge = dialogView.findViewById(R.id.tv_detail_type_badge);
        TextView tvSuppName = dialogView.findViewById(R.id.tv_detail_supplier_name);
        TextView tvSuppGstin = dialogView.findViewById(R.id.tv_detail_supplier_gstin);
        TextView tvSuppPhone = dialogView.findViewById(R.id.tv_detail_supplier_phone);

        TableLayout tableItems = dialogView.findViewById(R.id.table_detail_items);
        TextView tvTaxable = dialogView.findViewById(R.id.tv_detail_taxable);
        TextView tvCgstSgst = dialogView.findViewById(R.id.tv_detail_cgst_sgst);
        TextView tvIgst = dialogView.findViewById(R.id.tv_detail_igst);
        TextView tvCess = dialogView.findViewById(R.id.tv_detail_cess);
        TextView tvTotal = dialogView.findViewById(R.id.tv_detail_total);

        View rowIgst = dialogView.findViewById(R.id.row_detail_igst);
        View rowCess = dialogView.findViewById(R.id.row_detail_cess);

        View btnClose = dialogView.findViewById(R.id.btn_detail_close);
        View btnViewBill = dialogView.findViewById(R.id.btn_detail_view_bill);

        tvInvId.setText(row.invoiceId);
        tvDateMode.setText(row.formattedDate + " • " + row.paymentMethod + " • " + row.status);
        tvTypeBadge.setText(row.isB2B ? "REGISTERED VENDOR" : "UNREGISTERED VENDOR");
        tvSuppName.setText(row.supplierName);
        tvSuppGstin.setText("GSTIN: " + (!TextUtils.isEmpty(row.supplierGstin) ? row.supplierGstin : "Unregistered") + " • POS: " + row.placeOfSupply);
        tvSuppPhone.setText(!TextUtils.isEmpty(row.supplierPhone) ? "Phone: " + row.supplierPhone : "Phone: N/A");

        tvTaxable.setText(formatCurrency(row.taxableValue.doubleValue()));
        tvCgstSgst.setText(formatCurrency(row.cgstAmount.doubleValue()) + " / " + formatCurrency(row.sgstAmount.doubleValue()));
        tvIgst.setText(formatCurrency(row.igstAmount.doubleValue()));
        tvCess.setText(formatCurrency(row.cessAmount.doubleValue()));
        tvTotal.setText(formatCurrency(row.totalAmount.doubleValue()));

        if (row.igstAmount.compareTo(BigDecimal.ZERO) > 0) {
            rowIgst.setVisibility(View.VISIBLE);
        } else {
            rowIgst.setVisibility(View.GONE);
        }

        if (row.cessAmount.compareTo(BigDecimal.ZERO) > 0) {
            rowCess.setVisibility(View.VISIBLE);
        } else {
            rowCess.setVisibility(View.GONE);
        }

        // Populate items table
        tableItems.removeAllViews();
        // Item Header
        TableRow itemHeader = new TableRow(this);
        itemHeader.setBackgroundColor(android.graphics.Color.parseColor("#EEF2F6"));
        itemHeader.setPadding(0, dpToPx(4), 0, dpToPx(4));

        String[] itemColNames = {"Item", "Qty", "Price", "Tax %", "Total"};
        for (String cn : itemColNames) {
            TextView ctv = new TextView(this);
            ctv.setText(cn);
            ctv.setTypeface(null, Typeface.BOLD);
            ctv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10.5f);
            ctv.setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
            ctv.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
            ctv.setGravity("Item".equals(cn) ? Gravity.START : Gravity.END);
            itemHeader.addView(ctv);
        }
        tableItems.addView(itemHeader);

        for (PurchaseRegisterModels.ItemDetail item : row.items) {
            TableRow itr = new TableRow(this);
            itr.setPadding(0, dpToPx(4), 0, dpToPx(4));

            TextView tvName = new TextView(this);
            String uomStr = !TextUtils.isEmpty(item.unitOfMeasure) ? " (" + item.unitOfMeasure + ")" : "";
            tvName.setText(item.productName + uomStr);
            tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            tvName.setTextColor(ContextCompat.getColor(this, R.color.fg));
            tvName.setPadding(dpToPx(6), dpToPx(2), dpToPx(4), dpToPx(2));
            itr.addView(tvName);

            TextView tvQty = new TextView(this);
            tvQty.setText(CommonFunctions.formatQuantity(item.quantity));
            tvQty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            tvQty.setGravity(Gravity.END);
            tvQty.setTextColor(ContextCompat.getColor(this, R.color.fg));
            tvQty.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
            itr.addView(tvQty);

            TextView tvPrice = new TextView(this);
            tvPrice.setText("₹" + String.format(Locale.getDefault(), "%.2f", item.unitPrice));
            tvPrice.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            tvPrice.setGravity(Gravity.END);
            tvPrice.setTextColor(ContextCompat.getColor(this, R.color.fg));
            tvPrice.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
            itr.addView(tvPrice);

            TextView tvTaxRate = new TextView(this);
            tvTaxRate.setText(String.format(Locale.getDefault(), "%.0f%%", item.gstRate));
            tvTaxRate.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            tvTaxRate.setGravity(Gravity.CENTER);
            tvTaxRate.setTextColor(ContextCompat.getColor(this, R.color.material_blue));
            tvTaxRate.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
            itr.addView(tvTaxRate);

            TextView tvTotalAmt = new TextView(this);
            tvTotalAmt.setText(formatCurrency(item.lineTotal.doubleValue()));
            tvTotalAmt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f);
            tvTotalAmt.setTypeface(null, Typeface.BOLD);
            tvTotalAmt.setGravity(Gravity.END);
            tvTotalAmt.setTextColor(ContextCompat.getColor(this, R.color.fg));
            tvTotalAmt.setPadding(dpToPx(4), dpToPx(2), dpToPx(4), dpToPx(2));
            itr.addView(tvTotalAmt);

            tableItems.addView(itr);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(true).create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnViewBill.setOnClickListener(v -> {
            dialog.dismiss();
            openBillViewer(row);
        });

        dialog.show();
    }

    private void openBillViewer(PurchaseRegisterModels.PurchaseRegisterRow row) {
        Toast.makeText(this, "Generating purchase bill...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            Database db = Database.getInstance(PurchaseRegisterActivity.this);
            Purchase purchase = db.purchaseDao().getPurchaseById(row.purchaseId);
            if (purchase == null) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(PurchaseRegisterActivity.this, "Purchase order not found.", Toast.LENGTH_SHORT).show());
                return;
            }

            Suppliers supplier = db.supplierDao().getSupplierById(purchase.supplierId);
            List<PurchaseItemWithProduct> itemWraps = db.purchaseItemDao().getPurchaseItemsWithProductSync(purchase.purchaseId);

            List<CartItem> cartItems = new ArrayList<>();
            if (itemWraps != null) {
                for (PurchaseItemWithProduct piw : itemWraps) {
                    if (piw.purchaseItem == null) continue;
                    Product prod = piw.product;
                    if (prod == null) {
                        prod = new Product();
                        prod.productId = piw.purchaseItem.productId;
                        prod.productName = "Product #" + prod.productId;
                    }
                    CartItem ci = new CartItem(prod, piw.purchaseItem.quantity, piw.purchaseItem.unitPrice);
                    ci.discountValue = piw.purchaseItem.discountPercent;
                    ci.discountType = CartItem.DiscountType.PERCENT;
                    cartItems.add(ci);
                }
            }

            List<Config> configs = db.configDao().getAllConfigsSync();
            Map<String, String> configMap = new HashMap<>();
            if (configs != null) {
                for (Config c : configs) {
                    configMap.put(c.getConfigKey(), c.getConfigValue());
                }
            }

            final Suppliers finalSupplier = supplier;
            final List<CartItem> finalItems = cartItems;
            final Map<String, String> finalConfigs = configMap;

            new Handler(Looper.getMainLooper()).post(() -> {
                Bitmap billBitmap = CommonFunctions.createPurchaseBillBitmap(PurchaseRegisterActivity.this, purchase, finalSupplier, finalItems, finalConfigs);
                if (billBitmap != null) {
                    String fileName = "PO_" + purchase.invoiceId + "_" + System.currentTimeMillis();
                    BitmapHelper.saveBitmapToDownloads(billBitmap, PurchaseRegisterActivity.this, fileName);
                    File pngFile = BitmapHelper.saveBitmapAsPng(PurchaseRegisterActivity.this, billBitmap, fileName);
                    if (pngFile != null) {
                        String title = "Purchase Order #" + purchase.invoiceId;
                        BillViewerActivity.start(PurchaseRegisterActivity.this, pngFile.getAbsolutePath(), title, fileName);
                    }
                } else {
                    Toast.makeText(PurchaseRegisterActivity.this, "Failed to render bill image.", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showExportDialog() {
        if (currentReportData == null || currentReportData.rows.isEmpty()) {
            Toast.makeText(this, "No data available to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Excel (.xlsx) Spreadsheet", "PDF Document (.pdf)", "CSV Document (.csv)"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Export Purchase Register")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            exportExcel();
                            break;
                        case 1:
                            exportPdf();
                            break;
                        case 2:
                            exportCsv();
                            break;
                    }
                })
                .show();
    }

    private void exportExcel() {
        Toast.makeText(this, "Generating Excel...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File file = PurchaseRegisterExcelExporter.exportToExcel(PurchaseRegisterActivity.this, currentReportData);
                new Handler(Looper.getMainLooper()).post(() ->
                        showExportSuccess(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(PurchaseRegisterActivity.this, "Excel Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void exportPdf() {
        Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File file = PurchaseRegisterPdfExporter.exportToPdf(PurchaseRegisterActivity.this, currentReportData);
                new Handler(Looper.getMainLooper()).post(() ->
                        showExportSuccess(file, "application/pdf"));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(PurchaseRegisterActivity.this, "PDF Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void exportCsv() {
        Toast.makeText(this, "Generating CSV...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File file = PurchaseRegisterCsvExporter.exportToCsv(PurchaseRegisterActivity.this, currentReportData);
                new Handler(Looper.getMainLooper()).post(() ->
                        showExportSuccess(file, "text/csv"));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(PurchaseRegisterActivity.this, "CSV Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showExportSuccess(File file, String mimeType) {
        if (file == null || !file.exists() || isFinishing() || isDestroyed()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_export_success, null);
        TextView tvFileName = dialogView.findViewById(R.id.tv_dialog_file_name);
        TextView tvFileInfo = dialogView.findViewById(R.id.tv_dialog_file_info);
        ImageView ivFileIcon = dialogView.findViewById(R.id.iv_dialog_file_icon);
        View btnSave = dialogView.findViewById(R.id.btn_dialog_save);
        View btnShare = dialogView.findViewById(R.id.btn_dialog_share);
        View btnClose = dialogView.findViewById(R.id.btn_dialog_close);

        tvFileName.setText(file.getName());

        String typeLabel = "Document";
        if (mimeType != null) {
            if (mimeType.contains("pdf")) {
                typeLabel = "PDF Document";
                ivFileIcon.setColorFilter(ContextCompat.getColor(this, R.color.error_red));
            } else if (mimeType.contains("sheet") || mimeType.contains("excel")) {
                typeLabel = "Excel Workbook";
                ivFileIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_green));
            } else if (mimeType.contains("csv")) {
                typeLabel = "CSV Spreadsheet";
                ivFileIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_orange));
            }
        }
        tvFileInfo.setText(typeLabel + " • " + formatFileSize(file.length()));

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            dialog.dismiss();
            saveFileToDownloads(file, mimeType);
        });

        btnShare.setOnClickListener(v -> {
            dialog.dismiss();
            shareFile(file, mimeType);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void saveFileToDownloads(File file, String mimeType) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "File not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                pendingFileToSave = file;
                pendingMimeTypeToSave = mimeType;
                storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return;
            }
        }

        Toast.makeText(this, "Saving to Downloads...", Toast.LENGTH_SHORT).show();

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = false;
            String savedPath = "Download/Inventory Hub/Report/" + file.getName();
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentResolver resolver = getContentResolver();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Inventory Hub/Report");
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1);

                    Uri fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                    if (fileUri == null) {
                        throw new IOException("Failed to create MediaStore entry in Downloads.");
                    }

                    try {
                        try (InputStream in = new FileInputStream(file);
                             OutputStream out = resolver.openOutputStream(fileUri)) {
                            if (out == null) {
                                throw new IOException("Failed to open output stream for download URI.");
                            }
                            byte[] buffer = new byte[8192];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                out.write(buffer, 0, bytesRead);
                            }
                            out.flush();
                        }

                        ContentValues finishValues = new ContentValues();
                        finishValues.put(MediaStore.MediaColumns.IS_PENDING, 0);
                        resolver.update(fileUri, finishValues, null, null);
                        success = true;
                    } catch (Exception e) {
                        try {
                            resolver.delete(fileUri, null, null);
                        } catch (Exception ignored) {
                        }
                        throw e;
                    }
                } else {
                    File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    File targetDir = new File(downloadDir, "Inventory Hub" + File.separator + "Report");
                    if (!targetDir.exists() && !targetDir.mkdirs()) {
                        throw new IOException("Failed to create directory: " + targetDir.getAbsolutePath());
                    }
                    File destFile = new File(targetDir, file.getName());
                    try (InputStream in = new FileInputStream(file);
                         OutputStream out = new FileOutputStream(destFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        out.flush();
                    }
                    MediaScannerConnection.scanFile(
                            PurchaseRegisterActivity.this,
                            new String[]{destFile.getAbsolutePath()},
                            new String[]{mimeType},
                            null
                    );
                    savedPath = destFile.getAbsolutePath();
                    success = true;
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(PurchaseRegisterActivity.this, "Failed to save file: " + e.getMessage(), Toast.LENGTH_LONG).show());
                return;
            }

            if (success) {
                final String finalPath = savedPath;
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(PurchaseRegisterActivity.this, "File saved to " + finalPath, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void shareFile(File file, String mimeType) {
        try {
            Uri fileUri = FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    file
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Purchase Register"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private String formatCurrency(double amount) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return nf.format(amount);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_reports_purchase_register);
        }
    }
}
