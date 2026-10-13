package in.gbtsolutions.inventoryhub.migration;

import android.content.Context;
import androidx.sqlite.db.SupportSQLiteDatabase;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPOutputStream;
import in.gbtsolutions.inventoryhub.Database;

public class DatabaseCompressor {

    public interface CompressionProgressListener {
        void onProgress(long bytesWritten, long totalBytes);
    }

    public static File createGzipSnapshot(Context context, CompressionProgressListener listener) throws IOException {
        Database db = Database.getInstance(context);

        // 1. Force full checkpoint so dirty WAL cache merges into app_database
        SupportSQLiteDatabase sqlDb = db.getOpenHelper().getWritableDatabase();
        sqlDb.query("PRAGMA wal_checkpoint(FULL)").close();

        File dbFile = context.getDatabasePath("app_database");
        if (!dbFile.exists()) {
            throw new IOException("Database file does not exist: " + dbFile.getAbsolutePath());
        }

        long totalBytes = dbFile.length();
        File outputFile = new File(context.getCacheDir(), "offline_migration_dump.db.gz");
        if (outputFile.exists()) {
            outputFile.delete();
        }

        // 2. Stream compress using GZIP with fixed 8KB buffer (constant O(1) memory)
        try (FileInputStream fis = new FileInputStream(dbFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             GZIPOutputStream gzos = new GZIPOutputStream(fos)) {

            byte[] buffer = new byte[8192];
            int read;
            long written = 0;

            while ((read = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, read);
                written += read;
                if (listener != null) {
                    listener.onProgress(written, totalBytes);
                }
            }
            gzos.finish();
        }

        return outputFile;
    }
}
