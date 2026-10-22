package in.gbtsolutions.inventoryhub.online.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.Suppliers;
import in.gbtsolutions.inventoryhub.online.api.NetworkErrorHelper;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.online.models.OnlineSupplierDto;
import in.gbtsolutions.inventoryhub.online.models.SupplierMapper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import in.gbtsolutions.inventoryhub.online.models.PagedData;

public class OnlineSupplierRepository {

    public interface PagedSupplierCallback {
        void onSuccess(List<Suppliers> suppliers, int currentPage, int totalPages, int totalRecords, boolean hasMore);

        void onError(String errorMessage);
    }

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
    private Call<ApiResponse<PagedData<OnlineSupplierDto>>> currentSuppliersCall;

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
        fetchSuppliersPaged(1, 100, search, null, new PagedSupplierCallback() {
            @Override
            public void onSuccess(List<Suppliers> suppliers, int currentPage, int totalPages, int totalRecords, boolean hasMore) {
                if (callback != null) {
                    callback.onSuccess(suppliers);
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

    public synchronized void fetchSuppliersPaged(int page, int limit, String search, String status, PagedSupplierCallback callback) {
        if (currentSuppliersCall != null && !currentSuppliersCall.isCanceled()) {
            currentSuppliersCall.cancel();
        }

        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String statusParam = (status != null && !status.trim().isEmpty()) ? status.trim() : null;

        currentSuppliersCall = getApi().getSuppliers(page, limit, query, statusParam);
        currentSuppliersCall.enqueue(new Callback<ApiResponse<PagedData<OnlineSupplierDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PagedData<OnlineSupplierDto>>> call, Response<ApiResponse<PagedData<OnlineSupplierDto>>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PagedData<OnlineSupplierDto>> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        PagedData<OnlineSupplierDto> pagedData = body.getData();
                        List<Suppliers> domainSuppliers = SupplierMapper.toDomainList(pagedData.getItems());
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onSuccess(domainSuppliers, pagedData.getPage(), pagedData.getTotalPages(), pagedData.getTotalRecords(), pagedData.hasMore());
                            }
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onError(msg != null && !msg.isEmpty() ? msg : "Failed to load suppliers");
                            }
                        });
                        return;
                    }
                }
                handlePagedError(response, callback, "Failed to load suppliers from server");
            }

            @Override
            public void onFailure(Call<ApiResponse<PagedData<OnlineSupplierDto>>> call, Throwable t) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(NetworkErrorHelper.getErrorMessage(t));
                    }
                });
            }
        });
    }

    public void createSupplier(Suppliers supplier, SupplierActionCallback callback) {
        OnlineSupplierDto dto = SupplierMapper.toDto(supplier);
        getApi().createSupplier(dto).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineSupplierDto>> call, Response<ApiResponse<OnlineSupplierDto>> response) {
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
                            if (callback != null)
                                callback.onError(!msg.isEmpty() ? msg : "Failed to create supplier");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to create supplier");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineSupplierDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null)
                        callback.onError(NetworkErrorHelper.getErrorMessage(t));
                });
            }
        });
    }

    public void updateSupplier(Suppliers supplier, SupplierActionCallback callback) {
        OnlineSupplierDto dto = SupplierMapper.toDto(supplier);
        getApi().updateSupplier(supplier.supplierId, dto).enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineSupplierDto>> call, Response<ApiResponse<OnlineSupplierDto>> response) {
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
                            if (callback != null)
                                callback.onError(!msg.isEmpty() ? msg : "Failed to update supplier");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to update supplier");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineSupplierDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null)
                        callback.onError(NetworkErrorHelper.getErrorMessage(t));
                });
            }
        });
    }

    public void deleteSupplier(int supplierId, SupplierDeleteCallback callback) {
        getApi().deleteSupplier(supplierId).enqueue(new Callback<>() {
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

    private void handlePagedError(Response<?> response, PagedSupplierCallback callback, String defaultMsg) {
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

    private void handleError(Response<?> response, SupplierListCallback callback, String defaultMsg) {
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

    private void handleActionError(Response<?> response, SupplierActionCallback callback, String defaultMsg) {
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
