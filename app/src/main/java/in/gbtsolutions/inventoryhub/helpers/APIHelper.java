package in.gbtsolutions.inventoryhub.helpers;

import android.content.Context;
import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.UUID;

public class APIHelper {

    private static final String CHARSET = "UTF-8";

    // ---------- GET ----------
    public static JSONObject get(String urlStr) throws IOException, JSONException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        return new JSONObject(readResponse(conn));
    }

    // ---------- POST (raw JSON body) ----------
    public static JSONObject post(String urlStr, JSONObject body) throws IOException, JSONException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "application/json; charset=" + CHARSET);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(CHARSET));
        }
        return new JSONObject(readResponse(conn));
    }

    // ---------- POST FORM (multipart, supports text fields + file Uris) ----------
    public static JSONObject postForm(Context context, String urlStr, Map<String, Object> params) throws IOException, JSONException {

        String boundary = "----APIHelperBoundary" + UUID.randomUUID().toString();
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

        try (DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof Uri) {
                    writeFilePart(out, context, boundary, key, (Uri) value);
                } else {
                    writeTextPart(out, boundary, key, String.valueOf(value));
                }
            }
            out.writeBytes("--" + boundary + "--\r\n");
        }

        return new JSONObject(readResponse(conn));
    }

    // ---------- DOWNLOAD FILE (CSV) ----------
    public static String downloadFile(Context context, String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);

        int code = conn.getResponseCode();
        if (code < 200 || code >= 300) {
            throw new IOException("Download failed, HTTP " + code);
        }

        String fileName = "file_" + System.currentTimeMillis() + ".csv";

        // context.getExternalFilesDir(null) -> /storage/emulated/0/Android/data/<package>/files
        File outFile = new File(context.getExternalFilesDir(null), fileName);

        try (InputStream is = conn.getInputStream();
             FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }

        return fileName;
    }

    // ---------- Helpers ----------
    private static void writeTextPart(DataOutputStream out, String boundary, String name, String value) throws IOException {
        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
        out.writeBytes(value + "\r\n");
    }

    private static void writeFilePart(DataOutputStream out, Context context, String boundary, String name, Uri uri) throws IOException {
        String fileName = getFileName(context, uri);
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null) mimeType = "application/octet-stream";

        out.writeBytes("--" + boundary + "\r\n");
        out.writeBytes("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + fileName + "\"\r\n");
        out.writeBytes("Content-Type: " + mimeType + "\r\n\r\n");

        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            while (is != null && (bytesRead = is.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
        out.writeBytes("\r\n");
    }

    private static String getFileName(Context context, Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx != -1) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result != null ? result : "file";
    }

    private static String readResponse(HttpURLConnection conn) throws IOException {
        int code = conn.getResponseCode();
        InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, CHARSET))) {
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }
}
