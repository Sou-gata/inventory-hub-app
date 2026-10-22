package in.gbtsolutions.inventoryhub.reports.gstr1;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

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
        public enum Severity {
            @SerializedName("ERROR") ERROR, @SerializedName("WARNING") WARNING
        }

        @SerializedName("severity")
        public Severity severity;
        @SerializedName("invoiceOrRef")
        public String invoiceOrRef;
        @SerializedName("title")
        public String title;
        @SerializedName("description")
        public String description;

        public ValidationIssue() {
            this.severity = Severity.WARNING;
            this.invoiceOrRef = "";
            this.title = "";
            this.description = "";
        }

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

        @SerializedName("errorCount")
        public int errorCount = -1;

        @SerializedName("warningCount")
        public int warningCount = -1;

        public int getErrorCount() {
            if (errorCount >= 0) return errorCount;
            int count = 0;
            if (validationIssues != null) {
                for (ValidationIssue issue : validationIssues) {
                    if (issue != null && issue.severity == ValidationIssue.Severity.ERROR) count++;
                }
            }
            return count;
        }

        public int getWarningCount() {
            if (warningCount >= 0) return warningCount;
            int count = 0;
            if (validationIssues != null) {
                for (ValidationIssue issue : validationIssues) {
                    if (issue != null && issue.severity == ValidationIssue.Severity.WARNING)
                        count++;
                }
            }
            return count;
        }

        public void ensureNonNull() {
            if (kpi == null) kpi = new KPISummary();
            if (kpi.totalSales == null) kpi.totalSales = BigDecimal.ZERO;
            if (kpi.taxableValue == null) kpi.taxableValue = BigDecimal.ZERO;
            if (kpi.cgst == null) kpi.cgst = BigDecimal.ZERO;
            if (kpi.sgst == null) kpi.sgst = BigDecimal.ZERO;
            if (kpi.igst == null) kpi.igst = BigDecimal.ZERO;
            if (kpi.cess == null) kpi.cess = BigDecimal.ZERO;
            if (kpi.totalGst == null) kpi.totalGst = BigDecimal.ZERO;

            if (b2bList == null) b2bList = new ArrayList<>();
            for (B2BRow row : b2bList) {
                if (row == null) continue;
                if (row.taxableValue == null) row.taxableValue = BigDecimal.ZERO;
                if (row.cgst == null) row.cgst = BigDecimal.ZERO;
                if (row.sgst == null) row.sgst = BigDecimal.ZERO;
                if (row.igst == null) row.igst = BigDecimal.ZERO;
                if (row.cess == null) row.cess = BigDecimal.ZERO;
                if (row.totalTax == null) row.totalTax = BigDecimal.ZERO;
                if (row.invoiceValue == null) row.invoiceValue = BigDecimal.ZERO;
            }

            if (b2cLargeList == null) b2cLargeList = new ArrayList<>();
            for (B2CRow row : b2cLargeList) {
                if (row == null) continue;
                if (row.taxableValue == null) row.taxableValue = BigDecimal.ZERO;
                if (row.cgst == null) row.cgst = BigDecimal.ZERO;
                if (row.sgst == null) row.sgst = BigDecimal.ZERO;
                if (row.igst == null) row.igst = BigDecimal.ZERO;
                if (row.cess == null) row.cess = BigDecimal.ZERO;
                if (row.totalTax == null) row.totalTax = BigDecimal.ZERO;
                if (row.invoiceValue == null) row.invoiceValue = BigDecimal.ZERO;
            }

            if (b2cOthersList == null) b2cOthersList = new ArrayList<>();
            for (B2CRow row : b2cOthersList) {
                if (row == null) continue;
                if (row.taxableValue == null) row.taxableValue = BigDecimal.ZERO;
                if (row.cgst == null) row.cgst = BigDecimal.ZERO;
                if (row.sgst == null) row.sgst = BigDecimal.ZERO;
                if (row.igst == null) row.igst = BigDecimal.ZERO;
                if (row.cess == null) row.cess = BigDecimal.ZERO;
                if (row.totalTax == null) row.totalTax = BigDecimal.ZERO;
                if (row.invoiceValue == null) row.invoiceValue = BigDecimal.ZERO;
            }

            if (exportList == null) exportList = new ArrayList<>();
            for (ExportRow row : exportList) {
                if (row == null) continue;
                if (row.taxableValue == null) row.taxableValue = BigDecimal.ZERO;
                if (row.invoiceValue == null) row.invoiceValue = BigDecimal.ZERO;
                if (row.igst == null) row.igst = BigDecimal.ZERO;
                if (row.cgst == null) row.cgst = BigDecimal.ZERO;
                if (row.sgst == null) row.sgst = BigDecimal.ZERO;
                if (row.cess == null) row.cess = BigDecimal.ZERO;
            }

            if (creditNotesList == null) creditNotesList = new ArrayList<>();
            for (CreditDebitNoteRow row : creditNotesList) {
                if (row == null) continue;
                if (row.taxableValue == null) row.taxableValue = BigDecimal.ZERO;
                if (row.cgst == null) row.cgst = BigDecimal.ZERO;
                if (row.sgst == null) row.sgst = BigDecimal.ZERO;
                if (row.igst == null) row.igst = BigDecimal.ZERO;
                if (row.cess == null) row.cess = BigDecimal.ZERO;
                if (row.totalTax == null) row.totalTax = BigDecimal.ZERO;
                if (row.totalValue == null) row.totalValue = BigDecimal.ZERO;
            }

            if (debitNotesList == null) debitNotesList = new ArrayList<>();
            for (CreditDebitNoteRow row : debitNotesList) {
                if (row == null) continue;
                if (row.taxableValue == null) row.taxableValue = BigDecimal.ZERO;
                if (row.cgst == null) row.cgst = BigDecimal.ZERO;
                if (row.sgst == null) row.sgst = BigDecimal.ZERO;
                if (row.igst == null) row.igst = BigDecimal.ZERO;
                if (row.cess == null) row.cess = BigDecimal.ZERO;
                if (row.totalTax == null) row.totalTax = BigDecimal.ZERO;
                if (row.totalValue == null) row.totalValue = BigDecimal.ZERO;
            }

            if (nilExemptList == null) nilExemptList = new ArrayList<>();
            for (NilExemptRow row : nilExemptList) {
                if (row == null) continue;
                if (row.reportedValue == null) row.reportedValue = BigDecimal.ZERO;
            }

            if (hsnB2bList == null) hsnB2bList = new ArrayList<>();
            for (HsnRow row : hsnB2bList) {
                if (row == null) continue;
                if (row.taxableValue == null) row.taxableValue = BigDecimal.ZERO;
                if (row.cgst == null) row.cgst = BigDecimal.ZERO;
                if (row.sgst == null) row.sgst = BigDecimal.ZERO;
                if (row.igst == null) row.igst = BigDecimal.ZERO;
                if (row.cess == null) row.cess = BigDecimal.ZERO;
                if (row.totalTax == null) row.totalTax = BigDecimal.ZERO;
                if (row.totalValue == null) row.totalValue = BigDecimal.ZERO;
            }

            if (hsnB2cList == null) hsnB2cList = new ArrayList<>();
            for (HsnRow row : hsnB2cList) {
                if (row == null) continue;
                if (row.taxableValue == null) row.taxableValue = BigDecimal.ZERO;
                if (row.cgst == null) row.cgst = BigDecimal.ZERO;
                if (row.sgst == null) row.sgst = BigDecimal.ZERO;
                if (row.igst == null) row.igst = BigDecimal.ZERO;
                if (row.cess == null) row.cess = BigDecimal.ZERO;
                if (row.totalTax == null) row.totalTax = BigDecimal.ZERO;
                if (row.totalValue == null) row.totalValue = BigDecimal.ZERO;
            }

            if (taxDocSummary == null) taxDocSummary = new TaxAndDocSummary();
            if (taxDocSummary.grossInvoiceValue == null)
                taxDocSummary.grossInvoiceValue = BigDecimal.ZERO;
            if (taxDocSummary.taxableValue == null) taxDocSummary.taxableValue = BigDecimal.ZERO;
            if (taxDocSummary.cgst == null) taxDocSummary.cgst = BigDecimal.ZERO;
            if (taxDocSummary.sgst == null) taxDocSummary.sgst = BigDecimal.ZERO;
            if (taxDocSummary.igst == null) taxDocSummary.igst = BigDecimal.ZERO;
            if (taxDocSummary.cess == null) taxDocSummary.cess = BigDecimal.ZERO;
            if (taxDocSummary.totalGst == null) taxDocSummary.totalGst = BigDecimal.ZERO;

            if (validationIssues == null) validationIssues = new ArrayList<>();
            if (reconciliation == null) reconciliation = new ReconciliationResult();
            if (reconciliation.expectedTaxable == null)
                reconciliation.expectedTaxable = BigDecimal.ZERO;
            if (reconciliation.hsnTaxable == null) reconciliation.hsnTaxable = BigDecimal.ZERO;
            if (reconciliation.difference == null) reconciliation.difference = BigDecimal.ZERO;
        }
    }
}
