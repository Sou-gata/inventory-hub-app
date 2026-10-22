package in.gbtsolutions.inventoryhub.reports.audittrail;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AuditTrailPdfExporter {

    // A4 Landscape dimensions in points: 842 x 595
    private static final int PAGE_WIDTH = 842;
    private static final int PAGE_HEIGHT = 595;
    private static final int MARGIN = 30;
    private static final int CONTENT_W = PAGE_WIDTH - MARGIN * 2; // 782

    // Color palette matching backend auditTrail.pdf.ts
    private static final int COLOR_DARK = Color.parseColor("#1A1D2E");
    private static final int COLOR_BLUE = Color.parseColor("#2979FF");
    private static final int COLOR_MUTED = Color.parseColor("#4A5278");
    private static final int COLOR_KPI_LABEL = Color.parseColor("#5A6588");
    private static final int COLOR_BOX = Color.parseColor("#F4F6FC");
    private static final int COLOR_ZEBRA = Color.parseColor("#F9FAFC");
    private static final int COLOR_TOTAL_BG = Color.parseColor("#EEF2F6");
    private static final int COLOR_TOTAL_LINE = Color.parseColor("#E0E4EC");
    private static final int COLOR_FOOTER = Color.parseColor("#8892B0");

    // Action colors
    private static final int COLOR_CREATE = Color.parseColor("#2E7D32");
    private static final int COLOR_UPDATE = Color.parseColor("#F57C00");
    private static final int COLOR_DELETE = Color.parseColor("#C62828");
    private static final int COLOR_RECEIVE = Color.parseColor("#1565C0");
    private static final int COLOR_CANCEL = Color.parseColor("#6A1B9A");

    public static File exportToPdf(Context context, AuditTrailReportModels.ReportData report) throws IOException {
        report.ensureNonNull();

        PdfDocument document = new PdfDocument();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint boldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boldPaint.setFakeBoldText(true);

        int pageNumber = 1;
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
        PdfDocument.Page currentPage = document.startPage(pageInfo);
        Canvas canvas = currentPage.getCanvas();

        int y = MARGIN + 16;

        // 1. Header Banner
        boldPaint.setTextSize(16);
        boldPaint.setColor(COLOR_DARK);
        canvas.drawText(report.companyName, MARGIN, y, boldPaint);

        paint.setTextSize(10);
        paint.setColor(COLOR_MUTED);
        String dateHeader = "Generated: " + new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        float dateWidth = paint.measureText(dateHeader);
        canvas.drawText(dateHeader, PAGE_WIDTH - MARGIN - dateWidth, y, paint);

        y += 18;
        boldPaint.setTextSize(12);
        boldPaint.setColor(COLOR_BLUE);
        canvas.drawText("AUDIT TRAIL — ACTIVITY LOG", MARGIN, y, boldPaint);

        y += 14;
        paint.setTextSize(9);
        paint.setColor(COLOR_MUTED);
        String metaLine = "GSTIN: " + (!TextUtils.isEmpty(report.companyGst) ? report.companyGst : "N/A") + "  |  Period: " + (!TextUtils.isEmpty(report.startDate) ? report.startDate : "Start") + " to " + (!TextUtils.isEmpty(report.endDate) ? report.endDate : "End");
        canvas.drawText(metaLine, MARGIN, y, paint);

        y += 14;
        paint.setColor(COLOR_BLUE);
        paint.setStrokeWidth(1.5f);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);
        y += 14;

        // 2. KPI Summary Bar (Height 42)
        paint.setColor(COLOR_BOX);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 42, paint);

        int kpiColW = CONTENT_W / 6;
        AuditTrailReportModels.AuditTrailSummary s = report.summary;
        String[][] kpis = {{"TOTAL EVENTS", String.valueOf(s.totalEvents), String.valueOf(COLOR_DARK)}, {"CREATE", String.valueOf(s.creates), String.valueOf(COLOR_CREATE)}, {"UPDATE", String.valueOf(s.updates), String.valueOf(COLOR_UPDATE)}, {"DELETE", String.valueOf(s.deletes), String.valueOf(COLOR_DELETE)}, {"RECEIVE", String.valueOf(s.receives), String.valueOf(COLOR_RECEIVE)}, {"CANCEL", String.valueOf(s.cancels), String.valueOf(COLOR_CANCEL)}};

        for (int i = 0; i < kpis.length; i++) {
            int kx = MARGIN + kpiColW * i + 8;
            paint.setTextSize(8);
            paint.setColor(COLOR_KPI_LABEL);
            canvas.drawText(kpis[i][0], kx, y + 16, paint);

            boldPaint.setTextSize(11);
            boldPaint.setColor(Integer.parseInt(kpis[i][2]));
            canvas.drawText(kpis[i][1], kx, y + 32, boldPaint);
        }

        y += 54;

        // 3. Table Column Layout (Sum: 70 + 90 + 130 + 130 + 362 = 782 = CONTENT_W)
        // Notice: ID and RECORD ID are deliberately omitted per specification
        int[] colW = {70, 90, 130, 130, 362};
        int[] colX = new int[5];
        colX[0] = MARGIN;
        for (int i = 1; i < 5; i++) {
            colX[i] = colX[i - 1] + colW[i - 1];
        }

        y = drawTableHeader(canvas, y, colX, colW, boldPaint, paint);

        int rowHeight = 16;
        int bottomLimit = PAGE_HEIGHT - MARGIN - 20;

        if (report.rows.isEmpty()) {
            paint.setTextSize(9);
            paint.setColor(COLOR_DARK);
            canvas.drawText("No audit trail events in this period.", MARGIN + 4, y + 10, paint);
            y += rowHeight;
        }

        int rowIndex = 0;
        for (AuditTrailReportModels.AuditTrailRow row : report.rows) {
            if (y + rowHeight > bottomLimit) {
                drawPageFooter(canvas, pageNumber, paint);
                document.finishPage(currentPage);

                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                currentPage = document.startPage(pageInfo);
                canvas = currentPage.getCanvas();

                y = MARGIN + 10;
                boldPaint.setTextSize(10);
                boldPaint.setColor(COLOR_BLUE);
                canvas.drawText("AUDIT TRAIL (Continued) — " + report.companyName, MARGIN, y, boldPaint);
                y += 14;

                y = drawTableHeader(canvas, y, colX, colW, boldPaint, paint);
            }

            // Alternating zebra background
            if ((rowIndex + 1) % 2 == 0) {
                paint.setColor(COLOR_ZEBRA);
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(MARGIN, y - 10, PAGE_WIDTH - MARGIN, y + rowHeight - 10, paint);
            }

            // Action Badge / Text
            boldPaint.setTextSize(7.5f);
            boldPaint.setColor(getActionColor(row.actionType));
            canvas.drawText(truncate(row.actionType, colW[0] - 6, boldPaint), colX[0] + 3, y, boldPaint);

            // Module
            paint.setTextSize(7.5f);
            paint.setColor(COLOR_DARK);
            canvas.drawText(truncate(row.module, colW[1] - 6, paint), colX[1] + 3, y, paint);

            // Performed By
            canvas.drawText(truncate(row.performedBy, colW[2] - 6, paint), colX[2] + 3, y, paint);

            // Date & Time
            canvas.drawText(truncate(row.formattedDateTime, colW[3] - 6, paint), colX[3] + 3, y, paint);

            // Details
            canvas.drawText(truncate(row.details, colW[4] - 6, paint), colX[4] + 3, y, paint);

            y += rowHeight;
            rowIndex++;
        }

        // 4. Totals Row
        if (y + 26 > bottomLimit) {
            drawPageFooter(canvas, pageNumber, paint);
            document.finishPage(currentPage);

            pageNumber++;
            pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
            currentPage = document.startPage(pageInfo);
            canvas = currentPage.getCanvas();

            y = MARGIN + 10;
            boldPaint.setTextSize(10);
            boldPaint.setColor(COLOR_BLUE);
            canvas.drawText("AUDIT TRAIL (Continued) — " + report.companyName, MARGIN, y, boldPaint);
            y += 14;

            y = drawTableHeader(canvas, y, colX, colW, boldPaint, paint);
        }

        paint.setColor(COLOR_TOTAL_LINE);
        paint.setStrokeWidth(1.5f);
        canvas.drawLine(MARGIN, y - 6, PAGE_WIDTH - MARGIN, y - 6, paint);

        paint.setColor(COLOR_TOTAL_BG);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, y - 5, PAGE_WIDTH - MARGIN, y + 17, paint);

        boldPaint.setTextSize(7.5f);
        boldPaint.setColor(COLOR_DARK);
        canvas.drawText("TOTAL (" + s.totalEvents + " EVENTS)", colX[0] + 3, y + 6, boldPaint);
        canvas.drawText("Users: " + s.uniqueUsers, colX[2] + 3, y + 6, boldPaint);
        canvas.drawText("Modules: " + s.uniqueModules, colX[4] + 3, y + 6, boldPaint);

        y += 24;

        // 5. Module Breakdown Block
        if (report.moduleBreakdown != null && !report.moduleBreakdown.isEmpty()) {
            if (y + 60 > bottomLimit) {
                drawPageFooter(canvas, pageNumber, paint);
                document.finishPage(currentPage);

                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                currentPage = document.startPage(pageInfo);
                canvas = currentPage.getCanvas();
                y = MARGIN + 10;
            }

            y += 10;
            boldPaint.setTextSize(10);
            boldPaint.setColor(COLOR_BLUE);
            canvas.drawText("MODULE BREAKDOWN", MARGIN, y, boldPaint);
            y += 14;

            String[] mbLabels = {"MODULE", "TOTAL", "CREATE", "UPDATE", "DELETE", "RECEIVE", "CANCEL"};
            int[] mbW = {140, 70, 70, 70, 70, 70, 70};
            int[] mbX = new int[7];
            mbX[0] = MARGIN;
            for (int i = 1; i < 7; i++) {
                mbX[i] = mbX[i - 1] + mbW[i - 1];
            }

            int mbTotalW = 140 + 70 * 6;
            paint.setColor(COLOR_BOX);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawRect(MARGIN, y - 10, MARGIN + mbTotalW, y + 8, paint);

            boldPaint.setTextSize(8);
            boldPaint.setColor(COLOR_DARK);
            for (int i = 0; i < 7; i++) {
                canvas.drawText(mbLabels[i], mbX[i] + 4, y, boldPaint);
            }
            y += 16;

            for (int idx = 0; idx < report.moduleBreakdown.size(); idx++) {
                AuditTrailReportModels.AuditTrailModuleBreakdown m = report.moduleBreakdown.get(idx);
                if (y + 16 > bottomLimit) {
                    drawPageFooter(canvas, pageNumber, paint);
                    document.finishPage(currentPage);

                    pageNumber++;
                    pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                    currentPage = document.startPage(pageInfo);
                    canvas = currentPage.getCanvas();
                    y = MARGIN + 10;
                }

                if ((idx + 1) % 2 == 0) {
                    paint.setColor(COLOR_ZEBRA);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawRect(MARGIN, y - 10, MARGIN + mbTotalW, y + 6, paint);
                }

                paint.setTextSize(8);
                paint.setColor(COLOR_DARK);
                String[] mbVals = {m.module, String.valueOf(m.count), String.valueOf(m.creates), String.valueOf(m.updates), String.valueOf(m.deletes), String.valueOf(m.receives), String.valueOf(m.cancels)};

                for (int i = 0; i < 7; i++) {
                    canvas.drawText(mbVals[i], mbX[i] + 4, y, paint);
                }
                y += 16;
            }
        }

        drawPageFooter(canvas, pageNumber, paint);
        document.finishPage(currentPage);

        // Write to file
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "Audit_Trail_" + timestamp + ".pdf";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        if (!dir.exists()) dir.mkdirs();

        File outputFile = new File(dir, fileName);
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            document.writeTo(out);
        }
        document.close();

        return outputFile;
    }

    private static int drawTableHeader(Canvas canvas, int y, int[] colX, int[] colW, Paint boldPaint, Paint paint) {
        paint.setColor(COLOR_BLUE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, y - 11, PAGE_WIDTH - MARGIN, y + 9, paint);

        boldPaint.setColor(Color.WHITE);
        boldPaint.setTextSize(7.5f);

        String[] headers = {"ACTION", "MODULE", "PERFORMED BY", "DATE & TIME", "DETAILS"};
        for (int i = 0; i < headers.length; i++) {
            canvas.drawText(headers[i], colX[i] + 3, y, boldPaint);
        }

        return y + 16;
    }

    private static void drawPageFooter(Canvas canvas, int pageNumber, Paint paint) {
        paint.setTextSize(8);
        paint.setColor(COLOR_FOOTER);
        String leftText = "InventoryHub System Generated Audit Trail";
        String rightText = "Page " + pageNumber;
        canvas.drawText(leftText, MARGIN, PAGE_HEIGHT - 18, paint);
        float rw = paint.measureText(rightText);
        canvas.drawText(rightText, PAGE_WIDTH - MARGIN - rw, PAGE_HEIGHT - 18, paint);
    }

    private static int getActionColor(String action) {
        if (action == null) return COLOR_MUTED;
        String a = action.toUpperCase(Locale.getDefault());
        if ("ADD".equals(a) || "CREATE".equals(a)) return COLOR_CREATE;
        if ("EDIT".equals(a) || "UPDATE".equals(a)) return COLOR_UPDATE;
        if ("DELETE".equals(a)) return COLOR_DELETE;
        if ("RECEIVE".equals(a)) return COLOR_RECEIVE;
        if ("CANCEL".equals(a)) return COLOR_CANCEL;
        return COLOR_MUTED;
    }

    private static String truncate(String text, float maxW, Paint paint) {
        if (text == null) return "";
        if (paint.measureText(text) <= maxW) return text;
        String ell = "...";
        float ellW = paint.measureText(ell);
        if (maxW <= ellW) return ell;
        int len = text.length();
        while (len > 1 && paint.measureText(text.substring(0, len) + ell) > maxW) {
            len--;
        }
        return text.substring(0, len) + ell;
    }
}
