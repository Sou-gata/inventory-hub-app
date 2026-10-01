package in.gbtsolutions.inventoryhub.helpers;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import in.gbtsolutions.inventoryhub.R;

public class CategoryIconHelper {

    public static final String DEFAULT_ICON_KEY = "general";
    public static final String ICON_PREFIX = "cat_ic_";

    public static class IconItem {
        @NonNull
        public final String key; // e.g. "electronics" (without cat_ic_)
        @NonNull
        public final String label; // e.g. "Electronics"
        @DrawableRes
        public final int resId; // e.g. R.drawable.cat_ic_electronics

        public IconItem(@NonNull String key, @NonNull String label, @DrawableRes int resId) {
            this.key = key;
            this.label = label;
            this.resId = resId;
        }

        @NonNull
        public String getFullDrawableName() {
            return ICON_PREFIX + key;
        }
    }

    private static final List<IconItem> ALL_ICONS;
    private static final Map<String, Integer> KEY_TO_RES_MAP;

    static {
        List<IconItem> icons = new ArrayList<>(50);

        icons.add(new IconItem("electronics", "Electronics", R.drawable.cat_ic_electronics));
        icons.add(new IconItem("computer", "Computers & IT", R.drawable.cat_ic_computer));
        icons.add(new IconItem("phone", "Phones & Mobile", R.drawable.cat_ic_phone));
        icons.add(new IconItem("furniture", "Furniture", R.drawable.cat_ic_furniture));
        icons.add(new IconItem("groceries", "Groceries", R.drawable.cat_ic_groceries));
        icons.add(new IconItem("stationery", "Stationery", R.drawable.cat_ic_stationery));
        icons.add(new IconItem("hardware", "Hardware & Tools", R.drawable.cat_ic_hardware));
        icons.add(new IconItem("apparel", "Apparel & Clothes", R.drawable.cat_ic_apparel));
        icons.add(new IconItem("shoes", "Footwear & Shoes", R.drawable.cat_ic_shoes));
        icons.add(new IconItem("healthcare", "Health & Pharmacy", R.drawable.cat_ic_healthcare));
        icons.add(new IconItem("cosmetics", "Beauty & Cosmetics", R.drawable.cat_ic_cosmetics));
        icons.add(new IconItem("automotive", "Automotive & Parts", R.drawable.cat_ic_automotive));
        icons.add(new IconItem("books", "Books & Media", R.drawable.cat_ic_books));
        icons.add(new IconItem("sports", "Sports & Fitness", R.drawable.cat_ic_sports));
        icons.add(new IconItem("toys", "Toys & Gaming", R.drawable.cat_ic_toys));
        icons.add(new IconItem("beverages", "Beverages & Cafe", R.drawable.cat_ic_beverages));
        icons.add(new IconItem("bakery", "Bakery & Sweets", R.drawable.cat_ic_bakery));
        icons.add(new IconItem("meat", "Meat & Seafood", R.drawable.cat_ic_meat));
        icons.add(new IconItem("fruits", "Fresh Fruits & Veg", R.drawable.cat_ic_fruits));
        icons.add(new IconItem("jewelry", "Jewelry & Gems", R.drawable.cat_ic_jewelry));
        icons.add(new IconItem("home_appliances", "Home Appliances", R.drawable.cat_ic_home_appliances));
        icons.add(new IconItem("kitchenware", "Kitchenware", R.drawable.cat_ic_kitchenware));
        icons.add(new IconItem("lighting", "Lighting & Electrical", R.drawable.cat_ic_lighting));
        icons.add(new IconItem("cleaning", "Cleaning & Hygiene", R.drawable.cat_ic_cleaning));
        icons.add(new IconItem("gardening", "Plants & Gardening", R.drawable.cat_ic_gardening));
        icons.add(new IconItem("pet_supplies", "Pet Supplies", R.drawable.cat_ic_pet_supplies));
        icons.add(new IconItem("baby_products", "Baby & Maternity", R.drawable.cat_ic_baby_products));
        icons.add(new IconItem("music", "Music & Audio", R.drawable.cat_ic_music));
        icons.add(new IconItem("photography", "Cameras & Photo", R.drawable.cat_ic_photography));
        icons.add(new IconItem("networking", "Networking & LAN", R.drawable.cat_ic_networking));
        icons.add(new IconItem("security", "Security & Safety", R.drawable.cat_ic_security));
        icons.add(new IconItem("packaging", "Packaging & Boxes", R.drawable.cat_ic_packaging));
        icons.add(new IconItem("chemicals", "Chemicals & Lab", R.drawable.cat_ic_chemicals));
        icons.add(new IconItem("construction", "Construction Material", R.drawable.cat_ic_construction));
        icons.add(new IconItem("textiles", "Fabrics & Textiles", R.drawable.cat_ic_textiles));
        icons.add(new IconItem("optical", "Optical & Eyewear", R.drawable.cat_ic_optical));
        icons.add(new IconItem("watches", "Watches & Clocks", R.drawable.cat_ic_watches));
        icons.add(new IconItem("travel_bags", "Bags & Luggage", R.drawable.cat_ic_travel_bags));
        icons.add(new IconItem("electrical", "Power & Electricals", R.drawable.cat_ic_electrical));
        icons.add(new IconItem("plumbing", "Plumbing & Sanitary", R.drawable.cat_ic_plumbing));
        icons.add(new IconItem("wine_beverages", "Wine & Spirits", R.drawable.cat_ic_wine_beverages));
        icons.add(new IconItem("printer_supplies", "Printing & Ink", R.drawable.cat_ic_printer_supplies));
        icons.add(new IconItem("warehouse", "Warehouse & Storage", R.drawable.cat_ic_warehouse));
        icons.add(new IconItem("fitness", "Gym & Workout", R.drawable.cat_ic_fitness));
        icons.add(new IconItem("gift", "Gifts & Souvenirs", R.drawable.cat_ic_gift));
        icons.add(new IconItem("office_supplies", "Office Supplies", R.drawable.cat_ic_office_supplies));
        icons.add(new IconItem("paint", "Paints & Decor", R.drawable.cat_ic_paint));
        icons.add(new IconItem("solar_energy", "Solar & Energy", R.drawable.cat_ic_solar_energy));
        icons.add(new IconItem("general", "General & Retail", R.drawable.cat_ic_general));
        icons.add(new IconItem("miscellaneous", "Miscellaneous", R.drawable.cat_ic_miscellaneous));

        ALL_ICONS = Collections.unmodifiableList(icons);

        Map<String, Integer> map = new HashMap<>(icons.size() * 2);
        for (IconItem item : icons) {
            map.put(item.key.toLowerCase(Locale.ROOT), item.resId);
        }
        KEY_TO_RES_MAP = map;
    }

    /**
     * Sanitizes icon key by removing prefix if present and returning clean key.
     * When saving to DB, this ensures only the clean key is stored.
     */
    @NonNull
    public static String sanitizeIconKey(@Nullable String rawKey) {
        if (TextUtils.isEmpty(rawKey)) {
            return DEFAULT_ICON_KEY;
        }
        String clean = rawKey.trim().toLowerCase(Locale.ROOT);
        if (clean.startsWith(ICON_PREFIX)) {
            clean = clean.substring(ICON_PREFIX.length());
        }
        return clean.isEmpty() ? DEFAULT_ICON_KEY : clean;
    }

    /**
     * Resolves the drawable resource ID for a given icon key.
     * Concatenates "cat_ic_" with the saved name to look up the drawable.
     */
    @DrawableRes
    public static int getIconResId(@Nullable Context context, @Nullable String iconKey) {
        String cleanKey = sanitizeIconKey(iconKey);

        Integer cached = KEY_TO_RES_MAP.get(cleanKey);
        if (cached != null) {
            return cached;
        }

        // Dynamically resolve "cat_ic_" + cleanKey if not in direct map
        if (context != null) {
            String drawableName = ICON_PREFIX + cleanKey;
            int resId = context.getResources().getIdentifier(drawableName, "drawable", context.getPackageName());
            if (resId != 0) {
                return resId;
            }
        }

        return R.drawable.cat_ic_general;
    }

    /**
     * Returns the human-friendly display label for an icon key.
     */
    @NonNull
    public static String getIconLabel(@Nullable String iconKey) {
        String cleanKey = sanitizeIconKey(iconKey);
        for (IconItem item : ALL_ICONS) {
            if (item.key.equalsIgnoreCase(cleanKey)) {
                return item.label;
            }
        }
        return "General";
    }

    /**
     * Returns list of all 50 available icons.
     */
    @NonNull
    public static List<IconItem> getAllIcons() {
        return ALL_ICONS;
    }

    /**
     * Filters icons matching query by key or display label.
     */
    @NonNull
    public static List<IconItem> filterIcons(@Nullable String query) {
        if (TextUtils.isEmpty(query)) {
            return ALL_ICONS;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<IconItem> filtered = new ArrayList<>();
        for (IconItem item : ALL_ICONS) {
            if (item.key.contains(q) || item.label.toLowerCase(Locale.ROOT).contains(q)) {
                filtered.add(item);
            }
        }
        return filtered;
    }
}
