package in.gbtsolutions.inventoryhub.helpers;


import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.util.Size;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.resolutionselector.ResolutionSelector;
import androidx.camera.core.resolutionselector.ResolutionStrategy;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;

import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class QRCodeHelper {

    private static final String TAG = "QRCodeHelper";
    private Camera camera;
    private boolean shouldEnableFlash = false;

    public static Bitmap createQRCode(String content, int size) {
        return createQRCode(content, size, 0);
    }

    public static Bitmap createQRCode(String content, int size, int margin) {
        try {
            QRCodeWriter writer = new QRCodeWriter();

            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, margin);

            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints);

            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? android.graphics.Color.BLACK : android.graphics.Color.WHITE);
                }
            }

            return bitmap;
        } catch (WriterException e) {
            return null;
        }
    }

    public static Bitmap createUPIQRCode(String upiId, String payeeName, String amount, String note, int size) {
        try {
            String uri = "upi://pay" + "?pa=" + upiId + "&pn=" + URLEncoder.encode(payeeName, "UTF-8") + "&am=" + amount + "&cu=INR" + "&tn=" + URLEncoder.encode(note, "UTF-8");
            return createQRCode(uri, size);
        } catch (Exception e) {
            return null;
        }
    }

    public interface ScanCallback {
        void onScanComplete(String result);

        void onScanCancelled();
    }

    private final AppCompatActivity activity;
    private final PreviewView previewView;
    private final ScanCallback callback;

    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private ExecutorService cameraExecutor;

    private QRCodeReader qrCodeReader;
    private Map<DecodeHintType, Object> decodeHints;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    public QRCodeHelper(AppCompatActivity activity, PreviewView previewView, ScanCallback callback) {
        this.activity = activity;
        this.previewView = previewView;
        this.callback = callback;
        initScanner();
    }

    private void initScanner() {
        cameraExecutor = Executors.newSingleThreadExecutor();
        cameraProviderFuture = ProcessCameraProvider.getInstance(activity);

        qrCodeReader = new QRCodeReader();
        decodeHints = new HashMap<>();
        decodeHints.put(DecodeHintType.POSSIBLE_FORMATS, Collections.singletonList(BarcodeFormat.QR_CODE));
        decodeHints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);

        requestPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        startCamera();
                    } else {
                        Toast.makeText(activity, "Camera permission is required to scan.", Toast.LENGTH_SHORT).show();
                        callback.onScanCancelled();
                    }
                });
    }

    public void startScanning() {
        isProcessing.set(false);
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    public void release() {
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    public void stopScanning() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
    }

    private void startCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                Size targetResolution = new Size(640, 480);
                ResolutionStrategy strategy = new ResolutionStrategy(
                        targetResolution,
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                );
                ResolutionSelector resolutionSelector = new ResolutionSelector.Builder()
                        .setResolutionStrategy(strategy)
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::processImageProxy);

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        activity,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

                if (shouldEnableFlash && camera.getCameraInfo().hasFlashUnit()) {
                    camera.getCameraControl().enableTorch(true);
                }

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed", e);
                callback.onScanCancelled();
            }
        }, ContextCompat.getMainExecutor(activity));
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processImageProxy(ImageProxy imageProxy) {
        if (isProcessing.get()) {
            imageProxy.close();
            return;
        }

        try (imageProxy) {
            Bitmap bitmap = imageProxy.toBitmap();

            int fullWidth = bitmap.getWidth();
            int fullHeight = bitmap.getHeight();

            int boxSize = (int) (Math.min(fullWidth, fullHeight) * 0.70);
            int left = (fullWidth - boxSize) / 2;
            int top = (fullHeight - boxSize) / 2;

            int[] pixels = new int[boxSize * boxSize];

            bitmap.getPixels(pixels, 0, boxSize, left, top, boxSize, boxSize);

            RGBLuminanceSource source = new RGBLuminanceSource(boxSize, boxSize, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = qrCodeReader.decode(binaryBitmap, decodeHints);

            if (result.getText() != null) {
                isProcessing.set(true);
                Log.i(TAG, "Barcode found: " + result.getText());

                activity.runOnUiThread(() -> {
                    stopScanning();
                    callback.onScanComplete(result.getText());
                });
            }

        } catch (Exception e) {
            Log.e(TAG, "ZXing scanning failed: " + e.getMessage(), e);
            isProcessing.set(false);
        } finally {
            qrCodeReader.reset();
        }
    }

    public void turnFlashOn() {
        shouldEnableFlash = true;
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(true);
        }
    }

    public void turnFlashOff() {
        shouldEnableFlash = false;
        if (camera != null && camera.getCameraInfo().hasFlashUnit()) {
            camera.getCameraControl().enableTorch(false);
        }
    }
}