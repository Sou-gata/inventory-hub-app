package in.gbtsolutions.inventoryhub.reports.audittrail;

import android.content.Context;
import android.text.TextUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.models.AuditTrail;
import in.gbtsolutions.inventoryhub.models.Config;

public class AuditTrailCalculator {

    public static AuditTrailReportModels.ReportData generateReport(Context context, List<AuditTrail> items, String startDate, String endDate) {
        AuditTrailReportModels.ReportData report = new AuditTrailReportModels.ReportData();

        // 1. Company Configuration
        try {
            Database db = Database.getInstance(context);
            List<Config> configs = db.configDao().getAllConfigsSync();
            Map<String, String> configMap = new HashMap<>();
            if (configs != null) {
                for (Config c : configs) {
                    configMap.put(c.getConfigKey(), c.getConfigValue());
                }
            }
            report.companyName = configMap.getOrDefault("company_name", "InventoryHub");
            report.companyGst = configMap.getOrDefault("gst_number", "");
            if (TextUtils.isEmpty(report.companyGst)) {
                report.companyGst = configMap.getOrDefault("company_gst", "");
            }
            report.companyPhone = configMap.getOrDefault("company_phone", "");
            report.companyAddress = configMap.getOrDefault("company_address", "");
        } catch (Exception e) {
            report.companyName = "InventoryHub";
        }

        if (items == null) {
            items = new ArrayList<>();
        }

        int creates = 0;
        int updates = 0;
        int deletes = 0;
        int receives = 0;
        int cancels = 0;

        Set<String> uniqueUsers = new HashSet<>();
        Set<String> uniqueModules = new HashSet<>();
        Map<String, AuditTrailReportModels.AuditTrailModuleBreakdown> moduleMap = new LinkedHashMap<>();

        long minTimestamp = Long.MAX_VALUE;
        long maxTimestamp = Long.MIN_VALUE;

        SimpleDateFormat dateOnlyFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat timeOnlyFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());

        for (AuditTrail item : items) {
            if (item == null) continue;

            String action = item.actionType != null ? item.actionType.trim().toUpperCase(Locale.getDefault()) : "UNKNOWN";
            String module = item.module != null ? item.module.trim() : "General";
            String user = item.performedBy != null && !item.performedBy.trim().isEmpty() ? item.performedBy.trim() : "System";

            if (AuditTrail.ACTION_ADD.equalsIgnoreCase(action) || "CREATE".equalsIgnoreCase(action)) {
                creates++;
            } else if (AuditTrail.ACTION_EDIT.equalsIgnoreCase(action) || "UPDATE".equalsIgnoreCase(action)) {
                updates++;
            } else if (AuditTrail.ACTION_DELETE.equalsIgnoreCase(action)) {
                deletes++;
            } else if ("RECEIVE".equalsIgnoreCase(action)) {
                receives++;
            } else if ("CANCEL".equalsIgnoreCase(action)) {
                cancels++;
            }

            uniqueUsers.add(user);
            uniqueModules.add(module);

            if (!moduleMap.containsKey(module)) {
                AuditTrailReportModels.AuditTrailModuleBreakdown mb = new AuditTrailReportModels.AuditTrailModuleBreakdown();
                mb.module = module;
                moduleMap.put(module, mb);
            }
            AuditTrailReportModels.AuditTrailModuleBreakdown mb = moduleMap.get(module);
            if (mb != null) {
                mb.count++;
                if (AuditTrail.ACTION_ADD.equalsIgnoreCase(action) || "CREATE".equalsIgnoreCase(action)) {
                    mb.creates++;
                } else if (AuditTrail.ACTION_EDIT.equalsIgnoreCase(action) || "UPDATE".equalsIgnoreCase(action)) {
                    mb.updates++;
                } else if (AuditTrail.ACTION_DELETE.equalsIgnoreCase(action)) {
                    mb.deletes++;
                } else if ("RECEIVE".equalsIgnoreCase(action)) {
                    mb.receives++;
                } else if ("CANCEL".equalsIgnoreCase(action)) {
                    mb.cancels++;
                }
            }

            if (item.timestamp > 0) {
                if (item.timestamp < minTimestamp) minTimestamp = item.timestamp;
                if (item.timestamp > maxTimestamp) maxTimestamp = item.timestamp;
            }

            AuditTrailReportModels.AuditTrailRow row = new AuditTrailReportModels.AuditTrailRow();
            row.id = item.id;
            row.actionType = action;
            row.module = module;
            row.recordId = item.recordId != null ? item.recordId : "";
            row.details = item.details != null ? item.details : "";
            row.performedBy = user;
            row.timestamp = item.timestamp;

            if (item.timestamp > 0) {
                Date d = new Date(item.timestamp);
                row.formattedDate = dateOnlyFormat.format(d);
                row.formattedTime = timeOnlyFormat.format(d);
                row.formattedDateTime = dateTimeFormat.format(d);
            } else {
                row.formattedDate = item.getDateOnly();
                row.formattedTime = item.getTimeOnly();
                row.formattedDateTime = item.getFormattedDate();
            }

            report.rows.add(row);
        }

        // Summary
        report.summary.totalEvents = report.rows.size();
        report.summary.creates = creates;
        report.summary.updates = updates;
        report.summary.deletes = deletes;
        report.summary.receives = receives;
        report.summary.cancels = cancels;
        report.summary.uniqueUsers = uniqueUsers.size();
        report.summary.uniqueModules = uniqueModules.size();

        if (minTimestamp != Long.MAX_VALUE) {
            report.summary.firstEventAt = dateOnlyFormat.format(new Date(minTimestamp));
        }
        if (maxTimestamp != Long.MIN_VALUE) {
            report.summary.lastEventAt = dateOnlyFormat.format(new Date(maxTimestamp));
        }

        // Module Breakdown list
        List<AuditTrailReportModels.AuditTrailModuleBreakdown> breakdownList = new ArrayList<>(moduleMap.values());
        breakdownList.sort((a, b) -> Integer.compare(b.count, a.count));
        report.moduleBreakdown = breakdownList;

        // Period
        if (!TextUtils.isEmpty(startDate) && !TextUtils.isEmpty(endDate)) {
            report.startDate = startDate;
            report.endDate = endDate;
        } else if (minTimestamp != Long.MAX_VALUE && maxTimestamp != Long.MIN_VALUE) {
            report.startDate = report.summary.firstEventAt;
            report.endDate = report.summary.lastEventAt;
        } else {
            report.startDate = "Start";
            report.endDate = "End";
        }

        return report;
    }
}
