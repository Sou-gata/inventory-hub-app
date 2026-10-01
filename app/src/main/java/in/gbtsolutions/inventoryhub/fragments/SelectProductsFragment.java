package in.gbtsolutions.inventoryhub.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.SellProductAdapter;
import in.gbtsolutions.inventoryhub.helpers.SellCartManager;
import in.gbtsolutions.inventoryhub.models.Category;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.online.paging.EndlessRecyclerScrollListener;
import in.gbtsolutions.inventoryhub.online.repository.OnlineCategoryRepository;
import in.gbtsolutions.inventoryhub.online.repository.OnlineProductRepository;
import in.gbtsolutions.inventoryhub.repository.CategoryRepository;
import in.gbtsolutions.inventoryhub.repository.ProductRepository;
import in.gbtsolutions.inventoryhub.views.BatchSelectDialog;

public class SelectProductsFragment extends Fragment {

    private final List<Product> masterProductList = new ArrayList<>();
    private final Map<Integer, String> categoryNames = new HashMap<>();
    private EditText editSearch;
    private ImageView btnClearSearch;
    private RecyclerView recyclerView;
    private AppModeManager appModeManager;
    private ProductRepository productRepository;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private LinearLayout layoutEmptyProducts;
    private LinearLayout layoutSearchEmpty;
    private TextView textSearchEmptyQuery;
    private FrameLayout btnResetSearch;
    private SellProductAdapter adapter;
    private EndlessRecyclerScrollListener endlessScrollListener;
    private boolean onlineIsLoading = false;
    private boolean onlineHasMore = true;
    private int onlineCurrentPage = 1;

    private final SellCartManager.OnCartChangedListener cartListener = () -> {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    };
    private String currentQuery = "";

    public static SelectProductsFragment newInstance() {
        return new SelectProductsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_products, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupSearch();
        observeData();
    }

    @Override
    public void onStart() {
        super.onStart();
        SellCartManager.getInstance().addListener(cartListener);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        SellCartManager.getInstance().removeListener(cartListener);
    }

    private void initViews(View view) {
        editSearch = view.findViewById(R.id.edit_search_products);
        btnClearSearch = view.findViewById(R.id.btn_clear_search);
        recyclerView = view.findViewById(R.id.recycler_select_products);
        layoutEmptyProducts = view.findViewById(R.id.layout_empty_select_products);
        layoutSearchEmpty = view.findViewById(R.id.layout_search_empty);
        textSearchEmptyQuery = view.findViewById(R.id.text_search_empty_query);
        btnResetSearch = view.findViewById(R.id.btn_reset_search);
    }

    private void setupRecyclerView() {
        adapter = new SellProductAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);

        endlessScrollListener = new EndlessRecyclerScrollListener(layoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                if (appModeManager != null && appModeManager.isOnlineMode() && onlineHasMore && !onlineIsLoading) {
                    loadOnlineProducts(page, false);
                }
            }
        };
        recyclerView.addOnScrollListener(endlessScrollListener);

        adapter.setOnAddToCartClickListener(product -> {
            boolean batchMode = product.batchEnabled;
            if (batchMode) {
                BatchSelectDialog dialog = BatchSelectDialog.newInstance(product);
                dialog.setOnBatchesSelectedListener((prod, selections) -> {
                    boolean wasInCart = SellCartManager.getInstance().isInCart(prod.productId);
                    SellCartManager.getInstance().removeProduct(prod.productId);
                    if (selections != null) {
                        for (BatchSelectDialog.BatchSelection sel : selections) {
                            if (sel != null && sel.batch != null) {
                                SellCartManager.getInstance().addBatchItem(prod, sel.batch, sel.quantity);
                            }
                        }
                    }
                    if (adapter != null) adapter.notifyDataSetChanged();
                    if (getContext() != null) {
                        String name = (prod.productName != null && !prod.productName.trim().isEmpty())
                                ? prod.productName.trim()
                                : "Product";
                        if (!selections.isEmpty()) {
                            Toast.makeText(getContext(), name + " added to cart", Toast.LENGTH_SHORT).show();
                        } else if (wasInCart) {
                            Toast.makeText(getContext(), name + " removed from cart", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                dialog.show(getParentFragmentManager(), "BatchSelectDialog");
            } else {
                boolean wasInCart = SellCartManager.getInstance().isInCart(product.productId);
                SellCartManager.getInstance().toggleProduct(product);
                if (adapter != null) adapter.notifyDataSetChanged();
                if (getContext() != null) {
                    String name = (product.productName != null && !product.productName.trim().isEmpty())
                            ? product.productName.trim()
                            : "Product";
                    if (wasInCart) {
                        Toast.makeText(getContext(), name + " removed from cart", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), name + " added to cart", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void setupSearch() {
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim() : "";
                btnClearSearch.setVisibility(TextUtils.isEmpty(currentQuery) ? View.GONE : View.VISIBLE);
                if (appModeManager != null && appModeManager.isOnlineMode()) {
                    if (searchRunnable != null) {
                        searchHandler.removeCallbacks(searchRunnable);
                    }
                    searchRunnable = () -> loadOnlineProducts(1, true);
                    searchHandler.postDelayed(searchRunnable, 400);
                } else {
                    applyFilter();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnClearSearch.setOnClickListener(v -> editSearch.setText(""));
        btnResetSearch.setOnClickListener(v -> editSearch.setText(""));
    }

    private void loadOnlineProducts(int page, boolean isNewSearch) {
        if (productRepository == null) return;
        if (onlineIsLoading && !isNewSearch) return;
        onlineIsLoading = true;

        if (isNewSearch) {
            onlineCurrentPage = 1;
            onlineHasMore = true;
            if (endlessScrollListener != null) {
                endlessScrollListener.resetState();
            }
            adapter.clearProducts();
        }

        productRepository.fetchProductsPaged(page, EndlessRecyclerScrollListener.PAGE_SIZE, currentQuery, "all", null,
                new OnlineProductRepository.PagedProductCallback() {
                    @Override
                    public void onSuccess(List<Product> products, int currentPage, int totalPages, int totalRecords, boolean hasMore) {
                        if (!isAdded()) return;
                        onlineIsLoading = false;
                        onlineCurrentPage = currentPage;
                        onlineHasMore = hasMore;
                        if (endlessScrollListener != null) {
                            endlessScrollListener.setHasMore(hasMore);
                            endlessScrollListener.setLoading(false);
                        }

                        if (isNewSearch) {
                            adapter.setProducts(products);
                        } else {
                            adapter.addProducts(products);
                        }

                        int totalLoaded = adapter.getProducts().size();
                        if (totalLoaded == 0) {
                            recyclerView.setVisibility(View.GONE);
                            if (currentQuery.isEmpty()) {
                                layoutSearchEmpty.setVisibility(View.GONE);
                                layoutEmptyProducts.setVisibility(View.VISIBLE);
                            } else {
                                layoutEmptyProducts.setVisibility(View.GONE);
                                layoutSearchEmpty.setVisibility(View.VISIBLE);
                                if (textSearchEmptyQuery != null) {
                                    textSearchEmptyQuery.setText(String.format("No products match \"%s\"", currentQuery));
                                }
                            }
                        } else {
                            layoutEmptyProducts.setVisibility(View.GONE);
                            layoutSearchEmpty.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onError(String errorMessage, boolean sessionExpired) {
                        if (!isAdded() || getContext() == null) return;
                        onlineIsLoading = false;
                        if (endlessScrollListener != null) {
                            endlessScrollListener.setLoading(false);
                        }
                        Toast.makeText(getContext(), "Cloud: " + errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void observeData() {
        if (getActivity() == null) return;

        appModeManager = AppModeManager.getInstance(requireContext());
        productRepository = new ProductRepository(getActivity().getApplication());
        CategoryRepository categoryRepository = new CategoryRepository(getActivity().getApplication());

        if (appModeManager.isOnlineMode()) {
            categoryRepository.fetchCategoriesOnline(new OnlineCategoryRepository.CategoryListCallback() {
                @Override
                public void onSuccess(List<Category> categories) {
                    categoryNames.clear();
                    if (categories != null) {
                        for (Category cat : categories) {
                            if (cat.categoryName != null) {
                                categoryNames.put(cat.categoryId, cat.categoryName);
                            }
                        }
                    }
                    adapter.setCategories(categories);
                }

                @Override
                public void onError(String errorMessage) {}
            });
            loadOnlineProducts(1, true);
            return;
        }

        categoryRepository.getAllCategories().observe(getViewLifecycleOwner(), categories -> {
            categoryNames.clear();
            if (categories != null) {
                for (Category cat : categories) {
                    if (cat.categoryName != null) {
                        categoryNames.put(cat.categoryId, cat.categoryName);
                    }
                }
            }
            adapter.setCategories(categories);
            applyFilter();
        });

        productRepository.getAllProducts().observe(getViewLifecycleOwner(), products -> {
            masterProductList.clear();
            if (products != null) {
                masterProductList.addAll(products);
            }
            applyFilter();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (searchRunnable != null) {
            searchHandler.removeCallbacks(searchRunnable);
        }
    }

    private void applyFilter() {
        if (masterProductList.isEmpty()) {
            adapter.setProducts(new ArrayList<>());
            recyclerView.setVisibility(View.GONE);
            layoutSearchEmpty.setVisibility(View.GONE);
            layoutEmptyProducts.setVisibility(View.VISIBLE);
            return;
        }

        List<Product> filteredList = new ArrayList<>();
        String queryLower = currentQuery.toLowerCase(Locale.getDefault());

        for (Product product : masterProductList) {
            if (TextUtils.isEmpty(queryLower)) {
                filteredList.add(product);
                continue;
            }

            boolean matchesName = product.productName != null && product.productName.toLowerCase(Locale.getDefault()).contains(queryLower);
            boolean matchesSku = product.sku != null && product.sku.toLowerCase(Locale.getDefault()).contains(queryLower);
            boolean matchesBrand = product.brand != null && product.brand.toLowerCase(Locale.getDefault()).contains(queryLower);

            String catName = categoryNames.get(product.categoryId);
            boolean matchesCat = catName != null && catName.toLowerCase(Locale.getDefault()).contains(queryLower);

            if (matchesName || matchesSku || matchesBrand || matchesCat) {
                filteredList.add(product);
            }
        }

        adapter.setProducts(filteredList);

        if (filteredList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            layoutEmptyProducts.setVisibility(View.GONE);
            layoutSearchEmpty.setVisibility(View.VISIBLE);
            textSearchEmptyQuery.setText(String.format(Locale.getDefault(), "No products matching \"%s\"", currentQuery));
        } else {
            layoutSearchEmpty.setVisibility(View.GONE);
            layoutEmptyProducts.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
}
