package in.gbtsolutions.inventoryhub.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import in.gbtsolutions.inventoryhub.Configurations;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.QRCodeHelper;
import in.gbtsolutions.inventoryhub.views.ScannerOverlayView;

public class ScannerActivity extends AppCompatActivity {

    public static final String SCAN_RESULT = "scan_result";
    private boolean isFlashOn = false;
    private QRCodeHelper qrCodeHelper;
    private ScannerOverlayView scannerOverlayView;
    private ImageView btnFlash;
    private TextView tvTitle;
    private TextView tvHint;
    private TextView tabQr;
    private TextView tabBarcode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scanner);

        PreviewView previewView = findViewById(R.id.scannerPreviewView);
        scannerOverlayView = findViewById(R.id.scannerOverlayView);
        View btnBack = findViewById(R.id.btn_scanner_back);
        btnFlash = findViewById(R.id.btn_scanner_flash);
        tvTitle = findViewById(R.id.tv_scanner_title);
        tvHint = findViewById(R.id.tv_scanner_hint);
        tabQr = findViewById(R.id.tab_scan_qr);
        tabBarcode = findViewById(R.id.tab_scan_barcode);

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                setResult(RESULT_CANCELED);
                if (isFlashOn && qrCodeHelper != null) qrCodeHelper.turnFlashOff();
                finish();
            });
        }

        if (btnFlash != null) {
            btnFlash.setOnClickListener(v -> toggleFlash());
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }

        qrCodeHelper = new QRCodeHelper(this, previewView, new QRCodeHelper.ScanCallback() {
            @Override
            public void onScanComplete(String result) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(SCAN_RESULT, result);
                setResult(RESULT_OK, resultIntent);
                if (isFlashOn) qrCodeHelper.turnFlashOff();
                finish();
            }

            @Override
            public void onScanCancelled() {
                setResult(RESULT_CANCELED);
                if (isFlashOn) qrCodeHelper.turnFlashOff();
                finish();
            }
        });

        // Initialize mode based on global settings
        String defaultFormat = GlobalStore.getInstance().getCodeFormat();
        boolean isBarcodeDefault = Configurations.CODE_FORMAT_BARCODE.equalsIgnoreCase(defaultFormat);
        setScannerMode(isBarcodeDefault ? ScannerOverlayView.MODE_BARCODE : ScannerOverlayView.MODE_QR);

        if (tabQr != null) {
            tabQr.setOnClickListener(v -> setScannerMode(ScannerOverlayView.MODE_QR));
        }

        if (tabBarcode != null) {
            tabBarcode.setOnClickListener(v -> setScannerMode(ScannerOverlayView.MODE_BARCODE));
        }
    }

    private void setScannerMode(int mode) {
        if (scannerOverlayView != null) {
            scannerOverlayView.setMode(mode);
        }
        if (qrCodeHelper != null) {
            qrCodeHelper.setScanMode(mode == ScannerOverlayView.MODE_BARCODE ? QRCodeHelper.MODE_BARCODE : QRCodeHelper.MODE_QR);
        }

        boolean isBarcode = (mode == ScannerOverlayView.MODE_BARCODE);

        if (tvTitle != null) {
            tvTitle.setText(isBarcode ? R.string.scan_barcode : R.string.scan_qr_code);
        }
        if (tvHint != null) {
            tvHint.setText(isBarcode ? R.string.align_barcode_hint : R.string.align_qr_hint);
        }

        if (tabQr != null && tabBarcode != null) {
            if (isBarcode) {
                tabBarcode.setBackgroundResource(R.drawable.bg_status_chip);
                tabBarcode.setTextColor(ContextCompat.getColor(this, R.color.material_blue));
                tabBarcode.setTypeface(null, Typeface.BOLD);

                tabQr.setBackground(null);
                tabQr.setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
                tabQr.setTypeface(null, Typeface.NORMAL);
            } else {
                tabQr.setBackgroundResource(R.drawable.bg_status_chip);
                tabQr.setTextColor(ContextCompat.getColor(this, R.color.material_blue));
                tabQr.setTypeface(null, Typeface.BOLD);

                tabBarcode.setBackground(null);
                tabBarcode.setTextColor(ContextCompat.getColor(this, R.color.fg_muted));
                tabBarcode.setTypeface(null, Typeface.NORMAL);
            }
        }
    }

    private void toggleFlash() {
        isFlashOn = !isFlashOn;
        if (qrCodeHelper != null) {
            if (isFlashOn) {
                qrCodeHelper.turnFlashOn();
            } else {
                qrCodeHelper.turnFlashOff();
            }
        }
        if (btnFlash != null) {
            btnFlash.setImageResource(isFlashOn ? R.drawable.ic_flash_on : R.drawable.ic_flash_off);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFlashOn && qrCodeHelper != null) qrCodeHelper.turnFlashOn();
        if (qrCodeHelper != null) qrCodeHelper.startScanning();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (qrCodeHelper != null) {
            qrCodeHelper.release();
        }
    }

    @Override
    public void finish() {
        super.finish();
        BaseActivity.applyCloseTransition(this);
    }
}
