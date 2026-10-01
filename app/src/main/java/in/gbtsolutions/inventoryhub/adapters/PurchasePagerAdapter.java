package in.gbtsolutions.inventoryhub.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import in.gbtsolutions.inventoryhub.fragments.PurchaseDetailsFragment;
import in.gbtsolutions.inventoryhub.fragments.SelectPurchaseProductsFragment;

public class PurchasePagerAdapter extends FragmentStateAdapter {

    public static final int TAB_COUNT = 2;
    public static final int TAB_SELECT_PRODUCTS = 0;
    public static final int TAB_PURCHASE_DETAILS = 1;

    public PurchasePagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_SELECT_PRODUCTS:
                return SelectPurchaseProductsFragment.newInstance();
            case TAB_PURCHASE_DETAILS:
                return PurchaseDetailsFragment.newInstance();
            default:
                throw new IllegalArgumentException("Invalid tab position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
}
