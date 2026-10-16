package in.gbtsolutions.inventoryhub.reports.audittrail;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class AuditTrailReportModels {

    public static class AuditTrailRow {
        public long id;
        public String actionType = "";
        public String module = "";
        public String recordId = "";
        public String details = "";
        public String performedBy = "";
        public long timestamp;
        public String formattedDate = "";
        public String formattedTime = "";
        public String formattedDateTime = "";
    }

    public static class AuditTrailSummary {
        public int totalEvents = 0;
        public int creates = 0;
        public int updates = 0;
        public int deletes = 0;
        public int receives = 0;
        public int cancels = 0;
        public int uniqueUsers = 0;
        public int uniqueModules = 0;
        public String firstEventAt = "";
        public String lastEventAt = "";
    }

    public static class AuditTrailModuleBreakdown {
        public String module = "";
        public int count = 0;
        public int creates = 0;
        public int updates = 0;
        public int deletes = 0;
        public int receives = 0;
        public int cancels = 0;
    }

    public static class ReportData {
        public String companyName = "InventoryHub";
        public String companyGst = "";
        public String companyPhone = "";
        public String companyAddress = "";
        public String startDate = "";
        public String endDate = "";
        public AuditTrailSummary summary = new AuditTrailSummary();
        public List<AuditTrailModuleBreakdown> moduleBreakdown = new ArrayList<>();
        public List<AuditTrailRow> rows = new ArrayList<>();

        @SerializedName(value = "page", alternate = {"currentPage", "current_page"})
        public int page = 1;

        @SerializedName(value = "limit", alternate = {"pageSize", "perPage", "page_size", "per_page"})
        public int limit = 20;

        @SerializedName(value = "totalRecords", alternate = {"total", "totalCount", "total_records", "count"})
        public int totalRecords = 0;

        @SerializedName(value = "totalPages", alternate = {"pages", "total_pages"})
        public int totalPages = 1;

        @SerializedName(value = "hasMore", alternate = {"hasNextPage", "hasNext", "has_more"})
        public boolean hasMore = false;

        public void ensureNonNull() {
            if (companyName == null) companyName = "InventoryHub";
            if (companyGst == null) companyGst = "";
            if (companyPhone == null) companyPhone = "";
            if (companyAddress == null) companyAddress = "";
            if (startDate == null) startDate = "";
            if (endDate == null) endDate = "";
            if (summary == null) summary = new AuditTrailSummary();
            if (moduleBreakdown == null) moduleBreakdown = new ArrayList<>();
            if (rows == null) rows = new ArrayList<>();
            if (totalPages <= 0 && limit > 0 && totalRecords > 0) {
                totalPages = (int) Math.ceil((double) totalRecords / limit);
            }
            if (totalPages <= 0) totalPages = 1;
        }
    }
}
