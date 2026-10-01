package in.gbtsolutions.inventoryhub.online.api;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class OnlineApiClient {

    private static volatile OnlineApiClient instance;
    private Retrofit retrofit;
    private String currentBaseUrl;
    private OnlineApiService apiService;

    private OnlineApiClient() {
    }

    public static OnlineApiClient getInstance() {
        if (instance == null) {
            synchronized (OnlineApiClient.class) {
                if (instance == null) {
                    instance = new OnlineApiClient();
                }
            }
        }
        return instance;
    }

    public synchronized OnlineApiService getApiService(Context context) {
        String baseUrl = AppModeManager.getInstance(context).getServerUrl();
        if (retrofit == null || !baseUrl.equalsIgnoreCase(currentBaseUrl) || apiService == null) {
            this.currentBaseUrl = baseUrl;

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .addInterceptor(chain -> {
                        Request original = chain.request();
                        Request.Builder builder = original.newBuilder()
                                .header("Accept", "application/json")
                                .header("Content-Type", "application/json");

                        // Dynamically retrieve bearer token saved in SharedPreferences
                        String token = AppModeManager.getInstance(context).getAuthToken();
                        if (token != null && !token.trim().isEmpty()) {
                            builder.header("Authorization", "Bearer " + token.trim());
                        }

                        return chain.proceed(builder.build());
                    })
                    .addInterceptor(logging)
                    .build();

            this.retrofit = new Retrofit.Builder()
                    .baseUrl(currentBaseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            this.apiService = retrofit.create(OnlineApiService.class);
        }
        return apiService;
    }

    public synchronized void resetClient() {
        this.retrofit = null;
        this.apiService = null;
    }
}
