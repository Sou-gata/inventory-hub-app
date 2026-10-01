package in.gbtsolutions.inventoryhub.reports.gstr1;

import android.content.Context;
import android.text.TextUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.helpers.HelperMethods;
import in.gbtsolutions.inventoryhub.helpers.IndianStates;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.CreditDebitNote;
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleItemWithProduct;

public class GSTR1ReportCalculator {

    public static GSTR1Models.ReportData generateReport(Context context, String startDate, String endDate, double b2cLargeThreshold) {
        Database db = Database.getInstance(context);
        GSTR1Models.ReportData report = new GSTR1Models.ReportData();
        report.startDate = startDate;
        report.endDate = endDate;

        // Load company info
        List<Config> configs = db.configDao().getAllConfigsSync();
        Map<String, String> configMap = new HashMap<>();
        if (configs != null) {
            for (Config c : configs) {
                configMap.put(c.getConfigKey(), c.getConfigValue());
            }
        }
        report.companyGst = configMap.getOrDefault("gst_number", "");
        report.companyName = configMap.getOrDefault("company_name", "InventoryHub");
        String sellerStateCode = HelperMethods.extractStateCode(report.companyGst);
        if (sellerStateCode == null) sellerStateCode = "19";

        // Fetch sales in date range
        List<Sale> sales = db.saleDao().getSalesForExport(startDate, endDate);
        if (sales == null) sales = new ArrayList<>();

        // Group accumulators for B2B, B2C, Export, Nil/Exempt, HSN
        Map<String, B2BAccumulator> b2bMap = new LinkedHashMap<>();
        Map<String, B2CAccumulator> b2cLargeMap = new LinkedHashMap<>();
        Map<String, B2CAccumulator> b2cOthersMap = new LinkedHashMap<>();
        Map<String, ExportAccumulator> exportMap = new LinkedHashMap<>();
        Map<String, NilExemptAccumulator> nilMap = new LinkedHashMap<>();
        Map<String, HsnAccumulator> hsnB2bMap = new LinkedHashMap<>();
        Map<String, HsnAccumulator> hsnB2cMap = new LinkedHashMap<>();

        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalTaxable = BigDecimal.ZERO;
        BigDecimal totalCgst = BigDecimal.ZERO;
        BigDecimal totalSgst = BigDecimal.ZERO;
        BigDecimal totalIgst = BigDecimal.ZERO;
        BigDecimal totalCess = BigDecimal.ZERO;
        BigDecimal totalGst = BigDecimal.ZERO;
        int activeInvoicesCount = 0;

        for (Sale sale : sales) {
            if ("Cancelled".equalsIgnoreCase(sale.status)) {
                continue;
            }

            activeInvoicesCount++;
            BigDecimal invoiceTotal = GSTR1Models.toMoney(sale.totalAmount);
            totalGross = totalGross.add(invoiceTotal);

            List<SaleItemWithProduct> items = db.saleItemDao().getSaleItemsWithProductSync(sale.saleId);
            if (items == null) items = new ArrayList<>();

            // Determine recipient details
            String customerGstin = sale.customerGstin != null ? sale.customerGstin.trim().toUpperCase(Locale.US) : "";
            boolean isB2B = !TextUtils.isEmpty(customerGstin);
            String customerName = !TextUtils.isEmpty(sale.customerName) ? sale.customerName : (isB2B ? "Registered Customer" : "Walk-in Customer");
            String posStateCode = !TextUtils.isEmpty(sale.placeOfSupplyStateCode) ? sale.placeOfSupplyStateCode : sellerStateCode;
            String posStateName = IndianStates.getStateName(posStateCode);

            boolean isSameState = HelperMethods.isSameStateByCode(report.companyGst, posStateCode);

            // Invoice-level GST totals
            totalCgst = totalCgst.add(GSTR1Models.toMoney(sale.cgstAmount));
            totalSgst = totalSgst.add(GSTR1Models.toMoney(sale.sgstAmount));
            totalIgst = totalIgst.add(GSTR1Models.toMoney(sale.igstAmount));
            totalCess = totalCess.add(GSTR1Models.toMoney(sale.cessAmount));
            totalGst = totalGst.add(GSTR1Models.toMoney(sale.totalGst));

            // Process each item
            for (SaleItemWithProduct itemWrap : items) {
                if (itemWrap.saleItem == null) continue;
                double rate = itemWrap.saleItem.gstRate;
                if (rate == 0.0 && itemWrap.product != null) {
                    rate = itemWrap.product.gstPercent;
                }
                BigDecimal itemTaxable = GSTR1Models.toMoney(itemWrap.saleItem.subtotal);
                BigDecimal itemCgst = GSTR1Models.toMoney(itemWrap.saleItem.cgstAmount);
                BigDecimal itemSgst = GSTR1Models.toMoney(itemWrap.saleItem.sgstAmount);
                BigDecimal itemIgst = GSTR1Models.toMoney(itemWrap.saleItem.igstAmount);
                BigDecimal itemCess = GSTR1Models.toMoney(itemWrap.saleItem.cessAmount);
                BigDecimal itemTotalTax = itemCgst.add(itemSgst).add(itemIgst).add(itemCess);
                BigDecimal itemInvoiceVal = itemTaxable.add(itemTotalTax);

                totalTaxable = totalTaxable.add(itemTaxable);

                String gstCategory = (itemWrap.product != null && itemWrap.product.gstCategory != null)
                        ? itemWrap.product.gstCategory.toUpperCase(Locale.US)
                        : "TAXABLE";

                // Check Nil / Exempt / Non-GST
                if ("NIL_RATED".equals(gstCategory) || "EXEMPT".equals(gstCategory) || "NON_GST".equals(gstCategory) || rate == 0.0) {
                    String nilCat = "NIL_RATED".equals(gstCategory) || rate == 0.0 ? "Nil Rated" : ("EXEMPT".equals(gstCategory) ? "Exempt" : "Non-GST");
                    String supplyType = isSameState ? "Intra-State" : "Inter-State";
                    String recipientType = isB2B ? "Registered" : "Unregistered";
                    String nilKey = nilCat + "|" + supplyType + "|" + recipientType;
                    NilExemptAccumulator acc = nilMap.computeIfAbsent(nilKey, k -> new NilExemptAccumulator(nilCat, supplyType, recipientType));
                    acc.totalValue = acc.totalValue.add(itemTaxable);
                    acc.invoices.add(sale.saleId);
                }

                // Table 1: B2B
                if (isB2B) {
                    String b2bKey = customerGstin + "|" + posStateCode + "|" + rate;
                    B2BAccumulator acc = b2bMap.get(b2bKey);
                    if (acc == null) {
                        acc = new B2BAccumulator(customerGstin, customerName, posStateName, posStateCode, rate);
                        b2bMap.put(b2bKey, acc);
                    }
                    acc.taxableValue = acc.taxableValue.add(itemTaxable);
                    acc.cgst = acc.cgst.add(itemCgst);
                    acc.sgst = acc.sgst.add(itemSgst);
                    acc.igst = acc.igst.add(itemIgst);
                    acc.cess = acc.cess.add(itemCess);
                    acc.totalTax = acc.totalTax.add(itemTotalTax);
                    acc.invoiceValue = acc.invoiceValue.add(itemInvoiceVal);
                    acc.invoices.add(sale.saleId);
                }
                // Table 2: B2C (Large vs Others)
                else {
                    boolean isB2CLarge = !isSameState && sale.totalAmount > b2cLargeThreshold;
                    String category = isB2CLarge ? "B2C Large" : "B2C Others";
                    String b2cKey = category + "|" + posStateCode + "|" + rate;
                    Map<String, B2CAccumulator> targetMap = isB2CLarge ? b2cLargeMap : b2cOthersMap;
                    B2CAccumulator acc = targetMap.get(b2cKey);
                    if (acc == null) {
                        acc = new B2CAccumulator(category, posStateName, posStateCode, rate);
                        targetMap.put(b2cKey, acc);
                    }
                    acc.taxableValue = acc.taxableValue.add(itemTaxable);
                    acc.cgst = acc.cgst.add(itemCgst);
                    acc.sgst = acc.sgst.add(itemSgst);
                    acc.igst = acc.igst.add(itemIgst);
                    acc.cess = acc.cess.add(itemCess);
                    acc.totalTax = acc.totalTax.add(itemTotalTax);
                    acc.invoiceValue = acc.invoiceValue.add(itemInvoiceVal);
                    acc.invoices.add(sale.saleId);
                }

                // Table 6: HSN / SAC Summary
                String hsn = (itemWrap.saleItem.hsnCode != null && !itemWrap.saleItem.hsnCode.trim().isEmpty())
                        ? itemWrap.saleItem.hsnCode.trim()
                        : (itemWrap.product != null && itemWrap.product.hsnCode != null ? itemWrap.product.hsnCode.trim() : "UNCLASSIFIED");
                String uqc = (itemWrap.saleItem.uqc != null && !itemWrap.saleItem.uqc.trim().isEmpty())
                        ? itemWrap.saleItem.uqc.trim()
                        : (itemWrap.product != null && itemWrap.product.uqc != null ? itemWrap.product.uqc.trim() : "PCS");
                String desc = (itemWrap.product != null && itemWrap.product.productName != null) ? itemWrap.product.productName : "Product";
                String supplyType = isB2B ? "B2B" : "B2C";
                String hsnKey = hsn + "|" + uqc + "|" + rate + "|" + supplyType;

                Map<String, HsnAccumulator> targetHsnMap = isB2B ? hsnB2bMap : hsnB2cMap;
                HsnAccumulator hsnAcc = targetHsnMap.get(hsnKey);
                if (hsnAcc == null) {
                    hsnAcc = new HsnAccumulator(hsn, desc, uqc, rate, supplyType);
                    targetHsnMap.put(hsnKey, hsnAcc);
                }
                hsnAcc.quantity += itemWrap.saleItem.quantity;
                hsnAcc.taxableValue = hsnAcc.taxableValue.add(itemTaxable);
                hsnAcc.cgst = hsnAcc.cgst.add(itemCgst);
                hsnAcc.sgst = hsnAcc.sgst.add(itemSgst);
                hsnAcc.igst = hsnAcc.igst.add(itemIgst);
                hsnAcc.cess = hsnAcc.cess.add(itemCess);
                hsnAcc.totalTax = hsnAcc.totalTax.add(itemTotalTax);
                hsnAcc.totalValue = hsnAcc.totalValue.add(itemInvoiceVal);
            }
        }

        // Table 4: Credit / Debit Notes
        List<CreditDebitNote> notes = db.creditDebitNoteDao().getNotesBetweenDates(startDate, endDate);
        int creditNotesCount = 0;
        int debitNotesCount = 0;
        if (notes != null) {
            for (CreditDebitNote note : notes) {
                if ("Cancelled".equalsIgnoreCase(note.status)) continue;
                boolean isCredit = "CREDIT".equalsIgnoreCase(note.noteType);
                if (isCredit) creditNotesCount++; else debitNotesCount++;

                GSTR1Models.CreditDebitNoteRow row = new GSTR1Models.CreditDebitNoteRow();
                row.noteType = isCredit ? "CREDIT" : "DEBIT";
                row.recipientType = !TextUtils.isEmpty(note.customerGstin) ? "Registered" : "Unregistered";
                row.taxRate = note.gstRate;
                row.noteCount = 1;
                row.taxableValue = GSTR1Models.toMoney(note.taxableValue);
                row.cgst = GSTR1Models.toMoney(note.cgstAmount);
                row.sgst = GSTR1Models.toMoney(note.sgstAmount);
                row.igst = GSTR1Models.toMoney(note.igstAmount);
                row.cess = GSTR1Models.toMoney(note.cessAmount);
                row.totalTax = row.cgst.add(row.sgst).add(row.igst).add(row.cess);
                row.totalValue = GSTR1Models.toMoney(note.totalAmount);

                if (isCredit) {
                    report.creditNotesList.add(row);
                } else {
                    report.debitNotesList.add(row);
                }
            }
        }

        // Build Table 1 rows
        for (B2BAccumulator acc : b2bMap.values()) {
            GSTR1Models.B2BRow row = new GSTR1Models.B2BRow();
            row.customerGstin = acc.customerGstin;
            row.customerName = acc.customerName;
            row.placeOfSupply = acc.posStateName;
            row.posStateCode = acc.posStateCode;
            row.taxRate = acc.taxRate;
            row.invoiceCount = acc.invoices.size();
            row.taxableValue = GSTR1Models.roundMoney(acc.taxableValue);
            row.cgst = GSTR1Models.roundMoney(acc.cgst);
            row.sgst = GSTR1Models.roundMoney(acc.sgst);
            row.igst = GSTR1Models.roundMoney(acc.igst);
            row.cess = GSTR1Models.roundMoney(acc.cess);
            row.totalTax = GSTR1Models.roundMoney(acc.totalTax);
            row.invoiceValue = GSTR1Models.roundMoney(acc.invoiceValue);
            report.b2bList.add(row);
        }

        // Build Table 2 rows (Large & Others)
        for (B2CAccumulator acc : b2cLargeMap.values()) {
            report.b2cLargeList.add(createB2CRow(acc));
        }
        for (B2CAccumulator acc : b2cOthersMap.values()) {
            report.b2cOthersList.add(createB2CRow(acc));
        }

        // Build Table 3 rows
        for (ExportAccumulator acc : exportMap.values()) {
            GSTR1Models.ExportRow row = new GSTR1Models.ExportRow();
            row.category = acc.category;
            row.invoiceCount = acc.invoices.size();
            row.taxableValue = GSTR1Models.roundMoney(acc.taxableValue);
            row.invoiceValue = GSTR1Models.roundMoney(acc.invoiceValue);
            row.cgst = GSTR1Models.roundMoney(acc.cgst);
            row.sgst = GSTR1Models.roundMoney(acc.sgst);
            row.igst = GSTR1Models.roundMoney(acc.igst);
            row.cess = GSTR1Models.roundMoney(acc.cess);
            row.shippingBillNo = acc.shippingBillNo;
            row.shippingBillDate = acc.shippingBillDate;
            row.portCode = acc.portCode;
            report.exportList.add(row);
        }

        // Build Table 5 rows
        for (NilExemptAccumulator acc : nilMap.values()) {
            GSTR1Models.NilExemptRow row = new GSTR1Models.NilExemptRow();
            row.category = acc.category;
            row.supplyType = acc.supplyType;
            row.recipientType = acc.recipientType;
            row.invoiceCount = acc.invoices.size();
            row.reportedValue = GSTR1Models.roundMoney(acc.totalValue);
            report.nilExemptList.add(row);
        }

        // Build Table 6 rows (HSN B2B & B2C)
        for (HsnAccumulator acc : hsnB2bMap.values()) {
            report.hsnB2bList.add(createHsnRow(acc));
        }
        for (HsnAccumulator acc : hsnB2cMap.values()) {
            report.hsnB2cList.add(createHsnRow(acc));
        }

        // Build Table 7: Tax & Document Summary
        int cancelledInvoicesCount = db.saleDao().countCancelledSales(startDate, endDate);
        report.taxDocSummary.grossInvoiceValue = GSTR1Models.roundMoney(totalGross);
        report.taxDocSummary.taxableValue = GSTR1Models.roundMoney(totalTaxable);
        report.taxDocSummary.cgst = GSTR1Models.roundMoney(totalCgst);
        report.taxDocSummary.sgst = GSTR1Models.roundMoney(totalSgst);
        report.taxDocSummary.igst = GSTR1Models.roundMoney(totalIgst);
        report.taxDocSummary.cess = GSTR1Models.roundMoney(totalCess);
        report.taxDocSummary.totalGst = GSTR1Models.roundMoney(totalGst);

        report.taxDocSummary.taxInvoicesCount = activeInvoicesCount;
        report.taxDocSummary.cancelledInvoicesCount = cancelledInvoicesCount;
        report.taxDocSummary.creditNotesCount = creditNotesCount;
        report.taxDocSummary.debitNotesCount = debitNotesCount;
        report.taxDocSummary.netDocumentsCount = activeInvoicesCount + debitNotesCount - creditNotesCount - cancelledInvoicesCount;

        // Populate Top KPI Cards
        report.kpi.totalSales = report.taxDocSummary.grossInvoiceValue;
        report.kpi.taxableValue = report.taxDocSummary.taxableValue;
        report.kpi.cgst = report.taxDocSummary.cgst;
        report.kpi.sgst = report.taxDocSummary.sgst;
        report.kpi.igst = report.taxDocSummary.igst;
        report.kpi.cess = report.taxDocSummary.cess;
        report.kpi.totalGst = report.taxDocSummary.totalGst;
        report.kpi.totalInvoices = activeInvoicesCount;

        // Run Validation & Reconciliation
        report.validationIssues = GSTR1Validator.validateReport(sales, itemsList(db, sales));
        report.reconciliation = GSTR1Reconciler.reconcile(report);

        return report;
    }

    private static List<SaleItemWithProduct> itemsList(Database db, List<Sale> sales) {
        List<SaleItemWithProduct> list = new ArrayList<>();
        for (Sale s : sales) {
            List<SaleItemWithProduct> items = db.saleItemDao().getSaleItemsWithProductSync(s.saleId);
            if (items != null) list.addAll(items);
        }
        return list;
    }

    private static GSTR1Models.B2CRow createB2CRow(B2CAccumulator acc) {
        GSTR1Models.B2CRow row = new GSTR1Models.B2CRow();
        row.supplyCategory = acc.category;
        row.placeOfSupply = acc.posStateName;
        row.posStateCode = acc.posStateCode;
        row.taxRate = acc.taxRate;
        row.invoiceCount = acc.invoices.size();
        row.taxableValue = GSTR1Models.roundMoney(acc.taxableValue);
        row.cgst = GSTR1Models.roundMoney(acc.cgst);
        row.sgst = GSTR1Models.roundMoney(acc.sgst);
        row.igst = GSTR1Models.roundMoney(acc.igst);
        row.cess = GSTR1Models.roundMoney(acc.cess);
        row.totalTax = GSTR1Models.roundMoney(acc.totalTax);
        row.invoiceValue = GSTR1Models.roundMoney(acc.invoiceValue);
        return row;
    }

    private static GSTR1Models.HsnRow createHsnRow(HsnAccumulator acc) {
        GSTR1Models.HsnRow row = new GSTR1Models.HsnRow();
        row.hsnCode = acc.hsnCode;
        row.description = acc.description;
        row.uqc = acc.uqc;
        row.quantity = acc.quantity;
        row.taxRate = acc.taxRate;
        row.supplyType = acc.supplyType;
        row.taxableValue = GSTR1Models.roundMoney(acc.taxableValue);
        row.cgst = GSTR1Models.roundMoney(acc.cgst);
        row.sgst = GSTR1Models.roundMoney(acc.sgst);
        row.igst = GSTR1Models.roundMoney(acc.igst);
        row.cess = GSTR1Models.roundMoney(acc.cess);
        row.totalTax = GSTR1Models.roundMoney(acc.totalTax);
        row.totalValue = GSTR1Models.roundMoney(acc.totalValue);
        return row;
    }

    // Internal helper accumulators
    private static class B2BAccumulator {
        final String customerGstin;
        final String customerName;
        final String posStateName;
        final String posStateCode;
        final double taxRate;
        final Set<Integer> invoices = new HashSet<>();
        BigDecimal taxableValue = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        BigDecimal cess = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal invoiceValue = BigDecimal.ZERO;

        B2BAccumulator(String customerGstin, String customerName, String posStateName, String posStateCode, double taxRate) {
            this.customerGstin = customerGstin;
            this.customerName = customerName;
            this.posStateName = posStateName;
            this.posStateCode = posStateCode;
            this.taxRate = taxRate;
        }
    }

    private static class B2CAccumulator {
        final String category;
        final String posStateName;
        final String posStateCode;
        final double taxRate;
        final Set<Integer> invoices = new HashSet<>();
        BigDecimal taxableValue = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        BigDecimal cess = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal invoiceValue = BigDecimal.ZERO;

        B2CAccumulator(String category, String posStateName, String posStateCode, double taxRate) {
            this.category = category;
            this.posStateName = posStateName;
            this.posStateCode = posStateCode;
            this.taxRate = taxRate;
        }
    }

    private static class ExportAccumulator {
        final String category;
        final Set<Integer> invoices = new HashSet<>();
        BigDecimal taxableValue = BigDecimal.ZERO;
        BigDecimal invoiceValue = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        BigDecimal cess = BigDecimal.ZERO;
        String shippingBillNo;
        String shippingBillDate;
        String portCode;

        ExportAccumulator(String category) {
            this.category = category;
        }
    }

    private static class NilExemptAccumulator {
        final String category;
        final String supplyType;
        final String recipientType;
        final Set<Integer> invoices = new HashSet<>();
        BigDecimal totalValue = BigDecimal.ZERO;

        NilExemptAccumulator(String category, String supplyType, String recipientType) {
            this.category = category;
            this.supplyType = supplyType;
            this.recipientType = recipientType;
        }
    }

    private static class HsnAccumulator {
        final String hsnCode;
        final String description;
        final String uqc;
        final double taxRate;
        final String supplyType;
        double quantity = 0.0;
        BigDecimal taxableValue = BigDecimal.ZERO;
        BigDecimal cgst = BigDecimal.ZERO;
        BigDecimal sgst = BigDecimal.ZERO;
        BigDecimal igst = BigDecimal.ZERO;
        BigDecimal cess = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;

        HsnAccumulator(String hsnCode, String description, String uqc, double taxRate, String supplyType) {
            this.hsnCode = hsnCode;
            this.description = description;
            this.uqc = uqc;
            this.taxRate = taxRate;
            this.supplyType = supplyType;
        }
    }
}
