package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.SupplierDao;
import in.gbtsolutions.inventoryhub.models.Suppliers;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.online.repository.OnlineSupplierRepository;

public class SupplierRepository {
    private final SupplierDao supplierDao;
    private final LiveData<List<Suppliers>> allSuppliers;
    private final ExecutorService executorService;
    private final Application application;
    private final OnlineSupplierRepository onlineSupplierRepository;
    private final AppModeManager appModeManager;

    public interface SupplierActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public SupplierRepository(Application application) {
        this.application = application;
        Database db = Database.getInstance(application);
        supplierDao = db.supplierDao();
        allSuppliers = supplierDao.getAllSuppliers();
        executorService = Executors.newFixedThreadPool(4);
        onlineSupplierRepository = new OnlineSupplierRepository(application);
        appModeManager = AppModeManager.getInstance(application);
    }

    public boolean isOnlineMode() {
        return appModeManager.isOnlineMode();
    }

    public OnlineSupplierRepository getOnlineRepository() {
        return onlineSupplierRepository;
    }

    public LiveData<List<Suppliers>> getAllSuppliers() {
        return allSuppliers;
    }

    public LiveData<List<Suppliers>> getActiveSuppliers() {
        return supplierDao.getActiveSuppliers();
    }

    public LiveData<List<Suppliers>> searchSuppliers(String query) {
        return supplierDao.searchSuppliers(query);
    }

    public void fetchSuppliersOnline(OnlineSupplierRepository.SupplierListCallback callback) {
        fetchSuppliersOnline(null, callback);
    }

    public void fetchSuppliersOnline(String search, OnlineSupplierRepository.SupplierListCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineSupplierRepository.fetchSuppliers(search, callback);
        }
    }

    public void insert(Suppliers supplier) {
        insert(supplier, null);
    }

    public void insert(Suppliers supplier, SupplierActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineSupplierRepository.createSupplier(supplier, new OnlineSupplierRepository.SupplierActionCallback() {
                @Override
                public void onSuccess(Suppliers s) {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) callback.onError(errorMessage);
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                long id = supplierDao.insert(supplier);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logAddition(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_SUPPLIER,
                        supplier.supplierName,
                        "Added supplier: " + supplier.supplierName + " (Phone: " + (supplier.phone != null ? supplier.phone : "N/A") + ")");
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        String msg = e.getMessage() != null ? e.getMessage() : "Failed to add supplier.";
                        if (e instanceof android.database.sqlite.SQLiteConstraintException) {
                            msg = "A supplier with this phone number, email, or GST already exists.";
                        }
                        callback.onError(msg);
                    });
                }
            }
        });
    }

    public void update(Suppliers supplier) {
        update(supplier, null);
    }

    public void update(Suppliers supplier, SupplierActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineSupplierRepository.updateSupplier(supplier, new OnlineSupplierRepository.SupplierActionCallback() {
                @Override
                public void onSuccess(Suppliers s) {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) callback.onError(errorMessage);
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                supplierDao.update(supplier);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_SUPPLIER,
                        supplier.supplierName,
                        "Updated supplier: " + supplier.supplierName + " (Phone: " + (supplier.phone != null ? supplier.phone : "N/A") + ")");
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        String msg = e.getMessage() != null ? e.getMessage() : "Failed to update supplier.";
                        if (e instanceof android.database.sqlite.SQLiteConstraintException) {
                            msg = "A supplier with this phone number, email, or GST already exists.";
                        }
                        callback.onError(msg);
                    });
                }
            }
        });
    }

    public void delete(int supplierId, SupplierActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineSupplierRepository.deleteSupplier(supplierId, new OnlineSupplierRepository.SupplierDeleteCallback() {
                @Override
                public void onSuccess() {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) callback.onError(errorMessage);
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                Suppliers s = supplierDao.getSupplierById(supplierId);
                if (s != null) {
                    supplierDao.delete(s);
                }
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to delete supplier."));
                }
            }
        });
    }

    public void updateStatus(int supplierId, boolean isActive) {
        updateStatus(supplierId, isActive, null);
    }

    public void updateStatus(int supplierId, boolean isActive, SupplierActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            Suppliers s = new Suppliers();
            s.supplierId = supplierId;
            s.isActive = isActive;
            onlineSupplierRepository.updateSupplier(s, new OnlineSupplierRepository.SupplierActionCallback() {
                @Override
                public void onSuccess(Suppliers updated) {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) callback.onError(errorMessage);
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                supplierDao.updateStatus(supplierId, isActive, System.currentTimeMillis());
                Suppliers s = supplierDao.getSupplierById(supplierId);
                String name = s != null ? s.supplierName : ("ID:" + supplierId);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_SUPPLIER,
                        name,
                        (isActive ? "Activated" : "Deactivated") + " supplier: " + name);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to update supplier status."));
                }
            }
        });
    }
}
