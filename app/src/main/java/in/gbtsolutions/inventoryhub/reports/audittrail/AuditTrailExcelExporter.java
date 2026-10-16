package in.gbtsolutions.inventoryhub.reports.audittrail;

import android.content.Context;
import android.os.Environment;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AuditTrailExcelExporter {

    public static File exportToExcel(Context context, AuditTrailReportModels.ReportData report) throws IOException {
        report.ensureNonNull();

        XSSFWorkbook workbook = new XSSFWorkbook();

        // Styles
        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);

        CellStyle subtitleStyle = workbook.createCellStyle();
        Font subtitleFont = workbook.createFont();
        subtitleFont.setFontHeightInPoints((short) 10);
        subtitleFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        subtitleStyle.setFont(subtitleFont);

        // Header style: Royal Blue with white bold text
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

        CellStyle totalStyle = workbook.createCellStyle();
        Font totalFont = workbook.createFont();
        totalFont.setBold(true);
        totalStyle.setFont(totalFont);
        totalStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        totalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        totalStyle.setBorderBottom(BorderStyle.THIN);
        totalStyle.setBorderTop(BorderStyle.THIN);
        totalStyle.setBorderLeft(BorderStyle.THIN);
        totalStyle.setBorderRight(BorderStyle.THIN);

        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setBorderBottom(BorderStyle.THIN);
        cellStyle.setBorderTop(BorderStyle.THIN);
        cellStyle.setBorderLeft(BorderStyle.THIN);
        cellStyle.setBorderRight(BorderStyle.THIN);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        CellStyle detailsStyle = workbook.createCellStyle();
        detailsStyle.setBorderBottom(BorderStyle.THIN);
        detailsStyle.setBorderTop(BorderStyle.THIN);
        detailsStyle.setBorderLeft(BorderStyle.THIN);
        detailsStyle.setBorderRight(BorderStyle.THIN);
        detailsStyle.setWrapText(true);
        detailsStyle.setVerticalAlignment(VerticalAlignment.TOP);

        // Action-specific styles
        CellStyle actionAddStyle = workbook.createCellStyle();
        actionAddStyle.cloneStyleFrom(cellStyle);
        actionAddStyle.setAlignment(HorizontalAlignment.CENTER);
        actionAddStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        actionAddStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle actionEditStyle = workbook.createCellStyle();
        actionEditStyle.cloneStyleFrom(cellStyle);
        actionEditStyle.setAlignment(HorizontalAlignment.CENTER);
        actionEditStyle.setFillForegroundColor(IndexedColors.LEMON_CHIFFON.getIndex());
        actionEditStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle actionDeleteStyle = workbook.createCellStyle();
        actionDeleteStyle.cloneStyleFrom(cellStyle);
        actionDeleteStyle.setAlignment(HorizontalAlignment.CENTER);
        actionDeleteStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
        actionDeleteStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        // ================= Sheet 1: Audit Trail =================
        Sheet sheet1 = workbook.createSheet("Audit Trail");

        Row r0 = sheet1.createRow(0);
        Cell c0 = r0.createCell(0);
        c0.setCellValue("AUDIT TRAIL — " + report.companyName);
        c0.setCellStyle(titleStyle);

        Row r1 = sheet1.createRow(1);
        r1.createCell(0).setCellValue("GSTIN: " + (report.companyGst != null && !report.companyGst.isEmpty() ? report.companyGst : "N/A"));
        r1.getCell(0).setCellStyle(subtitleStyle);

        Row r2 = sheet1.createRow(2);
        r2.createCell(0).setCellValue("Period: " + report.startDate + " to " + report.endDate);
        r2.getCell(0).setCellStyle(subtitleStyle);

        String[] headers = {"ID", "Action", "Module", "Record ID", "Performed By", "Date & Time", "Details"};
        Row headerRow = sheet1.createRow(4);
        for (int i = 0; i < headers.length; i++) {
            Cell c = headerRow.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(headerStyle);
        }

        int rowIdx = 5;
        for (AuditTrailReportModels.AuditTrailRow item : report.rows) {
            Row row = sheet1.createRow(rowIdx++);

            Cell cId = row.createCell(0);
            cId.setCellValue(item.id);
            cId.setCellStyle(cellStyle);

            Cell cAction = row.createCell(1);
            cAction.setCellValue(item.actionType);
            String act = item.actionType != null ? item.actionType.toUpperCase(Locale.getDefault()) : "";
            if ("ADD".equals(act) || "CREATE".equals(act)) {
                cAction.setCellStyle(actionAddStyle);
            } else if ("EDIT".equals(act) || "UPDATE".equals(act)) {
                cAction.setCellStyle(actionEditStyle);
            } else if ("DELETE".equals(act)) {
                cAction.setCellStyle(actionDeleteStyle);
            } else {
                cAction.setCellStyle(cellStyle);
            }

            Cell cMod = row.createCell(2);
            cMod.setCellValue(item.module);
            cMod.setCellStyle(cellStyle);

            Cell cRec = row.createCell(3);
            cRec.setCellValue(item.recordId);
            cRec.setCellStyle(cellStyle);

            Cell cUser = row.createCell(4);
            cUser.setCellValue(item.performedBy);
            cUser.setCellStyle(cellStyle);

            Cell cDt = row.createCell(5);
            cDt.setCellValue(item.formattedDateTime);
            cDt.setCellStyle(cellStyle);

            Cell cDet = row.createCell(6);
            cDet.setCellValue(item.details);
            cDet.setCellStyle(detailsStyle);
        }

        // Totals Row
        Row totalRow = sheet1.createRow(rowIdx++);
        Cell totLabel = totalRow.createCell(0);
        totLabel.setCellValue("TOTAL");
        totLabel.setCellStyle(totalStyle);

        Cell totEvents = totalRow.createCell(1);
        totEvents.setCellValue(report.summary.totalEvents + " events");
        totEvents.setCellStyle(totalStyle);

        for (int i = 2; i < headers.length; i++) {
            Cell tc = totalRow.createCell(i);
            tc.setCellStyle(totalStyle);
        }

        // Column widths
        int[] widths = {8 * 256, 12 * 256, 16 * 256, 20 * 256, 22 * 256, 24 * 256, 75 * 256};
        for (int i = 0; i < widths.length; i++) {
            sheet1.setColumnWidth(i, widths[i]);
        }

        // ================= Sheet 2: Executive Summary =================
        Sheet sheet2 = workbook.createSheet("Executive Summary");
        sheet2.setColumnWidth(0, 32 * 256);
        sheet2.setColumnWidth(1, 24 * 256);

        Row s2r0 = sheet2.createRow(0);
        Cell s2c0 = s2r0.createCell(0);
        s2c0.setCellValue("AUDIT TRAIL EXECUTIVE SUMMARY — " + report.companyName);
        s2c0.setCellStyle(titleStyle);

        Row s2r1 = sheet2.createRow(1);
        s2r1.createCell(0).setCellValue("GSTIN: " + (!report.companyGst.isEmpty() ? report.companyGst : "N/A"));
        s2r1.getCell(0).setCellStyle(subtitleStyle);

        Row s2r2 = sheet2.createRow(2);
        s2r2.createCell(0).setCellValue("Reporting Period: " + report.startDate + " to " + report.endDate);
        s2r2.getCell(0).setCellStyle(subtitleStyle);

        Row s2Header = sheet2.createRow(4);
        Cell s2h1 = s2Header.createCell(0);
        s2h1.setCellValue("Metric Description");
        s2h1.setCellStyle(headerStyle);
        Cell s2h2 = s2Header.createCell(1);
        s2h2.setCellValue("Value");
        s2h2.setCellStyle(headerStyle);

        AuditTrailReportModels.AuditTrailSummary sm = report.summary;
        String[][] summaryData = {
                {"Total Events", String.valueOf(sm.totalEvents)},
                {"Create Events", String.valueOf(sm.creates)},
                {"Update Events", String.valueOf(sm.updates)},
                {"Delete Events", String.valueOf(sm.deletes)},
                {"Receive Events", String.valueOf(sm.receives)},
                {"Cancel Events", String.valueOf(sm.cancels)},
                {"Unique Users", String.valueOf(sm.uniqueUsers)},
                {"Unique Modules", String.valueOf(sm.uniqueModules)},
                {"First Event At", sm.firstEventAt},
                {"Last Event At", sm.lastEventAt}
        };

        int s2Idx = 5;
        for (String[] pair : summaryData) {
            Row r = sheet2.createRow(s2Idx++);
            Cell cL = r.createCell(0);
            cL.setCellValue(pair[0]);
            cL.setCellStyle(cellStyle);

            Cell cV = r.createCell(1);
            cV.setCellValue(pair[1]);
            cV.setCellStyle(cellStyle);
        }

        // ================= Sheet 3: Module Breakdown =================
        Sheet sheet3 = workbook.createSheet("Module Breakdown");
        String[] mbHeaders = {"Module", "Total", "Create", "Update", "Delete", "Receive", "Cancel"};
        Row mbHeaderRow = sheet3.createRow(0);
        for (int i = 0; i < mbHeaders.length; i++) {
            Cell c = mbHeaderRow.createCell(i);
            c.setCellValue(mbHeaders[i]);
            c.setCellStyle(headerStyle);
        }

        int mbRowIdx = 1;
        for (AuditTrailReportModels.AuditTrailModuleBreakdown m : report.moduleBreakdown) {
            Row r = sheet3.createRow(mbRowIdx++);
            Cell c0m = r.createCell(0);
            c0m.setCellValue(m.module);
            c0m.setCellStyle(cellStyle);

            int[] vals = {m.count, m.creates, m.updates, m.deletes, m.receives, m.cancels};
            for (int i = 0; i < vals.length; i++) {
                Cell cv = r.createCell(i + 1);
                cv.setCellValue(vals[i]);
                cv.setCellStyle(cellStyle);
            }
        }

        int[] mbWidths = {24 * 256, 12 * 256, 12 * 256, 12 * 256, 12 * 256, 12 * 256, 12 * 256};
        for (int i = 0; i < mbWidths.length; i++) {
            sheet3.setColumnWidth(i, mbWidths[i]);
        }

        // Write to file
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "Audit_Trail_" + timestamp + ".xlsx";

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
}
