package in.gbtsolutions.inventoryhub.reports.gstr1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class GSTR1Models {

    public static BigDecimal roundMoney(BigDecimal val) {
        if (val == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return val.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal toMoney(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }

    // Top Summary KPI Cards
    public static class KPISummary {
        public BigDecimal totalSales = BigDecimal.ZERO;
        public BigDecimal taxableValue = BigDecimal.ZERO;
        public BigDecimal cgst = BigDecimal.ZERO;
        public BigDecimal sgst = BigDecimal.ZERO;
        public BigDecimal igst = BigDecimal.ZERO;
        public BigDecimal cess = BigDecimal.ZERO;
        public BigDecimal totalGst = BigDecimal.ZERO;
        public int totalInvoices = 0;
    }

    // Table 1: B2B Supplies
    public static class B2BRow {
        public String customerGstin = "";
        public String customerName = "";
        public String placeOfSupply = "";
        public String posStateCode = "";
        public double taxRate = 0.0;
        public int invoiceCount = 0;
        public BigDecimal taxableValue = BigDecimal.ZERO;
        public BigDecimal cgst = BigDecimal.ZERO;
        public BigDecimal sgst = BigDecimal.ZERO;
        public BigDecimal igst = BigDecimal.ZERO;
        public BigDecimal cess = BigDecimal.ZERO;
        public BigDecimal totalTax = BigDecimal.ZERO;
        public BigDecimal invoiceValue = BigDecimal.ZERO;
    }

    // Table 2: B2C Supplies (B2C Large and B2C Others)
    public static class B2CRow {
        public String supplyCategory = ""; // "B2C Others" or "B2C Large"
        public String placeOfSupply = "";
        public String posStateCode = "";
        public double taxRate = 0.0;
        public int invoiceCount = 0;
        public BigDecimal taxableValue = BigDecimal.ZERO;
        public BigDecimal cgst = BigDecimal.ZERO;
        public BigDecimal sgst = BigDecimal.ZERO;
        public BigDecimal igst = BigDecimal.ZERO;
        public BigDecimal cess = BigDecimal.ZERO;
        public BigDecimal totalTax = BigDecimal.ZERO;
        public BigDecimal invoiceValue = BigDecimal.ZERO;
    }

    // Table 3: Exports & SEZ
    public static class ExportRow {
        public String category = ""; // "Export With IGST", "Export Without IGST / LUT", "SEZ With IGST", "SEZ Without IGST", "Deemed Export"
        public int invoiceCount = 0;
        public BigDecimal taxableValue = BigDecimal.ZERO;
        public BigDecimal invoiceValue = BigDecimal.ZERO;
        public BigDecimal igst = BigDecimal.ZERO;
        public BigDecimal cgst = BigDecimal.ZERO;
        public BigDecimal sgst = BigDecimal.ZERO;
        public BigDecimal cess = BigDecimal.ZERO;
        public String shippingBillNo;
        public String shippingBillDate;
        public String portCode;
    }

    // Table 4: Credit / Debit Notes
    public static class CreditDebitNoteRow {
        public String noteType = ""; // "CREDIT" or "DEBIT"
        public String recipientType = ""; // "Registered" or "Unregistered"
        public double taxRate = 0.0;
        public int noteCount = 0;
        public BigDecimal taxableValue = BigDecimal.ZERO;
        public BigDecimal cgst = BigDecimal.ZERO;
        public BigDecimal sgst = BigDecimal.ZERO;
        public BigDecimal igst = BigDecimal.ZERO;
        public BigDecimal cess = BigDecimal.ZERO;
        public BigDecimal totalTax = BigDecimal.ZERO;
        public BigDecimal totalValue = BigDecimal.ZERO;
    }

    // Table 5: Nil / Exempt / Non-GST Supplies
    public static class NilExemptRow {
        public String category = ""; // "Nil Rated", "Exempt", "Non-GST"
        public String supplyType = ""; // "Intra-State" or "Inter-State"
        public String recipientType = ""; // "Registered" or "Unregistered"
        public int invoiceCount = 0;
        public BigDecimal reportedValue = BigDecimal.ZERO;
    }

    // Table 6: HSN / SAC Summary
    public static class HsnRow {
        public String hsnCode = "";
        public String description = "";
        public String uqc = "PCS";
        public double quantity = 0.0;
        public double taxRate = 0.0;
        public String supplyType = "B2C"; // "B2B" or "B2C"
        public BigDecimal taxableValue = BigDecimal.ZERO;
        public BigDecimal cgst = BigDecimal.ZERO;
        public BigDecimal sgst = BigDecimal.ZERO;
        public BigDecimal igst = BigDecimal.ZERO;
        public BigDecimal cess = BigDecimal.ZERO;
        public BigDecimal totalTax = BigDecimal.ZERO;
        public BigDecimal totalValue = BigDecimal.ZERO;
    }

    // Table 7: Tax & Document Summary
    public static class TaxAndDocSummary {
        public BigDecimal grossInvoiceValue = BigDecimal.ZERO;
        public BigDecimal taxableValue = BigDecimal.ZERO;
        public BigDecimal cgst = BigDecimal.ZERO;
        public BigDecimal sgst = BigDecimal.ZERO;
        public BigDecimal igst = BigDecimal.ZERO;
        public BigDecimal cess = BigDecimal.ZERO;
        public BigDecimal totalGst = BigDecimal.ZERO;

        public int taxInvoicesCount = 0;
        public int cancelledInvoicesCount = 0;
        public int creditNotesCount = 0;
        public int debitNotesCount = 0;
        public int netDocumentsCount = 0;
    }

    // Validation Issue
    public static class ValidationIssue {
        public enum Severity { ERROR, WARNING }
        public final Severity severity;
        public final String invoiceOrRef;
        public final String title;
        public final String description;

        public ValidationIssue(Severity severity, String invoiceOrRef, String title, String description) {
            this.severity = severity;
            this.invoiceOrRef = invoiceOrRef;
            this.title = title;
            this.description = description;
        }
    }

    // Reconciliation Check
    public static class ReconciliationResult {
        public BigDecimal expectedTaxable = BigDecimal.ZERO;
        public BigDecimal hsnTaxable = BigDecimal.ZERO;
        public BigDecimal difference = BigDecimal.ZERO;
        public boolean isMatched = true;
    }

    // Comprehensive Container for all 7 reports
    public static class ReportData {
        public String startDate;
        public String endDate;
        public String companyGst;
        public String companyName;

        public KPISummary kpi = new KPISummary();
        public List<B2BRow> b2bList = new ArrayList<>();
        public List<B2CRow> b2cLargeList = new ArrayList<>();
        public List<B2CRow> b2cOthersList = new ArrayList<>();
        public List<ExportRow> exportList = new ArrayList<>();
        public List<CreditDebitNoteRow> creditNotesList = new ArrayList<>();
        public List<CreditDebitNoteRow> debitNotesList = new ArrayList<>();
        public List<NilExemptRow> nilExemptList = new ArrayList<>();
        public List<HsnRow> hsnB2bList = new ArrayList<>();
        public List<HsnRow> hsnB2cList = new ArrayList<>();
        public TaxAndDocSummary taxDocSummary = new TaxAndDocSummary();

        public List<ValidationIssue> validationIssues = new ArrayList<>();
        public ReconciliationResult reconciliation = new ReconciliationResult();

        public int getErrorCount() {
            int count = 0;
            for (ValidationIssue issue : validationIssues) {
                if (issue.severity == ValidationIssue.Severity.ERROR) count++;
            }
            return count;
        }

        public int getWarningCount() {
            int count = 0;
            for (ValidationIssue issue : validationIssues) {
                if (issue.severity == ValidationIssue.Severity.WARNING) count++;
            }
            return count;
        }
    }
}
