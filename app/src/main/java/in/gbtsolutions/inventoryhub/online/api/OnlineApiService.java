package in.gbtsolutions.inventoryhub.online.api;

import java.util.List;

import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.online.models.OnlineCategoryDto;
import in.gbtsolutions.inventoryhub.online.models.OnlinePendingReceiveDto;
import in.gbtsolutions.inventoryhub.online.models.OnlineProductDto;
import in.gbtsolutions.inventoryhub.online.models.OnlinePurchaseDto;
import in.gbtsolutions.inventoryhub.online.models.OnlineQrLookupRequest;
import in.gbtsolutions.inventoryhub.online.models.OnlineReceiveItemsRequest;
import in.gbtsolutions.inventoryhub.online.models.OnlineSaleDto;
import in.gbtsolutions.inventoryhub.online.models.PagedData;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface OnlineApiService {

    @GET("api/v1/products")
    Call<ApiResponse<PagedData<OnlineProductDto>>> getProducts(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("search") String search,
            @Query("stock_status") String stockStatus,
            @Query("category_id") Integer categoryId
    );

    @GET("api/v1/products/{id}")
    Call<ApiResponse<OnlineProductDto>> getProductById(
            @Path("id") int productId
    );

    @POST("api/v1/products")
    Call<ApiResponse<OnlineProductDto>> createProduct(
            @Body OnlineProductDto productDto
    );

    @PUT("api/v1/products/{id}")
    Call<ApiResponse<OnlineProductDto>> updateProduct(
            @Path("id") int productId,
            @Body OnlineProductDto productDto
    );

    @DELETE("api/v1/products/{id}")
    Call<ApiResponse<Object>> deleteProduct(
            @Path("id") int productId
    );

    @POST("api/v1/products/qr-lookup")
    Call<ApiResponse<OnlineProductDto>> lookupProductByQr(
            @Body OnlineQrLookupRequest request
    );

    @GET("api/v1/products/{id}/batches")
    Call<ApiResponse<List<OnlineProductDto.OnlineBatchDto>>> getBatchesForProduct(
            @Path("id") int productId,
            @Query("status") String status
    );

    @POST("api/v1/products/{id}/batches")
    Call<ApiResponse<OnlineProductDto.OnlineBatchDto>> createBatchForProduct(
            @Path("id") int productId,
            @Body OnlineProductDto.OnlineBatchDto batchDto
    );


    @GET("api/v1/categories")
    Call<ApiResponse<List<OnlineCategoryDto>>> getCategories();

    @POST("api/v1/categories")
    Call<ApiResponse<OnlineCategoryDto>> createCategory(
            @Body OnlineCategoryDto categoryDto
    );

    @PUT("api/v1/categories/{id}")
    Call<ApiResponse<OnlineCategoryDto>> updateCategory(
            @Path("id") int categoryId,
            @Body OnlineCategoryDto categoryDto
    );

    @DELETE("api/v1/categories/{id}")
    Call<ApiResponse<Object>> deleteCategory(
            @Path("id") int categoryId
    );

    @POST("api/v1/sales")
    Call<ApiResponse<OnlineSaleDto>> createSale(
            @Body OnlineSaleDto saleDto
    );


    @POST("api/v1/purchases")
    Call<ApiResponse<OnlinePurchaseDto>> createPurchase(
            @Body OnlinePurchaseDto purchaseDto
    );

    @GET("api/v1/purchases/pending-receive")
    Call<ApiResponse<List<OnlinePendingReceiveDto>>> getPendingReceives(
            @Query("search") String search
    );

    @POST("api/v1/purchases/{id}/receive")
    Call<ApiResponse<Object>> receivePurchaseItems(
            @Path("id") int purchaseId,
            @Body OnlineReceiveItemsRequest request
    );


    @GET("api/v1/health")
    Call<ApiResponse<Object>> checkHealth();

    @POST("api/v1/auth/login")
    Call<ApiResponse<com.google.gson.JsonObject>> login(
            @Body com.google.gson.JsonObject credentials
    );

    @POST("api/v1/devices/lookup")
    Call<com.google.gson.JsonObject> lookupDevice(
            @Body com.google.gson.JsonObject body
    );
}
