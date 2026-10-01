package in.gbtsolutions.inventoryhub.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import in.gbtsolutions.inventoryhub.fragments.SelectProductsFragment;
import in.gbtsolutions.inventoryhub.fragments.SellDetailsFragment;

public class SellPagerAdapter extends FragmentStateAdapter {

    public static final int TAB_COUNT = 2;
    public static final int TAB_SELECT_PRODUCTS = 0;
    public static final int TAB_SELL_DETAILS = 1;

    public SellPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_SELECT_PRODUCTS:
                return SelectProductsFragment.newInstance();
            case TAB_SELL_DETAILS:
                return SellDetailsFragment.newInstance();
            default:
                throw new IllegalArgumentException("Invalid tab position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
}
