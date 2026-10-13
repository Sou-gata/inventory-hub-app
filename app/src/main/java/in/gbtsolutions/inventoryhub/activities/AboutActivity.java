package in.gbtsolutions.inventoryhub.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.work.WorkInfo;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.migration.MigrationLauncher;
import in.gbtsolutions.inventoryhub.migration.MigrationWorker;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;

public class AboutActivity extends BaseActivity {

    private NavigationView navView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, getString(R.string.about), true);

        View headerContainer = findViewById(R.id.header_container);
        View scrollAbout = findViewById(R.id.scroll_about);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        // Apply edge-to-edge system insets (status bar & nav bar) via BaseActivity helper
        applyDrawerInsets(drawerLayout, headerContainer, scrollAbout, navView);

        // Standardized sidebar and header navigation
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_about);

        applyEdgeToEdgeInsets(headerContainer, scrollAbout);

        setupVersionInfo();
        setupClickListeners();
        setupMigrationSection();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_about);
        }
        updateModeAndMigrationState();
    }

    private void setupVersionInfo() {
        String versionName = "1.0.1";
        int versionCode = 1;

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (pInfo != null) {
                if (pInfo.versionName != null) {
                    versionName = pInfo.versionName;
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    versionCode = (int) pInfo.getLongVersionCode();
                } else {
                    versionCode = pInfo.versionCode;
                }
            }
        } catch (Exception ignored) {
        }

        String fullVersionString = "v" + versionName + " (Build " + versionCode + ")";

        TextView tvAppVersion = findViewById(R.id.tv_app_version);
        if (tvAppVersion != null) {
            tvAppVersion.setText(fullVersionString);
        }
    }

    private void setupClickListeners() {
        // Copy version / system summary on badge click
        View cardVersionBadge = findViewById(R.id.card_version_badge);
        if (cardVersionBadge != null) {
            cardVersionBadge.setOnClickListener(v -> copySystemInfoToClipboard());
        }

        View cardModeBadge = findViewById(R.id.card_mode_badge);
        if (cardModeBadge != null) {
            cardModeBadge.setOnClickListener(v -> copySystemInfoToClipboard());
        }

        // What's New Dialog
        View btnWhatsNew = findViewById(R.id.btn_whats_new);
        if (btnWhatsNew != null) {
            btnWhatsNew.setOnClickListener(v -> showWhatsNewDialog());
        }

        // Share App
        View btnShareApp = findViewById(R.id.btn_share_app);
        if (btnShareApp != null) {
            btnShareApp.setOnClickListener(v -> shareApp());
        }

        // Contact Support Email
        View btnContactSupport = findViewById(R.id.btn_contact_support);
        if (btnContactSupport != null) {
            btnContactSupport.setOnClickListener(v -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:" + getString(R.string.about_support_email)));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Inventory Hub Support Inquiry");
                try {
                    startActivity(Intent.createChooser(emailIntent, "Send Email"));
                } catch (Exception e) {
                    Toast.makeText(this, "No email application found.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Visit Website
        View btnVisitWebsite = findViewById(R.id.btn_visit_website);
        if (btnVisitWebsite != null) {
            btnVisitWebsite.setOnClickListener(v -> {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.about_website_url)));
                try {
                    startActivity(webIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open web browser.", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Open Source Licenses Dialog
        View btnLicenses = findViewById(R.id.btn_open_source_licenses);
        if (btnLicenses != null) {
            btnLicenses.setOnClickListener(v -> showOpenSourceLicensesDialog());
        }

        // Privacy Policy Dialog
        View btnPrivacy = findViewById(R.id.btn_privacy_policy);
        if (btnPrivacy != null) {
            btnPrivacy.setOnClickListener(v -> showPrivacyPolicyDialog());
        }
    }

    private void copySystemInfoToClipboard() {
        TextView tvAppVersion = findViewById(R.id.tv_app_version);
        String versionText = tvAppVersion != null ? tvAppVersion.getText().toString() : "v1.0.1";
        boolean isOnline = AppModeManager.getInstance(this).isOnlineMode();
        String modeText = isOnline ? "Online Cloud Mode" : "Offline Mode";

        String info = "Inventory Hub " + versionText + " [" + modeText + "]";
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("Inventory Hub Info", info);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Version & mode copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareApp() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Inventory Hub - Enterprise Inventory & POS Suite");
            shareIntent.putExtra(Intent.EXTRA_TEXT,
                    "Experience Inventory Hub: A modern hybrid cloud & offline inventory management, POS billing, and GST compliance suite.\n\nWebsite: "
                            + getString(R.string.about_website_url));
            startActivity(Intent.createChooser(shareIntent, "Share Inventory Hub via"));
        } catch (Exception e) {
            Toast.makeText(this, "Unable to launch share dialog.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showWhatsNewDialog() {
        String whatsNewMessage =
                "★ Hybrid Cloud Synchronization\n"
                        + "  Seamless dual-mode operation: work completely offline or connect with the enterprise cloud backend.\n\n"
                        + "★ One-Tap Cloud Data Migration\n"
                        + "  Migrate all local products, categories, batches, purchases, and sales orders to the cloud effortlessly.\n\n"
                        + "★ GSTR-1 GST & Tax Compliance\n"
                        + "  Automated GST return filing exports (B2B, B2CL, B2CS), Sales Register, Purchase Register, and User-wise sales reports with Excel & CSV export.\n\n"
                        + "★ Supplier Procurement & Goods Receiving\n"
                        + "  Complete purchase order lifecycle with dedicated goods receipt verification and stock intake.\n\n"
                        + "★ Handheld Laser & Camera Scanner\n"
                        + "  Direct hardware laser scanner support for enterprise handheld devices alongside camera vision.\n\n"
                        + "★ Comprehensive Audit Trail & Security\n"
                        + "  Immutable activity logging tracking every transaction, paired with secure user authentication and role permissions.";

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_whats_new)
                .setMessage(whatsNewMessage)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showOpenSourceLicensesDialog() {
        String licensesMessage =
                "• Android Jetpack & Room Database\n"
                        + "  Apache License 2.0 (Google LLC)\n\n"
                        + "• Material Components for Android\n"
                        + "  Apache License 2.0 (Google LLC)\n\n"
                        + "• Retrofit 2 & OkHttp 3\n"
                        + "  Apache License 2.0 (Square, Inc.)\n\n"
                        + "• Google Gson Serialization\n"
                        + "  Apache License 2.0 (Google LLC)\n\n"
                        + "• CameraX Framework & ZXing Core\n"
                        + "  Apache License 2.0 (Google LLC / ZXing Authors)\n\n"
                        + "• Apache POI Spreadsheet Framework\n"
                        + "  Apache License 2.0 (Apache Software Foundation)\n\n"
                        + "• OpenCSV Parsing Library\n"
                        + "  Apache License 2.0 (OpenCSV Authors)\n\n"
                        + "• AndroidX WorkManager\n"
                        + "  Apache License 2.0 (Google LLC)\n\n"
                        + "• jBCrypt Password Hashing\n"
                        + "  ISC / BSD License (Damien Miller)\n\n"
                        + "• PhotoView\n"
                        + "  Apache License 2.0 (Chris Banes)\n\n"
                        + "• Handheld PDA Platform SDK\n"
                        + "  Hardware Driver & Barcode Engine";

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_open_source_licenses)
                .setMessage(licensesMessage)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showPrivacyPolicyDialog() {
        String privacyMessage =
                "Inventory Hub is engineered for complete data privacy and enterprise compliance:\n\n"
                        + "1. Data Security & Storage: In Offline Mode, all inventory catalogs, stock counts, batch numbers, sales invoices, and customer details are saved securely on your local device. In Online Mode, records are transmitted securely to your authorized cloud backend over encrypted connections.\n\n"
                        + "2. Zero Commercial Tracking: No advertising SDKs, behavioral analytics, or third-party tracking beacons are embedded in this application.\n\n"
                        + "3. Scanner & Camera Permissions: Camera and hardware laser permissions are utilized exclusively for real-time barcode and QR code recognition. No camera feeds or images are stored or transmitted.\n\n"
                        + "4. Full Data Sovereignty: You maintain 100% ownership of all business transactions, stock data, and customer accounts, with on-demand local backup and Excel/CSV data exports.";

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_privacy_policy)
                .setMessage(privacyMessage)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void setupMigrationSection() {
        updateModeAndMigrationState();

        View btnMigrate = findViewById(R.id.btn_migrate_data);
        if (btnMigrate != null) {
            btnMigrate.setOnClickListener(v -> {
                new MaterialAlertDialogBuilder(this)
                        .setTitle("Migrate Offline Data to Cloud")
                        .setMessage("This will upload all your local offline products, inventory batches, sales, and purchases to the online cloud server.\n\nMake sure your device has a stable internet connection before starting.")
                        .setPositiveButton("Start Migration", (d, which) -> showMigrationProgressDialog())
                        .setNegativeButton("Cancel", null)
                        .show();
            });
        }
    }

    private void updateModeAndMigrationState() {
        boolean isOnline = AppModeManager.getInstance(this).isOnlineMode();

        // Top Hero Mode Badge
        TextView tvAppModeStatus = findViewById(R.id.tv_app_mode_status);
        ImageView ivModeDot = findViewById(R.id.iv_mode_dot);
        if (tvAppModeStatus != null) {
            tvAppModeStatus.setText(isOnline ? "Online Cloud" : "Offline Local");
        }
        if (ivModeDot != null) {
            int dotColor = ContextCompat.getColor(this, isOnline ? R.color.status_green : R.color.material_blue);
            ivModeDot.setImageTintList(ColorStateList.valueOf(dotColor));
        }

        // Cloud Migration Section Banner
        TextView tvBannerTitle = findViewById(R.id.tv_mode_banner_title);
        TextView tvBannerSubtitle = findViewById(R.id.tv_mode_banner_subtitle);
        TextView tvBannerTag = findViewById(R.id.tv_mode_banner_tag);
        ImageView ivBannerIcon = findViewById(R.id.iv_mode_banner_icon);

        if (tvBannerTitle != null) {
            tvBannerTitle.setText(isOnline ? "Online Cloud Mode Active" : "Offline Mode Active");
        }
        if (tvBannerSubtitle != null) {
            tvBannerSubtitle.setText(isOnline
                    ? "Connected to cloud backend. Live transactions synchronized."
                    : "All records stored securely on your device. Works offline.");
        }
        if (tvBannerTag != null) {
            tvBannerTag.setText(isOnline ? "ONLINE" : "OFFLINE");
            tvBannerTag.setTextColor(ContextCompat.getColor(this, isOnline ? R.color.status_green : R.color.status_orange));
            tvBannerTag.setBackgroundResource(isOnline ? R.drawable.bg_status_chip : R.drawable.bg_badge_default);
        }
        if (ivBannerIcon != null) {
            ivBannerIcon.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, isOnline ? R.color.status_green : R.color.material_blue)
            ));
        }

        // Mode-wise visibility for Migration Button & Divider
        // The migrate button will show in online mode only.
        View btnMigrate = findViewById(R.id.btn_migrate_data);
        View dividerMigration = findViewById(R.id.divider_migration_action);

        if (btnMigrate != null) {
            btnMigrate.setVisibility(isOnline ? View.VISIBLE : View.GONE);
        }
        if (dividerMigration != null) {
            dividerMigration.setVisibility(isOnline ? View.VISIBLE : View.GONE);
        }

        TextView tvMigrateTitle = findViewById(R.id.tv_migration_button_title);
        TextView tvMigrateSubtitle = findViewById(R.id.tv_migration_button_subtitle);
        ImageView ivMigrateIcon = findViewById(R.id.iv_migrate_action_icon);

        if (tvMigrateTitle != null) {
            tvMigrateTitle.setText("Migrate Offline Data to Cloud");
        }
        if (tvMigrateSubtitle != null) {
            tvMigrateSubtitle.setText("Upload local inventory, batches & orders to online server");
        }
        if (ivMigrateIcon != null) {
            ivMigrateIcon.setImageResource(R.drawable.ic_sync);
            ivMigrateIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.material_blue)));
        }
    }

    private void showMigrationProgressDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_migration_progress, null);
        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        dialog.setCanceledOnTouchOutside(false);

        ImageView ivIcon = dialogView.findViewById(R.id.iv_migration_icon);
        FrameLayout iconContainer = dialogView.findViewById(R.id.layout_icon_container);
        TextView tvTitle = dialogView.findViewById(R.id.tv_migration_title);
        TextView tvSubtitle = dialogView.findViewById(R.id.tv_migration_subtitle);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_migration);
        TextView tvStatus = dialogView.findViewById(R.id.tv_migration_status);
        TextView tvDetail = dialogView.findViewById(R.id.tv_migration_detail);
        MaterialButton btnAction = dialogView.findViewById(R.id.btn_migration_action);

        dialog.show();

        MigrationLauncher.startMigration(this).observe(this, workInfo -> {
            if (workInfo == null) return;

            WorkInfo.State state = workInfo.getState();

            if (state == WorkInfo.State.RUNNING) {
                String progressMsg = workInfo.getProgress().getString(MigrationWorker.KEY_PROGRESS);
                if (progressMsg != null && !progressMsg.isEmpty()) {
                    tvStatus.setText(progressMsg);
                }
            } else if (state == WorkInfo.State.SUCCEEDED) {
                progressBar.setVisibility(View.GONE);
                tvDetail.setVisibility(View.GONE);
                tvTitle.setText("Migration Successful!");
                tvSubtitle.setText("Your offline database has been migrated to the online server.");
                tvStatus.setText("Application is now running in Online Cloud Mode.");

                ivIcon.setImageResource(R.drawable.ic_check);
                ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_green)));
                iconContainer.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_green_bg)));

                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Done");
                btnAction.setOnClickListener(v -> {
                    dialog.dismiss();
                    updateModeAndMigrationState();
                });
            } else if (state == WorkInfo.State.FAILED) {
                progressBar.setVisibility(View.GONE);
                tvTitle.setText("Migration Failed");
                tvSubtitle.setText("Could not complete the cloud data transfer.");

                String errorMsg = workInfo.getOutputData().getString(MigrationWorker.KEY_ERROR);
                tvStatus.setText(errorMsg != null && !errorMsg.isEmpty() ? errorMsg : "An error occurred during migration.");
                tvDetail.setText("Your local offline database was NOT modified. Please check your network and try again.");

                ivIcon.setImageResource(R.drawable.ic_error);
                ivIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_red)));
                iconContainer.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.status_red_bg)));

                btnAction.setVisibility(View.VISIBLE);
                btnAction.setText("Dismiss");
                btnAction.setOnClickListener(v -> dialog.dismiss());
            }
        });
    }
}