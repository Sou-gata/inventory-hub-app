package in.gbtsolutions.inventoryhub.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.pm.PackageInfo;
import android.util.TypedValue;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import in.gbtsolutions.inventoryhub.Configurations;
import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.SeedDB;
import in.gbtsolutions.inventoryhub.helpers.HelperMethods;
import in.gbtsolutions.inventoryhub.helpers.ThemeManager;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.User;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiClient;
import in.gbtsolutions.inventoryhub.online.api.OnlineApiService;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.online.models.ApiResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    SharedPreferences sharedPreferences;
    private LinearLayout emailFieldContainer, passwordFieldContainer;
    private EditText emailInput, passwordInput;
    private ImageView emailIcon, passwordIcon, checkboxTick;
    private ImageButton btnTogglePassword, btnMenu;
    private TextView emailLabel, passwordLabel, errorText;
    private LinearLayout errorBanner, rememberMeRow;
    private FrameLayout checkboxBox, btnSignIn;
    private LinearLayout signInIdleContent, signInLoadingContent;
    private TextView tvDeviceID, tvValidity, tvAppVersion;
    private AlertDialog syncProgressDialog;
    private boolean showPassword = false;
    private boolean rememberMe = false;
    private boolean loading = false;
    private boolean isSyncing = false;
    private String serialNo;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sharedPreferences = getSharedPreferences(Configurations.PREF_NAME, MODE_PRIVATE);

        firstLogin();
        checkAutoLogin();

        bindViews();
        setupFocusListeners();
        setupPasswordToggle();
        setupRememberMe();
        setupSignInButton();
        setupMenu();

        serialNo = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String serialText = "Device ID: " + (serialNo != null ? serialNo.toUpperCase() : "");
        tvDeviceID.setText(serialText);

        String appVersion = getAppVersion();
        if (tvAppVersion != null) {
            tvAppVersion.setText("V " + appVersion);
        }
        loadCachedValidity();
    }

    private void firstLogin() {
        executorService.execute(() -> {
            Database db = Database.getInstance(getApplicationContext());
            if (db.userDao().count() == 0 || !sharedPreferences.contains("is_first_login")) {
                SeedDB.seedDB(LoginActivity.this);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("is_first_login", false);
                editor.apply();
            }
        });
    }

    private void checkAutoLogin() {
        long userId = sharedPreferences.getLong("user_id", -1);
        if (userId > 0) {
            executorService.execute(() -> {
                Database db = Database.getInstance(getApplicationContext());
                User user = db.userDao().getUserById(userId);
                if (user != null) {
                    GlobalStore.getInstance().setLoggedInUser(user);
                    runOnUiThread(() -> {
                        if (!isFinishing() && !isDestroyed()) {
                            onLoginSuccess();
                        }
                    });
                } else {
                    // Stale user ID from wiped database, clear it
                    sharedPreferences.edit().remove("user_id").apply();
                }
            });
        }
    }

    private void bindViews() {
        emailFieldContainer = findViewById(R.id.emailFieldContainer);
        passwordFieldContainer = findViewById(R.id.passwordFieldContainer);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        emailIcon = findViewById(R.id.emailIcon);
        passwordIcon = findViewById(R.id.passwordIcon);
        emailLabel = findViewById(R.id.emailLabel);
        passwordLabel = findViewById(R.id.passwordLabel);
        btnTogglePassword = findViewById(R.id.btnTogglePassword);
        btnMenu = findViewById(R.id.btnMenu);
        errorBanner = findViewById(R.id.errorBanner);
        errorText = findViewById(R.id.errorText);
        rememberMeRow = findViewById(R.id.rememberMeRow);
        checkboxBox = findViewById(R.id.checkboxBox);
        checkboxTick = findViewById(R.id.checkboxTick);
        btnSignIn = findViewById(R.id.btnSignIn);
        signInIdleContent = findViewById(R.id.signInIdleContent);
        signInLoadingContent = findViewById(R.id.signInLoadingContent);
        tvDeviceID = findViewById(R.id.tv_device_id);
        tvValidity = findViewById(R.id.tv_validity);
        tvAppVersion = findViewById(R.id.tv_app_version);
    }

    private void setupFocusListeners() {
        emailInput.setOnFocusChangeListener((v, hasFocus) ->
                onFieldFocusChange(hasFocus, emailFieldContainer, emailIcon, emailLabel));

        passwordInput.setOnFocusChangeListener((v, hasFocus) ->
                onFieldFocusChange(hasFocus, passwordFieldContainer, passwordIcon, passwordLabel));
    }

    private void onFieldFocusChange(boolean focused, LinearLayout container, ImageView icon, TextView label) {
        int teal = ContextCompat.getColor(this, R.color.accent_teal);
        int dim = ContextCompat.getColor(this, R.color.fg_dim);
        int muted = ContextCompat.getColor(this, R.color.fg_muted);

        container.setBackgroundResource(focused ? R.drawable.bg_field_focused : R.drawable.bg_field_normal);
        icon.setImageTintList(ColorStateList.valueOf(focused ? teal : dim));
        label.setTextColor(focused ? teal : muted);
    }

    private void setupPasswordToggle() {
        btnTogglePassword.setOnClickListener(v -> {
            showPassword = !showPassword;
            int cursorPos = passwordInput.getSelectionStart();
            passwordInput.setInputType(showPassword
                    ? android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    : android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordInput.setSelection(Math.max(cursorPos, 0));
            btnTogglePassword.setImageResource(showPassword ? R.drawable.ic_eye : R.drawable.ic_eye_off);
        });
    }

    private void setupRememberMe() {
        rememberMeRow.setOnClickListener(v -> {
            rememberMe = !rememberMe;
            checkboxBox.setBackgroundResource(rememberMe
                    ? R.drawable.bg_checkbox_checked
                    : R.drawable.bg_checkbox_unchecked);
            checkboxTick.setVisibility(rememberMe ? View.VISIBLE : View.GONE);
        });
    }

    private void setupSignInButton() {
        btnSignIn.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        if (loading) return;

        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showError("Please fill in all fields.");
            return;
        }

        hideError();
        setLoading(true);

        AppModeManager modeManager = AppModeManager.getInstance(getApplicationContext());

        if (modeManager.isOnlineMode()) {
            JsonObject req = new JsonObject();
            req.addProperty("username", email);
            req.addProperty("mobile", email);
            req.addProperty("device_id", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
            req.addProperty("password", password);

            OnlineApiService apiService = OnlineApiClient.getInstance().getApiService(LoginActivity.this);

            Call<ApiResponse<JsonObject>> call = apiService.login(req);
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse<JsonObject>> call, @NonNull Response<ApiResponse<JsonObject>> response) {
                    if (isFinishing() || isDestroyed()) return;

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess() && response.body().getData() != null) {
                        JsonObject data = response.body().getData();
                        String token = null;
                        if (data.has("token") && !data.get("token").isJsonNull()) {
                            token = data.get("token").getAsString();
                        } else if (data.has("access_token") && !data.get("access_token").isJsonNull()) {
                            token = data.get("access_token").getAsString();
                        }

                        if (token != null && !token.trim().isEmpty()) {
                            modeManager.setAuthToken(token.trim());
                            sharedPreferences.edit().putString(Configurations.KEY_AUTH_TOKEN, token.trim()).apply();
                        }

                        User user = new User();
                        if (data.has("user") && data.get("user").isJsonObject()) {
                            JsonObject uObj = data.getAsJsonObject("user");
                            user.id = uObj.has("id") ? uObj.get("id").getAsInt() : 1;
                            user.name = uObj.has("name") && !uObj.get("name").isJsonNull() ? uObj.get("name").getAsString() : email;
                            user.username = uObj.has("username") && !uObj.get("username").isJsonNull() ? uObj.get("username").getAsString() : email;
                            user.role = uObj.has("role") && !uObj.get("role").isJsonNull() ? uObj.get("role").getAsString() : "admin";
                        } else {
                            user.id = data.has("user_id") ? data.get("user_id").getAsInt() : 1;
                            user.name = data.has("name") && !data.get("name").isJsonNull() ? data.get("name").getAsString() : email;
                            user.username = email;
                            user.role = data.has("role") && !data.get("role").isJsonNull() ? data.get("role").getAsString() : "admin";
                        }

                        GlobalStore.getInstance().setLoggedInUser(user);
                        setLoading(false);
                        onLoginSuccess();
                    } else {
                        setLoading(false);
                        String errorMsg = "Invalid credentials.";
                        if (response.body() != null && !TextUtils.isEmpty(response.body().getMessage())) {
                            errorMsg = response.body().getMessage();
                        } else {
                            try {
                                if (response.errorBody() != null) {
                                    String errJson = response.errorBody().string();
                                    JsonObject errObj = new Gson().fromJson(errJson, JsonObject.class);
                                    if (errObj != null && errObj.has("message")) {
                                        errorMsg = errObj.get("message").getAsString();
                                    }
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        showError(errorMsg);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse<JsonObject>> call,
                                      @NonNull Throwable t) {
                    if (isFinishing() || isDestroyed()) return;
                    setLoading(false);
                    showError("Connection error: " + (t.getMessage() != null ? t.getMessage() : "Unable to reach server"));
                }
            });
            return;
        }

        executorService.execute(() -> {
            Database db = Database.getInstance(getApplicationContext());
            User user = db.userDao().getUserForLogin(email);

            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                if (user == null || !HelperMethods.comparePassword(password, user.password)) {
                    setLoading(false);
                    showError("Invalid credentials.");
                    return;
                }

                GlobalStore.getInstance().setLoggedInUser(user);
                setLoading(false);
                onLoginSuccess();
            });
        });
    }

    private void setLoading(boolean isLoading) {
        loading = isLoading;
        btnSignIn.setEnabled(!isLoading);
        btnSignIn.setBackgroundResource(isLoading
                ? R.drawable.bg_button_loading
                : R.drawable.bg_button_ripple);
        signInIdleContent.setVisibility(isLoading ? View.GONE : View.VISIBLE);
        signInLoadingContent.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        errorText.setText(message);
        errorBanner.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        errorBanner.setVisibility(View.GONE);
    }

    private void onLoginSuccess() {
        User user = GlobalStore.getInstance().getLoggedInUser();
        if (user == null) return;
        GlobalStore.getInstance().loadSettings(this);
        if (rememberMe) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong("user_id", user.id);
            editor.putBoolean("isAdmin", user.role != null && user.role.equalsIgnoreCase("admin"));
            editor.apply();
        } else {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("user_id");
            editor.remove("isAdmin");
            editor.apply();
        }

        Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
        startActivity(intent);
        BaseActivity.applyOpenTransition(this);
        finish();
    }

    @Override
    public void finish() {
        super.finish();
        BaseActivity.applyCloseTransition(this);
    }

    private void setupMenu() {
        btnMenu.setOnClickListener(this::showOptionsMenu);
    }

    private void showOptionsMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_login, popup.getMenu());
        popup.setForceShowIcon(true);

        MenuItem themeItem = popup.getMenu().findItem(R.id.action_theme_change);
        if (themeItem != null) {
            boolean isDark = ThemeManager.isDarkMode(this);
            themeItem.setIcon(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);
            themeItem.setTitle(isDark ? "Light Mode" : "Dark Mode");
        }

        popup.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_theme_change) {
                ThemeManager.toggleTheme(this);
                return true;
            } else if (itemId == R.id.action_sync_device_settings) {
                showSyncSettingsDialog();
                return true;
            }
            return false;
        });

        popup.show();
    }

    private void showSyncSettingsDialog() {
        if (isSyncing) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sync_settings, null);
        TextInputLayout tilEmail = dialogView.findViewById(R.id.til_sync_email);
        TextInputEditText etEmail = dialogView.findViewById(R.id.et_sync_email);
        TextInputLayout tilPhone = dialogView.findViewById(R.id.til_sync_phone);
        TextInputEditText etPhone = dialogView.findViewById(R.id.et_sync_phone);

        // Pre-fill previously entered email and phone if available
        String savedEmail = sharedPreferences.getString("sync_email", "");
        String savedPhone = sharedPreferences.getString("sync_phone", "");
        if (!TextUtils.isEmpty(savedEmail) && etEmail != null) {
            etEmail.setText(savedEmail);
            etEmail.setSelection(savedEmail.length());
        }
        if (!TextUtils.isEmpty(savedPhone) && etPhone != null) {
            etPhone.setText(savedPhone);
            etPhone.setSelection(savedPhone.length());
        }

        if (etEmail != null && tilEmail != null) {
            etEmail.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    tilEmail.setError(null);
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
        if (etPhone != null && tilPhone != null) {
            etPhone.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    tilPhone.setError(null);
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Sync Settings")
                .setView(dialogView)
                .setPositiveButton("Sync", null)
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            android.widget.Button syncBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (syncBtn != null) {
                syncBtn.setOnClickListener(v -> {
                    String email = etEmail != null && etEmail.getText() != null
                            ? etEmail.getText().toString().trim() : "";
                    String phone = etPhone != null && etPhone.getText() != null
                            ? etPhone.getText().toString().trim() : "";

                    boolean hasError = false;

                    if (TextUtils.isEmpty(email)) {
                        tilEmail.setError("Email is mandatory");
                        hasError = true;
                    } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        tilEmail.setError("Please enter a valid email address");
                        hasError = true;
                    } else {
                        tilEmail.setError(null);
                    }

                    if (TextUtils.isEmpty(phone)) {
                        tilPhone.setError("Phone number is mandatory");
                        hasError = true;
                    } else if (phone.length() != 10 || !phone.matches("\\d{10}")) {
                        tilPhone.setError("Phone number must be exactly 10 digits");
                        hasError = true;
                    } else {
                        tilPhone.setError(null);
                    }

                    if (hasError) {
                        return;
                    }

                    sharedPreferences.edit()
                            .putString("sync_email", email)
                            .putString("sync_phone", phone)
                            .apply();

                    dialog.dismiss();
                    performSyncDeviceSettings(email, phone);
                });
            }
        });

        dialog.show();
    }

    private void performSyncDeviceSettings(String email, String phone) {
        if (isSyncing) return;
        isSyncing = true;
        showSyncDialog();

        if (serialNo == null || serialNo.isEmpty()) {
            serialNo = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        String deviceId = serialNo != null ? serialNo.trim().toUpperCase() : "";
        String appVersion = getAppVersion();

        JsonObject reqJson = new JsonObject();
        reqJson.addProperty("device_id", deviceId);
        reqJson.addProperty("app_version", appVersion);
        reqJson.addProperty("phone_no", phone);
        reqJson.addProperty("email", email);

        OnlineApiService apiService = OnlineApiClient.getInstance().getApiService(LoginActivity.this);
        Call<JsonObject> call = apiService.lookupDevice(reqJson);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<JsonObject> call, @NonNull Response<JsonObject> response) {
                if (isFinishing() || isDestroyed()) {
                    isSyncing = false;
                    return;
                }

                if (response.isSuccessful() && response.body() != null) {
                    JsonObject body = response.body();
                    boolean isSuccess = body.has("success") && body.get("success").getAsBoolean();
                    if (isSuccess) {
                        JsonObject data = body.has("data") && body.get("data").isJsonObject()
                                ? body.getAsJsonObject("data") : null;
                        if (data != null) {
                            executorService.execute(() -> {
                                try {
                                    Database db = Database.getInstance(getApplicationContext());

                                    // Clear / drop users table
                                    db.userDao().deleteAll();

                                    // Parse & save users from response into existing users table
                                    if (data.has("users") && data.get("users").isJsonArray()) {
                                        JsonArray usersArray = data.getAsJsonArray("users");
                                        List<User> userList = new ArrayList<>();
                                        for (JsonElement elem : usersArray) {
                                            if (!elem.isJsonObject()) continue;
                                            JsonObject uObj = elem.getAsJsonObject();
                                            int userId = uObj.has("id") ? uObj.get("id").getAsInt() : 0;
                                            String name = uObj.has("name") && !uObj.get("name").isJsonNull() ? uObj.get("name").getAsString() : "";
                                            String username = uObj.has("username") && !uObj.get("username").isJsonNull() ? uObj.get("username").getAsString() : "";
                                            String mobile = uObj.has("mobile") && !uObj.get("mobile").isJsonNull() ? uObj.get("mobile").getAsString() : "";
                                            String password = uObj.has("password") && !uObj.get("password").isJsonNull() ? uObj.get("password").getAsString() : "";
                                            String role = uObj.has("role") && !uObj.get("role").isJsonNull() ? uObj.get("role").getAsString() : "user";
                                            int isActive = uObj.has("is_active") && !uObj.get("is_active").isJsonNull() ? uObj.get("is_active").getAsInt() : 1;

                                            String emailVal = uObj.has("email") && !uObj.get("email").isJsonNull() ? uObj.get("email").getAsString() : null;
                                            String contactVal = !TextUtils.isEmpty(mobile) ? mobile : null;

                                            User user = new User(name, username, password, emailVal, contactVal, role, isActive == 1);
                                            user.id = userId;
                                            userList.add(user);
                                        }
                                        db.userDao().insertAll(userList);
                                    }

                                    // Save token if present in handshake response
                                    if (data.has("token") && !data.get("token").isJsonNull()) {
                                        String handshakeToken = data.get("token").getAsString();
                                        AppModeManager.getInstance(getApplicationContext())
                                                .setAuthToken(handshakeToken.trim());
                                        sharedPreferences.edit().putString(Configurations.KEY_AUTH_TOKEN, handshakeToken.trim()).apply();
                                    }

                                    // Update configs table with agency details
                                    if (data.has("agency") && data.get("agency").isJsonObject()) {
                                        JsonObject agency = data.getAsJsonObject("agency");
                                        int agencyId = data.has("agency_id") && !data.get("agency_id").isJsonNull() ? data.get("agency_id").getAsInt() : 0;
                                        saveAgencyConfigs(db, agencyId, agency);
                                    }

                                    // Clear any stale auto-login session
                                    sharedPreferences.edit()
                                            .remove("user_id")
                                            .remove("isAdmin")
                                            .putBoolean("is_first_login", false)
                                            .apply();

                                    GlobalStore.getInstance().loadSettings(getApplicationContext());

                                    runOnUiThread(() -> {
                                        isSyncing = false;
                                        dismissSyncDialog();
                                        hideError();
                                        loadCachedValidity();
                                        String successMsg = body.has("message") && !body.get("message").isJsonNull()
                                                ? body.get("message").getAsString() : "Device verified successfully";
                                        Toast.makeText(LoginActivity.this, successMsg, Toast.LENGTH_LONG).show();
                                    });
                                } catch (Exception e) {
                                    runOnUiThread(() -> {
                                        isSyncing = false;
                                        dismissSyncDialog();
                                        showError("Error saving synced data: " + e.getMessage());
                                    });
                                }
                            });
                            return;
                        }
                    }

                    // Success is false or data is null
                    String msg = body.has("message") && !body.get("message").isJsonNull()
                            ? body.get("message").getAsString() : "Device verification failed";
                    isSyncing = false;
                    dismissSyncDialog();
                    showError(msg);
                    Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                } else {
                    // HTTP error code (e.g. 400, 404, etc.)
                    String errorMsg = "Device verification failed (HTTP " + response.code() + ")";
                    try {
                        if (response.errorBody() != null) {
                            String errStr = response.errorBody().string();
                            JsonObject obj = new Gson().fromJson(errStr, JsonObject.class);
                            if (obj != null && obj.has("message") && !obj.get("message").isJsonNull()) {
                                errorMsg = obj.get("message").getAsString();
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    isSyncing = false;
                    dismissSyncDialog();
                    showError(errorMsg);
                    Toast.makeText(LoginActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<JsonObject> call, @NonNull Throwable t) {
                if (isFinishing() || isDestroyed()) {
                    isSyncing = false;
                    return;
                }
                isSyncing = false;
                dismissSyncDialog();
                String msg = "Connection error: " + (t.getMessage() != null ? t.getMessage() : "Unable to reach server");
                showError(msg);
                Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void saveAgencyConfigs(Database db, int agencyId, JsonObject agency) {
        if (agency == null) return;
        Map<String, String> configs = new HashMap<>();

        // Map agency fields to standard config keys used across the app
        putIfPresent(configs, "company_name", agency, "name");
        putIfPresent(configs, "company_phone", agency, "phone_no");
        putIfPresent(configs, "gst_number", agency, "gst_no");
        putIfPresent(configs, "pan_number", agency, "pan_no");
        putIfPresent(configs, "address", agency, "address");
        putIfPresent(configs, "district", agency, "district_city");
        putIfPresent(configs, "state", agency, "state");
        putIfPresent(configs, "postal_code", agency, "pin_code");
        putIfPresent(configs, "licence_start_date", agency, "licence_start_date");
        putIfPresent(configs, "licence_end_date", agency, "licence_end_date");
        putIfPresent(configs, "agency_is_active", agency, "is_active");
        if (agencyId > 0) {
            configs.put("agency_id", String.valueOf(agencyId));
        }

        // Also store exact agency keys for compatibility
//        putIfPresent(configs, "name", agency, "name");
//        putIfPresent(configs, "phone_no", agency, "phone_no");
//        putIfPresent(configs, "gst_no", agency, "gst_no");
//        putIfPresent(configs, "pan_no", agency, "pan_no");
//        putIfPresent(configs, "district_city", agency, "district_city");
//        putIfPresent(configs, "pin_code", agency, "pin_code");

        for (Map.Entry<String, String> entry : configs.entrySet()) {
            Config existing = db.configDao().getConfigByKey(entry.getKey());
            if (existing != null) {
                existing.setConfigValue(entry.getValue());
                db.configDao().update(existing);
            } else {
                db.configDao().insert(new Config(entry.getKey(), entry.getValue()));
            }
        }
    }

    private void putIfPresent(Map<String, String> map, String configKey, JsonObject json, String jsonKey) {
        if (json.has(jsonKey) && !json.get(jsonKey).isJsonNull()) {
            map.put(configKey, json.get(jsonKey).getAsString());
        }
    }

    private void showSyncDialog() {
        if (syncProgressDialog == null) {
            ProgressBar progressBar = new ProgressBar(this);
            progressBar.setPadding(40, 40, 40, 40);
            syncProgressDialog = new MaterialAlertDialogBuilder(this)
                    .setTitle("Syncing Device Settings")
                    .setMessage("Connecting to server and verifying device...")
                    .setView(progressBar)
                    .setCancelable(false)
                    .create();
        }
        syncProgressDialog.show();
    }

    private void dismissSyncDialog() {
        if (syncProgressDialog != null && syncProgressDialog.isShowing()) {
            syncProgressDialog.dismiss();
        }
    }

    private void loadCachedValidity() {
        executorService.execute(() -> {
            Database db = Database.getInstance(getApplicationContext());
            String licenceEndDate = db.configDao().getValueByKey("licence_end_date");
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                updateValidityDisplay(licenceEndDate);
            });
        });
    }

    private void updateValidityDisplay(String licenceEndDateStr) {
        if (tvValidity == null) return;
        if (TextUtils.isEmpty(licenceEndDateStr)) {
            tvValidity.setVisibility(View.GONE);
            return;
        }
        try {
            String cleanDate = licenceEndDateStr.trim();
            if (cleanDate.contains("T")) {
                cleanDate = cleanDate.substring(0, cleanDate.indexOf("T"));
            }
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date endDate = sdf.parse(cleanDate);
            if (endDate != null) {
                long diffMillis = endDate.getTime() - System.currentTimeMillis();
                long days = TimeUnit.MILLISECONDS.toDays(diffMillis);
                if (days > 1) {
                    tvValidity.setText("License expires in " + days + " days");
                } else if (days == 1) {
                    tvValidity.setText("License expires tomorrow");
                } else if (days == 0) {
                    tvValidity.setText("License expires today");
                } else {
                    tvValidity.setText("License expired");
                }
                tvValidity.setVisibility(View.VISIBLE);
                return;
            }
        } catch (Exception ignored) {
        }
        tvValidity.setVisibility(View.GONE);
    }

    private String getAppVersion() {
        String versionName = "1.0.1";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            if (pInfo != null && pInfo.versionName != null) {
                versionName = pInfo.versionName;
            }
        } catch (Exception ignored) {
        }
        return versionName;
    }


    @Override
    protected void onDestroy() {
        dismissSyncDialog();
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        executorService.shutdown();
    }
}