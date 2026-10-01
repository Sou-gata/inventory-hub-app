package in.gbtsolutions.inventoryhub.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import android.widget.FrameLayout;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.navigation.NavigationView;

import in.gbtsolutions.inventoryhub.Configurations;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.adapters.NoFilterArrayAdapter;
import in.gbtsolutions.inventoryhub.helpers.ThemeManager;
import in.gbtsolutions.inventoryhub.online.config.AppMode;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.repository.ConfigRepository;

public class SettingsActivity extends BaseActivity {

    private NavigationView navView;
    private ConfigRepository configRepository;
    private SharedPreferences sharedPreferences;

    private MaterialSwitch swShowPaymentMethod;
    private EditText etUpiId;
    private MaterialButton btnSaveUpi;

    private MaterialSwitch swSaveBillGallery;
    private MaterialSwitch swPrintBill;

    private MaterialSwitch swDarkTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        configRepository = new ConfigRepository(getApplication());
        sharedPreferences = getSharedPreferences(Configurations.PREF_NAME, MODE_PRIVATE);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, getString(R.string.settings), true);

        View headerContainer = findViewById(R.id.header_container);
        View scrollSettings = findViewById(R.id.scroll_settings);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        applyDrawerInsets(drawerLayout, headerContainer, scrollSettings, navView);
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_settings);
        applyEdgeToEdgeInsets(headerContainer, scrollSettings);

        // Load settings state
        GlobalStore.getInstance().loadSettings(this);

        setupOperatingModeSetting();
        setupMandatoryFieldSetting();
        setupPaymentSettings();
        setupBillPrintSettings();
        setupAppearanceSetting();
        setupQuickLinks();
    }

    private void setupMandatoryFieldSetting() {
        AutoCompleteTextView dropdownMandatoryField = findViewById(R.id.dropdown_mandatory_field);
        View containerMandatoryField = findViewById(R.id.container_mandatory_field);
        View rowMandatoryField = findViewById(R.id.row_mandatory_field);

        String[] mandatoryOptions = getResources().getStringArray(R.array.csv_mandatory_options);
        String[] mandatoryValues = getResources().getStringArray(R.array.csv_mandatory_values);

        NoFilterArrayAdapter<String> mandatoryAdapter = new NoFilterArrayAdapter<>(this, R.layout.item_dropdown, mandatoryOptions);
        dropdownMandatoryField.setAdapter(mandatoryAdapter);
        dropdownMandatoryField.setDropDownBackgroundResource(R.drawable.bg_card);
        dropdownMandatoryField.setDropDownAnchor(R.id.container_mandatory_field);

        int dropdownWidthPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics());
        dropdownMandatoryField.setDropDownWidth(dropdownWidthPx);

        // Initial selection
        String currentMandatory = GlobalStore.getInstance().getCsvMandatoryField();
        String currentLabel = getOptionLabel(currentMandatory, mandatoryValues, mandatoryOptions);
        dropdownMandatoryField.setText(currentLabel, false);

        configRepository.getValueLiveData(Configurations.KEY_CSV_MANDATORY_FIELD).observe(this, val -> {
            if (val != null && !val.trim().isEmpty()) {
                String normalized = val.trim().toLowerCase();
                String label = getOptionLabel(normalized, mandatoryValues, mandatoryOptions);
                if (!dropdownMandatoryField.getText().toString().equals(label)) {
                    dropdownMandatoryField.setText(label, false);
                }
                GlobalStore.getInstance().setCsvMandatoryField(normalized);
                if (!normalized.equals(sharedPreferences.getString(Configurations.KEY_CSV_MANDATORY_FIELD, Configurations.MANDATORY_FIELD_SKU))) {
                    sharedPreferences.edit().putString(Configurations.KEY_CSV_MANDATORY_FIELD, normalized).apply();
                }
            }
        });

        View.OnClickListener showDropdown = v -> {
            if (dropdownMandatoryField.isPopupShowing()) {
                dropdownMandatoryField.dismissDropDown();
            } else {
                dropdownMandatoryField.showDropDown();
            }
        };
        dropdownMandatoryField.setOnClickListener(showDropdown);
        if (containerMandatoryField != null) {
            containerMandatoryField.setOnClickListener(showDropdown);
        }
        if (rowMandatoryField != null) {
            rowMandatoryField.setOnClickListener(showDropdown);
        }

        dropdownMandatoryField.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < mandatoryValues.length) {
                String selectedVal = mandatoryValues[position];
                String selectedLabel = mandatoryOptions[position];
                dropdownMandatoryField.setText(selectedLabel, false);
                sharedPreferences.edit().putString(Configurations.KEY_CSV_MANDATORY_FIELD, selectedVal).apply();
                GlobalStore.getInstance().setCsvMandatoryField(selectedVal);
                configRepository.set(Configurations.KEY_CSV_MANDATORY_FIELD, selectedVal);
            }
        });
    }

    private void setupPaymentSettings() {
        swShowPaymentMethod = findViewById(R.id.sw_show_payment_method);
        View rowShowPaymentMethod = findViewById(R.id.row_show_payment_method);
        etUpiId = findViewById(R.id.et_upi_id);
        btnSaveUpi = findViewById(R.id.btn_save_upi);

        // Initial state from GlobalStore / SharedPreferences
        boolean initialShowDialog = GlobalStore.getInstance().isShowPaymentMethodDialog();
        swShowPaymentMethod.setChecked(initialShowDialog);

        String initialUpi = GlobalStore.getInstance().getUpiId();
        if (initialUpi == null || initialUpi.isEmpty()) {
            initialUpi = sharedPreferences.getString(Configurations.KEY_UPI_ID, "");
        }
        if (initialUpi != null) {
            etUpiId.setText(initialUpi);
        }

        // LiveData observation for show_payment_method_dialog from configs table
        configRepository.getValueLiveData(Configurations.KEY_SHOW_PAYMENT_METHOD_DIALOG).observe(this, val -> {
            if (val != null) {
                boolean isChecked = "true".equalsIgnoreCase(val.trim());
                if (swShowPaymentMethod.isChecked() != isChecked) {
                    swShowPaymentMethod.setChecked(isChecked);
                }
                GlobalStore.getInstance().setShowPaymentMethodDialog(isChecked);
                if (sharedPreferences.getBoolean(Configurations.KEY_SHOW_PAYMENT_METHOD_DIALOG, false) != isChecked) {
                    sharedPreferences.edit().putBoolean(Configurations.KEY_SHOW_PAYMENT_METHOD_DIALOG, isChecked).apply();
                }
            }
        });

        // LiveData observation for upi_id from configs table
        configRepository.getValueLiveData(Configurations.KEY_UPI_ID).observe(this, val -> {
            if (val != null) {
                if (!etUpiId.hasFocus() && !val.equals(etUpiId.getText().toString())) {
                    etUpiId.setText(val);
                }
                GlobalStore.getInstance().setUpiId(val);
                if (!val.equals(sharedPreferences.getString(Configurations.KEY_UPI_ID, ""))) {
                    sharedPreferences.edit().putString(Configurations.KEY_UPI_ID, val).apply();
                }
            }
        });

        swShowPaymentMethod.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                applyPaymentMethodChange(isChecked);
            }
        });

        if (rowShowPaymentMethod != null) {
            rowShowPaymentMethod.setOnClickListener(v -> {
                boolean newState = !swShowPaymentMethod.isChecked();
                applyPaymentMethodChange(newState);
            });
        }

        if (btnSaveUpi != null) {
            btnSaveUpi.setOnClickListener(v -> saveUpiId());
        }

        etUpiId.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveUpiId();
                return true;
            }
            return false;
        });
    }

    private void applyPaymentMethodChange(boolean isChecked) {
        String upiId = etUpiId != null ? etUpiId.getText().toString().trim() : "";
        if (upiId.isEmpty()) {
            upiId = GlobalStore.getInstance().getUpiId();
        }
        if (upiId == null) {
            upiId = "";
        }

        if (isChecked && upiId.trim().isEmpty()) {
            // "if show payment method is on and upi id is off the setting will not be saved."
            if (swShowPaymentMethod != null) {
                swShowPaymentMethod.setChecked(false);
            }
            Toast.makeText(this, R.string.error_upi_required_for_payment_dialog, Toast.LENGTH_SHORT).show();
            if (etUpiId != null) {
                etUpiId.requestFocus();
            }
            return;
        }

        if (swShowPaymentMethod != null && swShowPaymentMethod.isChecked() != isChecked) {
            swShowPaymentMethod.setChecked(isChecked);
        }

        if (isChecked && !upiId.isEmpty()) {
            sharedPreferences.edit().putString(Configurations.KEY_UPI_ID, upiId).apply();
            GlobalStore.getInstance().setUpiId(upiId);
            configRepository.set(Configurations.KEY_UPI_ID, upiId);
        }

        sharedPreferences.edit().putBoolean(Configurations.KEY_SHOW_PAYMENT_METHOD_DIALOG, isChecked).apply();
        GlobalStore.getInstance().setShowPaymentMethodDialog(isChecked);
        configRepository.set(Configurations.KEY_SHOW_PAYMENT_METHOD_DIALOG, isChecked ? "true" : "false");
    }

    private void saveUpiId() {
        if (etUpiId == null) return;
        String enteredUpi = etUpiId.getText().toString().trim();
        boolean isPaymentMethodOn = swShowPaymentMethod != null && swShowPaymentMethod.isChecked();

        if (isPaymentMethodOn && enteredUpi.isEmpty()) {
            // "if show payment method is on and upi id is off the setting will not be saved."
            Toast.makeText(this, R.string.error_upi_cannot_be_empty_with_dialog, Toast.LENGTH_SHORT).show();
            etUpiId.requestFocus();
            return;
        }

        sharedPreferences.edit().putString(Configurations.KEY_UPI_ID, enteredUpi).apply();
        GlobalStore.getInstance().setUpiId(enteredUpi);
        configRepository.set(Configurations.KEY_UPI_ID, enteredUpi, new ConfigRepository.ConfigActionCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(SettingsActivity.this, R.string.upi_id_saved, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SettingsActivity.this, "Failed to save UPI ID: " + message, Toast.LENGTH_SHORT).show();
            }
        });

        // Hide keyboard & clear focus
        View currentFocus = getCurrentFocus();
        if (currentFocus != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(currentFocus.getWindowToken(), 0);
            }
        }
        etUpiId.clearFocus();
    }

    private void setupBillPrintSettings() {
        swSaveBillGallery = findViewById(R.id.sw_save_bill_gallery);
        View rowSaveBillGallery = findViewById(R.id.row_save_bill_gallery);
        swPrintBill = findViewById(R.id.sw_print_bill);
        View rowPrintBill = findViewById(R.id.row_print_bill);

        // 1. Save Bill to Gallery switch & row setup
        boolean initialSaveGallery = sharedPreferences.getBoolean(Configurations.KEY_SAVE_BILL_TO_GALLERY, true);
        if (swSaveBillGallery != null) {
            swSaveBillGallery.setChecked(initialSaveGallery);
            swSaveBillGallery.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    toggleSaveBillToGallery(isChecked);
                }
            });
        }
        if (rowSaveBillGallery != null) {
            rowSaveBillGallery.setOnClickListener(v -> {
                if (swSaveBillGallery != null) {
                    boolean newState = !swSaveBillGallery.isChecked();
                    swSaveBillGallery.setChecked(newState);
                    toggleSaveBillToGallery(newState);
                }
            });
        }

        configRepository.getValueLiveData(Configurations.KEY_SAVE_BILL_TO_GALLERY).observe(this, val -> {
            if (val != null && swSaveBillGallery != null) {
                boolean isChecked = "true".equalsIgnoreCase(val.trim());
                if (swSaveBillGallery.isChecked() != isChecked) {
                    swSaveBillGallery.setChecked(isChecked);
                }
                GlobalStore.getInstance().setSaveBillToGallery(isChecked);
                if (sharedPreferences.getBoolean(Configurations.KEY_SAVE_BILL_TO_GALLERY, false) != isChecked) {
                    sharedPreferences.edit().putBoolean(Configurations.KEY_SAVE_BILL_TO_GALLERY, isChecked).apply();
                }
            }
        });

        // 2. Print Bill switch & row setup
        boolean initialPrintBill = GlobalStore.getInstance().isPrintBill();
        if (!initialPrintBill) {
            initialPrintBill = sharedPreferences.getBoolean(Configurations.KEY_PRINT_BILL, false);
        }
        if (swPrintBill != null) {
            swPrintBill.setChecked(initialPrintBill);
            swPrintBill.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (buttonView.isPressed()) {
                    togglePrintBill(isChecked);
                }
            });
        }
        if (rowPrintBill != null) {
            rowPrintBill.setOnClickListener(v -> {
                if (swPrintBill != null) {
                    boolean newState = !swPrintBill.isChecked();
                    swPrintBill.setChecked(newState);
                    togglePrintBill(newState);
                }
            });
        }

        configRepository.getValueLiveData(Configurations.KEY_PRINT_BILL).observe(this, val -> {
            if (val != null && swPrintBill != null) {
                boolean isChecked = "true".equalsIgnoreCase(val.trim());
                if (swPrintBill.isChecked() != isChecked) {
                    swPrintBill.setChecked(isChecked);
                }
                GlobalStore.getInstance().setPrintBill(isChecked);
                if (sharedPreferences.getBoolean(Configurations.KEY_PRINT_BILL, false) != isChecked) {
                    sharedPreferences.edit().putBoolean(Configurations.KEY_PRINT_BILL, isChecked).apply();
                }
            }
        });
    }

    private void toggleSaveBillToGallery(boolean isChecked) {
        sharedPreferences.edit().putBoolean(Configurations.KEY_SAVE_BILL_TO_GALLERY, isChecked).apply();
        GlobalStore.getInstance().setSaveBillToGallery(isChecked);
        configRepository.set(Configurations.KEY_SAVE_BILL_TO_GALLERY, isChecked ? "true" : "false");
    }

    private void togglePrintBill(boolean isChecked) {
        sharedPreferences.edit().putBoolean(Configurations.KEY_PRINT_BILL, isChecked).apply();
        GlobalStore.getInstance().setPrintBill(isChecked);
        configRepository.set(Configurations.KEY_PRINT_BILL, isChecked ? "true" : "false");
    }

    private void setupAppearanceSetting() {
        swDarkTheme = findViewById(R.id.sw_dark_theme);
        View rowDarkTheme = findViewById(R.id.row_dark_theme);
        ImageView ivThemeIcon = findViewById(R.id.iv_theme_icon);
        TextView tvThemeDesc = findViewById(R.id.tv_theme_desc);

        boolean isDark = ThemeManager.isDarkMode(this);
        swDarkTheme.setChecked(isDark);
        if (ivThemeIcon != null) {
            ivThemeIcon.setImageResource(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);
        }
        if (tvThemeDesc != null) {
            tvThemeDesc.setText(isDark ? "Dark theme active (easier on eyes in low light)" : "Light theme active (clean high contrast)");
        }

        swDarkTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                ThemeManager.setSavedThemeMode(SettingsActivity.this, isChecked ? ThemeManager.MODE_DARK : ThemeManager.MODE_LIGHT);
            }
        });

        if (rowDarkTheme != null) {
            rowDarkTheme.setOnClickListener(v -> {
                boolean newState = !swDarkTheme.isChecked();
                swDarkTheme.setChecked(newState);
                ThemeManager.setSavedThemeMode(SettingsActivity.this, newState ? ThemeManager.MODE_DARK : ThemeManager.MODE_LIGHT);
            });
        }
    }

    private void setupOperatingModeSetting() {
        MaterialSwitch swOnlineMode = findViewById(R.id.sw_online_mode);
        View rowOnlineMode = findViewById(R.id.row_online_mode);
        TextView textModeTitle = findViewById(R.id.text_mode_title);
        TextView textModeDesc = findViewById(R.id.text_mode_desc);
        ImageView iconModeStatus = findViewById(R.id.icon_mode_status);

        View rowServerUrl = findViewById(R.id.row_server_url);
        TextView textServerUrl = findViewById(R.id.text_server_url);

        View rowTestConnection = findViewById(R.id.row_test_connection);
        TextView textConnectionStatus = findViewById(R.id.text_connection_status);
        View progressTestConnection = findViewById(R.id.progress_test_connection);

        AppModeManager modeManager = AppModeManager.getInstance(this);

        if (textServerUrl != null) {
            textServerUrl.setText(modeManager.getServerUrl());
        }

        Runnable updateUiForMode = () -> {
            boolean isOnline = modeManager.isOnlineMode();
            if (swOnlineMode != null && swOnlineMode.isChecked() != isOnline) {
                swOnlineMode.setChecked(isOnline);
            }
            if (textModeTitle != null) {
                textModeTitle.setText(isOnline ? "Online Mode (Cloud Server)" : "Offline Mode (Local Database)");
            }
            if (textModeDesc != null) {
                textModeDesc.setText(isOnline
                        ? "Active: All data is loaded, saved, and searched via Cloud API"
                        : "Active: All data is saved and read from device's local database");
            }
            if (iconModeStatus != null) {
                iconModeStatus.setColorFilter(ContextCompat.getColor(this,
                        isOnline ? R.color.status_green : R.color.material_blue));
            }
        };

        updateUiForMode.run();

        if (swOnlineMode != null) {
            swOnlineMode.setOnCheckedChangeListener((btn, isChecked) -> {
                AppMode newMode = isChecked ? AppMode.ONLINE : AppMode.OFFLINE;
                if (newMode != modeManager.getCurrentMode()) {
                    modeManager.setAppMode(newMode);
                    updateUiForMode.run();
                    Toast.makeText(this, isChecked ? "Switched to Online Mode (Cloud Server)" : "Switched to Offline Mode (Local Database)", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (rowOnlineMode != null && swOnlineMode != null) {
            rowOnlineMode.setOnClickListener(v -> swOnlineMode.toggle());
        }

        if (rowServerUrl != null) {
            rowServerUrl.setOnClickListener(v -> showEditServerUrlDialog(textServerUrl));
        }

        if (rowTestConnection != null) {
            rowTestConnection.setOnClickListener(v -> {
                if (progressTestConnection != null) progressTestConnection.setVisibility(View.VISIBLE);
                if (textConnectionStatus != null) {
                    textConnectionStatus.setText("Connecting to server...");
                    textConnectionStatus.setTextColor(ContextCompat.getColor(SettingsActivity.this, R.color.fg_muted));
                }
                modeManager.testServerConnection(modeManager.getServerUrl(), (isSuccess, message) -> {
                    if (progressTestConnection != null) progressTestConnection.setVisibility(View.GONE);
                    if (textConnectionStatus != null) {
                        textConnectionStatus.setText(message);
                        textConnectionStatus.setTextColor(ContextCompat.getColor(SettingsActivity.this,
                                isSuccess ? R.color.status_green : R.color.status_red));
                    }
                });
            });
        }
    }

    private void showEditServerUrlDialog(TextView textServerUrl) {
        AppModeManager modeManager = AppModeManager.getInstance(this);
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(modeManager.getServerUrl());
        if (input.getText() != null) {
            input.setSelection(input.getText().length());
        }
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
        FrameLayout container = new FrameLayout(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = pad;
        params.rightMargin = pad;
        input.setLayoutParams(params);
        container.addView(input);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Cloud Server Base URL")
                .setMessage("Enter the base address for the API server (e.g. http://10.0.2.2:3000/ or https://api.inventoryhub.com/):")
                .setView(container)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newUrl = input.getText().toString().trim();
                    if (!newUrl.isEmpty()) {
                        modeManager.setServerUrl(newUrl);
                        if (textServerUrl != null) {
                            textServerUrl.setText(modeManager.getServerUrl());
                        }
                        Toast.makeText(this, "Server URL updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void setupQuickLinks() {
        View rowBackupRestore = findViewById(R.id.row_backup_restore);
        if (rowBackupRestore != null) {
            rowBackupRestore.setOnClickListener(v -> navigateTo(BackupRestoreActivity.class, false));
        }

        View rowAboutApp = findViewById(R.id.row_about_app);
        if (rowAboutApp != null) {
            rowAboutApp.setOnClickListener(v -> navigateTo(AboutActivity.class, false));
        }
    }

    private String getOptionLabel(String value, String[] values, String[] labels) {
        if (value != null) {
            for (int i = 0; i < values.length; i++) {
                if (values[i].equalsIgnoreCase(value.trim())) {
                    return labels[i];
                }
            }
        }
        return labels.length > 0 ? labels[0] : "SKU";
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_settings);
        }
    }
}