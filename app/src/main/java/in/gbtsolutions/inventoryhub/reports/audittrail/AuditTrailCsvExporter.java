package in.gbtsolutions.inventoryhub.reports.audittrail;

import android.content.Context;
import android.os.Environment;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AuditTrailCsvExporter {

    public static File exportToCsv(Context context, AuditTrailReportModels.ReportData report) throws IOException {
        report.ensureNonNull();

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "Audit_Trail_" + timestamp + ".csv";

        File dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (dir == null) dir = context.getFilesDir();
        if (!dir.exists()) dir.mkdirs();

        File outputFile = new File(dir, fileName);

        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            // Header Info
            writer.writeNext(new String[]{"AUDIT TRAIL REPORT", report.companyName});
            writer.writeNext(new String[]{"GSTIN", report.companyGst != null ? report.companyGst : ""});
            writer.writeNext(new String[]{"Period", (report.startDate != null ? report.startDate : "") + " to " + (report.endDate != null ? report.endDate : "")});
            writer.writeNext(new String[]{""});

            // Table Data
            writer.writeNext(new String[]{"ID", "Action", "Module", "Record ID", "Performed By", "Date & Time", "Details"});

            for (AuditTrailReportModels.AuditTrailRow row : report.rows) {
                writer.writeNext(new String[]{String.valueOf(row.id), row.actionType != null ? row.actionType : "", row.module != null ? row.module : "", row.recordId != null ? row.recordId : "", row.performedBy != null ? row.performedBy : "", row.formattedDateTime != null ? row.formattedDateTime : "", row.details != null ? row.details : ""});
            }
        }

        return outputFile;
    }
}
