package in.gbtsolutions.inventoryhub.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "credit_debit_notes")
public class CreditDebitNote {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "note_id")
    public int noteId;

    @ColumnInfo(name = "note_number")
    public String noteNumber;

    @ColumnInfo(name = "note_type") // "CREDIT" or "DEBIT"
    public String noteType;

    @ColumnInfo(name = "note_date") // "YYYY-MM-DD"
    public String noteDate;

    @ColumnInfo(name = "original_invoice_number")
    public String originalInvoiceNumber;

    @ColumnInfo(name = "original_invoice_date")
    public String originalInvoiceDate;

    @ColumnInfo(name = "buyer_id")
    public Integer buyerId;

    @ColumnInfo(name = "customer_name")
    public String customerName;

    @ColumnInfo(name = "customer_gstin")
    public String customerGstin;

    @ColumnInfo(name = "is_registered", defaultValue = "0")
    public boolean isRegistered = false;

    @ColumnInfo(name = "place_of_supply")
    public String placeOfSupply;

    @ColumnInfo(name = "place_of_supply_state_code")
    public String placeOfSupplyStateCode;

    @ColumnInfo(name = "taxable_value")
    public double taxableValue;

    @ColumnInfo(name = "gst_rate")
    public double gstRate;

    @ColumnInfo(name = "cgst_amount")
    public double cgstAmount;

    @ColumnInfo(name = "sgst_amount")
    public double sgstAmount;

    @ColumnInfo(name = "igst_amount")
    public double igstAmount;

    @ColumnInfo(name = "cess_amount", defaultValue = "0.0")
    public double cessAmount = 0.0;

    @ColumnInfo(name = "total_amount")
    public double totalAmount;

    @ColumnInfo(name = "reason")
    public String reason;

    @ColumnInfo(name = "status", defaultValue = "Active")
    public String status = "Active"; // "Active", "Cancelled"

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public CreditDebitNote() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    @Ignore
    public CreditDebitNote(String noteNumber, String noteType, String noteDate, String originalInvoiceNumber,
                           String originalInvoiceDate, Integer buyerId, String customerName, String customerGstin,
                           boolean isRegistered, String placeOfSupply, String placeOfSupplyStateCode,
                           double taxableValue, double gstRate, double cgstAmount, double sgstAmount,
                           double igstAmount, double cessAmount, double totalAmount, String reason) {
        this.noteNumber = noteNumber;
        this.noteType = noteType;
        this.noteDate = noteDate;
        this.originalInvoiceNumber = originalInvoiceNumber;
        this.originalInvoiceDate = originalInvoiceDate;
        this.buyerId = buyerId;
        this.customerName = customerName;
        this.customerGstin = customerGstin;
        this.isRegistered = isRegistered;
        this.placeOfSupply = placeOfSupply;
        this.placeOfSupplyStateCode = placeOfSupplyStateCode;
        this.taxableValue = taxableValue;
        this.gstRate = gstRate;
        this.cgstAmount = cgstAmount;
        this.sgstAmount = sgstAmount;
        this.igstAmount = igstAmount;
        this.cessAmount = cessAmount;
        this.totalAmount = totalAmount;
        this.reason = reason;
        this.status = "Active";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
