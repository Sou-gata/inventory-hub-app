package in.gbtsolutions.inventoryhub.online.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.UnitOfMeasure;
import in.gbtsolutions.inventoryhub.online.api.NetworkErrorHelper;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.online.models.OnlineUnitOfMeasureDto;
import in.gbtsolutions.inventoryhub.online.models.PagedData;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnlineUnitOfMeasureRepository {

    public interface PagedUnitCallback {
        void onSuccess(List<UnitOfMeasure> units, int currentPage, int totalPages, int totalRecords, boolean hasMore);

        void onError(String errorMessage, boolean sessionExpired);
    }

    public interface UnitListCallback {
        void onSuccess(List<UnitOfMeasure> units);

        void onError(String errorMessage);
    }

    public interface UnitActionCallback {
        void onSuccess(UnitOfMeasure unit);

        void onError(String errorMessage);
    }

    public interface UnitDeleteCallback {
        void onSuccess();

        void onError(String errorMessage);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private Call<ApiResponse<PagedData<OnlineUnitOfMeasureDto>>> currentPagedCall;
    private Call<ApiResponse<List<OnlineUnitOfMeasureDto>>> currentListCall;

    public OnlineUnitOfMeasureRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    private OnlineApiService getApi() {
        return OnlineApiClient.getInstance().getApiService(context);
    }

    /**
     * Fetch paginated units of measure from backend (used when viewing units list in UnitOfMeasureActivity)
     */
    public synchronized void fetchUnitsPaged(int page, int limit, String search, PagedUnitCallback callback) {
        if (currentPagedCall != null && !currentPagedCall.isCanceled()) {
            currentPagedCall.cancel();
        }

        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;

        currentPagedCall = getApi().getPaginatedUnits(page, limit, query, null, "is_default", "desc");
        currentPagedCall.enqueue(new Callback<ApiResponse<PagedData<OnlineUnitOfMeasureDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PagedData<OnlineUnitOfMeasureDto>>> call, Response<ApiResponse<PagedData<OnlineUnitOfMeasureDto>>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PagedData<OnlineUnitOfMeasureDto>> apiResp = response.body();
                    if (apiResp.isSuccess() && apiResp.getData() != null) {
                        PagedData<OnlineUnitOfMeasureDto> pagedData = apiResp.getData();
                        List<UnitOfMeasure> domainUnits = OnlineUnitOfMeasureDto.toDomainList(pagedData.getItems());
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onSuccess(domainUnits, pagedData.getPage(), pagedData.getTotalPages(), pagedData.getTotalRecords(), pagedData.hasMore());
                            }
                        });
                        return;
                    } else {
                        String errMsg = apiResp.getMessage();
                        boolean sessionExpired = apiResp.isSessionExpired();
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onError(errMsg != null && !errMsg.isEmpty() ? errMsg : "Failed to load units", sessionExpired);
                            }
                        });
                        return;
                    }
                }

                handlePagedError(response, callback);
            }

            @Override
            public void onFailure(Call<ApiResponse<PagedData<OnlineUnitOfMeasureDto>>> call, Throwable t) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(NetworkErrorHelper.getErrorMessage(t), false);
                    }
                });
            }
        });
    }

    /**
     * Fetch all units of measure from backend (used when adding product in AddProductActivity)
     */
    public synchronized void fetchAllUnits(UnitListCallback callback) {
        if (currentListCall != null && !currentListCall.isCanceled()) {
            currentListCall.cancel();
        }

        currentListCall = getApi().getAllUnits();
        currentListCall.enqueue(new Callback<ApiResponse<List<OnlineUnitOfMeasureDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<OnlineUnitOfMeasureDto>>> call, Response<ApiResponse<List<OnlineUnitOfMeasureDto>>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<OnlineUnitOfMeasureDto>> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        List<UnitOfMeasure> domainUnits = OnlineUnitOfMeasureDto.toDomainList(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(domainUnits);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null)
                                callback.onError(msg != null && !msg.isEmpty() ? msg : "Failed to load units of measure");
                        });
                        return;
                    }
                }
                handleListError(response, callback);
            }

            @Override
            public void onFailure(Call<ApiResponse<List<OnlineUnitOfMeasureDto>>> call, Throwable t) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(NetworkErrorHelper.getErrorMessage(t));
                    }
                });
            }
        });
    }

    public void createUnit(UnitOfMeasure uom, UnitActionCallback callback) {
        OnlineUnitOfMeasureDto dto = OnlineUnitOfMeasureDto.toDto(uom);
        getApi().createUnit(dto).enqueue(new Callback<ApiResponse<OnlineUnitOfMeasureDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineUnitOfMeasureDto>> call, Response<ApiResponse<OnlineUnitOfMeasureDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineUnitOfMeasureDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        UnitOfMeasure created = body.getData().toDomain();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(created);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null)
                                callback.onError(msg != null && !msg.isEmpty() ? msg : "Failed to create unit of measure");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to create unit of measure");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineUnitOfMeasureDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null)
                        callback.onError(NetworkErrorHelper.getErrorMessage(t));
                });
            }
        });
    }

    public void updateUnit(UnitOfMeasure uom, UnitActionCallback callback) {
        OnlineUnitOfMeasureDto dto = OnlineUnitOfMeasureDto.toDto(uom);
        getApi().updateUnit(uom.uomId, dto).enqueue(new Callback<ApiResponse<OnlineUnitOfMeasureDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineUnitOfMeasureDto>> call, Response<ApiResponse<OnlineUnitOfMeasureDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineUnitOfMeasureDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        UnitOfMeasure updated = body.getData().toDomain();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(updated);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null)
                                callback.onError(msg != null && !msg.isEmpty() ? msg : "Failed to update unit of measure");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to update unit of measure");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineUnitOfMeasureDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null)
                        callback.onError(NetworkErrorHelper.getErrorMessage(t));
                });
            }
        });
    }

    public void deleteUnit(int uomId, UnitDeleteCallback callback) {
        getApi().deleteUnit(uomId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess();
                    });
                    return;
                }
                handleDeleteError(response, callback, "Failed to delete unit of measure");
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

    public synchronized void cancel() {
        if (currentPagedCall != null && !currentPagedCall.isCanceled()) {
            currentPagedCall.cancel();
        }
        if (currentListCall != null && !currentListCall.isCanceled()) {
            currentListCall.cancel();
        }
    }

    private void handlePagedError(Response<?> response, PagedUnitCallback callback) {
        String errorMessage = "Server error (" + response.code() + ")";
        boolean sessionExpired = response.code() == 401;
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                ApiResponse<?> errResp = gson.fromJson(errorJson, ApiResponse.class);
                if (errResp != null) {
                    if (errResp.getMessage() != null && !errResp.getMessage().isEmpty()) {
                        errorMessage = errResp.getMessage();
                    }
                    if (errResp.isSessionExpired()) {
                        sessionExpired = true;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        final String finalMsg = errorMessage;
        final boolean finalSessionExpired = sessionExpired;
        mainHandler.post(() -> {
            if (callback != null) {
                callback.onError(finalMsg, finalSessionExpired);
            }
        });
    }

    private void handleListError(Response<?> response, UnitListCallback callback) {
        String errorMessage = "Server error (" + response.code() + ")";
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                ApiResponse<?> errResp = gson.fromJson(errorJson, ApiResponse.class);
                if (errResp != null && errResp.getMessage() != null && !errResp.getMessage().isEmpty()) {
                    errorMessage = errResp.getMessage();
                }
            }
        } catch (Exception ignored) {
        }
        final String finalMsg = errorMessage;
        mainHandler.post(() -> {
            if (callback != null) callback.onError(finalMsg);
        });
    }

    private void handleActionError(Response<?> response, UnitActionCallback callback, String defaultMsg) {
        String errorMessage = defaultMsg + " (" + response.code() + ")";
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                ApiResponse<?> errResp = gson.fromJson(errorJson, ApiResponse.class);
                if (errResp != null && errResp.getMessage() != null && !errResp.getMessage().isEmpty()) {
                    errorMessage = errResp.getMessage();
                }
            }
        } catch (Exception ignored) {
        }
        final String finalMsg = errorMessage;
        mainHandler.post(() -> {
            if (callback != null) callback.onError(finalMsg);
        });
    }

    private void handleDeleteError(Response<?> response, UnitDeleteCallback callback, String defaultMsg) {
        String errorMessage = defaultMsg + " (" + response.code() + ")";
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                ApiResponse<?> errResp = gson.fromJson(errorJson, ApiResponse.class);
                if (errResp != null && errResp.getMessage() != null && !errResp.getMessage().isEmpty()) {
                    errorMessage = errResp.getMessage();
                }
            }
        } catch (Exception ignored) {
        }
        final String finalMsg = errorMessage;
        mainHandler.post(() -> {
            if (callback != null) callback.onError(finalMsg);
        });
    }
}
