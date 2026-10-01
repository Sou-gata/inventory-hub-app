package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.SupplierDao;
import in.gbtsolutions.inventoryhub.models.Suppliers;

public class SupplierRepository {
    private final SupplierDao supplierDao;
    private final LiveData<List<Suppliers>> allSuppliers;
    private final ExecutorService executorService;
    private final Application application;

    public SupplierRepository(Application application) {
        this.application = application;
        Database db = Database.getInstance(application);
        supplierDao = db.supplierDao();
        allSuppliers = supplierDao.getAllSuppliers();
        executorService = Executors.newFixedThreadPool(4);
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

    public void insert(Suppliers supplier) {
        executorService.execute(() -> {
            long id = supplierDao.insert(supplier);
            in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logAddition(application,
                    in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_SUPPLIER,
                    supplier.supplierName,
                    "Added supplier: " + supplier.supplierName + " (Phone: " + (supplier.phone != null ? supplier.phone : "N/A") + ")");
        });
    }

    public void update(Suppliers supplier) {
        executorService.execute(() -> {
            supplierDao.update(supplier);
            in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                    in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_SUPPLIER,
                    supplier.supplierName,
                    "Updated supplier: " + supplier.supplierName + " (Phone: " + (supplier.phone != null ? supplier.phone : "N/A") + ")");
        });
    }

    public void updateStatus(int supplierId, boolean isActive) {
        executorService.execute(() -> {
            supplierDao.updateStatus(supplierId, isActive, System.currentTimeMillis());
            Suppliers s = supplierDao.getSupplierById(supplierId);
            String name = s != null ? s.supplierName : ("ID:" + supplierId);
            in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                    in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_SUPPLIER,
                    name,
                    (isActive ? "Activated" : "Deactivated") + " supplier: " + name);
        });
    }
}
