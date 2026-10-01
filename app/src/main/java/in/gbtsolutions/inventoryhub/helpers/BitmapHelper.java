package in.gbtsolutions.inventoryhub.helpers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BitmapHelper {
    public static Bitmap createBitmapFromView(View view, int width, int height) {
        int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
        int heightSpec;
        if (height == 0) {
            heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        } else {
            heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
        }
        view.measure(widthSpec, heightSpec);
        view.layout(0, 0, view.getMeasuredWidth(), view.getMeasuredHeight());
        Bitmap bitmap = Bitmap.createBitmap(view.getMeasuredWidth(), view.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        view.draw(canvas);
        return bitmap;
    }

    public static void saveBitmapToMediaStore(Bitmap bitmap, Context context) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "BILL_" + timeStamp + ".jpg";
        saveBitmapToMediaStore(bitmap, context, fileName);
    }

    public static Uri saveBitmapToMediaStore(Bitmap bitmap, Context context, String fileName) {
        if (bitmap == null || context == null) return null;
        ContentResolver resolver = context.getContentResolver();
        Uri imageUri = null;

        try {
            boolean isPng = fileName != null && fileName.toLowerCase().endsWith(".png");
            String mimeType = isPng ? "image/png" : "image/jpeg";
            Bitmap.CompressFormat format = isPng ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG;
            int quality = isPng ? 100 : 95;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/InventoryHub");

                imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri == null) {
                    throw new IOException("Failed to create new MediaStore record.");
                }
                try (OutputStream stream = resolver.openOutputStream(imageUri)) {
                    if (stream == null || !bitmap.compress(format, quality, stream)) {
                        throw new IOException("Failed to save bitmap.");
                    }
                }
            } else {
                java.io.File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                java.io.File appDir = new java.io.File(picturesDir, "InventoryHub");
                if (!appDir.exists()) {
                    appDir.mkdirs();
                }
                java.io.File imageFile = new java.io.File(appDir, fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(imageFile)) {
                    if (!bitmap.compress(format, quality, fos)) {
                        throw new IOException("Failed to save bitmap.");
                    }
                }
                imageUri = Uri.fromFile(imageFile);
                android.media.MediaScannerConnection.scanFile(context,
                        new String[]{imageFile.getAbsolutePath()},
                        new String[]{mimeType}, null);
            }
            Toast.makeText(context, "Bill saved to gallery", Toast.LENGTH_SHORT).show();
            return imageUri;
        } catch (Exception e) {
            if (imageUri != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                try {
                    resolver.delete(imageUri, null, null);
                } catch (Exception ignored) {}
            }
            e.printStackTrace();
            Toast.makeText(context, "Failed to save bill to gallery", Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    public static File saveBitmapAsPng(Context context, Bitmap bitmap, String fileName) {
        if (bitmap == null || context == null) return null;
        try {
            File dir = new File(context.getFilesDir(), "bills");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "bill_" + System.currentTimeMillis() + ".png";
            } else if (!fileName.toLowerCase().endsWith(".png")) {
                fileName = fileName + ".png";
            }
            File file = new File(dir, fileName);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            }
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Uri saveBitmapToDownloads(Bitmap bitmap, Context context, String fileName) {
        ContentResolver resolver = context.getContentResolver();
        Uri fileUri = null;

        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                fileUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
                if (fileUri == null) {
                    throw new IOException("Failed to create MediaStore record in Downloads.");
                }
                try (OutputStream stream = resolver.openOutputStream(fileUri)) {
                    if (stream == null || !bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw new IOException("Failed to write bitmap to Downloads.");
                    }
                }
            } else {
                java.io.File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs();
                }
                java.io.File file = new java.io.File(downloadDir, fileName);
                try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                        throw new IOException("Failed to compress bitmap to file.");
                    }
                }
                fileUri = Uri.fromFile(file);
            }
            return fileUri;
        } catch (Exception e) {
            if (fileUri != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                try {
                    resolver.delete(fileUri, null, null);
                } catch (Exception ignored) {}
            }
            e.printStackTrace();
            return null;
        }
    }

    public static Bitmap scaleBitmapToWidth(Bitmap originalBitmap, int newWidth) {
        int originalWidth = originalBitmap.getWidth();
        int originalHeight = originalBitmap.getHeight();
        float aspectRatio = (float) originalHeight / (float) originalWidth;
        int newHeight = (int) Math.ceil(newWidth * aspectRatio);
        return Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true);
    }

    public static Bitmap convertToMonoBitmap(Bitmap src) {
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap bwBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        int threshold = 128;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = src.getPixel(x, y);

                // Convert to grayscale
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                int gray = (r + g + b) / 3;

                if (gray < threshold) {
                    bwBitmap.setPixel(x, y, 0xFF000000); // black
                } else {
                    bwBitmap.setPixel(x, y, 0xFFFFFFFF); // white
                }
            }
        }
        return bwBitmap;
    }

    public static int convertDpToPx(Context context, float dp) {
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, context.getResources().getDisplayMetrics());
    }

    public static void shareBitmap(Context context, Bitmap bitmap, String title, String subject) {
        if (context == null || bitmap == null) return;
        try {
            File cachePath = new File(context.getCacheDir(), "shared_qr");
            if (!cachePath.exists()) cachePath.mkdirs();

            File imageFile = new File(cachePath, "qr_" + System.currentTimeMillis() + ".png");
            try (FileOutputStream stream = new FileOutputStream(imageFile)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            }

            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", imageFile);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            if (subject != null) {
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                shareIntent.putExtra(Intent.EXTRA_TEXT, subject);
            }
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(shareIntent, title != null ? title : "Share");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(context, "Unable to share image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public static void shareImageFile(Context context, File file, String title, String subject) {
        if (context == null || file == null || !file.exists()) return;
        try {
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/png");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            if (subject != null) {
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                shareIntent.putExtra(Intent.EXTRA_TEXT, subject);
            }
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooser = Intent.createChooser(shareIntent, title != null ? title : "Share Bill");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(context, "Unable to share bill: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
