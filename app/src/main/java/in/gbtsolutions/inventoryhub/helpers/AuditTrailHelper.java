package in.gbtsolutions.inventoryhub.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Configurations;
import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.models.AuditTrail;
import in.gbtsolutions.inventoryhub.models.User;

public class AuditTrailHelper {

    private static final String TAG = "AuditTrailHelper";
    public static final long RETENTION_DAYS = 30L;
    public static final long RETENTION_MILLIS = RETENTION_DAYS * 24L * 60L * 60L * 1000L;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public static void logAddition(Context context, String module, String recordId, String details) {
        log(context, AuditTrail.ACTION_ADD, module, recordId, details);
    }

    public static void logEdit(Context context, String module, String recordId, String details) {
        log(context, AuditTrail.ACTION_EDIT, module, recordId, details);
    }

    public static void logDeletion(Context context, String module, String recordId, String details) {
        log(context, AuditTrail.ACTION_DELETE, module, recordId, details);
    }

    public static void log(Context context, String actionType, String module, String recordId, String details) {
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        EXECUTOR.execute(() -> {
            try {
                String performedBy = resolveCurrentUserName(appContext);
                long now = System.currentTimeMillis();

                AuditTrail trail = new AuditTrail(
                        actionType,
                        module,
                        recordId != null ? recordId : "",
                        details != null ? details : "",
                        performedBy,
                        now
                );

                Database db = Database.getInstance(appContext);
                db.auditTrailDao().insert(trail);

                // Automatic 30-day retention policy:
                // Delete records where timestamp is older than 30 days ago
                long cutoffTimestamp = now - RETENTION_MILLIS;
                int deletedCount = db.auditTrailDao().deleteOlderThan(cutoffTimestamp);
                if (deletedCount > 0) {
                    Log.d(TAG, "Pruned " + deletedCount + " audit trail records older than 30 days.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to write audit trail entry: " + e.getMessage(), e);
            }
        });
    }

    public static void pruneOldRecords(Context context) {
        if (context == null) return;
        final Context appContext = context.getApplicationContext();

        EXECUTOR.execute(() -> {
            try {
                Database db = Database.getInstance(appContext);
                long cutoffTimestamp = System.currentTimeMillis() - RETENTION_MILLIS;
                int deletedCount = db.auditTrailDao().deleteOlderThan(cutoffTimestamp);
                if (deletedCount > 0) {
                    Log.d(TAG, "Pruned " + deletedCount + " old audit trail records on startup.");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to prune old audit records: " + e.getMessage(), e);
            }
        });
    }

    public static String resolveCurrentUserName(Context context) {
        try {
            User user = GlobalStore.getInstance().getLoggedInUser();
            if (user != null) {
                String name = user.name != null && !user.name.trim().isEmpty() ? user.name.trim() : user.username;
                String role = user.role != null && !user.role.trim().isEmpty() ? " (" + user.role.trim() + ")" : "";
                return (name != null ? name : "User") + role;
            }

            if (context != null) {
                long userId = UserHelper.getCurrentUserId(context);
                if (userId > 0) {
                    Database db = Database.getInstance(context);
                    User dbUser = db.userDao().getUserById(userId);
                    if (dbUser != null) {
                        String name = dbUser.name != null && !dbUser.name.trim().isEmpty() ? dbUser.name.trim() : dbUser.username;
                        String role = dbUser.role != null && !dbUser.role.trim().isEmpty() ? " (" + dbUser.role.trim() + ")" : "";
                        return (name != null ? name : "User") + role;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "Admin";
    }
}
