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
import in.gbtsolutions.inventoryhub.adapters.BuyerAdapter;
import in.gbtsolutions.inventoryhub.helpers.ThemeManager;
import in.gbtsolutions.inventoryhub.models.Buyer;
import in.gbtsolutions.inventoryhub.online.repository.OnlineBuyerRepository;
import in.gbtsolutions.inventoryhub.repository.BuyerRepository;

public class BuyerActivity extends BaseActivity {

    private enum FilterStatus {
        ALL,
        ACTIVE,
        DEACTIVATED
    }

    private NavigationView navView;
    private RecyclerView recyclerBuyers;
    private BuyerAdapter buyerAdapter;
    private BuyerRepository buyerRepository;

    private View layoutEmptyState;
    private View layoutListContainer;
    private TextView textBuyerCount;
    private TextView textMetaLabel;

    private EditText editSearchBuyer;
    private ImageView btnClearSearch;
    private View layoutSearchEmpty;
    private TextView textSearchEmptyQuery;
    private View btnResetSearch;

    private TextView chipFilterAll;
    private TextView chipFilterActive;
    private TextView chipFilterDeactive;

    private final List<Buyer> allBuyers = new ArrayList<>();
    private String currentSearchQuery = "";
    private FilterStatus currentFilter = FilterStatus.ALL;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buyer);

        buyerRepository = new BuyerRepository(getApplication());

        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        View headerContainer = findViewById(R.id.header_container);
        View contentContainer = findViewById(R.id.buyer_content_container);

        // Apply edge-to-edge system insets
        applyDrawerInsets(drawerLayout, headerContainer, contentContainer, navView);

        // Standardized sidebar and header navigation
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_buyer);

        initViews();
        setupFilterChips();
        setupRecyclerView();
        observeBuyers();
    }

    private void initViews() {
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        layoutListContainer = findViewById(R.id.layout_list_container);
        textBuyerCount = findViewById(R.id.text_buyer_count);
        textMetaLabel = findViewById(R.id.text_meta_label);
        recyclerBuyers = findViewById(R.id.recycler_buyers);

        editSearchBuyer = findViewById(R.id.edit_search_buyer);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        layoutSearchEmpty = findViewById(R.id.layout_search_empty);
        textSearchEmptyQuery = findViewById(R.id.text_search_empty_query);
        btnResetSearch = findViewById(R.id.btn_reset_search);

        chipFilterAll = findViewById(R.id.chip_filter_all);
        chipFilterActive = findViewById(R.id.chip_filter_active);
        chipFilterDeactive = findViewById(R.id.chip_filter_deactive);

        FloatingActionButton fabAddBuyer = findViewById(R.id.fab_add_buyer);
        if (fabAddBuyer != null) {
            fabAddBuyer.setOnClickListener(v -> openAddBuyer());
        }

        View btnEmptyAdd = findViewById(R.id.btn_empty_add_buyer);
        if (btnEmptyAdd != null) {
            btnEmptyAdd.setOnClickListener(v -> openAddBuyer());
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
        if (editSearchBuyer != null) {
            editSearchBuyer.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

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
                public void afterTextChanged(Editable s) {}
            });

            editSearchBuyer.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard(v);
                    editSearchBuyer.clearFocus();
                    return true;
                }
                return false;
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (editSearchBuyer != null) {
                    editSearchBuyer.setText("");
                }
            });
        }

        if (btnResetSearch != null) {
            btnResetSearch.setOnClickListener(v -> {
                if (editSearchBuyer != null) {
                    editSearchBuyer.setText("");
                }
                selectFilter(FilterStatus.ALL);
            });
        }
    }

    private void setupRecyclerView() {
        buyerAdapter = new BuyerAdapter();
        recyclerBuyers.setLayoutManager(new LinearLayoutManager(this));
        recyclerBuyers.setAdapter(buyerAdapter);

        buyerAdapter.setOnBuyerClickListener(this::showBuyerDetailsSheet);
        buyerAdapter.setOnBuyerEditListener(this::openEditBuyer);
        buyerAdapter.setOnBuyerStatusToggleListener(this::toggleBuyerStatus);
    }

    private void observeBuyers() {
        if (buyerRepository.isOnlineMode()) {
            loadOnlineBuyers();
            return;
        }
        buyerRepository.getAllBuyers().observe(this, buyers -> {
            allBuyers.clear();
            if (buyers != null) {
                allBuyers.addAll(buyers);
            }
            applyFilters();
        });
    }

    private void loadOnlineBuyers() {
        buyerRepository.fetchBuyersOnline(new OnlineBuyerRepository.BuyerListCallback() {
            @Override
            public void onSuccess(List<Buyer> buyers) {
                runOnUiThread(() -> {
                    allBuyers.clear();
                    if (buyers != null) {
                        allBuyers.addAll(buyers);
                    }
                    applyFilters();
                });
            }

            @Override
            public void onError(String errorMessage) {
                runOnUiThread(() -> {
                    showToast("Failed to load buyers: " + errorMessage);
                    applyFilters();
                });
            }
        });
    }

    private void applyFilters() {
        if (allBuyers.isEmpty()) {
            layoutListContainer.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.GONE);
            }
            textBuyerCount.setText("0 Buyers");
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        layoutListContainer.setVisibility(View.VISIBLE);

        List<Buyer> filteredList = new ArrayList<>();
        String lowerQuery = currentSearchQuery.toLowerCase(Locale.getDefault());

        for (Buyer buyer : allBuyers) {
            // 1. Status Filter
            boolean matchesStatus;
            switch (currentFilter) {
                case ACTIVE:
                    matchesStatus = buyer.isActive;
                    break;
                case DEACTIVATED:
                    matchesStatus = !buyer.isActive;
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
                filteredList.add(buyer);
            } else {
                boolean matchesName = buyer.buyerName != null && buyer.buyerName.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesContact = buyer.contactPerson != null && buyer.contactPerson.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesPhone = buyer.phone != null && buyer.phone.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesEmail = buyer.email != null && buyer.email.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesCity = buyer.city != null && buyer.city.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesGst = buyer.gst != null && buyer.gst.toLowerCase(Locale.getDefault()).contains(lowerQuery);

                if (matchesName || matchesContact || matchesPhone || matchesEmail || matchesCity || matchesGst) {
                    filteredList.add(buyer);
                }
            }
        }

        if (filteredList.isEmpty()) {
            recyclerBuyers.setVisibility(View.GONE);
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.VISIBLE);
            }
            if (textSearchEmptyQuery != null) {
                if (!currentSearchQuery.isEmpty()) {
                    textSearchEmptyQuery.setText("No buyers found matching \"" + currentSearchQuery + "\"");
                } else {
                    textSearchEmptyQuery.setText("No buyers match the selected filter status.");
                }
            }
            if (textMetaLabel != null) {
                textMetaLabel.setText("SEARCH RESULTS");
            }
            textBuyerCount.setText("0 Found");
        } else {
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.GONE);
            }
            recyclerBuyers.setVisibility(View.VISIBLE);
            buyerAdapter.setBuyers(filteredList);

            if (currentSearchQuery.isEmpty() && currentFilter == FilterStatus.ALL) {
                if (textMetaLabel != null) {
                    textMetaLabel.setText("ALL BUYERS");
                }
                String countText = filteredList.size() + (filteredList.size() == 1 ? " Buyer" : " Buyers");
                textBuyerCount.setText(countText);
            } else {
                if (textMetaLabel != null) {
                    textMetaLabel.setText(currentSearchQuery.isEmpty() ? getFilterTitle() : "SEARCH RESULTS");
                }
                String countText = filteredList.size() + " of " + allBuyers.size() + " Found";
                textBuyerCount.setText(countText);
            }
        }
    }

    private String getFilterTitle() {
        switch (currentFilter) {
            case ACTIVE:
                return "ACTIVE BUYERS";
            case DEACTIVATED:
                return "DEACTIVATED BUYERS";
            default:
                return "ALL BUYERS";
        }
    }

    private void toggleBuyerStatus(@NonNull Buyer buyer) {
        boolean newStatus = !buyer.isActive;
        buyerRepository.updateStatus(buyer.buyerId, newStatus, new BuyerRepository.BuyerActionCallback() {
            @Override
            public void onSuccess() {
                runOnUiThread(() -> {
                    String actionText = newStatus ? "activated" : "deactivated";
                    showToast("Buyer \"" + buyer.buyerName + "\" " + actionText);
                    if (buyerRepository.isOnlineMode()) {
                        loadOnlineBuyers();
                    }
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> showToast("Failed to update status: " + message));
            }
        });
    }

    private void showBuyerDetailsSheet(@NonNull Buyer buyer) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_buyer_details, null);
        dialog.setContentView(view);

        TextView textName = view.findViewById(R.id.text_detail_buyer_name);
        TextView textContactPerson = view.findViewById(R.id.text_detail_contact_person);
        View btnClose = view.findViewById(R.id.btn_close_buyer_detail);
        View statusBadge = view.findViewById(R.id.layout_detail_status_badge);
        TextView textStatus = view.findViewById(R.id.text_detail_status);
        TextView textBuyerId = view.findViewById(R.id.text_detail_buyer_id);
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
        View btnEdit = view.findViewById(R.id.btn_detail_edit_buyer);

        // Populate fields
        textName.setText(!TextUtils.isEmpty(buyer.buyerName) ? buyer.buyerName : "Unnamed Buyer");
        if (!TextUtils.isEmpty(buyer.contactPerson)) {
            textContactPerson.setVisibility(View.VISIBLE);
            textContactPerson.setText(String.format("Contact: %s", buyer.contactPerson));
        } else {
            textContactPerson.setVisibility(View.GONE);
        }

        textBuyerId.setText(String.format(Locale.getDefault(), "#BUYER-%d", buyer.buyerId));

        if (buyer.createdAt > 0) {
            textDate.setVisibility(View.VISIBLE);
            textDate.setText(String.format("Added %s", dateFormat.format(new Date(buyer.createdAt))));
        } else {
            textDate.setVisibility(View.GONE);
        }

        // Active Status
        if (buyer.isActive) {
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
        if (!TextUtils.isEmpty(buyer.phone)) {
            rowPhone.setVisibility(View.VISIBLE);
            textPhone.setText(buyer.phone);
            btnCall.setVisibility(View.VISIBLE);
            textQuickCall.setText(buyer.phone);
            btnCall.setOnClickListener(v -> {
                Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + buyer.phone));
                startActivity(dialIntent);
            });
        } else {
            rowPhone.setVisibility(View.GONE);
            btnCall.setVisibility(View.GONE);
        }

        // Email
        if (!TextUtils.isEmpty(buyer.email)) {
            rowEmail.setVisibility(View.VISIBLE);
            textEmail.setText(buyer.email);
            btnEmail.setVisibility(View.VISIBLE);
            textQuickEmail.setText("Email");
            btnEmail.setOnClickListener(v -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + buyer.email));
                startActivity(emailIntent);
            });
        } else {
            rowEmail.setVisibility(View.GONE);
            btnEmail.setVisibility(View.GONE);
        }

        // GST / PAN
        StringBuilder gstPanBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(buyer.gst)) {
            gstPanBuilder.append("GST: ").append(buyer.gst);
        }
        if (!TextUtils.isEmpty(buyer.pan)) {
            if (gstPanBuilder.length() > 0) {
                gstPanBuilder.append(" • ");
            }
            gstPanBuilder.append("PAN: ").append(buyer.pan);
        }
        if (gstPanBuilder.length() > 0) {
            rowGstPan.setVisibility(View.VISIBLE);
            textGstPan.setText(gstPanBuilder.toString());
        } else {
            rowGstPan.setVisibility(View.GONE);
        }

        // Address
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
        if (!TextUtils.isEmpty(buyer.country)) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(buyer.country);
        }

        if (addressBuilder.length() > 0) {
            rowAddress.setVisibility(View.VISIBLE);
            textAddress.setText(addressBuilder.toString());
        } else {
            rowAddress.setVisibility(View.GONE);
        }

        // Notes
        if (!TextUtils.isEmpty(buyer.notes)) {
            rowNotes.setVisibility(View.VISIBLE);
            textNotes.setText(buyer.notes);
        } else {
            rowNotes.setVisibility(View.GONE);
        }

        // Action: Close
        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Action: Toggle Status (Deactivate / Activate)
        btnToggle.setOnClickListener(v -> {
            dialog.dismiss();
            toggleBuyerStatus(buyer);
        });

        // Action: Edit
        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            openEditBuyer(buyer);
        });

        dialog.show();
    }

    private void openEditBuyer(@NonNull Buyer buyer) {
        Intent intent = new Intent(this, AddBuyerActivity.class);
        intent.putExtra("buyer_id", buyer.buyerId);
        intent.putExtra("buyer_name", buyer.buyerName);
        intent.putExtra("contact_person", buyer.contactPerson);
        intent.putExtra("phone", buyer.phone);
        intent.putExtra("email", buyer.email);
        intent.putExtra("address", buyer.address);
        intent.putExtra("city", buyer.city);
        intent.putExtra("state_code", buyer.stateCode);
        intent.putExtra("postal_code", buyer.postalCode);
        intent.putExtra("country", buyer.country);
        intent.putExtra("gst", buyer.gst);
        intent.putExtra("pan", buyer.pan);
        intent.putExtra("notes", buyer.notes);
        intent.putExtra("is_active", buyer.isActive);
        intent.putExtra("created_at", buyer.createdAt);
        startActivity(intent);
        applyTransition(this);
    }

    private void openAddBuyer() {
        navigateTo(AddBuyerActivity.class, false);
    }

    private void focusSearchInput() {
        if (editSearchBuyer != null) {
            editSearchBuyer.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editSearchBuyer, InputMethodManager.SHOW_IMPLICIT);
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
            navView.setCheckedItem(R.id.nav_buyer);
        }
        if (buyerRepository != null && buyerRepository.isOnlineMode()) {
            loadOnlineBuyers();
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
}