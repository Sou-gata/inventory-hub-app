package in.gbtsolutions.inventoryhub.activities;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.View;
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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.reports.gstr1.GSTR1CsvExporter;
import in.gbtsolutions.inventoryhub.reports.gstr1.GSTR1ExcelExporter;
import in.gbtsolutions.inventoryhub.reports.gstr1.GSTR1Models;
import in.gbtsolutions.inventoryhub.reports.gstr1.GSTR1PdfExporter;
import in.gbtsolutions.inventoryhub.reports.gstr1.GSTR1ReportCalculator;

public class ReportsActivity extends BaseActivity {

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
    // Filter views
    private TextView tvStartDate;
    private TextView tvEndDate;
    private TextView chipThisMonth;
    private TextView chipLastMonth;
    private TextView chipThisQuarter;
    private TextView chipAllTime;
    private MaterialButton btnGenerate;
    private MaterialButton btnValidate;
    private MaterialButton btnExport;
    // Validation banner
    private LinearLayout layoutValidationBanner;
    private ImageView ivValidationIcon;
    private TextView tvValidationText;
    private TextView btnViewIssues;
    // KPI cards
    private TextView kpiTotalSales;
    private TextView kpiTaxableValue;
    private TextView kpiCgst;
    private TextView kpiSgst;
    private TextView kpiIgst;
    private TextView kpiTotalGst;
    private TextView kpiInvoicesCount;
    // Section Tabs
    private TextView tabSecB2b;
    private TextView tabSecB2c;
    private TextView tabSecExport;
    private TextView tabSecNotes;
    private TextView tabSecNil;
    private TextView tabSecHsn;
    private TextView tabSecTaxdoc;
    // Subtabs
    private LinearLayout layoutSubtabsContainer;
    private TextView subtabOne;
    private TextView subtabTwo;
    // Table Container
    private TextView tvActiveSectionTitle;
    private TextView tvActiveSectionDesc;
    private TableLayout tableContent;
    private TextView tvTableEmptyState;
    // State
    private String selectedStartDate; // "YYYY-MM-DD"
    private String selectedEndDate;   // "YYYY-MM-DD"
    private int activeSectionIndex = 1; // 1: B2B, 2: B2C, 3: Export, 4: Notes, 5: Nil, 6: HSN, 7: TaxDoc
    private int activeSubtabIndex = 1;   // 1 or 2
    private GSTR1Models.ReportData currentReportData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        GlobalStore.getInstance().loadSettings(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        View headerContainer = findViewById(R.id.header_container);
        View reportsScrollView = findViewById(R.id.reports_scroll_view);

        applyDrawerInsets(drawerLayout, headerContainer, reportsScrollView, navView);
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_reports_gstr1);

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

        btnGenerate = findViewById(R.id.btn_generate_report);
        btnValidate = findViewById(R.id.btn_validate_report);
        btnExport = findViewById(R.id.btn_export_report);

        layoutValidationBanner = findViewById(R.id.layout_validation_banner);
        ivValidationIcon = findViewById(R.id.iv_validation_icon);
        tvValidationText = findViewById(R.id.tv_validation_text);
        btnViewIssues = findViewById(R.id.btn_view_issues);

        kpiTotalSales = findViewById(R.id.kpi_total_sales);
        kpiTaxableValue = findViewById(R.id.kpi_taxable_value);
        kpiCgst = findViewById(R.id.kpi_cgst);
        kpiSgst = findViewById(R.id.kpi_sgst);
        kpiIgst = findViewById(R.id.kpi_igst);
        kpiTotalGst = findViewById(R.id.kpi_total_gst);
        kpiInvoicesCount = findViewById(R.id.kpi_invoices_count);

        tabSecB2b = findViewById(R.id.tab_sec_b2b);
        tabSecB2c = findViewById(R.id.tab_sec_b2c);
        tabSecExport = findViewById(R.id.tab_sec_export);
        tabSecNotes = findViewById(R.id.tab_sec_notes);
        tabSecNil = findViewById(R.id.tab_sec_nil);
        tabSecHsn = findViewById(R.id.tab_sec_hsn);
        tabSecTaxdoc = findViewById(R.id.tab_sec_taxdoc);

        layoutSubtabsContainer = findViewById(R.id.layout_subtabs_container);
        subtabOne = findViewById(R.id.subtab_one);
        subtabTwo = findViewById(R.id.subtab_two);

        tvActiveSectionTitle = findViewById(R.id.tv_active_section_title);
        tvActiveSectionDesc = findViewById(R.id.tv_active_section_desc);
        tableContent = findViewById(R.id.table_gstr1_content);
        tvTableEmptyState = findViewById(R.id.tv_table_empty_state);
    }

    private void initListeners() {
        findViewById(R.id.btn_pick_start_date).setOnClickListener(v -> showDatePicker(true));
        findViewById(R.id.btn_pick_end_date).setOnClickListener(v -> showDatePicker(false));

        chipThisMonth.setOnClickListener(v -> {
            highlightChip(chipThisMonth);
            setDefaultDateRangeThisMonth();
            loadReport();
        });

        chipLastMonth.setOnClickListener(v -> {
            highlightChip(chipLastMonth);
            setDateRangeLastMonth();
            loadReport();
        });

        chipThisQuarter.setOnClickListener(v -> {
            highlightChip(chipThisQuarter);
            setDateRangeThisQuarter();
            loadReport();
        });

        chipAllTime.setOnClickListener(v -> {
            highlightChip(chipAllTime);
            selectedStartDate = "2020-01-01";
            selectedEndDate = "2099-12-31";
            tvStartDate.setText("All Time");
            tvEndDate.setText("All Time");
            loadReport();
        });

        btnGenerate.setOnClickListener(v -> loadReport());
        btnValidate.setOnClickListener(v -> showValidationDialog());
        layoutValidationBanner.setOnClickListener(v -> showValidationDialog());
        btnExport.setOnClickListener(v -> showExportDialog());

        // Tabs
        tabSecB2b.setOnClickListener(v -> switchSection(1));
        tabSecB2c.setOnClickListener(v -> switchSection(2));
        tabSecExport.setOnClickListener(v -> switchSection(3));
        tabSecNotes.setOnClickListener(v -> switchSection(4));
        tabSecNil.setOnClickListener(v -> switchSection(5));
        tabSecHsn.setOnClickListener(v -> switchSection(6));
        tabSecTaxdoc.setOnClickListener(v -> switchSection(7));

        // Subtabs
        subtabOne.setOnClickListener(v -> switchSubtab(1));
        subtabTwo.setOnClickListener(v -> switchSubtab(2));
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
        int month = cal.get(Calendar.MONTH); // 0-based
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

    private void highlightChip(TextView activeChip) {
        TextView[] chips = {chipThisMonth, chipLastMonth, chipThisQuarter, chipAllTime};
        for (TextView c : chips) {
            if (c == activeChip) {
                c.setTextColor(ContextCompat.getColor(this, R.color.material_blue));
                c.setTypeface(null, Typeface.BOLD);
            } else {
                c.setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
                c.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void showDatePicker(boolean isStart) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker().setTitleText(isStart ? "Select Start Date" : "Select End Date").setSelection(MaterialDatePicker.todayInUtcMilliseconds()).build();

        picker.addOnPositiveButtonClickListener(selection -> {
            if (selection != null) {
                Date date = new Date(selection);
                if (isStart) {
                    selectedStartDate = DB_DATE_FORMAT.format(date);
                    tvStartDate.setText(DISPLAY_DATE_FORMAT.format(date));
                } else {
                    selectedEndDate = DB_DATE_FORMAT.format(date);
                    tvEndDate.setText(DISPLAY_DATE_FORMAT.format(date));
                }
                loadReport();
            }
        });

        picker.show(getSupportFragmentManager(), isStart ? "PICK_START_DATE" : "PICK_END_DATE");
    }

    private void loadReport() {
        btnGenerate.setEnabled(false);
        btnGenerate.setText("Calculating...");

        Executors.newSingleThreadExecutor().execute(() -> {
            GSTR1Models.ReportData report = GSTR1ReportCalculator.generateReport(ReportsActivity.this, selectedStartDate, selectedEndDate, 100000.0 // B2C Large Threshold
            );

            new Handler(Looper.getMainLooper()).post(() -> {
                btnGenerate.setEnabled(true);
                btnGenerate.setText("Generate");
                currentReportData = report;
                renderReport();
            });
        });
    }

    private void renderReport() {
        if (currentReportData == null) return;

        // 1. KPI Cards
        kpiTotalSales.setText(String.format(Locale.getDefault(), "₹ %,.2f", currentReportData.kpi.totalSales.doubleValue()));
        kpiTaxableValue.setText(String.format(Locale.getDefault(), "₹ %,.2f", currentReportData.kpi.taxableValue.doubleValue()));
        kpiCgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", currentReportData.kpi.cgst.doubleValue()));
        kpiSgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", currentReportData.kpi.sgst.doubleValue()));
        kpiIgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", currentReportData.kpi.igst.doubleValue()));
        kpiTotalGst.setText(String.format(Locale.getDefault(), "₹ %,.2f", currentReportData.kpi.totalGst.doubleValue()));
        kpiInvoicesCount.setText(String.valueOf(currentReportData.kpi.totalInvoices));

        // 2. Validation & Reconciliation Banner
        layoutValidationBanner.setVisibility(View.VISIBLE);
        int errors = currentReportData.getErrorCount();
        int warnings = currentReportData.getWarningCount();

        if (errors > 0) {
            ivValidationIcon.setImageResource(R.drawable.ic_close);
            ivValidationIcon.setColorFilter(ContextCompat.getColor(this, R.color.error_red));
            tvValidationText.setText(errors + " GST Error(s) found! (Click to review)");
            tvValidationText.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        } else if (warnings > 0 || !currentReportData.reconciliation.isMatched) {
            ivValidationIcon.setImageResource(R.drawable.ic_nav_reports);
            ivValidationIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_orange));
            String msg = warnings + " Warning(s)";
            if (!currentReportData.reconciliation.isMatched) {
                msg += " • HSN Mismatch ₹" + currentReportData.reconciliation.difference.toPlainString();
            }
            tvValidationText.setText(msg);
            tvValidationText.setTextColor(ContextCompat.getColor(this, R.color.status_orange));
        } else {
            ivValidationIcon.setImageResource(R.drawable.ic_nav_reports);
            ivValidationIcon.setColorFilter(ContextCompat.getColor(this, R.color.status_green));
            tvValidationText.setText("All GST data validated & reconciled ✓");
            tvValidationText.setTextColor(ContextCompat.getColor(this, R.color.status_green));
        }

        // 3. Render Table for active section
        renderActiveSectionTable();
    }

    private void switchSection(int sectionIndex) {
        activeSectionIndex = sectionIndex;
        activeSubtabIndex = 1;

        TextView[] tabs = {tabSecB2b, tabSecB2c, tabSecExport, tabSecNotes, tabSecNil, tabSecHsn, tabSecTaxdoc};
        for (int i = 0; i < tabs.length; i++) {
            if (i + 1 == sectionIndex) {
                tabs[i].setBackgroundResource(R.drawable.bg_card);
                tabs[i].setTextColor(ContextCompat.getColor(this, R.color.material_blue));
                tabs[i].setTypeface(null, Typeface.BOLD);
            } else {
                tabs[i].setBackgroundColor(Color.TRANSPARENT);
                tabs[i].setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
                tabs[i].setTypeface(null, Typeface.NORMAL);
            }
        }

        // Setup subtabs if needed
        if (sectionIndex == 2) { // B2C
            layoutSubtabsContainer.setVisibility(View.VISIBLE);
            subtabOne.setText("B2C Others");
            subtabTwo.setText("B2C Large (> ₹1L)");
            updateSubtabUI();
        } else if (sectionIndex == 4) { // Notes
            layoutSubtabsContainer.setVisibility(View.VISIBLE);
            subtabOne.setText("Credit Notes");
            subtabTwo.setText("Debit Notes");
            updateSubtabUI();
        } else if (sectionIndex == 6) { // HSN
            layoutSubtabsContainer.setVisibility(View.VISIBLE);
            subtabOne.setText("B2B HSN");
            subtabTwo.setText("B2C HSN");
            updateSubtabUI();
        } else {
            layoutSubtabsContainer.setVisibility(View.GONE);
        }

        renderActiveSectionTable();
    }

    private void switchSubtab(int subtabIndex) {
        activeSubtabIndex = subtabIndex;
        updateSubtabUI();
        renderActiveSectionTable();
    }

    private void updateSubtabUI() {
        if (activeSubtabIndex == 1) {
            subtabOne.setBackgroundResource(R.drawable.bg_card);
            subtabOne.setTextColor(ContextCompat.getColor(this, R.color.material_blue));
            subtabOne.setTypeface(null, Typeface.BOLD);

            subtabTwo.setBackgroundColor(Color.TRANSPARENT);
            subtabTwo.setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
            subtabTwo.setTypeface(null, Typeface.NORMAL);
        } else {
            subtabTwo.setBackgroundResource(R.drawable.bg_card);
            subtabTwo.setTextColor(ContextCompat.getColor(this, R.color.material_blue));
            subtabTwo.setTypeface(null, Typeface.BOLD);

            subtabOne.setBackgroundColor(Color.TRANSPARENT);
            subtabOne.setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
            subtabOne.setTypeface(null, Typeface.NORMAL);
        }
    }

    private void renderActiveSectionTable() {
        if (currentReportData == null) return;
        tableContent.removeAllViews();

        switch (activeSectionIndex) {
            case 1: // B2B
                tvActiveSectionTitle.setText("1. B2B Taxable Outward Supplies");
                tvActiveSectionDesc.setText("Supplies made to registered buyers (grouped by GSTIN + POS + Rate)");
                renderB2BTable();
                break;
            case 2: // B2C
                if (activeSubtabIndex == 1) {
                    tvActiveSectionTitle.setText("2. B2C Others (Table 7)");
                    tvActiveSectionDesc.setText("Intra-state supplies and inter-state supplies <= ₹1 Lakh");
                    renderB2CTable(currentReportData.b2cOthersList);
                } else {
                    tvActiveSectionTitle.setText("2. B2C Large (Table 5)");
                    tvActiveSectionDesc.setText("Inter-state supplies to unregistered persons with invoice value > ₹1 Lakh");
                    renderB2CTable(currentReportData.b2cLargeList);
                }
                break;
            case 3: // Exports
                tvActiveSectionTitle.setText("3. Exports & SEZ Supplies (Table 6A / 6B)");
                tvActiveSectionDesc.setText("Export of goods/services and supplies to SEZ developers/units");
                renderExportTable();
                break;
            case 4: // Notes
                if (activeSubtabIndex == 1) {
                    tvActiveSectionTitle.setText("4. Credit Notes (Table 9B)");
                    tvActiveSectionDesc.setText("Credit notes issued for sales returns and discount adjustments");
                    renderNotesTable(currentReportData.creditNotesList);
                } else {
                    tvActiveSectionTitle.setText("4. Debit Notes (Table 9B)");
                    tvActiveSectionDesc.setText("Debit notes issued for supplementary charges and price revisions");
                    renderNotesTable(currentReportData.debitNotesList);
                }
                break;
            case 5: // Nil/Exempt
                tvActiveSectionTitle.setText("5. Nil Rated, Exempted & Non-GST Outward Supplies (Table 8)");
                tvActiveSectionDesc.setText("Non-taxable goods, exempted turnover, and zero-tax supplies");
                renderNilTable();
                break;
            case 6: // HSN/SAC
                if (activeSubtabIndex == 1) {
                    tvActiveSectionTitle.setText("6. HSN/SAC B2B Summary (Table 12)");
                    tvActiveSectionDesc.setText("HSN rate-wise summary of items supplied to registered businesses");
                    renderHsnTable(currentReportData.hsnB2bList);
                } else {
                    tvActiveSectionTitle.setText("6. HSN/SAC B2C Summary (Table 12)");
                    tvActiveSectionDesc.setText("HSN rate-wise summary of items supplied to consumers / walk-in buyers");
                    renderHsnTable(currentReportData.hsnB2cList);
                }
                break;
            case 7: // Tax & Doc Summary
                tvActiveSectionTitle.setText("7. Tax & Document Summary (Table 13)");
                tvActiveSectionDesc.setText("Overall GST liability breakdown and document serial count summary");
                renderTaxDocTable();
                break;
        }
    }

    private void renderB2BTable() {
        if (currentReportData.b2bList.isEmpty()) {
            tvTableEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tvTableEmptyState.setVisibility(View.GONE);

        String[] headers = {"Customer GSTIN", "Customer Name", "Place of Supply", "Rate", "Invoices", "Taxable Value", "CGST", "SGST", "IGST", "CESS", "Total Tax", "Invoice Value"};
        addTableHeaderRow(headers);

        for (GSTR1Models.B2BRow row : currentReportData.b2bList) {
            String[] cells = {row.customerGstin, row.customerName, row.placeOfSupply, row.taxRate + "%", String.valueOf(row.invoiceCount), "₹ " + row.taxableValue.toPlainString(), "₹ " + row.cgst.toPlainString(), "₹ " + row.sgst.toPlainString(), "₹ " + row.igst.toPlainString(), "₹ " + row.cess.toPlainString(), "₹ " + row.totalTax.toPlainString(), "₹ " + row.invoiceValue.toPlainString()};
            addTableRow(cells);
        }
    }

    private void renderB2CTable(List<GSTR1Models.B2CRow> list) {
        if (list.isEmpty()) {
            tvTableEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tvTableEmptyState.setVisibility(View.GONE);

        String[] headers = {"Supply Category", "Place of Supply", "Rate", "Invoices", "Taxable Value", "CGST", "SGST", "IGST", "CESS", "Total Tax", "Invoice Value"};
        addTableHeaderRow(headers);

        for (GSTR1Models.B2CRow row : list) {
            String[] cells = {row.supplyCategory, row.placeOfSupply, row.taxRate + "%", String.valueOf(row.invoiceCount), "₹ " + row.taxableValue.toPlainString(), "₹ " + row.cgst.toPlainString(), "₹ " + row.sgst.toPlainString(), "₹ " + row.igst.toPlainString(), "₹ " + row.cess.toPlainString(), "₹ " + row.totalTax.toPlainString(), "₹ " + row.invoiceValue.toPlainString()};
            addTableRow(cells);
        }
    }

    private void renderExportTable() {
        if (currentReportData.exportList.isEmpty()) {
            tvTableEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tvTableEmptyState.setVisibility(View.GONE);

        String[] headers = {"Category", "Invoices", "Taxable Value", "Invoice Value", "IGST", "CGST", "SGST", "CESS", "Shipping Bill", "Port Code"};
        addTableHeaderRow(headers);

        for (GSTR1Models.ExportRow row : currentReportData.exportList) {
            String[] cells = {row.category, String.valueOf(row.invoiceCount), "₹ " + row.taxableValue.toPlainString(), "₹ " + row.invoiceValue.toPlainString(), "₹ " + row.igst.toPlainString(), "₹ " + row.cgst.toPlainString(), "₹ " + row.sgst.toPlainString(), "₹ " + row.cess.toPlainString(), row.shippingBillNo != null ? row.shippingBillNo : "N/A", row.portCode != null ? row.portCode : "N/A"};
            addTableRow(cells);
        }
    }

    private void renderNotesTable(List<GSTR1Models.CreditDebitNoteRow> list) {
        if (list.isEmpty()) {
            tvTableEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tvTableEmptyState.setVisibility(View.GONE);

        String[] headers = {"Note Type", "Recipient", "Rate", "Count", "Taxable Value", "CGST", "SGST", "IGST", "CESS", "Total Tax", "Total Value"};
        addTableHeaderRow(headers);

        for (GSTR1Models.CreditDebitNoteRow row : list) {
            String[] cells = {row.noteType, row.recipientType, row.taxRate + "%", String.valueOf(row.noteCount), "₹ " + row.taxableValue.toPlainString(), "₹ " + row.cgst.toPlainString(), "₹ " + row.sgst.toPlainString(), "₹ " + row.igst.toPlainString(), "₹ " + row.cess.toPlainString(), "₹ " + row.totalTax.toPlainString(), "₹ " + row.totalValue.toPlainString()};
            addTableRow(cells);
        }
    }

    private void renderNilTable() {
        if (currentReportData.nilExemptList.isEmpty()) {
            tvTableEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tvTableEmptyState.setVisibility(View.GONE);

        String[] headers = {"Category", "Supply Type", "Recipient", "Invoice Count", "Reported Value"};
        addTableHeaderRow(headers);

        for (GSTR1Models.NilExemptRow row : currentReportData.nilExemptList) {
            String[] cells = {row.category, row.supplyType, row.recipientType, String.valueOf(row.invoiceCount), "₹ " + row.reportedValue.toPlainString()};
            addTableRow(cells);
        }
    }

    private void renderHsnTable(List<GSTR1Models.HsnRow> list) {
        if (list.isEmpty()) {
            tvTableEmptyState.setVisibility(View.VISIBLE);
            return;
        }
        tvTableEmptyState.setVisibility(View.GONE);

        String[] headers = {"HSN/SAC", "Description", "UQC", "Quantity", "Rate", "Taxable Value", "CGST", "SGST", "IGST", "CESS", "Total Tax", "Total Value"};
        addTableHeaderRow(headers);

        for (GSTR1Models.HsnRow row : list) {
            String[] cells = {row.hsnCode, row.description, row.uqc, String.valueOf(row.quantity), row.taxRate + "%", "₹ " + row.taxableValue.toPlainString(), "₹ " + row.cgst.toPlainString(), "₹ " + row.sgst.toPlainString(), "₹ " + row.igst.toPlainString(), "₹ " + row.cess.toPlainString(), "₹ " + row.totalTax.toPlainString(), "₹ " + row.totalValue.toPlainString()};
            addTableRow(cells);
        }
    }

    private void renderTaxDocTable() {
        tvTableEmptyState.setVisibility(View.GONE);

        String[] headers = {"Summary Item", "Amount / Count"};
        addTableHeaderRow(headers);

        addTableRow(new String[]{"Gross Invoice Value", "₹ " + currentReportData.taxDocSummary.grossInvoiceValue.toPlainString()});
        addTableRow(new String[]{"Taxable Value", "₹ " + currentReportData.taxDocSummary.taxableValue.toPlainString()});
        addTableRow(new String[]{"Central Tax (CGST)", "₹ " + currentReportData.taxDocSummary.cgst.toPlainString()});
        addTableRow(new String[]{"State Tax (SGST)", "₹ " + currentReportData.taxDocSummary.sgst.toPlainString()});
        addTableRow(new String[]{"Integrated Tax (IGST)", "₹ " + currentReportData.taxDocSummary.igst.toPlainString()});
        addTableRow(new String[]{"Cess", "₹ " + currentReportData.taxDocSummary.cess.toPlainString()});
        addTableRow(new String[]{"Total GST Output Liability", "₹ " + currentReportData.taxDocSummary.totalGst.toPlainString()});
        addTableRow(new String[]{"Active Invoices Issued", String.valueOf(currentReportData.taxDocSummary.taxInvoicesCount)});
        addTableRow(new String[]{"Cancelled Invoices", String.valueOf(currentReportData.taxDocSummary.cancelledInvoicesCount)});
        addTableRow(new String[]{"Credit Notes Issued", String.valueOf(currentReportData.taxDocSummary.creditNotesCount)});
        addTableRow(new String[]{"Debit Notes Issued", String.valueOf(currentReportData.taxDocSummary.debitNotesCount)});
        addTableRow(new String[]{"Net Documents (Tax Invoices + Debit - Credit - Cancelled)", String.valueOf(currentReportData.taxDocSummary.netDocumentsCount)});
    }

    private void addTableHeaderRow(String[] headers) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(ContextCompat.getColor(this, R.color.material_blue));
        row.setPadding(8, 10, 8, 10);

        for (String h : headers) {
            TextView tv = new TextView(this);
            tv.setText(h);
            tv.setTextColor(Color.WHITE);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextSize(11);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setPadding(12, 4, 12, 4);
            row.addView(tv);
        }
        tableContent.addView(row);
    }

    private void addTableRow(String[] cells) {
        TableRow row = new TableRow(this);
        row.setPadding(8, 8, 8, 8);
        int childCount = tableContent.getChildCount();
        if (childCount % 2 == 0) {
            row.setBackgroundColor(Color.parseColor("#F8F9FD"));
        } else {
            row.setBackgroundColor(Color.WHITE);
        }

        for (String c : cells) {
            TextView tv = new TextView(this);
            tv.setText(c);
            tv.setTextColor(ContextCompat.getColor(this, R.color.fg));
            tv.setTextSize(11);
            tv.setGravity(Gravity.CENTER_VERTICAL);
            tv.setPadding(12, 4, 12, 4);
            row.addView(tv);
        }
        tableContent.addView(row);
    }

    private void showValidationDialog() {
        if (currentReportData == null) return;

        StringBuilder sb = new StringBuilder();

        // Reconciliation
        sb.append("--- RECONCILIATION ---\n");
        sb.append("Supplies Taxable: ₹ ").append(currentReportData.reconciliation.expectedTaxable.toPlainString()).append("\n");
        sb.append("HSN Summary Taxable: ₹ ").append(currentReportData.reconciliation.hsnTaxable.toPlainString()).append("\n");
        if (currentReportData.reconciliation.isMatched) {
            sb.append("Status: RECONCILED ✓\n\n");
        } else {
            sb.append("Status: ⚠ MISMATCH (Diff: ₹ ").append(currentReportData.reconciliation.difference.toPlainString()).append(")\n\n");
        }

        // Issues
        sb.append("--- VALIDATION ISSUES (").append(currentReportData.validationIssues.size()).append(") ---\n");
        if (currentReportData.validationIssues.isEmpty()) {
            sb.append("No errors or warnings found. Report is export-ready!");
        } else {
            for (GSTR1Models.ValidationIssue issue : currentReportData.validationIssues) {
                sb.append("[").append(issue.severity.name()).append("] ").append(issue.invoiceOrRef).append(": ").append(issue.title).append("\n").append("   ").append(issue.description).append("\n\n");
            }
        }

        new MaterialAlertDialogBuilder(this).setTitle("GSTR-1 Validation & Audit").setMessage(sb.toString()).setPositiveButton("OK", (dialog, which) -> dialog.dismiss()).show();
    }

    private void showExportDialog() {
        if (currentReportData == null) {
            Toast.makeText(this, "Please generate the report first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"PDF Document (Management Report)", "Excel Workbook (.xlsx - 8 Sheets)", "CSV (Full Summary)"};
        new MaterialAlertDialogBuilder(this).setTitle("Export GSTR-1 Report").setItems(options, (dialog, which) -> {
            switch (which) {
                case 0:
                    exportPdf();
                    break;
                case 1:
                    exportExcel();
                    break;
                case 2:
                    exportCsv();
                    break;
            }
        }).show();
    }

    private void exportPdf() {
        Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File file = GSTR1PdfExporter.exportToPdf(ReportsActivity.this, currentReportData);
                new Handler(Looper.getMainLooper()).post(() -> showExportSuccess(file, "application/pdf"));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(ReportsActivity.this, "PDF Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void exportExcel() {
        Toast.makeText(this, "Generating Excel...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File file = GSTR1ExcelExporter.exportToExcel(ReportsActivity.this, currentReportData);
                new Handler(Looper.getMainLooper()).post(() -> showExportSuccess(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(ReportsActivity.this, "Excel Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void exportCsv() {
        Toast.makeText(this, "Generating CSV...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File file = GSTR1CsvExporter.exportToCsv(ReportsActivity.this, currentReportData);
                new Handler(Looper.getMainLooper()).post(() -> showExportSuccess(file, "text/csv"));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(ReportsActivity.this, "CSV Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
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

        AlertDialog dialog = new MaterialAlertDialogBuilder(this).setView(dialogView).setCancelable(true).create();

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

        // On Android 9 and below, check WRITE_EXTERNAL_STORAGE permission
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
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
                        try (InputStream in = new FileInputStream(file); OutputStream out = resolver.openOutputStream(fileUri)) {
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
                    try (InputStream in = new FileInputStream(file); OutputStream out = new FileOutputStream(destFile)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                        out.flush();
                    }
                    MediaScannerConnection.scanFile(ReportsActivity.this, new String[]{destFile.getAbsolutePath()}, new String[]{mimeType}, null);
                    success = true;
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(ReportsActivity.this, "Failed to save file: " + e.getMessage(), Toast.LENGTH_LONG).show());
                return;
            }

            if (success) {
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(ReportsActivity.this, "File saved to " + savedPath, Toast.LENGTH_LONG).show());
            }
        });
    }

    private void shareFile(File file, String mimeType) {
        try {
            Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", file);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "GSTR-1 Summary Report (" + selectedStartDate + " to " + selectedEndDate + ")");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(shareIntent, "Share GSTR-1 Report via"));
        } catch (Exception e) {
            Toast.makeText(this, "Error sharing file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        GlobalStore.getInstance().loadSettings(this);
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_reports_gstr1);
        }
    }
}
