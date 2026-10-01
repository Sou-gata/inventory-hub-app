package in.gbtsolutions.inventoryhub.online.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleItem;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.online.models.OnlineSaleDto;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnlineSaleRepository {

    public interface SaleCreateCallback {
        void onSuccess(int saleId, String invoiceNo);
        void onError(String errorMessage);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    public OnlineSaleRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    private OnlineApiService getApi() {
        return OnlineApiClient.getInstance().getApiService(context);
    }

    public void createSale(Sale sale, List<SaleItem> items, SaleCreateCallback callback) {
        OnlineSaleDto dto = new OnlineSaleDto();
        dto.buyerId = sale.buyerId;
        dto.invoiceNo = sale.invoiceId;
        dto.saleDate = sale.billingDate;
        dto.totalAmount = sale.totalAmount;
        dto.taxAmount = sale.totalGst;
        dto.discountAmount = sale.discountAmount;
        dto.netAmount = sale.totalAmount;
        dto.paymentType = sale.paymentMethod;
        dto.paymentStatus = sale.status;

        if (items != null) {
            for (SaleItem item : items) {
                OnlineSaleDto.OnlineSaleItemDto itemDto = new OnlineSaleDto.OnlineSaleItemDto();
                itemDto.productId = item.productId;
                itemDto.batchId = item.batchId;
                itemDto.quantity = item.quantity;
                itemDto.unitPrice = item.unitPrice;
                itemDto.sellingPrice = item.unitPrice;
                itemDto.taxPercent = item.cgstRate + item.sgstRate + item.igstRate;
                dto.items.add(itemDto);
            }
        }

        getApi().createSale(dto).enqueue(new Callback<ApiResponse<OnlineSaleDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineSaleDto>> call, Response<ApiResponse<OnlineSaleDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineSaleDto> body = response.body();
                    if (body.isSuccess()) {
                        OnlineSaleDto result = body.getData();
                        int saleId = result != null ? result.saleId : 0;
                        String invoiceNo = result != null && result.invoiceNo != null ? result.invoiceNo : sale.invoiceId;
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(saleId, invoiceNo);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Failed to record sale on server");
                        });
                        return;
                    }
                }

                String errorMsg = "Server error (" + response.code() + ")";
                try {
                    if (response.errorBody() != null) {
                        ApiResponse<?> err = gson.fromJson(response.errorBody().string(), ApiResponse.class);
                        if (err != null && err.getMessage() != null && !err.getMessage().isEmpty()) {
                            errorMsg = err.getMessage();
                        }
                    }
                } catch (Exception ignored) {}
                final String finalMsg = errorMsg;
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(finalMsg);
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineSaleDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                });
            }
        });
    }
}
