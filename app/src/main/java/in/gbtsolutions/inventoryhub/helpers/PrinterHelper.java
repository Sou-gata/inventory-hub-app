package in.gbtsolutions.inventoryhub.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import in.gbtsolutions.inventoryhub.InventoryHubApp;

public class PrinterHelper {
    public final static int PRNSTS_OK = 0;
    public final static int PRNSTS_OUT_OF_PAPER = -1;
    public final static int PRNSTS_NOT_SUPPORTED = -999;

    private final PrintingCallback mCallback;
    private final Context mContext;
    private boolean isSupported = false;
    private PrinterDriver mPrinterDriver;
    private HandlerThread mPrintThread;
    private Handler mPrintHandler;
    private Looper mPrintLooper;

    private final int PRINT_TEXT = 0;
    private final int PRINT_BITMAP = 1;
    private final int PRINT_BARCODE = 2;
    private final int PRINT_FORWARD = 3;
    private final int PRINT_PDF_PAGE = 4;
    private final int PRINT_PDF_ALL = 5;

    public PrinterHelper() {
        this(null, null);
    }

    public PrinterHelper(PrintingCallback callback) {
        this(null, callback);
    }

    public PrinterHelper(Context context, PrintingCallback callback) {
        this.mCallback = callback;
        this.mContext = resolveContext(context, callback);
        initPrinter(this.mContext);
    }

    private Context resolveContext(Context context, PrintingCallback callback) {
        if (context != null) {
            return context;
        }
        if (callback instanceof Context) {
            return (Context) callback;
        }
        try {
            return InventoryHubApp.getInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static boolean isDeviceSupported() {
        try {
            Class.forName("android.device.PrinterManager");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public boolean isPrinterSupported() {
        return isSupported;
    }

    private void initPrinter(Context context) {
        if (!isDeviceSupported()) {
            isSupported = false;
            showNotCompatibleToast(context);
            return;
        }

        try {
            mPrinterDriver = new HardwarePrinterDriver();
            isSupported = true;

            mPrintThread = new HandlerThread("PrinterWorkerThread");
            mPrintThread.start();
            mPrintLooper = mPrintThread.getLooper();
            mPrintHandler = new Handler(mPrintLooper) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case PRINT_TEXT:
                        case PRINT_BARCODE:
                        case PRINT_BITMAP:
                        case PRINT_PDF_PAGE:
                            doPrint(msg.what, msg.obj);
                            break;
                        case PRINT_PDF_ALL:
                            doPrintMultiplePages((Bitmap[]) msg.obj);
                            break;
                        case PRINT_FORWARD:
                            PrinterDriver driver = getPrinterDriver();
                            if (driver != null) {
                                driver.paperFeed(msg.arg1);
                                updatePrintStatus(100);
                            }
                            break;
                    }
                }
            };
        } catch (Throwable t) {
            isSupported = false;
            mPrinterDriver = null;
            showNotCompatibleToast(context);
        }
    }

    private void showNotCompatibleToast(Context context) {
        Context target = context != null ? context : resolveContext(null, null);
        if (target == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            try {
                Toast.makeText(target, "your device is not compatable with printing", Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
        });
    }

    private PrinterDriver getPrinterDriver() {
        if (!isSupported) {
            return null;
        }
        if (mPrinterDriver == null) {
            try {
                mPrinterDriver = new HardwarePrinterDriver();
            } catch (Throwable t) {
                isSupported = false;
                mPrinterDriver = null;
            }
        }
        return mPrinterDriver;
    }

    public void printText(String content, Bundle fontInfo) {
        if (!isSupported || mPrintHandler == null) return;
        Message msg = mPrintHandler.obtainMessage(PRINT_TEXT);
        msg.obj = new PrintJob<>(content, fontInfo);
        msg.sendToTarget();
    }

    public void printBitmap(Bitmap bitmap) {
        if (!isSupported || mPrintHandler == null) return;
        Message msg = mPrintHandler.obtainMessage(PRINT_BITMAP, bitmap);
        msg.sendToTarget();
    }

    public void printBitmap(Bitmap bitmap, Context context) {
        if (!isSupported) {
            showNotCompatibleToast(context != null ? context : mContext);
            return;
        }
        Context toastCtx = context != null ? context : mContext;
        if (toastCtx != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try {
                    Toast.makeText(toastCtx.getApplicationContext(), "Printing bill...", Toast.LENGTH_SHORT).show();
                } catch (Throwable ignored) {
                }
            });
        }
        printBitmap(bitmap);
    }

    public void printPdfPage(Bitmap bitmap) {
        if (!isSupported || mPrintHandler == null) return;
        Message msg = mPrintHandler.obtainMessage(PRINT_PDF_PAGE, bitmap);
        msg.sendToTarget();
    }

    public void printAllPdfPages(Bitmap[] bitmaps) {
        if (!isSupported || mPrintHandler == null) return;
        Message msg = mPrintHandler.obtainMessage(PRINT_PDF_ALL, bitmaps);
        msg.sendToTarget();
    }

    public void printBarcode(String content, int barcodeType) {
        if (!isSupported || mPrintHandler == null) return;
        Message msg = mPrintHandler.obtainMessage(PRINT_BARCODE);
        msg.obj = new PrintJob<>(content, barcodeType);
        msg.sendToTarget();
    }

    public void feedPaper(int lines) {
        if (!isSupported || mPrintHandler == null) return;
        mPrintHandler.obtainMessage(PRINT_FORWARD, lines, 0).sendToTarget();
    }

    public void setGrayLevel(int level) {
        if (!isSupported) return;
        PrinterDriver driver = getPrinterDriver();
        if (driver != null) {
            driver.setGrayLevel(level);
        }
    }

    public void setSpeedLevel(int level) {
        if (!isSupported) return;
        PrinterDriver driver = getPrinterDriver();
        if (driver != null) {
            driver.setSpeedLevel(level);
        }
    }

    public void destroy() {
        if (mPrinterDriver != null) {
            mPrinterDriver.close();
            mPrinterDriver = null;
        }
        if (mPrintThread != null) {
            mPrintThread.quitSafely();
            mPrintThread = null;
        }
    }

    @SuppressWarnings("unchecked")
    private void doPrint(int type, Object data) {
        PrinterDriver driver = getPrinterDriver();
        if (driver == null) {
            updatePrintStatus(-256);
            return;
        }

        int ret = driver.getStatus();
        if (ret != PRNSTS_OK) {
            updatePrintStatus(ret);
            return;
        }

        driver.setupPage(384, -1);
        switch (type) {
            case PRINT_TEXT:
                PrintJob<String, Bundle> textJob = (PrintJob<String, Bundle>) data;
                Bundle fontInfo = textJob.getData2();
                int fontSize = 24;
                int fontStyle = 0x0000;
                String fontName = "simsun";
                if (fontInfo != null) {
                    fontSize = fontInfo.getInt("font-size", 24);
                    fontStyle = fontInfo.getInt("font-style", 0);
                    fontName = fontInfo.getString("font-name", "simsun");
                }

                int height = 0;
                String[] texts = textJob.getData1().split("\n");
                for (String text : texts) {
                    height += driver.drawTextEx(text, 5, height, 384, -1, fontName, fontSize, 0, fontStyle, 0);
                }
                break;

            case PRINT_BARCODE:
                PrintJob<String, Integer> barcodeJob = (PrintJob<String, Integer>) data;
                String barcodeText = barcodeJob.getData1();
                int barcodeType = barcodeJob.getData2();
                driver.drawBarcode(barcodeText, 50, 10, barcodeType, 8, 120, 0);
                break;

            case PRINT_BITMAP:
            case PRINT_PDF_PAGE:
                Bitmap bitmap = (Bitmap) data;
                if (bitmap != null) {
                    Bitmap bwBitmap = prepareBitmapForPrint(bitmap);
                    if (bwBitmap != null) {
                        driver.drawBitmap(bwBitmap, 0, 0);
                    }
                }
                break;
        }

        ret = driver.printPage(0);
        driver.paperFeed(32);
        updatePrintStatus(ret);
    }

    private void doPrintMultiplePages(Bitmap[] bitmaps) {
        PrinterDriver driver = getPrinterDriver();
        if (driver == null) {
            updatePrintStatus(-256);
            return;
        }

        int ret = driver.getStatus();
        if (ret != PRNSTS_OK) {
            updatePrintStatus(ret);
            return;
        }

        for (int i = 0; i < bitmaps.length; i++) {
            if (bitmaps[i] != null) {
                driver.setupPage(384, -1);
                Bitmap bwBitmap = prepareBitmapForPrint(bitmaps[i]);
                if (bwBitmap != null) {
                    driver.drawBitmap(bwBitmap, 30, 0);
                }
                ret = driver.printPage(0);

                if (ret != PRNSTS_OK) {
                    updatePrintStatus(ret);
                    return;
                }

                if (i < bitmaps.length - 1) {
                    driver.paperFeed(16);
                }
            }
        }
        driver.paperFeed(16);
        updatePrintStatus(ret);
    }

    // Update status by calling the callback on the UI thread
    private void updatePrintStatus(final int status) {
        if (mCallback != null) {
            new Handler(Looper.getMainLooper()).post(() -> mCallback.onPrintFinished(status));
        }
    }

    private Bitmap prepareBitmapForPrint(Bitmap src) {
        if (src == null) return null;
        int targetWidth = 384;
        Bitmap scaled = src;
        if (src.getWidth() != targetWidth && src.getWidth() > 0) {
            int targetHeight = (int) ((float) src.getHeight() * ((float) targetWidth / (float) src.getWidth()));
            if (targetHeight <= 0) targetHeight = 1;
            scaled = Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true);
        }
        return convertToMonoBitmap(scaled);
    }

    private Bitmap convertToMonoBitmap(Bitmap src) {
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

    // Callback interface to communicate with the Activity
    public interface PrintingCallback {
        void onPrintFinished(int statusCode);
    }

    // Helper class to pass multiple data types in a message
    private static class PrintJob<T, E> {
        private final T data1;
        private final E data2;

        public PrintJob(T data1, E data2) {
            this.data1 = data1;
            this.data2 = data2;
        }

        public T getData1() {
            return data1;
        }

        public E getData2() {
            return data2;
        }
    }

    // --- Isolated Hardware Driver abstraction to prevent NoClassDefFoundError on non-POS devices ---

    private interface PrinterDriver {
        int getStatus();
        void setupPage(int width, int height);
        int printPage(int mode);
        void paperFeed(int lines);
        void setGrayLevel(int level);
        void setSpeedLevel(int level);
        int drawTextEx(String text, int x, int y, int width, int height, String fontName, int fontSize, int fontRotate, int fontStyle, int fontFormat);
        int drawBarcode(String text, int x, int y, int barcodeType, int width, int height, int rotate);
        int drawBitmap(Bitmap bitmap, int x, int y);
        void close();
    }

    private static class HardwarePrinterDriver implements PrinterDriver {
        private final android.device.PrinterManager mPrinterManager;

        HardwarePrinterDriver() throws Throwable {
            mPrinterManager = new android.device.PrinterManager();
            mPrinterManager.open();
        }

        @Override
        public int getStatus() {
            return mPrinterManager.getStatus();
        }

        @Override
        public void setupPage(int width, int height) {
            mPrinterManager.setupPage(width, height);
        }

        @Override
        public int printPage(int mode) {
            return mPrinterManager.printPage(mode);
        }

        @Override
        public void paperFeed(int lines) {
            mPrinterManager.paperFeed(lines);
        }

        @Override
        public void setGrayLevel(int level) {
            mPrinterManager.setGrayLevel(level);
        }

        @Override
        public void setSpeedLevel(int level) {
            mPrinterManager.setSpeedLevel(level);
        }

        @Override
        public int drawTextEx(String text, int x, int y, int width, int height, String fontName, int fontSize, int fontRotate, int fontStyle, int fontFormat) {
            return mPrinterManager.drawTextEx(text, x, y, width, height, fontName, fontSize, fontRotate, fontStyle, fontFormat);
        }

        @Override
        public int drawBarcode(String text, int x, int y, int barcodeType, int width, int height, int rotate) {
            return mPrinterManager.drawBarcode(text, x, y, barcodeType, width, height, rotate);
        }

        @Override
        public int drawBitmap(Bitmap bitmap, int x, int y) {
            return mPrinterManager.drawBitmap(bitmap, x, y);
        }

        @Override
        public void close() {
            try {
                mPrinterManager.close();
            } catch (Throwable ignored) {
            }
        }
    }
}
