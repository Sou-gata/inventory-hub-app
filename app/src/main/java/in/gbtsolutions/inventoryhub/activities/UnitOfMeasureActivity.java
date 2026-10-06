package in.gbtsolutions.inventoryhub.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
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

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.UnitOfMeasureAdapter;
import in.gbtsolutions.inventoryhub.helpers.ThemeManager;
import in.gbtsolutions.inventoryhub.models.UnitOfMeasure;
import in.gbtsolutions.inventoryhub.repository.UnitOfMeasureRepository;

public class UnitOfMeasureActivity extends BaseActivity {

    private final List<UnitOfMeasure> allUnits = new ArrayList<>();
    private NavigationView navView;
    private RecyclerView recyclerUom;
    private UnitOfMeasureAdapter uomAdapter;
    private UnitOfMeasureRepository uomRepository;
    private View layoutEmptyState;
    private View layoutListContainer;
    private TextView textUomCount;
    private TextView textMetaLabel;
    private EditText editSearchUom;
    private ImageView btnClearSearch;
    private View layoutSearchEmpty;
    private TextView textSearchEmptyQuery;
    private View btnResetSearch;
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unit_of_measure);

        uomRepository = new UnitOfMeasureRepository(getApplication());

        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        View headerContainer = findViewById(R.id.header_container);
        View contentContainer = findViewById(R.id.uom_content_container);

        // Apply edge-to-edge system insets (status bar & nav bar)
        applyDrawerInsets(drawerLayout, headerContainer, contentContainer, navView);

        // Standardized sidebar and header navigation
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_uom);

        initViews();
        setupRecyclerView();
        observeUnits();
    }

    private void initViews() {
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        layoutListContainer = findViewById(R.id.layout_list_container);
        textUomCount = findViewById(R.id.text_uom_count);
        textMetaLabel = findViewById(R.id.text_meta_label);
        recyclerUom = findViewById(R.id.recycler_uom);

        editSearchUom = findViewById(R.id.edit_search_uom);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        layoutSearchEmpty = findViewById(R.id.layout_search_empty);
        textSearchEmptyQuery = findViewById(R.id.text_search_empty_query);
        btnResetSearch = findViewById(R.id.btn_reset_search);

        FloatingActionButton fabAddUom = findViewById(R.id.fab_add_uom);
        if (fabAddUom != null) {
            fabAddUom.setOnClickListener(v -> openAddUom());
        }

        View btnEmptyAdd = findViewById(R.id.btn_empty_add_uom);
        if (btnEmptyAdd != null) {
            btnEmptyAdd.setOnClickListener(v -> openAddUom());
        }

        setupSearch();
    }

    private void setupSearch() {
        if (editSearchUom != null) {
            editSearchUom.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s != null ? s.toString() : "";
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    filterUnits(query);
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            editSearchUom.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard(v);
                    editSearchUom.clearFocus();
                    return true;
                }
                return false;
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (editSearchUom != null) {
                    editSearchUom.setText("");
                }
            });
        }

        if (btnResetSearch != null) {
            btnResetSearch.setOnClickListener(v -> {
                if (editSearchUom != null) {
                    editSearchUom.setText("");
                }
            });
        }
    }

    private void setupRecyclerView() {
        uomAdapter = new UnitOfMeasureAdapter();
        recyclerUom.setLayoutManager(new LinearLayoutManager(this));
        recyclerUom.setAdapter(uomAdapter);

        uomAdapter.setOnUomClickListener(uom -> {
            if (uom.isDefault) {
                showDefaultUnitInfoDialog(uom);
            } else {
                openEditUom(uom);
            }
        });

        uomAdapter.setOnUomEditListener(this::openEditUom);
        uomAdapter.setOnUomDeleteListener(this::confirmDeleteUom);
    }

    private void observeUnits() {
        uomRepository.getAllUnitsOfMeasure().observe(this, units -> {
            allUnits.clear();
            if (units != null) {
                allUnits.addAll(units);
            }
            filterUnits(currentSearchQuery);
        });
    }

    private void filterUnits(String query) {
        currentSearchQuery = query != null ? query.trim() : "";

        if (allUnits.isEmpty()) {
            layoutListContainer.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.GONE);
            }
            textUomCount.setText("0 Units");
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        layoutListContainer.setVisibility(View.VISIBLE);

        List<UnitOfMeasure> filteredList;
        if (currentSearchQuery.isEmpty()) {
            filteredList = new ArrayList<>(allUnits);
        } else {
            filteredList = new ArrayList<>();
            String lowerQuery = currentSearchQuery.toLowerCase(Locale.getDefault());
            for (UnitOfMeasure uom : allUnits) {
                boolean matchesName = uom.name != null && uom.name.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesDesc = uom.description != null && uom.description.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                if (matchesName || matchesDesc) {
                    filteredList.add(uom);
                }
            }
        }

        if (filteredList.isEmpty()) {
            recyclerUom.setVisibility(View.GONE);
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.VISIBLE);
            }
            if (textSearchEmptyQuery != null) {
                textSearchEmptyQuery.setText("No units found matching \"" + currentSearchQuery + "\"");
            }
            if (textMetaLabel != null) {
                textMetaLabel.setText("SEARCH RESULTS");
            }
            textUomCount.setText("0 Found");
        } else {
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.GONE);
            }
            recyclerUom.setVisibility(View.VISIBLE);
            uomAdapter.setUnits(filteredList);

            if (currentSearchQuery.isEmpty()) {
                if (textMetaLabel != null) {
                    textMetaLabel.setText("ALL UNITS OF MEASURE");
                }
                String countText = filteredList.size() + (filteredList.size() == 1 ? " Unit" : " Units");
                textUomCount.setText(countText);
            } else {
                if (textMetaLabel != null) {
                    textMetaLabel.setText("SEARCH RESULTS");
                }
                String countText = filteredList.size() + " of " + allUnits.size() + " Found";
                textUomCount.setText(countText);
            }
        }
    }

    private void showDefaultUnitInfoDialog(@NonNull UnitOfMeasure uom) {
        new MaterialAlertDialogBuilder(this).setTitle(uom.name + " (Default Unit)").setMessage("This is a system default unit of measure.\n\nDefault units (KG, Gram, Liter, Mililiter, Pics, Packet) are permanent system standards and cannot be edited or deleted.").setPositiveButton("OK", null).show();
    }

    private void confirmDeleteUom(@NonNull UnitOfMeasure uom) {
        if (uom.isDefault) {
            showToast("Default units of measure cannot be deleted.");
            return;
        }

        new MaterialAlertDialogBuilder(this).setTitle("Delete Unit of Measure").setMessage("Are you sure you want to delete \"" + uom.name + "\"? This action cannot be undone.").setPositiveButton("Delete", (dialog, which) -> {
            uomRepository.delete(uom, new UnitOfMeasureRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    showToast("Unit of measure \"" + uom.name + "\" deleted successfully");
                }

                @Override
                public void onError(String message) {
                    showToast(message != null ? message : "Failed to delete unit of measure.");
                }
            });
        }).setNegativeButton("Cancel", null).show();
    }

    private void openEditUom(@NonNull UnitOfMeasure uom) {
        Intent intent = new Intent(this, AddUnitOfMeasureActivity.class);
        intent.putExtra("uom_id", uom.uomId);
        intent.putExtra("uom_name", uom.name);
        intent.putExtra("uom_desc", uom.description);
        intent.putExtra("is_default", uom.isDefault);
        intent.putExtra("created_at", uom.createdAt);
        startActivity(intent);
        applyTransition(this);
    }

    private void openAddUom() {
        navigateTo(AddUnitOfMeasureActivity.class, false);
    }

    private void focusSearchInput() {
        if (editSearchUom != null) {
            editSearchUom.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editSearchUom, InputMethodManager.SHOW_IMPLICIT);
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
            navView.setCheckedItem(R.id.nav_uom);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_unit_of_measure, menu);

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
