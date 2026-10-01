package in.gbtsolutions.inventoryhub.online.paging;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

public abstract class EndlessRecyclerScrollListener extends RecyclerView.OnScrollListener {

    public static final int PAGE_SIZE = 20;
    public static final int DEFAULT_PREFETCH_DISTANCE = 4;

    private int visibleThreshold = DEFAULT_PREFETCH_DISTANCE;
    private int currentPage = 1;
    private int previousTotalItemCount = 0;
    private boolean loading = false;
    private boolean hasMore = true;
    private final int startingPageIndex = 1;

    private final RecyclerView.LayoutManager layoutManager;

    public EndlessRecyclerScrollListener(LinearLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
        this.visibleThreshold = DEFAULT_PREFETCH_DISTANCE;
    }

    public EndlessRecyclerScrollListener(GridLayoutManager layoutManager) {
        this.layoutManager = layoutManager;
        this.visibleThreshold = DEFAULT_PREFETCH_DISTANCE * layoutManager.getSpanCount();
    }

    public EndlessRecyclerScrollListener(RecyclerView.LayoutManager layoutManager, int prefetchDistance) {
        this.layoutManager = layoutManager;
        this.visibleThreshold = prefetchDistance;
    }

    public int getLastVisibleItem(int[] lastVisibleItemPositions) {
        int maxSize = 0;
        for (int i = 0; i < lastVisibleItemPositions.length; i++) {
            if (i == 0) {
                maxSize = lastVisibleItemPositions[i];
            } else if (lastVisibleItemPositions[i] > maxSize) {
                maxSize = lastVisibleItemPositions[i];
            }
        }
        return maxSize;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView view, int dx, int dy) {
        if (dy <= 0) return;

        int totalItemCount = layoutManager.getItemCount();
        int lastVisibleItemPosition = 0;

        if (layoutManager instanceof StaggeredGridLayoutManager) {
            int[] lastVisibleItemPositions = ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(null);
            lastVisibleItemPosition = getLastVisibleItem(lastVisibleItemPositions);
        } else if (layoutManager instanceof GridLayoutManager) {
            lastVisibleItemPosition = ((GridLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof LinearLayoutManager) {
            lastVisibleItemPosition = ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        }

        if (totalItemCount < previousTotalItemCount) {
            this.currentPage = this.startingPageIndex;
            this.previousTotalItemCount = totalItemCount;
            if (totalItemCount == 0) {
                this.loading = true;
            }
        }

        if (loading && (totalItemCount > previousTotalItemCount)) {
            loading = false;
            previousTotalItemCount = totalItemCount;
        }

        if (!loading && hasMore && (lastVisibleItemPosition + visibleThreshold) >= totalItemCount) {
            currentPage++;
            loading = true;
            onLoadMore(currentPage, totalItemCount, view);
        }
    }

    public void resetState() {
        this.currentPage = this.startingPageIndex;
        this.previousTotalItemCount = 0;
        this.loading = false;
        this.hasMore = true;
    }

    public void setLoading(boolean loading) {
        this.loading = loading;
    }

    public boolean isLoading() {
        return loading;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int page) {
        this.currentPage = page;
    }

    public abstract void onLoadMore(int page, int totalItemsCount, RecyclerView view);
}
