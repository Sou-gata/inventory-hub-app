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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.CategoryAdapter;
import in.gbtsolutions.inventoryhub.helpers.ThemeManager;
import in.gbtsolutions.inventoryhub.models.Category;
import in.gbtsolutions.inventoryhub.online.repository.OnlineCategoryRepository;
import in.gbtsolutions.inventoryhub.repository.CategoryRepository;

public class CategoryActivity extends BaseActivity {

    private NavigationView navView;
    private RecyclerView recyclerCategories;
    private CategoryAdapter categoryAdapter;
    private CategoryRepository categoryRepository;

    private View layoutEmptyState;
    private View layoutListContainer;
    private TextView textCategoryCount;
    private TextView textMetaLabel;

    private EditText editSearchCategory;
    private ImageView btnClearSearch;
    private View layoutSearchEmpty;
    private TextView textSearchEmptyQuery;
    private View btnResetSearch;

    private final List<Category> allCategories = new ArrayList<>();
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category);

        categoryRepository = new CategoryRepository(getApplication());

        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);
        View headerContainer = findViewById(R.id.header_container);
        View contentContainer = findViewById(R.id.category_content_container);

        // Apply edge-to-edge system insets (status bar & nav bar)
        applyDrawerInsets(drawerLayout, headerContainer, contentContainer, navView);

        // Standardized sidebar and header navigation
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_category);

        initViews();
        setupRecyclerView();
        observeCategories();
    }

    private void initViews() {
        layoutEmptyState = findViewById(R.id.layout_empty_state);
        layoutListContainer = findViewById(R.id.layout_list_container);
        textCategoryCount = findViewById(R.id.text_category_count);
        textMetaLabel = findViewById(R.id.text_meta_label);
        recyclerCategories = findViewById(R.id.recycler_categories);

        editSearchCategory = findViewById(R.id.edit_search_category);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        layoutSearchEmpty = findViewById(R.id.layout_search_empty);
        textSearchEmptyQuery = findViewById(R.id.text_search_empty_query);
        btnResetSearch = findViewById(R.id.btn_reset_search);

        FloatingActionButton fabAddCategory = findViewById(R.id.fab_add_category);
        if (fabAddCategory != null) {
            fabAddCategory.setOnClickListener(v -> openAddCategory());
        }

        View btnEmptyAdd = findViewById(R.id.btn_empty_add_category);
        if (btnEmptyAdd != null) {
            btnEmptyAdd.setOnClickListener(v -> openAddCategory());
        }

        setupSearch();
    }

    private void setupSearch() {
        if (editSearchCategory != null) {
            editSearchCategory.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s != null ? s.toString() : "";
                    if (btnClearSearch != null) {
                        btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                    filterCategories(query);
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            editSearchCategory.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    hideKeyboard(v);
                    editSearchCategory.clearFocus();
                    return true;
                }
                return false;
            });
        }

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> {
                if (editSearchCategory != null) {
                    editSearchCategory.setText("");
                }
            });
        }

        if (btnResetSearch != null) {
            btnResetSearch.setOnClickListener(v -> {
                if (editSearchCategory != null) {
                    editSearchCategory.setText("");
                }
            });
        }
    }

    private void setupRecyclerView() {
        categoryAdapter = new CategoryAdapter();
        recyclerCategories.setLayoutManager(new LinearLayoutManager(this));
        recyclerCategories.setAdapter(categoryAdapter);

        categoryAdapter.setOnCategoryClickListener(this::openEditCategory);
        categoryAdapter.setOnCategoryEditListener(this::openEditCategory);
    }

    private void observeCategories() {
        if (categoryRepository.isOnlineMode()) {
            loadOnlineCategories();
        } else {
            categoryRepository.getAllCategories().observe(this, categories -> {
                allCategories.clear();
                if (categories != null) {
                    allCategories.addAll(categories);
                }
                filterCategories(currentSearchQuery);
            });
        }
    }

    private void loadOnlineCategories() {
        categoryRepository.fetchCategoriesOnline(new OnlineCategoryRepository.CategoryListCallback() {
            @Override
            public void onSuccess(List<Category> categories) {
                allCategories.clear();
                if (categories != null) {
                    allCategories.addAll(categories);
                }
                filterCategories(currentSearchQuery);
            }

            @Override
            public void onError(String errorMessage) {
                android.widget.Toast.makeText(CategoryActivity.this, "Failed to load categories: " + errorMessage, android.widget.Toast.LENGTH_LONG).show();
            }
        });
    }

    private void filterCategories(String query) {
        currentSearchQuery = query != null ? query.trim() : "";

        if (allCategories.isEmpty()) {
            layoutListContainer.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.GONE);
            }
            textCategoryCount.setText("0 Categories");
            return;
        }

        layoutEmptyState.setVisibility(View.GONE);
        layoutListContainer.setVisibility(View.VISIBLE);

        List<Category> filteredList;
        if (currentSearchQuery.isEmpty()) {
            filteredList = new ArrayList<>(allCategories);
        } else {
            filteredList = new ArrayList<>();
            String lowerQuery = currentSearchQuery.toLowerCase(Locale.getDefault());
            for (Category category : allCategories) {
                boolean matchesName = category.categoryName != null &&
                        category.categoryName.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                boolean matchesDesc = category.description != null &&
                        category.description.toLowerCase(Locale.getDefault()).contains(lowerQuery);
                if (matchesName || matchesDesc) {
                    filteredList.add(category);
                }
            }
        }

        if (filteredList.isEmpty()) {
            recyclerCategories.setVisibility(View.GONE);
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.VISIBLE);
            }
            if (textSearchEmptyQuery != null) {
                textSearchEmptyQuery.setText("No categories found matching \"" + currentSearchQuery + "\"");
            }
            if (textMetaLabel != null) {
                textMetaLabel.setText("SEARCH RESULTS");
            }
            textCategoryCount.setText("0 Found");
        } else {
            if (layoutSearchEmpty != null) {
                layoutSearchEmpty.setVisibility(View.GONE);
            }
            recyclerCategories.setVisibility(View.VISIBLE);
            categoryAdapter.setCategories(filteredList);

            if (currentSearchQuery.isEmpty()) {
                if (textMetaLabel != null) {
                    textMetaLabel.setText("ALL CATEGORIES");
                }
                String countText = filteredList.size() + (filteredList.size() == 1 ? " Category" : " Categories");
                textCategoryCount.setText(countText);
            } else {
                if (textMetaLabel != null) {
                    textMetaLabel.setText("SEARCH RESULTS");
                }
                String countText = filteredList.size() + " of " + allCategories.size() + " Found";
                textCategoryCount.setText(countText);
            }
        }
    }

    private void focusSearchInput() {
        if (editSearchCategory != null) {
            editSearchCategory.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(editSearchCategory, InputMethodManager.SHOW_IMPLICIT);
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

    private void openEditCategory(@NonNull Category category) {
        Intent intent = new Intent(this, AddCategoryActivity.class);
        intent.putExtra("category_id", category.categoryId);
        intent.putExtra("category_name", category.categoryName);
        intent.putExtra("category_desc", category.description);
        intent.putExtra("category_icon", category.icon);
        intent.putExtra("created_at", category.createdAt);
        startActivity(intent);
        applyTransition(this);
    }

    private void openAddCategory() {
        navigateTo(AddCategoryActivity.class, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_category);
        }
        if (categoryRepository != null && categoryRepository.isOnlineMode()) {
            loadOnlineCategories();
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