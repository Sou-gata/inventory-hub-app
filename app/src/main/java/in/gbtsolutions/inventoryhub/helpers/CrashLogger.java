package in.gbtsolutions.inventoryhub.helpers;

import android.app.Application;
import android.content.ContentValues;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public class CrashLogger {

    private static final int MAX_LOG_FILES = 20;
    private static volatile boolean isInitialized = false;
    private static Thread.UncaughtExceptionHandler defaultHandler;

    public static synchronized void init(Application application) {
        if (isInitialized) {
            return;
        }
        isInitialized = true;
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try {
                saveCrashLog(application, thread, throwable);
            } catch (Throwable t) {
                t.printStackTrace();
            } finally {
                if (defaultHandler != null) {
                    defaultHandler.uncaughtException(thread, throwable);
                }
            }
        });
    }

    private static void saveCrashLog(Application application, Thread thread, Throwable throwable) {
        try {
            StringWriter stackTrace = new StringWriter();
            throwable.printStackTrace(new PrintWriter(stackTrace));

            String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());

            String versionName = "Unknown";
            long versionCode = 0;
            try {
                PackageInfo pInfo;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    pInfo = application.getPackageManager().getPackageInfo(application.getPackageName(), PackageManager.PackageInfoFlags.of(0));
                } else {
                    //noinspection deprecation
                    pInfo = application.getPackageManager().getPackageInfo(application.getPackageName(), 0);
                }
                versionName = pInfo.versionName;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    versionCode = pInfo.getLongVersionCode();
                } else {
                    //noinspection deprecation
                    versionCode = pInfo.versionCode;
                }
            } catch (PackageManager.NameNotFoundException ignored) {
            }

            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / (1024 * 1024);
            long totalMemory = runtime.totalMemory() / (1024 * 1024);
            long freeMemory = runtime.freeMemory() / (1024 * 1024);

            String log = "CRASH REPORT\n" + "Time: " + time + "\n" + "App Version: " + versionName + " (" + versionCode + ")\n" + "Device: " + Build.MANUFACTURER + " " + Build.MODEL + "\n" + "Android Version: " + Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")\n" + "Thread: " + thread.getName() + "\n" + "Memory (MB): Free: " + freeMemory + " / Total: " + totalMemory + " / Max: " + maxMemory + "\n\n" + stackTrace;

            String fileName = "crash_log_" + new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date()) + ".txt";

            boolean savedSuccessfully = false;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                savedSuccessfully = saveToDownloadsMediaStore(application, fileName, log);
            } else {
                savedSuccessfully = saveToDownloadsPreQ(fileName, log);
            }

            if (!savedSuccessfully) {
                saveToFallback(application, fileName, log);
            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static boolean saveToDownloadsMediaStore(Application application, String fileName, String log) {
        Uri uri = null;
        try {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Inventory Hub/logs");
            values.put(MediaStore.Downloads.IS_PENDING, 1);

            uri = application.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                return false;
            }

            try (OutputStream output = application.getContentResolver().openOutputStream(uri)) {
                if (output == null) {
                    return false;
                }
                output.write(log.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }

            ContentValues completed = new ContentValues();
            completed.put(MediaStore.Downloads.IS_PENDING, 0);
            application.getContentResolver().update(uri, completed, null, null);
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            if (uri != null) {
                try {
                    application.getContentResolver().delete(uri, null, null);
                } catch (Throwable ignored) {
                }
            }
            return false;
        }
    }

    private static boolean saveToDownloadsPreQ(String fileName, String log) {
        try {
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File folder = new File(downloads, "Inventory Hub/logs");

            if (!folder.exists() && !folder.mkdirs()) {
                return false;
            }

            pruneOldLogs(folder);

            File file = new File(folder, fileName);
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(log.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
            return true;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    private static void saveToFallback(Application application, String fileName, String log) {
        try {
            File logDir = application.getExternalFilesDir("crash_logs");
            if (logDir == null) {
                logDir = new File(application.getFilesDir(), "crash_logs");
            }

            if (!logDir.exists() && !logDir.mkdirs()) {
                return;
            }

            pruneOldLogs(logDir);

            File logFile = new File(logDir, fileName);
            try (FileOutputStream output = new FileOutputStream(logFile)) {
                output.write(log.getBytes(StandardCharsets.UTF_8));
                output.flush();
            }
        } catch (Throwable ignored) {
        }
    }

    private static void pruneOldLogs(File logDir) {
        File[] files = logDir.listFiles((dir, name) -> name.startsWith("crash_log_") && name.endsWith(".txt"));
        if (files != null && files.length >= MAX_LOG_FILES) {
            Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));
            int filesToDelete = files.length - MAX_LOG_FILES + 1;
            for (int i = 0; i < filesToDelete; i++) {
                files[i].delete();
            }
        }
    }
}


