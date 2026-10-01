package in.gbtsolutions.inventoryhub.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;

import in.gbtsolutions.inventoryhub.Configurations;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.BitmapHelper;
import in.gbtsolutions.inventoryhub.helpers.PrinterHelper;

public class BillViewerActivity extends BaseActivity {

    public static final String EXTRA_IMAGE_PATH = "extra_image_path";
    public static final String EXTRA_BILL_TITLE = "extra_bill_title";
    public static final String EXTRA_FILE_NAME = "extra_file_name";

    private PhotoView photoView;
    private ProgressBar progressLoading;
    private View layoutError;
    private TextView textErrorMessage;

    private String imagePath;
    private String billTitle;
    private String fileName;
    private File currentFile;
    private Bitmap currentBitmap;

    public static void start(Context context, String imagePath, String title, String fileName) {
        if (context == null || TextUtils.isEmpty(imagePath)) return;
        Intent intent = new Intent(context, BillViewerActivity.class);
        intent.putExtra(EXTRA_IMAGE_PATH, imagePath);
        intent.putExtra(EXTRA_BILL_TITLE, title);
        intent.putExtra(EXTRA_FILE_NAME, fileName);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    public static void openBill(Context context, Bitmap billBitmap, String fileName, String title, boolean saveToGallery, boolean printBill) {
        if (context == null || billBitmap == null) return;

        // 1. Save PNG to app storage for viewing and sharing
        File pngFile = BitmapHelper.saveBitmapAsPng(context, billBitmap, fileName);

        // 2. If configured, also save to MediaStore Gallery
        if (saveToGallery) {
            BitmapHelper.saveBitmapToMediaStore(billBitmap, context, fileName);
        }

        // 3. If configured, send to printer
        boolean shouldPrint = printBill || GlobalStore.getInstance().isPrintBill()
                || context.getSharedPreferences(Configurations.PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(Configurations.KEY_PRINT_BILL, false);

        if (shouldPrint) {
            PrinterHelper printerHelper = new PrinterHelper(context, null);
            printerHelper.printBitmap(billBitmap, context);
        }

        // 4. Open PNG inside PhotoView activity
        if (pngFile != null && pngFile.exists()) {
            start(context, pngFile.getAbsolutePath(), title, fileName);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bill_viewer);

        imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        billTitle = getIntent().getStringExtra(EXTRA_BILL_TITLE);
        fileName = getIntent().getStringExtra(EXTRA_FILE_NAME);

        if (TextUtils.isEmpty(billTitle)) {
            billTitle = getString(R.string.title_bill_preview);
        }
        if (TextUtils.isEmpty(fileName)) {
            fileName = "bill_" + System.currentTimeMillis() + ".png";
        }

        View headerContainer = findViewById(R.id.header_container);
        View contentContainer = findViewById(R.id.content_container);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, billTitle, true);
        applyEdgeToEdgeInsets(headerContainer, contentContainer);

        photoView = findViewById(R.id.photo_view);
        progressLoading = findViewById(R.id.progress_loading);
        layoutError = findViewById(R.id.layout_error);
        textErrorMessage = findViewById(R.id.text_error_message);

        loadImage();
    }

    private void loadImage() {
        if (TextUtils.isEmpty(imagePath)) {
            showError("Bill image path is missing.");
            return;
        }

        currentFile = new File(imagePath);
        if (!currentFile.exists()) {
            showError("Bill image file not found.");
            return;
        }

        progressLoading.setVisibility(View.VISIBLE);
        layoutError.setVisibility(View.GONE);

        new Thread(() -> {
            Bitmap bitmap = BitmapFactory.decodeFile(currentFile.getAbsolutePath());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (bitmap != null) {
                    currentBitmap = bitmap;
                    progressLoading.setVisibility(View.GONE);
                    photoView.setImageBitmap(bitmap);
                } else {
                    showError(getString(R.string.failed_to_load_bill));
                }
            });
        }).start();
    }

    private void showError(String message) {
        progressLoading.setVisibility(View.GONE);
        layoutError.setVisibility(View.VISIBLE);
        if (textErrorMessage != null && message != null) {
            textErrorMessage.setText(message);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_bill_viewer, menu);
        MenuItem printItem = menu.findItem(R.id.action_print_bill);
        if (printItem != null) {
            printItem.setVisible(isPrintEnabled());
        }
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem printItem = menu.findItem(R.id.action_print_bill);
        if (printItem != null) {
            printItem.setVisible(isPrintEnabled());
        }
        return super.onPrepareOptionsMenu(menu);
    }

    private boolean isPrintEnabled() {
        boolean enabled = GlobalStore.getInstance().isPrintBill();
        if (!enabled) {
            enabled = getSharedPreferences(Configurations.PREF_NAME, MODE_PRIVATE)
                    .getBoolean(Configurations.KEY_PRINT_BILL, false);
        }
        return enabled;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_share_bill) {
            if (currentFile != null && currentFile.exists()) {
                BitmapHelper.shareImageFile(this, currentFile, billTitle, billTitle);
            } else if (currentBitmap != null) {
                BitmapHelper.shareBitmap(this, currentBitmap, billTitle, billTitle);
            } else {
                Toast.makeText(this, "Image not loaded yet", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.action_print_bill) {
            if (currentBitmap != null) {
                new PrinterHelper(this, null).printBitmap(currentBitmap, this);
            } else if (currentFile != null && currentFile.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(currentFile.getAbsolutePath());
                if (bmp != null) {
                    currentBitmap = bmp;
                    new PrinterHelper(this, null).printBitmap(bmp, this);
                } else {
                    Toast.makeText(this, "Image not loaded yet", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Image not loaded yet", Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.action_download_bill) {
            if (currentBitmap != null) {
                BitmapHelper.saveBitmapToMediaStore(currentBitmap, this, fileName);
            } else {
                Toast.makeText(this, "Image not loaded yet", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
