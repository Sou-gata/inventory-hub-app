package in.gbtsolutions.inventoryhub.backup;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class BackupFileFormat {

    public static final byte[] MAGIC = {'I', 'H', 'B', 'K'};
    public static final byte VERSION = 0x01;

    public static final byte[] SQLITE_MAGIC = {
            0x53, 0x51, 0x4C, 0x69, 0x74, 0x65, 0x20, 0x66,
            0x6F, 0x72, 0x6D, 0x61, 0x74, 0x20, 0x33, 0x00
    };

    public static class ParsedBackup {
        public final byte[] salt;
        public final byte[] iv;
        public final byte[] ciphertext;

        public ParsedBackup(@NonNull byte[] salt, @NonNull byte[] iv, @NonNull byte[] ciphertext) {
            this.salt = salt;
            this.iv = iv;
            this.ciphertext = ciphertext;
        }
    }

    public static void write(@NonNull OutputStream out, @NonNull byte[] salt, @NonNull byte[] iv, @NonNull byte[] ciphertext) throws IOException {
        out.write(MAGIC);
        out.write(VERSION);
        out.write(salt);
        out.write(iv);
        out.write(ciphertext);
        out.flush();
    }

    @NonNull
    public static ParsedBackup read(@NonNull InputStream in) throws IOException {
        byte[] magic = new byte[MAGIC.length];
        readFully(in, magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new IOException("INVALID_MAGIC");
        }

        int version = in.read();
        if (version != (VERSION & 0xFF)) {
            throw new IOException("UNSUPPORTED_VERSION");
        }

        byte[] salt = new byte[BackupCrypto.SALT_LENGTH];
        readFully(in, salt);

        byte[] iv = new byte[BackupCrypto.IV_LENGTH];
        readFully(in, iv);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, bytesRead);
        }

        byte[] ciphertext = buffer.toByteArray();
        if (ciphertext.length == 0) {
            throw new IOException("EMPTY_CIPHERTEXT");
        }

        return new ParsedBackup(salt, iv, ciphertext);
    }

    public static boolean isValidSQLite(@NonNull byte[] data) {
        if (data.length < SQLITE_MAGIC.length) {
            return false;
        }
        for (int i = 0; i < SQLITE_MAGIC.length; i++) {
            if (data[i] != SQLITE_MAGIC[i]) {
                return false;
            }
        }
        return true;
    }

    private static void readFully(@NonNull InputStream in, @NonNull byte[] buffer) throws IOException {
        int bytesRead = 0;
        while (bytesRead < buffer.length) {
            int read = in.read(buffer, bytesRead, buffer.length - bytesRead);
            if (read == -1) {
                throw new IOException("UNEXPECTED_EOF");
            }
            bytesRead += read;
        }
    }
}
