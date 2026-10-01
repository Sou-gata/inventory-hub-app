package in.gbtsolutions.inventoryhub.activities;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Process;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.backup.BackupManager;

public class BackupRestoreActivity extends BaseActivity {

    private NavigationView navView;
    private View progressOverlay;
    private TextView tvProgressMessage;

    private final ActivityResultLauncher<String[]> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) {
                    showRestorePasswordDialog(uri);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, getString(R.string.backup_restore), true);

        View headerContainer = findViewById(R.id.header_container);
        View mainContent = findViewById(R.id.main_content);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        progressOverlay = findViewById(R.id.progress_overlay);
        tvProgressMessage = findViewById(R.id.tv_progress_message);

        applyDrawerInsets(drawerLayout, headerContainer, mainContent, navView);
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_settings);
        applyEdgeToEdgeInsets(headerContainer, mainContent);

        MaterialButton btnCreateBackup = findViewById(R.id.btn_create_backup);
        MaterialButton btnRestoreBackup = findViewById(R.id.btn_restore_backup);

        btnCreateBackup.setOnClickListener(v -> showBackupPasswordDialog());
        btnRestoreBackup.setOnClickListener(v -> showRestoreConfirmationDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_settings);
        }
    }

    // =========================================================================
    // BACKUP FLOW
    // =========================================================================

    private void showBackupPasswordDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_backup_password, null);
        TextInputLayout tilPassword = dialogView.findViewById(R.id.til_backup_password);
        TextInputEditText etPassword = dialogView.findViewById(R.id.et_backup_password);
        TextInputLayout tilConfirm = dialogView.findViewById(R.id.til_confirm_password);
        TextInputEditText etConfirm = dialogView.findViewById(R.id.et_confirm_password);

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.backup_data)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.btn_create_backup, null)
                .create();

        dialog.setOnShowListener(d -> {
            MaterialButton btnOk = (MaterialButton) dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnOk.setOnClickListener(v -> {
                String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
                String confirm = etConfirm.getText() != null ? etConfirm.getText().toString().trim() : "";

                tilPassword.setError(null);
                tilConfirm.setError(null);

                // Both empty is allowed (default encryption password will be used)
                if (!TextUtils.isEmpty(password) || !TextUtils.isEmpty(confirm)) {
                    if (!password.equals(confirm)) {
                        tilConfirm.setError(getString(R.string.error_passwords_mismatch));
                        return;
                    }
                }

                dialog.dismiss();
                performBackup(password);
            });
        });

        dialog.show();
    }

    private void performBackup(String password) {
        showProgress(getString(R.string.creating_backup));

        BackupManager.createBackup(this, password, new BackupManager.BackupCallback() {
            @Override
            public void onSuccess(@NonNull File backupFile) {
                hideProgress();
                showBackupSuccessDialog(backupFile);
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                hideProgress();
                showErrorDialog(errorMessage);
            }
        });
    }

    private void showBackupSuccessDialog(@NonNull File backupFile) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.backup_success_title)
                .setMessage(getString(R.string.backup_success_message, backupFile.getAbsolutePath()))
                .setPositiveButton(android.R.string.ok, null)
                .setNeutralButton(R.string.share_backup, (d, which) -> shareBackupFile(backupFile))
                .show();
    }

    private void shareBackupFile(@NonNull File file) {
        try {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/octet-stream");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(shareIntent, getString(R.string.share_backup)));
        } catch (Exception e) {
            Toast.makeText(this, "Could not open share sheet: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // =========================================================================
    // RESTORE FLOW
    // =========================================================================

    private void showRestoreConfirmationDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_warning_title)
                .setMessage(R.string.restore_warning_message)
                .setIcon(R.drawable.ic_error)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.btn_continue, (d, which) -> {
                    // Launch system file picker for .ihbak files
                    filePickerLauncher.launch(new String[]{"*/*"});
                })
                .show();
    }

    private void showRestorePasswordDialog(@NonNull Uri uri) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_restore_password, null);
        TextView tvFileName = dialogView.findViewById(R.id.tv_restore_file_name);
        TextInputEditText etPassword = dialogView.findViewById(R.id.et_restore_password);

        String displayName = getFileNameFromUri(uri);
        if (!TextUtils.isEmpty(displayName)) {
            tvFileName.setText(displayName);
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_data)
                .setView(dialogView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.btn_restore_backup, (d, which) -> {
                    String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
                    performRestore(uri, password);
                })
                .show();
    }

    private void performRestore(@NonNull Uri uri, @NonNull String password) {
        showProgress(getString(R.string.restoring_backup));

        BackupManager.restoreBackup(this, uri, password, new BackupManager.RestoreCallback() {
            @Override
            public void onSuccess() {
                hideProgress();
                showRestoreSuccessDialog();
            }

            @Override
            public void onError(@NonNull String errorMessage) {
                hideProgress();
                String message;
                if ("WRONG_PASSWORD".equals(errorMessage)) {
                    message = getString(R.string.error_wrong_password);
                } else if ("INVALID_FORMAT".equals(errorMessage) || "INVALID_DB".equals(errorMessage)) {
                    message = getString(R.string.error_invalid_backup);
                } else {
                    message = errorMessage;
                }
                showErrorDialog(message);
            }
        });
    }

    private void showRestoreSuccessDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.restore_success_title)
                .setMessage(R.string.restore_success_message)
                .setCancelable(false)
                .setPositiveButton(R.string.btn_restart_app, (d, which) -> restartApp())
                .show();
    }

    private void restartApp() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        Process.killProcess(Process.myPid());
        System.exit(0);
    }

    // =========================================================================
    // UI HELPERS
    // =========================================================================

    private void showProgress(String message) {
        if (tvProgressMessage != null) {
            tvProgressMessage.setText(message);
        }
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgress() {
        if (progressOverlay != null) {
            progressOverlay.setVisibility(View.GONE);
        }
    }

    private void showErrorDialog(String message) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_dialog_title) // Reusing existing error/alert styled title or custom
                .setIcon(R.drawable.ic_error)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    @Nullable
    private String getFileNameFromUri(@NonNull Uri uri) {
        String result = null;
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (result == null && uri.getPath() != null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
