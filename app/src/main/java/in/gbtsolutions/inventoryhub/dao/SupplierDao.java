package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.Suppliers;

@Dao
public interface SupplierDao {
    @Insert
    long insert(Suppliers supplier);

    @Update
    void update(Suppliers supplier);

    @Delete
    void delete(Suppliers supplier);

    @Query("SELECT * FROM suppliers WHERE supplier_id = :supplierId")
    Suppliers getSupplierById(int supplierId);

    @Query("SELECT * FROM suppliers ORDER BY supplier_name ASC")
    LiveData<List<Suppliers>> getAllSuppliers();

    @Query("SELECT * FROM suppliers WHERE is_active = 1 ORDER BY supplier_name ASC")
    LiveData<List<Suppliers>> getActiveSuppliers();

    @Query("SELECT * FROM suppliers WHERE supplier_name LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR gst LIKE '%' || :query || '%' OR contact_person LIKE '%' || :query || '%' OR city LIKE '%' || :query || '%' ORDER BY supplier_name ASC")
    LiveData<List<Suppliers>> searchSuppliers(String query);

    @Query("UPDATE suppliers SET is_active = :isActive, updated_at = :updatedAt WHERE supplier_id = :supplierId")
    void updateStatus(int supplierId, boolean isActive, long updatedAt);

    @Query("SELECT COUNT(*) FROM suppliers")
    int count();

    @Query("SELECT * FROM suppliers")
    List<Suppliers> getAllSuppliersSync();
}
