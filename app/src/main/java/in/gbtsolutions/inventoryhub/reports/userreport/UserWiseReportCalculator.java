package in.gbtsolutions.inventoryhub.reports.userreport;

import android.content.Context;
import android.text.TextUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.helpers.UserHelper;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.User;

public class UserWiseReportCalculator {

    public static UserWiseReportModels.ReportData generateReport(Context context, String startDate, String endDate, long filterUserId) {
        Database db = Database.getInstance(context);
        UserWiseReportModels.ReportData report = new UserWiseReportModels.ReportData();
        report.startDate = startDate;
        report.endDate = endDate;
        report.filterUserId = filterUserId;

        // 1. Company Configuration
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

        // 2. Fetch Users
        List<User> allUsers = db.userDao().getAllUsersSync();
        if (allUsers == null) {
            allUsers = new ArrayList<>();
        }

        Map<Long, UserWiseReportModels.UserWiseReportRow> rowMap = new LinkedHashMap<>();

        // Pre-populate with all known registered users
        for (User u : allUsers) {
            if (u == null) continue;
            UserWiseReportModels.UserWiseReportRow row = new UserWiseReportModels.UserWiseReportRow();
            row.userId = u.id;
            row.userName = !TextUtils.isEmpty(u.name) ? u.name.trim() : ("User #" + u.id);
            row.username = u.username != null ? u.username.trim() : "";
            row.role = UserHelper.formatUserRole(u);
            row.contact = u.contact != null ? u.contact.trim() : "";
            row.email = u.email != null ? u.email.trim() : "";
            row.isActive = u.isActive;
            rowMap.put((long) u.id, row);
        }

        // Set filter display name
        if (filterUserId <= 0 && filterUserId != 0) {
            report.filterUserName = "All Users";
        } else if (filterUserId == 0) {
            report.filterUserName = "System / Unassigned";
        } else {
            UserWiseReportModels.UserWiseReportRow filterRow = rowMap.get(filterUserId);
            if (filterRow != null) {
                report.filterUserName = filterRow.userName + (!TextUtils.isEmpty(filterRow.username) ? " (@" + filterRow.username + ")" : "");
            } else {
                report.filterUserName = "User #" + filterUserId;
            }
        }

        // 3. Fetch Purchases and aggregate
        List<Purchase> purchases = db.purchaseDao().getPurchasesForExport(startDate, endDate);
        if (purchases != null) {
            for (Purchase purchase : purchases) {
                if (purchase == null || "Cancelled".equalsIgnoreCase(purchase.status)) {
                    continue;
                }

                long uid = purchase.createdBy;
                UserWiseReportModels.UserWiseReportRow row = getOrCreateRow(rowMap, uid);

                row.purchaseCount++;
                row.purchaseTaxable = row.purchaseTaxable.add(UserWiseReportModels.toMoney(purchase.subtotalAmount));
                row.purchaseCgst = row.purchaseCgst.add(UserWiseReportModels.toMoney(purchase.cgstAmount));
                row.purchaseSgst = row.purchaseSgst.add(UserWiseReportModels.toMoney(purchase.sgstAmount));
                row.purchaseIgst = row.purchaseIgst.add(UserWiseReportModels.toMoney(purchase.igstAmount));

                BigDecimal purGst = UserWiseReportModels.toMoney(purchase.totalGst);
                if (purGst.compareTo(BigDecimal.ZERO) == 0) {
                    purGst = UserWiseReportModels.toMoney(purchase.cgstAmount + purchase.sgstAmount + purchase.igstAmount);
                }
                row.purchaseTotalGst = row.purchaseTotalGst.add(purGst);
                row.purchaseGrossTotal = row.purchaseGrossTotal.add(UserWiseReportModels.toMoney(purchase.totalAmount));
            }
        }

        // 4. Fetch Sales and aggregate
        List<Sale> sales = db.saleDao().getSalesForExport(startDate, endDate);
        if (sales != null) {
            for (Sale sale : sales) {
                if (sale == null || "Cancelled".equalsIgnoreCase(sale.status)) {
                    continue;
                }

                long uid = sale.createdBy;
                UserWiseReportModels.UserWiseReportRow row = getOrCreateRow(rowMap, uid);

                row.salesCount++;
                row.salesTaxable = row.salesTaxable.add(UserWiseReportModels.toMoney(sale.subtotalAmount));
                row.salesCgst = row.salesCgst.add(UserWiseReportModels.toMoney(sale.cgstAmount));
                row.salesSgst = row.salesSgst.add(UserWiseReportModels.toMoney(sale.sgstAmount));
                row.salesIgst = row.salesIgst.add(UserWiseReportModels.toMoney(sale.igstAmount));
                row.salesCess = row.salesCess.add(UserWiseReportModels.toMoney(sale.cessAmount));

                BigDecimal saleGst = UserWiseReportModels.toMoney(sale.totalGst);
                if (saleGst.compareTo(BigDecimal.ZERO) == 0) {
                    saleGst = UserWiseReportModels.toMoney(sale.cgstAmount + sale.sgstAmount + sale.igstAmount + sale.cessAmount);
                }
                row.salesTotalGst = row.salesTotalGst.add(saleGst);
                row.salesGrossTotal = row.salesGrossTotal.add(UserWiseReportModels.toMoney(sale.totalAmount));
            }
        }

        // 5. Compute Row Totals & Collect rows
        List<UserWiseReportModels.UserWiseReportRow> allRows = new ArrayList<>();
        for (UserWiseReportModels.UserWiseReportRow row : rowMap.values()) {
            row.totalTransactions = row.salesCount + row.purchaseCount;
            row.totalCombinedGst = row.salesTotalGst.add(row.purchaseTotalGst);
            row.netGstLiability = row.salesTotalGst.subtract(row.purchaseTotalGst);
            row.netTurnover = row.salesGrossTotal.subtract(row.purchaseGrossTotal);

            // Apply User Filter if specified
            if (filterUserId >= 0) {
                if (row.userId == filterUserId) {
                    allRows.add(row);
                }
            } else {
                allRows.add(row);
            }
        }

        // 6. Sort Rows
        // Priority: Active users with transactions first (highest transactions & sales first),
        // then other users alphabetically by name.
        Collections.sort(allRows, (r1, r2) -> {
            if (r1.totalTransactions > 0 && r2.totalTransactions == 0) return -1;
            if (r1.totalTransactions == 0 && r2.totalTransactions > 0) return 1;
            if (r1.totalTransactions > 0 && r2.totalTransactions > 0) {
                int cmp = Integer.compare(r2.totalTransactions, r1.totalTransactions);
                if (cmp != 0) return cmp;
                return r2.salesGrossTotal.compareTo(r1.salesGrossTotal);
            }
            return r1.userName.compareToIgnoreCase(r2.userName);
        });

        report.rows = allRows;

        // 7. Compute Summary
        UserWiseReportModels.UserWiseReportSummary summary = report.summary;
        summary.totalUsersCount = allRows.size();

        for (UserWiseReportModels.UserWiseReportRow r : allRows) {
            if (r.totalTransactions > 0) {
                summary.activeUsersCount++;
            }
            summary.totalSalesCount += r.salesCount;
            summary.totalGrossSales = summary.totalGrossSales.add(r.salesGrossTotal);
            summary.totalSalesTaxable = summary.totalSalesTaxable.add(r.salesTaxable);
            summary.totalSalesCgst = summary.totalSalesCgst.add(r.salesCgst);
            summary.totalSalesSgst = summary.totalSalesSgst.add(r.salesSgst);
            summary.totalSalesIgst = summary.totalSalesIgst.add(r.salesIgst);
            summary.totalSalesCess = summary.totalSalesCess.add(r.salesCess);
            summary.totalSalesGst = summary.totalSalesGst.add(r.salesTotalGst);

            summary.totalPurchaseCount += r.purchaseCount;
            summary.totalGrossPurchases = summary.totalGrossPurchases.add(r.purchaseGrossTotal);
            summary.totalPurchaseTaxable = summary.totalPurchaseTaxable.add(r.purchaseTaxable);
            summary.totalPurchaseCgst = summary.totalPurchaseCgst.add(r.purchaseCgst);
            summary.totalPurchaseSgst = summary.totalPurchaseSgst.add(r.purchaseSgst);
            summary.totalPurchaseIgst = summary.totalPurchaseIgst.add(r.purchaseIgst);
            summary.totalPurchaseGst = summary.totalPurchaseGst.add(r.purchaseTotalGst);

            summary.totalCombinedGst = summary.totalCombinedGst.add(r.totalCombinedGst);
            summary.netGstLiability = summary.netGstLiability.add(r.netGstLiability);
            summary.netTurnover = summary.netTurnover.add(r.netTurnover);
            summary.totalTransactions += r.totalTransactions;
        }

        return report;
    }

    private static UserWiseReportModels.UserWiseReportRow getOrCreateRow(
            Map<Long, UserWiseReportModels.UserWiseReportRow> map, long userId) {
        UserWiseReportModels.UserWiseReportRow row = map.get(userId);
        if (row == null) {
            row = new UserWiseReportModels.UserWiseReportRow();
            row.userId = userId;
            if (userId <= 0) {
                row.userName = "System / Unassigned";
                row.username = "system";
                row.role = "SYSTEM";
            } else {
                row.userName = "User #" + userId;
                row.username = "user" + userId;
                row.role = "STAFF";
            }
            map.put(userId, row);
        }
        return row;
    }
}
