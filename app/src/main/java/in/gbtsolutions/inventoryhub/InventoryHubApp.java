package in.gbtsolutions.inventoryhub;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import in.gbtsolutions.inventoryhub.helpers.ThemeManager;

public class InventoryHubApp extends Application implements DefaultLifecycleObserver {

    private static InventoryHubApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        ThemeManager.applyTheme(this);
        GlobalStore.getInstance().loadSettings(this);
        SeedDB.seedDB(this);
        in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.pruneOldRecords(this);
    }

    public static InventoryHubApp getInstance() {
        return instance;
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {

    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {

    }
}
