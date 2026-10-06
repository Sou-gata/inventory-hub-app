package in.gbtsolutions.inventoryhub.reports.purchaseregister;

import android.content.Context;
import android.text.TextUtils;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.helpers.IndianStates;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseItemWithProduct;
import in.gbtsolutions.inventoryhub.models.PurchaseWithSupplier;
import in.gbtsolutions.inventoryhub.models.Suppliers;

public class PurchaseRegisterCalculator {

    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    static {
        DB_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        DISPLAY_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static PurchaseRegisterModels.ReportData generateReport(Context context, String startDate, String endDate) {
        Database db = Database.getInstance(context);
        PurchaseRegisterModels.ReportData report = new PurchaseRegisterModels.ReportData();
        report.startDate = startDate;
        report.endDate = endDate;

        // Load company configuration
        List<Config> configs = db.configDao().getAllConfigsSync();
        Map<String, String> configMap = new HashMap<>();
        if (configs != null) {
            for (Config c : configs) {
                configMap.put(c.getConfigKey(), c.getConfigValue());
            }
        }
        report.companyName = configMap.getOrDefault("company_name", "InventoryHub");
        report.companyGst = configMap.getOrDefault("gst_number", "");
        if (TextUtils.isEmpty(report.companyGst)) {
            report.companyGst = configMap.getOrDefault("company_gst", "");
        }
        report.companyPhone = configMap.getOrDefault("company_phone", "");
        report.companyAddress = configMap.getOrDefault("company_address", "");

        List<PurchaseWithSupplier> purchasesWithSuppliers = db.purchaseDao().getPurchasesWithSupplierForExport(startDate, endDate);
        if (purchasesWithSuppliers == null) {
            purchasesWithSuppliers = new ArrayList<>();
        }

        PurchaseRegisterModels.PurchaseRegisterSummary summary = report.summary;

        for (PurchaseWithSupplier pws : purchasesWithSuppliers) {
            Purchase purchase = pws.purchase;
            if (purchase == null) continue;
            Suppliers supplier = pws.supplier;

            PurchaseRegisterModels.PurchaseRegisterRow row = new PurchaseRegisterModels.PurchaseRegisterRow();
            row.purchaseId = purchase.purchaseId;
            row.invoiceId = !TextUtils.isEmpty(purchase.invoiceId) ? purchase.invoiceId : ("PO-" + purchase.purchaseId);
            row.billingDate = purchase.billingDate != null ? purchase.billingDate : "";
            row.formattedDate = formatDate(row.billingDate);

            // Supplier Name
            if (supplier != null && !TextUtils.isEmpty(supplier.supplierName)) {
                row.supplierName = supplier.supplierName;
            } else {
                row.supplierName = "Vendor #" + purchase.supplierId;
            }

            // Supplier Phone
            if (supplier != null && !TextUtils.isEmpty(supplier.phone)) {
                row.supplierPhone = supplier.phone;
            }

            // Supplier GSTIN
            if (supplier != null && !TextUtils.isEmpty(supplier.gst)) {
                row.supplierGstin = supplier.gst.trim().toUpperCase(Locale.US);
            }
            row.isB2B = !TextUtils.isEmpty(row.supplierGstin);

            // Place of Supply
            if (supplier != null && !TextUtils.isEmpty(supplier.stateCode)) {
                row.posStateCode = supplier.stateCode;
                row.placeOfSupply = IndianStates.getStateName(supplier.stateCode);
            } else if (supplier != null && !TextUtils.isEmpty(supplier.city)) {
                row.placeOfSupply = supplier.city;
            } else {
                row.placeOfSupply = "Local";
            }

            row.status = !TextUtils.isEmpty(purchase.status) ? purchase.status : "Completed";
            row.paymentMethod = "Credit / Account";

            row.taxableValue = PurchaseRegisterModels.toMoney(purchase.subtotalAmount);
            row.cgstAmount = PurchaseRegisterModels.toMoney(purchase.cgstAmount);
            row.sgstAmount = PurchaseRegisterModels.toMoney(purchase.sgstAmount);
            row.igstAmount = PurchaseRegisterModels.toMoney(purchase.igstAmount);
            row.totalTax = PurchaseRegisterModels.toMoney(purchase.totalGst);
            if (row.totalTax.compareTo(BigDecimal.ZERO) == 0) {
                row.totalTax = row.cgstAmount.add(row.sgstAmount).add(row.igstAmount);
            }
            row.discountAmount = PurchaseRegisterModels.toMoney(purchase.discountAmount);
            row.otherCharges = PurchaseRegisterModels.toMoney(purchase.otherCharges);
            row.totalAmount = PurchaseRegisterModels.toMoney(purchase.totalAmount);

            // Fetch Items
            List<PurchaseItemWithProduct> itemWraps = db.purchaseItemDao().getPurchaseItemsWithProductSync(purchase.purchaseId);
            if (itemWraps != null) {
                row.itemCount = itemWraps.size();
                for (PurchaseItemWithProduct piw : itemWraps) {
                    if (piw.purchaseItem == null) continue;
                    PurchaseRegisterModels.ItemDetail itemDetail = new PurchaseRegisterModels.ItemDetail();
                    itemDetail.productName = piw.product != null && !TextUtils.isEmpty(piw.product.productName)
                            ? piw.product.productName : "Product #" + piw.purchaseItem.productId;
                    itemDetail.sku = piw.product != null && piw.product.sku != null ? piw.product.sku : "";
                    itemDetail.hsnCode = piw.product != null && piw.product.hsnCode != null ? piw.product.hsnCode : "";
                    itemDetail.unitOfMeasure = piw.product != null && piw.product.unitOfMeasure != null ? piw.product.unitOfMeasure : "";
                    itemDetail.quantity = piw.purchaseItem.quantity;
                    itemDetail.unitPrice = piw.purchaseItem.unitPrice;
                    double rate = piw.purchaseItem.cgstRate + piw.purchaseItem.sgstRate + piw.purchaseItem.igstRate;
                    if (rate <= 0 && piw.product != null) {
                        rate = piw.product.gstPercent;
                    }
                    itemDetail.gstRate = rate;
                    itemDetail.taxableValue = PurchaseRegisterModels.toMoney(piw.purchaseItem.subtotal);
                    itemDetail.cgstAmount = PurchaseRegisterModels.toMoney(piw.purchaseItem.cgstAmount);
                    itemDetail.sgstAmount = PurchaseRegisterModels.toMoney(piw.purchaseItem.sgstAmount);
                    itemDetail.igstAmount = PurchaseRegisterModels.toMoney(piw.purchaseItem.igstAmount);
                    itemDetail.totalTax = itemDetail.cgstAmount.add(itemDetail.sgstAmount).add(itemDetail.igstAmount);
                    itemDetail.lineTotal = itemDetail.taxableValue.add(itemDetail.totalTax);
                    row.items.add(itemDetail);
                }
            }

            report.rows.add(row);

            // Accumulate in Summary if not cancelled
            if (!"Cancelled".equalsIgnoreCase(row.status)) {
                summary.grossPurchases = summary.grossPurchases.add(row.totalAmount);
                summary.taxableValue = summary.taxableValue.add(row.taxableValue);
                summary.cgst = summary.cgst.add(row.cgstAmount);
                summary.sgst = summary.sgst.add(row.sgstAmount);
                summary.igst = summary.igst.add(row.igstAmount);
                summary.cess = summary.cess.add(row.cessAmount);
                summary.totalTax = summary.totalTax.add(row.totalTax);
                summary.totalInvoices++;
                if (row.isB2B) {
                    summary.b2bInvoices++;
                } else {
                    summary.b2cInvoices++;
                }
            } else {
                summary.cancelledInvoices++;
            }
        }

        return report;
    }

    private static String formatDate(String dateStr) {
        if (TextUtils.isEmpty(dateStr)) return "";
        try {
            Date d = DB_DATE_FORMAT.parse(dateStr);
            if (d != null) {
                return DISPLAY_DATE_FORMAT.format(d);
            }
        } catch (Exception ignored) {}
        return dateStr;
    }
}
