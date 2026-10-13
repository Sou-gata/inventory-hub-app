package in.gbtsolutions.inventoryhub.reports.purchaseregister;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class PurchaseRegisterModels {

    public static BigDecimal toMoney(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal roundMoney(BigDecimal val) {
        if (val == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return val.setScale(2, RoundingMode.HALF_UP);
    }

    public static class PurchaseRegisterRow {
        public int purchaseId;
        public String invoiceId = "";
        public String billingDate = "";      // "yyyy-MM-dd"
        public String formattedDate = "";    // "dd MMM yyyy"
        public String supplierName = "";
        public String supplierPhone = "";
        public String supplierGstin = "";
        public boolean isB2B = false;
        public String placeOfSupply = "";
        public String posStateCode = "";
        public String paymentMethod = "Credit / Account";
        public String status = "Completed";

        public BigDecimal taxableValue = BigDecimal.ZERO;
        public BigDecimal cgstAmount = BigDecimal.ZERO;
        public BigDecimal sgstAmount = BigDecimal.ZERO;
        public BigDecimal igstAmount = BigDecimal.ZERO;
        public BigDecimal cessAmount = BigDecimal.ZERO;
        public BigDecimal totalTax = BigDecimal.ZERO;
        public BigDecimal discountAmount = BigDecimal.ZERO;
        public BigDecimal otherCharges = BigDecimal.ZERO;
        public BigDecimal totalAmount = BigDecimal.ZERO;

        public int itemCount = 0;
        public List<ItemDetail> items = new ArrayList<>();
    }

    public static class ItemDetail {
        public String productName = "";
        public String sku = "";
        public String hsnCode = "";
        public String unitOfMeasure = "";
        public double quantity = 0.0;
        public double unitPrice = 0.0;
        public double gstRate = 0.0;
        public BigDecimal taxableValue = BigDecimal.ZERO;
        public BigDecimal cgstAmount = BigDecimal.ZERO;
        public BigDecimal sgstAmount = BigDecimal.ZERO;
        public BigDecimal igstAmount = BigDecimal.ZERO;
        public BigDecimal totalTax = BigDecimal.ZERO;
        public BigDecimal lineTotal = BigDecimal.ZERO;
    }

    public static class PurchaseRegisterSummary {
        public BigDecimal grossPurchases = BigDecimal.ZERO;
        public BigDecimal taxableValue = BigDecimal.ZERO;
        public BigDecimal cgst = BigDecimal.ZERO;
        public BigDecimal sgst = BigDecimal.ZERO;
        public BigDecimal igst = BigDecimal.ZERO;
        public BigDecimal cess = BigDecimal.ZERO;
        public BigDecimal totalTax = BigDecimal.ZERO;
        public int totalInvoices = 0;
        public int b2bInvoices = 0;
        public int b2cInvoices = 0;
        public int cancelledInvoices = 0;
    }

    public static class ReportData {
        public String companyName = "";
        public String companyGst = "";
        public String companyPhone = "";
        public String companyAddress = "";
        public String startDate = "";
        public String endDate = "";
        public PurchaseRegisterSummary summary = new PurchaseRegisterSummary();
        public List<PurchaseRegisterRow> rows = new ArrayList<>();

        public void ensureNonNull() {
            if (summary == null) summary = new PurchaseRegisterSummary();
            if (summary.grossPurchases == null) summary.grossPurchases = BigDecimal.ZERO;
            if (summary.taxableValue == null) summary.taxableValue = BigDecimal.ZERO;
            if (summary.cgst == null) summary.cgst = BigDecimal.ZERO;
            if (summary.sgst == null) summary.sgst = BigDecimal.ZERO;
            if (summary.igst == null) summary.igst = BigDecimal.ZERO;
            if (summary.cess == null) summary.cess = BigDecimal.ZERO;
            if (summary.totalTax == null) summary.totalTax = BigDecimal.ZERO;

            if (rows == null) rows = new ArrayList<>();
            for (PurchaseRegisterRow row : rows) {
                if (row == null) continue;
                if (row.taxableValue == null) row.taxableValue = BigDecimal.ZERO;
                if (row.cgstAmount == null) row.cgstAmount = BigDecimal.ZERO;
                if (row.sgstAmount == null) row.sgstAmount = BigDecimal.ZERO;
                if (row.igstAmount == null) row.igstAmount = BigDecimal.ZERO;
                if (row.cessAmount == null) row.cessAmount = BigDecimal.ZERO;
                if (row.totalTax == null) row.totalTax = BigDecimal.ZERO;
                if (row.discountAmount == null) row.discountAmount = BigDecimal.ZERO;
                if (row.otherCharges == null) row.otherCharges = BigDecimal.ZERO;
                if (row.totalAmount == null) row.totalAmount = BigDecimal.ZERO;
                if (row.items == null) row.items = new ArrayList<>();
                for (ItemDetail item : row.items) {
                    if (item == null) continue;
                    if (item.taxableValue == null) item.taxableValue = BigDecimal.ZERO;
                    if (item.cgstAmount == null) item.cgstAmount = BigDecimal.ZERO;
                    if (item.sgstAmount == null) item.sgstAmount = BigDecimal.ZERO;
                    if (item.igstAmount == null) item.igstAmount = BigDecimal.ZERO;
                    if (item.totalTax == null) item.totalTax = BigDecimal.ZERO;
                    if (item.lineTotal == null) item.lineTotal = BigDecimal.ZERO;
                }
            }
        }
    }
}
