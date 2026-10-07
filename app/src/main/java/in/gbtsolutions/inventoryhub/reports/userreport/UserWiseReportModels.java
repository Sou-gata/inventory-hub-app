package in.gbtsolutions.inventoryhub.reports.userreport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class UserWiseReportModels {

    public static BigDecimal toMoney(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal roundMoney(BigDecimal val) {
        if (val == null) return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        return val.setScale(2, RoundingMode.HALF_UP);
    }

    public static class UserWiseReportRow {
        public long userId;
        public String userName = "";
        public String username = "";
        public String role = "";
        public String contact = "";
        public String email = "";
        public boolean isActive = true;

        // Sales Metrics (Output Tax & Turnover)
        public int salesCount = 0;
        public BigDecimal salesTaxable = BigDecimal.ZERO;
        public BigDecimal salesCgst = BigDecimal.ZERO;
        public BigDecimal salesSgst = BigDecimal.ZERO;
        public BigDecimal salesIgst = BigDecimal.ZERO;
        public BigDecimal salesCess = BigDecimal.ZERO;
        public BigDecimal salesTotalGst = BigDecimal.ZERO;
        public BigDecimal salesGrossTotal = BigDecimal.ZERO;

        // Purchase Metrics (Input Tax & Inflow)
        public int purchaseCount = 0;
        public BigDecimal purchaseTaxable = BigDecimal.ZERO;
        public BigDecimal purchaseCgst = BigDecimal.ZERO;
        public BigDecimal purchaseSgst = BigDecimal.ZERO;
        public BigDecimal purchaseIgst = BigDecimal.ZERO;
        public BigDecimal purchaseTotalGst = BigDecimal.ZERO;
        public BigDecimal purchaseGrossTotal = BigDecimal.ZERO;

        // Combined & Net Metrics
        public BigDecimal totalCombinedGst = BigDecimal.ZERO; // Sales GST + Purchase GST
        public BigDecimal netGstLiability = BigDecimal.ZERO;  // Output GST - Input GST
        public BigDecimal netTurnover = BigDecimal.ZERO;      // Sales Gross - Purchase Gross
        public int totalTransactions = 0;                     // salesCount + purchaseCount
    }

    public static class UserWiseReportSummary {
        public BigDecimal totalGrossSales = BigDecimal.ZERO;
        public BigDecimal totalSalesTaxable = BigDecimal.ZERO;
        public BigDecimal totalSalesCgst = BigDecimal.ZERO;
        public BigDecimal totalSalesSgst = BigDecimal.ZERO;
        public BigDecimal totalSalesIgst = BigDecimal.ZERO;
        public BigDecimal totalSalesCess = BigDecimal.ZERO;
        public BigDecimal totalSalesGst = BigDecimal.ZERO;

        public BigDecimal totalGrossPurchases = BigDecimal.ZERO;
        public BigDecimal totalPurchaseTaxable = BigDecimal.ZERO;
        public BigDecimal totalPurchaseCgst = BigDecimal.ZERO;
        public BigDecimal totalPurchaseSgst = BigDecimal.ZERO;
        public BigDecimal totalPurchaseIgst = BigDecimal.ZERO;
        public BigDecimal totalPurchaseGst = BigDecimal.ZERO;

        public BigDecimal totalCombinedGst = BigDecimal.ZERO;
        public BigDecimal netGstLiability = BigDecimal.ZERO;
        public BigDecimal netTurnover = BigDecimal.ZERO;

        public int totalSalesCount = 0;
        public int totalPurchaseCount = 0;
        public int totalTransactions = 0;
        public int totalUsersCount = 0;
        public int activeUsersCount = 0;
    }

    public static class ReportData {
        public String companyName = "";
        public String companyGst = "";
        public String companyPhone = "";
        public String companyAddress = "";
        public String startDate = "";
        public String endDate = "";
        public long filterUserId = -1; // -1 = All Users
        public String filterUserName = "All Users";
        public UserWiseReportSummary summary = new UserWiseReportSummary();
        public List<UserWiseReportRow> rows = new ArrayList<>();
    }
}
