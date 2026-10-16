package in.gbtsolutions.inventoryhub.online.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import in.gbtsolutions.inventoryhub.online.api.NetworkErrorHelper;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.online.models.OnlineDeviceDashboardDto;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnlineDashboardRepository {

    public interface DashboardCallback {
        void onSuccess(OnlineDeviceDashboardDto dashboardData);

        void onError(String errorMessage, boolean sessionExpired);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private Call<ApiResponse<OnlineDeviceDashboardDto>> currentCall;

    public OnlineDashboardRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    private OnlineApiService getApi() {
        return OnlineApiClient.getInstance().getApiService(context);
    }

    public synchronized void fetchDashboardStats(DashboardCallback callback) {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }

        currentCall = getApi().getDeviceDashboardStats();
        currentCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineDeviceDashboardDto>> call, Response<ApiResponse<OnlineDeviceDashboardDto>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineDeviceDashboardDto> apiResp = response.body();
                    if (apiResp.isSuccess() && apiResp.getData() != null) {
                        OnlineDeviceDashboardDto data = apiResp.getData();
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onSuccess(data);
                            }
                        });
                        return;
                    } else {
                        String errMsg = apiResp.getMessage();
                        boolean sessionExpired = apiResp.isSessionExpired();
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onError(errMsg != null && !errMsg.isEmpty() ? errMsg : "Failed to load dashboard data", sessionExpired);
                            }
                        });
                        return;
                    }
                }

                // Handle HTTP errors
                String defaultMsg = "Failed to load dashboard data (" + response.code() + ")";
                String msg = defaultMsg;
                boolean sessionExpired = response.code() == 401;

                try {
                    if (response.errorBody() != null) {
                        ApiResponse<?> err = gson.fromJson(response.errorBody().string(), ApiResponse.class);
                        if (err != null) {
                            if (err.getMessage() != null && !err.getMessage().isEmpty()) {
                                msg = err.getMessage();
                            }
                            if (err.isSessionExpired()) {
                                sessionExpired = true;
                            }
                        }
                    }
                } catch (Exception ignored) {
                }

                final String finalMsg = msg;
                final boolean finalSessionExpired = sessionExpired;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(finalMsg, finalSessionExpired);
                    }
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineDeviceDashboardDto>> call, Throwable t) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(NetworkErrorHelper.getErrorMessage(t), false);
                    }
                });
            }
        });
    }

    public synchronized void cancel() {
        if (currentCall != null && !currentCall.isCanceled()) {
            currentCall.cancel();
        }
    }
}
