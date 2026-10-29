package in.gbtsolutions.inventoryhub.helpers;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;

import java.util.HashMap;
import java.util.Map;

public class BarcodeHelper {
    private static final String TAG = "BarcodeHelper";

    public static Bitmap createBarcode(String content, int width, int height) {
        return createBarcode(content, width, height, 10);
    }

    public static Bitmap createBarcode(String content, int width, int height, int margin) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        try {
            Code128Writer writer = new Code128Writer();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, margin);

            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, width, height, hints);

            int bmWidth = bitMatrix.getWidth();
            int bmHeight = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(bmWidth, bmHeight, Bitmap.Config.RGB_565);

            for (int x = 0; x < bmWidth; x++) {
                for (int y = 0; y < bmHeight; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }

            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Barcode creation failed: " + e.getMessage(), e);
            return null;
        }
    }
}

