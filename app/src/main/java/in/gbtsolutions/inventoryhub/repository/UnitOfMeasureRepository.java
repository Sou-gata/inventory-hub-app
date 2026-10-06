package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.UnitOfMeasureDao;
import in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper;
import in.gbtsolutions.inventoryhub.models.AuditTrail;
import in.gbtsolutions.inventoryhub.models.UnitOfMeasure;

public class UnitOfMeasureRepository {

    public interface ActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public static final String[][] DEFAULT_UNITS = new String[][]{
            {"KG", "Kilogram (Standard unit of weight)"},
            {"Gram", "Gram (Weight measurement)"},
            {"Liter", "Liter (Liquid volume measurement)"},
            {"Mililiter", "Milliliter (Liquid volume measurement)"},
            {"Pics", "Pieces / Count units"},
            {"Packet", "Packet packaging unit"}
    };

    private final Application application;
    private final UnitOfMeasureDao unitOfMeasureDao;
    private final LiveData<List<UnitOfMeasure>> allUnits;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    public UnitOfMeasureRepository(Application application) {
        this.application = application;
        Database db = Database.getInstance(application);
        this.unitOfMeasureDao = db.unitOfMeasureDao();
        this.allUnits = unitOfMeasureDao.getAllUnitsOfMeasure();
        this.executorService = Executors.newFixedThreadPool(3);
        this.mainHandler = new Handler(Looper.getMainLooper());

        // Ensure default units exist in background
        ensureDefaultUnitsExist(null);
    }

    public LiveData<List<UnitOfMeasure>> getAllUnitsOfMeasure() {
        return allUnits;
    }

    public LiveData<List<UnitOfMeasure>> search(String query) {
        return unitOfMeasureDao.search(query != null ? query.trim() : "");
    }

    public void ensureDefaultUnitsExist(@Nullable Runnable onComplete) {
        executorService.execute(() -> {
            try {
                for (String[] def : DEFAULT_UNITS) {
                    UnitOfMeasure existing = unitOfMeasureDao.getByName(def[0]);
                    if (existing == null) {
                        UnitOfMeasure uom = new UnitOfMeasure(def[0], def[1], true);
                        unitOfMeasureDao.insert(uom);
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (onComplete != null) {
                    mainHandler.post(onComplete);
                }
            }
        });
    }

    public void insert(@NonNull UnitOfMeasure uom, @Nullable ActionCallback callback) {
        final String name = uom.name != null ? uom.name.trim() : "";
        if (name.isEmpty()) {
            if (callback != null) callback.onError("Unit name cannot be empty.");
            return;
        }

        executorService.execute(() -> {
            try {
                UnitOfMeasure existing = unitOfMeasureDao.getByName(name);
                if (existing != null) {
                    postError(callback, "A unit of measure with the name \"" + name + "\" already exists.");
                    return;
                }

                uom.name = name;
                uom.isDefault = false; // User-created units are never default
                uom.createdAt = System.currentTimeMillis();
                uom.updatedAt = System.currentTimeMillis();

                long id = unitOfMeasureDao.insert(uom);
                if (id <= 0) {
                    postError(callback, "Failed to create unit of measure.");
                    return;
                }

                AuditTrailHelper.logAddition(
                        application,
                        AuditTrail.MODULE_UNIT_OF_MEASURE,
                        name,
                        "Added unit of measure: " + name
                );

                postSuccess(callback);
            } catch (Exception e) {
                postError(callback, e.getMessage() != null ? e.getMessage() : "Error saving unit of measure.");
            }
        });
    }

    public void update(@NonNull UnitOfMeasure uom, @Nullable ActionCallback callback) {
        final String name = uom.name != null ? uom.name.trim() : "";
        if (name.isEmpty()) {
            if (callback != null) callback.onError("Unit name cannot be empty.");
            return;
        }

        executorService.execute(() -> {
            try {
                UnitOfMeasure existing = unitOfMeasureDao.getById(uom.uomId);
                if (existing == null) {
                    postError(callback, "Unit of measure not found.");
                    return;
                }

                if (existing.isDefault) {
                    postError(callback, "Default units of measure (KG, Gram, Liter, Mililiter, Pics, Packet) cannot be edited.");
                    return;
                }

                UnitOfMeasure duplicate = unitOfMeasureDao.getByNameExcluding(name, uom.uomId);
                if (duplicate != null) {
                    postError(callback, "Another unit of measure named \"" + name + "\" already exists.");
                    return;
                }

                uom.name = name;
                uom.isDefault = false; // Cannot be upgraded to default
                uom.updatedAt = System.currentTimeMillis();
                unitOfMeasureDao.update(uom);

                AuditTrailHelper.logEdit(
                        application,
                        AuditTrail.MODULE_UNIT_OF_MEASURE,
                        name,
                        "Updated unit of measure: " + name
                );

                postSuccess(callback);
            } catch (Exception e) {
                postError(callback, e.getMessage() != null ? e.getMessage() : "Error updating unit of measure.");
            }
        });
    }

    public void delete(@NonNull UnitOfMeasure uom, @Nullable ActionCallback callback) {
        executorService.execute(() -> {
            try {
                UnitOfMeasure existing = unitOfMeasureDao.getById(uom.uomId);
                if (existing == null) {
                    postError(callback, "Unit of measure not found.");
                    return;
                }

                if (existing.isDefault) {
                    postError(callback, "Default units of measure (KG, Gram, Liter, Mililiter, Pics, Packet) cannot be deleted.");
                    return;
                }

                int deletedCount = unitOfMeasureDao.deleteByIdIfNotDefault(uom.uomId);
                if (deletedCount <= 0) {
                    postError(callback, "Could not delete unit of measure.");
                    return;
                }

                AuditTrailHelper.logDeletion(
                        application,
                        AuditTrail.MODULE_UNIT_OF_MEASURE,
                        existing.name,
                        "Deleted unit of measure: " + existing.name
                );

                postSuccess(callback);
            } catch (Exception e) {
                postError(callback, e.getMessage() != null ? e.getMessage() : "Error deleting unit of measure.");
            }
        });
    }

    private void postSuccess(@Nullable ActionCallback callback) {
        if (callback != null) {
            mainHandler.post(callback::onSuccess);
        }
    }

    private void postError(@Nullable ActionCallback callback, String message) {
        if (callback != null) {
            mainHandler.post(() -> callback.onError(message));
        }
    }
}
