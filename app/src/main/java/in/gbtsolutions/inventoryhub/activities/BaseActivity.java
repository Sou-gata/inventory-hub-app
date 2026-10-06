package in.gbtsolutions.inventoryhub.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.ColorRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuView;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.drawable.Drawable;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Configurations;
import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.ThemeManager;
import in.gbtsolutions.inventoryhub.models.User;
import in.gbtsolutions.inventoryhub.online.config.AppMode;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;

public abstract class BaseActivity extends AppCompatActivity {

    private final Map<Integer, CollapsibleMenuSection> collapsibleSections = new HashMap<>();
    private final Map<Integer, CollapsibleMenuSection> childToSectionMap = new HashMap<>();
    protected final int[] userMenus = {R.id.nav_home, R.id.nav_inventory, R.id.nav_purchase, R.id.nav_purchase_new_order, R.id.nav_purchase_history, R.id.nav_receive, R.id.nav_sell, R.id.nav_sell_create, R.id.nav_sell_history, R.id.nav_reports, R.id.nav_reports_gstr1, R.id.nav_reports_sales_register, R.id.nav_reports_purchase_register, R.id.nav_audit_trail, R.id.nav_profile, R.id.nav_about};
    @Nullable
    private NavigationView activeNavView = null;
    @Nullable
    private DrawerLayout currentDrawerLayout = null;
    @Nullable
    private Toolbar currentToolbar = null;
    private AppModeManager appModeManager;
    private AppModeManager.OnModeChangeListener baseModeChangeListener;
    // Cached pre-computed density values to avoid recurring conversions during scrolling and layout passes
    private int dimenSubmenuHeight;
    private int dimenSubmenuPaddingStart;
    private int dimenSubmenuPaddingEnd;
    private int dimenStandardPaddingStart;
    private int dimenStandardPaddingEnd;
    private int dimenChevronSize;
    private int dimenChevronMarginEnd;
    private boolean dimensInitialized = false;

    @SuppressWarnings("deprecation")
    public static void applyOpenTransition(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_right, R.anim.slide_out_left_subtle);
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left_subtle, R.anim.slide_out_right);
        } else {
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left_subtle);
        }
    }

    @SuppressWarnings("deprecation")
    public static void applyCloseTransition(@NonNull Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            activity.overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left_subtle, R.anim.slide_out_right);
        } else {
            activity.overridePendingTransition(R.anim.slide_in_left_subtle, R.anim.slide_out_right);
        }
    }

    public static void applyTransition(@NonNull Activity activity) {
        applyOpenTransition(activity);
    }

    public static void applySlideTransition(@NonNull Activity activity) {
        applyOpenTransition(activity);
    }

    public static void applyFadeTransition(@NonNull Activity activity) {
        applyOpenTransition(activity);
    }

    @NonNull
    private static String getInitials(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) return "";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        } else if (parts.length >= 2) {
            String first = parts[0].substring(0, 1);
            String last = parts[parts.length - 1].substring(0, 1);
            return (first + last).toUpperCase();
        }
        return "";
    }

    private void ensureDimensions() {
        if (!dimensInitialized) {
            float density = getResources().getDisplayMetrics().density;
            dimenSubmenuHeight = (int) (40 * density);
            dimenSubmenuPaddingStart = (int) (52 * density);
            dimenSubmenuPaddingEnd = (int) (16 * density);
            dimenStandardPaddingStart = (int) (24 * density);
            dimenStandardPaddingEnd = (int) (24 * density);
            dimenChevronSize = (int) (18 * density);
            dimenChevronMarginEnd = (int) (16 * density);
            dimensInitialized = true;
        }
    }

    private void initCollapsibleSections() {
        if (collapsibleSections.isEmpty()) {
            registerCollapsibleSection(new CollapsibleMenuSection(
                    R.id.nav_purchase, R.id.nav_purchase_new_order, R.id.nav_purchase_history));
            registerCollapsibleSection(new CollapsibleMenuSection(
                    R.id.nav_sell, R.id.nav_sell_create, R.id.nav_sell_history));
            registerCollapsibleSection(new CollapsibleMenuSection(
                    R.id.nav_reports,
                    R.id.nav_reports_gstr1,
                    R.id.nav_reports_sales_register,
                    R.id.nav_reports_purchase_register,
                    R.id.nav_audit_trail));
        }
    }

    private void registerCollapsibleSection(@NonNull CollapsibleMenuSection section) {
        collapsibleSections.put(section.parentId, section);
        for (int childId : section.childIds) {
            childToSectionMap.put(childId, section);
        }
    }

    @Nullable
    private CollapsibleMenuSection findSectionByParent(int parentId) {
        initCollapsibleSections();
        return collapsibleSections.get(parentId);
    }

    @Nullable
    private CollapsibleMenuSection findSectionByChild(int childId) {
        initCollapsibleSections();
        return childToSectionMap.get(childId);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        if (isEdgeToEdgeEnabled()) {
            EdgeToEdge.enable(this);
        }
        super.onCreate(savedInstanceState);
        setupStatusBar();
        ensureModeListener();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupStatusBar();
        if (activeNavView != null) {
            applyMenuVisibility(activeNavView, isCurrentUserAdmin());
            activeNavView.post(() -> refreshAllNavigationItems(activeNavView));
        }
        updateNavHeaderModeBadge();
    }

    @Override
    protected void onDestroy() {
        if (baseModeChangeListener != null && appModeManager != null) {
            appModeManager.removeOnModeChangeListener(baseModeChangeListener);
            baseModeChangeListener = null;
        }
        for (CollapsibleMenuSection section : collapsibleSections.values()) {
            if (section.animator != null) {
                section.animator.cancel();
                section.animator = null;
            }
        }
        activeNavView = null;
        super.onDestroy();
    }

    protected boolean isEdgeToEdgeEnabled() {
        return true;
    }

    @ColorRes
    protected int getHeaderColorRes() {
        return R.color.toolbar_bg;
    }

    @SuppressWarnings("deprecation")
    protected void setupStatusBar() {
        if (getWindow() == null) return;

        boolean isDark = ThemeManager.isDarkMode(this);
        // On Android 15+ (API 35+), EdgeToEdge handles status bar transparency automatically
        if (Build.VERSION.SDK_INT < 35) {
            if (isEdgeToEdgeEnabled()) {
                getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            } else {
                getWindow().setStatusBarColor(ContextCompat.getColor(this, getHeaderColorRes()));
            }
        }
        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insetsController.setAppearanceLightStatusBars(false);
        insetsController.setAppearanceLightNavigationBars(!isDark);
    }

    protected void applyHeaderInsets(@NonNull View headerView) {
        int initialLeft = headerView.getPaddingLeft();
        int initialRight = headerView.getPaddingRight();
        int initialBottom = headerView.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(headerView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(initialLeft, systemBars.top, initialRight, initialBottom);
            return insets;
        });
    }

    protected void applyEdgeToEdgeInsets(@Nullable View headerView, @Nullable View contentView) {
        View decorView = getWindow().getDecorView();
        ViewCompat.setOnApplyWindowInsetsListener(decorView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (headerView != null) {
                headerView.setPadding(systemBars.left, systemBars.top, systemBars.right, headerView.getPaddingBottom());
            }
            if (contentView != null) {
                contentView.setPadding(systemBars.left, contentView.getPaddingTop(), systemBars.right, systemBars.bottom);
            }
            return insets;
        });
    }

    protected void applyDrawerInsets(@NonNull DrawerLayout drawerLayout, @Nullable View headerView, @Nullable View contentView, @Nullable NavigationView navigationView) {
        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            if (headerView != null) {
                headerView.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            }
            if (contentView != null) {
                contentView.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
            }
            View navHeader = drawerLayout.findViewById(R.id.nav_header_container);
            if (navHeader != null) {
                int baseTop = (int) (16 * getResources().getDisplayMetrics().density);
                navHeader.setPadding(navHeader.getPaddingLeft(), systemBars.top + baseTop, navHeader.getPaddingRight(), navHeader.getPaddingBottom());
            } else if (navigationView != null && navigationView.getHeaderCount() > 0) {
                View header = navigationView.getHeaderView(0);
                if (header != null) {
                    int baseTop = (int) (16 * getResources().getDisplayMetrics().density);
                    header.setPadding(header.getPaddingLeft(), systemBars.top + baseTop, header.getPaddingRight(), header.getPaddingBottom());
                }
            }
            View navFooter = drawerLayout.findViewById(R.id.nav_footer_container);
            if (navFooter != null) {
                int baseBottom = (int) (12 * getResources().getDisplayMetrics().density);
                navFooter.setPadding(navFooter.getPaddingLeft(), navFooter.getPaddingTop(), navFooter.getPaddingRight(), systemBars.bottom + baseBottom);
            }
            return insets;
        });
    }

    protected boolean isDashboard() {
        return this instanceof HomeActivity;
    }

    protected void setupDrawerNavigation(@NonNull DrawerLayout drawerLayout, @NonNull Toolbar toolbar, @NonNull NavigationView navView, @IdRes int currentNavId) {
        setupDrawerNavigation(drawerLayout, toolbar, navView, currentNavId, isDashboard());
    }

    protected void setupDrawerNavigation(@NonNull DrawerLayout drawerLayout, @NonNull Toolbar toolbar, @NonNull NavigationView navView, @IdRes int currentNavId, boolean isDashboardScreen) {
        this.currentDrawerLayout = drawerLayout;
        this.currentToolbar = toolbar;
        setSupportActionBar(toolbar);

        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.START);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawerLayout.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                int height = drawerLayout.getHeight();
                int exclusionHeight = (int) (200 * getResources().getDisplayMetrics().density);
                int top = Math.max(0, (height - exclusionHeight) / 2);
                int width = (int) (40 * getResources().getDisplayMetrics().density);
                Rect rect = new Rect(0, top, width, Math.min(height, top + exclusionHeight));
                drawerLayout.setSystemGestureExclusionRects(Collections.singletonList(rect));
            });
        }

        if (isDashboardScreen) {
            // Dashboard: Hamburger toggle icon linked to DrawerLayout
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.nav_open, R.string.nav_close);
            toggle.getDrawerArrowDrawable().setColor(ContextCompat.getColor(this, R.color.white));
            drawerLayout.addDrawerListener(toggle);
            toggle.syncState();
        } else {
            // Non-Dashboard: Back button instead of hamburger icon
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowHomeEnabled(true);
                actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            }

            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            int whiteColor = ContextCompat.getColor(this, R.color.white);
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(whiteColor);
            }
            toolbar.setNavigationContentDescription(R.string.nav_back);
            toolbar.setNavigationOnClickListener(v -> handleBackNavigation(drawerLayout));
        }

        // Close drawer first when system back is pressed
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else if (isDashboardScreen) {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                } else {
                    handleBackNavigation(drawerLayout);
                }
            }
        });

        // Attach chevron action views to all collapsible parent items
        ensureDimensions();
        initCollapsibleSections();

        if (currentNavId != 0) {
            navView.setCheckedItem(currentNavId);
            CollapsibleMenuSection activeSection = findSectionByChild(currentNavId);
            if (activeSection != null) {
                activeSection.isExpanded = true;
            }
        }
        for (CollapsibleMenuSection section : collapsibleSections.values()) {
            MenuItem parentItem = navView.getMenu().findItem(section.parentId);
            if (parentItem != null && parentItem.getActionView() == null) {
                ImageView chevron = new ImageView(this);
                LinearLayoutCompat.LayoutParams lp =
                        new LinearLayoutCompat.LayoutParams(dimenChevronSize, dimenChevronSize);
                lp.gravity = Gravity.CENTER_VERTICAL;
                lp.setMarginEnd(dimenChevronMarginEnd);
                chevron.setLayoutParams(lp);
                chevron.setImageResource(R.drawable.ic_nav_chevron);
                chevron.setColorFilter(ContextCompat.getColor(this, R.color.nav_item_icon_tint),
                        PorterDuff.Mode.SRC_IN);
                chevron.setRotation(section.isExpanded ? 90f : 0f);
                chevron.setClickable(false);
                chevron.setFocusable(false);
                parentItem.setActionView(chevron);
            }
        }

        View logoutBtn = drawerLayout.findViewById(R.id.btn_nav_logout);
        if (logoutBtn != null) {
            logoutBtn.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                showLogoutConfirmationDialog();
            });
        }

        this.activeNavView = navView;

        // Apply role-based menu visibility (Admin sees all items, regular user sees only userMenus)
        applyMenuVisibility(navView, isCurrentUserAdmin());

        // Populate and synchronize nav header with current logged in user and role
        updateNavHeaderUser(drawerLayout);
        updateNavHeaderModeBadge(drawerLayout);
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerStateChanged(int newState) {
                if (newState == DrawerLayout.STATE_DRAGGING || newState == DrawerLayout.STATE_SETTLING) {
                    refreshAllNavigationItems(navView);
                }
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                updateNavHeaderUser(drawerLayout);
                updateNavHeaderModeBadge(drawerLayout);
                if (activeNavView != null) {
                    applyMenuVisibility(activeNavView, isCurrentUserAdmin());
                    activeNavView.post(() -> refreshAllNavigationItems(activeNavView));
                } else {
                    refreshAllNavigationItems(navView);
                }
            }
        });

        View navHeader = drawerLayout.findViewById(R.id.nav_header_container);
        if (navHeader != null) {
            navHeader.setOnClickListener(v -> {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(BaseActivity.this instanceof ProfileActivity)) {
                    Intent intent = new Intent(BaseActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    applyTransition(BaseActivity.this);
                }
            });
        }

        // Configure RecyclerView child items to style the submenu rows with tree guide rails and handle collapse
        RecyclerView rv = findNavigationRecyclerView(navView);
        if (rv != null) {
            rv.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(@NonNull View view) {
                    setupNavigationItemView(view);
                }

                @Override
                public void onChildViewDetachedFromWindow(@NonNull View view) {
                }
            });

            rv.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    refreshAllNavigationItems(navView);
                }
            });

            RecyclerView.Adapter<?> adapter = rv.getAdapter();
            if (adapter != null) {
                adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
                    @Override
                    public void onChanged() {
                        rv.post(() -> refreshAllNavigationItems(navView));
                    }

                    @Override
                    public void onItemRangeChanged(int positionStart, int itemCount) {
                        rv.post(() -> refreshAllNavigationItems(navView));
                    }

                    @Override
                    public void onItemRangeChanged(int positionStart, int itemCount, @Nullable Object payload) {
                        rv.post(() -> refreshAllNavigationItems(navView));
                    }
                });
            }

            rv.post(() -> refreshAllNavigationItems(navView));
        }

        navView.setNavigationItemSelectedListener(menuItem -> {
            int id = menuItem.getItemId();

            // Handle collapsible parent items: toggle expand/collapse with smooth accordion animation
            CollapsibleMenuSection parentSection = findSectionByParent(id);
            if (parentSection != null) {
                toggleCollapsibleSection(navView, id);
                return true;
            }

            // Handle Purchase child items
            if (id == R.id.nav_purchase_new_order) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof PurchaseActivity)) {
                    Intent intent = new Intent(this, PurchaseActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_purchase_history) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof PurchaseHistoryActivity)) {
                    Intent intent = new Intent(this, PurchaseHistoryActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            }

            // Handle Sell child items
            if (id == R.id.nav_sell_create) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof SellActivity)) {
                    Intent intent = new Intent(this, SellActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_sell_history) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof SellHistoryActivity)) {
                    Intent intent = new Intent(this, SellHistoryActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            }

            // Handle Reports child items
            if (id == R.id.nav_reports_gstr1) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof ReportsActivity)) {
                    Intent intent = new Intent(this, ReportsActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_reports_sales_register) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof SalesRegisterActivity)) {
                    Intent intent = new Intent(this, SalesRegisterActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_reports_purchase_register) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof PurchaseRegisterActivity)) {
                    Intent intent = new Intent(this, PurchaseRegisterActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_audit_trail) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof AuditTrailActivity)) {
                    Intent intent = new Intent(this, AuditTrailActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            }

            if (id == currentNavId) {
                drawerLayout.closeDrawer(GravityCompat.START);
                return true;
            }

            if (id == R.id.nav_home) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof HomeActivity)) {
                    Intent intent = new Intent(this, HomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                }
                return true;
            } else if (id == R.id.nav_inventory) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof InventoryActivity)) {
                    Intent intent = new Intent(this, InventoryActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_receive) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof PendingReceiveActivity)) {
                    Intent intent = new Intent(this, PendingReceiveActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_category) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof CategoryActivity)) {
                    Intent intent = new Intent(this, CategoryActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_uom) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof UnitOfMeasureActivity)) {
                    Intent intent = new Intent(this, UnitOfMeasureActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_buyer) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof BuyerActivity)) {
                    Intent intent = new Intent(this, BuyerActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_supplier) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof SupplierActivity)) {
                    Intent intent = new Intent(this, SupplierActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_profile) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof ProfileActivity)) {
                    Intent intent = new Intent(this, ProfileActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_settings) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof SettingsActivity)) {
                    Intent intent = new Intent(this, SettingsActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_reports) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof ReportsActivity)) {
                    Intent intent = new Intent(this, ReportsActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            } else if (id == R.id.nav_about) {
                drawerLayout.closeDrawer(GravityCompat.START);
                if (!(this instanceof AboutActivity)) {
                    Intent intent = new Intent(this, AboutActivity.class);
                    startActivity(intent);
                    applyTransition(this);
                }
                return true;
            }

            menuItem.setChecked(true);
            showToast(Objects.requireNonNull(menuItem.getTitle()).toString());
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    /**
     * Finds the RecyclerView inside a NavigationView.
     */
    @Nullable
    private RecyclerView findNavigationRecyclerView(@NonNull NavigationView navView) {
        for (int i = 0; i < navView.getChildCount(); i++) {
            View child = navView.getChildAt(i);
            if (child instanceof RecyclerView) {
                return (RecyclerView) child;
            }
        }
        return null;
    }

    /**
     * Finds a child NavigationMenuItemView by menu item ID.
     */
    @SuppressLint("RestrictedApi")
    @Nullable
    private View findNavigationMenuItemView(@NonNull NavigationView navView, @IdRes int menuItemId) {
        RecyclerView rv = findNavigationRecyclerView(navView);
        if (rv != null) {
            for (int i = 0; i < rv.getChildCount(); i++) {
                View child = rv.getChildAt(i);
                if (child instanceof MenuView.ItemView) {
                    MenuItem item = ((MenuView.ItemView) child).getItemData();
                    if (item != null && item.getItemId() == menuItemId) {
                        return child;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Recursively traverses a view hierarchy to find the first TextView child.
     */
    @Nullable
    private TextView findTextViewInView(@Nullable View view) {
        if (view instanceof TextView) {
            return (TextView) view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) view;
            for (int i = 0; i < vg.getChildCount(); i++) {
                TextView found = findTextViewInView(vg.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    @SuppressLint("RestrictedApi")
    private void setupNavigationItemView(@NonNull View itemView) {
        if (!(itemView instanceof MenuView.ItemView)) return;

        // Register layout change listener once per view to re-enforce submenu styling whenever presenter rebinds
        if (itemView.getTag(R.id.tag_nav_item_listener_added) == null) {
            itemView.setTag(R.id.tag_nav_item_listener_added, Boolean.TRUE);
            itemView.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                enforceNavigationItemStyle(v);
            });
        }

        enforceNavigationItemStyle(itemView);
    }

    @SuppressLint("RestrictedApi")
    private void enforceNavigationItemStyle(@NonNull View itemView) {
        if (!(itemView instanceof MenuView.ItemView)) return;
        MenuItem item = ((MenuView.ItemView) itemView).getItemData();
        if (item == null) return;

        int id = item.getItemId();
        CollapsibleMenuSection section = findSectionByChild(id);
        ensureDimensions();

        if (section != null) {
            boolean isLast = section.isLastChild(id);

            // Submenu indentation
            if (itemView.getPaddingStart() != dimenSubmenuPaddingStart || itemView.getPaddingEnd() != dimenSubmenuPaddingEnd) {
                itemView.setPaddingRelative(dimenSubmenuPaddingStart, 0, dimenSubmenuPaddingEnd, 0);
            }

            // Submenu guide rail and branch background
            int bgRes = isLast ? R.drawable.bg_nav_submenu_last : R.drawable.bg_nav_submenu_first;
            Drawable currentBg = itemView.getBackground();
            Object appliedDrawable = itemView.getTag(R.id.tag_nav_item_drawable);
            Object appliedResId = itemView.getTag(R.id.tag_nav_item_bg_res);

            boolean needsBg = currentBg == null
                    || currentBg != appliedDrawable
                    || !Integer.valueOf(bgRes).equals(appliedResId);

            if (needsBg) {
                Drawable newBg = ContextCompat.getDrawable(this, bgRes);
                if (newBg != null) {
                    itemView.setBackground(newBg);
                    itemView.setTag(R.id.tag_nav_item_drawable, newBg);
                    itemView.setTag(R.id.tag_nav_item_bg_res, bgRes);
                }
            }

            // Submenu text styling
            TextView textView = (TextView) itemView.getTag(R.id.tag_nav_item_textview);
            if (textView == null) {
                textView = findTextViewInView(itemView);
                if (textView != null) {
                    itemView.setTag(R.id.tag_nav_item_textview, textView);
                }
            }
            if (textView != null && !Boolean.TRUE.equals(itemView.getTag(R.id.tag_nav_item_text_styled))) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
                textView.setTextColor(ContextCompat.getColorStateList(this, R.color.nav_submenu_text_color));
                itemView.setTag(R.id.tag_nav_item_text_styled, Boolean.TRUE);
            }

            // Submenu height and visibility (when not actively running an accordion animation)
            if (!section.isAnimating) {
                int targetHeight = section.isExpanded ? dimenSubmenuHeight : 0;
                ViewGroup.LayoutParams lp = itemView.getLayoutParams();
                if (lp != null && lp.height != targetHeight) {
                    lp.height = targetHeight;
                    itemView.setLayoutParams(lp);
                }
                int targetVisibility = section.isExpanded ? View.VISIBLE : View.GONE;
                if (itemView.getVisibility() != targetVisibility) {
                    itemView.setVisibility(targetVisibility);
                }
                float targetAlpha = section.isExpanded ? 1f : 0f;
                if (itemView.getAlpha() != targetAlpha) {
                    itemView.setAlpha(targetAlpha);
                }
            }
        } else {
            // Reset standard items to guarantee view recycling never retains submenu properties
            Object appliedDrawable = itemView.getTag(R.id.tag_nav_item_drawable);
            if (appliedDrawable != null) {
                if (itemView.getBackground() == appliedDrawable) {
                    Drawable defaultBg = ContextCompat.getDrawable(this, R.drawable.bg_nav_item_selector);
                    itemView.setBackground(defaultBg);
                }
                itemView.setTag(R.id.tag_nav_item_drawable, null);
                itemView.setTag(R.id.tag_nav_item_bg_res, null);
            }

            if (itemView.getPaddingStart() != dimenStandardPaddingStart || itemView.getPaddingEnd() != dimenStandardPaddingEnd) {
                itemView.setPaddingRelative(dimenStandardPaddingStart, 0, dimenStandardPaddingEnd, 0);
            }

            TextView textView = (TextView) itemView.getTag(R.id.tag_nav_item_textview);
            if (textView == null) {
                textView = findTextViewInView(itemView);
                if (textView != null) {
                    itemView.setTag(R.id.tag_nav_item_textview, textView);
                }
            }
            if (textView != null && Boolean.TRUE.equals(itemView.getTag(R.id.tag_nav_item_text_styled))) {
                textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f);
                textView.setTextColor(ContextCompat.getColorStateList(this, R.color.nav_item_text_color));
                itemView.setTag(R.id.tag_nav_item_text_styled, Boolean.FALSE);
            }

            ViewGroup.LayoutParams lp = itemView.getLayoutParams();
            if (lp != null && lp.height != ViewGroup.LayoutParams.WRAP_CONTENT) {
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                itemView.setLayoutParams(lp);
            }
            if (itemView.getVisibility() != View.VISIBLE) {
                itemView.setVisibility(View.VISIBLE);
            }
            if (itemView.getAlpha() != 1f) {
                itemView.setAlpha(1f);
            }
        }
    }

    private void refreshAllNavigationItems(@Nullable NavigationView navView) {
        if (navView == null) return;
        RecyclerView rv = findNavigationRecyclerView(navView);
        if (rv != null) {
            for (int i = 0; i < rv.getChildCount(); i++) {
                setupNavigationItemView(rv.getChildAt(i));
            }
        }
    }

    /**
     * Toggles a collapsible menu section: rotates chevron, animates accordion height,
     * and collapses any other currently open section to keep the drawer tidy.
     */
    private void toggleCollapsibleSection(@NonNull NavigationView navView, int parentId) {
        CollapsibleMenuSection target = findSectionByParent(parentId);
        if (target == null) return;

        boolean willExpand = !target.isExpanded;
        target.isExpanded = willExpand;
        animateSectionChevron(navView, parentId, willExpand);
        animateSectionSubmenu(navView, target, willExpand);

        // Accordion behavior: collapse other expanded sections
        if (willExpand) {
            for (CollapsibleMenuSection other : collapsibleSections.values()) {
                if (other.parentId != parentId && other.isExpanded) {
                    other.isExpanded = false;
                    animateSectionChevron(navView, other.parentId, false);
                    animateSectionSubmenu(navView, other, false);
                }
            }
        }
    }

    /**
     * Rotates the chevron icon on a parent menu item 0↔90° with a smooth interpolator.
     */
    private void animateSectionChevron(@NonNull NavigationView navView, int parentId, boolean expanded) {
        MenuItem parentItem = navView.getMenu().findItem(parentId);
        View actionView = parentItem != null ? parentItem.getActionView() : null;
        if (actionView != null) {
            float targetRotation = expanded ? 90f : 0f;
            actionView.animate()
                    .rotation(targetRotation)
                    .setDuration(260)
                    .setInterpolator(new FastOutSlowInInterpolator())
                    .start();
        }
    }

    /**
     * Smooth accordion expand/collapse animation for any collapsible section's submenu rows.
     */
    private void animateSectionSubmenu(@NonNull NavigationView navView, @NonNull CollapsibleMenuSection section, boolean expand) {
        if (section.animator != null && section.animator.isRunning()) {
            section.animator.cancel();
        }

        List<View> childViews = new ArrayList<>();
        for (int cid : section.childIds) {
            View cv = findNavigationMenuItemView(navView, cid);
            if (cv != null) childViews.add(cv);
        }

        if (childViews.isEmpty()) {
            refreshAllNavigationItems(navView);
            for (int cid : section.childIds) {
                View cv = findNavigationMenuItemView(navView, cid);
                if (cv != null) childViews.add(cv);
            }
            if (childViews.isEmpty()) {
                return;
            }
        }

        section.isAnimating = true;
        ensureDimensions();
        final int targetHeight = dimenSubmenuHeight;

        int currentH = 0;
        for (View cv : childViews) {
            if (cv.getLayoutParams() != null && cv.getLayoutParams().height > 0) {
                currentH = cv.getLayoutParams().height;
                break;
            }
        }

        final int startH = expand ? Math.max(0, currentH) : (currentH > 0 ? currentH : targetHeight);
        final int endH = expand ? targetHeight : 0;
        final float startAlpha = expand
                ? (!childViews.isEmpty() && currentH > 0 ? childViews.get(0).getAlpha() : 0f)
                : (!childViews.isEmpty() ? childViews.get(0).getAlpha() : 1f);
        final float endAlpha = expand ? 1f : 0f;

        if (expand) {
            for (View cv : childViews) {
                cv.setVisibility(View.VISIBLE);
                if (cv.getLayoutParams() != null) {
                    cv.getLayoutParams().height = startH;
                }
                cv.setAlpha(startAlpha);
                enforceNavigationItemStyle(cv);
            }
        }

        section.animator = ValueAnimator.ofFloat(0f, 1f);
        section.animator.setDuration(260);
        section.animator.setInterpolator(new FastOutSlowInInterpolator());
        final List<View> finalChildViews = new ArrayList<>(childViews);
        section.animator.addUpdateListener(animation -> {
            float fraction = animation.getAnimatedFraction();
            int h = (int) (startH + (endH - startH) * fraction);
            float a = startAlpha + (endAlpha - startAlpha) * fraction;

            for (View cv : finalChildViews) {
                if (cv.getLayoutParams() != null) {
                    cv.getLayoutParams().height = h;
                    cv.setAlpha(a);
                    cv.requestLayout();
                }
            }
        });

        section.animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                section.isAnimating = false;
                if (!expand) {
                    for (View cv : finalChildViews) {
                        cv.setVisibility(View.GONE);
                        if (cv.getLayoutParams() != null) {
                            cv.getLayoutParams().height = 0;
                        }
                    }
                } else {
                    for (View cv : finalChildViews) {
                        if (cv.getLayoutParams() != null) {
                            cv.getLayoutParams().height = targetHeight;
                        }
                        cv.setAlpha(1f);
                        enforceNavigationItemStyle(cv);
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                section.isAnimating = false;
            }
        });

        section.animator.start();
    }

    protected void handleBackNavigation(@Nullable DrawerLayout drawerLayout) {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
            return;
        }

        if (isTaskRoot()) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        } else {
            finish();
        }
    }

    @Nullable
    protected Toolbar setupToolbar(@IdRes int toolbarId, @Nullable String title, boolean showBackButton) {
        Toolbar toolbar = findViewById(toolbarId);
        if (toolbar != null) {
            setupToolbar(toolbar, title, showBackButton);
        }
        return toolbar;
    }

    @Nullable
    protected Toolbar setupToolbar(@IdRes int toolbarId, @StringRes int titleResId, boolean showBackButton) {
        return setupToolbar(toolbarId, getString(titleResId), showBackButton);
    }

    protected void setupToolbar(@NonNull Toolbar toolbar, @Nullable String title, boolean showBackButton) {
        this.currentToolbar = toolbar;
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (title != null) {
                actionBar.setTitle(title);
            }
            actionBar.setDisplayHomeAsUpEnabled(showBackButton);
            actionBar.setDisplayShowHomeEnabled(showBackButton);
            if (showBackButton) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back);
            }
        }

        int whiteColor = ContextCompat.getColor(this, R.color.white);
        toolbar.setTitleTextColor(whiteColor);

        if (showBackButton) {
            toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
            if (toolbar.getNavigationIcon() != null) {
                toolbar.getNavigationIcon().setTint(whiteColor);
            }
            toolbar.setNavigationContentDescription(R.string.nav_back);
            toolbar.setNavigationOnClickListener(v -> handleBackNavigation(null));
        }
    }

    private void ensureModeListener() {
        if (appModeManager == null) {
            appModeManager = AppModeManager.getInstance(this);
            baseModeChangeListener = newMode -> updateNavHeaderModeBadge();
            appModeManager.addOnModeChangeListener(baseModeChangeListener);
        }
    }

    @Nullable
    protected DrawerLayout getDrawerLayout() {
        if (currentDrawerLayout != null) {
            return currentDrawerLayout;
        }
        return findViewById(R.id.drawer_layout);
    }

    public void updateNavHeaderModeBadge() {
        DrawerLayout drawerLayout = getDrawerLayout();
        if (drawerLayout != null) {
            updateNavHeaderModeBadge(drawerLayout);
        }
    }

    public void updateNavHeaderModeBadge(@NonNull DrawerLayout drawerLayout) {
        View headerView = drawerLayout.findViewById(R.id.nav_header_container);
        if (headerView != null) {
            updateNavHeaderModeBadge(headerView);
        }
    }

    public void updateNavHeaderModeBadge(@NonNull View headerView) {
        TextView modeBadge = headerView.findViewById(R.id.nav_header_mode_badge);
        if (modeBadge == null) return;

        ensureModeListener();
        boolean isOnline = appModeManager != null && appModeManager.isOnlineMode();

        int padStart = modeBadge.getPaddingStart();
        int padTop = modeBadge.getPaddingTop();
        int padEnd = modeBadge.getPaddingEnd();
        int padBottom = modeBadge.getPaddingBottom();

        modeBadge.setText(isOnline ? "Online" : "Offline");
        modeBadge.setBackgroundResource(isOnline ? R.drawable.bg_icon_badge_green : R.drawable.bg_icon_badge_red);
        modeBadge.setPaddingRelative(padStart, padTop, padEnd, padBottom);

        if (!modeBadge.hasOnClickListeners()) {
            modeBadge.setOnClickListener(v -> {
                boolean online = AppModeManager.getInstance(this).isOnlineMode();
                Toast.makeText(this, online ? "Active: Cloud Mode (Online)" : "Active: Local Database Mode (Offline)", Toast.LENGTH_SHORT).show();
            });
        }
    }

    protected void updateToolbarModeBadge() {
        updateNavHeaderModeBadge();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
            int white = ContextCompat.getColor(this, R.color.white);
            for (int i = 0; i < menu.size(); i++) {
                MenuItem item = menu.getItem(i);
                if (item.getIcon() != null) {
                    item.getIcon().setTint(white);
                }
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            handleBackNavigation(findViewById(R.id.drawer_layout));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean isDarkMode() {
        return ThemeManager.isDarkMode(this);
    }

    public void toggleTheme() {
        ThemeManager.toggleTheme(this);
    }

    public void showToast(@NonNull String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void navigateTo(@NonNull Class<?> destination, boolean finishCurrent) {
        Intent intent = new Intent(this, destination);
        startActivity(intent);
        applyTransition(this);
        if (finishCurrent) {
            finish();
        }
    }

    public void logout() {
        SharedPreferences preferences = getSharedPreferences(Configurations.PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        if (preferences.contains("user_id")) {
            editor.remove("user_id");
        }
        if (preferences.contains("isAdmin")) {
            editor.remove("isAdmin");
        }
        editor.remove(Configurations.KEY_AUTH_TOKEN);
        editor.apply();
        AppModeManager.getInstance(this).clearAuthToken();
        GlobalStore.getInstance().clearLoggedInUser();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        applyTransition(this);
        finish();
    }

    protected void showLogoutConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_dialog_title)
                .setMessage(R.string.logout_dialog_message)
                .setPositiveButton(R.string.logout, (dialog, which) -> logout())
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    public boolean isCurrentUserAdmin() {
        User user = GlobalStore.getInstance().getLoggedInUser();
        if (user != null && user.role != null) {
            return user.role.trim().equalsIgnoreCase("admin");
        }
        SharedPreferences preferences = getSharedPreferences(Configurations.PREF_NAME, MODE_PRIVATE);
        if (preferences.contains("isAdmin")) {
            return preferences.getBoolean("isAdmin", false);
        }
        return false;
    }

    private boolean isItemInUserMenus(int itemId) {
        for (int id : userMenus) {
            if (id == itemId) {
                return true;
            }
        }
        return false;
    }

    protected void applyMenuVisibility(@Nullable NavigationView navView, boolean isAdmin) {
        if (navView == null) return;
        applyMenuVisibility(navView.getMenu(), isAdmin);
    }

    private void applyMenuVisibility(@NonNull Menu menu, boolean isAdmin) {
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            if (item == null) continue;
            if (isAdmin) {
                item.setVisible(true);
            } else {
                item.setVisible(isItemInUserMenus(item.getItemId()));
            }
            if (item.hasSubMenu() && item.getSubMenu() != null) {
                applyMenuVisibility(item.getSubMenu(), isAdmin);
            }
        }
    }

    public void updateNavHeaderUser(@NonNull DrawerLayout drawerLayout) {
        View headerView = drawerLayout.findViewById(R.id.nav_header_container);
        if (headerView == null) return;
        updateNavHeaderModeBadge(headerView);

        TextView nameView = headerView.findViewById(R.id.nav_header_name);
        TextView emailView = headerView.findViewById(R.id.nav_header_email);
        TextView roleView = headerView.findViewById(R.id.nav_header_role);
        TextView initialsView = headerView.findViewById(R.id.nav_header_avatar_text);
        ImageView iconView = headerView.findViewById(R.id.nav_header_icon);

        User user = GlobalStore.getInstance().getLoggedInUser();
        if (user != null) {
            bindUserDetails(nameView, emailView, roleView, initialsView, iconView, user);
            if (activeNavView != null) {
                boolean isAdmin = user.role != null && user.role.trim().equalsIgnoreCase("admin");
                applyMenuVisibility(activeNavView, isAdmin);
            }
            return;
        }

        // Fallback: check SharedPreferences or database if memory store was cleared
        SharedPreferences preferences = getSharedPreferences(Configurations.PREF_NAME, MODE_PRIVATE);
        long userId = preferences.getLong("user_id", -1);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Database db = Database.getInstance(getApplicationContext());
                User dbUser = null;
                if (userId > 0) {
                    dbUser = db.userDao().getUserById(userId);
                }
                if (dbUser == null) {
                    dbUser = db.userDao().getUserForLogin("admin");
                }
                final User resolvedUser = dbUser;
                if (resolvedUser != null) {
                    GlobalStore.getInstance().setLoggedInUser(resolvedUser);
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            bindUserDetails(nameView, emailView, roleView, initialsView, iconView, resolvedUser);
                            if (activeNavView != null) {
                                boolean isAdmin = resolvedUser.role != null && resolvedUser.role.trim().equalsIgnoreCase("admin");
                                applyMenuVisibility(activeNavView, isAdmin);
                            }
                        }
                    });
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void bindUserDetails(@Nullable TextView nameView,
                                 @Nullable TextView emailView,
                                 @Nullable TextView roleView,
                                 @Nullable TextView initialsView,
                                 @Nullable ImageView iconView,
                                 @NonNull User user) {
        if (nameView != null) {
            String name = user.name != null && !user.name.trim().isEmpty() ? user.name.trim() : user.username;
            nameView.setText(name);
        }

        if (emailView != null) {
            emailView.setText(user.email != null ? user.email.trim() : "");
        }

        if (roleView != null) {
            String role = user.role != null ? user.role.trim() : "user";
            boolean isAdmin = role.equalsIgnoreCase("admin");
            roleView.setText(isAdmin ? "Admin" : "User");
            roleView.setTextColor(ContextCompat.getColor(this, isAdmin ? R.color.nav_role_admin_text : R.color.nav_role_user_text));
            roleView.setVisibility(View.VISIBLE);
        }

        if (initialsView != null && iconView != null) {
            String initials = getInitials(user.name != null && !user.name.trim().isEmpty() ? user.name : user.username);
            if (!initials.isEmpty()) {
                initialsView.setText(initials);
                initialsView.setVisibility(View.VISIBLE);
                iconView.setVisibility(View.GONE);
            } else {
                initialsView.setVisibility(View.GONE);
                iconView.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
        applyCloseTransition(this);
    }

    // Model for collapsible drawer menu section.
    private static class CollapsibleMenuSection {
        final int parentId;
        final int[] childIds;
        boolean isExpanded = false;
        boolean isAnimating = false;
        @Nullable
        ValueAnimator animator = null;

        CollapsibleMenuSection(int parentId, int... childIds) {
            this.parentId = parentId;
            this.childIds = childIds != null ? childIds : new int[0];
        }

        boolean isFirstChild(int id) {
            return childIds.length > 0 && childIds[0] == id;
        }

        boolean isLastChild(int id) {
            return childIds.length > 0 && childIds[childIds.length - 1] == id;
        }
    }
}
