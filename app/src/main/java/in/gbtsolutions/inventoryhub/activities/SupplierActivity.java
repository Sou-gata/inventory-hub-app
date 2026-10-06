package in.gbtsolutions.inventoryhub.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.SupplierAdapter;
import in.gbtsolutions.inventoryhub.helpers.ThemeManager;
import in.gbtsolutions.inventoryhub.models.Suppliers;
import in.gbtsolutions.inventoryhub.online.repository.OnlineSupplierRepository;
import in.gbtsolutions.inventoryhub.repository.SupplierRepository;

public class SupplierActivity extends BaseActivity {

    private final List<Suppliers> allSuppliers = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private NavigationView navView;
    private RecyclerView recyclerSuppliers;
    private SupplierAdapter supplierAdapter;
    private SupplierRepository supplierRepository;
    private View layoutEmptyState;
    private View layoutListContainer;
    private TextView textSupplierCount;
    private TextView textMetaLabel;
    private EditText editSearchSupplier;
    private ImageView btnClearSearch;
    private View layoutSearchEmpty;
    private TextView textSearchEmptyQuery;
    private View btnResetSearch;
    private TextView chipFilterAll;
    private TextView chipFilterActive;
    private TextView chipFilterDeactive;
    private String currentSearchQuery = "";
    private FilterStatus currentFilter = FilterStatus.ALL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supplier);

        supplierRepository = new SupplierRepository(getApplication());

        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        View headerContainer = findViewById(R.id.header_container);
        View contentContainer = findViewById(R.id.supplier_content_container);

        // Apply edge-to-edge system insets
        applyDrawerInsets(drawerLayout, headerContainer, contentContainer, navView);

        // Standardized sidebar and header navigation
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_supplier);

        initViews();
        setupFilterChips();
        setupRecyclerView();
        observeSuppliers();
    }

    private void initViews() {
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        layoutListContainer = findViewById(R.id.layout_list_container);
        textSupplierCount = findViewById(R.id.text_supplier_count);
        textMetaLabel = findViewById(R.id.text_meta_label);
        recyclerSuppliers = findViewById(R.id.recycler_suppliers);

        editSearchSupplier = findViewById(R.id.edit_search_supplier);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        layoutSearchEmpty = findViewById(R.id.layout_search_empty);
        textSearchEmptyQuery = findViewById(R.id.text_search_empty_query);
        btnResetSearch = findViewById(R.id.btn_reset_search);

        chipFilterAll = findViewById(R.id.chip_filter_all);
        chipFilterActive = findViewById(R.id.chip_filter_active);
        chipFilterDeactive = findViewById(R.id.chip_filter_deactive);

        FloatingActionButton fabAddSupplier = findViewById(R.id.fab_add_supplier);
        if (fabAddSupplier != null) {
            fabAddSupplier.setOnClickListener(v -> openAddSupplier());
        }

        View btnEmptyAdd = findViewById(R.id.btn_empty_add_supplier);
        if (btnEmptyAdd != null) {
            btnEmptyAdd.setOnClickListener(v -> openAddSupplier());
        }

        setupSearch();
    }

    private void setupFilterChips() {
        chipFilterAll.setOnClickListener(v -> selectFilter(FilterStatus.ALL));
        chipFilterActive.setOnClickListener(v -> selectFilter(FilterStatus.ACTIVE));
        chipFilterDeactive.setOnClickListener(v -> selectFilter(FilterStatus.DEACTIVATED));
    }

    private void selectFilter(FilterStatus filter) {
        currentFilter = filter;

        chipFilterAll.setBackgroundResource(filter == FilterStatus.ALL ? R.drawable.bg_button : R.drawable.bg_card);
        chipFilterAll.setTextColor(ContextCompat.getColor(this, filter == FilterStatus.ALL ? R.color.white : R.color.fg_muted));

        chipFilterActive.setBackgroundResource(filter == FilterStatus.ACTIVE ? R.drawable.bg_button : R.drawable.bg_card);
        chipFilterActive.setTextColor(ContextCompat.getColor(this, filter == FilterStatus.ACTIVE ? R.color.white : R.color.fg_muted));

        chipFilterDeactive.setBackgroundResource(filter == FilterStatus.DEACTIVATED ? R.drawable.bg_button : R.drawable.bg_card);
        chipFilterDeactive.setTextColor(ContextCompat.getColor(this, filter == FilterStatus.DEACTIVATED ? R.color.white : R.color.fg_muted));

        applyFilters();
    }

    private void setupSearch() {
        if (editSearchSupplier != null) {
            editSearchSupplier.addTextChangedListener(new TextWatcher() {
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
                    applyFilters();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            editSearchSupplier.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard(v);
                    editSearchSupplier.clearFocus();
                    return true;
                }
                return false;
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (editSearchSupplier != null) {
                    editSearchSupplier.setText("");
                }
            });
        }

        if (btnResetSearch != null) {
            btnResetSearch.setOnClickListener(v -> {
                if (editSearchSupplier != null) {
                    editSearchSupplier.setText("");
                }
                selectFilter(FilterStatus.ALL);
            });
        }
    }

    private void setupRecyclerView() {
        supplierAdapter = new SupplierAdapter();
        recyclerSuppliers.setLayoutManager(new LinearLayoutManager(this));
        recyclerSuppliers.setAdapter(supplierAdapter);

        supplierAdapter.setOnSupplierClickListener(this::showSupplierDetailsSheet);
        supplierAdapter.setOnSupplierEditListener(this::openEditSupplier);
        supplierAdapter.setOnSupplierStatusToggleListener(this::toggleSupplierStatus);
    }

    private void observeSuppliers() {
        if (supplierRepository.isOnlineMode()) {
            loadOnlineSuppliers();
            return;
        }
        supplierRepository.getAllSuppliers().observe(this, suppliers -> {
            allSuppliers.clear();
            if (suppliers != null) {
                allSuppliers.addAll(suppliers);
            }
            applyFilters();
        });
    }

    private void loadOnlineSuppliers() {
        supplierRepository.fetchSuppliersOnline(new OnlineSupplierRepository.SupplierListCallback() {
            @Override
            public void onSuccess(List<Suppliers> suppliers) {
                runOnUiThread(() -> {
                    allSuppliers.clear();
                    if (suppliers != null) {
                        allSuppliers.addAll(suppliers);
                    }
                    applyFilters();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    showToast("Failed to load suppliers: " + errorMessage);
                    applyFilters();
                });
            }
        });
    }

    private void applyFilters() {
        if (allSuppliers.isEmpty()) {
            layoutListContainer.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.GONE);
            }
            textSupplierCount.setText("0 Suppliers");
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        layoutListContainer.setVisibility(View.VISIBLE);

        List<Suppliers> filteredList = new ArrayList<>();
        String lowerQuery = currentSearchQuery.toLowerCase(Locale.getDefault());

        for (Suppliers supplier : allSuppliers) {
            // 1. Status Filter
            boolean matchesStatus;
            switch (currentFilter) {
                case ACTIVE:
                    matchesStatus = supplier.isActive;
                    break;
                case DEACTIVATED:
                    matchesStatus = !supplier.isActive;
                    break;
                case ALL:
                default:
                    matchesStatus = true;
                    break;
            }

            if (!matchesStatus) {
                continue;
            }

            // 2. Search Query Filter
            if (lowerQuery.isEmpty()) {
                filteredList.add(supplier);
            } else {
                boolean matchesName = supplier.supplierName != null && supplier.supplierName.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesContact = supplier.contactPerson != null && supplier.contactPerson.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesPhone = supplier.phone != null && supplier.phone.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesEmail = supplier.email != null && supplier.email.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesCity = supplier.city != null && supplier.city.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesGst = supplier.gst != null && supplier.gst.toLowerCase(Locale.getDefault()).contains(lowerQuery);

                if (matchesName || matchesContact || matchesPhone || matchesEmail || matchesCity || matchesGst) {
                    filteredList.add(supplier);
                }
            }
        }

        if (filteredList.isEmpty()) {
            recyclerSuppliers.setVisibility(View.GONE);
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.VISIBLE);
            }
            if (textSearchEmptyQuery != null) {
                if (!currentSearchQuery.isEmpty()) {
                    textSearchEmptyQuery.setText("No suppliers found matching \"" + currentSearchQuery + "\"");
                } else {
                    textSearchEmptyQuery.setText("No suppliers match the selected filter status.");
                }
            }
            if (textMetaLabel != null) {
                textMetaLabel.setText("SEARCH RESULTS");
            }
            textSupplierCount.setText("0 Found");
        } else {
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.GONE);
            }
            recyclerSuppliers.setVisibility(View.VISIBLE);
            supplierAdapter.setSuppliers(filteredList);

            if (currentSearchQuery.isEmpty() && currentFilter == FilterStatus.ALL) {
                if (textMetaLabel != null) {
                    textMetaLabel.setText("ALL SUPPLIERS");
                }
                String countText = filteredList.size() + (filteredList.size() == 1 ? " Supplier" : " Suppliers");
                textSupplierCount.setText(countText);
            } else {
                if (textMetaLabel != null) {
                    textMetaLabel.setText(currentSearchQuery.isEmpty() ? getFilterTitle() : "SEARCH RESULTS");
                }
                String countText = filteredList.size() + " of " + allSuppliers.size() + " Found";
                textSupplierCount.setText(countText);
            }
        }
    }

    private String getFilterTitle() {
        switch (currentFilter) {
            case ACTIVE:
                return "ACTIVE SUPPLIERS";
            case DEACTIVATED:
                return "DEACTIVATED SUPPLIERS";
            default:
                return "ALL SUPPLIERS";
        }
    }

    private void toggleSupplierStatus(@NonNull Suppliers supplier) {
        boolean newStatus = !supplier.isActive;
        supplierRepository.updateStatus(supplier.supplierId, newStatus, new SupplierRepository.SupplierActionCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    String actionText = newStatus ? "activated" : "deactivated";
                    showToast("Supplier \"" + supplier.supplierName + "\" " + actionText);
                    if (supplierRepository.isOnlineMode()) {
                        loadOnlineSuppliers();
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showToast("Failed to update status: " + message));
            }
        });
    }

    private void showSupplierDetailsSheet(@NonNull Suppliers supplier) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_supplier_details, null);
        dialog.setContentView(view);

        TextView textName = view.findViewById(R.id.text_detail_supplier_name);
        TextView textContactPerson = view.findViewById(R.id.text_detail_contact_person);
        View btnClose = view.findViewById(R.id.btn_close_supplier_detail);
        View statusBadge = view.findViewById(R.id.layout_detail_status_badge);
        TextView textStatus = view.findViewById(R.id.text_detail_status);
        TextView textSupplierId = view.findViewById(R.id.text_detail_supplier_id);
        TextView textDate = view.findViewById(R.id.text_detail_date);

        View btnCall = view.findViewById(R.id.btn_action_call);
        TextView textQuickCall = view.findViewById(R.id.text_quick_call);
        View btnEmail = view.findViewById(R.id.btn_action_email);
        TextView textQuickEmail = view.findViewById(R.id.text_quick_email);

        View rowPhone = view.findViewById(R.id.row_detail_phone);
        TextView textPhone = view.findViewById(R.id.text_detail_phone);
        View rowEmail = view.findViewById(R.id.row_detail_email);
        TextView textEmail = view.findViewById(R.id.text_detail_email);
        View rowGstPan = view.findViewById(R.id.row_detail_gst_pan);
        TextView textGstPan = view.findViewById(R.id.text_detail_gst_pan);
        View rowAddress = view.findViewById(R.id.row_detail_address);
        TextView textAddress = view.findViewById(R.id.text_detail_address);
        View rowNotes = view.findViewById(R.id.row_detail_notes);
        TextView textNotes = view.findViewById(R.id.text_detail_notes);

        View btnToggle = view.findViewById(R.id.btn_detail_toggle_status);
        ImageView iconToggle = view.findViewById(R.id.icon_detail_toggle);
        TextView textToggle = view.findViewById(R.id.text_detail_toggle_status);
        View btnEdit = view.findViewById(R.id.btn_detail_edit_supplier);

        // Populate fields
        textName.setText(!TextUtils.isEmpty(supplier.supplierName) ? supplier.supplierName : "Unnamed Supplier");
        if (!TextUtils.isEmpty(supplier.contactPerson)) {
            textContactPerson.setVisibility(View.VISIBLE);
            textContactPerson.setText(String.format("Contact: %s", supplier.contactPerson));
        } else {
            textContactPerson.setVisibility(View.GONE);
        }

        textSupplierId.setText(String.format(Locale.getDefault(), "#SUPPLIER-%d", supplier.supplierId));

        if (supplier.createdAt > 0) {
            textDate.setVisibility(View.VISIBLE);
            textDate.setText(String.format("Added %s", dateFormat.format(new Date(supplier.createdAt))));
        } else {
            textDate.setVisibility(View.GONE);
        }

        // Active Status
        if (supplier.isActive) {
            textStatus.setText("Active");
            statusBadge.setBackgroundResource(R.drawable.bg_status_chip);
            textStatus.setTextColor(ContextCompat.getColor(this, R.color.accent_teal));

            textToggle.setText("Deactivate");
            iconToggle.setImageResource(R.drawable.ic_power);
            iconToggle.setColorFilter(ContextCompat.getColor(this, R.color.fg_dim));
        } else {
            textStatus.setText("Deactivated");
            statusBadge.setBackgroundResource(R.drawable.bg_field_normal);
            textStatus.setTextColor(ContextCompat.getColor(this, R.color.fg_dim));

            textToggle.setText("Activate");
            iconToggle.setImageResource(R.drawable.ic_power);
            iconToggle.setColorFilter(ContextCompat.getColor(this, R.color.accent_teal));
        }

        // Phone
        if (!TextUtils.isEmpty(supplier.phone)) {
            rowPhone.setVisibility(View.VISIBLE);
            textPhone.setText(supplier.phone);
            btnCall.setVisibility(View.VISIBLE);
            textQuickCall.setText(supplier.phone);
            btnCall.setOnClickListener(v -> {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + supplier.phone));
                startActivity(dialIntent);
            });
        } else {
            rowPhone.setVisibility(View.GONE);
            btnCall.setVisibility(View.GONE);
        }

        // Email
        if (!TextUtils.isEmpty(supplier.email)) {
            rowEmail.setVisibility(View.VISIBLE);
            textEmail.setText(supplier.email);
            btnEmail.setVisibility(View.VISIBLE);
            textQuickEmail.setText("Email");
            btnEmail.setOnClickListener(v -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + supplier.email));
                startActivity(emailIntent);
            });
        } else {
            rowEmail.setVisibility(View.GONE);
            btnEmail.setVisibility(View.GONE);
        }

        // GST / PAN
        StringBuilder gstPanBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(supplier.gst)) {
            gstPanBuilder.append("GST: ").append(supplier.gst);
        }
        if (!TextUtils.isEmpty(supplier.pan)) {
            if (gstPanBuilder.length() > 0) {
                gstPanBuilder.append(" • ");
            }
            gstPanBuilder.append("PAN: ").append(supplier.pan);
        }
        if (gstPanBuilder.length() > 0) {
            rowGstPan.setVisibility(View.VISIBLE);
            textGstPan.setText(gstPanBuilder.toString());
        } else {
            rowGstPan.setVisibility(View.GONE);
        }

        // Address
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
        if (!TextUtils.isEmpty(supplier.country)) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(supplier.country);
        }

        if (addressBuilder.length() > 0) {
            rowAddress.setVisibility(View.VISIBLE);
            textAddress.setText(addressBuilder.toString());
        } else {
            rowAddress.setVisibility(View.GONE);
        }

        // Notes
        if (!TextUtils.isEmpty(supplier.notes)) {
            rowNotes.setVisibility(View.VISIBLE);
            textNotes.setText(supplier.notes);
        } else {
            rowNotes.setVisibility(View.GONE);
        }

        // Action: Close
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Action: Toggle Status (Deactivate / Activate)
        btnToggle.setOnClickListener(v -> {
            dialog.dismiss();
            toggleSupplierStatus(supplier);
        });

        // Action: Edit
        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            openEditSupplier(supplier);
        });

        dialog.show();
    }

    private void openEditSupplier(@NonNull Suppliers supplier) {
        Intent intent = new Intent(this, AddSupplierActivity.class);
        intent.putExtra("supplier_id", supplier.supplierId);
        intent.putExtra("supplier_name", supplier.supplierName);
        intent.putExtra("contact_person", supplier.contactPerson);
        intent.putExtra("phone", supplier.phone);
        intent.putExtra("email", supplier.email);
        intent.putExtra("address", supplier.address);
        intent.putExtra("city", supplier.city);
        intent.putExtra("state_code", supplier.stateCode);
        intent.putExtra("postal_code", supplier.postalCode);
        intent.putExtra("country", supplier.country);
        intent.putExtra("gst", supplier.gst);
        intent.putExtra("pan", supplier.pan);
        intent.putExtra("notes", supplier.notes);
        intent.putExtra("is_active", supplier.isActive);
        intent.putExtra("created_at", supplier.createdAt);
        startActivity(intent);
        applyTransition(this);
    }

    private void openAddSupplier() {
        navigateTo(AddSupplierActivity.class, false);
    }

    private void focusSearchInput() {
        if (editSearchSupplier != null) {
            editSearchSupplier.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editSearchSupplier, InputMethodManager.SHOW_IMPLICIT);
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
            navView.setCheckedItem(R.id.nav_supplier);
        }
        if (supplierRepository != null && supplierRepository.isOnlineMode()) {
            loadOnlineSuppliers();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_category, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);
        if (searchItem != null && searchItem.getIcon() != null) {
            searchItem.getIcon().setTint(ContextCompat.getColor(this, R.color.white));
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
        if (item.getItemId() == R.id.action_search) {
            focusSearchInput();
            return true;
        } else if (item.getItemId() == R.id.action_theme_toggle) {
            ThemeManager.toggleTheme(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private enum FilterStatus {
        ALL, ACTIVE, DEACTIVATED
    }
}