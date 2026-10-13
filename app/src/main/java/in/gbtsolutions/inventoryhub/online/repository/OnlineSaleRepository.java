package in.gbtsolutions.inventoryhub.online.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleItem;
import in.gbtsolutions.inventoryhub.models.SaleItemWithProduct;
import in.gbtsolutions.inventoryhub.models.SaleWithBuyer;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.online.models.OnlineSaleDto;
import in.gbtsolutions.inventoryhub.online.models.PagedData;
import in.gbtsolutions.inventoryhub.online.models.SaleMapper;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnlineSaleRepository {

    public interface SaleCreateCallback {
        void onSuccess(int saleId, String invoiceNo);
        void onError(String errorMessage);
    }

    public interface SaleListCallback {
        default void onSuccess(List<SaleWithBuyer> sales, boolean hasMore) {}
        default void onSuccess(List<SaleWithBuyer> sales, int currentPage, int totalPages, int totalRecords, boolean hasMore) {
            onSuccess(sales, hasMore);
        }
        void onError(String errorMessage);
    }

    public interface SaleDetailCallback {
        void onSuccess(SaleWithBuyer saleWithBuyer, List<SaleItemWithProduct> items);
        void onError(String errorMessage);
    }

    public interface SaleActionCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private Call<ApiResponse<PagedData<OnlineSaleDto>>> currentSalesCall;

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
        dto.discountPercent = sale.discountPercent;
        dto.netAmount = sale.totalAmount;
        dto.paymentType = sale.paymentMethod;
        dto.paymentStatus = sale.status;
        dto.customerName = sale.customerName;
        dto.customerPhone = sale.customerPhone;
        dto.customerGstin = sale.customerGstin;
        dto.isWalkIn = sale.isWalkIn;
        dto.placeOfSupply = sale.placeOfSupply;
        dto.placeOfSupplyStateCode = sale.placeOfSupplyStateCode;
        dto.subtotalAmount = sale.subtotalAmount;
        dto.cgstAmount = sale.cgstAmount;
        dto.sgstAmount = sale.sgstAmount;
        dto.igstAmount = sale.igstAmount;
        dto.cessAmount = sale.cessAmount;
        dto.otherCharges = sale.otherCharges;

        if (items != null) {
            for (SaleItem item : items) {
                OnlineSaleDto.OnlineSaleItemDto itemDto = new OnlineSaleDto.OnlineSaleItemDto();
                itemDto.productId = item.productId;
                itemDto.batchId = item.batchId;
                itemDto.quantity = item.quantity;
                itemDto.unitPrice = item.unitPrice;
                itemDto.sellingPrice = item.unitPrice;
                itemDto.taxPercent = item.cgstRate + item.sgstRate + item.igstRate;
                itemDto.cgstRate = item.cgstRate;
                itemDto.sgstRate = item.sgstRate;
                itemDto.igstRate = item.igstRate;
                itemDto.cgstAmount = item.cgstAmount;
                itemDto.sgstAmount = item.sgstAmount;
                itemDto.igstAmount = item.igstAmount;
                itemDto.cessRate = item.cessRate;
                itemDto.cessAmount = item.cessAmount;
                itemDto.subtotal = item.subtotal;
                itemDto.discountPercent = item.discountPercent;
                itemDto.discountAmount = item.discountAmount;
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

                handleError(response, callback != null ? callback::onError : null, "Failed to record sale on server");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineSaleDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                });
            }
        });
    }

    public synchronized void fetchSales(int page, int limit, String search, String startDate, String endDate, SaleListCallback callback) {
        if (page == 1 && currentSalesCall != null && !currentSalesCall.isCanceled()) {
            currentSalesCall.cancel();
        }

        String query = (search != null && !search.trim().isEmpty()) ? search.trim() : null;
        String start = (startDate != null && !startDate.trim().isEmpty()) ? startDate.trim() : null;
        String end = (endDate != null && !endDate.trim().isEmpty()) ? endDate.trim() : null;

        currentSalesCall = getApi().getSales(page, limit, query, start, end);
        currentSalesCall.enqueue(new Callback<>() {
            @Override
            public void onResponse(Call<ApiResponse<PagedData<OnlineSaleDto>>> call, Response<ApiResponse<PagedData<OnlineSaleDto>>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PagedData<OnlineSaleDto>> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        PagedData<OnlineSaleDto> pagedData = body.getData();
                        List<SaleWithBuyer> domainSales = SaleMapper.toDomainList(pagedData.getItems());
                        boolean hasMore = pagedData.hasMore();
                        mainHandler.post(() -> {
                            if (callback != null) {
                                callback.onSuccess(domainSales, pagedData.getPage(), pagedData.getTotalPages(), pagedData.getTotalRecords(), hasMore);
                            }
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null)
                                callback.onError(!msg.isEmpty() ? msg : "Failed to load sales");
                        });
                        return;
                    }
                }

                handleError(response, callback != null ? callback::onError : null, "Failed to load sales from server");
            }

            @Override
            public void onFailure(Call<ApiResponse<PagedData<OnlineSaleDto>>> call, Throwable t) {
                if (call.isCanceled()) return;

                mainHandler.post(() -> {
                    if (callback != null)
                        callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                });
            }
        });
    }

    public void fetchSaleById(int saleId, SaleDetailCallback callback) {
        getApi().getSaleById(saleId).enqueue(new Callback<ApiResponse<OnlineSaleDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineSaleDto>> call, Response<ApiResponse<OnlineSaleDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineSaleDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        OnlineSaleDto dto = body.getData();
                        SaleWithBuyer swb = SaleMapper.toDomainSaleWithBuyer(dto);
                        List<SaleItemWithProduct> items = SaleMapper.toDomainSaleItemsWithProduct(dto);
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(swb, items);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Failed to load sale details");
                        });
                        return;
                    }
                }

                handleError(response, callback != null ? callback::onError : null, "Failed to load sale details");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineSaleDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                });
            }
        });
    }

    public void cancelSale(int saleId, SaleActionCallback callback) {
        getApi().cancelSale(saleId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess();
                    });
                } else {
                    handleError(response, callback != null ? callback::onError : null, "Failed to cancel sale");
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
