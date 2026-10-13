package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class OnlineDeviceDashboardDto {

    // Valuation & Stock KPIs
    @SerializedName(value = "totalValuation", alternate = {"total_valuation", "valuation"})
    public double totalValuation;

    @SerializedName(value = "totalUnits", alternate = {"total_units", "units"})
    public double totalUnits;

    @SerializedName(value = "totalProducts", alternate = {"total_products", "products_count", "productsCount"})
    public int totalProducts;

    // Sales KPIs
    @SerializedName(value = "totalRevenue", alternate = {"total_revenue", "revenue"})
    public double totalRevenue;

    @SerializedName(value = "totalSalesCount", alternate = {"total_sales_count", "sales_count", "active_sales_count", "salesCount"})
    public int totalSalesCount;

    // Critical Alerts / Low Stock KPI
    @SerializedName(value = "lowStockCount", alternate = {"low_stock_count", "reorder_count", "reorderCount"})
    public int lowStockCount;

    // Pending Purchase Orders KPI
    @SerializedName(value = "pendingPurchasesCount", alternate = {"pending_purchases_count", "pending_receives_count", "pendingReceivesCount"})
    public int pendingPurchasesCount;

    // Directory Totals
    @SerializedName(value = "totalCategories", alternate = {"total_categories", "categories_count", "categoriesCount"})
    public int totalCategories;

    @SerializedName(value = "totalBuyers", alternate = {"total_buyers", "buyers_count", "buyersCount"})
    public int totalBuyers;

    @SerializedName(value = "totalSuppliers", alternate = {"total_suppliers", "suppliers_count", "suppliersCount"})
    public int totalSuppliers;

    // Detailed Lists for Activity Hub & Alerts Watchlist
    @SerializedName(value = "lowStockProducts", alternate = {"low_stock_items", "lowStockItems", "low_stock_products"})
    public List<OnlineProductDto> lowStockProducts = new ArrayList<>();

    @SerializedName(value = "recentSales", alternate = {"recent_sales", "sales"})
    public List<OnlineSaleDto> recentSales = new ArrayList<>();

    @SerializedName(value = "recentPurchases", alternate = {"recent_purchases", "purchases"})
    public List<OnlinePurchaseDto> recentPurchases = new ArrayList<>();

    @SerializedName(value = "categories", alternate = {"category_list", "categoryList"})
    public List<OnlineCategoryDto> categories = new ArrayList<>();

    public OnlineDeviceDashboardDto() {
    }
}
