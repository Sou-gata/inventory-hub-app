package in.gbtsolutions.inventoryhub.backup;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.AEADBadTagException;

import in.gbtsolutions.inventoryhub.Database;

public class BackupManager {

    private static final String DATABASE_NAME = "app_database";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    public interface BackupCallback {
        void onSuccess(@NonNull File backupFile);
        void onError(@NonNull String errorMessage);
    }

    public interface RestoreCallback {
        void onSuccess();
        void onError(@NonNull String errorMessage);
    }

    public static void createBackup(@NonNull Context context, @Nullable String userPassword, @NonNull BackupCallback callback) {
        final Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                // 1. Resolve password
                String effectivePassword = BackupCrypto.resolvePassword(userPassword);

                // 2. Checkpoint WAL and close Room Database cleanly to commit all transactions to disk
                try {
                    Database db = Database.getInstance(appContext);
                    if (db.isOpen()) {
                        db.getOpenHelper().getWritableDatabase().query("PRAGMA wal_checkpoint(FULL)").close();
                    }
                } catch (Exception ignored) {
                } finally {
                    Database.invalidateInstance();
                }

                // 3. Read the database file
                File dbFile = appContext.getDatabasePath(DATABASE_NAME);
                if (!dbFile.exists() || dbFile.length() == 0) {
                    postBackupError(callback, "Database file not found or is empty.");
                    return;
                }

                byte[] dbBytes = readFileToByteArray(dbFile);

                // 4. Encrypt using AES-256-GCM + PBKDF2
                BackupCrypto.EncryptedPayload payload = BackupCrypto.encrypt(dbBytes, effectivePassword);

                // 5. Ensure target backup directory exists ('Download/Inventory Hub/Backup')
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File backupDir = new File(downloadsDir, "Inventory Hub" + File.separator + "Backup");
                if (!backupDir.exists() && !backupDir.mkdirs()) {
                    File appExtDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                    if (appExtDir != null) {
                        backupDir = new File(appExtDir, "Inventory Hub" + File.separator + "Backup");
                    } else {
                        backupDir = new File(appContext.getFilesDir(), "Inventory Hub" + File.separator + "Backup");
                    }
                    if (!backupDir.exists()) {
                        backupDir.mkdirs();
                    }
                }

                // 6. Generate filename with timestamp
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "InventoryHub_Backup_" + timeStamp + ".st";

                File destFile = new File(backupDir, fileName);
                if (destFile.getParentFile() != null && !destFile.getParentFile().exists()) {
                    destFile.getParentFile().mkdirs();
                }

                // 7. Write header + payload
                try (OutputStream os = new FileOutputStream(destFile)) {
                    BackupFileFormat.write(os, payload.salt, payload.iv, payload.ciphertext);
                }

                // 8. Inform media scanner so user and other apps can immediately see the file
                MediaScannerConnection.scanFile(appContext, new String[]{destFile.getAbsolutePath()}, null, null);

                postBackupSuccess(callback, destFile);

            } catch (Exception e) {
                postBackupError(callback, e.getMessage() != null ? e.getMessage() : "Error creating backup");
            }
        });
    }

    public static void restoreBackup(@NonNull Context context, @NonNull Uri backupUri, @Nullable String userPassword, @NonNull RestoreCallback callback) {
        final Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            try {
                // 1. Resolve password
                String effectivePassword = BackupCrypto.resolvePassword(userPassword);

                // 2. Read and parse binary backup file
                BackupFileFormat.ParsedBackup parsed;
                try (InputStream is = appContext.getContentResolver().openInputStream(backupUri)) {
                    if (is == null) {
                        postRestoreError(callback, "Cannot open selected backup file.");
                        return;
                    }
                    parsed = BackupFileFormat.read(is);
                } catch (Exception e) {
                    postRestoreError(callback, "INVALID_FORMAT");
                    return;
                }

                // 3. Decrypt payload
                byte[] decryptedBytes;
                try {
                    decryptedBytes = BackupCrypto.decrypt(parsed.ciphertext, effectivePassword, parsed.salt, parsed.iv);
                } catch (AEADBadTagException e) {
                    postRestoreError(callback, "WRONG_PASSWORD");
                    return;
                } catch (Exception e) {
                    postRestoreError(callback, "WRONG_PASSWORD");
                    return;
                }

                // 4. Verify decrypted content is a valid SQLite DB
                if (!BackupFileFormat.isValidSQLite(decryptedBytes)) {
                    postRestoreError(callback, "INVALID_DB");
                    return;
                }

                // 5. Close existing Room connection and invalidate instance
                Database.invalidateInstance();

                // 6. Overwrite database file and remove auxiliary WAL/SHM files
                File dbFile = appContext.getDatabasePath(DATABASE_NAME);
                if (dbFile.getParentFile() != null && !dbFile.getParentFile().exists()) {
                    dbFile.getParentFile().mkdirs();
                }

                File walFile = new File(dbFile.getParentFile(), DATABASE_NAME + "-wal");
                File shmFile = new File(dbFile.getParentFile(), DATABASE_NAME + "-shm");

                if (walFile.exists()) {
                    walFile.delete();
                }
                if (shmFile.exists()) {
                    shmFile.delete();
                }

                try (FileOutputStream fos = new FileOutputStream(dbFile, false)) {
                    fos.write(decryptedBytes);
                    fos.flush();
                    fos.getFD().sync();
                }

                postRestoreSuccess(callback);

            } catch (Exception e) {
                postRestoreError(callback, e.getMessage() != null ? e.getMessage() : "Error restoring backup");
            }
        });
    }

    private static byte[] readFileToByteArray(@NonNull File file) throws Exception {
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream((int) file.length())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
            return bos.toByteArray();
        }
    }

    private static void postBackupSuccess(@NonNull BackupCallback callback, @NonNull File file) {
        MAIN_HANDLER.post(() -> callback.onSuccess(file));
    }

    private static void postBackupError(@NonNull BackupCallback callback, @NonNull String error) {
        MAIN_HANDLER.post(() -> callback.onError(error));
    }

    private static void postRestoreSuccess(@NonNull RestoreCallback callback) {
        MAIN_HANDLER.post(callback::onSuccess);
    }

    private static void postRestoreError(@NonNull RestoreCallback callback, @NonNull String error) {
        MAIN_HANDLER.post(() -> callback.onError(error));
    }
}
