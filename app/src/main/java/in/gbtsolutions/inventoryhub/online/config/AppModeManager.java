package in.gbtsolutions.inventoryhub.online.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import in.gbtsolutions.inventoryhub.Configurations;
import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.models.Config;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AppModeManager {
    public static final String KEY_DEVICE_ID = "device_id";
    public static final String KEY_APP_MODE = "app_mode";

    public interface OnModeChangeListener {
        void onModeChanged(AppMode newMode);
    }

    public interface ConnectionTestCallback {
        void onResult(boolean isSuccess, String message);
    }

    private static volatile AppModeManager instance;
    private static final OkHttpClient HEALTH_CHECK_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build();
    private final Context appContext;
    private final SharedPreferences preferences;
    private final CopyOnWriteArrayList<OnModeChangeListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private AppMode currentMode;
    private String serverUrl;

    private AppModeManager(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.preferences = appContext.getSharedPreferences(Configurations.PREF_NAME, Context.MODE_PRIVATE);

        String savedModeKey = preferences.getString(KEY_APP_MODE, "offline");
        this.currentMode = AppMode.fromKey(savedModeKey);

        this.serverUrl = preferences.getString("online_server_url", Configurations.DEFAULT_ONLINE_SERVER_URL);

        loadModeFromConfigTable();
    }

    public static AppModeManager getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AppModeManager.class) {
                if (instance == null) {
                    instance = new AppModeManager(context);
                }
            }
        }
        return instance;
    }

    public void loadModeFromConfigTable() {
        executor.execute(() -> {
            try {
                String modeVal = Database.getInstance(appContext).configDao().getValueByKey(Configurations.KEY_MODE);
                if (modeVal != null && !modeVal.trim().isEmpty()) {
                    AppMode mode = AppMode.fromKey(modeVal.trim());
                    if (this.currentMode != mode) {
                        mainHandler.post(() -> setAppMode(mode));
                    }
                }
                String migVal = Database.getInstance(appContext).configDao().getValueByKey(Configurations.KEY_MIGRATION_COMPLETED);
                if (migVal != null && !migVal.trim().isEmpty()) {
                    String normalized = normalizeMigrationStatus(migVal.trim());
                    preferences.edit().putString(Configurations.KEY_MIGRATION_COMPLETED, normalized).apply();
                }
            } catch (Exception ignored) {
            }
        });
    }

    public void refreshModeFromConfigTable() {
        loadModeFromConfigTable();
    }

    public synchronized AppMode getCurrentMode() {
        return currentMode;
    }

    public synchronized boolean isOnlineMode() {
        return currentMode == AppMode.ONLINE;
    }

    public synchronized boolean isOfflineMode() {
        return currentMode == AppMode.OFFLINE;
    }

    public synchronized void setAppMode(AppMode newMode) {
        if (newMode == null || this.currentMode == newMode) {
            return;
        }
        this.currentMode = newMode;
        preferences.edit().putString(KEY_APP_MODE, newMode.getKey()).apply();
        try {
            GlobalStore.getInstance().setMode(newMode.name());
        } catch (Exception ignored) {
        }

        // Notify listeners on the main thread
        mainHandler.post(() -> {
            for (OnModeChangeListener listener : listeners) {
                try {
                    listener.onModeChanged(newMode);
                } catch (Exception ignored) {
                }
            }
        });
    }

    public synchronized String getServerUrl() {
        return Configurations.DEFAULT_ONLINE_SERVER_URL;
    }

    public synchronized void setServerUrl(String url) {
        this.serverUrl = Configurations.DEFAULT_ONLINE_SERVER_URL;
    }

    public synchronized String getAuthToken() {
        return preferences.getString(Configurations.KEY_AUTH_TOKEN, "");
    }

    public synchronized void setAuthToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            preferences.edit().remove(Configurations.KEY_AUTH_TOKEN).apply();
        } else {
            preferences.edit().putString(Configurations.KEY_AUTH_TOKEN, token.trim()).apply();
        }
    }

    public synchronized void clearAuthToken() {
        preferences.edit().remove(Configurations.KEY_AUTH_TOKEN).apply();
    }

    public synchronized boolean hasAuthToken() {
        String token = getAuthToken();
        return token != null && !token.trim().isEmpty();
    }

    public synchronized String getDeviceId() {
        String deviceId = preferences.getString(KEY_DEVICE_ID, null);
        if (deviceId == null || deviceId.trim().isEmpty()) {
            try {
                deviceId = Settings.Secure.getString(appContext.getContentResolver(), Settings.Secure.ANDROID_ID);
            } catch (Exception ignored) {
            }
        }
        return deviceId != null ? deviceId.trim() : "";
    }

    public synchronized void setDeviceId(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            preferences.edit().remove(KEY_DEVICE_ID).apply();
        } else {
            preferences.edit().putString(KEY_DEVICE_ID, deviceId.trim()).apply();
        }
    }

    public synchronized String getMigrationStatusString() {
        return preferences.getString(Configurations.KEY_MIGRATION_COMPLETED, "0/0");
    }

    public synchronized int getMigratedCount() {
        return parseMigrationCount(getMigrationStatusString(), 0);
    }

    public synchronized int getAllowedMigrationCount() {
        return parseMigrationCount(getMigrationStatusString(), 1);
    }

    public synchronized boolean canMigrate() {
        String migrationString = getMigrationStatusString();
        if (migrationString == null || migrationString.trim().isEmpty()) {
            return false;
        }
        String[] migrations = migrationString.trim().split("/");
        int doneMigration = 0;
        int allowedMigration = 0;
        if (migrations.length > 0) {
            try {
                doneMigration = Integer.parseInt(migrations[0].trim());
            } catch (Exception ignored) {
            }
        }
        if (migrations.length > 1) {
            try {
                allowedMigration = Integer.parseInt(migrations[1].trim());
            } catch (Exception ignored) {
            }
        }
        return doneMigration < allowedMigration;
    }

    public synchronized boolean isMigrationCompleted() {
        String migrationString = getMigrationStatusString();
        if (migrationString == null || migrationString.trim().isEmpty()) {
            return false;
        }
        String[] migrations = migrationString.trim().split("/");
        int doneMigration = 0;
        int allowedMigration = 0;
        if (migrations.length > 0) {
            try {
                doneMigration = Integer.parseInt(migrations[0].trim());
            } catch (Exception ignored) {
            }
        }
        if (migrations.length > 1) {
            try {
                allowedMigration = Integer.parseInt(migrations[1].trim());
            } catch (Exception ignored) {
            }
        }
        return allowedMigration > 0 && doneMigration >= allowedMigration;
    }

    public synchronized String incrementMigrationCount() {
        String migration = getMigrationStatusString();
        int doneMigration = 0;
        int totalMigration = 0;
        if (migration != null && !migration.trim().isEmpty()) {
            String[] parts = migration.trim().split("/");
            if (parts.length > 0) {
                try {
                    doneMigration = Integer.parseInt(parts[0].trim());
                } catch (Exception ignored) {
                }
            }
            if (parts.length > 1) {
                try {
                    totalMigration = Integer.parseInt(parts[1].trim());
                } catch (Exception ignored) {
                }
            }
        }
        int updatedDone = doneMigration + 1;
        String newStatus = updatedDone + "/" + totalMigration;
        setMigrationStatus(newStatus);
        return newStatus;
    }

    public synchronized void setMigrationStatus(String status) {
        String normalized = normalizeMigrationStatus(status);
        preferences.edit().putString(Configurations.KEY_MIGRATION_COMPLETED, normalized).apply();
        try {
            GlobalStore.getInstance().setSetting(Configurations.KEY_MIGRATION_COMPLETED, normalized);
        } catch (Exception ignored) {
        }
        executor.execute(() -> {
            try {
                Database db = Database.getInstance(appContext);
                if (db != null && db.configDao() != null) {
                    Config existing = db.configDao().getConfigByKey(Configurations.KEY_MIGRATION_COMPLETED);
                    if (existing != null) {
                        existing.setConfigValue(normalized);
                        db.configDao().update(existing);
                    } else {
                        db.configDao().insert(new Config(Configurations.KEY_MIGRATION_COMPLETED, normalized));
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    public synchronized void setMigrationCompleted(boolean completed) {
        if (completed) {
            incrementMigrationCount();
        }
    }

    public static String normalizeMigrationStatus(String status) {
        if (status == null || status.trim().isEmpty()) return "0/0";
        status = status.trim();
        if ("true".equalsIgnoreCase(status)) return "1/1";
        if ("false".equalsIgnoreCase(status)) return "0/1";
        String[] parts = status.split("/");
        int done = 0;
        int total = 0;
        if (parts.length > 0) {
            try {
                done = Integer.parseInt(parts[0].trim());
            } catch (Exception ignored) {
            }
        }
        if (parts.length > 1) {
            try {
                total = Integer.parseInt(parts[1].trim());
            } catch (Exception ignored) {
            }
        }
        return done + "/" + total;
    }

    public static int parseMigrationCount(String status, int index) {
        if (status == null || status.trim().isEmpty()) return 0;
        String[] parts = status.trim().split("/");
        if (parts.length > index) {
            try {
                return Integer.parseInt(parts[index].trim());
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    public void addOnModeChangeListener(OnModeChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeOnModeChangeListener(OnModeChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    /**
     * Tests server connectivity asynchronously by hitting the base URL or health check.
     */
    public void testServerConnection(String targetUrl, ConnectionTestCallback callback) {
        executor.execute(() -> {
            String testUrl = (targetUrl != null && !targetUrl.trim().isEmpty())
                    ? targetUrl.trim() : getServerUrl();
            if (!testUrl.endsWith("/")) {
                testUrl += "/";
            }
            // append health check or ping
            String fullUrl = testUrl + "api/v1/health";

            OkHttpClient client = HEALTH_CHECK_CLIENT;

            Request request = new Request.Builder()
                    .url(fullUrl)
                    .header("Accept", "application/json")
                    .header("X-Client-Type", "android")
                    .header("X-Device-Id", getDeviceId())
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                boolean success = response.isSuccessful();
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onResult(success, success ? "Connected successfully! (HTTP " + response.code() + ")"
                                : "Server returned HTTP " + response.code());
                    }
                });
            } catch (IOException e) {
                // If /api/v1/health isn't ready yet, try root URL
                try {
                    Request fallbackReq = new Request.Builder()
                            .url(testUrl)
                            .header("Accept", "application/json")
                            .header("X-Client-Type", "android")
                            .header("X-Device-Id", getDeviceId())
                            .get()
                            .build();
                    try (Response fbResponse = client.newCall(fallbackReq).execute()) {
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onResult(true, "Connected to host (HTTP " + fbResponse.code() + ")");
                            }
                        });
                        return;
                    }
                } catch (IOException ex) {
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onResult(false, "Connection failed: " + (e.getMessage() != null ? e.getMessage() : "Timeout"));
                        }
                    });
                }
            }
        });
    }
}
