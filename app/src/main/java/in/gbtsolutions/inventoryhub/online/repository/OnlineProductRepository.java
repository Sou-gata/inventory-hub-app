package in.gbtsolutions.inventoryhub.online.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.online.models.OnlineProductDto;
import in.gbtsolutions.inventoryhub.online.models.PagedData;
import in.gbtsolutions.inventoryhub.online.models.ProductMapper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnlineProductRepository {

    public interface PagedProductCallback {
        void onSuccess(List<Product> products, int currentPage, int totalPages, int totalRecords, boolean hasMore);
        void onError(String errorMessage, boolean sessionExpired);
    }

    public interface ProductActionCallback {
        void onSuccess(Product product);
        void onError(String errorMessage);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private Call<ApiResponse<PagedData<OnlineProductDto>>> currentSearchCall;

    public OnlineProductRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    private OnlineApiService getApi() {
        return OnlineApiClient.getInstance().getApiService(context);
    }

    /**
     * Loads a page of products from the server.
     * Cancels any pending search call if a new query is initiated.
     */
    public synchronized void fetchProducts(int page, int limit, String search, String stockStatus,
                                           Integer categoryId, PagedProductCallback callback) {
        if (currentSearchCall != null && !currentSearchCall.isCanceled()) {
            currentSearchCall.cancel();
        }

        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String status = (stockStatus != null && !stockStatus.trim().isEmpty()) ? stockStatus.trim() : null;

        currentSearchCall = getApi().getProducts(page, limit, query, status, categoryId);
        currentSearchCall.enqueue(new Callback<ApiResponse<PagedData<OnlineProductDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PagedData<OnlineProductDto>>> call,
                                   Response<ApiResponse<PagedData<OnlineProductDto>>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PagedData<OnlineProductDto>> apiResp = response.body();
                    if (apiResp.isSuccess() && apiResp.getData() != null) {
                        PagedData<OnlineProductDto> pagedData = apiResp.getData();
                        List<Product> domainProducts = ProductMapper.toDomainList(pagedData.getItems());
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onSuccess(
                                        domainProducts,
                                        pagedData.getPage(),
                                        pagedData.getTotalPages(),
                                        pagedData.getTotalRecords(),
                                        pagedData.hasMore()
                                );
                            }
                        });
                        return;
                    } else {
                        String errMsg = apiResp.getMessage();
                        boolean sessionExpired = apiResp.isSessionExpired();
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onError(errMsg.isEmpty() ? "Failed to load products" : errMsg, sessionExpired);
                            }
                        });
                        return;
                    }
                }

                // Handle error body if server responded with 4xx / 5xx ApiErrorResponse
                handleErrorResponse(response, callback);
            }

            @Override
            public void onFailure(Call<ApiResponse<PagedData<OnlineProductDto>>> call, Throwable t) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    if (callback != null) {
                        String msg = t.getMessage() != null ? t.getMessage() : "Network connection error";
                        callback.onError(msg, false);
                    }
                });
            }
        });
    }

    /**
     * Saves a new product to the remote cloud server.
     */
    public void createProduct(Product product, ProductActionCallback callback) {
        OnlineProductDto dto = ProductMapper.toDto(product);
        getApi().createProduct(dto).enqueue(new Callback<ApiResponse<OnlineProductDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineProductDto>> call,
                                   Response<ApiResponse<OnlineProductDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineProductDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        Product created = ProductMapper.toDomain(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(created);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Failed to create product");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to create product");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineProductDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                    }
                });
            }
        });
    }

    /**
     * Updates an existing product on the remote cloud server.
     */
    public void updateProduct(Product product, ProductActionCallback callback) {
        OnlineProductDto dto = ProductMapper.toDto(product);
        getApi().updateProduct(product.productId, dto).enqueue(new Callback<ApiResponse<OnlineProductDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineProductDto>> call,
                                   Response<ApiResponse<OnlineProductDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineProductDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        Product updated = ProductMapper.toDomain(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(updated);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Failed to update product");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to update product");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineProductDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                    }
                });
            }
        });
    }

    /**
     * Deletes a product on the remote cloud server.
     */
    public void deleteProduct(Product product, ProductActionCallback callback) {
        getApi().deleteProduct(product.productId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess(product);
                    });
                } else {
                    handleActionError(response, callback, "Failed to delete product");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                    }
                });
            }
        });
    }

    public void getProductById(int productId, ProductActionCallback callback) {
        getApi().getProductById(productId).enqueue(new Callback<ApiResponse<OnlineProductDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineProductDto>> call,
                                   Response<ApiResponse<OnlineProductDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineProductDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        Product product = ProductMapper.toDomain(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(product);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Product not found on server");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to fetch product details");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineProductDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                    }
                });
            }
        });
    }

    /**
     * Looks up a product on the remote server using decrypted QR code data.
     */
    public void lookupProductByQr(in.gbtsolutions.inventoryhub.online.models.OnlineQrLookupRequest request,
                                  ProductActionCallback callback) {
        getApi().lookupProductByQr(request).enqueue(new Callback<ApiResponse<OnlineProductDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineProductDto>> call,
                                   Response<ApiResponse<OnlineProductDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineProductDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        Product product = ProductMapper.toDomain(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(product);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Product not found on server for scanned QR");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to resolve scanned QR code online");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineProductDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                    }
                });
            }
        });
    }

    private void handleErrorResponse(Response<?> response, PagedProductCallback callback) {
        String errorMessage = "Server error (" + response.code() + ")";
        boolean sessionExpired = false;
        try {
            if (response.errorBody() != null) {
                String errorJson = response.errorBody().string();
                ApiResponse<?> errResp = gson.fromJson(errorJson, ApiResponse.class);
                if (errResp != null) {
                    if (errResp.getMessage() != null && !errResp.getMessage().isEmpty()) {
                        errorMessage = errResp.getMessage();
                    }
                    sessionExpired = errResp.isSessionExpired();
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

    private void handleActionError(Response<?> response, ProductActionCallback callback, String defaultMsg) {
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
