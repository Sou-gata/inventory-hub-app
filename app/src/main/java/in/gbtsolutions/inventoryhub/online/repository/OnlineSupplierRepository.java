package in.gbtsolutions.inventoryhub.online.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.Suppliers;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.online.models.OnlineSupplierDto;
import in.gbtsolutions.inventoryhub.online.models.SupplierMapper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnlineSupplierRepository {

    public interface SupplierListCallback {
        void onSuccess(List<Suppliers> suppliers);
        void onError(String errorMessage);
    }

    public interface SupplierActionCallback {
        void onSuccess(Suppliers supplier);
        void onError(String errorMessage);
    }

    public interface SupplierDeleteCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    public OnlineSupplierRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    private OnlineApiService getApi() {
        return OnlineApiClient.getInstance().getApiService(context);
    }

    public void fetchSuppliers(SupplierListCallback callback) {
        fetchSuppliers(null, callback);
    }

    public void fetchSuppliers(String search, SupplierListCallback callback) {
        getApi().getSuppliers(search).enqueue(new Callback<ApiResponse<List<OnlineSupplierDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<OnlineSupplierDto>>> call,
                                   Response<ApiResponse<List<OnlineSupplierDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<OnlineSupplierDto>> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        List<Suppliers> domainSuppliers = SupplierMapper.toDomainList(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(domainSuppliers);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Failed to load suppliers");
                        });
                        return;
                    }
                }
                handleError(response, callback, "Failed to load suppliers from server");
            }

            @Override
            public void onFailure(Call<ApiResponse<List<OnlineSupplierDto>>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                    }
                });
            }
        });
    }

    public void createSupplier(Suppliers supplier, SupplierActionCallback callback) {
        OnlineSupplierDto dto = SupplierMapper.toDto(supplier);
        getApi().createSupplier(dto).enqueue(new Callback<ApiResponse<OnlineSupplierDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineSupplierDto>> call,
                                   Response<ApiResponse<OnlineSupplierDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineSupplierDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        Suppliers created = SupplierMapper.toDomain(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(created);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Failed to create supplier");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to create supplier");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineSupplierDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                });
            }
        });
    }

    public void updateSupplier(Suppliers supplier, SupplierActionCallback callback) {
        OnlineSupplierDto dto = SupplierMapper.toDto(supplier);
        getApi().updateSupplier(supplier.supplierId, dto).enqueue(new Callback<ApiResponse<OnlineSupplierDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineSupplierDto>> call,
                                   Response<ApiResponse<OnlineSupplierDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineSupplierDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        Suppliers updated = SupplierMapper.toDomain(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(updated);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Failed to update supplier");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to update supplier");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineSupplierDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                });
            }
        });
    }

    public void deleteSupplier(int supplierId, SupplierDeleteCallback callback) {
        getApi().deleteSupplier(supplierId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess();
                    });
                } else {
                    String msg = "Failed to delete supplier";
                    try {
                        if (response.errorBody() != null) {
                            ApiResponse<?> err = gson.fromJson(response.errorBody().string(), ApiResponse.class);
                            if (err != null && err.getMessage() != null) msg = err.getMessage();
                        }
                    } catch (Exception ignored) {}
                    final String finalMsg = msg;
                    mainHandler.post(() -> {
                        if (callback != null) callback.onError(finalMsg);
                    });
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                });
            }
        });
    }

    private void handleError(Response<?> response, SupplierListCallback callback, String defaultMsg) {
        String msg = defaultMsg + " (" + response.code() + ")";
        try {
            if (response.errorBody() != null) {
                ApiResponse<?> err = gson.fromJson(response.errorBody().string(), ApiResponse.class);
                if (err != null && err.getMessage() != null && !err.getMessage().isEmpty()) {
                    msg = err.getMessage();
                }
            }
        } catch (Exception ignored) {}
        final String finalMsg = msg;
        mainHandler.post(() -> {
            if (callback != null) callback.onError(finalMsg);
        });
    }

    private void handleActionError(Response<?> response, SupplierActionCallback callback, String defaultMsg) {
        String msg = defaultMsg + " (" + response.code() + ")";
        try {
            if (response.errorBody() != null) {
                ApiResponse<?> err = gson.fromJson(response.errorBody().string(), ApiResponse.class);
                if (err != null && err.getMessage() != null && !err.getMessage().isEmpty()) {
                    msg = err.getMessage();
                }
            }
        } catch (Exception ignored) {}
        final String finalMsg = msg;
        mainHandler.post(() -> {
            if (callback != null) callback.onError(finalMsg);
        });
    }
}
