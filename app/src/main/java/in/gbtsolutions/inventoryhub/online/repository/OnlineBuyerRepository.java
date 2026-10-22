package in.gbtsolutions.inventoryhub.online.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.Buyer;
import in.gbtsolutions.inventoryhub.online.api.NetworkErrorHelper;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.online.models.BuyerMapper;
import in.gbtsolutions.inventoryhub.online.models.OnlineBuyerDto;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import in.gbtsolutions.inventoryhub.online.models.PagedData;

public class OnlineBuyerRepository {

    public interface PagedBuyerCallback {
        void onSuccess(List<Buyer> buyers, int currentPage, int totalPages, int totalRecords, boolean hasMore);

        void onError(String errorMessage);
    }

    public interface BuyerListCallback {
        void onSuccess(List<Buyer> buyers);

        void onError(String errorMessage);
    }

    public interface BuyerActionCallback {
        void onSuccess(Buyer buyer);

        void onError(String errorMessage);
    }

    public interface BuyerDeleteCallback {
        void onSuccess();

        void onError(String errorMessage);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private Call<ApiResponse<PagedData<OnlineBuyerDto>>> currentBuyersCall;

    public OnlineBuyerRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    private OnlineApiService getApi() {
        return OnlineApiClient.getInstance().getApiService(context);
    }

    public void fetchBuyers(BuyerListCallback callback) {
        fetchBuyers(null, callback);
    }

    public void fetchBuyers(String search, BuyerListCallback callback) {
        fetchBuyersPaged(1, 100, search, null, new PagedBuyerCallback() {
            @Override
            public void onSuccess(List<Buyer> buyers, int currentPage, int totalPages, int totalRecords, boolean hasMore) {
                if (callback != null) {
                    callback.onSuccess(buyers);
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (callback != null) {
                    callback.onError(errorMessage);
                }
            }
        });
    }

    public synchronized void fetchBuyersPaged(int page, int limit, String search, String status, PagedBuyerCallback callback) {
        if (currentBuyersCall != null && !currentBuyersCall.isCanceled()) {
            currentBuyersCall.cancel();
        }

        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String statusParam = (status != null && !status.trim().isEmpty()) ? status.trim() : null;

        currentBuyersCall = getApi().getBuyers(page, limit, query, statusParam);
        currentBuyersCall.enqueue(new Callback<ApiResponse<PagedData<OnlineBuyerDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PagedData<OnlineBuyerDto>>> call, Response<ApiResponse<PagedData<OnlineBuyerDto>>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PagedData<OnlineBuyerDto>> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        PagedData<OnlineBuyerDto> pagedData = body.getData();
                        List<Buyer> domainBuyers = BuyerMapper.toDomainList(pagedData.getItems());
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onSuccess(domainBuyers, pagedData.getPage(), pagedData.getTotalPages(), pagedData.getTotalRecords(), pagedData.hasMore());
                            }
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onError(msg != null && !msg.isEmpty() ? msg : "Failed to load buyers");
                            }
                        });
                        return;
                    }
                }
                handlePagedError(response, callback, "Failed to load buyers from server");
            }

            @Override
            public void onFailure(Call<ApiResponse<PagedData<OnlineBuyerDto>>> call, Throwable t) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(NetworkErrorHelper.getErrorMessage(t));
                    }
                });
            }
        });
    }

    public void createBuyer(Buyer buyer, BuyerActionCallback callback) {
        OnlineBuyerDto dto = BuyerMapper.toDto(buyer);
        getApi().createBuyer(dto).enqueue(new Callback<ApiResponse<OnlineBuyerDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineBuyerDto>> call, Response<ApiResponse<OnlineBuyerDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineBuyerDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        Buyer created = BuyerMapper.toDomain(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(created);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null)
                                callback.onError(!msg.isEmpty() ? msg : "Failed to create buyer");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to create buyer");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineBuyerDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null)
                        callback.onError(NetworkErrorHelper.getErrorMessage(t));
                });
            }
        });
    }

    public void updateBuyer(Buyer buyer, BuyerActionCallback callback) {
        OnlineBuyerDto dto = BuyerMapper.toDto(buyer);
        getApi().updateBuyer(buyer.buyerId, dto).enqueue(new Callback<ApiResponse<OnlineBuyerDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineBuyerDto>> call, Response<ApiResponse<OnlineBuyerDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineBuyerDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        Buyer updated = BuyerMapper.toDomain(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(updated);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null)
                                callback.onError(!msg.isEmpty() ? msg : "Failed to update buyer");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to update buyer");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineBuyerDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null)
                        callback.onError(NetworkErrorHelper.getErrorMessage(t));
                });
            }
        });
    }

    public void deleteBuyer(int buyerId, BuyerDeleteCallback callback) {
        getApi().deleteBuyer(buyerId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess();
                    });
                } else {
                    String msg = "Failed to delete buyer";
                    try {
                        if (response.errorBody() != null) {
                            ApiResponse<?> err = gson.fromJson(response.errorBody().string(), ApiResponse.class);
                            if (err != null && err.getMessage() != null) msg = err.getMessage();
                        }
                    } catch (Exception ignored) {
                    }
                    final String finalMsg = msg;
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError(finalMsg);
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null)
                        callback.onError(NetworkErrorHelper.getErrorMessage(t));
                });
            }
        });
    }

    private void handlePagedError(Response<?> response, PagedBuyerCallback callback, String defaultMsg) {
        String msg = defaultMsg + " (" + response.code() + ")";
        try {
            if (response.errorBody() != null) {
                ApiResponse<?> err = gson.fromJson(response.errorBody().string(), ApiResponse.class);
                if (err != null && err.getMessage() != null && !err.getMessage().isEmpty()) {
                    msg = err.getMessage();
                }
            }
        } catch (Exception ignored) {
        }
        final String finalMsg = msg;
        mainHandler.post(() -> {
            if (callback != null) callback.onError(finalMsg);
        });
    }

    private void handleError(Response<?> response, BuyerListCallback callback, String defaultMsg) {
        String msg = defaultMsg + " (" + response.code() + ")";
        try {
            if (response.errorBody() != null) {
                ApiResponse<?> err = gson.fromJson(response.errorBody().string(), ApiResponse.class);
                if (err != null && err.getMessage() != null && !err.getMessage().isEmpty()) {
                    msg = err.getMessage();
                }
            }
        } catch (Exception ignored) {
        }
        final String finalMsg = msg;
        mainHandler.post(() -> {
            if (callback != null) callback.onError(finalMsg);
        });
    }

    private void handleActionError(Response<?> response, BuyerActionCallback callback, String defaultMsg) {
        String msg = defaultMsg + " (" + response.code() + ")";
        try {
            if (response.errorBody() != null) {
                ApiResponse<?> err = gson.fromJson(response.errorBody().string(), ApiResponse.class);
                if (err != null && err.getMessage() != null && !err.getMessage().isEmpty()) {
                    msg = err.getMessage();
                }
            }
        } catch (Exception ignored) {
        }
        final String finalMsg = msg;
        mainHandler.post(() -> {
            if (callback != null) callback.onError(finalMsg);
        });
    }
}
