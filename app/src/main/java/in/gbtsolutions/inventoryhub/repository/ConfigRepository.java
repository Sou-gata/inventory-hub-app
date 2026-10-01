package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.ConfigDao;
import in.gbtsolutions.inventoryhub.models.Config;

public class ConfigRepository {

    private final ConfigDao configDao;
    private final LiveData<List<Config>> allConfigs;
    private final ExecutorService executorService;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ConfigValueCallback {
        void onResult(String value);
    }

    public interface ConfigActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public ConfigRepository(Application application) {
        Database db = Database.getInstance(application);
        this.configDao = db.configDao();
        this.allConfigs = configDao.getAllConfigs();
        this.executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<Config>> getAllConfigs() {
        return allConfigs;
    }

    public LiveData<String> getValueLiveData(String key) {
        return configDao.getValueLiveData(key);
    }

    public void set(String key, String value) {
        executorService.execute(() -> configDao.insert(new Config(key, value)));
    }

    public void set(String key, String value, ConfigActionCallback callback) {
        executorService.execute(() -> {
            try {
                Config existing = configDao.getConfigByKey(key);
                if (existing != null) {
                    existing.setConfigValue(value);
                    configDao.update(existing);
                } else {
                    configDao.insert(new Config(key, value));
                }
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to set config."));
                }
            }
        });
    }

    public void saveConfigs(Map<String, String> configs, ConfigActionCallback callback) {
        executorService.execute(() -> {
            try {
                if (configs != null) {
                    for (Map.Entry<String, String> entry : configs.entrySet()) {
                        Config existing = configDao.getConfigByKey(entry.getKey());
                        if (existing != null) {
                            existing.setConfigValue(entry.getValue());
                            configDao.update(existing);
                        } else {
                            configDao.insert(new Config(entry.getKey(), entry.getValue()));
                        }
                    }
                }
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save configs."));
                }
            }
        });
    }

    public void get(String key, ConfigValueCallback callback) {
        executorService.execute(() -> {
            String value = configDao.getValueByKey(key);
            if (callback != null) {
                mainHandler.post(() -> callback.onResult(value));
            }
        });
    }

    public void delete(String key) {
        executorService.execute(() -> configDao.deleteByKey(key));
    }

    public void delete(String key, ConfigActionCallback callback) {
        executorService.execute(() -> {
            try {
                configDao.deleteByKey(key);
                if (callback != null) {
                    mainHandler.post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    mainHandler.post(() -> callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to delete config."));
                }
            }
        });
    }
}
