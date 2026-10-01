package in.gbtsolutions.inventoryhub.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.PurchasePagerAdapter;
import in.gbtsolutions.inventoryhub.helpers.PurchaseCartManager;

public class PurchaseActivity extends BaseActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, getString(R.string.title_create_purchase), true);

        View headerContainer = findViewById(R.id.header_container);
        View contentContainer = findViewById(R.id.view_pager_sell);
        applyEdgeToEdgeInsets(headerContainer, contentContainer);

        // Always start with a clean purchase cart
        PurchaseCartManager.getInstance().clear();
        initViews();
        setupViewPager();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tab_layout_sell);
        viewPager = findViewById(R.id.view_pager_sell);
    }

    private void setupViewPager() {
        PurchasePagerAdapter pagerAdapter = new PurchasePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            View customView = getLayoutInflater().inflate(R.layout.item_sell_tab, null);
            ImageView icon = customView.findViewById(R.id.tab_icon);
            TextView title = customView.findViewById(R.id.tab_title);

            switch (position) {
                case PurchasePagerAdapter.TAB_SELECT_PRODUCTS:
                    title.setText(R.string.tab_select_products);
                    icon.setImageResource(R.drawable.ic_nav_inventory);
                    break;
                case PurchasePagerAdapter.TAB_PURCHASE_DETAILS:
                    title.setText(R.string.tab_purchase_details);
                    icon.setImageResource(R.drawable.ic_nav_sub_create_sale);
                    break;
            }
            tab.setCustomView(customView);
        }).attach();

        updateCartBadge();
    }

    private final PurchaseCartManager.OnCartChangedListener cartListener = this::updateCartBadge;

    @Override
    protected void onStart() {
        super.onStart();
        PurchaseCartManager.getInstance().addListener(cartListener);
        updateCartBadge();
    }

    @Override
    protected void onStop() {
        super.onStop();
        PurchaseCartManager.getInstance().removeListener(cartListener);
    }

    private void updateCartBadge() {
        if (tabLayout == null) return;
        TabLayout.Tab tab = tabLayout.getTabAt(PurchasePagerAdapter.TAB_PURCHASE_DETAILS);
        if (tab == null || tab.getCustomView() == null) return;

        TextView badge = tab.getCustomView().findViewById(R.id.tab_badge);
        if (badge != null) {
            int count = PurchaseCartManager.getInstance().getItemCount();
            if (count > 0) {
                badge.setVisibility(View.VISIBLE);
                badge.setText(String.valueOf(count));
            } else {
                badge.setVisibility(View.GONE);
            }
        }
    }
}
