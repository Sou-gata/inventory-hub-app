package in.gbtsolutions.inventoryhub.online.api;

import com.google.gson.JsonObject;

import java.util.List;

import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.online.models.OnlineBuyerDto;
import in.gbtsolutions.inventoryhub.online.models.OnlineCategoryDto;
import in.gbtsolutions.inventoryhub.online.models.OnlineDeviceDashboardDto;
import in.gbtsolutions.inventoryhub.online.models.OnlinePendingReceiveDto;
import in.gbtsolutions.inventoryhub.online.models.OnlineProductDto;
import in.gbtsolutions.inventoryhub.online.models.OnlinePurchaseDto;
import in.gbtsolutions.inventoryhub.online.models.OnlineQrLookupRequest;
import in.gbtsolutions.inventoryhub.online.models.OnlineReceiveItemsRequest;
import in.gbtsolutions.inventoryhub.online.models.OnlineSaleDto;
import in.gbtsolutions.inventoryhub.online.models.OnlineSupplierDto;
import in.gbtsolutions.inventoryhub.online.models.PagedData;
import in.gbtsolutions.inventoryhub.reports.gstr1.GSTR1Models;
import in.gbtsolutions.inventoryhub.reports.purchaseregister.PurchaseRegisterModels;
import in.gbtsolutions.inventoryhub.reports.salesregister.SalesRegisterModels;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;

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

    @GET("api/v1/buyers")
    Call<ApiResponse<List<OnlineBuyerDto>>> getBuyers(
            @Query("search") String search
    );

    @GET("api/v1/buyers/{id}")
    Call<ApiResponse<OnlineBuyerDto>> getBuyerById(
            @Path("id") int buyerId
    );

    @POST("api/v1/buyers")
    Call<ApiResponse<OnlineBuyerDto>> createBuyer(
            @Body OnlineBuyerDto buyerDto
    );

    @PUT("api/v1/buyers/{id}")
    Call<ApiResponse<OnlineBuyerDto>> updateBuyer(
            @Path("id") int buyerId,
            @Body OnlineBuyerDto buyerDto
    );

    @DELETE("api/v1/buyers/{id}")
    Call<ApiResponse<Object>> deleteBuyer(
            @Path("id") int buyerId
    );

    @GET("api/v1/suppliers")
    Call<ApiResponse<List<OnlineSupplierDto>>> getSuppliers(
            @Query("search") String search
    );

    @GET("api/v1/suppliers/{id}")
    Call<ApiResponse<OnlineSupplierDto>> getSupplierById(
            @Path("id") int supplierId
    );

    @POST("api/v1/suppliers")
    Call<ApiResponse<OnlineSupplierDto>> createSupplier(
            @Body OnlineSupplierDto supplierDto
    );

    @PUT("api/v1/suppliers/{id}")
    Call<ApiResponse<OnlineSupplierDto>> updateSupplier(
            @Path("id") int supplierId,
            @Body OnlineSupplierDto supplierDto
    );

    @DELETE("api/v1/suppliers/{id}")
    Call<ApiResponse<Object>> deleteSupplier(
            @Path("id") int supplierId
    );

    @POST("api/v1/sales")
    Call<ApiResponse<OnlineSaleDto>> createSale(
            @Body OnlineSaleDto saleDto
    );

    @GET("api/v1/sales")
    Call<ApiResponse<PagedData<OnlineSaleDto>>> getSales(
            @Query("page") int page,
            @Query("limit") int limit,
            @Query("search") String search,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @GET("api/v1/sales/{id}")
    Call<ApiResponse<OnlineSaleDto>> getSaleById(
            @Path("id") int saleId
    );

    @PUT("api/v1/sales/{id}/cancel")
    Call<ApiResponse<Object>> cancelSale(
            @Path("id") int saleId
    );


    @POST("api/v1/purchases")
    Call<ApiResponse<OnlinePurchaseDto>> createPurchase(
            @Body OnlinePurchaseDto purchaseDto
    );

    @GET("api/v1/purchases")
    Call<ApiResponse<List<OnlinePurchaseDto>>> getPurchases(
            @Query("search") String search,
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
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

    @PUT("api/v1/purchases/{id}/cancel")
    Call<ApiResponse<Object>> cancelPurchase(
            @Path("id") int purchaseId
    );


    @GET("api/v1/dashboard/device")
    Call<ApiResponse<OnlineDeviceDashboardDto>> getDeviceDashboardStats();

    @GET("api/v1/health")
    Call<ApiResponse<Object>> checkHealth();

    @POST("api/v1/auth/login")
    Call<ApiResponse<JsonObject>> login(@Body JsonObject credentials);

    @POST("api/v1/devices/lookup")
    Call<JsonObject> lookupDevice(@Body JsonObject body);

    @GET("api/v1/reports/gstr1")
    Call<ApiResponse<GSTR1Models.ReportData>> getGstr1Report(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate,
            @Query("b2cLargeThreshold") Double b2cLargeThreshold
    );

    @Streaming
    @GET("api/v1/reports/gstr1/csv")
    Call<ResponseBody> downloadGstr1Csv(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate,
            @Query("b2cLargeThreshold") Double b2cLargeThreshold
    );

    @Streaming
    @GET("api/v1/reports/gstr1/excel")
    Call<ResponseBody> downloadGstr1Excel(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate,
            @Query("b2cLargeThreshold") Double b2cLargeThreshold
    );

    @Streaming
    @GET("api/v1/reports/gstr1/pdf")
    Call<ResponseBody> downloadGstr1Pdf(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate,
            @Query("b2cLargeThreshold") Double b2cLargeThreshold
    );

    // Sales Register
    @GET("api/v1/reports/sales-register")
    Call<ApiResponse<SalesRegisterModels.ReportData>> getSalesRegister(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate,
            @Query("includeItems") Boolean includeItems
    );

    @Streaming
    @GET("api/v1/reports/sales-register/csv")
    Call<ResponseBody> downloadSalesRegisterCsv(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @Streaming
    @GET("api/v1/reports/sales-register/excel")
    Call<ResponseBody> downloadSalesRegisterExcel(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @Streaming
    @GET("api/v1/reports/sales-register/pdf")
    Call<ResponseBody> downloadSalesRegisterPdf(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    // Purchase Register
    @GET("api/v1/reports/purchase-register")
    Call<ApiResponse<PurchaseRegisterModels.ReportData>> getPurchaseRegister(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate,
            @Query("includeItems") Boolean includeItems
    );

    @Streaming
    @GET("api/v1/reports/purchase-register/csv")
    Call<ResponseBody> downloadPurchaseRegisterCsv(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @Streaming
    @GET("api/v1/reports/purchase-register/excel")
    Call<ResponseBody> downloadPurchaseRegisterExcel(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    @Streaming
    @GET("api/v1/reports/purchase-register/pdf")
    Call<ResponseBody> downloadPurchaseRegisterPdf(
            @Query("startDate") String startDate,
            @Query("endDate") String endDate
    );

    // Offline to Online Migration
    @Multipart
    @POST("api/v1/migration/upload")
    Call<ApiResponse<JsonObject>> uploadMigrationDump(
            @Part MultipartBody.Part database
    );

    @GET("api/v1/migration/status/{jobId}")
    Call<ApiResponse<JsonObject>> getMigrationStatus(
            @Path("jobId") String jobId
    );
}
