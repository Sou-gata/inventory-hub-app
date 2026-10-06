package in.gbtsolutions.inventoryhub.reports.purchaseregister;

import android.content.Context;
import android.os.Environment;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PurchaseRegisterCsvExporter {

    public static File exportToCsv(Context context, PurchaseRegisterModels.ReportData report) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "Purchase_Register_" + timestamp + ".csv";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        if (!dir.exists()) dir.mkdirs();

        File outputFile = new File(dir, fileName);

        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            // Header Info
            writer.writeNext(new String[]{"PURCHASE REGISTER REPORT", report.companyName});
            writer.writeNext(new String[]{"GSTIN", report.companyGst != null ? report.companyGst : ""});
            writer.writeNext(new String[]{"Period", (report.startDate != null ? report.startDate : "") + " to " + (report.endDate != null ? report.endDate : "")});
            writer.writeNext(new String[]{""});

            // Executive Summary Block
            writer.writeNext(new String[]{"--- EXECUTIVE SUMMARY ---"});
            writer.writeNext(new String[]{"Metric", "Amount / Count"});
            writer.writeNext(new String[]{"Gross Purchases (Total Inward)", "₹ " + report.summary.grossPurchases.toPlainString()});
            writer.writeNext(new String[]{"Total Taxable Purchases", "₹ " + report.summary.taxableValue.toPlainString()});
            writer.writeNext(new String[]{"Central Tax (CGST ITC)", "₹ " + report.summary.cgst.toPlainString()});
            writer.writeNext(new String[]{"State Tax (SGST ITC)", "₹ " + report.summary.sgst.toPlainString()});
            writer.writeNext(new String[]{"Integrated Tax (IGST ITC)", "₹ " + report.summary.igst.toPlainString()});
            writer.writeNext(new String[]{"Compensation Cess", "₹ " + report.summary.cess.toPlainString()});
            writer.writeNext(new String[]{"Total Input Tax Credit (ITC)", "₹ " + report.summary.totalTax.toPlainString()});
            writer.writeNext(new String[]{"Total Purchase Bills", String.valueOf(report.summary.totalInvoices)});
            writer.writeNext(new String[]{"Registered (B2B) Bills", String.valueOf(report.summary.b2bInvoices)});
            writer.writeNext(new String[]{"Unregistered Bills", String.valueOf(report.summary.b2cInvoices)});
            writer.writeNext(new String[]{"Cancelled Bills", String.valueOf(report.summary.cancelledInvoices)});
            writer.writeNext(new String[]{""});

            // Table Data
            writer.writeNext(new String[]{"--- PURCHASE REGISTER (INWARD TAX DETAILS) ---"});
            writer.writeNext(new String[]{
                    "#", "Invoice / PO #", "Date", "Supplier Name", "Supplier Phone",
                    "Supplier GSTIN", "Type", "Place of Supply", "Taxable Value",
                    "CGST", "SGST", "IGST", "Cess", "Total Tax",
                    "Bill Total", "Payment Terms", "Status"
            });

            int slNo = 1;
            for (PurchaseRegisterModels.PurchaseRegisterRow row : report.rows) {
                writer.writeNext(new String[]{
                        String.valueOf(slNo++),
                        row.invoiceId,
                        row.formattedDate,
                        row.supplierName,
                        row.supplierPhone != null ? row.supplierPhone : "",
                        row.supplierGstin != null ? row.supplierGstin : "",
                        row.isB2B ? "Registered" : "Unregistered",
                        row.placeOfSupply,
                        row.taxableValue.toPlainString(),
                        row.cgstAmount.toPlainString(),
                        row.sgstAmount.toPlainString(),
                        row.igstAmount.toPlainString(),
                        row.cessAmount.toPlainString(),
                        row.totalTax.toPlainString(),
                        row.totalAmount.toPlainString(),
                        row.paymentMethod,
                        row.status
                });
            }

            // Total Row
            writer.writeNext(new String[]{
                    "TOTAL",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    report.summary.taxableValue.toPlainString(),
                    report.summary.cgst.toPlainString(),
                    report.summary.sgst.toPlainString(),
                    report.summary.igst.toPlainString(),
                    report.summary.cess.toPlainString(),
                    report.summary.totalTax.toPlainString(),
                    report.summary.grossPurchases.toPlainString(),
                    "",
                    ""
            });
        }

        return outputFile;
    }
}
