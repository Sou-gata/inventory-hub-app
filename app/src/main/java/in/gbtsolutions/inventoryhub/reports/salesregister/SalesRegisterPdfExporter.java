package in.gbtsolutions.inventoryhub.reports.salesregister;

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

public class SalesRegisterPdfExporter {

    // A4 Landscape points: 842 x 595
    private static final int PAGE_WIDTH = 842;
    private static final int PAGE_HEIGHT = 595;
    private static final int MARGIN = 30;

    public static File exportToPdf(Context context, SalesRegisterModels.ReportData report) throws IOException {
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
        canvas.drawText("SALES REGISTER — INVOICE-WISE TAX DETAILS", MARGIN, y, boldPaint);

        y += 14;
        paint.setTextSize(9);
        paint.setColor(Color.parseColor("#4A5278"));
        String gstinPeriod = "GSTIN: " + (report.companyGst != null && !report.companyGst.isEmpty() ? report.companyGst : "N/A")
                + "  |  Period: " + (report.startDate != null ? report.startDate : "Start") + " to " + (report.endDate != null ? report.endDate : "End");
        canvas.drawText(gstinPeriod, MARGIN, y, paint);

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

        // KPI 1: Invoices
        canvas.drawText("TOTAL INVOICES", MARGIN + 10, kpiY, paint);
        canvas.drawText(report.summary.totalInvoices + " (" + report.summary.b2bInvoices + " B2B / " + report.summary.b2cInvoices + " B2C)", MARGIN + 10, kpiY + 14, boldPaint);

        // KPI 2: Gross Sales
        canvas.drawText("GROSS SALES", MARGIN + colWidth + 5, kpiY, paint);
        canvas.drawText("₹ " + report.summary.grossSales.toPlainString(), MARGIN + colWidth + 5, kpiY + 14, boldPaint);

        // KPI 3: Taxable Value
        canvas.drawText("TAXABLE VALUE", MARGIN + colWidth * 2 + 5, kpiY, paint);
        canvas.drawText("₹ " + report.summary.taxableValue.toPlainString(), MARGIN + colWidth * 2 + 5, kpiY + 14, boldPaint);

        // KPI 4: CGST
        canvas.drawText("CGST", MARGIN + colWidth * 3 + 5, kpiY, paint);
        canvas.drawText("₹ " + report.summary.cgst.toPlainString(), MARGIN + colWidth * 3 + 5, kpiY + 14, boldPaint);

        // KPI 5: SGST
        canvas.drawText("SGST", MARGIN + colWidth * 4 + 5, kpiY, paint);
        canvas.drawText("₹ " + report.summary.sgst.toPlainString(), MARGIN + colWidth * 4 + 5, kpiY + 14, boldPaint);

        // KPI 6: Total Tax
        canvas.drawText("TOTAL TAX", MARGIN + colWidth * 5 + 5, kpiY, paint);
        boldPaint.setColor(Color.parseColor("#2979FF"));
        canvas.drawText("₹ " + report.summary.totalTax.toPlainString(), MARGIN + colWidth * 5 + 5, kpiY + 14, boldPaint);

        y += 54;

        // 3. Table Column Layout
        int[] colX = {
                MARGIN,             // 0: #
                MARGIN + 24,        // 1: Invoice #
                MARGIN + 105,       // 2: Date
                MARGIN + 175,       // 3: Customer
                MARGIN + 310,       // 4: GSTIN
                MARGIN + 400,       // 5: POS
                MARGIN + 465,       // 6: Taxable
                MARGIN + 535,       // 7: CGST
                MARGIN + 595,       // 8: SGST
                MARGIN + 655,       // 9: IGST
                MARGIN + 715,       // 10: Tax
                MARGIN + 775        // 11: Total
        };

        // Draw Table Header
        y = drawTableHeader(canvas, y, colX, boldPaint, paint);

        int slNo = 1;
        int rowHeight = 18;

        for (SalesRegisterModels.SalesRegisterRow row : report.rows) {
            if (y + rowHeight > PAGE_HEIGHT - MARGIN - 20) {
                // Page footer
                drawPageFooter(canvas, pageNumber, paint);
                document.finishPage(currentPage);

                pageNumber++;
                pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create();
                currentPage = document.startPage(pageInfo);
                canvas = currentPage.getCanvas();

                y = MARGIN + 10;
                boldPaint.setTextSize(10);
                boldPaint.setColor(Color.parseColor("#2979FF"));
                canvas.drawText("SALES REGISTER (Continued) — " + report.companyName, MARGIN, y, boldPaint);
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

            // Row Cells
            canvas.drawText(String.valueOf(slNo++), colX[0], y, paint);
            canvas.drawText(truncate(row.invoiceId, 12), colX[1], y, paint);
            canvas.drawText(row.formattedDate, colX[2], y, paint);
            canvas.drawText(truncate(row.customerName, 20), colX[3], y, paint);
            canvas.drawText(!TextUtils.isEmpty(row.customerGstin) ? row.customerGstin : "Unregistered", colX[4], y, paint);
            canvas.drawText(truncate(row.placeOfSupply, 10), colX[5], y, paint);
            canvas.drawText("₹" + row.taxableValue.toPlainString(), colX[6], y, paint);
            canvas.drawText("₹" + row.cgstAmount.toPlainString(), colX[7], y, paint);
            canvas.drawText("₹" + row.sgstAmount.toPlainString(), colX[8], y, paint);
            canvas.drawText("₹" + row.igstAmount.toPlainString(), colX[9], y, paint);
            canvas.drawText("₹" + row.totalTax.toPlainString(), colX[10], y, paint);
            boldPaint.setColor(Color.parseColor("#1A1D2E"));
            canvas.drawText("₹" + row.totalAmount.toPlainString(), colX[11], y, boldPaint);

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

        paint.setColor(Color.parseColor("#E0E4EC"));
        paint.setStrokeWidth(1.5f);
        canvas.drawLine(MARGIN, y - 8, PAGE_WIDTH - MARGIN, y - 8, paint);

        paint.setColor(Color.parseColor("#EEF2F6"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, y - 7, PAGE_WIDTH - MARGIN, y + 16, paint);

        boldPaint.setTextSize(8.5f);
        boldPaint.setColor(Color.parseColor("#1A1D2E"));
        canvas.drawText("TOTAL (" + report.summary.totalInvoices + " INVOICES):", colX[3], y + 6, boldPaint);
        canvas.drawText("₹" + report.summary.taxableValue.toPlainString(), colX[6], y + 6, boldPaint);
        canvas.drawText("₹" + report.summary.cgst.toPlainString(), colX[7], y + 6, boldPaint);
        canvas.drawText("₹" + report.summary.sgst.toPlainString(), colX[8], y + 6, boldPaint);
        canvas.drawText("₹" + report.summary.igst.toPlainString(), colX[9], y + 6, boldPaint);
        canvas.drawText("₹" + report.summary.totalTax.toPlainString(), colX[10], y + 6, boldPaint);
        boldPaint.setColor(Color.parseColor("#2979FF"));
        canvas.drawText("₹" + report.summary.grossSales.toPlainString(), colX[11], y + 6, boldPaint);

        drawPageFooter(canvas, pageNumber, paint);
        document.finishPage(currentPage);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "Sales_Register_" + timestamp + ".pdf";

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
        paint.setColor(Color.parseColor("#2979FF"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20, paint);

        boldPaint.setColor(Color.WHITE);
        boldPaint.setTextSize(8);

        canvas.drawText("#", colX[0], y + 13, boldPaint);
        canvas.drawText("INVOICE #", colX[1], y + 13, boldPaint);
        canvas.drawText("DATE", colX[2], y + 13, boldPaint);
        canvas.drawText("CUSTOMER", colX[3], y + 13, boldPaint);
        canvas.drawText("GSTIN", colX[4], y + 13, boldPaint);
        canvas.drawText("POS", colX[5], y + 13, boldPaint);
        canvas.drawText("TAXABLE", colX[6], y + 13, boldPaint);
        canvas.drawText("CGST", colX[7], y + 13, boldPaint);
        canvas.drawText("SGST", colX[8], y + 13, boldPaint);
        canvas.drawText("IGST", colX[9], y + 13, boldPaint);
        canvas.drawText("TOTAL TAX", colX[10], y + 13, boldPaint);
        canvas.drawText("TOTAL", colX[11], y + 13, boldPaint);

        return y + 30;
    }

    private static void drawPageFooter(Canvas canvas, int pageNumber, Paint paint) {
        paint.setTextSize(8);
        paint.setColor(Color.parseColor("#8892B0"));
        canvas.drawText("Page " + pageNumber, PAGE_WIDTH / 2 - 15, PAGE_HEIGHT - 12, paint);
        canvas.drawText("InventoryHub System Generated Sales Register", MARGIN, PAGE_HEIGHT - 12, paint);
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 1) + "…";
    }
}
