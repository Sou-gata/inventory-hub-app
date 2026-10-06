package in.gbtsolutions.inventoryhub.reports.purchaseregister;

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

public class PurchaseRegisterExcelExporter {

    public static File exportToExcel(Context context, PurchaseRegisterModels.ReportData report) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();

        // Title Style
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        // Header Style (Royal Blue with White Bold Text)
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

        // Data Styles
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

        // Total Row Style
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

        // 1. Sheet: Purchase Register
        buildPurchaseRegisterSheet(workbook, report, titleStyle, headerStyle, normalStyle, centerStyle, moneyStyle, totalStyle, totalLabelStyle);

        // 2. Sheet: Executive Summary
        buildSummarySheet(workbook, report, titleStyle, headerStyle, normalStyle, moneyStyle);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "Purchase_Register_" + timestamp + ".xlsx";

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

    private static void buildPurchaseRegisterSheet(
            XSSFWorkbook wb,
            PurchaseRegisterModels.ReportData report,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle normalStyle,
            CellStyle centerStyle,
            CellStyle moneyStyle,
            CellStyle totalStyle,
            CellStyle totalLabelStyle) {

        Sheet sheet = wb.createSheet("Purchase Register");
        int r = 0;

        // Title Block
        Row r0 = sheet.createRow(r++);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("PURCHASE REGISTER — " + report.companyName);
        c0.setCellStyle(titleStyle);

        Row r1 = sheet.createRow(r++);
        r1.createCell(0).setCellValue("GSTIN: " + (report.companyGst != null && !report.companyGst.isEmpty() ? report.companyGst : "N/A"));

        Row r2 = sheet.createRow(r++);
        String period = "Period: " + (report.startDate != null ? report.startDate : "Start") + " to " + (report.endDate != null ? report.endDate : "End");
        r2.createCell(0).setCellValue(period);
        r++; // Blank row

        // Headers
        String[] headers = {
                "#", "Invoice / PO #", "Date", "Supplier Name", "Supplier Phone",
                "Supplier GSTIN", "Type", "Place of Supply", "Taxable Value (₹)",
                "CGST (₹)", "SGST (₹)", "IGST (₹)", "Cess (₹)", "Total Tax (₹)",
                "Bill Total (₹)", "Payment", "Status"
        };

        Row headerRow = sheet.createRow(r++);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        int slNo = 1;
        for (PurchaseRegisterModels.PurchaseRegisterRow rowData : report.rows) {
            Row row = sheet.createRow(r++);

            Cell cSl = row.createCell(0);
            cSl.setCellValue(slNo++);
            cSl.setCellStyle(centerStyle);

            Cell cInv = row.createCell(1);
            cInv.setCellValue(rowData.invoiceId);
            cInv.setCellStyle(centerStyle);

            Cell cDate = row.createCell(2);
            cDate.setCellValue(rowData.formattedDate);
            cDate.setCellStyle(centerStyle);

            Cell cSupp = row.createCell(3);
            cSupp.setCellValue(rowData.supplierName);
            cSupp.setCellStyle(normalStyle);

            Cell cPhone = row.createCell(4);
            cPhone.setCellValue(rowData.supplierPhone != null ? rowData.supplierPhone : "");
            cPhone.setCellStyle(normalStyle);

            Cell cGstin = row.createCell(5);
            cGstin.setCellValue(rowData.supplierGstin != null ? rowData.supplierGstin : "");
            cGstin.setCellStyle(centerStyle);

            Cell cType = row.createCell(6);
            cType.setCellValue(rowData.isB2B ? "Registered" : "Unregistered");
            cType.setCellStyle(centerStyle);

            Cell cPos = row.createCell(7);
            cPos.setCellValue(rowData.placeOfSupply);
            cPos.setCellStyle(normalStyle);

            Cell cTaxable = row.createCell(8);
            cTaxable.setCellValue(rowData.taxableValue.doubleValue());
            cTaxable.setCellStyle(moneyStyle);

            Cell cCgst = row.createCell(9);
            cCgst.setCellValue(rowData.cgstAmount.doubleValue());
            cCgst.setCellStyle(moneyStyle);

            Cell cSgst = row.createCell(10);
            cSgst.setCellValue(rowData.sgstAmount.doubleValue());
            cSgst.setCellStyle(moneyStyle);

            Cell cIgst = row.createCell(11);
            cIgst.setCellValue(rowData.igstAmount.doubleValue());
            cIgst.setCellStyle(moneyStyle);

            Cell cCess = row.createCell(12);
            cCess.setCellValue(rowData.cessAmount.doubleValue());
            cCess.setCellStyle(moneyStyle);

            Cell cTotalTax = row.createCell(13);
            cTotalTax.setCellValue(rowData.totalTax.doubleValue());
            cTotalTax.setCellStyle(moneyStyle);

            Cell cTotalAmt = row.createCell(14);
            cTotalAmt.setCellValue(rowData.totalAmount.doubleValue());
            cTotalAmt.setCellStyle(moneyStyle);

            Cell cPay = row.createCell(15);
            cPay.setCellValue(rowData.paymentMethod);
            cPay.setCellStyle(centerStyle);

            Cell cStatus = row.createCell(16);
            cStatus.setCellValue(rowData.status);
            cStatus.setCellStyle(centerStyle);
        }

        // Total Row
        Row totalRow = sheet.createRow(r++);
        for (int i = 0; i < headers.length; i++) {
            Cell c = totalRow.createCell(i);
            if (i < 8) {
                c.setCellStyle(totalLabelStyle);
                if (i == 7) c.setCellValue("TOTAL:");
            } else if (i <= 14) {
                c.setCellStyle(totalStyle);
                switch (i) {
                    case 8: c.setCellValue(report.summary.taxableValue.doubleValue()); break;
                    case 9: c.setCellValue(report.summary.cgst.doubleValue()); break;
                    case 10: c.setCellValue(report.summary.sgst.doubleValue()); break;
                    case 11: c.setCellValue(report.summary.igst.doubleValue()); break;
                    case 12: c.setCellValue(report.summary.cess.doubleValue()); break;
                    case 13: c.setCellValue(report.summary.totalTax.doubleValue()); break;
                    case 14: c.setCellValue(report.summary.grossPurchases.doubleValue()); break;
                }
            } else {
                c.setCellStyle(totalLabelStyle);
            }
        }

        // Set column widths
        sheet.setColumnWidth(0, 6 * 256);
        sheet.setColumnWidth(1, 16 * 256);
        sheet.setColumnWidth(2, 14 * 256);
        sheet.setColumnWidth(3, 24 * 256);
        sheet.setColumnWidth(4, 15 * 256);
        sheet.setColumnWidth(5, 18 * 256);
        sheet.setColumnWidth(6, 12 * 256);
        sheet.setColumnWidth(7, 18 * 256);
        sheet.setColumnWidth(8, 16 * 256);
        sheet.setColumnWidth(9, 14 * 256);
        sheet.setColumnWidth(10, 14 * 256);
        sheet.setColumnWidth(11, 14 * 256);
        sheet.setColumnWidth(12, 12 * 256);
        sheet.setColumnWidth(13, 16 * 256);
        sheet.setColumnWidth(14, 18 * 256);
        sheet.setColumnWidth(15, 14 * 256);
        sheet.setColumnWidth(16, 14 * 256);
    }

    private static void buildSummarySheet(
            XSSFWorkbook wb,
            PurchaseRegisterModels.ReportData report,
            CellStyle titleStyle,
            CellStyle headerStyle,
            CellStyle normalStyle,
            CellStyle moneyStyle) {

        Sheet sheet = wb.createSheet("Executive Summary");
        int r = 0;

        Row r0 = sheet.createRow(r++);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("PURCHASE REGISTER EXECUTIVE SUMMARY — " + report.companyName);
        c0.setCellStyle(titleStyle);

        Row r1 = sheet.createRow(r++);
        r1.createCell(0).setCellValue("GSTIN: " + (report.companyGst != null && !report.companyGst.isEmpty() ? report.companyGst : "N/A"));

        Row r2 = sheet.createRow(r++);
        r2.createCell(0).setCellValue("Reporting Period: " + (report.startDate != null ? report.startDate : "Start") + " to " + (report.endDate != null ? report.endDate : "End"));
        r++;

        String[] sumHeaders = {"Metric Description", "Value / Amount"};
        Row hRow = sheet.createRow(r++);
        for (int i = 0; i < sumHeaders.length; i++) {
            Cell c = hRow.createCell(i);
            c.setCellValue(sumHeaders[i]);
            c.setCellStyle(headerStyle);
        }

        Object[][] summaryData = {
                {"Gross Purchases (Total Inward Amount)", report.summary.grossPurchases.doubleValue(), true},
                {"Total Taxable Purchases Turnover", report.summary.taxableValue.doubleValue(), true},
                {"Central Tax (CGST ITC)", report.summary.cgst.doubleValue(), true},
                {"State Tax (SGST ITC)", report.summary.sgst.doubleValue(), true},
                {"Integrated Tax (IGST ITC)", report.summary.igst.doubleValue(), true},
                {"Compensation Cess", report.summary.cess.doubleValue(), true},
                {"Total Input Tax Credit (ITC)", report.summary.totalTax.doubleValue(), true},
                {"Total Purchase Bills", report.summary.totalInvoices, false},
                {"Registered Supplier (B2B) Bills", report.summary.b2bInvoices, false},
                {"Unregistered Supplier Bills", report.summary.b2cInvoices, false},
                {"Cancelled Bills", report.summary.cancelledInvoices, false}
        };

        for (Object[] rowData : summaryData) {
            Row row = sheet.createRow(r++);
            Cell cDesc = row.createCell(0);
            cDesc.setCellValue((String) rowData[0]);
            cDesc.setCellStyle(normalStyle);

            Cell cVal = row.createCell(1);
            boolean isMoney = (Boolean) rowData[2];
            if (isMoney) {
                cVal.setCellValue((Double) rowData[1]);
                cVal.setCellStyle(moneyStyle);
            } else {
                cVal.setCellValue((Integer) rowData[1]);
                cVal.setCellStyle(normalStyle);
            }
        }

        sheet.setColumnWidth(0, 42 * 256);
        sheet.setColumnWidth(1, 20 * 256);
    }
}
