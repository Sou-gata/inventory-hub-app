package in.gbtsolutions.inventoryhub.reports.userreport;

import android.content.Context;
import android.os.Environment;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserWiseReportExcelExporter {

    public static File exportToExcel(Context context, UserWiseReportModels.ReportData report) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();

        // 1. Title Style
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        // 2. Header Style (Royal Blue with White Bold Text)
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // 3. Data Styles
        DataFormat format = workbook.createDataFormat();
        CellStyle moneyStyle = workbook.createCellStyle();
        moneyStyle.setDataFormat(format.getFormat("₹#,##0.00"));
        moneyStyle.setAlignment(HorizontalAlignment.RIGHT);
        moneyStyle.setBorderBottom(BorderStyle.THIN);
        moneyStyle.setBorderTop(BorderStyle.THIN);
        moneyStyle.setBorderLeft(BorderStyle.THIN);
        moneyStyle.setBorderRight(BorderStyle.THIN);

        CellStyle normalStyle = workbook.createCellStyle();
        normalStyle.setBorderBottom(BorderStyle.THIN);
        normalStyle.setBorderTop(BorderStyle.THIN);
        normalStyle.setBorderLeft(BorderStyle.THIN);
        normalStyle.setBorderRight(BorderStyle.THIN);

        CellStyle centerStyle = workbook.createCellStyle();
        centerStyle.setAlignment(HorizontalAlignment.CENTER);
        centerStyle.setBorderBottom(BorderStyle.THIN);
        centerStyle.setBorderTop(BorderStyle.THIN);
        centerStyle.setBorderLeft(BorderStyle.THIN);
        centerStyle.setBorderRight(BorderStyle.THIN);

        // 4. Total Row Style
        CellStyle totalStyle = workbook.createCellStyle();
        Font totalFont = workbook.createFont();
        totalFont.setBold(true);
        totalStyle.setFont(totalFont);
        totalStyle.setDataFormat(format.getFormat("₹#,##0.00"));
        totalStyle.setAlignment(HorizontalAlignment.RIGHT);
        totalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalStyle.setBorderTop(BorderStyle.DOUBLE);
        totalStyle.setBorderBottom(BorderStyle.DOUBLE);
        totalStyle.setBorderLeft(BorderStyle.THIN);
        totalStyle.setBorderRight(BorderStyle.THIN);

        CellStyle totalLabelStyle = workbook.createCellStyle();
        totalLabelStyle.setFont(totalFont);
        totalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);
        totalLabelStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalLabelStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalLabelStyle.setBorderTop(BorderStyle.DOUBLE);
        totalLabelStyle.setBorderBottom(BorderStyle.DOUBLE);
        totalLabelStyle.setBorderLeft(BorderStyle.THIN);
        totalLabelStyle.setBorderRight(BorderStyle.THIN);

        CellStyle totalCenterStyle = workbook.createCellStyle();
        totalCenterStyle.setFont(totalFont);
        totalCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        totalCenterStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalCenterStyle.setBorderTop(BorderStyle.DOUBLE);
        totalCenterStyle.setBorderBottom(BorderStyle.DOUBLE);
        totalCenterStyle.setBorderLeft(BorderStyle.THIN);
        totalCenterStyle.setBorderRight(BorderStyle.THIN);

        // Sheet 1: User Wise Report
        buildUserWiseSheet(workbook, report, titleStyle, headerStyle, normalStyle, centerStyle, moneyStyle, totalStyle, totalLabelStyle, totalCenterStyle);

        // Sheet 2: Executive Summary
        buildSummarySheet(workbook, report, titleStyle, headerStyle, normalStyle, centerStyle, moneyStyle);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "User_Wise_Report_" + timestamp + ".xlsx";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        if (!dir.exists()) dir.mkdirs();

        File outputFile = new File(dir, fileName);
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            workbook.write(out);
        }
        workbook.close();

        return outputFile;
    }

    private static void buildUserWiseSheet(
            XSSFWorkbook wb,
            UserWiseReportModels.ReportData report,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle normalStyle,
            CellStyle centerStyle,
            CellStyle moneyStyle,
            CellStyle totalStyle,
            CellStyle totalLabelStyle,
            CellStyle totalCenterStyle) {

        Sheet sheet = wb.createSheet("User Wise Report");
        int r = 0;

        // Title Block
        Row r0 = sheet.createRow(r++);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("USER WISE REPORT — " + report.companyName);
        c0.setCellStyle(titleStyle);

        Row r1 = sheet.createRow(r++);
        r1.createCell(0).setCellValue("GSTIN: " + (report.companyGst != null && !report.companyGst.isEmpty() ? report.companyGst : "N/A"));

        Row r2 = sheet.createRow(r++);
        String period = "Period: " + (report.startDate != null ? report.startDate : "Start") + " to " + (report.endDate != null ? report.endDate : "End")
                + "  |  User Filter: " + (report.filterUserName != null ? report.filterUserName : "All Users");
        r2.createCell(0).setCellValue(period);
        r++; // Blank row

        // Headers
        String[] headers = {
                "#", "User ID", "User Name", "Username", "Role",
                "Sales Inv", "Sales Taxable (₹)", "Sales GST (₹)", "Gross Sales (₹)",
                "Purchase Orders", "Purchase Taxable (₹)", "Purchase GST (₹)", "Gross Purchases (₹)",
                "Total GST (₹)", "Net Turnover (₹)"
        };

        Row headerRow = sheet.createRow(r++);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        int slNo = 1;
        for (UserWiseReportModels.UserWiseReportRow rowData : report.rows) {
            Row row = sheet.createRow(r++);

            Cell cSl = row.createCell(0);
            cSl.setCellValue(slNo++);
            cSl.setCellStyle(centerStyle);

            Cell cUid = row.createCell(1);
            cUid.setCellValue(rowData.userId);
            cUid.setCellStyle(centerStyle);

            Cell cName = row.createCell(2);
            cName.setCellValue(rowData.userName);
            cName.setCellStyle(normalStyle);

            Cell cUname = row.createCell(3);
            cUname.setCellValue(rowData.username != null ? rowData.username : "");
            cUname.setCellStyle(normalStyle);

            Cell cRole = row.createCell(4);
            cRole.setCellValue(rowData.role);
            cRole.setCellStyle(centerStyle);

            Cell cSalesCount = row.createCell(5);
            cSalesCount.setCellValue(rowData.salesCount);
            cSalesCount.setCellStyle(centerStyle);

            Cell cSalesTaxable = row.createCell(6);
            cSalesTaxable.setCellValue(rowData.salesTaxable.doubleValue());
            cSalesTaxable.setCellStyle(moneyStyle);

            Cell cSalesGst = row.createCell(7);
            cSalesGst.setCellValue(rowData.salesTotalGst.doubleValue());
            cSalesGst.setCellStyle(moneyStyle);

            Cell cSalesGross = row.createCell(8);
            cSalesGross.setCellValue(rowData.salesGrossTotal.doubleValue());
            cSalesGross.setCellStyle(moneyStyle);

            Cell cPurCount = row.createCell(9);
            cPurCount.setCellValue(rowData.purchaseCount);
            cPurCount.setCellStyle(centerStyle);

            Cell cPurTaxable = row.createCell(10);
            cPurTaxable.setCellValue(rowData.purchaseTaxable.doubleValue());
            cPurTaxable.setCellStyle(moneyStyle);

            Cell cPurGst = row.createCell(11);
            cPurGst.setCellValue(rowData.purchaseTotalGst.doubleValue());
            cPurGst.setCellStyle(moneyStyle);

            Cell cPurGross = row.createCell(12);
            cPurGross.setCellValue(rowData.purchaseGrossTotal.doubleValue());
            cPurGross.setCellStyle(moneyStyle);

            Cell cTotalGst = row.createCell(13);
            cTotalGst.setCellValue(rowData.totalCombinedGst.doubleValue());
            cTotalGst.setCellStyle(moneyStyle);

            Cell cNetTurnover = row.createCell(14);
            cNetTurnover.setCellValue(rowData.netTurnover.doubleValue());
            cNetTurnover.setCellStyle(moneyStyle);
        }

        // Total Row
        Row totalRow = sheet.createRow(r);
        Cell tLabel = totalRow.createCell(0);
        tLabel.setCellValue("TOTAL");
        tLabel.setCellStyle(totalCenterStyle);

        for (int i = 1; i < 5; i++) {
            Cell cEmpty = totalRow.createCell(i);
            cEmpty.setCellValue("");
            cEmpty.setCellStyle(totalLabelStyle);
        }

        Cell tSalesCount = totalRow.createCell(5);
        tSalesCount.setCellValue(report.summary.totalSalesCount);
        tSalesCount.setCellStyle(totalCenterStyle);

        Cell tSalesTaxable = totalRow.createCell(6);
        tSalesTaxable.setCellValue(report.summary.totalSalesTaxable.doubleValue());
        tSalesTaxable.setCellStyle(totalStyle);

        Cell tSalesGst = totalRow.createCell(7);
        tSalesGst.setCellValue(report.summary.totalSalesGst.doubleValue());
        tSalesGst.setCellStyle(totalStyle);

        Cell tSalesGross = totalRow.createCell(8);
        tSalesGross.setCellValue(report.summary.totalGrossSales.doubleValue());
        tSalesGross.setCellStyle(totalStyle);

        Cell tPurCount = totalRow.createCell(9);
        tPurCount.setCellValue(report.summary.totalPurchaseCount);
        tPurCount.setCellStyle(totalCenterStyle);

        Cell tPurTaxable = totalRow.createCell(10);
        tPurTaxable.setCellValue(report.summary.totalPurchaseTaxable.doubleValue());
        tPurTaxable.setCellStyle(totalStyle);

        Cell tPurGst = totalRow.createCell(11);
        tPurGst.setCellValue(report.summary.totalPurchaseGst.doubleValue());
        tPurGst.setCellStyle(totalStyle);

        Cell tPurGross = totalRow.createCell(12);
        tPurGross.setCellValue(report.summary.totalGrossPurchases.doubleValue());
        tPurGross.setCellStyle(totalStyle);

        Cell tTotalGst = totalRow.createCell(13);
        tTotalGst.setCellValue(report.summary.totalCombinedGst.doubleValue());
        tTotalGst.setCellStyle(totalStyle);

        Cell tNetTurnover = totalRow.createCell(14);
        tNetTurnover.setCellValue(report.summary.netTurnover.doubleValue());
        tNetTurnover.setCellStyle(totalStyle);

        // Adjust column widths
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.max(currentWidth + 1000, 3000));
        }
    }

    private static void buildSummarySheet(
            XSSFWorkbook wb,
            UserWiseReportModels.ReportData report,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle normalStyle,
            CellStyle centerStyle,
            CellStyle moneyStyle) {

        Sheet sheet = wb.createSheet("Executive Summary");
        int r = 0;

        Row r0 = sheet.createRow(r++);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("USER REPORT EXECUTIVE SUMMARY");
        c0.setCellStyle(titleStyle);

        Row r1 = sheet.createRow(r++);
        r1.createCell(0).setCellValue("Generated for: " + report.companyName);

        Row r2 = sheet.createRow(r++);
        r2.createCell(0).setCellValue("Period: " + report.startDate + " to " + report.endDate);
        r++; // Blank

        Row hRow = sheet.createRow(r++);
        Cell h0 = hRow.createCell(0);
        h0.setCellValue("Metric Description");
        h0.setCellStyle(headerStyle);

        Cell h1 = hRow.createCell(1);
        h1.setCellValue("Value / Count");
        h1.setCellStyle(headerStyle);

        Object[][] metrics = {
                {"Gross Sales Turnover (₹)", report.summary.totalGrossSales.doubleValue(), true},
                {"Total Sales Invoices Count", (double) report.summary.totalSalesCount, false},
                {"Total Output Sales GST (₹)", report.summary.totalSalesGst.doubleValue(), true},
                {"Gross Purchases Value (₹)", report.summary.totalGrossPurchases.doubleValue(), true},
                {"Total Purchase Orders Count", (double) report.summary.totalPurchaseCount, false},
                {"Total Input Purchase GST (₹)", report.summary.totalPurchaseGst.doubleValue(), true},
                {"Combined Total GST Handled (₹)", report.summary.totalCombinedGst.doubleValue(), true},
                {"Net GST Liability (Output - Input) (₹)", report.summary.netGstLiability.doubleValue(), true},
                {"Net Turnover (Sales - Purchases) (₹)", report.summary.netTurnover.doubleValue(), true},
                {"Total Transactions Count", (double) report.summary.totalTransactions, false},
                {"Active Users with Transactions", (double) report.summary.activeUsersCount, false},
                {"Total Registered Users Evaluated", (double) report.summary.totalUsersCount, false}
        };

        for (Object[] m : metrics) {
            Row row = sheet.createRow(r++);
            Cell cLabel = row.createCell(0);
            cLabel.setCellValue((String) m[0]);
            cLabel.setCellStyle(normalStyle);

            Cell cVal = row.createCell(1);
            boolean isMoney = (Boolean) m[2];
            double val = (Double) m[1];
            cVal.setCellValue(val);
            if (isMoney) {
                cVal.setCellStyle(moneyStyle);
            } else {
                cVal.setCellStyle(centerStyle);
            }
        }

        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        sheet.setColumnWidth(0, sheet.getColumnWidth(0) + 1500);
        sheet.setColumnWidth(1, sheet.getColumnWidth(1) + 1500);
    }
}
