package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.AuditTrailDao;
import in.gbtsolutions.inventoryhub.models.AuditTrail;

public class AuditTrailRepository {

    private final AuditTrailDao auditTrailDao;
    private final LiveData<List<AuditTrail>> allAuditTrails;
    private final ExecutorService executorService;

    public AuditTrailRepository(Application application) {
        Database db = Database.getInstance(application);
        auditTrailDao = db.auditTrailDao();
        allAuditTrails = auditTrailDao.getAllAuditTrails();
        executorService = Executors.newFixedThreadPool(2);
    }

    public LiveData<List<AuditTrail>> getAllAuditTrails() {
        return allAuditTrails;
    }

    public LiveData<List<AuditTrail>> searchAuditTrails(String query) {
        return auditTrailDao.searchAuditTrails(query);
    }

    public void insert(AuditTrail auditTrail) {
        executorService.execute(() -> auditTrailDao.insert(auditTrail));
    }

    public void deleteOlderThan(long cutoffTimestamp) {
        executorService.execute(() -> auditTrailDao.deleteOlderThan(cutoffTimestamp));
    }

    public void deleteById(int id) {
        executorService.execute(() -> auditTrailDao.deleteById(id));
    }

    public void clearAll() {
        executorService.execute(() -> auditTrailDao.clearAll());
    }

    public LiveData<Integer> getCount() {
        return auditTrailDao.getCountLiveData();
    }
}
