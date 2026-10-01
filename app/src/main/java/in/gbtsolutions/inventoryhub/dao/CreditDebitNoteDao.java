package in.gbtsolutions.inventoryhub.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.CreditDebitNote;

@Dao
public interface CreditDebitNoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(CreditDebitNote note);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CreditDebitNote> notes);

    @Update
    void update(CreditDebitNote note);

    @Query("SELECT * FROM credit_debit_notes WHERE note_date BETWEEN :startDate AND :endDate ORDER BY note_date DESC")
    List<CreditDebitNote> getNotesBetweenDates(String startDate, String endDate);

    @Query("SELECT * FROM credit_debit_notes WHERE note_date BETWEEN :startDate AND :endDate ORDER BY note_date DESC")
    LiveData<List<CreditDebitNote>> getNotesBetweenDatesLive(String startDate, String endDate);

    @Query("SELECT * FROM credit_debit_notes ORDER BY note_date DESC")
    LiveData<List<CreditDebitNote>> getAllNotes();

    @Query("SELECT COUNT(*) FROM credit_debit_notes WHERE note_date BETWEEN :startDate AND :endDate AND note_type = :type AND status != 'Cancelled'")
    int countNotesByType(String startDate, String endDate, String type);

    @Query("SELECT COUNT(*) FROM credit_debit_notes")
    int count();
}
