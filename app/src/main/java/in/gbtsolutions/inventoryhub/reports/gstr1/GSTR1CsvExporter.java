package in.gbtsolutions.inventoryhub.reports.gstr1;

import android.content.Context;
import android.os.Environment;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GSTR1CsvExporter {

    public static File exportToCsv(Context context, GSTR1Models.ReportData report) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "GSTR1_Summary_" + timestamp + ".csv";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        if (!dir.exists()) dir.mkdirs();

        File outputFile = new File(dir, fileName);

        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            // Header Info
            writer.writeNext(new String[]{"GSTR-1 SUMMARY REPORT", report.companyName});
            writer.writeNext(new String[]{"GSTIN", report.companyGst});
            writer.writeNext(new String[]{"Period", (report.startDate != null ? report.startDate : "") + " to " + (report.endDate != null ? report.endDate : "")});
            writer.writeNext(new String[]{""});

            // Executive KPI Summary
            writer.writeNext(new String[]{"--- EXECUTIVE SUMMARY ---"});
            writer.writeNext(new String[]{"Metric", "Value"});
            writer.writeNext(new String[]{"Gross Invoice Value", "₹ " + report.kpi.totalSales.toPlainString()});
            writer.writeNext(new String[]{"Taxable Value", "₹ " + report.kpi.taxableValue.toPlainString()});
            writer.writeNext(new String[]{"CGST", "₹ " + report.kpi.cgst.toPlainString()});
            writer.writeNext(new String[]{"SGST", "₹ " + report.kpi.sgst.toPlainString()});
            writer.writeNext(new String[]{"IGST", "₹ " + report.kpi.igst.toPlainString()});
            writer.writeNext(new String[]{"Cess", "₹ " + report.kpi.cess.toPlainString()});
            writer.writeNext(new String[]{"Total GST", "₹ " + report.kpi.totalGst.toPlainString()});
            writer.writeNext(new String[]{"Invoices Count", String.valueOf(report.kpi.totalInvoices)});
            writer.writeNext(new String[]{""});

            // Table 1: B2B Supplies
            writer.writeNext(new String[]{"--- 1. B2B SUPPLIES ---"});
            writer.writeNext(new String[]{"Customer GSTIN", "Customer Name", "Place of Supply", "Rate (%)", "Invoices", "Taxable Value", "CGST", "SGST", "IGST", "CESS", "Total Tax", "Invoice Value"});
            for (GSTR1Models.B2BRow row : report.b2bList) {
                writer.writeNext(new String[]{
                        row.customerGstin,
                        row.customerName,
                        row.placeOfSupply,
                        String.valueOf(row.taxRate),
                        String.valueOf(row.invoiceCount),
                        row.taxableValue.toPlainString(),
                        row.cgst.toPlainString(),
                        row.sgst.toPlainString(),
                        row.igst.toPlainString(),
                        row.cess.toPlainString(),
                        row.totalTax.toPlainString(),
                        row.invoiceValue.toPlainString()
                });
            }
            writer.writeNext(new String[]{""});

            // Table 2: B2C Supplies (Large & Others)
            writer.writeNext(new String[]{"--- 2. B2C SUPPLIES ---"});
            writer.writeNext(new String[]{"Category", "Place of Supply", "Rate (%)", "Invoices", "Taxable Value", "CGST", "SGST", "IGST", "CESS", "Total Tax", "Invoice Value"});
            for (GSTR1Models.B2CRow row : report.b2cOthersList) {
                writer.writeNext(new String[]{
                        row.supplyCategory,
                        row.placeOfSupply,
                        String.valueOf(row.taxRate),
                        String.valueOf(row.invoiceCount),
                        row.taxableValue.toPlainString(),
                        row.cgst.toPlainString(),
                        row.sgst.toPlainString(),
                        row.igst.toPlainString(),
                        row.cess.toPlainString(),
                        row.totalTax.toPlainString(),
                        row.invoiceValue.toPlainString()
                });
            }
            for (GSTR1Models.B2CRow row : report.b2cLargeList) {
                writer.writeNext(new String[]{
                        row.supplyCategory,
                        row.placeOfSupply,
                        String.valueOf(row.taxRate),
                        String.valueOf(row.invoiceCount),
                        row.taxableValue.toPlainString(),
                        row.cgst.toPlainString(),
                        row.sgst.toPlainString(),
                        row.igst.toPlainString(),
                        row.cess.toPlainString(),
                        row.totalTax.toPlainString(),
                        row.invoiceValue.toPlainString()
                });
            }
            writer.writeNext(new String[]{""});

            // Table 6: HSN / SAC Summary
            writer.writeNext(new String[]{"--- 6. HSN / SAC SUMMARY ---"});
            writer.writeNext(new String[]{"HSN/SAC", "Description", "UQC", "Quantity", "Rate (%)", "Supply Type", "Taxable Value", "CGST", "SGST", "IGST", "CESS", "Total Tax", "Total Value"});
            for (GSTR1Models.HsnRow row : report.hsnB2bList) {
                writer.writeNext(new String[]{
                        row.hsnCode, row.description, row.uqc, String.valueOf(row.quantity), String.valueOf(row.taxRate), row.supplyType,
                        row.taxableValue.toPlainString(), row.cgst.toPlainString(), row.sgst.toPlainString(), row.igst.toPlainString(),
                        row.cess.toPlainString(), row.totalTax.toPlainString(), row.totalValue.toPlainString()
                });
            }
            for (GSTR1Models.HsnRow row : report.hsnB2cList) {
                writer.writeNext(new String[]{
                        row.hsnCode, row.description, row.uqc, String.valueOf(row.quantity), String.valueOf(row.taxRate), row.supplyType,
                        row.taxableValue.toPlainString(), row.cgst.toPlainString(), row.sgst.toPlainString(), row.igst.toPlainString(),
                        row.cess.toPlainString(), row.totalTax.toPlainString(), row.totalValue.toPlainString()
                });
            }
        }

        return outputFile;
    }
}
