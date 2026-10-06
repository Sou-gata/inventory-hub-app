package in.gbtsolutions.inventoryhub;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.User;

public class GlobalStore {
    private static GlobalStore instance;
    private User loggedInUser = null;

    // Cached settings
    private boolean allowOutOfStockSell = false;
    private String csvMandatoryField = Configurations.MANDATORY_FIELD_HSN;
    private boolean showPaymentMethodDialog = false;
    private String upiId = "";
    private boolean saveBillToGallery = true;
    private boolean printBill = false;
    private final Map<String, Object> settingsMap = new HashMap<>();

    private GlobalStore() {
    }

    public static synchronized GlobalStore getInstance() {
        if (instance == null) {
            instance = new GlobalStore();
        }
        return instance;
    }

    public User getLoggedInUser() {
        return loggedInUser;
    }

    public void setLoggedInUser(User user) {
        loggedInUser = user;
    }

    public void clearLoggedInUser() {
        loggedInUser = null;
    }

    /**
     * Loads all available settings from SharedPreferences and caches them in GlobalStore.
     */
    public synchronized void loadSettings(Context context) {
        if (context == null) return;
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(Configurations.PREF_NAME, Context.MODE_PRIVATE);

        this.allowOutOfStockSell = prefs.getBoolean(Configurations.KEY_ALLOW_OUT_OF_STOCK_SELL, false);
        this.csvMandatoryField = prefs.getString(Configurations.KEY_CSV_MANDATORY_FIELD, Configurations.MANDATORY_FIELD_HSN);
        this.showPaymentMethodDialog = prefs.getBoolean(Configurations.KEY_SHOW_PAYMENT_METHOD_DIALOG, false);
        this.upiId = prefs.getString(Configurations.KEY_UPI_ID, "");
        this.saveBillToGallery = prefs.getBoolean(Configurations.KEY_SAVE_BILL_TO_GALLERY, true);
        this.printBill = prefs.getBoolean(Configurations.KEY_PRINT_BILL, false);

        settingsMap.clear();
        Map<String, ?> all = prefs.getAll();
        if (all != null) {
            settingsMap.putAll(all);
        }
        settingsMap.put(Configurations.KEY_ALLOW_OUT_OF_STOCK_SELL, this.allowOutOfStockSell);
        settingsMap.put(Configurations.KEY_CSV_MANDATORY_FIELD, this.csvMandatoryField);
        settingsMap.put(Configurations.KEY_SHOW_PAYMENT_METHOD_DIALOG, this.showPaymentMethodDialog);
        settingsMap.put(Configurations.KEY_UPI_ID, this.upiId);
        settingsMap.put(Configurations.KEY_SAVE_BILL_TO_GALLERY, this.saveBillToGallery);
        settingsMap.put(Configurations.KEY_PRINT_BILL, this.printBill);

        new Thread(() -> {
            try {
                List<Config> configs = Database.getInstance(context.getApplicationContext()).configDao().getAllConfigsSync();
                if (configs != null) {
                    for (Config c : configs) {
                        if (c.getConfigKey() != null && c.getConfigValue() != null) {
                            settingsMap.put(c.getConfigKey(), c.getConfigValue());
                            if (Configurations.KEY_UPI_ID.equals(c.getConfigKey()) && !c.getConfigValue().isEmpty()) {
                                this.upiId = c.getConfigValue();
                            } else if (Configurations.KEY_SAVE_BILL_TO_GALLERY.equals(c.getConfigKey())) {
                                this.saveBillToGallery = "true".equalsIgnoreCase(c.getConfigValue());
                            } else if (Configurations.KEY_PRINT_BILL.equals(c.getConfigKey())) {
                                this.printBill = "true".equalsIgnoreCase(c.getConfigValue());
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }).start();
    }

    public boolean isSaveBillToGallery() {
        return saveBillToGallery;
    }

    public void setSaveBillToGallery(boolean saveBillToGallery) {
        this.saveBillToGallery = saveBillToGallery;
        settingsMap.put(Configurations.KEY_SAVE_BILL_TO_GALLERY, saveBillToGallery);
    }

    public boolean isPrintBill() {
        return printBill;
    }

    public void setPrintBill(boolean printBill) {
        this.printBill = printBill;
        settingsMap.put(Configurations.KEY_PRINT_BILL, printBill);
    }

    public boolean isShowPaymentMethodDialog() {
        return showPaymentMethodDialog;
    }

    public void setShowPaymentMethodDialog(boolean showPaymentMethodDialog) {
        this.showPaymentMethodDialog = showPaymentMethodDialog;
        settingsMap.put(Configurations.KEY_SHOW_PAYMENT_METHOD_DIALOG, showPaymentMethodDialog);
    }

    public String getUpiId() {
        return upiId != null ? upiId : "";
    }

    public void setUpiId(String upiId) {
        this.upiId = upiId != null ? upiId : "";
        settingsMap.put(Configurations.KEY_UPI_ID, this.upiId);
    }



    public boolean isAllowOutOfStockSell() {
        return allowOutOfStockSell;
    }

    public void setAllowOutOfStockSell(boolean allowOutOfStockSell) {
        this.allowOutOfStockSell = allowOutOfStockSell;
        settingsMap.put(Configurations.KEY_ALLOW_OUT_OF_STOCK_SELL, allowOutOfStockSell);
    }

    public String getCsvMandatoryField() {
        return csvMandatoryField != null ? csvMandatoryField : Configurations.MANDATORY_FIELD_HSN;
    }

    public void setCsvMandatoryField(String mandatoryField) {
        this.csvMandatoryField = mandatoryField;
        settingsMap.put(Configurations.KEY_CSV_MANDATORY_FIELD, mandatoryField);
    }

    public Object getSetting(String key) {
        return settingsMap.get(key);
    }

    public boolean getBooleanSetting(String key, boolean defaultValue) {
        Object val = settingsMap.get(key);
        if (val instanceof Boolean) {
            return (Boolean) val;
        }
        return defaultValue;
    }

    public String getStringSetting(String key, String defaultValue) {
        Object val = settingsMap.get(key);
        if (val instanceof String) {
            return (String) val;
        }
        return defaultValue;
    }

    public int getIntSetting(String key, int defaultValue) {
        Object val = settingsMap.get(key);
        if (val instanceof Integer) {
            return (Integer) val;
        }
        return defaultValue;
    }

    public long getLongSetting(String key, long defaultValue) {
        Object val = settingsMap.get(key);
        if (val instanceof Long) {
            return (Long) val;
        } else if (val instanceof Integer) {
            return ((Integer) val).longValue();
        }
        return defaultValue;
    }

    public void setSetting(String key, Object value) {
        settingsMap.put(key, value);
    }
}
