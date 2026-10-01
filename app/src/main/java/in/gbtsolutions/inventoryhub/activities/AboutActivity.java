package in.gbtsolutions.inventoryhub.activities;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import in.gbtsolutions.inventoryhub.R;

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
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_about);
        }
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
        // Copy version on badge click
        View cardVersionBadge = findViewById(R.id.card_version_badge);
        if (cardVersionBadge != null) {
            cardVersionBadge.setOnClickListener(v -> {
                TextView tvAppVersion = findViewById(R.id.tv_app_version);
                String versionText = tvAppVersion != null ? tvAppVersion.getText().toString() : "Inventory Hub v1.0.1";
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) {
                    ClipData clip = ClipData.newPlainText("Inventory Hub Version", versionText);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Version copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            });
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

    private void showOpenSourceLicensesDialog() {
        String licensesMessage =
                "• Android Jetpack & AndroidX Libraries\n"
                        + "  Apache License 2.0 (Google LLC)\n\n"
                        + "• Material Components for Android\n"
                        + "  Apache License 2.0 (Google LLC)\n\n"
                        + "• ZXing Core & Barcode Scanning\n"
                        + "  Apache License 2.0 (ZXing Authors)\n\n"
                        + "• CameraX Framework\n"
                        + "  Apache License 2.0 (Google LLC)\n\n"
                        + "• OpenCSV Parsing Library\n"
                        + "  Apache License 2.0 (OpenCSV Authors)\n\n"
                        + "• jBCrypt Password Hashing\n"
                        + "  ISC / BSD License (Damien Miller)";

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_open_source_licenses)
                .setMessage(licensesMessage)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showPrivacyPolicyDialog() {
        String privacyMessage =
                "Inventory Hub is committed to protecting your business data:\n\n"
                        + "1. Offline Storage: All inventory items, stock counts, batch numbers, orders, sales invoices, and customer details are saved exclusively in a local SQLite database on your device.\n\n"
                        + "2. Zero Tracking: No analytics, tracking beacons, or telemetry data are collected without explicit administrative consent.\n\n"
                        + "3. Camera Access: The camera permission is utilized strictly for real-time barcode and QR code recognition within the app. No images or videos are stored or transmitted.\n\n"
                        + "4. Ownership: You maintain full, perpetual ownership of all business transactions and inventory information.";

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.about_privacy_policy)
                .setMessage(privacyMessage)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}