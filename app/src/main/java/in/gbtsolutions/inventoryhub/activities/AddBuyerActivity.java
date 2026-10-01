package in.gbtsolutions.inventoryhub.activities;

import android.database.sqlite.SQLiteConstraintException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper;
import in.gbtsolutions.inventoryhub.models.AuditTrail;
import in.gbtsolutions.inventoryhub.models.Buyer;

public class AddBuyerActivity extends BaseActivity {

    private EditText inputBuyerName;
    private EditText inputContactPerson;
    private EditText inputPhone;
    private EditText inputEmail;
    private EditText inputGst;
    private EditText inputPan;
    private EditText inputAddress;
    private EditText inputCity;
    private EditText inputState;
    private EditText inputPostalCode;
    private EditText inputCountry;
    private SwitchMaterial switchActive;
    private TextView textStatusHint;
    private EditText inputNotes;

    private View bannerError;
    private TextView textError;
    private View btnSaveBuyer;
    private TextView textSaveButton;
    private View containerSaveIdle;
    private ProgressBar progressSaving;

    private int buyerId = -1;
    private long createdAt = 0;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_buyer);

        buyerId = getIntent().getIntExtra("buyer_id", -1);
        boolean isEditMode = buyerId > 0;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setupToolbar(toolbar, isEditMode ? "Edit Buyer" : "Add Buyer", true);

        View headerContainer = findViewById(R.id.header_container);
        View scrollContainer = findViewById(R.id.scroll_container);
        applyEdgeToEdgeInsets(headerContainer, scrollContainer);

        initViews(isEditMode);
    }

    private void initViews(boolean isEditMode) {
        inputBuyerName = findViewById(R.id.input_buyer_name);
        inputContactPerson = findViewById(R.id.input_buyer_contact_person);
        inputPhone = findViewById(R.id.input_buyer_phone);
        inputEmail = findViewById(R.id.input_buyer_email);
        inputGst = findViewById(R.id.input_buyer_gst);
        inputPan = findViewById(R.id.input_buyer_pan);
        inputAddress = findViewById(R.id.input_buyer_address);
        inputCity = findViewById(R.id.input_buyer_city);
        inputState = findViewById(R.id.input_buyer_state);
        inputPostalCode = findViewById(R.id.input_buyer_postal_code);
        inputCountry = findViewById(R.id.input_buyer_country);
        switchActive = findViewById(R.id.switch_buyer_active);
        textStatusHint = findViewById(R.id.text_status_hint);
        inputNotes = findViewById(R.id.input_buyer_notes);

        bannerError = findViewById(R.id.banner_error);
        textError = findViewById(R.id.text_error);
        btnSaveBuyer = findViewById(R.id.btn_save_buyer);
        textSaveButton = findViewById(R.id.text_save_button);
        containerSaveIdle = findViewById(R.id.container_save_idle);
        progressSaving = findViewById(R.id.progress_saving);

        switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            textStatusHint.setText(isChecked ? "Account is active and in good standing." : "Account is deactivated.");
        });

        if (isEditMode) {
            textSaveButton.setText("Update Buyer");
            String existingName = getIntent().getStringExtra("buyer_name");
            String existingContact = getIntent().getStringExtra("contact_person");
            String existingPhone = getIntent().getStringExtra("phone");
            String existingEmail = getIntent().getStringExtra("email");
            String existingGst = getIntent().getStringExtra("gst");
            String existingPan = getIntent().getStringExtra("pan");
            String existingAddress = getIntent().getStringExtra("address");
            String existingCity = getIntent().getStringExtra("city");
            String existingState = getIntent().getStringExtra("state_code");
            String existingPostal = getIntent().getStringExtra("postal_code");
            String existingCountry = getIntent().getStringExtra("country");
            String existingNotes = getIntent().getStringExtra("notes");
            boolean isActive = getIntent().getBooleanExtra("is_active", true);
            createdAt = getIntent().getLongExtra("created_at", System.currentTimeMillis());

            if (existingName != null) inputBuyerName.setText(existingName);
            if (existingContact != null) inputContactPerson.setText(existingContact);
            if (existingPhone != null) inputPhone.setText(existingPhone);
            if (existingEmail != null) inputEmail.setText(existingEmail);
            if (existingGst != null) inputGst.setText(existingGst);
            if (existingPan != null) inputPan.setText(existingPan);
            if (existingAddress != null) inputAddress.setText(existingAddress);
            if (existingCity != null) inputCity.setText(existingCity);
            if (existingState != null) inputState.setText(existingState);
            if (existingPostal != null) inputPostalCode.setText(existingPostal);
            if (existingCountry != null) inputCountry.setText(existingCountry);
            if (existingNotes != null) inputNotes.setText(existingNotes);

            switchActive.setChecked(isActive);
            textStatusHint.setText(isActive ? "Account is active and in good standing." : "Account is deactivated.");
        } else {
            inputCountry.setText("India");
        }

        btnSaveBuyer.setOnClickListener(v -> saveBuyer());
    }

    private void saveBuyer() {
        hideError();

        String name = inputBuyerName.getText() != null ? inputBuyerName.getText().toString().trim() : "";
        String contactPerson = inputContactPerson.getText() != null ? inputContactPerson.getText().toString().trim() : "";
        String phone = inputPhone.getText() != null ? inputPhone.getText().toString().trim() : "";
        String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
        String gst = inputGst.getText() != null ? inputGst.getText().toString().trim().toUpperCase() : "";
        String pan = inputPan.getText() != null ? inputPan.getText().toString().trim().toUpperCase() : "";
        String address = inputAddress.getText() != null ? inputAddress.getText().toString().trim() : "";
        String city = inputCity.getText() != null ? inputCity.getText().toString().trim() : "";
        String state = inputState.getText() != null ? inputState.getText().toString().trim() : "";
        String postalCode = inputPostalCode.getText() != null ? inputPostalCode.getText().toString().trim() : "";
        String country = inputCountry.getText() != null ? inputCountry.getText().toString().trim() : "";
        String notes = inputNotes.getText() != null ? inputNotes.getText().toString().trim() : "";
        boolean isActive = switchActive.isChecked();

        if (TextUtils.isEmpty(name)) {
            showError("Please enter a buyer / company name.");
            inputBuyerName.requestFocus();
            return;
        }

        if (name.length() < 2) {
            showError("Buyer name must be at least 2 characters.");
            inputBuyerName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(phone)) {
            showError("Please enter a phone number.");
            inputPhone.requestFocus();
            return;
        }

        if (phone.length() < 7) {
            showError("Please enter a valid phone number.");
            inputPhone.requestFocus();
            return;
        }

        // Convert empty unique strings to null so SQLite unique constraints allow multiple records
        final String finalEmail = email.isEmpty() ? null : email;
        final String finalGst = gst.isEmpty() ? null : gst;
        final String finalPan = pan.isEmpty() ? null : pan;

        setLoading(true);

        executorService.execute(() -> {
            try {
                Database db = Database.getInstance(getApplicationContext());
                Buyer buyer = new Buyer();
                buyer.buyerName = name;
                buyer.contactPerson = contactPerson;
                buyer.phone = phone;
                buyer.email = finalEmail;
                buyer.address = address;
                buyer.city = city;
                buyer.stateCode = state;
                buyer.postalCode = postalCode;
                buyer.country = country;
                buyer.gst = finalGst;
                buyer.pan = finalPan;
                buyer.notes = notes;
                buyer.isActive = isActive;
                buyer.updatedAt = System.currentTimeMillis();

                if (buyerId > 0) {
                    buyer.buyerId = buyerId;
                    buyer.createdAt = createdAt > 0 ? createdAt : System.currentTimeMillis();
                    db.buyerDao().update(buyer);

                    AuditTrailHelper.logEdit(
                            getApplicationContext(),
                            AuditTrail.MODULE_BUYER,
                            name,
                            "Updated buyer: " + name
                    );

                    mainHandler.post(() -> {
                        setLoading(false);
                        showToast("Buyer \"" + name + "\" updated successfully");
                        finish();
                    });
                } else {
                    buyer.createdAt = System.currentTimeMillis();
                    long id = db.buyerDao().insert(buyer);

                    if (id > 0) {
                        AuditTrailHelper.logAddition(
                                getApplicationContext(),
                                AuditTrail.MODULE_BUYER,
                                name,
                                "Added buyer: " + name
                        );
                    }

                    mainHandler.post(() -> {
                        setLoading(false);
                        if (id > 0) {
                            showToast("Buyer \"" + name + "\" added successfully");
                            finish();
                        } else {
                            showError("Failed to add buyer. Please verify the details.");
                        }
                    });
                }
            } catch (SQLiteConstraintException e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    showError("A buyer with this phone number, email, or GST already exists.");
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    showError("Failed to save buyer: " + e.getMessage());
                });
            }
        });
    }

    private void setLoading(boolean loading) {
        btnSaveBuyer.setEnabled(!loading);
        containerSaveIdle.setVisibility(loading ? View.GONE : View.VISIBLE);
        progressSaving.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        textError.setText(message);
        bannerError.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        bannerError.setVisibility(View.GONE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}
