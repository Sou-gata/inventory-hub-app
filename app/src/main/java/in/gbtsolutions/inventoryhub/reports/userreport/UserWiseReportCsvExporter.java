package in.gbtsolutions.inventoryhub.reports.userreport;

import android.content.Context;
import android.os.Environment;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserWiseReportCsvExporter {

    public static File exportToCsv(Context context, UserWiseReportModels.ReportData report) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "User_Wise_Report_" + timestamp + ".csv";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        if (!dir.exists()) dir.mkdirs();

        File outputFile = new File(dir, fileName);

        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            // Header Info
            writer.writeNext(new String[]{"USER WISE REPORT (SALES, PURCHASES & GST SUMMARY)", report.companyName});
            writer.writeNext(new String[]{"GSTIN", report.companyGst != null ? report.companyGst : ""});
            writer.writeNext(new String[]{"Period", (report.startDate != null ? report.startDate : "") + " to " + (report.endDate != null ? report.endDate : "")});
            writer.writeNext(new String[]{"User Filter", report.filterUserName != null ? report.filterUserName : "All Users"});
            writer.writeNext(new String[]{""});

            // Executive Summary Block
            writer.writeNext(new String[]{"--- EXECUTIVE SUMMARY ---"});
            writer.writeNext(new String[]{"Metric", "Amount / Count"});
            writer.writeNext(new String[]{"Gross Sales Turnover", "₹ " + report.summary.totalGrossSales.toPlainString()});
            writer.writeNext(new String[]{"Sales Invoices Count", String.valueOf(report.summary.totalSalesCount)});
            writer.writeNext(new String[]{"Sales Output GST", "₹ " + report.summary.totalSalesGst.toPlainString()});
            writer.writeNext(new String[]{"Gross Purchases Value", "₹ " + report.summary.totalGrossPurchases.toPlainString()});
            writer.writeNext(new String[]{"Purchase Orders Count", String.valueOf(report.summary.totalPurchaseCount)});
            writer.writeNext(new String[]{"Purchase Input GST", "₹ " + report.summary.totalPurchaseGst.toPlainString()});
            writer.writeNext(new String[]{"Combined Total GST Handled", "₹ " + report.summary.totalCombinedGst.toPlainString()});
            writer.writeNext(new String[]{"Net GST Liability (Output - Input)", "₹ " + report.summary.netGstLiability.toPlainString()});
            writer.writeNext(new String[]{"Net Turnover (Sales - Purchases)", "₹ " + report.summary.netTurnover.toPlainString()});
            writer.writeNext(new String[]{"Total Transactions", String.valueOf(report.summary.totalTransactions)});
            writer.writeNext(new String[]{"Active Users with Transactions", String.valueOf(report.summary.activeUsersCount)});
            writer.writeNext(new String[]{"Total Users Evaluated", String.valueOf(report.summary.totalUsersCount)});
            writer.writeNext(new String[]{""});

            // Table Data
            writer.writeNext(new String[]{"--- USER-WISE PERFORMANCE & TAX BREAKDOWN ---"});
            writer.writeNext(new String[]{
                    "#", "User ID", "User Name", "Username", "Role",
                    "Sales Count", "Sales Taxable (₹)", "Sales CGST (₹)", "Sales SGST (₹)", "Sales IGST (₹)", "Sales GST (₹)", "Gross Sales (₹)",
                    "Purchase Count", "Purchase Taxable (₹)", "Purchase CGST (₹)", "Purchase SGST (₹)", "Purchase IGST (₹)", "Purchase GST (₹)", "Gross Purchases (₹)",
                    "Total GST (₹)", "Net Turnover (₹)"
            });

            int slNo = 1;
            for (UserWiseReportModels.UserWiseReportRow row : report.rows) {
                writer.writeNext(new String[]{
                        String.valueOf(slNo++),
                        String.valueOf(row.userId),
                        row.userName,
                        row.username != null ? row.username : "",
                        row.role,
                        String.valueOf(row.salesCount),
                        row.salesTaxable.toPlainString(),
                        row.salesCgst.toPlainString(),
                        row.salesSgst.toPlainString(),
                        row.salesIgst.toPlainString(),
                        row.salesTotalGst.toPlainString(),
                        row.salesGrossTotal.toPlainString(),
                        String.valueOf(row.purchaseCount),
                        row.purchaseTaxable.toPlainString(),
                        row.purchaseCgst.toPlainString(),
                        row.purchaseSgst.toPlainString(),
                        row.purchaseIgst.toPlainString(),
                        row.purchaseTotalGst.toPlainString(),
                        row.purchaseGrossTotal.toPlainString(),
                        row.totalCombinedGst.toPlainString(),
                        row.netTurnover.toPlainString()
                });
            }

            // Total Row
            writer.writeNext(new String[]{
                    "TOTAL",
                    "",
                    "",
                    "",
                    "",
                    String.valueOf(report.summary.totalSalesCount),
                    report.summary.totalSalesTaxable.toPlainString(),
                    report.summary.totalSalesCgst.toPlainString(),
                    report.summary.totalSalesSgst.toPlainString(),
                    report.summary.totalSalesIgst.toPlainString(),
                    report.summary.totalSalesGst.toPlainString(),
                    report.summary.totalGrossSales.toPlainString(),
                    String.valueOf(report.summary.totalPurchaseCount),
                    report.summary.totalPurchaseTaxable.toPlainString(),
                    report.summary.totalPurchaseCgst.toPlainString(),
                    report.summary.totalPurchaseSgst.toPlainString(),
                    report.summary.totalPurchaseIgst.toPlainString(),
                    report.summary.totalPurchaseGst.toPlainString(),
                    report.summary.totalGrossPurchases.toPlainString(),
                    report.summary.totalCombinedGst.toPlainString(),
                    report.summary.netTurnover.toPlainString()
            });
        }

        return outputFile;
    }
}
