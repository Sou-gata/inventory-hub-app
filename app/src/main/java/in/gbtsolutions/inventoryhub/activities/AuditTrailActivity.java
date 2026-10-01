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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audit_trail);

        repository = new AuditTrailRepository(getApplication());

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
        observeAuditData();

        // Ensure 30-day retention pruning runs in background
        AuditTrailHelper.pruneOldRecords(this);
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

        btnResetFilters.setOnClickListener(v -> resetFilters());
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
                applyFilterAndSearch();
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

        applyFilterAndSearch();
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
                applyFilterAndSearch();
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

        recyclerAuditTrails.setLayoutManager(new LinearLayoutManager(this));
        recyclerAuditTrails.setAdapter(adapter);
    }

    private void observeAuditData() {
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
                btnResetFilters.setVisibility(View.VISIBLE);
            } else {
                textEmptyTitle.setText("No Audit Trail Records");
                textEmptySubtitle.setText("All edits, additions, and deletions will be automatically tracked here and retained for 30 days.");
                btnResetFilters.setVisibility(View.GONE);
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
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 101, 0, "Export CSV")
                .setIcon(R.drawable.ic_upload_file)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == 101) {
            exportAuditTrailCsv();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exportAuditTrailCsv() {
        if (displayedList.isEmpty()) {
            Toast.makeText(this, "No audit trail records to export", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File exportDir = new File(getExternalFilesDir(null), "audit_reports");
            if (!exportDir.exists()) {
                exportDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File csvFile = new File(exportDir, "AuditTrail_" + timestamp + ".csv");

            FileWriter writer = new FileWriter(csvFile);
            writer.append("ID,Action,Module,Record ID,Performed By,Date & Time,Details\n");

            for (AuditTrail item : displayedList) {
                writer.append(String.valueOf(item.id)).append(",");
                writer.append("\"").append(item.actionType != null ? item.actionType : "").append("\",");
                writer.append("\"").append(item.module != null ? item.module : "").append("\",");
                writer.append("\"").append(item.recordId != null ? item.recordId.replace("\"", "\"\"") : "").append("\",");
                writer.append("\"").append(item.performedBy != null ? item.performedBy.replace("\"", "\"\"") : "").append("\",");
                writer.append("\"").append(item.getFormattedDate()).append("\",");
                writer.append("\"").append(item.details != null ? item.details.replace("\"", "\"\"") : "").append("\"\n");
            }

            writer.flush();
            writer.close();

            Uri fileUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", csvFile);

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/csv");
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Audit Trail Report (" + timestamp + ")");
            sendIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(sendIntent, "Share Audit Trail CSV"));
        } catch (Exception e) {
            Toast.makeText(this, "Failed to export CSV: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
