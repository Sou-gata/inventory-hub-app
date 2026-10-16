package in.gbtsolutions.inventoryhub.online.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.api.NetworkErrorHelper;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.reports.audittrail.AuditTrailReportModels;
import in.gbtsolutions.inventoryhub.reports.gstr1.GSTR1Models;
import in.gbtsolutions.inventoryhub.reports.purchaseregister.PurchaseRegisterModels;
import in.gbtsolutions.inventoryhub.reports.salesregister.SalesRegisterModels;
import in.gbtsolutions.inventoryhub.reports.userreport.UserWiseReportModels;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnlineReportRepository {

    public interface ReportCallback {
        void onSuccess(GSTR1Models.ReportData reportData);
        void onError(String errorMessage, boolean sessionExpired);
    }

    public interface SalesRegisterCallback {
        void onSuccess(SalesRegisterModels.ReportData reportData);
        void onError(String errorMessage, boolean sessionExpired);
    }

    public interface PurchaseRegisterCallback {
        void onSuccess(PurchaseRegisterModels.ReportData reportData);
        void onError(String errorMessage, boolean sessionExpired);
    }

    public interface AuditTrailCallback {
        void onSuccess(AuditTrailReportModels.ReportData reportData);
        void onError(String errorMessage, boolean sessionExpired);
    }

    public interface UserWiseReportCallback {
        void onSuccess(UserWiseReportModels.ReportData reportData);
        void onError(String errorMessage, boolean sessionExpired);
    }

    public interface DownloadCallback {
        void onSuccess(File downloadedFile, String mimeType);
        void onError(String errorMessage, boolean sessionExpired);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();
    private Call<ApiResponse<GSTR1Models.ReportData>> currentGstr1Call;
    private Call<ApiResponse<SalesRegisterModels.ReportData>> currentSalesCall;
    private Call<ApiResponse<PurchaseRegisterModels.ReportData>> currentPurchaseCall;
    private Call<ApiResponse<AuditTrailReportModels.ReportData>> currentAuditTrailCall;
    private Call<ApiResponse<UserWiseReportModels.ReportData>> currentUserReportCall;

    public OnlineReportRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    private OnlineApiService getApi() {
        return OnlineApiClient.getInstance().getApiService(context);
    }

    // =========================================================================
    // GSTR-1 REPORT
    // =========================================================================

    public synchronized void fetchGstr1Report(String startDate, String endDate, Double threshold, ReportCallback callback) {
        if (currentGstr1Call != null && !currentGstr1Call.isCanceled()) {
            currentGstr1Call.cancel();
        }

        currentGstr1Call = getApi().getGstr1Report(startDate, endDate, threshold);
        currentGstr1Call.enqueue(new Callback<ApiResponse<GSTR1Models.ReportData>>() {
            @Override
            public void onResponse(Call<ApiResponse<GSTR1Models.ReportData>> call, Response<ApiResponse<GSTR1Models.ReportData>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<GSTR1Models.ReportData> apiResp = response.body();
                    if (apiResp.isSuccess() && apiResp.getData() != null) {
                        GSTR1Models.ReportData data = apiResp.getData();
                        data.ensureNonNull();
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
                                callback.onError(errMsg != null && !errMsg.isEmpty() ? errMsg : "Failed to load GSTR-1 report", sessionExpired);
                            }
                        });
                        return;
                    }
                }

                handleHttpError(response, "Failed to load GSTR-1 report", (msg, sessionExpired) -> {
                    if (callback != null) callback.onError(msg, sessionExpired);
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<GSTR1Models.ReportData>> call, Throwable t) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(NetworkErrorHelper.getErrorMessage(t), false);
                    }
                });
            }
        });
    }

    public synchronized void downloadGstr1Report(String format, String startDate, String endDate, Double threshold, DownloadCallback callback) {
        Call<ResponseBody> call;
        String mimeType;
        String extension;
        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            call = getApi().downloadGstr1Excel(startDate, endDate, threshold);
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            extension = ".xlsx";
        } else if ("csv".equalsIgnoreCase(format)) {
            call = getApi().downloadGstr1Csv(startDate, endDate, threshold);
            mimeType = "text/csv";
            extension = ".csv";
        } else {
            call = getApi().downloadGstr1Pdf(startDate, endDate, threshold);
            mimeType = "application/pdf";
            extension = ".pdf";
        }

        streamResponseToFile(call, "GSTR1_Report_", extension, mimeType, callback);
    }

    // =========================================================================
    // SALES REGISTER REPORT
    // =========================================================================

    public synchronized void fetchSalesRegister(String startDate, String endDate, Boolean includeItems, SalesRegisterCallback callback) {
        if (currentSalesCall != null && !currentSalesCall.isCanceled()) {
            currentSalesCall.cancel();
        }

        currentSalesCall = getApi().getSalesRegister(startDate, endDate, includeItems);
        currentSalesCall.enqueue(new Callback<ApiResponse<SalesRegisterModels.ReportData>>() {
            @Override
            public void onResponse(Call<ApiResponse<SalesRegisterModels.ReportData>> call, Response<ApiResponse<SalesRegisterModels.ReportData>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<SalesRegisterModels.ReportData> apiResp = response.body();
                    if (apiResp.isSuccess() && apiResp.getData() != null) {
                        SalesRegisterModels.ReportData data = apiResp.getData();
                        data.ensureNonNull();
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
                                callback.onError(errMsg != null && !errMsg.isEmpty() ? errMsg : "Failed to load Sales Register", sessionExpired);
                            }
                        });
                        return;
                    }
                }

                handleHttpError(response, "Failed to load Sales Register", (msg, sessionExpired) -> {
                    if (callback != null) callback.onError(msg, sessionExpired);
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<SalesRegisterModels.ReportData>> call, Throwable t) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(NetworkErrorHelper.getErrorMessage(t), false);
                    }
                });
            }
        });
    }

    public synchronized void downloadSalesRegister(String format, String startDate, String endDate, DownloadCallback callback) {
        Call<ResponseBody> call;
        String mimeType;
        String extension;
        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            call = getApi().downloadSalesRegisterExcel(startDate, endDate);
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            extension = ".xlsx";
        } else if ("csv".equalsIgnoreCase(format)) {
            call = getApi().downloadSalesRegisterCsv(startDate, endDate);
            mimeType = "text/csv";
            extension = ".csv";
        } else {
            call = getApi().downloadSalesRegisterPdf(startDate, endDate);
            mimeType = "application/pdf";
            extension = ".pdf";
        }

        streamResponseToFile(call, "Sales_Register_", extension, mimeType, callback);
    }

    // =========================================================================
    // PURCHASE REGISTER REPORT
    // =========================================================================

    public synchronized void fetchPurchaseRegister(String startDate, String endDate, Boolean includeItems, PurchaseRegisterCallback callback) {
        if (currentPurchaseCall != null && !currentPurchaseCall.isCanceled()) {
            currentPurchaseCall.cancel();
        }

        currentPurchaseCall = getApi().getPurchaseRegister(startDate, endDate, includeItems);
        currentPurchaseCall.enqueue(new Callback<ApiResponse<PurchaseRegisterModels.ReportData>>() {
            @Override
            public void onResponse(Call<ApiResponse<PurchaseRegisterModels.ReportData>> call, Response<ApiResponse<PurchaseRegisterModels.ReportData>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<PurchaseRegisterModels.ReportData> apiResp = response.body();
                    if (apiResp.isSuccess() && apiResp.getData() != null) {
                        PurchaseRegisterModels.ReportData data = apiResp.getData();
                        data.ensureNonNull();
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
                                callback.onError(errMsg != null && !errMsg.isEmpty() ? errMsg : "Failed to load Purchase Register", sessionExpired);
                            }
                        });
                        return;
                    }
                }

                handleHttpError(response, "Failed to load Purchase Register", (msg, sessionExpired) -> {
                    if (callback != null) callback.onError(msg, sessionExpired);
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<PurchaseRegisterModels.ReportData>> call, Throwable t) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(NetworkErrorHelper.getErrorMessage(t), false);
                    }
                });
            }
        });
    }

    public synchronized void downloadPurchaseRegister(String format, String startDate, String endDate, DownloadCallback callback) {
        Call<ResponseBody> call;
        String mimeType;
        String extension;
        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            call = getApi().downloadPurchaseRegisterExcel(startDate, endDate);
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            extension = ".xlsx";
        } else if ("csv".equalsIgnoreCase(format)) {
            call = getApi().downloadPurchaseRegisterCsv(startDate, endDate);
            mimeType = "text/csv";
            extension = ".csv";
        } else {
            call = getApi().downloadPurchaseRegisterPdf(startDate, endDate);
            mimeType = "application/pdf";
            extension = ".pdf";
        }

        streamResponseToFile(call, "Purchase_Register_", extension, mimeType, callback);
    }

    // =========================================================================
    // AUDIT TRAIL REPORT
    // =========================================================================

    public synchronized void fetchAuditTrailReport(String startDate, String endDate, String module, String action, String search, Integer page, Integer limit, AuditTrailCallback callback) {
        if (currentAuditTrailCall != null && !currentAuditTrailCall.isCanceled()) {
            currentAuditTrailCall.cancel();
        }

        currentAuditTrailCall = getApi().getAuditTrailReport(startDate, endDate, module, action, search, page, limit);
        currentAuditTrailCall.enqueue(new Callback<ApiResponse<AuditTrailReportModels.ReportData>>() {
            @Override
            public void onResponse(Call<ApiResponse<AuditTrailReportModels.ReportData>> call, Response<ApiResponse<AuditTrailReportModels.ReportData>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<AuditTrailReportModels.ReportData> apiResp = response.body();
                    if (apiResp.isSuccess() && apiResp.getData() != null) {
                        AuditTrailReportModels.ReportData data = apiResp.getData();
                        data.ensureNonNull();
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
                                callback.onError(errMsg != null && !errMsg.isEmpty() ? errMsg : "Failed to load Audit Trail report", sessionExpired);
                            }
                        });
                        return;
                    }
                }

                handleHttpError(response, "Failed to load Audit Trail report", (msg, sessionExpired) -> {
                    if (callback != null) callback.onError(msg, sessionExpired);
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<AuditTrailReportModels.ReportData>> call, Throwable t) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(NetworkErrorHelper.getErrorMessage(t), false);
                    }
                });
            }
        });
    }

    public synchronized void fetchAuditTrailReport(String startDate, String endDate, String module, String action, String search, AuditTrailCallback callback) {
        fetchAuditTrailReport(startDate, endDate, module, action, search, null, null, callback);
    }

    public synchronized void downloadAuditTrailReport(String format, String startDate, String endDate, String module, String action, String search, DownloadCallback callback) {
        Call<ResponseBody> call;
        String mimeType;
        String extension;
        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            call = getApi().downloadAuditTrailExcel(startDate, endDate, module, action, search);
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            extension = ".xlsx";
        } else if ("csv".equalsIgnoreCase(format)) {
            call = getApi().downloadAuditTrailCsv(startDate, endDate, module, action, search);
            mimeType = "text/csv";
            extension = ".csv";
        } else {
            call = getApi().downloadAuditTrailPdf(startDate, endDate, module, action, search);
            mimeType = "application/pdf";
            extension = ".pdf";
        }

        streamResponseToFile(call, "Audit_Trail_", extension, mimeType, callback);
    }

    // =========================================================================
    // USER WISE REPORT
    // =========================================================================

    public synchronized void fetchUserWiseReport(String startDate, String endDate, Long filterUserId, UserWiseReportCallback callback) {
        if (currentUserReportCall != null && !currentUserReportCall.isCanceled()) {
            currentUserReportCall.cancel();
        }

        Long filterParam = (filterUserId != null && filterUserId > 0) ? filterUserId : null;
        currentUserReportCall = getApi().getUserReport(startDate, endDate, filterParam);
        currentUserReportCall.enqueue(new Callback<ApiResponse<UserWiseReportModels.ReportData>>() {
            @Override
            public void onResponse(Call<ApiResponse<UserWiseReportModels.ReportData>> call, Response<ApiResponse<UserWiseReportModels.ReportData>> response) {
                if (call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<UserWiseReportModels.ReportData> apiResp = response.body();
                    if (apiResp.isSuccess() && apiResp.getData() != null) {
                        UserWiseReportModels.ReportData data = apiResp.getData();
                        data.ensureNonNull();
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
                                callback.onError(errMsg != null && !errMsg.isEmpty() ? errMsg : "Failed to load User Wise report", sessionExpired);
                            }
                        });
                        return;
                    }
                }

                handleHttpError(response, "Failed to load User Wise report", (msg, sessionExpired) -> {
                    if (callback != null) callback.onError(msg, sessionExpired);
                });
            }

            @Override
            public void onFailure(Call<ApiResponse<UserWiseReportModels.ReportData>> call, Throwable t) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(NetworkErrorHelper.getErrorMessage(t), false);
                    }
                });
            }
        });
    }

    public synchronized void downloadUserWiseReport(String format, String startDate, String endDate, Long filterUserId, DownloadCallback callback) {
        Call<ResponseBody> call;
        String mimeType;
        String extension;
        Long filterParam = (filterUserId != null && filterUserId > 0) ? filterUserId : null;

        if ("excel".equalsIgnoreCase(format) || "xlsx".equalsIgnoreCase(format)) {
            call = getApi().downloadUserReportExcel(startDate, endDate, filterParam);
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            extension = ".xlsx";
        } else if ("csv".equalsIgnoreCase(format)) {
            call = getApi().downloadUserReportCsv(startDate, endDate, filterParam);
            mimeType = "text/csv";
            extension = ".csv";
        } else {
            call = getApi().downloadUserReportPdf(startDate, endDate, filterParam);
            mimeType = "application/pdf";
            extension = ".pdf";
        }

        streamResponseToFile(call, "User_Wise_Report_", extension, mimeType, callback);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private interface ErrorDispatcher {
        void dispatch(String message, boolean sessionExpired);
    }

    private void handleHttpError(Response<?> response, String defaultPrefix, ErrorDispatcher dispatcher) {
        String msg = defaultPrefix + " (" + response.code() + ")";
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
        mainHandler.post(() -> dispatcher.dispatch(finalMsg, finalSessionExpired));
    }

    private void streamResponseToFile(Call<ResponseBody> call, String filePrefix, String extension, String mimeType, DownloadCallback callback) {
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        try {
                            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
                            String filename = filePrefix + timestamp + extension;
                            File cacheDir = new File(context.getCacheDir(), "reports");
                            if (!cacheDir.exists()) cacheDir.mkdirs();
                            File targetFile = new File(cacheDir, filename);

                            try (InputStream is = response.body().byteStream();
                                 OutputStream os = new FileOutputStream(targetFile)) {
                                byte[] buffer = new byte[8192];
                                int read;
                                while ((read = is.read(buffer)) != -1) {
                                    os.write(buffer, 0, read);
                                }
                                os.flush();
                            }

                            mainHandler.post(() -> {
                                if (callback != null) {
                                    callback.onSuccess(targetFile, mimeType);
                                }
                            });
                        } catch (Exception e) {
                            mainHandler.post(() -> {
                                if (callback != null) {
                                    callback.onError("Failed to save downloaded report: " + e.getMessage(), false);
                                }
                            });
                        }
                    });
                } else {
                    boolean sessionExpired = response.code() == 401;
                    mainHandler.post(() -> {
                        if (callback != null) {
                            callback.onError("Report download failed (" + response.code() + ")", sessionExpired);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError(NetworkErrorHelper.getErrorMessage(t), false);
                    }
                });
            }
        });
    }

    public synchronized void cancel() {
        if (currentGstr1Call != null && !currentGstr1Call.isCanceled()) {
            currentGstr1Call.cancel();
        }
        if (currentSalesCall != null && !currentSalesCall.isCanceled()) {
            currentSalesCall.cancel();
        }
        if (currentPurchaseCall != null && !currentPurchaseCall.isCanceled()) {
            currentPurchaseCall.cancel();
        }
        if (currentAuditTrailCall != null && !currentAuditTrailCall.isCanceled()) {
            currentAuditTrailCall.cancel();
        }
        if (currentUserReportCall != null && !currentUserReportCall.isCanceled()) {
            currentUserReportCall.cancel();
        }
    }
}
