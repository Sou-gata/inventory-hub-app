package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic container for paginated server responses inside ApiResponse.data
 */
public class PagedData<T> {

    @SerializedName(value = "items", alternate = {"products", "rows", "records", "list"})
    private List<T> items;

    @SerializedName(value = "page", alternate = {"currentPage", "current_page"})
    private int page = 1;

    @SerializedName(value = "limit", alternate = {"pageSize", "perPage", "page_size", "per_page"})
    private int limit = 30;

    @SerializedName(value = "totalRecords", alternate = {"total", "totalCount", "total_records", "count"})
    private int totalRecords = 0;

    @SerializedName(value = "totalPages", alternate = {"pages", "total_pages"})
    private int totalPages = 1;

    @SerializedName(value = "hasMore", alternate = {"hasNextPage", "hasNext", "has_more"})
    private Boolean hasMore;

    public List<T> getItems() {
        return items != null ? items : new ArrayList<>();
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getTotalPages() {
        if (totalPages <= 0 && limit > 0 && totalRecords > 0) {
            return (int) Math.ceil((double) totalRecords / limit);
        }
        return totalPages > 0 ? totalPages : 1;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean hasMore() {
        if (hasMore != null) {
            return hasMore;
        }
        return page < getTotalPages();
    }

    public void setHasMore(Boolean hasMore) {
        this.hasMore = hasMore;
    }
}
