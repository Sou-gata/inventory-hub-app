package in.gbtsolutions.inventoryhub.reports.userreport;

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
import java.util.Locale;

public class UserWiseReportPdfExporter {

    // A4 Landscape dimensions in PostScript points: 842 x 595
    private static final int PAGE_WIDTH = 842;
    private static final int PAGE_HEIGHT = 595;
    private static final int MARGIN = 30;

    public static File exportToPdf(Context context, UserWiseReportModels.ReportData report) throws IOException {
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
        boldPaint.setColor(Color.parseColor("#1A1D2E"));
        canvas.drawText(report.companyName, MARGIN, y, boldPaint);

        paint.setTextSize(10);
        paint.setColor(Color.parseColor("#4A5278"));
        String dateHeader = "Generated: " + new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date());
        float dateWidth = paint.measureText(dateHeader);
        canvas.drawText(dateHeader, PAGE_WIDTH - MARGIN - dateWidth, y, paint);

        y += 18;
        boldPaint.setTextSize(12);
        boldPaint.setColor(Color.parseColor("#2979FF"));
        canvas.drawText("USER WISE REPORT — SALES, PURCHASES & GST SUMMARY", MARGIN, y, boldPaint);

        y += 14;
        paint.setTextSize(9);
        paint.setColor(Color.parseColor("#4A5278"));
        String metaLine = "GSTIN: " + (report.companyGst != null && !report.companyGst.isEmpty() ? report.companyGst : "N/A")
                + "  |  Period: " + (report.startDate != null ? report.startDate : "Start") + " to " + (report.endDate != null ? report.endDate : "End")
                + "  |  User: " + (report.filterUserName != null ? report.filterUserName : "All Users");
        canvas.drawText(metaLine, MARGIN, y, paint);

        y += 14;
        paint.setColor(Color.parseColor("#2979FF"));
        paint.setStrokeWidth(1.5f);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);
        y += 14;

        // 2. KPI Summary Bar
        paint.setColor(Color.parseColor("#F4F6FC"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 42, paint);

        int kpiY = y + 16;
        paint.setColor(Color.parseColor("#5A6588"));
        paint.setTextSize(8);
        boldPaint.setTextSize(9);
        boldPaint.setColor(Color.parseColor("#1A1D2E"));

        int colWidth = (PAGE_WIDTH - MARGIN * 2) / 6;

        // KPI 1: Gross Sales
        canvas.drawText("GROSS SALES", MARGIN + 8, kpiY, paint);
        canvas.drawText("₹" + report.summary.totalGrossSales.toPlainString() + " (" + report.summary.totalSalesCount + " Inv)", MARGIN + 8, kpiY + 14, boldPaint);

        // KPI 2: Gross Purchases
        canvas.drawText("GROSS PURCHASES", MARGIN + colWidth + 5, kpiY, paint);
        canvas.drawText("₹" + report.summary.totalGrossPurchases.toPlainString() + " (" + report.summary.totalPurchaseCount + " PO)", MARGIN + colWidth + 5, kpiY + 14, boldPaint);

        // KPI 3: Sales GST
        canvas.drawText("SALES GST (OUTPUT)", MARGIN + colWidth * 2 + 5, kpiY, paint);
        canvas.drawText("₹" + report.summary.totalSalesGst.toPlainString(), MARGIN + colWidth * 2 + 5, kpiY + 14, boldPaint);

        // KPI 4: Purchase GST
        canvas.drawText("PURCHASE GST (INPUT)", MARGIN + colWidth * 3 + 5, kpiY, paint);
        canvas.drawText("₹" + report.summary.totalPurchaseGst.toPlainString(), MARGIN + colWidth * 3 + 5, kpiY + 14, boldPaint);

        // KPI 5: Total Combined GST
        canvas.drawText("COMBINED GST", MARGIN + colWidth * 4 + 5, kpiY, paint);
        canvas.drawText("₹" + report.summary.totalCombinedGst.toPlainString(), MARGIN + colWidth * 4 + 5, kpiY + 14, boldPaint);

        // KPI 6: Net Turnover
        canvas.drawText("NET TURNOVER", MARGIN + colWidth * 5 + 5, kpiY, paint);
        boldPaint.setColor(Color.parseColor("#2979FF"));
        canvas.drawText("₹" + report.summary.netTurnover.toPlainString(), MARGIN + colWidth * 5 + 5, kpiY + 14, boldPaint);

        y += 54;

        // 3. Table Column Layout
        int[] colX = {
                MARGIN,             // 0: #
                MARGIN + 22,        // 1: User / Staff
                MARGIN + 145,       // 2: Role
                MARGIN + 205,       // 3: Sales Inv
                MARGIN + 260,       // 4: Gross Sales
                MARGIN + 338,       // 5: Sales GST
                MARGIN + 408,       // 6: Purchases
                MARGIN + 468,       // 7: Gross Purchase
                MARGIN + 550,       // 8: Purchase GST
                MARGIN + 626,       // 9: Total GST
                MARGIN + 700        // 10: Net Amount
        };

        y = drawTableHeader(canvas, y, colX, boldPaint, paint);

        int slNo = 1;
        int rowHeight = 18;

        for (UserWiseReportModels.UserWiseReportRow row : report.rows) {
            if (y + rowHeight > PAGE_HEIGHT - MARGIN - 20) {
                drawPageFooter(canvas, pageNumber, paint);
                document.finishPage(currentPage);

                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                currentPage = document.startPage(pageInfo);
                canvas = currentPage.getCanvas();

                y = MARGIN + 10;
                boldPaint.setTextSize(10);
                boldPaint.setColor(Color.parseColor("#2979FF"));
                canvas.drawText("USER WISE REPORT (Continued) — " + report.companyName, MARGIN, y, boldPaint);
                y += 14;

                y = drawTableHeader(canvas, y, colX, boldPaint, paint);
            }

            // Alternating row background
            if (slNo % 2 == 0) {
                paint.setColor(Color.parseColor("#F9FAFC"));
                paint.setStyle(Paint.Style.FILL);
                canvas.drawRect(MARGIN, y - 11, PAGE_WIDTH - MARGIN, y + rowHeight - 11, paint);
            }

            paint.setColor(Color.parseColor("#1A1D2E"));
            paint.setTextSize(8);
            boldPaint.setTextSize(8);

            // Draw Cells
            canvas.drawText(String.valueOf(slNo++), colX[0], y, paint);
            String userDisplay = row.userName + (!TextUtils.isEmpty(row.username) ? " (@" + row.username + ")" : "");
            canvas.drawText(truncate(userDisplay, 18), colX[1], y, paint);
            canvas.drawText(truncate(row.role, 10), colX[2], y, paint);
            canvas.drawText(String.valueOf(row.salesCount), colX[3], y, paint);
            canvas.drawText("₹" + row.salesGrossTotal.toPlainString(), colX[4], y, paint);
            canvas.drawText("₹" + row.salesTotalGst.toPlainString(), colX[5], y, paint);
            canvas.drawText(String.valueOf(row.purchaseCount), colX[6], y, paint);
            canvas.drawText("₹" + row.purchaseGrossTotal.toPlainString(), colX[7], y, paint);
            canvas.drawText("₹" + row.purchaseTotalGst.toPlainString(), colX[8], y, paint);
            canvas.drawText("₹" + row.totalCombinedGst.toPlainString(), colX[9], y, paint);

            boldPaint.setColor(row.netTurnover.doubleValue() >= 0 ? Color.parseColor("#1B5E20") : Color.parseColor("#C62828"));
            canvas.drawText("₹" + row.netTurnover.toPlainString(), colX[10], y, boldPaint);

            y += rowHeight;
        }

        // Draw Summary Total Row
        if (y + 24 > PAGE_HEIGHT - MARGIN - 20) {
            drawPageFooter(canvas, pageNumber, paint);
            document.finishPage(currentPage);

            pageNumber++;
            pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
            currentPage = document.startPage(pageInfo);
            canvas = currentPage.getCanvas();
            y = MARGIN + 20;
        }

        paint.setColor(Color.parseColor("#D0D5E5"));
        paint.setStrokeWidth(1f);
        canvas.drawLine(MARGIN, y - 6, PAGE_WIDTH - MARGIN, y - 6, paint);

        paint.setColor(Color.parseColor("#EBF0FA"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, y - 5, PAGE_WIDTH - MARGIN, y + 16, paint);

        boldPaint.setColor(Color.parseColor("#1A1D2E"));
        boldPaint.setTextSize(8);
        canvas.drawText("TOTAL", colX[0], y + 6, boldPaint);
        canvas.drawText(String.valueOf(report.summary.totalSalesCount), colX[3], y + 6, boldPaint);
        canvas.drawText("₹" + report.summary.totalGrossSales.toPlainString(), colX[4], y + 6, boldPaint);
        canvas.drawText("₹" + report.summary.totalSalesGst.toPlainString(), colX[5], y + 6, boldPaint);
        canvas.drawText(String.valueOf(report.summary.totalPurchaseCount), colX[6], y + 6, boldPaint);
        canvas.drawText("₹" + report.summary.totalGrossPurchases.toPlainString(), colX[7], y + 6, boldPaint);
        canvas.drawText("₹" + report.summary.totalPurchaseGst.toPlainString(), colX[8], y + 6, boldPaint);
        canvas.drawText("₹" + report.summary.totalCombinedGst.toPlainString(), colX[9], y + 6, boldPaint);

        boldPaint.setColor(Color.parseColor("#2979FF"));
        canvas.drawText("₹" + report.summary.netTurnover.toPlainString(), colX[10], y + 6, boldPaint);

        // Draw double underline
        paint.setColor(Color.parseColor("#1A1D2E"));
        paint.setStrokeWidth(1.2f);
        canvas.drawLine(MARGIN, y + 18, PAGE_WIDTH - MARGIN, y + 18, paint);
        canvas.drawLine(MARGIN, y + 20, PAGE_WIDTH - MARGIN, y + 20, paint);

        drawPageFooter(canvas, pageNumber, paint);
        document.finishPage(currentPage);

        // Write to file
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "User_Wise_Report_" + timestamp + ".pdf";

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

    private static int drawTableHeader(Canvas canvas, int y, int[] colX, Paint boldPaint, Paint paint) {
        paint.setColor(Color.parseColor("#EBF0FA"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, y - 11, PAGE_WIDTH - MARGIN, y + 10, paint);

        boldPaint.setColor(Color.parseColor("#1A1D2E"));
        boldPaint.setTextSize(8);

        canvas.drawText("#", colX[0], y, boldPaint);
        canvas.drawText("USER / STAFF", colX[1], y, boldPaint);
        canvas.drawText("ROLE", colX[2], y, boldPaint);
        canvas.drawText("SALES (INV)", colX[3], y, boldPaint);
        canvas.drawText("GROSS SALES", colX[4], y, boldPaint);
        canvas.drawText("SALES GST", colX[5], y, boldPaint);
        canvas.drawText("PURCHASES (PO)", colX[6], y, boldPaint);
        canvas.drawText("GROSS PURCHASE", colX[7], y, boldPaint);
        canvas.drawText("PURCHASE GST", colX[8], y, boldPaint);
        canvas.drawText("TOTAL GST", colX[9], y, boldPaint);
        canvas.drawText("NET AMOUNT", colX[10], y, boldPaint);

        paint.setColor(Color.parseColor("#CBD2E5"));
        paint.setStrokeWidth(1f);
        canvas.drawLine(MARGIN, y + 11, PAGE_WIDTH - MARGIN, y + 11, paint);

        return y + 20;
    }

    private static void drawPageFooter(Canvas canvas, int pageNumber, Paint paint) {
        paint.setTextSize(8);
        paint.setColor(Color.parseColor("#7A849E"));
        String footerText = "Generated by InventoryHub  |  Page " + pageNumber;
        float width = paint.measureText(footerText);
        canvas.drawText(footerText, PAGE_WIDTH / 2f - width / 2f, PAGE_HEIGHT - MARGIN + 10, paint);
    }

    private static String truncate(String text, int maxChars) {
        if (text == null) return "";
        if (text.length() <= maxChars) return text;
        return text.substring(0, maxChars - 1) + "…";
    }
}
