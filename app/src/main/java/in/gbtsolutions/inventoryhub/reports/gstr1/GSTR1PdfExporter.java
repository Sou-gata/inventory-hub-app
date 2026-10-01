package in.gbtsolutions.inventoryhub.reports.gstr1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class GSTR1PdfExporter {

    private static final int PAGE_WIDTH = 595; // A4 portrait points
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN = 36;

    public static File exportToPdf(Context context, GSTR1Models.ReportData report) throws IOException {
        PdfDocument document = new PdfDocument();

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint boldPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        boldPaint.setFakeBoldText(true);

        // Page 1: Header + Executive Summary KPIs + Tax & Doc Summary
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        int y = MARGIN + 20;

        // Company Title
        boldPaint.setTextSize(16);
        boldPaint.setColor(Color.parseColor("#1A1D2E"));
        canvas.drawText(report.companyName, MARGIN, y, boldPaint);
        y += 18;

        // Subheader
        paint.setTextSize(10);
        paint.setColor(Color.parseColor("#4A5278"));
        canvas.drawText("GSTR-1 Summary Report | GSTIN: " + (report.companyGst != null ? report.companyGst : "N/A"), MARGIN, y, paint);
        y += 14;
        canvas.drawText("Period: " + (report.startDate != null ? report.startDate : "Start") + " to " + (report.endDate != null ? report.endDate : "End"), MARGIN, y, paint);
        y += 20;

        // Divider
        paint.setColor(Color.parseColor("#2979FF"));
        paint.setStrokeWidth(2f);
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, paint);
        y += 24;

        // Section Title
        boldPaint.setTextSize(12);
        boldPaint.setColor(Color.parseColor("#2979FF"));
        canvas.drawText("1. EXECUTIVE SUMMARY & GST LIABILITY", MARGIN, y, boldPaint);
        y += 16;

        // KPI Box
        paint.setColor(Color.parseColor("#F4F6FC"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 160, paint);

        int textY = y + 22;
        paint.setColor(Color.parseColor("#1A1D2E"));
        paint.setTextSize(10);

        drawKpiLine(canvas, paint, boldPaint, "Gross Invoice Value:", "₹ " + report.kpi.totalSales.toPlainString(), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "Taxable Turnover:", "₹ " + report.kpi.taxableValue.toPlainString(), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "Central Tax (CGST):", "₹ " + report.kpi.cgst.toPlainString(), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "State Tax (SGST):", "₹ " + report.kpi.sgst.toPlainString(), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "Integrated Tax (IGST):", "₹ " + report.kpi.igst.toPlainString(), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "Cess:", "₹ " + report.kpi.cess.toPlainString(), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "Total GST Output Liability:", "₹ " + report.kpi.totalGst.toPlainString(), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "Total Invoices Filed:", String.valueOf(report.kpi.totalInvoices), MARGIN + 14, textY);

        y += 180;

        // Document Summary Box
        boldPaint.setTextSize(12);
        boldPaint.setColor(Color.parseColor("#2979FF"));
        canvas.drawText("2. DOCUMENT SUMMARY (TABLE 13)", MARGIN, y, boldPaint);
        y += 16;

        paint.setColor(Color.parseColor("#F4F6FC"));
        paint.setStyle(Paint.Style.FILL);
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 100, paint);

        textY = y + 22;
        paint.setColor(Color.parseColor("#1A1D2E"));
        drawKpiLine(canvas, paint, boldPaint, "Tax Invoices Issued:", String.valueOf(report.taxDocSummary.taxInvoicesCount), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "Cancelled Invoices:", String.valueOf(report.taxDocSummary.cancelledInvoicesCount), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "Credit Notes:", String.valueOf(report.taxDocSummary.creditNotesCount), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "Debit Notes:", String.valueOf(report.taxDocSummary.debitNotesCount), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "Net Documents Issued:", String.valueOf(report.taxDocSummary.netDocumentsCount), MARGIN + 14, textY);

        y += 120;

        // Reconciliation Box
        boldPaint.setTextSize(12);
        boldPaint.setColor(Color.parseColor("#2979FF"));
        canvas.drawText("3. AUDIT & RECONCILIATION CHECK", MARGIN, y, boldPaint);
        y += 16;

        paint.setColor(Color.parseColor("#F4F6FC"));
        canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 70, paint);

        textY = y + 22;
        paint.setColor(Color.parseColor("#1A1D2E"));
        drawKpiLine(canvas, paint, boldPaint, "Supplies Taxable Total:", "₹ " + report.reconciliation.expectedTaxable.toPlainString(), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "HSN Taxable Total:", "₹ " + report.reconciliation.hsnTaxable.toPlainString(), MARGIN + 14, textY);
        textY += 18;
        drawKpiLine(canvas, paint, boldPaint, "Reconciliation Status:", report.reconciliation.isMatched ? "MATCHED (Difference: ₹0.00)" : "MISMATCH (Diff: ₹" + report.reconciliation.difference.toPlainString() + ")", MARGIN + 14, textY);

        // Footer
        paint.setTextSize(8);
        paint.setColor(Color.parseColor("#8892B0"));
        canvas.drawText("Generated on " + new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(new Date()) + " via InventoryHub GST Engine", MARGIN, PAGE_HEIGHT - 20, paint);

        document.finishPage(page);

        // Page 2: B2B & B2C Summary Tables
        PdfDocument.PageInfo page2Info = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create();
        PdfDocument.Page page2 = document.startPage(page2Info);
        Canvas c2 = page2.getCanvas();

        y = MARGIN + 20;
        boldPaint.setTextSize(13);
        boldPaint.setColor(Color.parseColor("#2979FF"));
        c2.drawText("4. B2B SUPPLIES (TABLE 4)", MARGIN, y, boldPaint);
        y += 18;

        // Table Header
        paint.setColor(Color.parseColor("#2979FF"));
        paint.setStyle(Paint.Style.FILL);
        c2.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20, paint);

        boldPaint.setColor(Color.WHITE);
        boldPaint.setTextSize(8);
        c2.drawText("GSTIN / Customer", MARGIN + 4, y + 14, boldPaint);
        c2.drawText("POS", MARGIN + 140, y + 14, boldPaint);
        c2.drawText("Rate", MARGIN + 200, y + 14, boldPaint);
        c2.drawText("Taxable", MARGIN + 250, y + 14, boldPaint);
        c2.drawText("CGST+SGST", MARGIN + 330, y + 14, boldPaint);
        c2.drawText("IGST", MARGIN + 410, y + 14, boldPaint);
        c2.drawText("Total Tax", MARGIN + 460, y + 14, boldPaint);
        y += 24;

        paint.setTextSize(8);
        paint.setColor(Color.parseColor("#1A1D2E"));

        int b2bCount = 0;
        for (GSTR1Models.B2BRow row : report.b2bList) {
            if (b2bCount++ > 15) break; // Keep compact
            String name = row.customerGstin + " (" + truncate(row.customerName, 12) + ")";
            c2.drawText(name, MARGIN + 4, y + 10, paint);
            c2.drawText(truncate(row.placeOfSupply, 10), MARGIN + 140, y + 10, paint);
            c2.drawText(row.taxRate + "%", MARGIN + 200, y + 10, paint);
            c2.drawText("₹" + row.taxableValue.toPlainString(), MARGIN + 250, y + 10, paint);
            c2.drawText("₹" + row.cgst.add(row.sgst).toPlainString(), MARGIN + 330, y + 10, paint);
            c2.drawText("₹" + row.igst.toPlainString(), MARGIN + 410, y + 10, paint);
            c2.drawText("₹" + row.totalTax.toPlainString(), MARGIN + 460, y + 10, paint);
            y += 16;
        }
        if (report.b2bList.isEmpty()) {
            c2.drawText("No B2B taxable supplies in this period.", MARGIN + 4, y + 10, paint);
            y += 16;
        }

        y += 24;
        boldPaint.setTextSize(13);
        boldPaint.setColor(Color.parseColor("#2979FF"));
        c2.drawText("5. B2C SUPPLIES (TABLE 5 & 7)", MARGIN, y, boldPaint);
        y += 18;

        paint.setColor(Color.parseColor("#2979FF"));
        paint.setStyle(Paint.Style.FILL);
        c2.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 20, paint);

        boldPaint.setColor(Color.WHITE);
        boldPaint.setTextSize(8);
        c2.drawText("Category", MARGIN + 4, y + 14, boldPaint);
        c2.drawText("Place of Supply", MARGIN + 140, y + 14, boldPaint);
        c2.drawText("Rate", MARGIN + 220, y + 14, boldPaint);
        c2.drawText("Invoices", MARGIN + 270, y + 14, boldPaint);
        c2.drawText("Taxable", MARGIN + 330, y + 14, boldPaint);
        c2.drawText("Total Tax", MARGIN + 420, y + 14, boldPaint);
        y += 24;

        paint.setTextSize(8);
        paint.setColor(Color.parseColor("#1A1D2E"));

        int b2cCount = 0;
        for (GSTR1Models.B2CRow row : report.b2cOthersList) {
            if (b2cCount++ > 10) break;
            c2.drawText(row.supplyCategory, MARGIN + 4, y + 10, paint);
            c2.drawText(truncate(row.placeOfSupply, 12), MARGIN + 140, y + 10, paint);
            c2.drawText(row.taxRate + "%", MARGIN + 220, y + 10, paint);
            c2.drawText(String.valueOf(row.invoiceCount), MARGIN + 270, y + 10, paint);
            c2.drawText("₹" + row.taxableValue.toPlainString(), MARGIN + 330, y + 10, paint);
            c2.drawText("₹" + row.totalTax.toPlainString(), MARGIN + 420, y + 10, paint);
            y += 16;
        }
        for (GSTR1Models.B2CRow row : report.b2cLargeList) {
            c2.drawText(row.supplyCategory, MARGIN + 4, y + 10, paint);
            c2.drawText(truncate(row.placeOfSupply, 12), MARGIN + 140, y + 10, paint);
            c2.drawText(row.taxRate + "%", MARGIN + 220, y + 10, paint);
            c2.drawText(String.valueOf(row.invoiceCount), MARGIN + 270, y + 10, paint);
            c2.drawText("₹" + row.taxableValue.toPlainString(), MARGIN + 330, y + 10, paint);
            c2.drawText("₹" + row.totalTax.toPlainString(), MARGIN + 420, y + 10, paint);
            y += 16;
        }

        // Footer
        paint.setTextSize(8);
        paint.setColor(Color.parseColor("#8892B0"));
        c2.drawText("Page 2 of 2 | InventoryHub GSTR-1 Review Report", MARGIN, PAGE_HEIGHT - 20, paint);

        document.finishPage(page2);

        // Write PDF file
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "GSTR1_Report_" + timestamp + ".pdf";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        if (!dir.exists()) dir.mkdirs();

        File outputFile = new File(dir, fileName);
        try (FileOutputStream out = new FileOutputStream(outputFile)) {
            document.writeTo(out);
        } finally {
            document.close();
        }

        return outputFile;
    }

    private static void drawKpiLine(Canvas canvas, Paint labelPaint, Paint valPaint, String label, String val, int x, int y) {
        valPaint.setTextSize(10);
        valPaint.setColor(Color.parseColor("#1A1D2E"));
        canvas.drawText(label, x, y, labelPaint);
        canvas.drawText(val, x + 200, y, valPaint);
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 1) + "…";
    }
}
