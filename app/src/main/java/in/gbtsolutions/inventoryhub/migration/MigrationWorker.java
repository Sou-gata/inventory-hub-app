package in.gbtsolutions.inventoryhub.migration;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.gson.JsonObject;
import java.io.File;
import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.online.api.NetworkErrorHelper;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.config.AppMode;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Response;

public class MigrationWorker extends Worker {

    public static final String NOTIFICATION_CHANNEL_ID = "migration_channel";
    public static final int NOTIFICATION_ID = 9001;
    public static final String KEY_PROGRESS = "progress_message";
    public static final String KEY_ERROR = "error_message";

    public MigrationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        updateProgress("Preparing database snapshot...");

        File dumpGz = null;
        try {
            // 1. Create Compressed Snapshot using streaming GZIP (constant O(1) memory)
            dumpGz = DatabaseCompressor.createGzipSnapshot(context, (written, total) -> {
                int pct = total > 0 ? (int) ((written * 100) / total) : 0;
                updateProgress("Compressing database: " + pct + "%");
            });

            // 2. Upload to Server
            updateProgress("Uploading database snapshot to cloud...");
            RequestBody fileBody = RequestBody.create(MediaType.parse("application/gzip"), dumpGz);
            MultipartBody.Part part = MultipartBody.Part.createFormData("database", dumpGz.getName(), fileBody);

            OnlineApiService api = OnlineApiClient.getInstance().getApiService(context);
            Response<ApiResponse<JsonObject>> uploadRes = api.uploadMigrationDump(part).execute();

            if (!uploadRes.isSuccessful() || uploadRes.body() == null || !uploadRes.body().isSuccess()) {
                String errMsg = "Server upload failed (HTTP " + uploadRes.code() + ")";
                if (uploadRes.body() != null && uploadRes.body().getMessage() != null) {
                    errMsg = uploadRes.body().getMessage();
                }
                return Result.failure(new Data.Builder().putString(KEY_ERROR, errMsg).build());
            }

            JsonObject uploadData = uploadRes.body().getData();
            if (uploadData == null || !uploadData.has("jobId")) {
                return Result.failure(new Data.Builder().putString(KEY_ERROR, "Invalid server response: missing jobId").build());
            }
            String jobId = uploadData.get("jobId").getAsString();

            // 3. Poll for Status until COMPLETED or FAILED
            boolean completed = false;
            String serverMigrationStatus = null;
            while (!completed) {
                Thread.sleep(2500);
                Response<ApiResponse<JsonObject>> statusRes = api.getMigrationStatus(jobId).execute();

                if (statusRes.isSuccessful() && statusRes.body() != null) {
                    JsonObject data = statusRes.body().getData();
                    if (data != null) {
                        String status = data.has("status") ? data.get("status").getAsString() : "PROCESSING";
                        String progress = data.has("progress") ? data.get("progress").getAsString() : "";

                        updateProgress(progress);

                        if ("COMPLETED".equalsIgnoreCase(status)) {
                            completed = true;
                            if (data.has("migration_completed") && !data.get("migration_completed").isJsonNull()) {
                                serverMigrationStatus = data.get("migration_completed").getAsString();
                            }
                        } else if ("FAILED".equalsIgnoreCase(status)) {
                            String err = data.has("error") ? data.get("error").getAsString() : "Migration failed on server";
                            return Result.failure(new Data.Builder().putString(KEY_ERROR, err).build());
                        }
                    }
                }
            }

            // 4. Finalize Mode Switch & Update Migration Count in Android DB and Prefs
            AppModeManager.getInstance(context).setAppMode(AppMode.ONLINE);
            if (serverMigrationStatus != null && !serverMigrationStatus.trim().isEmpty()) {
                AppModeManager.getInstance(context).setMigrationStatus(serverMigrationStatus.trim());
            } else {
                AppModeManager.getInstance(context).incrementMigrationCount();
            }

            // 5. Invalidate Local DB and rename to backup
            Database.invalidateInstance();
            File originalDb = context.getDatabasePath("app_database");
            File backupDb = new File(context.getFilesDir(), "app_database_archived.db");
            if (originalDb.exists()) {
                if (backupDb.exists()) backupDb.delete();
                originalDb.renameTo(backupDb);
            }

            return Result.success();

        } catch (Exception e) {
            return Result.failure(new Data.Builder().putString(KEY_ERROR, NetworkErrorHelper.getErrorMessage(e, e.getMessage())).build());
        } finally {
            if (dumpGz != null && dumpGz.exists()) {
                dumpGz.delete();
            }
        }
    }

    private void updateProgress(String message) {
        setProgressAsync(new Data.Builder().putString(KEY_PROGRESS, message).build());
        try {
            setForegroundAsync(createForegroundInfo(message));
        } catch (Exception ignored) {
        }
    }

    private ForegroundInfo createForegroundInfo(String statusText) {
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(getApplicationContext(), NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Cloud Migration")
                .setContentText(statusText)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build();
        return new ForegroundInfo(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Database Migration",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getApplicationContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
