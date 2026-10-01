package in.gbtsolutions.inventoryhub.online.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseItem;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.online.models.OnlinePendingReceiveDto;
import in.gbtsolutions.inventoryhub.online.models.OnlinePurchaseDto;
import in.gbtsolutions.inventoryhub.online.models.OnlineReceiveItemsRequest;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnlinePurchaseRepository {

    public interface PurchaseCreateCallback {
        void onSuccess(int purchaseId);
        void onError(String errorMessage);
    }

    public interface PendingReceivesCallback {
        void onSuccess(List<OnlinePendingReceiveDto> pendingList);
        void onError(String errorMessage);
    }

    public interface ReceiveActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    public OnlinePurchaseRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    private OnlineApiService getApi() {
        return OnlineApiClient.getInstance().getApiService(context);
    }

    public void createPurchase(Purchase purchase, List<PurchaseItem> items, PurchaseCreateCallback callback) {
        OnlinePurchaseDto dto = new OnlinePurchaseDto();
        dto.supplierId = purchase.supplierId;
        dto.invoiceId = purchase.invoiceId;
        dto.purchaseDate = purchase.billingDate;
        dto.totalAmount = purchase.totalAmount;
        dto.status = purchase.status != null ? purchase.status : "Pending";

        if (items != null) {
            for (PurchaseItem item : items) {
                OnlinePurchaseDto.OnlinePurchaseItemDto itemDto = new OnlinePurchaseDto.OnlinePurchaseItemDto();
                itemDto.productId = item.productId;
                itemDto.quantity = item.quantity;
                itemDto.unitPrice = item.unitPrice;
                itemDto.taxPercent = item.cgstRate + item.sgstRate + item.igstRate;
                dto.items.add(itemDto);
            }
        }

        getApi().createPurchase(dto).enqueue(new Callback<ApiResponse<OnlinePurchaseDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlinePurchaseDto>> call,
                                   Response<ApiResponse<OnlinePurchaseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlinePurchaseDto> body = response.body();
                    if (body.isSuccess()) {
                        OnlinePurchaseDto result = body.getData();
                        int purchaseId = result != null ? result.purchaseId : 0;
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(purchaseId);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Failed to record purchase on server");
                        });
                        return;
                    }
                }
                handleError(response, callback != null ? callback::onError : null, "Failed to create purchase order");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlinePurchaseDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                });
            }
        });
    }

    public void fetchPendingReceives(String search, PendingReceivesCallback callback) {
        getApi().getPendingReceives(search).enqueue(new Callback<ApiResponse<List<OnlinePendingReceiveDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<OnlinePendingReceiveDto>>> call,
                                   Response<ApiResponse<List<OnlinePendingReceiveDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<OnlinePendingReceiveDto>> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(body.getData());
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Failed to load pending receives");
                        });
                        return;
                    }
                }
                handleError(response, callback != null ? callback::onError : null, "Failed to load pending receives");
            }

            @Override
            public void onFailure(Call<ApiResponse<List<OnlinePendingReceiveDto>>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                });
            }
        });
    }

    public void receiveItems(int purchaseId, String notes,
                             List<OnlineReceiveItemsRequest.ReceiveItemPayload> items,
                             ReceiveActionCallback callback) {
        OnlineReceiveItemsRequest req = new OnlineReceiveItemsRequest();
        req.purchaseId = purchaseId;
        req.notes = notes;
        req.items = items;

        getApi().receivePurchaseItems(purchaseId, req).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess();
                    });
                } else {
                    handleError(response, callback != null ? callback::onError : null, "Failed to receive items");
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

    private void handleError(Response<?> response, GenericErrorCallback callback, String defaultMsg) {
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
        if (callback != null) {
            mainHandler.post(() -> callback.onError(finalMsg));
        }
    }

    private interface GenericErrorCallback {
        void onError(String msg);
    }
}
