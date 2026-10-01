package in.gbtsolutions.inventoryhub;

public class Configurations {
    public static final String KEY_ALLOW_OUT_OF_STOCK_SELL = "allow_out_of_stock_sell";
    public static final String KEY_CSV_MANDATORY_FIELD = "csv_mandatory_field";
    public static final String MANDATORY_FIELD_SKU = "sku";
    public static final String MANDATORY_FIELD_HSN = "hsn";
    public static final String MANDATORY_FIELD_BOTH = "both";
    public static final String mkFixed = "F42B7169C8053EA271D94F6C18A507BE";
    public static final String ivFixed = "A5B4C3D2E1F09876";
    public static final String DEFAULT_STATE = "19"; // West Bengal
    public static final String DEFAULT_BACKUP_PASSWORD = mkFixed;
    public static String PREF_NAME = "inventory_pref";

    public static final String KEY_SHOW_PAYMENT_METHOD_DIALOG = "show_payment_method_dialog";
    public static final String KEY_UPI_ID = "upi_id";
    public static final String KEY_SAVE_BILL_TO_GALLERY = "save_bill_to_gallery";
    public static final String KEY_PRINT_BILL = "print_bill";

    // Application Operating Mode (Online vs Offline)
    public static final String KEY_APP_MODE = "app_mode";
    public static final String MODE_OFFLINE = "offline";
    public static final String MODE_ONLINE = "online";
    public static final String KEY_ONLINE_SERVER_URL = "online_server_url";
    public static final String DEFAULT_ONLINE_SERVER_URL = "https://imgbt.gbtsolutions.in/";
    public static final String KEY_AUTH_TOKEN = "auth_token";
}
