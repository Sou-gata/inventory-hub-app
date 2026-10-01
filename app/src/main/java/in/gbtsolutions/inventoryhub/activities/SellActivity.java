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
import in.gbtsolutions.inventoryhub.adapters.SellPagerAdapter;
import in.gbtsolutions.inventoryhub.helpers.SellCartManager;

public class SellActivity extends BaseActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sell);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, getString(R.string.title_create_sale), true);

        View headerContainer = findViewById(R.id.header_container);
        View contentContainer = findViewById(R.id.view_pager_sell);
        applyEdgeToEdgeInsets(headerContainer, contentContainer);

        SellCartManager.getInstance().clear();
        initViews();
        setupViewPager();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tab_layout_sell);
        viewPager = findViewById(R.id.view_pager_sell);
    }

    private void setupViewPager() {
        SellPagerAdapter pagerAdapter = new SellPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            View customView = getLayoutInflater().inflate(R.layout.item_sell_tab, null);
            ImageView icon = customView.findViewById(R.id.tab_icon);
            TextView title = customView.findViewById(R.id.tab_title);

            switch (position) {
                case SellPagerAdapter.TAB_SELECT_PRODUCTS:
                    title.setText(R.string.tab_select_products);
                    icon.setImageResource(R.drawable.ic_nav_inventory);
                    break;
                case SellPagerAdapter.TAB_SELL_DETAILS:
                    title.setText(R.string.tab_sell_details);
                    icon.setImageResource(R.drawable.ic_nav_sub_create_sale);
                    break;
            }
            tab.setCustomView(customView);
        }).attach();

        updateCartBadge();
    }

    private final SellCartManager.OnCartChangedListener cartListener = this::updateCartBadge;

    @Override
    protected void onStart() {
        super.onStart();
        SellCartManager.getInstance().addListener(cartListener);
        updateCartBadge();
    }

    @Override
    protected void onStop() {
        super.onStop();
        SellCartManager.getInstance().removeListener(cartListener);
    }

    private void updateCartBadge() {
        if (tabLayout == null) return;
        TabLayout.Tab tab = tabLayout.getTabAt(SellPagerAdapter.TAB_SELL_DETAILS);
        if (tab == null || tab.getCustomView() == null) return;

        TextView badge = tab.getCustomView().findViewById(R.id.tab_badge);
        if (badge != null) {
            int count = SellCartManager.getInstance().getItemCount();
            if (count > 0) {
                badge.setVisibility(View.VISIBLE);
                badge.setText(String.valueOf(count));
            } else {
                badge.setVisibility(View.GONE);
            }
        }
    }
}