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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.UserHelper;
import in.gbtsolutions.inventoryhub.models.User;
import in.gbtsolutions.inventoryhub.reports.userreport.UserWiseReportCalculator;
import in.gbtsolutions.inventoryhub.reports.userreport.UserWiseReportCsvExporter;
import in.gbtsolutions.inventoryhub.reports.userreport.UserWiseReportExcelExporter;
import in.gbtsolutions.inventoryhub.reports.userreport.UserWiseReportModels;
import in.gbtsolutions.inventoryhub.reports.userreport.UserWiseReportPdfExporter;

public class UserWiseReportActivity extends BaseActivity {

    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    static {
        DB_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        DISPLAY_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private File pendingFileToSave;
    private String pendingMimeTypeToSave;

    private final ActivityResultLauncher<String> storagePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> {
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
    private TextView chipToday;
    private TextView chipThisMonth;
    private TextView chipLastMonth;
    private TextView chipThisQuarter;
    private TextView chipAllTime;

    private View btnSelectUser;
    private TextView tvSelectedUserName;
    private ImageView btnClearUserFilter;

    private EditText inputSearch;
    private ImageView btnClearSearch;

    private TextView chipFilterAllRoles;
    private TextView chipFilterAdmin;
    private TextView chipFilterStaff;
    private TextView chipFilterActiveOnly;

    private MaterialButton btnGenerate;
    private MaterialButton btnExport;

    // KPI Cards
    private TextView kpiGrossSales;
    private TextView kpiSalesCount;
    private TextView kpiGrossPurchases;
    private TextView kpiPurchaseCount;
    private TextView kpiSalesGst;
    private TextView kpiPurchaseGst;
    private TextView kpiTotalGst;
    private TextView kpiNetTurnover;

    // Table
    private TableLayout tableUserReport;
    private TextView tvTableRecordsCount;
    private LinearLayout layoutEmptyState;
    private TextView tvEmptyMessage;

    // State
    private String selectedStartDate; // "yyyy-MM-dd"
    private String selectedEndDate;   // "yyyy-MM-dd"
    private long selectedUserId = -1;  // -1 = All users
    private String selectedUserDisplayName = "All Users (Staff & Admins)";
    private int currentRoleFilter = 0; // 0: All Roles, 1: Admin, 2: Staff, 3: With Transactions Only
    private String currentSearchQuery = "";
    private UserWiseReportModels.ReportData currentReportData = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_wise_report);

        GlobalStore.getInstance().loadSettings(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        View headerContainer = findViewById(R.id.header_container);
        View scrollView = findViewById(R.id.user_wise_report_scroll_view);

        applyDrawerInsets(drawerLayout, headerContainer, scrollView, navView);
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_reports_user_summary);

        initViews();
        initListeners();
        setDefaultDateRangeThisMonth();
        loadReport();
    }

    private void initViews() {
        tvStartDate = findViewById(R.id.tv_start_date);
        tvEndDate = findViewById(R.id.tv_end_date);
        chipToday = findViewById(R.id.chip_today);
        chipThisMonth = findViewById(R.id.chip_this_month);
        chipLastMonth = findViewById(R.id.chip_last_month);
        chipThisQuarter = findViewById(R.id.chip_this_quarter);
        chipAllTime = findViewById(R.id.chip_all_time);

        btnSelectUser = findViewById(R.id.btn_select_user);
        tvSelectedUserName = findViewById(R.id.tv_selected_user_name);
        btnClearUserFilter = findViewById(R.id.btn_clear_user_filter);

        inputSearch = findViewById(R.id.input_search);
        btnClearSearch = findViewById(R.id.btn_clear_search);

        chipFilterAllRoles = findViewById(R.id.chip_filter_all_roles);
        chipFilterAdmin = findViewById(R.id.chip_filter_admin);
        chipFilterStaff = findViewById(R.id.chip_filter_staff);
        chipFilterActiveOnly = findViewById(R.id.chip_filter_active_only);

        btnGenerate = findViewById(R.id.btn_generate_report);
        btnExport = findViewById(R.id.btn_export_report);

        kpiGrossSales = findViewById(R.id.kpi_gross_sales);
        kpiSalesCount = findViewById(R.id.kpi_sales_count);
        kpiGrossPurchases = findViewById(R.id.kpi_gross_purchases);
        kpiPurchaseCount = findViewById(R.id.kpi_purchase_count);
        kpiSalesGst = findViewById(R.id.kpi_sales_gst);
        kpiPurchaseGst = findViewById(R.id.kpi_purchase_gst);
        kpiTotalGst = findViewById(R.id.kpi_total_gst);
        kpiNetTurnover = findViewById(R.id.kpi_net_turnover);

        tableUserReport = findViewById(R.id.table_user_report);
        tvTableRecordsCount = findViewById(R.id.tv_table_records_count);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
    }

    private void initListeners() {
        findViewById(R.id.btn_pick_start_date).setOnClickListener(v -> showDatePicker(true));
        findViewById(R.id.btn_pick_end_date).setOnClickListener(v -> showDatePicker(false));

        chipToday.setOnClickListener(v -> {
            highlightPresetChip(chipToday);
            setDateRangeToday();
            loadReport();
        });

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

        btnSelectUser.setOnClickListener(v -> showUserSelectionDialog());

        btnClearUserFilter.setOnClickListener(v -> {
            selectedUserId = -1;
            selectedUserDisplayName = "All Users (Staff & Admins)";
            tvSelectedUserName.setText(selectedUserDisplayName);
            btnClearUserFilter.setVisibility(View.GONE);
            loadReport();
        });

        btnGenerate.setOnClickListener(v -> loadReport());
        btnExport.setOnClickListener(v -> showExportDialog());

        // Real-Time Search Filter
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

        // Role Filter Chips
        chipFilterAllRoles.setOnClickListener(v -> {
            currentRoleFilter = 0;
            updateRoleChips();
            renderFilteredTable();
        });

        chipFilterAdmin.setOnClickListener(v -> {
            currentRoleFilter = 1;
            updateRoleChips();
            renderFilteredTable();
        });

        chipFilterStaff.setOnClickListener(v -> {
            currentRoleFilter = 2;
            updateRoleChips();
            renderFilteredTable();
        });

        chipFilterActiveOnly.setOnClickListener(v -> {
            currentRoleFilter = 3;
            updateRoleChips();
            renderFilteredTable();
        });
    }

    private void updateRoleChips() {
        chipFilterAllRoles.setBackgroundResource(currentRoleFilter == 0 ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        chipFilterAllRoles.setTextColor(ContextCompat.getColor(this, currentRoleFilter == 0 ? R.color.white : R.color.fg_muted));

        chipFilterAdmin.setBackgroundResource(currentRoleFilter == 1 ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        chipFilterAdmin.setTextColor(ContextCompat.getColor(this, currentRoleFilter == 1 ? R.color.white : R.color.fg_muted));

        chipFilterStaff.setBackgroundResource(currentRoleFilter == 2 ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        chipFilterStaff.setTextColor(ContextCompat.getColor(this, currentRoleFilter == 2 ? R.color.white : R.color.fg_muted));

        chipFilterActiveOnly.setBackgroundResource(currentRoleFilter == 3 ? R.drawable.bg_tab_active : R.drawable.bg_tab_inactive);
        chipFilterActiveOnly.setTextColor(ContextCompat.getColor(this, currentRoleFilter == 3 ? R.color.white : R.color.fg_muted));
    }

    private void showUserSelectionDialog() {
        UserHelper.getAllUsersAsync(this, users -> {
            if (isFinishing() || isDestroyed()) return;

            int count = (users != null ? users.size() : 0) + 1; // +1 for "All Users"
            String[] items = new String[count];
            items[0] = "All Users (All Staff & Admins)";

            int checkedIndex = 0;
            if (users != null) {
                for (int i = 0; i < users.size(); i++) {
                    User u = users.get(i);
                    items[i + 1] = UserHelper.formatUserNameAndUsername(u, u.id) + " [" + UserHelper.formatUserRole(u) + "]";
                    if (selectedUserId == u.id) {
                        checkedIndex = i + 1;
                    }
                }
            }

            new MaterialAlertDialogBuilder(this)
                    .setTitle("Select Target User")
                    .setSingleChoiceItems(items, checkedIndex, (dialog, which) -> {
                        dialog.dismiss();
                        if (which == 0) {
                            selectedUserId = -1;
                            selectedUserDisplayName = "All Users (Staff & Admins)";
                            btnClearUserFilter.setVisibility(View.GONE);
                        } else if (users != null && which - 1 < users.size()) {
                            User chosen = users.get(which - 1);
                            selectedUserId = chosen.id;
                            selectedUserDisplayName = UserHelper.formatUserNameAndUsername(chosen, chosen.id);
                            btnClearUserFilter.setVisibility(View.VISIBLE);
                        }
                        tvSelectedUserName.setText(selectedUserDisplayName);
                        loadReport();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
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

    private void showDatePicker(boolean isStartDate) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isStartDate ? "Select Start Date" : "Select End Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            Date date = new Date(selection);
            String dbDate = DB_DATE_FORMAT.format(date);
            String displayDate = DISPLAY_DATE_FORMAT.format(date);

            if (isStartDate) {
                selectedStartDate = dbDate;
                tvStartDate.setText(displayDate);
            } else {
                selectedEndDate = dbDate;
                tvEndDate.setText(displayDate);
            }
            clearPresetChipHighlight();
            loadReport();
        });

        picker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private void clearPresetChipHighlight() {
        TextView[] chips = {chipThisMonth, chipLastMonth, chipThisQuarter, chipAllTime, chipToday};
        for (TextView c : chips) {
            c.setBackgroundResource(R.drawable.bg_tab_inactive);
            c.setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
        }
    }

    private void loadReport() {
        btnGenerate.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            UserWiseReportModels.ReportData data = UserWiseReportCalculator.generateReport(
                    UserWiseReportActivity.this,
                    selectedStartDate,
                    selectedEndDate,
                    selectedUserId
            );

            new Handler(Looper.getMainLooper()).post(() -> {
                btnGenerate.setEnabled(true);
                currentReportData = data;
                updateKpiCards(data.summary);
                renderFilteredTable();
            });
        });
    }

    private void updateKpiCards(UserWiseReportModels.UserWiseReportSummary summary) {
        if (summary == null) return;

        kpiGrossSales.setText("₹ " + summary.totalGrossSales.toPlainString());
        kpiSalesCount.setText(summary.totalSalesCount + " invoices");

        kpiGrossPurchases.setText("₹ " + summary.totalGrossPurchases.toPlainString());
        kpiPurchaseCount.setText(summary.totalPurchaseCount + " purchase orders");

        kpiSalesGst.setText("₹ " + summary.totalSalesGst.toPlainString());
        kpiPurchaseGst.setText("₹ " + summary.totalPurchaseGst.toPlainString());

        kpiTotalGst.setText("₹ " + summary.totalCombinedGst.toPlainString());

        kpiNetTurnover.setText("₹ " + summary.netTurnover.toPlainString());
        if (summary.netTurnover.compareTo(BigDecimal.ZERO) >= 0) {
            kpiNetTurnover.setTextColor(ContextCompat.getColor(this, R.color.status_green));
        } else {
            kpiNetTurnover.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        }
    }

    private void renderFilteredTable() {
        if (tableUserReport == null) return;
        tableUserReport.removeAllViews();

        if (currentReportData == null || currentReportData.rows == null || currentReportData.rows.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("No user transaction data found for the selected period.");
            tvTableRecordsCount.setText("0 users");
            return;
        }

        List<UserWiseReportModels.UserWiseReportRow> filtered = new ArrayList<>();
        for (UserWiseReportModels.UserWiseReportRow row : currentReportData.rows) {
            // 1. Role filter
            if (currentRoleFilter == 1 && !"ADMIN".equalsIgnoreCase(row.role)) {
                continue;
            } else if (currentRoleFilter == 2 && !"STAFF".equalsIgnoreCase(row.role)) {
                continue;
            } else if (currentRoleFilter == 3 && row.totalTransactions <= 0) {
                continue;
            }

            // 2. Search query filter
            if (!TextUtils.isEmpty(currentSearchQuery)) {
                boolean matches = false;
                if (row.userName != null && row.userName.toLowerCase(Locale.getDefault()).contains(currentSearchQuery)) {
                    matches = true;
                } else if (row.username != null && row.username.toLowerCase(Locale.getDefault()).contains(currentSearchQuery)) {
                    matches = true;
                } else if (row.role != null && row.role.toLowerCase(Locale.getDefault()).contains(currentSearchQuery)) {
                    matches = true;
                } else if (row.contact != null && row.contact.toLowerCase(Locale.getDefault()).contains(currentSearchQuery)) {
                    matches = true;
                }
                if (!matches) continue;
            }

            filtered.add(row);
        }

        tvTableRecordsCount.setText(filtered.size() + " user" + (filtered.size() != 1 ? "s" : ""));

        if (filtered.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            tvEmptyMessage.setText("No users match the search filter \"" + currentSearchQuery + "\"");
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);

        // Build Table Header
        TableRow headerRow = createTableHeaderRow();
        tableUserReport.addView(headerRow);
        tableUserReport.addView(createDividerRow());

        int slNo = 1;
        int sumSalesCount = 0;
        BigDecimal sumSalesGross = BigDecimal.ZERO;
        BigDecimal sumSalesGst = BigDecimal.ZERO;
        int sumPurCount = 0;
        BigDecimal sumPurGross = BigDecimal.ZERO;
        BigDecimal sumPurGst = BigDecimal.ZERO;
        BigDecimal sumCombinedGst = BigDecimal.ZERO;
        BigDecimal sumNetTurnover = BigDecimal.ZERO;

        for (UserWiseReportModels.UserWiseReportRow row : filtered) {
            TableRow dataRow = createDataRow(row, slNo++);
            tableUserReport.addView(dataRow);
            tableUserReport.addView(createDividerRow());

            sumSalesCount += row.salesCount;
            sumSalesGross = sumSalesGross.add(row.salesGrossTotal);
            sumSalesGst = sumSalesGst.add(row.salesTotalGst);
            sumPurCount += row.purchaseCount;
            sumPurGross = sumPurGross.add(row.purchaseGrossTotal);
            sumPurGst = sumPurGst.add(row.purchaseTotalGst);
            sumCombinedGst = sumCombinedGst.add(row.totalCombinedGst);
            sumNetTurnover = sumNetTurnover.add(row.netTurnover);
        }

        // Summary Total Row
        if (filtered.size() > 1) {
            TableRow totalRow = createTotalRow(
                    filtered.size(),
                    sumSalesCount,
                    sumSalesGross,
                    sumSalesGst,
                    sumPurCount,
                    sumPurGross,
                    sumPurGst,
                    sumCombinedGst,
                    sumNetTurnover
            );
            tableUserReport.addView(totalRow);
        }
    }

    private TableRow createTableHeaderRow() {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(ContextCompat.getColor(this, R.color.material_blue));
        row.setPadding(0, dpToPx(8), 0, dpToPx(8));

        String[] headers = {
                "#", "User / Staff", "Role", "Sales (Inv)", "Gross Sales",
                "Sales GST", "Purchases (PO)", "Gross Purchase", "Purchase GST",
                "Total GST", "Net Amount"
        };

        for (String title : headers) {
            TextView tv = new TextView(this);
            tv.setText(title);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            tv.setTextColor(ContextCompat.getColor(this, R.color.white));
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setPadding(dpToPx(10), dpToPx(6), dpToPx(10), dpToPx(6));
            tv.setGravity(Gravity.CENTER);
            row.addView(tv);
        }

        return row;
    }

    private TableRow createDividerRow() {
        TableRow row = new TableRow(this);
        View divider = new View(this);
        TableRow.LayoutParams params = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                dpToPx(1)
        );
        params.span = 11;
        divider.setLayoutParams(params);
        divider.setBackgroundColor(ContextCompat.getColor(this, R.color.border));
        row.addView(divider);
        return row;
    }

    private TableRow createTotalRow(
            int userCount,
            int totalSalesCount,
            BigDecimal totalSalesGross,
            BigDecimal totalSalesGst,
            int totalPurCount,
            BigDecimal totalPurGross,
            BigDecimal totalPurGst,
            BigDecimal totalCombinedGst,
            BigDecimal totalNetTurnover) {

        TableRow row = new TableRow(this);
        row.setBackgroundColor(Color.parseColor("#1E2235"));
        row.setPadding(0, dpToPx(8), 0, dpToPx(8));

        // 0: #
        row.addView(createCellText("TOTAL", Gravity.CENTER, R.color.white, 11, true));
        // 1: User / Staff
        row.addView(createCellText("(" + userCount + " Users)", Gravity.CENTER, R.color.white, 11, true));
        // 2: Role
        row.addView(createCellText("", Gravity.CENTER, R.color.fg_dim, 11, false));
        // 3: Sales (Inv)
        row.addView(createCellText(String.valueOf(totalSalesCount), Gravity.CENTER, R.color.white, 11, true));
        // 4: Gross Sales
        row.addView(createCellText("₹ " + totalSalesGross.toPlainString(), Gravity.END, R.color.white, 11, true));
        // 5: Sales GST
        row.addView(createCellText("₹ " + totalSalesGst.toPlainString(), Gravity.END, R.color.white, 11, true));
        // 6: Purchases (PO)
        row.addView(createCellText(String.valueOf(totalPurCount), Gravity.CENTER, R.color.white, 11, true));
        // 7: Gross Purchase
        row.addView(createCellText("₹ " + totalPurGross.toPlainString(), Gravity.END, R.color.white, 11, true));
        // 8: Purchase GST
        row.addView(createCellText("₹ " + totalPurGst.toPlainString(), Gravity.END, R.color.white, 11, true));
        // 9: Total GST
        row.addView(createCellText("₹ " + totalCombinedGst.toPlainString(), Gravity.END, R.color.material_blue, 11, true));
        // 10: Net Amount
        int netColor = totalNetTurnover.compareTo(BigDecimal.ZERO) >= 0 ? R.color.status_green : R.color.error_red;
        row.addView(createCellText("₹ " + totalNetTurnover.toPlainString(), Gravity.END, netColor, 12, true));

        return row;
    }

    private TableRow createDataRow(UserWiseReportModels.UserWiseReportRow rowData, int index) {
        TableRow row = new TableRow(this);
        row.setBackgroundColor(Color.TRANSPARENT);
        row.setPadding(0, dpToPx(8), 0, dpToPx(8));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> showUserDetailDialog(rowData));

        // 0: #
        TextView tvSl = createCellText(String.valueOf(index), Gravity.CENTER, R.color.fg_dim, 11, false);
        row.addView(tvSl);

        // 1: User / Staff Details (Name + @username)
        LinearLayout layoutUser = new LinearLayout(this);
        layoutUser.setOrientation(LinearLayout.VERTICAL);
        layoutUser.setPadding(dpToPx(10), dpToPx(2), dpToPx(10), dpToPx(2));

        TextView tvName = new TextView(this);
        tvName.setText(rowData.userName);
        tvName.setTextColor(ContextCompat.getColor(this, R.color.fg));
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvName.setTypeface(Typeface.DEFAULT_BOLD);
        layoutUser.addView(tvName);

        if (!TextUtils.isEmpty(rowData.username)) {
            TextView tvUname = new TextView(this);
            tvUname.setText("@" + rowData.username);
            tvUname.setTextColor(ContextCompat.getColor(this, R.color.fg_dim));
            tvUname.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10);
            layoutUser.addView(tvUname);
        }
        row.addView(layoutUser);

        // 2: Role
        TextView tvRole = createCellText(rowData.role, Gravity.CENTER, R.color.material_blue, 10, true);
        row.addView(tvRole);

        // 3: Sales Inv Count
        TextView tvSalesCount = createCellText(String.valueOf(rowData.salesCount), Gravity.CENTER, R.color.fg, 11, false);
        row.addView(tvSalesCount);

        // 4: Gross Sales
        TextView tvSalesGross = createCellText("₹ " + rowData.salesGrossTotal.toPlainString(), Gravity.END, R.color.fg, 11, true);
        row.addView(tvSalesGross);

        // 5: Sales GST
        TextView tvSalesGst = createCellText("₹ " + rowData.salesTotalGst.toPlainString(), Gravity.END, R.color.material_blue, 11, false);
        row.addView(tvSalesGst);

        // 6: Purchases Count
        TextView tvPurCount = createCellText(String.valueOf(rowData.purchaseCount), Gravity.CENTER, R.color.fg, 11, false);
        row.addView(tvPurCount);

        // 7: Gross Purchases
        TextView tvPurGross = createCellText("₹ " + rowData.purchaseGrossTotal.toPlainString(), Gravity.END, R.color.fg, 11, true);
        row.addView(tvPurGross);

        // 8: Purchase GST
        TextView tvPurGst = createCellText("₹ " + rowData.purchaseTotalGst.toPlainString(), Gravity.END, R.color.status_orange, 11, false);
        row.addView(tvPurGst);

        // 9: Total Combined GST
        TextView tvTotalGst = createCellText("₹ " + rowData.totalCombinedGst.toPlainString(), Gravity.END, R.color.fg, 11, true);
        row.addView(tvTotalGst);

        // 10: Net Amount (Turnover)
        int netColor = rowData.netTurnover.compareTo(BigDecimal.ZERO) >= 0 ? R.color.status_green : R.color.error_red;
        TextView tvNet = createCellText("₹ " + rowData.netTurnover.toPlainString(), Gravity.END, netColor, 12, true);
        row.addView(tvNet);

        return row;
    }

    private TextView createCellText(String text, int gravity, int colorResId, float textSizeSp, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setGravity(gravity | Gravity.CENTER_VERTICAL);
        tv.setTextColor(ContextCompat.getColor(this, colorResId));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
        if (bold) {
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        }
        tv.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        return tv;
    }

    private void showUserDetailDialog(UserWiseReportModels.UserWiseReportRow row) {
        if (row == null || isFinishing() || isDestroyed()) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_report_detail, null);

        TextView tvName = dialogView.findViewById(R.id.tv_detail_user_name);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_detail_user_subtitle);
        TextView tvRoleBadge = dialogView.findViewById(R.id.tv_detail_role_badge);

        // Sales
        TextView tvSalesCount = dialogView.findViewById(R.id.tv_detail_sales_count);
        TextView tvSalesGross = dialogView.findViewById(R.id.tv_detail_sales_gross);
        TextView tvSalesTaxable = dialogView.findViewById(R.id.tv_detail_sales_taxable);
        TextView tvSalesTaxBreakup = dialogView.findViewById(R.id.tv_detail_sales_tax_breakup);
        TextView tvSalesTotalGst = dialogView.findViewById(R.id.tv_detail_sales_total_gst);

        // Purchase
        TextView tvPurchaseCount = dialogView.findViewById(R.id.tv_detail_purchase_count);
        TextView tvPurchaseGross = dialogView.findViewById(R.id.tv_detail_purchase_gross);
        TextView tvPurchaseTaxable = dialogView.findViewById(R.id.tv_detail_purchase_taxable);
        TextView tvPurchaseTaxBreakup = dialogView.findViewById(R.id.tv_detail_purchase_tax_breakup);
        TextView tvPurchaseTotalGst = dialogView.findViewById(R.id.tv_detail_purchase_total_gst);

        // Summary
        TextView tvCombinedGst = dialogView.findViewById(R.id.tv_detail_combined_gst);
        TextView tvNetGstLiability = dialogView.findViewById(R.id.tv_detail_net_gst_liability);
        TextView tvNetTurnover = dialogView.findViewById(R.id.tv_detail_net_turnover);
        View btnClose = dialogView.findViewById(R.id.btn_close_detail);

        tvName.setText(row.userName);
        String sub = (!TextUtils.isEmpty(row.username) ? "@" + row.username : "ID: #" + row.userId)
                + (!TextUtils.isEmpty(row.contact) ? " • " + row.contact : "");
        tvSubtitle.setText(sub);
        tvRoleBadge.setText(row.role);

        // Sales
        tvSalesCount.setText(row.salesCount + " Invoices");
        tvSalesGross.setText("₹ " + row.salesGrossTotal.toPlainString());
        tvSalesTaxable.setText("₹ " + row.salesTaxable.toPlainString());
        tvSalesTaxBreakup.setText("CGST: ₹ " + row.salesCgst.toPlainString() + " | SGST: ₹ " + row.salesSgst.toPlainString() + " | IGST: ₹ " + row.salesIgst.toPlainString());
        tvSalesTotalGst.setText("₹ " + row.salesTotalGst.toPlainString());

        // Purchase
        tvPurchaseCount.setText(row.purchaseCount + " Orders");
        tvPurchaseGross.setText("₹ " + row.purchaseGrossTotal.toPlainString());
        tvPurchaseTaxable.setText("₹ " + row.purchaseTaxable.toPlainString());
        tvPurchaseTaxBreakup.setText("CGST: ₹ " + row.purchaseCgst.toPlainString() + " | SGST: ₹ " + row.purchaseSgst.toPlainString() + " | IGST: ₹ " + row.purchaseIgst.toPlainString());
        tvPurchaseTotalGst.setText("₹ " + row.purchaseTotalGst.toPlainString());

        // Net Summary
        tvCombinedGst.setText("₹ " + row.totalCombinedGst.toPlainString());
        tvNetGstLiability.setText("₹ " + row.netGstLiability.toPlainString());
        tvNetTurnover.setText("₹ " + row.netTurnover.toPlainString());
        if (row.netTurnover.compareTo(BigDecimal.ZERO) >= 0) {
            tvNetTurnover.setTextColor(ContextCompat.getColor(this, R.color.status_green));
        } else {
            tvNetTurnover.setTextColor(ContextCompat.getColor(this, R.color.error_red));
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showExportDialog() {
        if (currentReportData == null || currentReportData.rows == null || currentReportData.rows.isEmpty()) {
            Toast.makeText(this, "No data available to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Excel (.xlsx) Spreadsheet", "PDF Document (.pdf)", "CSV Document (.csv)"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Export User Wise Report")
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
                File file = UserWiseReportExcelExporter.exportToExcel(UserWiseReportActivity.this, currentReportData);
                new Handler(Looper.getMainLooper()).post(() ->
                        showExportSuccess(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(UserWiseReportActivity.this, "Excel Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void exportPdf() {
        Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File file = UserWiseReportPdfExporter.exportToPdf(UserWiseReportActivity.this, currentReportData);
                new Handler(Looper.getMainLooper()).post(() ->
                        showExportSuccess(file, "application/pdf"));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(UserWiseReportActivity.this, "PDF Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void exportCsv() {
        Toast.makeText(this, "Generating CSV...", Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                File file = UserWiseReportCsvExporter.exportToCsv(UserWiseReportActivity.this, currentReportData);
                new Handler(Looper.getMainLooper()).post(() ->
                        showExportSuccess(file, "text/csv"));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(UserWiseReportActivity.this, "CSV Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
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

        Executors.newSingleThreadExecutor().execute(() -> {
            boolean success = false;
            String savedPath = "Download/Inventory Hub/Report/" + file.getName();
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentResolver resolver = getContentResolver();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType != null ? mimeType : "application/octet-stream");
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
                    savedPath = destFile.getAbsolutePath();
                    MediaScannerConnection.scanFile(this, new String[]{destFile.getAbsolutePath()}, new String[]{mimeType}, null);
                    success = true;
                }

                String finalPath = savedPath;
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(UserWiseReportActivity.this, "File saved to " + finalPath, Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(UserWiseReportActivity.this, "Failed to save file: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void shareFile(File file, String mimeType) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "File not found.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType != null ? mimeType : "application/octet-stream");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, file.getName());
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, "Share Report via"));
        } catch (Exception e) {
            Toast.makeText(this, "Sharing failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_reports_user_summary);
        }
    }
}
