package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.AuditTrail;

@Dao
public interface AuditTrailDao {

    @Insert
    long insert(AuditTrail auditTrail);

    @Query("SELECT * FROM audit_trails ORDER BY timestamp DESC")
    LiveData<List<AuditTrail>> getAllAuditTrails();

    @Query("SELECT * FROM audit_trails ORDER BY timestamp DESC")
    List<AuditTrail> getAllAuditTrailsSync();

    @Query("SELECT * FROM audit_trails WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    LiveData<List<AuditTrail>> getAuditTrailsSince(long sinceTimestamp);

    @Query("SELECT * FROM audit_trails WHERE details LIKE '%' || :query || '%' OR performed_by LIKE '%' || :query || '%' OR module LIKE '%' || :query || '%' OR record_id LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    LiveData<List<AuditTrail>> searchAuditTrails(String query);

    @Query("DELETE FROM audit_trails WHERE timestamp < :cutoffTimestamp")
    int deleteOlderThan(long cutoffTimestamp);

    @Query("DELETE FROM audit_trails WHERE id = :id")
    void deleteById(int id);

    @Query("DELETE FROM audit_trails")
    void clearAll();

    @Query("SELECT COUNT(*) FROM audit_trails")
    int getCount();

    @Query("SELECT COUNT(*) FROM audit_trails")
    LiveData<Integer> getCountLiveData();
}
