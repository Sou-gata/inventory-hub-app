package in.gbtsolutions.inventoryhub.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Configurations;
import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper;
import in.gbtsolutions.inventoryhub.models.AuditTrail;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.User;
import in.gbtsolutions.inventoryhub.repository.ConfigRepository;

public class ProfileActivity extends BaseActivity {

    // Config Keys
    public static final String KEY_COMPANY_NAME = "company_name";
    public static final String KEY_COMPANY_PHONE = "company_phone";
    public static final String KEY_GST_NUMBER = "gst_number";
    public static final String KEY_PAN_NUMBER = "pan_number";
    public static final String KEY_ADDRESS = "address";
    public static final String KEY_DISTRICT = "district";
    public static final String KEY_STATE = "state";
    public static final String KEY_POSTAL_CODE = "postal_code";
    public static final String KEY_FOOTER_1 = Configurations.KEY_FOOTER_1;
    public static final String KEY_FOOTER_2 = Configurations.KEY_FOOTER_2;
    public static final String KEY_FOOTER_3 = Configurations.KEY_FOOTER_3;
    public static final String KEY_FOOTER_4 = Configurations.KEY_FOOTER_4;
    private final Map<String, String> cachedConfigs = new HashMap<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    // User Account Views
    private TextView textProfileAvatar;
    private ImageView iconProfileAvatar;
    private TextView textProfileName;
    private TextView textProfileRole;
    private TextView textProfileUsername;
    private TextView textProfileEmail;
    private TextView textProfilePhone;
    // Company Config Views
    private TextView badgeEditMode;
    private EditText inputCompanyName;
    private EditText inputCompanyPhone;
    private EditText inputGstNumber;
    private EditText inputPanNumber;
    private EditText inputAddress;
    private EditText inputDistrict;
    private EditText inputPostalCode;
    private EditText inputState;
    private EditText inputFooter1;
    private EditText inputFooter2;
    private EditText inputFooter3;
    private EditText inputFooter4;
    // Banners & Controls
    private View bannerError;
    private TextView textError;
    private View bannerSuccess;
    private TextView textSuccess;
    private View layoutEditActions;
    private View btnSaveProfile;
    private View containerSaveIdle;
    private ProgressBar progressSaving;
    private View btnCancelEdit;
    private View btnEnterEditMode;
    private NavigationView navView;
    private MenuItem menuEditItem;
    private ConfigRepository configRepository;
    private boolean isEditMode = false;
    private boolean isSaving = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, getString(R.string.profile), true);

        View headerContainer = findViewById(R.id.header_container);
        View mainContent = findViewById(R.id.main_content);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        navView = findViewById(R.id.nav_view);

        applyDrawerInsets(drawerLayout, headerContainer, mainContent, navView);
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_profile);
        applyEdgeToEdgeInsets(headerContainer, mainContent);

        configRepository = new ConfigRepository(getApplication());

        initViews();
        setupListeners();
        bindUserDetails();
        loadConfigurationData();
        setEditMode(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (navView != null) {
            navView.setCheckedItem(R.id.nav_profile);
        }
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        if (drawerLayout != null) {
            updateNavHeaderUser(drawerLayout);
        }
    }

    private void initViews() {
        // User Profile Views
        textProfileAvatar = findViewById(R.id.text_profile_avatar);
        iconProfileAvatar = findViewById(R.id.icon_profile_avatar);
        textProfileName = findViewById(R.id.text_profile_name);
        textProfileRole = findViewById(R.id.text_profile_role);
        textProfileUsername = findViewById(R.id.text_profile_username);
        textProfileEmail = findViewById(R.id.text_profile_email);
        textProfilePhone = findViewById(R.id.text_profile_phone);

        // Config Inputs
        badgeEditMode = findViewById(R.id.badge_edit_mode);
        inputCompanyName = findViewById(R.id.input_company_name);
        inputCompanyPhone = findViewById(R.id.input_company_phone);
        inputGstNumber = findViewById(R.id.input_gst_number);
        inputPanNumber = findViewById(R.id.input_pan_number);
        inputAddress = findViewById(R.id.input_address);
        inputDistrict = findViewById(R.id.input_district);
        inputPostalCode = findViewById(R.id.input_postal_code);
        inputState = findViewById(R.id.input_state);
        inputFooter1 = findViewById(R.id.input_footer_1);
        inputFooter2 = findViewById(R.id.input_footer_2);
        inputFooter3 = findViewById(R.id.input_footer_3);
        inputFooter4 = findViewById(R.id.input_footer_4);

        // Banners & Action Buttons
        bannerError = findViewById(R.id.banner_error);
        textError = findViewById(R.id.text_error);
        bannerSuccess = findViewById(R.id.banner_success);
        textSuccess = findViewById(R.id.text_success);

        layoutEditActions = findViewById(R.id.layout_edit_actions);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        containerSaveIdle = findViewById(R.id.container_save_idle);
        progressSaving = findViewById(R.id.progress_saving);
        btnCancelEdit = findViewById(R.id.btn_cancel_edit);
        btnEnterEditMode = findViewById(R.id.btn_enter_edit_mode);
    }

    private void setupListeners() {
        btnEnterEditMode.setOnClickListener(v -> setEditMode(true));

        btnCancelEdit.setOnClickListener(v -> {
            restoreCachedConfigs();
            hideBanners();
            setEditMode(false);
        });

        btnSaveProfile.setOnClickListener(v -> saveCompanyConfigurations());
    }

    private void bindUserDetails() {
        User user = GlobalStore.getInstance().getLoggedInUser();
        if (user != null) {
            populateUserCard(user);
            return;
        }

        // Fallback: Query current logged in user from room database
        SharedPreferences preferences = getSharedPreferences(Configurations.PREF_NAME, MODE_PRIVATE);
        long userId = preferences.getLong("user_id", -1);
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                Database db = Database.getInstance(getApplicationContext());
                User dbUser = null;
                if (userId > 0) {
                    dbUser = db.userDao().getUserById(userId);
                }
                if (dbUser == null) {
                    dbUser = db.userDao().getUserForLogin("admin");
                }
                final User resolvedUser = dbUser;
                if (resolvedUser != null) {
                    GlobalStore.getInstance().setLoggedInUser(resolvedUser);
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            populateUserCard(resolvedUser);
                        }
                    });
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void populateUserCard(@NonNull User user) {
        String displayName = (user.name != null && !user.name.trim().isEmpty()) ? user.name.trim() : user.username;
        textProfileName.setText(displayName);

        if (user.username != null && !user.username.trim().isEmpty()) {
            textProfileUsername.setText(String.format("@%s", user.username.trim()));
            textProfileUsername.setVisibility(View.VISIBLE);
        } else {
            textProfileUsername.setVisibility(View.GONE);
        }

        if (user.role != null && !user.role.trim().isEmpty()) {
            textProfileRole.setText(user.role.trim().toUpperCase());
            textProfileRole.setVisibility(View.VISIBLE);
        } else {
            textProfileRole.setVisibility(View.GONE);
        }

        if (user.email != null && !user.email.trim().isEmpty()) {
            textProfileEmail.setText(user.email.trim());
            textProfileEmail.setVisibility(View.VISIBLE);
        } else {
            textProfileEmail.setText("Not specified");
        }

        if (user.contact != null && !user.contact.trim().isEmpty()) {
            textProfilePhone.setText(user.contact.trim());
            textProfilePhone.setVisibility(View.VISIBLE);
        } else {
            textProfilePhone.setText("Not specified");
        }

        // Avatar Initials
        String initials = extractInitials(displayName);
        if (!initials.isEmpty()) {
            textProfileAvatar.setText(initials);
            textProfileAvatar.setVisibility(View.VISIBLE);
            iconProfileAvatar.setVisibility(View.GONE);
        } else {
            textProfileAvatar.setVisibility(View.GONE);
            iconProfileAvatar.setVisibility(View.VISIBLE);
        }
    }

    private String extractInitials(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) return "";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        } else {
            return ("" + parts[0].charAt(0)).toUpperCase();
        }
    }

    private void loadConfigurationData() {
        configRepository.getAllConfigs().observe(this, (List<Config> configs) -> {
            if (configs != null) {
                for (Config config : configs) {
                    if (config.getConfigKey() != null) {
                        cachedConfigs.put(config.getConfigKey(), config.getConfigValue() != null ? config.getConfigValue() : "");
                    }
                }
                // Only update fields automatically if the user is not actively editing
                if (!isEditMode && !isSaving) {
                    restoreCachedConfigs();
                }
            }
        });
    }

    private void restoreCachedConfigs() {
        inputCompanyName.setText(cachedConfigs.getOrDefault(KEY_COMPANY_NAME, ""));
        inputCompanyPhone.setText(cachedConfigs.getOrDefault(KEY_COMPANY_PHONE, ""));
        inputGstNumber.setText(cachedConfigs.getOrDefault(KEY_GST_NUMBER, ""));
        inputPanNumber.setText(cachedConfigs.getOrDefault(KEY_PAN_NUMBER, ""));
        inputAddress.setText(cachedConfigs.getOrDefault(KEY_ADDRESS, ""));
        inputDistrict.setText(cachedConfigs.getOrDefault(KEY_DISTRICT, ""));
        inputPostalCode.setText(cachedConfigs.getOrDefault(KEY_POSTAL_CODE, ""));
        inputState.setText(cachedConfigs.getOrDefault(KEY_STATE, ""));
        inputFooter1.setText(cachedConfigs.getOrDefault(KEY_FOOTER_1, ""));
        inputFooter2.setText(cachedConfigs.getOrDefault(KEY_FOOTER_2, ""));
        inputFooter3.setText(cachedConfigs.getOrDefault(KEY_FOOTER_3, ""));
        inputFooter4.setText(cachedConfigs.getOrDefault(KEY_FOOTER_4, ""));
    }

    private void setEditMode(boolean edit) {
        isEditMode = edit;

        badgeEditMode.setText(edit ? "Editing" : "View Mode");
        badgeEditMode.setTextColor(ContextCompat.getColor(this, edit ? R.color.accent_teal : R.color.fg_muted));

        setFieldEditable(inputCompanyName, edit);
        setFieldEditable(inputCompanyPhone, edit);
        setFieldEditable(inputGstNumber, edit);
        setFieldEditable(inputPanNumber, edit);
        setFieldEditable(inputAddress, edit);
        setFieldEditable(inputDistrict, edit);
        setFieldEditable(inputPostalCode, edit);
        setFieldEditable(inputState, edit);
        setFieldEditable(inputFooter1, edit);
        setFieldEditable(inputFooter2, edit);
        setFieldEditable(inputFooter3, edit);
        setFieldEditable(inputFooter4, edit);

        btnEnterEditMode.setVisibility(edit ? View.GONE : View.VISIBLE);
        layoutEditActions.setVisibility(edit ? View.VISIBLE : View.GONE);


        if(!isCurrentUserAdmin()) {
            btnEnterEditMode.setVisibility(View.GONE);
        }

        if (edit) {
            inputCompanyName.requestFocus();
            inputCompanyName.setSelection(inputCompanyName.getText().length());
        }

        updateMenuIcon();
    }

    private void setFieldEditable(@NonNull EditText editText, boolean editable) {
        editText.setFocusable(editable);
        editText.setFocusableInTouchMode(editable);
        editText.setCursorVisible(editable);
        editText.setLongClickable(editable);
        editText.setTextColor(ContextCompat.getColor(this, R.color.fg));
        editText.setBackgroundResource(R.drawable.bg_field_normal);
        editText.setAlpha(editable ? 1.0f : 0.88f);
    }

    private void saveCompanyConfigurations() {
        hideBanners();

        String companyName = inputCompanyName.getText() != null ? inputCompanyName.getText().toString().trim() : "";
        String companyPhone = inputCompanyPhone.getText() != null ? inputCompanyPhone.getText().toString().trim() : "";
        String gstNumber = inputGstNumber.getText() != null ? inputGstNumber.getText().toString().trim().toUpperCase() : "";
        String panNumber = inputPanNumber.getText() != null ? inputPanNumber.getText().toString().trim().toUpperCase() : "";
        String address = inputAddress.getText() != null ? inputAddress.getText().toString().trim() : "";
        String district = inputDistrict.getText() != null ? inputDistrict.getText().toString().trim() : "";
        String postalCode = inputPostalCode.getText() != null ? inputPostalCode.getText().toString().trim() : "";
        String state = inputState.getText() != null ? inputState.getText().toString().trim() : "";
        String footer1 = inputFooter1.getText() != null ? inputFooter1.getText().toString().trim() : "";
        String footer2 = inputFooter2.getText() != null ? inputFooter2.getText().toString().trim() : "";
        String footer3 = inputFooter3.getText() != null ? inputFooter3.getText().toString().trim() : "";
        String footer4 = inputFooter4.getText() != null ? inputFooter4.getText().toString().trim() : "";

        if (TextUtils.isEmpty(companyName)) {
            showError("Company / Business Name cannot be empty.");
            inputCompanyName.requestFocus();
            return;
        }

        setSavingState(true);

        Map<String, String> configsToSave = new HashMap<>();
        configsToSave.put(KEY_COMPANY_NAME, companyName);
        configsToSave.put(KEY_COMPANY_PHONE, companyPhone);
        configsToSave.put(KEY_GST_NUMBER, gstNumber);
        configsToSave.put(KEY_PAN_NUMBER, panNumber);
        configsToSave.put(KEY_ADDRESS, address);
        configsToSave.put(KEY_DISTRICT, district);
        configsToSave.put(KEY_POSTAL_CODE, postalCode);
        configsToSave.put(KEY_STATE, state);
        configsToSave.put(KEY_FOOTER_1, footer1);
        configsToSave.put(KEY_FOOTER_2, footer2);
        configsToSave.put(KEY_FOOTER_3, footer3);
        configsToSave.put(KEY_FOOTER_4, footer4);

        configRepository.saveConfigs(configsToSave, new ConfigRepository.ConfigActionCallback() {
            @Override
            public void onSuccess() {
                setSavingState(false);
                cachedConfigs.putAll(configsToSave);
                showSuccess("Company configuration saved successfully!");
                setEditMode(false);
                showToast("Profile saved successfully");
                AuditTrailHelper.logEdit(ProfileActivity.this, AuditTrail.MODULE_CONFIG, companyName, "Updated company profile and business details for: " + companyName);
            }

            @Override
            public void onError(String message) {
                setSavingState(false);
                showError(message != null ? message : "Failed to save configuration.");
            }
        });
    }

    private void setSavingState(boolean saving) {
        this.isSaving = saving;
        progressSaving.setVisibility(saving ? View.VISIBLE : View.GONE);
        containerSaveIdle.setVisibility(saving ? View.GONE : View.VISIBLE);
        btnSaveProfile.setEnabled(!saving);
        btnCancelEdit.setEnabled(!saving);
        if (menuEditItem != null) {
            menuEditItem.setEnabled(!saving);
        }
    }

    private void showError(String message) {
        textError.setText(message);
        bannerError.setVisibility(View.VISIBLE);
        bannerSuccess.setVisibility(View.GONE);
    }

    private void showSuccess(String message) {
        textSuccess.setText(message);
        bannerSuccess.setVisibility(View.VISIBLE);
        bannerError.setVisibility(View.GONE);
        handler.postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                bannerSuccess.setVisibility(View.GONE);
            }
        }, 4000);
    }

    private void hideBanners() {
        bannerError.setVisibility(View.GONE);
        bannerSuccess.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!isCurrentUserAdmin()) return true;
        getMenuInflater().inflate(R.menu.menu_profile, menu);
        menuEditItem = menu.findItem(R.id.action_edit_profile);
        updateMenuIcon();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit_profile) {
            if (isEditMode) {
                // Currently in edit mode, tapping toggles cancel
                restoreCachedConfigs();
                hideBanners();
                setEditMode(false);
            } else {
                setEditMode(true);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateMenuIcon() {
        if (menuEditItem != null) {
            if (isEditMode) {
                menuEditItem.setIcon(R.drawable.ic_close);
                menuEditItem.setTitle(R.string.cancel);
            } else {
                menuEditItem.setIcon(R.drawable.ic_edit);
                menuEditItem.setTitle("Edit Profile");
            }
            if (menuEditItem.getIcon() != null) {
                menuEditItem.getIcon().setTint(ContextCompat.getColor(this, R.color.white));
            }
        }
    }
}