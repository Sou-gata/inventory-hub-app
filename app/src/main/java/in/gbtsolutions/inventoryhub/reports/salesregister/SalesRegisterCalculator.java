package in.gbtsolutions.inventoryhub.reports.salesregister;

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
import in.gbtsolutions.inventoryhub.models.Buyer;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleItemWithProduct;
import in.gbtsolutions.inventoryhub.models.SaleWithBuyer;

public class SalesRegisterCalculator {

    private static final SimpleDateFormat DB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.US);

    static {
        DB_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        DISPLAY_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static SalesRegisterModels.ReportData generateReport(Context context, String startDate, String endDate) {
        Database db = Database.getInstance(context);
        SalesRegisterModels.ReportData report = new SalesRegisterModels.ReportData();
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

        List<SaleWithBuyer> salesWithBuyers = db.saleDao().getSalesWithBuyerForExport(startDate, endDate);
        if (salesWithBuyers == null) {
            salesWithBuyers = new ArrayList<>();
        }

        SalesRegisterModels.SalesRegisterSummary summary = report.summary;

        for (SaleWithBuyer swb : salesWithBuyers) {
            Sale sale = swb.sale;
            if (sale == null) continue;
            Buyer buyer = swb.buyer;

            SalesRegisterModels.SalesRegisterRow row = new SalesRegisterModels.SalesRegisterRow();
            row.saleId = sale.saleId;
            row.invoiceId = !TextUtils.isEmpty(sale.invoiceId) ? sale.invoiceId : ("INV-" + sale.saleId);
            row.billingDate = sale.billingDate != null ? sale.billingDate : "";
            row.formattedDate = formatDate(row.billingDate);

            // Customer Name
            if (!TextUtils.isEmpty(sale.customerName)) {
                row.customerName = sale.customerName;
            } else if (buyer != null && !TextUtils.isEmpty(buyer.buyerName)) {
                row.customerName = buyer.buyerName;
            } else {
                row.customerName = "Walk-in Customer";
            }

            // Customer Phone
            if (!TextUtils.isEmpty(sale.customerPhone)) {
                row.customerPhone = sale.customerPhone;
            } else if (buyer != null && !TextUtils.isEmpty(buyer.phone)) {
                row.customerPhone = buyer.phone;
            }

            // Customer GSTIN
            if (!TextUtils.isEmpty(sale.customerGstin)) {
                row.customerGstin = sale.customerGstin.trim().toUpperCase(Locale.US);
            } else if (buyer != null && !TextUtils.isEmpty(buyer.gst)) {
                row.customerGstin = buyer.gst.trim().toUpperCase(Locale.US);
            }
            row.isB2B = !TextUtils.isEmpty(row.customerGstin);

            // Place of Supply
            row.posStateCode = sale.placeOfSupplyStateCode != null ? sale.placeOfSupplyStateCode : "";
            if (!TextUtils.isEmpty(sale.placeOfSupply)) {
                row.placeOfSupply = sale.placeOfSupply;
            } else if (!TextUtils.isEmpty(row.posStateCode)) {
                row.placeOfSupply = IndianStates.getStateName(row.posStateCode);
            } else if (buyer != null && !TextUtils.isEmpty(buyer.city)) {
                row.placeOfSupply = buyer.city;
            } else {
                row.placeOfSupply = "Local";
            }

            row.paymentMethod = !TextUtils.isEmpty(sale.paymentMethod) ? sale.paymentMethod : "Cash";
            row.status = !TextUtils.isEmpty(sale.status) ? sale.status : "Completed";

            row.taxableValue = SalesRegisterModels.toMoney(sale.subtotalAmount);
            row.cgstAmount = SalesRegisterModels.toMoney(sale.cgstAmount);
            row.sgstAmount = SalesRegisterModels.toMoney(sale.sgstAmount);
            row.igstAmount = SalesRegisterModels.toMoney(sale.igstAmount);
            row.cessAmount = SalesRegisterModels.toMoney(sale.cessAmount);
            row.totalTax = SalesRegisterModels.toMoney(sale.totalGst);
            if (row.totalTax.compareTo(BigDecimal.ZERO) == 0) {
                row.totalTax = row.cgstAmount.add(row.sgstAmount).add(row.igstAmount).add(row.cessAmount);
            }
            row.discountAmount = SalesRegisterModels.toMoney(sale.discountAmount);
            row.otherCharges = SalesRegisterModels.toMoney(sale.otherCharges);
            row.totalAmount = SalesRegisterModels.toMoney(sale.totalAmount);

            // Fetch Items
            List<SaleItemWithProduct> itemWraps = db.saleItemDao().getSaleItemsWithProductSync(sale.saleId);
            if (itemWraps != null) {
                row.itemCount = itemWraps.size();
                for (SaleItemWithProduct iw : itemWraps) {
                    if (iw.saleItem == null) continue;
                    SalesRegisterModels.ItemDetail itemDetail = new SalesRegisterModels.ItemDetail();
                    itemDetail.productName = iw.product != null && !TextUtils.isEmpty(iw.product.productName)
                            ? iw.product.productName : "Product #" + iw.saleItem.productId;
                    itemDetail.sku = iw.product != null && iw.product.sku != null ? iw.product.sku : "";
                    itemDetail.hsnCode = iw.saleItem.hsnCode != null ? iw.saleItem.hsnCode
                            : (iw.product != null && iw.product.hsnCode != null ? iw.product.hsnCode : "");
                    itemDetail.quantity = iw.saleItem.quantity;
                    itemDetail.unitPrice = iw.saleItem.unitPrice;
                    itemDetail.gstRate = iw.saleItem.gstRate > 0 ? iw.saleItem.gstRate
                            : (iw.product != null ? iw.product.gstPercent : 0.0);
                    itemDetail.taxableValue = SalesRegisterModels.toMoney(iw.saleItem.subtotal);
                    itemDetail.cgstAmount = SalesRegisterModels.toMoney(iw.saleItem.cgstAmount);
                    itemDetail.sgstAmount = SalesRegisterModels.toMoney(iw.saleItem.sgstAmount);
                    itemDetail.igstAmount = SalesRegisterModels.toMoney(iw.saleItem.igstAmount);
                    itemDetail.totalTax = itemDetail.cgstAmount.add(itemDetail.sgstAmount).add(itemDetail.igstAmount);
                    itemDetail.lineTotal = itemDetail.taxableValue.add(itemDetail.totalTax);
                    row.items.add(itemDetail);
                }
            }

            report.rows.add(row);

            // Accumulate in Summary if not cancelled
            if (!"Cancelled".equalsIgnoreCase(row.status)) {
                summary.grossSales = summary.grossSales.add(row.totalAmount);
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
