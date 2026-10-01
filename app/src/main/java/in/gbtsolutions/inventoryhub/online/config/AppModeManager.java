package in.gbtsolutions.inventoryhub.online.config;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Configurations;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AppModeManager {

    public interface OnModeChangeListener {
        void onModeChanged(AppMode newMode);
    }

    public interface ConnectionTestCallback {
        void onResult(boolean isSuccess, String message);
    }

    private static volatile AppModeManager instance;
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

        String savedModeKey = preferences.getString(Configurations.KEY_APP_MODE, Configurations.MODE_OFFLINE);
        this.currentMode = AppMode.fromKey(savedModeKey);

        this.serverUrl = preferences.getString(Configurations.KEY_ONLINE_SERVER_URL, Configurations.DEFAULT_ONLINE_SERVER_URL);
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
        preferences.edit().putString(Configurations.KEY_APP_MODE, newMode.getKey()).apply();

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

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(fullUrl)
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
                    Request fallbackReq = new Request.Builder().url(testUrl).get().build();
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
