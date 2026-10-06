package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(tableName = "audit_trails", indices = {
        @Index(value = "timestamp"),
        @Index(value = "action_type"),
        @Index(value = "module")
})
public class AuditTrail {

    public static final String ACTION_ADD = "ADD";
    public static final String ACTION_EDIT = "EDIT";
    public static final String ACTION_DELETE = "DELETE";

    public static final String MODULE_PRODUCT = "Product";
    public static final String MODULE_CATEGORY = "Category";
    public static final String MODULE_BUYER = "Buyer";
    public static final String MODULE_SUPPLIER = "Supplier";
    public static final String MODULE_SALE = "Sale";
    public static final String MODULE_PURCHASE = "Purchase";
    public static final String MODULE_USER = "User";
    public static final String MODULE_CONFIG = "Settings";
    public static final String MODULE_UNIT_OF_MEASURE = "Unit of Measure";

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "action_type")
    public String actionType;

    @ColumnInfo(name = "module")
    public String module;

    @ColumnInfo(name = "record_id")
    public String recordId;

    @ColumnInfo(name = "details")
    public String details;

    @ColumnInfo(name = "performed_by")
    public String performedBy; 

    @ColumnInfo(name = "timestamp")
    public long timestamp;

    public AuditTrail() {
    }

    @androidx.room.Ignore
    public AuditTrail(String actionType, String module, String recordId, String details, String performedBy, long timestamp) {
        this.actionType = actionType;
        this.module = module;
        this.recordId = recordId;
        this.details = details;
        this.performedBy = performedBy;
        this.timestamp = timestamp;
    }

    public String getFormattedDate() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return String.valueOf(timestamp);
        }
    }

    public String getTimeOnly() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }

    public String getDateOnly() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            return sdf.format(new Date(timestamp));
        } catch (Exception e) {
            return "";
        }
    }
}
