package in.gbtsolutions.inventoryhub.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.media.MediaScannerConnection;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.io.IOException;

import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.online.paging.EndlessRecyclerScrollListener;
import in.gbtsolutions.inventoryhub.online.repository.OnlineReportRepository;
import in.gbtsolutions.inventoryhub.reports.audittrail.AuditTrailCalculator;
import in.gbtsolutions.inventoryhub.reports.audittrail.AuditTrailCsvExporter;
import in.gbtsolutions.inventoryhub.reports.audittrail.AuditTrailExcelExporter;
import in.gbtsolutions.inventoryhub.reports.audittrail.AuditTrailPdfExporter;
import in.gbtsolutions.inventoryhub.reports.audittrail.AuditTrailReportModels;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.AuditTrailAdapter;
import in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper;
import in.gbtsolutions.inventoryhub.models.AuditTrail;
import in.gbtsolutions.inventoryhub.repository.AuditTrailRepository;

public class AuditTrailActivity extends BaseActivity {

    private enum ActionFilter {
        ALL, ADD, EDIT, DELETE
    }

    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private Toolbar toolbar;

    private TextView textCountTotal;
    private TextView textCountAdds;
    private TextView textCountEdits;
    private TextView textCountDeletions;

    private EditText editSearchAudit;
    private ImageView btnClearSearch;

    private TextView chipFilterAll;
    private TextView chipFilterAdd;
    private TextView chipFilterEdit;
    private TextView chipFilterDelete;
    private Spinner spinnerModuleFilter;

    private TextView textTrailCountHeader;
    private ProgressBar progressLoading;
    private RecyclerView recyclerAuditTrails;
    private View layoutEmptyState;
    private TextView textEmptyTitle;
    private TextView textEmptySubtitle;
    private Button btnResetFilters;

    private AuditTrailAdapter adapter;
    private AuditTrailRepository repository;

    private final List<AuditTrail> masterList = new ArrayList<>();
    private final List<AuditTrail> displayedList = new ArrayList<>();

    private ActionFilter currentActionFilter = ActionFilter.ALL;
    private String selectedModule = "All Modules";
    private String currentSearchQuery = "";

    private MaterialButton btnExport;
    private AppModeManager appModeManager;
    private OnlineReportRepository onlineReportRepository;

    private EndlessRecyclerScrollListener endlessScrollListener;
    private int onlineCurrentPage = 1;
    private boolean onlineIsLoading = false;
    private boolean onlineHasMore = true;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;

    private File pendingFileToSave;
    private String pendingMimeTypeToSave;

    private final ActivityResultLauncher<String> storagePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    if (pendingFileToSave != null) {
                        saveFileToDownloads(pendingFileToSave, pendingMimeTypeToSave, true);
                    }
                } else {
                    Toast.makeText(this, "Storage permission is required to save reports to Downloads.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_trail);
        if (!isCurrentUserAdmin()) {
            Toast.makeText(this, "Access denied: Administrator privileges required.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        repository = new AuditTrailRepository(getApplication());
        appModeManager = AppModeManager.getInstance(this);
        onlineReportRepository = new OnlineReportRepository(this);

        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar = findViewById(R.id.toolbar);
        navView = findViewById(R.id.nav_view);

        View headerContainer = findViewById(R.id.header_container);
        View contentContainer = findViewById(R.id.audit_content_container);

        applyDrawerInsets(drawerLayout, headerContainer, contentContainer, navView);
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_audit_trail);

        initViews();
        setupModuleSpinner();
        setupFilterChips();
        setupSearch();
        setupRecyclerView();
        loadAuditData();

        // Ensure 30-day retention pruning runs in background (offline mode only)
        if (appModeManager == null || !appModeManager.isOnlineMode()) {
            AuditTrailHelper.pruneOldRecords(this);
        }
    }

    private void initViews() {
        textCountTotal = findViewById(R.id.text_count_total);
        textCountAdds = findViewById(R.id.text_count_adds);
        textCountEdits = findViewById(R.id.text_count_edits);
        textCountDeletions = findViewById(R.id.text_count_deletions);

        editSearchAudit = findViewById(R.id.edit_search_audit);
        btnClearSearch = findViewById(R.id.btn_clear_search);

        chipFilterAll = findViewById(R.id.chip_filter_all);
        chipFilterAdd = findViewById(R.id.chip_filter_add);
        chipFilterEdit = findViewById(R.id.chip_filter_edit);
        chipFilterDelete = findViewById(R.id.chip_filter_delete);
        spinnerModuleFilter = findViewById(R.id.spinner_module_filter);

        textTrailCountHeader = findViewById(R.id.text_trail_count_header);
        progressLoading = findViewById(R.id.progress_loading);
        recyclerAuditTrails = findViewById(R.id.recycler_audit_trails);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        textEmptyTitle = findViewById(R.id.text_empty_title);
        textEmptySubtitle = findViewById(R.id.text_empty_subtitle);
        btnResetFilters = findViewById(R.id.btn_reset_filters);
        btnExport = findViewById(R.id.btn_export_report);

        btnResetFilters.setOnClickListener(v -> resetFilters());
        if (btnExport != null) {
            btnExport.setOnClickListener(v -> showExportDialog());
        }
    }

    private void setupModuleSpinner() {
        String[] modules = new String[]{
                "All Modules",
                AuditTrail.MODULE_PRODUCT,
                AuditTrail.MODULE_CATEGORY,
                AuditTrail.MODULE_BUYER,
                AuditTrail.MODULE_SUPPLIER,
                AuditTrail.MODULE_SALE,
                AuditTrail.MODULE_PURCHASE,
                AuditTrail.MODULE_USER,
                AuditTrail.MODULE_UNIT_OF_MEASURE,
                AuditTrail.MODULE_CONFIG
        };

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, modules);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModuleFilter.setAdapter(spinnerAdapter);

        spinnerModuleFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedModule = modules[position];
                if (appModeManager != null && appModeManager.isOnlineMode()) {
                    loadOnlineAuditTrails(1, true);
                } else {
                    applyFilterAndSearch();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupFilterChips() {
        chipFilterAll.setOnClickListener(v -> setActionFilter(ActionFilter.ALL));
        chipFilterAdd.setOnClickListener(v -> setActionFilter(ActionFilter.ADD));
        chipFilterEdit.setOnClickListener(v -> setActionFilter(ActionFilter.EDIT));
        chipFilterDelete.setOnClickListener(v -> setActionFilter(ActionFilter.DELETE));
    }

    private void setActionFilter(ActionFilter filter) {
        currentActionFilter = filter;

        updateChipStyle(chipFilterAll, filter == ActionFilter.ALL);
        updateChipStyle(chipFilterAdd, filter == ActionFilter.ADD);
        updateChipStyle(chipFilterEdit, filter == ActionFilter.EDIT);
        updateChipStyle(chipFilterDelete, filter == ActionFilter.DELETE);

        if (appModeManager != null && appModeManager.isOnlineMode()) {
            loadOnlineAuditTrails(1, true);
        } else {
            applyFilterAndSearch();
        }
    }

    private void updateChipStyle(TextView chip, boolean isSelected) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_button);
            chip.setTextColor(ContextCompat.getColor(this, R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_card);
            chip.setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
        }
    }

    private void setupSearch() {
        editSearchAudit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim() : "";
                btnClearSearch.setVisibility(currentSearchQuery.isEmpty() ? View.GONE : View.VISIBLE);
                if (appModeManager != null && appModeManager.isOnlineMode()) {
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    searchRunnable = () -> loadOnlineAuditTrails(1, true);
                    searchHandler.postDelayed(searchRunnable, 400);
                } else {
                    applyFilterAndSearch();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnClearSearch.setOnClickListener(v -> editSearchAudit.setText(""));
    }

    private void setupRecyclerView() {
        adapter = new AuditTrailAdapter();
        adapter.setOnAuditTrailClickListener(this::showAuditDetailDialog);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerAuditTrails.setLayoutManager(layoutManager);
        recyclerAuditTrails.setAdapter(adapter);

        endlessScrollListener = new EndlessRecyclerScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (appModeManager != null && appModeManager.isOnlineMode() && onlineHasMore && !onlineIsLoading) {
                    loadOnlineAuditTrails(page, false);
                }
            }
        };
        recyclerAuditTrails.addOnScrollListener(endlessScrollListener);
    }

    private boolean isInitialLoad = true;

    @Override
    protected void onResume() {
        super.onResume();
        if (isInitialLoad) {
            isInitialLoad = false;
        } else {
            if (appModeManager != null && appModeManager.isOnlineMode()) {
                loadOnlineAuditTrails(1, true);
            }
        }
    }

    private void loadAuditData() {
        if (appModeManager != null && appModeManager.isOnlineMode()) {
            loadOnlineAuditTrails(1, true);
        } else {
            observeOfflineAuditData();
        }
    }

    private void observeOfflineAuditData() {
        progressLoading.setVisibility(View.VISIBLE);
        repository.getAllAuditTrails().observe(this, items -> {
            progressLoading.setVisibility(View.GONE);
            masterList.clear();
            if (items != null) {
                masterList.addAll(items);
            }
            updateMetrics(masterList);
            applyFilterAndSearch();
        });
    }

    private void loadOnlineAuditTrails(int page, boolean isNewSearch) {
        if (onlineIsLoading && !isNewSearch) return;
        onlineIsLoading = true;

        if (isNewSearch) {
            onlineCurrentPage = 1;
            onlineHasMore = true;
            if (endlessScrollListener != null) {
                endlessScrollListener.resetState();
            }
            adapter.clearAuditTrails();
            progressLoading.setVisibility(View.VISIBLE);
            textTrailCountHeader.setText("Loading activity logs...");
        } else {
            adapter.showLoadingFooter();
        }

        String[] dates = getDefault30DayDateRange();
        String mod = !"All Modules".equalsIgnoreCase(selectedModule) ? selectedModule : null;
        String act = currentActionFilter != ActionFilter.ALL ? currentActionFilter.name() : null;
        String srch = !currentSearchQuery.isEmpty() ? currentSearchQuery : null;

        onlineReportRepository.fetchAuditTrailReport(dates[0], dates[1], mod, act, srch, page,
                EndlessRecyclerScrollListener.PAGE_SIZE, new OnlineReportRepository.AuditTrailCallback() {
            @Override
            public void onSuccess(AuditTrailReportModels.ReportData reportData) {
                if (isFinishing() || isDestroyed()) return;
                onlineIsLoading = false;
                progressLoading.setVisibility(View.GONE);

                if (reportData == null) {
                    reportData = new AuditTrailReportModels.ReportData();
                }
                reportData.ensureNonNull();

                onlineCurrentPage = reportData.page;
                onlineHasMore = reportData.hasMore;
                if (endlessScrollListener != null) {
                    endlessScrollListener.setHasMore(onlineHasMore);
                    endlessScrollListener.setLoading(false);
                }

                adapter.removeFooter();

                List<AuditTrail> newItems = new ArrayList<>();
                for (AuditTrailReportModels.AuditTrailRow row : reportData.rows) {
                    newItems.add(convertRowToAuditTrail(row));
                }

                if (isNewSearch) {
                    displayedList.clear();
                    masterList.clear();
                    displayedList.addAll(newItems);
                    masterList.addAll(newItems);
                    adapter.setAuditTrails(new ArrayList<>(displayedList));
                } else {
                    displayedList.addAll(newItems);
                    masterList.addAll(newItems);
                    adapter.addAuditTrails(newItems);
                }

                if (reportData.summary != null) {
                    textCountTotal.setText(String.valueOf(reportData.summary.totalEvents));
                    textCountAdds.setText(String.valueOf(reportData.summary.creates));
                    int editsTotal = reportData.summary.updates + reportData.summary.receives + reportData.summary.cancels;
                    textCountEdits.setText(String.valueOf(editsTotal));
                    textCountDeletions.setText(String.valueOf(reportData.summary.deletes));
                }

                int totalLoaded = adapter.getAuditTrails().size();
                int totalRecords = reportData.totalRecords > 0 ? reportData.totalRecords :
                        (reportData.summary != null ? reportData.summary.totalEvents : totalLoaded);
                if (totalRecords < totalLoaded) totalRecords = totalLoaded;

                textTrailCountHeader.setText(String.format(Locale.getDefault(), "Showing %d of %d activity logs", totalLoaded, totalRecords));

                boolean isEmpty = totalLoaded == 0;
                recyclerAuditTrails.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
                layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
                if (isEmpty) {
                    boolean hasFilter = currentActionFilter != ActionFilter.ALL
                            || !"All Modules".equalsIgnoreCase(selectedModule)
                            || !currentSearchQuery.isEmpty();
                    if (hasFilter) {
                        textEmptyTitle.setText("No Matching Logs Found");
                        textEmptySubtitle.setText("Try adjusting or resetting your search and filter criteria.");
                        btnResetFilters.setText("Reset Filters");
                        btnResetFilters.setVisibility(View.VISIBLE);
                        btnResetFilters.setOnClickListener(v -> resetFilters());
                    } else {
                        textEmptyTitle.setText("No Audit Trail Available");
                        textEmptySubtitle.setText("Events will be recorded here as operations occur in the app.");
                        btnResetFilters.setText("Refresh");
                        btnResetFilters.setVisibility(View.VISIBLE);
                        btnResetFilters.setOnClickListener(v -> loadOnlineAuditTrails(1, true));
                    }
                }
            }

            @Override
            public void onError(String errorMessage, boolean sessionExpired) {
                if (isFinishing() || isDestroyed()) return;
                onlineIsLoading = false;
                progressLoading.setVisibility(View.GONE);
                if (endlessScrollListener != null) {
                    endlessScrollListener.setLoading(false);
                }

                if (sessionExpired) {
                    Toast.makeText(AuditTrailActivity.this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                    appModeManager.clearAuthToken();
                    navigateTo(LoginActivity.class, true);
                    return;
                }

                if (isNewSearch) {
                    Toast.makeText(AuditTrailActivity.this, "Failed to load audit trail: " + errorMessage, Toast.LENGTH_SHORT).show();
                    if (adapter.getAuditTrails().isEmpty()) {
                        recyclerAuditTrails.setVisibility(View.GONE);
                        layoutEmptyState.setVisibility(View.VISIBLE);
                        textEmptyTitle.setText("Failed to Load Logs");
                        textEmptySubtitle.setText(errorMessage);
                        btnResetFilters.setText("Retry");
                        btnResetFilters.setVisibility(View.VISIBLE);
                        btnResetFilters.setOnClickListener(v -> loadOnlineAuditTrails(1, true));
                    }
                } else {
                    adapter.showRetryFooter("Error: " + errorMessage, () -> loadOnlineAuditTrails(page, false));
                }
            }
        });
    }

    private AuditTrail convertRowToAuditTrail(AuditTrailReportModels.AuditTrailRow row) {
        AuditTrail item = new AuditTrail();
        item.id = (int) row.id;
        item.actionType = row.actionType != null ? row.actionType : AuditTrail.ACTION_EDIT;
        item.module = row.module != null ? row.module : "General";
        item.recordId = row.recordId != null ? row.recordId : "";
        item.details = row.details != null ? row.details : "";
        item.performedBy = row.performedBy != null ? row.performedBy : "System";
        item.timestamp = row.timestamp;
        return item;
    }

    private String[] getDefault30DayDateRange() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        long now = System.currentTimeMillis();
        String end = sdf.format(new Date(now));
        String start = sdf.format(new Date(now - 30L * 24 * 60 * 60 * 1000));
        return new String[]{start, end};
    }

    private void updateMetrics(List<AuditTrail> list) {
        int total = list.size();
        int adds = 0;
        int edits = 0;
        int deletions = 0;

        for (AuditTrail item : list) {
            if (AuditTrail.ACTION_ADD.equalsIgnoreCase(item.actionType)) {
                adds++;
            } else if (AuditTrail.ACTION_EDIT.equalsIgnoreCase(item.actionType)) {
                edits++;
            } else if (AuditTrail.ACTION_DELETE.equalsIgnoreCase(item.actionType)) {
                deletions++;
            }
        }

        textCountTotal.setText(String.valueOf(total));
        textCountAdds.setText(String.valueOf(adds));
        textCountEdits.setText(String.valueOf(edits));
        textCountDeletions.setText(String.valueOf(deletions));
    }

    private void applyFilterAndSearch() {
        displayedList.clear();
        String queryLower = currentSearchQuery.toLowerCase(Locale.getDefault());
        boolean hasModuleFilter = !"All Modules".equalsIgnoreCase(selectedModule);

        for (AuditTrail item : masterList) {
            // Action filter check
            if (currentActionFilter == ActionFilter.ADD && !AuditTrail.ACTION_ADD.equalsIgnoreCase(item.actionType)) {
                continue;
            }
            if (currentActionFilter == ActionFilter.EDIT && !AuditTrail.ACTION_EDIT.equalsIgnoreCase(item.actionType)) {
                continue;
            }
            if (currentActionFilter == ActionFilter.DELETE && !AuditTrail.ACTION_DELETE.equalsIgnoreCase(item.actionType)) {
                continue;
            }

            // Module filter check
            if (hasModuleFilter && !selectedModule.equalsIgnoreCase(item.module)) {
                continue;
            }

            // Search query check
            if (!queryLower.isEmpty()) {
                boolean matchesDetail = item.details != null && item.details.toLowerCase(Locale.getDefault()).contains(queryLower);
                boolean matchesRecord = item.recordId != null && item.recordId.toLowerCase(Locale.getDefault()).contains(queryLower);
                boolean matchesUser = item.performedBy != null && item.performedBy.toLowerCase(Locale.getDefault()).contains(queryLower);
                boolean matchesModule = item.module != null && item.module.toLowerCase(Locale.getDefault()).contains(queryLower);
                boolean matchesAction = item.actionType != null && item.actionType.toLowerCase(Locale.getDefault()).contains(queryLower);

                if (!matchesDetail && !matchesRecord && !matchesUser && !matchesModule && !matchesAction) {
                    continue;
                }
            }

            displayedList.add(item);
        }

        adapter.setAuditTrails(displayedList);
        updateListVisibility();
    }

    private void updateListVisibility() {
        boolean isEmpty = displayedList.isEmpty();
        recyclerAuditTrails.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        layoutEmptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);

        if (isEmpty) {
            boolean hasActiveFilters = currentActionFilter != ActionFilter.ALL
                    || !"All Modules".equalsIgnoreCase(selectedModule)
                    || !currentSearchQuery.isEmpty();

            if (hasActiveFilters) {
                textEmptyTitle.setText("No Matching Logs");
                textEmptySubtitle.setText("No audit trail entries found matching your active search or filters.");
                btnResetFilters.setText("Reset Filters");
                btnResetFilters.setVisibility(View.VISIBLE);
                btnResetFilters.setOnClickListener(v -> resetFilters());
            } else {
                if (appModeManager != null && appModeManager.isOnlineMode()) {
                    textEmptyTitle.setText("No Audit Trail Records");
                    textEmptySubtitle.setText("No activity logs found on server for the past 30 days.");
                    btnResetFilters.setText("Refresh");
                    btnResetFilters.setVisibility(View.VISIBLE);
                    btnResetFilters.setOnClickListener(v -> loadOnlineAuditTrails(1, true));
                } else {
                    textEmptyTitle.setText("No Audit Trail Records");
                    textEmptySubtitle.setText("All edits, additions, and deletions will be automatically tracked here and retained for 30 days.");
                    btnResetFilters.setVisibility(View.GONE);
                }
            }
            textTrailCountHeader.setText("No records to display");
        } else {
            textTrailCountHeader.setText("Showing " + displayedList.size() + " of " + masterList.size() + " activity logs");
        }
    }

    private void resetFilters() {
        editSearchAudit.setText("");
        spinnerModuleFilter.setSelection(0);
        setActionFilter(ActionFilter.ALL);
    }

    private void showAuditDetailDialog(@NonNull AuditTrail item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_audit_trail_details, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        LinearLayout layoutBadge = view.findViewById(R.id.dialog_layout_action_badge);
        ImageView iconType = view.findViewById(R.id.dialog_icon_action_type);
        TextView textType = view.findViewById(R.id.dialog_text_action_type);
        TextView textModule = view.findViewById(R.id.dialog_text_module);
        TextView textRecordId = view.findViewById(R.id.dialog_text_record_id);
        TextView textUser = view.findViewById(R.id.dialog_text_user);
        TextView textTimestamp = view.findViewById(R.id.dialog_text_timestamp);
        TextView textDetails = view.findViewById(R.id.dialog_text_details);
        ImageView btnClose = view.findViewById(R.id.dialog_btn_close);
        Button btnCopy = view.findViewById(R.id.dialog_btn_copy);
        Button btnDismiss = view.findViewById(R.id.dialog_btn_dismiss);

        // Bind data
        String action = item.actionType != null ? item.actionType.toUpperCase() : "EVENT";
        textType.setText(action);

        if (AuditTrail.ACTION_ADD.equalsIgnoreCase(action)) {
            layoutBadge.setBackgroundResource(R.drawable.bg_badge_add);
            int green = ContextCompat.getColor(this, R.color.status_green);
            textType.setTextColor(green);
            iconType.setImageResource(R.drawable.ic_add);
            iconType.setColorFilter(green);
        } else if (AuditTrail.ACTION_EDIT.equalsIgnoreCase(action)) {
            layoutBadge.setBackgroundResource(R.drawable.bg_badge_edit);
            int blue = ContextCompat.getColor(this, R.color.status_blue);
            textType.setTextColor(blue);
            iconType.setImageResource(R.drawable.ic_edit);
            iconType.setColorFilter(blue);
        } else if (AuditTrail.ACTION_DELETE.equalsIgnoreCase(action)) {
            layoutBadge.setBackgroundResource(R.drawable.bg_badge_delete);
            int red = ContextCompat.getColor(this, R.color.status_red);
            textType.setTextColor(red);
            iconType.setImageResource(R.drawable.ic_delete_outline);
            iconType.setColorFilter(red);
        }

        textModule.setText(item.module != null ? item.module : "General");
        textRecordId.setText(item.recordId != null && !item.recordId.isEmpty() ? item.recordId : "System Event");
        textUser.setText(item.performedBy != null && !item.performedBy.isEmpty() ? item.performedBy : "System");
        textTimestamp.setText(item.getFormattedDate());
        textDetails.setText(item.details != null ? item.details : "No details recorded.");

        btnClose.setOnClickListener(v -> dialog.dismiss());
        btnDismiss.setOnClickListener(v -> dialog.dismiss());

        btnCopy.setOnClickListener(v -> {
            String copyText = "Audit Trail Log\n"
                    + "Action: " + action + "\n"
                    + "Module: " + item.module + "\n"
                    + "Record: " + item.recordId + "\n"
                    + "User: " + item.performedBy + "\n"
                    + "Date/Time: " + item.getFormattedDate() + "\n"
                    + "Details: " + item.details;

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Audit Trail Entry", copyText);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied details to clipboard", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
        if (onlineReportRepository != null) {
            onlineReportRepository.cancel();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (appModeManager != null && appModeManager.isOnlineMode()) {
            menu.add(0, 102, 0, "Refresh")
                    .setIcon(R.drawable.ic_sync)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 101) {
            showExportDialog();
            return true;
        } else if (item.getItemId() == 102) {
            if (appModeManager != null && appModeManager.isOnlineMode()) {
                loadOnlineAuditTrails(1, true);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showExportDialog() {
        if ((appModeManager == null || !appModeManager.isOnlineMode()) && displayedList.isEmpty()) {
            Toast.makeText(this, "No audit trail records to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = {"Excel (.xlsx) Spreadsheet", "PDF Document (.pdf)", "CSV Document (.csv)"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Export Audit Trail")
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

    private String[] resolveReportDateRange() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        long minTs = Long.MAX_VALUE;
        long maxTs = Long.MIN_VALUE;
        List<AuditTrail> sourceList = !masterList.isEmpty() ? masterList : displayedList;
        for (AuditTrail a : sourceList) {
            if (a.timestamp > 0) {
                if (a.timestamp < minTs) minTs = a.timestamp;
                if (a.timestamp > maxTs) maxTs = a.timestamp;
            }
        }
        String start;
        String end;
        if (minTs != Long.MAX_VALUE && maxTs != Long.MIN_VALUE) {
            start = sdf.format(new Date(minTs));
            end = sdf.format(new Date(maxTs));
        } else {
            long now = System.currentTimeMillis();
            end = sdf.format(new Date(now));
            start = sdf.format(new Date(now - 30L * 24 * 60 * 60 * 1000));
        }
        return new String[]{start, end};
    }

    private void exportExcel() {
        if (appModeManager != null && appModeManager.isOnlineMode()) {
            Toast.makeText(this, "Generating Excel on server...", Toast.LENGTH_SHORT).show();
            String[] dates = getDefault30DayDateRange();
            String mod = !"All Modules".equalsIgnoreCase(selectedModule) ? selectedModule : null;
            String act = currentActionFilter != ActionFilter.ALL ? currentActionFilter.name() : null;
            String srch = !TextUtils.isEmpty(currentSearchQuery) ? currentSearchQuery : null;

            onlineReportRepository.downloadAuditTrailReport("excel", dates[0], dates[1], mod, act, srch, new OnlineReportRepository.DownloadCallback() {
                @Override
                public void onSuccess(File downloadedFile, String mimeType) {
                    saveFileToDownloads(downloadedFile, mimeType, true);
                }

                @Override
                public void onError(String errorMessage, boolean sessionExpired) {
                    if (sessionExpired) {
                        Toast.makeText(AuditTrailActivity.this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                        appModeManager.clearAuthToken();
                        navigateTo(LoginActivity.class, true);
                        return;
                    }
                    Toast.makeText(AuditTrailActivity.this, "Failed to generate Excel report on server: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(this, "Generating Excel...", Toast.LENGTH_SHORT).show();
            exportExcelLocally();
        }
    }

    private void exportExcelLocally() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String[] dates = resolveReportDateRange();
                AuditTrailReportModels.ReportData report = AuditTrailCalculator.generateReport(
                        AuditTrailActivity.this, new ArrayList<>(displayedList), dates[0], dates[1]);
                File file = AuditTrailExcelExporter.exportToExcel(AuditTrailActivity.this, report);
                new Handler(Looper.getMainLooper()).post(() ->
                        saveFileToDownloads(file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", true));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(AuditTrailActivity.this, "Excel Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void exportPdf() {
        if (appModeManager != null && appModeManager.isOnlineMode()) {
            Toast.makeText(this, "Generating PDF on server...", Toast.LENGTH_SHORT).show();
            String[] dates = getDefault30DayDateRange();
            String mod = !"All Modules".equalsIgnoreCase(selectedModule) ? selectedModule : null;
            String act = currentActionFilter != ActionFilter.ALL ? currentActionFilter.name() : null;
            String srch = !TextUtils.isEmpty(currentSearchQuery) ? currentSearchQuery : null;

            onlineReportRepository.downloadAuditTrailReport("pdf", dates[0], dates[1], mod, act, srch, new OnlineReportRepository.DownloadCallback() {
                @Override
                public void onSuccess(File downloadedFile, String mimeType) {
                    saveFileToDownloads(downloadedFile, mimeType, true);
                }

                @Override
                public void onError(String errorMessage, boolean sessionExpired) {
                    if (sessionExpired) {
                        Toast.makeText(AuditTrailActivity.this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                        appModeManager.clearAuthToken();
                        navigateTo(LoginActivity.class, true);
                        return;
                    }
                    Toast.makeText(AuditTrailActivity.this, "Failed to generate PDF report on server: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(this, "Generating PDF...", Toast.LENGTH_SHORT).show();
            exportPdfLocally();
        }
    }

    private void exportPdfLocally() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String[] dates = resolveReportDateRange();
                AuditTrailReportModels.ReportData report = AuditTrailCalculator.generateReport(
                        AuditTrailActivity.this, new ArrayList<>(displayedList), dates[0], dates[1]);
                File file = AuditTrailPdfExporter.exportToPdf(AuditTrailActivity.this, report);
                new Handler(Looper.getMainLooper()).post(() ->
                        saveFileToDownloads(file, "application/pdf", true));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(AuditTrailActivity.this, "PDF Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void exportCsv() {
        if (appModeManager != null && appModeManager.isOnlineMode()) {
            Toast.makeText(this, "Generating CSV on server...", Toast.LENGTH_SHORT).show();
            String[] dates = getDefault30DayDateRange();
            String mod = !"All Modules".equalsIgnoreCase(selectedModule) ? selectedModule : null;
            String act = currentActionFilter != ActionFilter.ALL ? currentActionFilter.name() : null;
            String srch = !TextUtils.isEmpty(currentSearchQuery) ? currentSearchQuery : null;

            onlineReportRepository.downloadAuditTrailReport("csv", dates[0], dates[1], mod, act, srch, new OnlineReportRepository.DownloadCallback() {
                @Override
                public void onSuccess(File downloadedFile, String mimeType) {
                    saveFileToDownloads(downloadedFile, mimeType, true);
                }

                @Override
                public void onError(String errorMessage, boolean sessionExpired) {
                    if (sessionExpired) {
                        Toast.makeText(AuditTrailActivity.this, "Session expired. Please log in again.", Toast.LENGTH_LONG).show();
                        appModeManager.clearAuthToken();
                        navigateTo(LoginActivity.class, true);
                        return;
                    }
                    Toast.makeText(AuditTrailActivity.this, "Failed to generate CSV report on server: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
        } else {
            Toast.makeText(this, "Generating CSV...", Toast.LENGTH_SHORT).show();
            exportCsvLocally();
        }
    }

    private void exportCsvLocally() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String[] dates = resolveReportDateRange();
                AuditTrailReportModels.ReportData report = AuditTrailCalculator.generateReport(
                        AuditTrailActivity.this, new ArrayList<>(displayedList), dates[0], dates[1]);
                File file = AuditTrailCsvExporter.exportToCsv(AuditTrailActivity.this, report);
                new Handler(Looper.getMainLooper()).post(() ->
                        saveFileToDownloads(file, "text/csv", true));
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(AuditTrailActivity.this, "CSV Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showExportSuccess(File file, String mimeType, String savedPath) {
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
        String infoText = typeLabel + " • " + formatFileSize(file.length());
        if (savedPath != null && !savedPath.isEmpty()) {
            infoText += "\nSaved to: " + savedPath;
        }
        tvFileInfo.setText(infoText);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        btnSave.setOnClickListener(v -> {
            dialog.dismiss();
            openFile(file, mimeType);
        });

        btnShare.setOnClickListener(v -> {
            dialog.dismiss();
            shareFile(file, mimeType);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void openFile(File file, String mimeType) {
        if (file == null || !file.exists()) {
            Toast.makeText(this, "File not found.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mimeType != null ? mimeType : "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No app found to open this file.", Toast.LENGTH_SHORT).show();
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    private void saveFileToDownloads(File file, String mimeType) {
        saveFileToDownloads(file, mimeType, true);
    }

    private void saveFileToDownloads(File file, String mimeType, boolean showSuccessDialogAfter) {
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
            String savedPath = "Download/Inventory Hub/Reports/" + file.getName();
            File savedFile = null;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentResolver resolver = getContentResolver();
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, file.getName());
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Inventory Hub/Reports");
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
                    File targetDir = new File(downloadDir, "Inventory Hub" + File.separator + "Reports");
                    if (!targetDir.exists() && !targetDir.mkdirs()) {
                        throw new IOException("Failed to create target directory: " + targetDir.getAbsolutePath());
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
                    savedFile = destFile;
                    MediaScannerConnection.scanFile(this, new String[]{destFile.getAbsolutePath()}, null, null);
                    success = true;
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() ->
                        Toast.makeText(AuditTrailActivity.this, "Failed to save: " + e.getMessage(), Toast.LENGTH_LONG).show());
                return;
            }

            if (success) {
                final String finalPath = savedPath;
                final File fileForDialog = (savedFile != null && savedFile.exists()) ? savedFile : file;
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(AuditTrailActivity.this, "Saved to " + finalPath, Toast.LENGTH_LONG).show();
                    if (showSuccessDialogAfter) {
                        showExportSuccess(fileForDialog, mimeType, finalPath);
                    }
                });
            }
        });
    }

    private void shareFile(File file, String mimeType) {
        if (file == null || !file.exists()) return;
        try {
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType(mimeType != null ? mimeType : "*/*");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Share Audit Trail Report"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to share: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
