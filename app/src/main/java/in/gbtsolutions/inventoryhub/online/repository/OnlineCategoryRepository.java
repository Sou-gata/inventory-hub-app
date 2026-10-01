package in.gbtsolutions.inventoryhub.online.repository;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.google.gson.Gson;

import java.util.List;

import in.gbtsolutions.inventoryhub.models.Category;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import in.gbtsolutions.inventoryhub.online.models.CategoryMapper;
import in.gbtsolutions.inventoryhub.online.models.OnlineCategoryDto;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OnlineCategoryRepository {

    public interface CategoryListCallback {
        void onSuccess(List<Category> categories);
        void onError(String errorMessage);
    }

    public interface CategoryActionCallback {
        void onSuccess(Category category);
        void onError(String errorMessage);
    }

    public interface CategoryDeleteCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Gson gson = new Gson();

    public OnlineCategoryRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    private OnlineApiService getApi() {
        return OnlineApiClient.getInstance().getApiService(context);
    }

    public void fetchCategories(CategoryListCallback callback) {
        getApi().getCategories().enqueue(new Callback<ApiResponse<List<OnlineCategoryDto>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<OnlineCategoryDto>>> call,
                                   Response<ApiResponse<List<OnlineCategoryDto>>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<List<OnlineCategoryDto>> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        List<Category> domainCategories = CategoryMapper.toDomainList(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(domainCategories);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Failed to load categories");
                        });
                        return;
                    }
                }
                handleError(response, callback, "Failed to load categories from server");
            }

            @Override
            public void onFailure(Call<ApiResponse<List<OnlineCategoryDto>>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                    }
                });
            }
        });
    }

    public void createCategory(Category category, CategoryActionCallback callback) {
        OnlineCategoryDto dto = CategoryMapper.toDto(category);
        getApi().createCategory(dto).enqueue(new Callback<ApiResponse<OnlineCategoryDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineCategoryDto>> call,
                                   Response<ApiResponse<OnlineCategoryDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineCategoryDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        Category created = CategoryMapper.toDomain(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(created);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Failed to create category");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to create category");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineCategoryDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                });
            }
        });
    }

    public void updateCategory(Category category, CategoryActionCallback callback) {
        OnlineCategoryDto dto = CategoryMapper.toDto(category);
        getApi().updateCategory(category.categoryId, dto).enqueue(new Callback<ApiResponse<OnlineCategoryDto>>() {
            @Override
            public void onResponse(Call<ApiResponse<OnlineCategoryDto>> call,
                                   Response<ApiResponse<OnlineCategoryDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<OnlineCategoryDto> body = response.body();
                    if (body.isSuccess() && body.getData() != null) {
                        Category updated = CategoryMapper.toDomain(body.getData());
                        mainHandler.post(() -> {
                            if (callback != null) callback.onSuccess(updated);
                        });
                        return;
                    } else {
                        String msg = body.getMessage();
                        mainHandler.post(() -> {
                            if (callback != null) callback.onError(!msg.isEmpty() ? msg : "Failed to update category");
                        });
                        return;
                    }
                }
                handleActionError(response, callback, "Failed to update category");
            }

            @Override
            public void onFailure(Call<ApiResponse<OnlineCategoryDto>> call, Throwable t) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError("Network error: " + (t.getMessage() != null ? t.getMessage() : "Timeout"));
                });
            }
        });
    }

    public void deleteCategory(int categoryId, CategoryDeleteCallback callback) {
        getApi().deleteCategory(categoryId).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    mainHandler.post(() -> {
                        if (callback != null) callback.onSuccess();
                    });
                } else {
                    String msg = "Failed to delete category";
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

    private void handleError(Response<?> response, CategoryListCallback callback, String defaultMsg) {
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

    private void handleActionError(Response<?> response, CategoryActionCallback callback, String defaultMsg) {
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
