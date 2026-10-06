package in.gbtsolutions.inventoryhub.reports.salesregister;

import android.content.Context;
import android.os.Environment;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SalesRegisterCsvExporter {

    public static File exportToCsv(Context context, SalesRegisterModels.ReportData report) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "Sales_Register_" + timestamp + ".csv";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        if (!dir.exists()) dir.mkdirs();

        File outputFile = new File(dir, fileName);

        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            // Header Info
            writer.writeNext(new String[]{"SALES REGISTER REPORT", report.companyName});
            writer.writeNext(new String[]{"GSTIN", report.companyGst != null ? report.companyGst : ""});
            writer.writeNext(new String[]{"Period", (report.startDate != null ? report.startDate : "") + " to " + (report.endDate != null ? report.endDate : "")});
            writer.writeNext(new String[]{""});

            // Executive Summary Block
            writer.writeNext(new String[]{"--- EXECUTIVE SUMMARY ---"});
            writer.writeNext(new String[]{"Metric", "Amount / Count"});
            writer.writeNext(new String[]{"Gross Sales (Invoice Total)", "₹ " + report.summary.grossSales.toPlainString()});
            writer.writeNext(new String[]{"Total Taxable Turnover", "₹ " + report.summary.taxableValue.toPlainString()});
            writer.writeNext(new String[]{"Central Tax (CGST)", "₹ " + report.summary.cgst.toPlainString()});
            writer.writeNext(new String[]{"State Tax (SGST)", "₹ " + report.summary.sgst.toPlainString()});
            writer.writeNext(new String[]{"Integrated Tax (IGST)", "₹ " + report.summary.igst.toPlainString()});
            writer.writeNext(new String[]{"Compensation Cess", "₹ " + report.summary.cess.toPlainString()});
            writer.writeNext(new String[]{"Total Tax Liability", "₹ " + report.summary.totalTax.toPlainString()});
            writer.writeNext(new String[]{"Active Invoices Count", String.valueOf(report.summary.totalInvoices)});
            writer.writeNext(new String[]{"B2B Invoices Count", String.valueOf(report.summary.b2bInvoices)});
            writer.writeNext(new String[]{"B2C Invoices Count", String.valueOf(report.summary.b2cInvoices)});
            writer.writeNext(new String[]{"Cancelled Invoices", String.valueOf(report.summary.cancelledInvoices)});
            writer.writeNext(new String[]{""});

            // Table Data
            writer.writeNext(new String[]{"--- SALES REGISTER (INVOICE-WISE TAX DETAILS) ---"});
            writer.writeNext(new String[]{
                    "#", "Invoice No", "Date", "Customer Name", "Customer Phone",
                    "Customer GSTIN", "Type", "Place of Supply", "Taxable Value",
                    "CGST", "SGST", "IGST", "Cess", "Total Tax",
                    "Invoice Total", "Payment Mode", "Status"
            });

            int slNo = 1;
            for (SalesRegisterModels.SalesRegisterRow row : report.rows) {
                writer.writeNext(new String[]{
                        String.valueOf(slNo++),
                        row.invoiceId,
                        row.formattedDate,
                        row.customerName,
                        row.customerPhone != null ? row.customerPhone : "",
                        row.customerGstin != null ? row.customerGstin : "",
                        row.isB2B ? "B2B" : "B2C",
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
                    report.summary.grossSales.toPlainString(),
                    "",
                    ""
            });
        }

        return outputFile;
    }
}
