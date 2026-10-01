package in.gbtsolutions.inventoryhub.reports.gstr1;

import android.content.Context;
import android.os.Environment;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
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

public class GSTR1ExcelExporter {

    public static File exportToExcel(Context context, GSTR1Models.ReportData report) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();

        // Styles
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        // 1. Sheet: Summary & KPIs
        buildSummarySheet(workbook, report, titleStyle, headerStyle);

        // 2. Sheet: B2B Supplies
        buildB2BSheet(workbook, report, headerStyle);

        // 3. Sheet: B2C Large
        buildB2CSheet(workbook, "B2C Large", report.b2cLargeList, headerStyle);

        // 4. Sheet: B2C Others
        buildB2CSheet(workbook, "B2C Others", report.b2cOthersList, headerStyle);

        // 5. Sheet: Exports & SEZ
        buildExportSheet(workbook, report, headerStyle);

        // 6. Sheet: Credit & Debit Notes
        buildNotesSheet(workbook, report, headerStyle);

        // 7. Sheet: Nil & Exempt
        buildNilSheet(workbook, report, headerStyle);

        // 8. Sheet: HSN Summary
        buildHsnSheet(workbook, report, headerStyle);

        // Write to file in app-accessible Download directory
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "GSTR1_Summary_" + timestamp + ".xlsx";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        if (!dir.exists()) dir.mkdirs();

        File outputFile = new File(dir, fileName);
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            workbook.write(out);
        } finally {
            workbook.close();
        }

        return outputFile;
    }

    private static void buildSummarySheet(XSSFWorkbook wb, GSTR1Models.ReportData report, CellStyle titleStyle, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet("Executive Summary");
        int r = 0;

        Row r0 = sheet.createRow(r++);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("GSTR-1 Summary Report — " + report.companyName);
        c0.setCellStyle(titleStyle);

        Row r1 = sheet.createRow(r++);
        r1.createCell(0).setCellValue("GSTIN: " + report.companyGst);

        Row r2 = sheet.createRow(r++);
        r2.createCell(0).setCellValue("Reporting Period: " + (report.startDate != null ? report.startDate : "Start") + " to " + (report.endDate != null ? report.endDate : "End"));
        r++;

        // KPI Table
        Row rHeader = sheet.createRow(r++);
        String[] kpiHeaders = {"Metric", "Amount / Count"};
        for (int i = 0; i < kpiHeaders.length; i++) {
            Cell c = rHeader.createCell(i);
            c.setCellValue(kpiHeaders[i]);
            c.setCellStyle(headerStyle);
        }

        addSummaryRow(sheet, r++, "Gross Invoice Value", "₹ " + report.kpi.totalSales.toPlainString());
        addSummaryRow(sheet, r++, "Total Taxable Value", "₹ " + report.kpi.taxableValue.toPlainString());
        addSummaryRow(sheet, r++, "Central Tax (CGST)", "₹ " + report.kpi.cgst.toPlainString());
        addSummaryRow(sheet, r++, "State Tax (SGST)", "₹ " + report.kpi.sgst.toPlainString());
        addSummaryRow(sheet, r++, "Integrated Tax (IGST)", "₹ " + report.kpi.igst.toPlainString());
        addSummaryRow(sheet, r++, "Cess", "₹ " + report.kpi.cess.toPlainString());
        addSummaryRow(sheet, r++, "Total GST Liability", "₹ " + report.kpi.totalGst.toPlainString());
        addSummaryRow(sheet, r++, "Active Invoices Count", String.valueOf(report.kpi.totalInvoices));
        addSummaryRow(sheet, r++, "Cancelled Invoices", String.valueOf(report.taxDocSummary.cancelledInvoicesCount));
        addSummaryRow(sheet, r++, "Credit Notes Count", String.valueOf(report.taxDocSummary.creditNotesCount));
        addSummaryRow(sheet, r++, "Debit Notes Count", String.valueOf(report.taxDocSummary.debitNotesCount));
        addSummaryRow(sheet, r++, "Net Documents Issued", String.valueOf(report.taxDocSummary.netDocumentsCount));

        sheet.setColumnWidth(0, 30 * 256);
        sheet.setColumnWidth(1, 25 * 256);
    }

    private static void addSummaryRow(Sheet sheet, int rowIdx, String title, String val) {
        Row row = sheet.createRow(rowIdx);
        row.createCell(0).setCellValue(title);
        row.createCell(1).setCellValue(val);
    }

    private static void buildB2BSheet(XSSFWorkbook wb, GSTR1Models.ReportData report, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet("B2B Supplies");
        String[] headers = {"Customer GSTIN", "Customer Name", "Place of Supply", "Rate (%)", "Invoices", "Taxable Value", "CGST", "SGST", "IGST", "CESS", "Total Tax", "Invoice Value"};
        createHeaderRow(sheet, headers, headerStyle);

        int r = 1;
        for (GSTR1Models.B2BRow row : report.b2bList) {
            Row rowView = sheet.createRow(r++);
            rowView.createCell(0).setCellValue(row.customerGstin);
            rowView.createCell(1).setCellValue(row.customerName);
            rowView.createCell(2).setCellValue(row.placeOfSupply);
            rowView.createCell(3).setCellValue(row.taxRate);
            rowView.createCell(4).setCellValue(row.invoiceCount);
            rowView.createCell(5).setCellValue(row.taxableValue.doubleValue());
            rowView.createCell(6).setCellValue(row.cgst.doubleValue());
            rowView.createCell(7).setCellValue(row.sgst.doubleValue());
            rowView.createCell(8).setCellValue(row.igst.doubleValue());
            rowView.createCell(9).setCellValue(row.cess.doubleValue());
            rowView.createCell(10).setCellValue(row.totalTax.doubleValue());
            rowView.createCell(11).setCellValue(row.invoiceValue.doubleValue());
        }
        autoSizeCols(sheet, headers.length);
    }

    private static void buildB2CSheet(XSSFWorkbook wb, String sheetName, java.util.List<GSTR1Models.B2CRow> list, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet(sheetName);
        String[] headers = {"Supply Category", "Place of Supply", "Rate (%)", "Invoices", "Taxable Value", "CGST", "SGST", "IGST", "CESS", "Total Tax", "Invoice Value"};
        createHeaderRow(sheet, headers, headerStyle);

        int r = 1;
        for (GSTR1Models.B2CRow row : list) {
            Row rowView = sheet.createRow(r++);
            rowView.createCell(0).setCellValue(row.supplyCategory);
            rowView.createCell(1).setCellValue(row.placeOfSupply);
            rowView.createCell(2).setCellValue(row.taxRate);
            rowView.createCell(3).setCellValue(row.invoiceCount);
            rowView.createCell(4).setCellValue(row.taxableValue.doubleValue());
            rowView.createCell(5).setCellValue(row.cgst.doubleValue());
            rowView.createCell(6).setCellValue(row.sgst.doubleValue());
            rowView.createCell(7).setCellValue(row.igst.doubleValue());
            rowView.createCell(8).setCellValue(row.cess.doubleValue());
            rowView.createCell(9).setCellValue(row.totalTax.doubleValue());
            rowView.createCell(10).setCellValue(row.invoiceValue.doubleValue());
        }
        autoSizeCols(sheet, headers.length);
    }

    private static void buildExportSheet(XSSFWorkbook wb, GSTR1Models.ReportData report, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet("Exports & SEZ");
        String[] headers = {"Category", "Invoices", "Taxable Value", "Invoice Value", "IGST", "CGST", "SGST", "CESS", "Shipping Bill No", "Shipping Bill Date", "Port Code"};
        createHeaderRow(sheet, headers, headerStyle);

        int r = 1;
        for (GSTR1Models.ExportRow row : report.exportList) {
            Row rowView = sheet.createRow(r++);
            rowView.createCell(0).setCellValue(row.category);
            rowView.createCell(1).setCellValue(row.invoiceCount);
            rowView.createCell(2).setCellValue(row.taxableValue.doubleValue());
            rowView.createCell(3).setCellValue(row.invoiceValue.doubleValue());
            rowView.createCell(4).setCellValue(row.igst.doubleValue());
            rowView.createCell(5).setCellValue(row.cgst.doubleValue());
            rowView.createCell(6).setCellValue(row.sgst.doubleValue());
            rowView.createCell(7).setCellValue(row.cess.doubleValue());
            rowView.createCell(8).setCellValue(row.shippingBillNo != null ? row.shippingBillNo : "");
            rowView.createCell(9).setCellValue(row.shippingBillDate != null ? row.shippingBillDate : "");
            rowView.createCell(10).setCellValue(row.portCode != null ? row.portCode : "");
        }
        autoSizeCols(sheet, headers.length);
    }

    private static void buildNotesSheet(XSSFWorkbook wb, GSTR1Models.ReportData report, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet("Credit & Debit Notes");
        String[] headers = {"Note Type", "Recipient Type", "Rate (%)", "Count", "Taxable Value", "CGST", "SGST", "IGST", "CESS", "Total Tax", "Total Value"};
        createHeaderRow(sheet, headers, headerStyle);

        int r = 1;
        java.util.List<GSTR1Models.CreditDebitNoteRow> allNotes = new java.util.ArrayList<>();
        allNotes.addAll(report.creditNotesList);
        allNotes.addAll(report.debitNotesList);

        for (GSTR1Models.CreditDebitNoteRow row : allNotes) {
            Row rowView = sheet.createRow(r++);
            rowView.createCell(0).setCellValue(row.noteType);
            rowView.createCell(1).setCellValue(row.recipientType);
            rowView.createCell(2).setCellValue(row.taxRate);
            rowView.createCell(3).setCellValue(row.noteCount);
            rowView.createCell(4).setCellValue(row.taxableValue.doubleValue());
            rowView.createCell(5).setCellValue(row.cgst.doubleValue());
            rowView.createCell(6).setCellValue(row.sgst.doubleValue());
            rowView.createCell(7).setCellValue(row.igst.doubleValue());
            rowView.createCell(8).setCellValue(row.cess.doubleValue());
            rowView.createCell(9).setCellValue(row.totalTax.doubleValue());
            rowView.createCell(10).setCellValue(row.totalValue.doubleValue());
        }
        autoSizeCols(sheet, headers.length);
    }

    private static void buildNilSheet(XSSFWorkbook wb, GSTR1Models.ReportData report, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet("Nil & Exempt");
        String[] headers = {"Category", "Supply Type", "Recipient Type", "Invoice Count", "Reported Value"};
        createHeaderRow(sheet, headers, headerStyle);

        int r = 1;
        for (GSTR1Models.NilExemptRow row : report.nilExemptList) {
            Row rowView = sheet.createRow(r++);
            rowView.createCell(0).setCellValue(row.category);
            rowView.createCell(1).setCellValue(row.supplyType);
            rowView.createCell(2).setCellValue(row.recipientType);
            rowView.createCell(3).setCellValue(row.invoiceCount);
            rowView.createCell(4).setCellValue(row.reportedValue.doubleValue());
        }
        autoSizeCols(sheet, headers.length);
    }

    private static void buildHsnSheet(XSSFWorkbook wb, GSTR1Models.ReportData report, CellStyle headerStyle) {
        Sheet sheet = wb.createSheet("HSN Summary");
        String[] headers = {"HSN/SAC", "Description", "UQC", "Quantity", "Rate (%)", "Supply Type", "Taxable Value", "CGST", "SGST", "IGST", "CESS", "Total Tax", "Total Value"};
        createHeaderRow(sheet, headers, headerStyle);

        int r = 1;
        java.util.List<GSTR1Models.HsnRow> allHsn = new java.util.ArrayList<>();
        allHsn.addAll(report.hsnB2bList);
        allHsn.addAll(report.hsnB2cList);

        for (GSTR1Models.HsnRow row : allHsn) {
            Row rowView = sheet.createRow(r++);
            rowView.createCell(0).setCellValue(row.hsnCode);
            rowView.createCell(1).setCellValue(row.description);
            rowView.createCell(2).setCellValue(row.uqc);
            rowView.createCell(3).setCellValue(row.quantity);
            rowView.createCell(4).setCellValue(row.taxRate);
            rowView.createCell(5).setCellValue(row.supplyType);
            rowView.createCell(6).setCellValue(row.taxableValue.doubleValue());
            rowView.createCell(7).setCellValue(row.cgst.doubleValue());
            rowView.createCell(8).setCellValue(row.sgst.doubleValue());
            rowView.createCell(9).setCellValue(row.igst.doubleValue());
            rowView.createCell(10).setCellValue(row.cess.doubleValue());
            rowView.createCell(11).setCellValue(row.totalTax.doubleValue());
            rowView.createCell(12).setCellValue(row.totalValue.doubleValue());
        }
        autoSizeCols(sheet, headers.length);
    }

    private static void createHeaderRow(Sheet sheet, String[] headers, CellStyle style) {
        Row r = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell c = r.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(style);
        }
    }

    private static void autoSizeCols(Sheet sheet, int numCols) {
        for (int i = 0; i < numCols; i++) {
            sheet.setColumnWidth(i, 16 * 256);
        }
    }
}
