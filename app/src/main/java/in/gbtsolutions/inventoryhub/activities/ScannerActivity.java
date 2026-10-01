package in.gbtsolutions.inventoryhub.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.QRCodeHelper;

public class ScannerActivity extends AppCompatActivity {

    public static final String SCAN_RESULT = "scan_result";
    boolean isFlashOn = false;
    private QRCodeHelper qrCodeHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scanner);

        PreviewView previewView = findViewById(R.id.scannerPreviewView);

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isFlashOn) qrCodeHelper.turnFlashOn();
        qrCodeHelper.startScanning();
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