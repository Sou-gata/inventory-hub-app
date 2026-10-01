package in.gbtsolutions.inventoryhub.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import in.gbtsolutions.inventoryhub.Configurations;
import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.activities.BillViewerActivity;
import in.gbtsolutions.inventoryhub.activities.ScannerActivity;
import in.gbtsolutions.inventoryhub.adapters.CartItemAdapter;
import in.gbtsolutions.inventoryhub.adapters.SearchableDropdownAdapter;
import in.gbtsolutions.inventoryhub.helpers.BitmapHelper;
import in.gbtsolutions.inventoryhub.helpers.CommonFunctions;
import in.gbtsolutions.inventoryhub.helpers.HelperMethods;
import in.gbtsolutions.inventoryhub.helpers.IndianStates;
import in.gbtsolutions.inventoryhub.helpers.ProductQRHelper;
import in.gbtsolutions.inventoryhub.helpers.QRCodeHelper;
import in.gbtsolutions.inventoryhub.helpers.PrinterHelper;
import in.gbtsolutions.inventoryhub.helpers.SellCartManager;
import in.gbtsolutions.inventoryhub.models.Buyer;
import in.gbtsolutions.inventoryhub.models.CartItem;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleItem;
import in.gbtsolutions.inventoryhub.models.SaleItemWithProduct;
import in.gbtsolutions.inventoryhub.repository.BuyerRepository;
import in.gbtsolutions.inventoryhub.repository.ConfigRepository;
import in.gbtsolutions.inventoryhub.repository.SaleRepository;
import in.gbtsolutions.inventoryhub.views.BatchSelectDialog;

public class SellDetailsFragment extends Fragment {

    private final Map<String, String> cachedCompanyConfigs = new HashMap<>();
    private final List<Buyer> buyerList = new ArrayList<>();
    private final ActivityResultLauncher<Intent> qrCodeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        String scanResult = data.getStringExtra(ScannerActivity.SCAN_RESULT);
                        Product product = ProductQRHelper.parseScannedQR(scanResult);
                        if (product != null) {
                            boolean isBatchEnabled = product.batchEnabled;
                            ProductBatch batch = product.getBatch();

                            if (isBatchEnabled) {
                                if (batch != null) {
                                    if (batch.quantity <= 0) {
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), "Insufficient stock", Toast.LENGTH_SHORT).show();
                                        }
                                        return;
                                    }
                                    CartItem existing = SellCartManager.getInstance().getCartItem(product.productId, batch.batchId);
                                    int currentInCart = (existing != null) ? existing.quantity : 0;
                                    if (!GlobalStore.getInstance().isAllowOutOfStockSell() && currentInCart + 1 > batch.quantity) {
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), "Insufficient stock", Toast.LENGTH_SHORT).show();
                                        }
                                        return;
                                    }

                                    if (product.sellingPrice > 0) {
                                        batch.sellingPrice = product.sellingPrice;
                                    }
                                    SellCartManager.getInstance().addBatchItem(product, batch, 1);
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), product.productName + " added to cart", Toast.LENGTH_SHORT).show();
                                    }
                                } else {
                                    // Batch is enabled from settings and no batch is present in QR -> Open batch selection dialog
                                    BatchSelectDialog dialog = BatchSelectDialog.newInstance(product);
                                    dialog.setOnBatchesSelectedListener((prod, selections) -> {
                                        if (selections != null) {
                                            for (BatchSelectDialog.BatchSelection sel : selections) {
                                                if (sel != null && sel.batch != null) {
                                                    SellCartManager.getInstance().addBatchItem(prod, sel.batch, sel.quantity);
                                                }
                                            }
                                        }
                                        if (selections != null && !selections.isEmpty() && getContext() != null) {
                                            String name = (prod != null && prod.productName != null && !prod.productName.trim().isEmpty())
                                                    ? prod.productName.trim()
                                                    : "Product";
                                            Toast.makeText(getContext(), name + " added to cart", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                    dialog.show(getParentFragmentManager(), "BatchSelectDialog");
                                }
                            } else {
                                // Batch is disabled from settings (even if batch info is present in QR) -> No dialog, add directly
                                CartItem existing = SellCartManager.getInstance().getCartItem(product.productId, null);
                                int currentInCart = (existing != null) ? existing.quantity : 0;
                                if (!GlobalStore.getInstance().isAllowOutOfStockSell() && (product.quantity <= 0 || currentInCart + 1 > product.quantity)) {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "Insufficient stock", Toast.LENGTH_SHORT).show();
                                    }
                                    return;
                                }

                                SellCartManager.getInstance().addProduct(product);
                                if (getContext() != null) {
                                    Toast.makeText(getContext(), product.productName + " added to cart", Toast.LENGTH_SHORT).show();
                                }
                            }
                        } else {
                            if (getContext() != null) {
                                Toast.makeText(getContext(), "Product not found or invalid QR code", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                } else if (result.getResultCode() == Activity.RESULT_CANCELED) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Scan cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
    // UI elements
    private RecyclerView recyclerView;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutSummaryContainer;
    private CartItemAdapter adapter;
    // Customer & Invoice views
    private TextView tabCustomerWalkin;
    private TextView tabCustomerRegistered;
    private LinearLayout layoutContainerWalkin;
    private LinearLayout layoutContainerRegistered;
    private EditText inputWalkinName;
    private EditText inputWalkinPhone;
    private EditText inputWalkinGstin;
    private in.gbtsolutions.inventoryhub.views.InstantAutoCompleteTextView dropdownWalkinPos;
    private boolean isWalkInCustomer = true;
    private in.gbtsolutions.inventoryhub.helpers.IndianStates.StateItem selectedPosState = null;
    private LinearLayout containerCustomerDropdown;
    private AutoCompleteTextView dropdownCustomer;
    private ImageView btnClearCustomer;
    private ImageView iconCustomerDropdown;
    private EditText inputInvoiceNumber;
    private ImageView ivClearInvoice;
    private LinearLayout containerDatePicker;
    private EditText inputSaleDate;
    // Price summary views
    private TextView textCartSubtotal;
    private TextView textTotalDiscount;
    private TextView textTotalGst;
    private LinearLayout rowCgst;
    private TextView textCgstAmount;
    private LinearLayout rowSgst;
    private TextView textSgstAmount;
    private LinearLayout rowIgst;
    private TextView textIgstAmount;
    private EditText inputOtherCharges;
    private TextView textGrandTotal;
    private Button btnSell;
    private FloatingActionButton fabAddByQR;
    // State
    private long selectedDateMillis = System.currentTimeMillis();
    private Buyer selectedBuyer = null;
    private ConfigRepository configRepository;
    private String userGst = null;
    private final SellCartManager.OnCartChangedListener cartListener = this::updateCartDisplay;
    private String paymentMethod = "Cash";
    private SearchableDropdownAdapter<Buyer> buyerAdapter;

    public static SellDetailsFragment newInstance() {
        return new SellDetailsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sell_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupCustomerTypeTabs();
        setupWalkinFields();
        setupCustomerDropdown();
        setupDatePicker();
        setupCalculationListeners();
        setupConfigObserver();
        updateCartDisplay();
        handleSell();

        fabAddByQR.setOnClickListener(v -> handleAddByQR());
        ivClearInvoice.setOnClickListener(v -> inputInvoiceNumber.setText(""));
    }

    @Override
    public void onStart() {
        super.onStart();
        SellCartManager.getInstance().addListener(cartListener);
        updateCartDisplay();
    }

    @Override
    public void onStop() {
        super.onStop();
        SellCartManager.getInstance().removeListener(cartListener);
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_cart_items);
        layoutEmptyState = view.findViewById(R.id.layout_empty_sell_details);
        layoutSummaryContainer = view.findViewById(R.id.layout_sell_summary_container);
        fabAddByQR = view.findViewById(R.id.fab_add_by_qr);

        // Customer Type & Walk-in
        tabCustomerWalkin = view.findViewById(R.id.tab_customer_walkin);
        tabCustomerRegistered = view.findViewById(R.id.tab_customer_registered);
        layoutContainerWalkin = view.findViewById(R.id.layout_container_walkin);
        layoutContainerRegistered = view.findViewById(R.id.layout_container_registered);
        inputWalkinName = view.findViewById(R.id.input_walkin_name);
        inputWalkinPhone = view.findViewById(R.id.input_walkin_phone);
        inputWalkinGstin = view.findViewById(R.id.input_walkin_gstin);
        dropdownWalkinPos = view.findViewById(R.id.dropdown_walkin_pos);

        // Customer & Invoice
        containerCustomerDropdown = view.findViewById(R.id.container_customer_dropdown);
        dropdownCustomer = view.findViewById(R.id.dropdown_customer);
        btnClearCustomer = view.findViewById(R.id.btn_clear_customer);
        iconCustomerDropdown = view.findViewById(R.id.icon_customer_dropdown);
        inputInvoiceNumber = view.findViewById(R.id.input_invoice_number);
        ivClearInvoice = view.findViewById(R.id.iv_clear_invoice);
        containerDatePicker = view.findViewById(R.id.container_date_picker);
        inputSaleDate = view.findViewById(R.id.input_sale_date);

        // Price Summary
        textCartSubtotal = view.findViewById(R.id.text_cart_subtotal);
        textTotalDiscount = view.findViewById(R.id.text_total_discount);
        textTotalGst = view.findViewById(R.id.text_total_gst);
        rowCgst = view.findViewById(R.id.row_cgst);
        textCgstAmount = view.findViewById(R.id.text_cgst_amount);
        rowSgst = view.findViewById(R.id.row_sgst);
        textSgstAmount = view.findViewById(R.id.text_sgst_amount);
        rowIgst = view.findViewById(R.id.row_igst);
        textIgstAmount = view.findViewById(R.id.text_igst_amount);
        inputOtherCharges = view.findViewById(R.id.input_other_charges);
        textGrandTotal = view.findViewById(R.id.text_grand_total);

        btnSell = view.findViewById(R.id.btn_sell);
    }

    private void setupRecyclerView() {
        adapter = new CartItemAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnRemoveCartItemListener(item -> {
            SellCartManager.getInstance().removeCartItem(item);
            if (getContext() != null) {
                String name = (item.product != null && !TextUtils.isEmpty(item.product.productName))
                        ? item.product.productName.trim()
                        : "Product";
                Toast.makeText(getContext(), name + " removed from cart", Toast.LENGTH_SHORT).show();
            }
        });

        adapter.setOnCartItemUpdateListener(this::recalculateSummary);
    }

    private void setupCustomerDropdown() {
        Context context = getContext();
        if (context == null) return;

        buyerAdapter = new SearchableDropdownAdapter<>(context, buyerList, new SearchableDropdownAdapter.FilterCriterion<Buyer>() {
            @Override
            public boolean matches(Buyer b, String query) {
                if (b == null) return false;
                if (b.buyerName != null && b.buyerName.toLowerCase(Locale.getDefault()).contains(query))
                    return true;
                if (b.phone != null && b.phone.toLowerCase(Locale.getDefault()).contains(query))
                    return true;
                if (b.contactPerson != null && b.contactPerson.toLowerCase(Locale.getDefault()).contains(query))
                    return true;
                if (b.city != null && b.city.toLowerCase(Locale.getDefault()).contains(query))
                    return true;
                return b.gst != null && b.gst.toLowerCase(Locale.getDefault()).contains(query);
            }

            @Override
            public String getTitle(Buyer b) {
                return b != null && b.buyerName != null ? b.buyerName.trim() : "Unknown";
            }

            @Override
            public String getSubtitle(Buyer b) {
                if (b == null) return "";
                StringBuilder sb = new StringBuilder();
                if (!TextUtils.isEmpty(b.phone)) {
                    sb.append(b.phone.trim());
                }
                if (!TextUtils.isEmpty(b.city)) {
                    if (sb.length() > 0) sb.append(" • ");
                    sb.append(b.city.trim());
                }
                return sb.toString();
            }
        });

        dropdownCustomer.setAdapter(buyerAdapter);
        dropdownCustomer.setDropDownBackgroundResource(R.drawable.bg_card);

        View.OnClickListener showDropdownAction = v -> {
            dropdownCustomer.requestFocus();
            if (buyerList.isEmpty()) {
                dropdownCustomer.setHint("No customers found");
            }
            dropdownCustomer.showDropDown();
        };

        dropdownCustomer.setOnClickListener(showDropdownAction);
        if (containerCustomerDropdown != null) {
            containerCustomerDropdown.setOnClickListener(showDropdownAction);
        }
        if (iconCustomerDropdown != null) {
            iconCustomerDropdown.setOnClickListener(v -> {
                if (dropdownCustomer.isPopupShowing()) {
                    dropdownCustomer.dismissDropDown();
                } else {
                    showDropdownAction.onClick(v);
                }
            });
        }

        if (btnClearCustomer != null) {
            btnClearCustomer.setOnClickListener(v -> {
                dropdownCustomer.setText("");
                selectedBuyer = null;
                recalculateSummary();
                dropdownCustomer.requestFocus();
                dropdownCustomer.showDropDown();
            });
        }

        dropdownCustomer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (btnClearCustomer != null) {
                    btnClearCustomer.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                }
                if (selectedBuyer != null && (s == null || !TextUtils.equals(s.toString().trim(), selectedBuyer.buyerName))) {
                    selectedBuyer = null;
                    recalculateSummary();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        dropdownCustomer.setOnItemClickListener((parent, view, position, id) -> {
            Buyer buyer = buyerAdapter.getItem(position);
            if (buyer != null) {
                selectedBuyer = buyer;
                dropdownCustomer.setText(buyer.buyerName, false);
                dropdownCustomer.setSelection(dropdownCustomer.getText().length());
                dropdownCustomer.clearFocus();
                recalculateSummary();
            }
        });

        // Load active buyers from repository
        if (getActivity() != null) {
            BuyerRepository buyerRepository = new BuyerRepository(getActivity().getApplication());
            buyerRepository.getActiveBuyers().observe(getViewLifecycleOwner(), buyers -> {
                buyerList.clear();
                if (buyers != null) {
                    buyerList.addAll(buyers);
                }
                buyerAdapter.updateData(buyerList);
            });
        }
    }

    private void setupDatePicker() {
        // Pre-fill with today's date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        inputSaleDate.setText(sdf.format(new Date(selectedDateMillis)));

        View.OnClickListener openDatePicker = v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Sale Date")
                    .setSelection(selectedDateMillis)
                    .build();

            datePicker.addOnPositiveButtonClickListener(selection -> {
                if (selection != null) {
                    selectedDateMillis = selection;
                    Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    utcCalendar.setTimeInMillis(selection);
                    SimpleDateFormat pickerFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                    pickerFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                    inputSaleDate.setText(pickerFormat.format(utcCalendar.getTime()));
                }
            });

            if (isAdded()) {
                datePicker.show(getParentFragmentManager(), "SALE_DATE_PICKER");
            }
        };

        inputSaleDate.setOnClickListener(openDatePicker);
        if (containerDatePicker != null) {
            containerDatePicker.setOnClickListener(openDatePicker);
        }
    }

    private void setupCalculationListeners() {
        TextWatcher recalculateWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                recalculateSummary();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        };

        inputOtherCharges.addTextChangedListener(recalculateWatcher);
    }

    private void updateCartDisplay() {
        List<CartItem> items = SellCartManager.getInstance().getCartItems();
        if (items.isEmpty()) {
            if (recyclerView != null) recyclerView.setVisibility(View.GONE);
            if (layoutSummaryContainer != null) layoutSummaryContainer.setVisibility(View.GONE);
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.VISIBLE);
        } else {
            if (layoutEmptyState != null) layoutEmptyState.setVisibility(View.GONE);
            if (recyclerView != null) {
                recyclerView.setVisibility(View.VISIBLE);
                adapter.setCartItems(items);
            }
            if (layoutSummaryContainer != null) {
                layoutSummaryContainer.setVisibility(View.VISIBLE);
            }
            recalculateSummary();
        }
    }

    private void recalculateSummary() {
        double grossSubtotal = SellCartManager.getInstance().getGrossSubtotal();
        double totalDiscount = SellCartManager.getInstance().getTotalDiscount();
        double taxableSubtotal = SellCartManager.getInstance().getTaxableSubtotal();
        double totalGst = SellCartManager.getInstance().getTotalGst();

        String buyerGst = getBuyerGst();
        boolean isSameState = isSameState();

        double cgst = 0.0;
        double sgst = 0.0;
        double igst = 0.0;

        if (isSameState) {
            cgst = totalGst / 2.0;
            sgst = totalGst / 2.0;
            if (rowCgst != null) rowCgst.setVisibility(View.VISIBLE);
            if (rowSgst != null) rowSgst.setVisibility(View.VISIBLE);
            if (rowIgst != null) rowIgst.setVisibility(View.GONE);
        } else {
            igst = totalGst;
            if (rowCgst != null) rowCgst.setVisibility(View.GONE);
            if (rowSgst != null) rowSgst.setVisibility(View.GONE);
            if (rowIgst != null) rowIgst.setVisibility(View.VISIBLE);
        }

        // Parse other charges
        double otherCharges = parseDoubleSafe(inputOtherCharges.getText().toString().trim());

        // Calculate grand total: Taxable Subtotal + Total GST + Other Charges
        double grandTotal = calculateGrandTotal();

        // Update UI
        if (textCartSubtotal != null) {
            textCartSubtotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", grossSubtotal));
        }
        if (textTotalDiscount != null) {
            textTotalDiscount.setText(String.format(Locale.getDefault(), "-₹ %,.2f", totalDiscount));
        }
        if (textTotalGst != null) {
            textTotalGst.setText(String.format(Locale.getDefault(), "₹ %,.2f", totalGst));
        }
        if (textCgstAmount != null) {
            textCgstAmount.setText(String.format(Locale.getDefault(), "₹ %,.2f", cgst));
        }
        if (textSgstAmount != null) {
            textSgstAmount.setText(String.format(Locale.getDefault(), "₹ %,.2f", sgst));
        }
        if (textIgstAmount != null) {
            textIgstAmount.setText(String.format(Locale.getDefault(), "₹ %,.2f", igst));
        }
        if (textGrandTotal != null) {
            textGrandTotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", grandTotal));
        }
    }

    private void setupConfigObserver() {
        if (getActivity() != null) {
            configRepository = new ConfigRepository(getActivity().getApplication());
            configRepository.getAllConfigs().observe(getViewLifecycleOwner(), configs -> {
                if (configs != null) {
                    cachedCompanyConfigs.clear();
                    for (Config c : configs) {
                        cachedCompanyConfigs.put(c.getConfigKey(), c.getConfigValue());
                        GlobalStore.getInstance().setSetting(c.getConfigKey(), c.getConfigValue());
                    }
                    userGst = cachedCompanyConfigs.get("gst_number");
                    setDefaultHomeState();
                    recalculateSummary();
                }
            });
        }
    }

    private void setupCustomerTypeTabs() {
        if (tabCustomerWalkin != null && tabCustomerRegistered != null) {
            tabCustomerWalkin.setOnClickListener(v -> setCustomerMode(true));
            tabCustomerRegistered.setOnClickListener(v -> setCustomerMode(false));
            setCustomerMode(true);
        }
    }

    private void setCustomerMode(boolean walkin) {
        isWalkInCustomer = walkin;
        if (getContext() == null) return;
        if (walkin) {
            tabCustomerWalkin.setBackgroundResource(R.drawable.bg_card);
            tabCustomerWalkin.setTextColor(ContextCompat.getColor(requireContext(), R.color.material_blue));
            tabCustomerWalkin.setTypeface(null, android.graphics.Typeface.BOLD);

            tabCustomerRegistered.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tabCustomerRegistered.setTextColor(ContextCompat.getColor(requireContext(), R.color.fg_muted));
            tabCustomerRegistered.setTypeface(null, android.graphics.Typeface.NORMAL);

            if (layoutContainerWalkin != null) layoutContainerWalkin.setVisibility(View.VISIBLE);
            if (layoutContainerRegistered != null)
                layoutContainerRegistered.setVisibility(View.GONE);
        } else {
            tabCustomerRegistered.setBackgroundResource(R.drawable.bg_card);
            tabCustomerRegistered.setTextColor(ContextCompat.getColor(requireContext(), R.color.material_blue));
            tabCustomerRegistered.setTypeface(null, android.graphics.Typeface.BOLD);

            tabCustomerWalkin.setBackgroundColor(android.graphics.Color.TRANSPARENT);
            tabCustomerWalkin.setTextColor(ContextCompat.getColor(requireContext(), R.color.fg_muted));
            tabCustomerWalkin.setTypeface(null, android.graphics.Typeface.NORMAL);

            if (layoutContainerRegistered != null)
                layoutContainerRegistered.setVisibility(View.VISIBLE);
            if (layoutContainerWalkin != null) layoutContainerWalkin.setVisibility(View.GONE);
        }
        recalculateSummary();
    }

    private void setupWalkinFields() {
        if (dropdownWalkinPos == null || getContext() == null) return;
        List<in.gbtsolutions.inventoryhub.helpers.IndianStates.StateItem> states =
                in.gbtsolutions.inventoryhub.helpers.IndianStates.getAllStates();
        ArrayAdapter<in.gbtsolutions.inventoryhub.helpers.IndianStates.StateItem> stateAdapter =
                new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, states);
        dropdownWalkinPos.setAdapter(stateAdapter);
        dropdownWalkinPos.setDropDownBackgroundResource(R.drawable.bg_card);

        dropdownWalkinPos.setOnItemClickListener((parent, view, position, id) -> {
            selectedPosState = states.get(position);
            dropdownWalkinPos.setText(selectedPosState.getDisplayName(), false);
            recalculateSummary();
        });

        dropdownWalkinPos.setOnClickListener(v -> dropdownWalkinPos.showDropDown());

        setDefaultHomeState();

        if (inputWalkinGstin != null) {
            inputWalkinGstin.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s != null && s.length() >= 2) {
                        String code = in.gbtsolutions.inventoryhub.helpers.IndianStates.getStateCodeFromGstin(s.toString().trim());
                        if (code != null) {
                            in.gbtsolutions.inventoryhub.helpers.IndianStates.StateItem item =
                                    in.gbtsolutions.inventoryhub.helpers.IndianStates.findByCode(code);
                            if (item != null && (selectedPosState == null || !selectedPosState.code.equals(code))) {
                                selectedPosState = item;
                                dropdownWalkinPos.setText(item.getDisplayName(), false);
                            }
                        }
                    }
                    recalculateSummary();
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });
        }
    }

    private void setDefaultHomeState() {
        if (selectedPosState != null) return;
        String stateCode = "19"; // Default West Bengal
        if (!TextUtils.isEmpty(userGst) && userGst.length() >= 2) {
            String derived = userGst.substring(0, 2);
            if (in.gbtsolutions.inventoryhub.helpers.IndianStates.findByCode(derived) != null) {
                stateCode = derived;
            }
        }
        selectedPosState = in.gbtsolutions.inventoryhub.helpers.IndianStates.findByCode(stateCode);
        if (selectedPosState != null && dropdownWalkinPos != null) {
            dropdownWalkinPos.setText(selectedPosState.getDisplayName(), false);
        }
    }

    public String getBuyerGst() {
        if (isWalkInCustomer) {
            return (inputWalkinGstin != null) ? inputWalkinGstin.getText().toString().trim() : null;
        }
        return (selectedBuyer != null) ? selectedBuyer.gst : null;
    }

    public boolean isSameState() {
        if (isWalkInCustomer) {
            String gstin = (inputWalkinGstin != null) ? inputWalkinGstin.getText().toString().trim() : "";
            if (!TextUtils.isEmpty(gstin) && gstin.length() >= 2) {
                return HelperMethods.isSameState(userGst, gstin);
            }
            if (selectedPosState != null && !TextUtils.isEmpty(userGst)) {
                return HelperMethods.isSameStateByCode(userGst, selectedPosState.code);
            }
            return true;
        } else {
            String buyerGst = getBuyerGst();
            if (!TextUtils.isEmpty(userGst) && !TextUtils.isEmpty(buyerGst)) {
                return HelperMethods.isSameState(userGst, buyerGst);
            }
            if (selectedBuyer != null && !TextUtils.isEmpty(selectedBuyer.stateCode) && !TextUtils.isEmpty(userGst)) {
                return HelperMethods.isSameStateByCode(userGst, selectedBuyer.stateCode);
            }
            return true;
        }
    }

    private void handleSell() {
        btnSell.setOnClickListener(v -> {
            List<CartItem> items = SellCartManager.getInstance().getCartItems();
            if (items.isEmpty()) {
                Toast.makeText(getContext(), "Cart is empty. Please add items to cart first.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isWalkInCustomer) {
                String gstin = (inputWalkinGstin != null) ? inputWalkinGstin.getText().toString().trim() : "";
                if (!TextUtils.isEmpty(gstin)) {
                    if (!HelperMethods.validateGST(gstin)) {
                        Toast.makeText(getContext(), "Please enter a valid 15-character GSTIN or leave blank for B2C.", Toast.LENGTH_LONG).show();
                        inputWalkinGstin.requestFocus();
                        return;
                    }
                }
            } else {
                if (selectedBuyer == null) {
                    Toast.makeText(getContext(), "Please select a registered customer for this sale.", Toast.LENGTH_SHORT).show();
                    dropdownCustomer.requestFocus();
                    dropdownCustomer.showDropDown();
                    return;
                }
            }

            // Check stock availability if out-of-stock selling is not permitted
            boolean allowOutOfStock = GlobalStore.getInstance().isAllowOutOfStockSell();

            if (!allowOutOfStock) {
                for (CartItem item : items) {
                    if (item.batchId != null) {
                        if (item.quantity > item.batchAvailableQty) {
                            Toast.makeText(getContext(),
                                    "Insufficient stock for batch " + (item.batchNo != null ? item.batchNo : "") +
                                            " (Available: " + item.batchAvailableQty + ", Requested: " + item.quantity + ")",
                                    Toast.LENGTH_LONG).show();
                            return;
                        }
                    } else if (item.product != null && item.quantity > item.product.quantity) {
                        Toast.makeText(getContext(),
                                "Insufficient stock for " + item.product.productName +
                                        " (Available: " + item.product.quantity + ", Requested: " + item.quantity + ")",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                }
            }
            if (GlobalStore.getInstance().isShowPaymentMethodDialog()) {
                showPaymentMethodDialog();
            } else {
                processSale();
            }
        });
    }

    private void showPaymentMethodDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_payment_method, null);
        RadioGroup rgPayment = view.findViewById(R.id.rg_payment);
        RadioButton radioCash = view.findViewById(R.id.radio_cash);
        radioCash.setChecked(true);

        MaterialAlertDialogBuilder builder =
                new MaterialAlertDialogBuilder(getContext())
                        .setTitle("Select Payment Method")
                        .setView(view)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Proceed", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            Button proceedButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button cancleButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            proceedButton.setOnClickListener(v -> {
                int selectedId = rgPayment.getCheckedRadioButtonId();
                if (selectedId == R.id.radio_cash) {
                    paymentMethod = "Cash";
                    dialog.dismiss();
                    processSale();
                } else if (selectedId == R.id.radio_upi) {
                    paymentMethod = "UPI";
                    dialog.dismiss();
                    showUPIQrDialog();
                } else {
                    Toast.makeText(getContext(), "Please select a payment method", Toast.LENGTH_SHORT).show();
                }
            });
            cancleButton.setOnClickListener(v -> dialog.dismiss());
        });
        dialog.show();
    }

    private double calculateGrandTotal() {
        double taxableSubtotal = SellCartManager.getInstance().getTaxableSubtotal();
        double totalGst = SellCartManager.getInstance().getTotalGst();
        double otherCharges = inputOtherCharges != null ? parseDoubleSafe(inputOtherCharges.getText().toString().trim()) : 0.0;
        return Math.max(0.0, taxableSubtotal + totalGst + otherCharges);
    }

    private void showUPIQrDialog() {
        if (getContext() == null) return;
        View view = getLayoutInflater().inflate(R.layout.dialog_upi_qr, null);
        TextView tvPayee = view.findViewById(R.id.tv_payee);
        TextView tvAmount = view.findViewById(R.id.tv_amount);
        TextView tvUPIId = view.findViewById(R.id.tv_upi_id);
        ImageView ivUpiQr = view.findViewById(R.id.iv_upi_qr);

        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
        MaterialButton btnPaid = view.findViewById(R.id.btn_paid);

        String upiId = cachedCompanyConfigs.get(Configurations.KEY_UPI_ID);
        if (TextUtils.isEmpty(upiId)) {
            upiId = GlobalStore.getInstance().getUpiId();
        }

        String payeeName = cachedCompanyConfigs.get("company_name");
        if (TextUtils.isEmpty(payeeName)) {
            payeeName = GlobalStore.getInstance().getStringSetting("company_name", "M/S. Lokenath Traders");
        }
        double grandTotal = calculateGrandTotal();

        tvPayee.setText(payeeName);
        tvUPIId.setText(upiId);
        if (tvAmount != null) {
            tvAmount.setText(String.format(Locale.getDefault(), "₹ %,.2f", grandTotal));
        }

        String upiAmount = String.format(Locale.US, "%.2f", grandTotal);
        Bitmap qrBitmap = QRCodeHelper.createUPIQRCode(upiId, payeeName, upiAmount, "Payment to " + payeeName, 500);
        if (qrBitmap != null) {
            ivUpiQr.setImageBitmap(qrBitmap);
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getContext()).setView(view);
        AlertDialog dialog = builder.create();
        btnCancel.setOnClickListener(v -> {
            dialog.dismiss();
        });
        btnPaid.setOnClickListener(v -> {
            dialog.dismiss();
            processSale();
        });

        dialog.show();
    }

    private void processSale() {
        List<CartItem> items = SellCartManager.getInstance().getCartItems();
        if (items.isEmpty()) return;
        if (!isWalkInCustomer && selectedBuyer == null) return;

        String invoiceId = inputInvoiceNumber.getText().toString().trim();
        if (TextUtils.isEmpty(invoiceId)) {
            invoiceId = HelperMethods.generateInvoiceNo();
        }

        String billingDate;
        try {
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            Date parsedDate = displayFormat.parse(inputSaleDate.getText().toString().trim());
            if (parsedDate != null) {
                billingDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate);
            } else {
                billingDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(selectedDateMillis));
            }
        } catch (Exception e) {
            billingDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(selectedDateMillis));
        }
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        long createdBy = 1;
        if (GlobalStore.getInstance().getLoggedInUser() != null) {
            createdBy = GlobalStore.getInstance().getLoggedInUser().id;
        }

        double grossSubtotal = SellCartManager.getInstance().getGrossSubtotal();
        double totalDiscount = SellCartManager.getInstance().getTotalDiscount();
        double discountPercent = grossSubtotal > 0 ? (totalDiscount / grossSubtotal) * 100.0 : 0.0;
        double taxableSubtotal = SellCartManager.getInstance().getTaxableSubtotal();
        double totalGst = SellCartManager.getInstance().getTotalGst();
        double otherCharges = parseDoubleSafe(inputOtherCharges.getText().toString().trim());
        double grandTotal = calculateGrandTotal();

        boolean sameState = isSameState();
        double cgst = sameState ? totalGst / 2.0 : 0.0;
        double sgst = sameState ? totalGst / 2.0 : 0.0;
        double igst = sameState ? 0.0 : totalGst;

        int buyerIdToStore = (!isWalkInCustomer && selectedBuyer != null) ? selectedBuyer.buyerId : 0;

        Sale sale = new Sale(
                buyerIdToStore,
                invoiceId,
                grossSubtotal,
                cgst,
                sgst,
                igst,
                totalGst,
                otherCharges,
                grandTotal,
                "Completed",
                createdBy,
                billingDate,
                now,
                now,
                totalDiscount,
                discountPercent
        );

        if (GlobalStore.getInstance().isShowPaymentMethodDialog() && !TextUtils.isEmpty(paymentMethod)) {
            sale.paymentMethod = paymentMethod;
        } else {
            sale.paymentMethod = "Cash";
        }

        if (isWalkInCustomer) {
            String name = (inputWalkinName != null) ? inputWalkinName.getText().toString().trim() : "";
            if (name.isEmpty()) name = "Walk-in Customer";
            String phone = (inputWalkinPhone != null) ? inputWalkinPhone.getText().toString().trim() : "";
            String gstin = (inputWalkinGstin != null) ? inputWalkinGstin.getText().toString().trim() : "";
            String posCode = (selectedPosState != null) ? selectedPosState.code : Configurations.DEFAULT_STATE;
            String posName = (selectedPosState != null) ? selectedPosState.name : IndianStates.getStateName(Configurations.DEFAULT_STATE);

            if (!gstin.isEmpty()) {
                if (!HelperMethods.validateGST(gstin)) {
                    Toast.makeText(getContext(), "Invalid GST No.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            sale.buyerId = 0;
            sale.isWalkIn = true;
            sale.customerName = name;
            sale.customerPhone = !phone.isEmpty() ? phone : null;
            sale.customerGstin = TextUtils.isEmpty(gstin) ? null : gstin.toUpperCase(Locale.US);
            sale.placeOfSupply = posName;
            sale.placeOfSupplyStateCode = posCode;
        } else {
            sale.buyerId = selectedBuyer.buyerId;
            sale.isWalkIn = false;
            sale.customerName = selectedBuyer.buyerName;
            sale.customerPhone = !TextUtils.isEmpty(selectedBuyer.phone) ? selectedBuyer.phone.trim() : null;
            sale.customerGstin = !TextUtils.isEmpty(selectedBuyer.gst) ? selectedBuyer.gst.trim().toUpperCase(Locale.US) : null;
            String bCode = HelperMethods.extractStateCode(!TextUtils.isEmpty(selectedBuyer.gst) ? selectedBuyer.gst : selectedBuyer.stateCode);
            if (bCode == null) bCode = Configurations.DEFAULT_STATE;
            sale.placeOfSupplyStateCode = bCode;
            sale.placeOfSupply = IndianStates.getStateName(bCode);
        }

        List<SaleItem> saleItems = new ArrayList<>();
        List<SaleItemWithProduct> billSaleItems = new ArrayList<>();
        double accumulatedCess = 0.0;

        for (CartItem item : items) {
            if (item.product == null) continue;
            double itemGstRate = item.product.gstPercent;
            double itemTaxable = item.getTaxableAmount();
            double itemGstAmt = item.getGstAmount();

            double itemCgstRate = sameState ? itemGstRate / 2.0 : 0.0;
            double itemSgstRate = sameState ? itemGstRate / 2.0 : 0.0;
            double itemIgstRate = sameState ? 0.0 : itemGstRate;

            double itemCgstAmount = sameState ? itemGstAmt / 2.0 : 0.0;
            double itemSgstAmount = sameState ? itemGstAmt / 2.0 : 0.0;
            double itemIgstAmount = sameState ? 0.0 : itemGstAmt;

            SaleItem saleItem = new SaleItem(
                    0,
                    item.product.productId,
                    item.quantity,
                    item.sellingPrice,
                    itemCgstRate,
                    itemSgstRate,
                    itemIgstRate,
                    itemCgstAmount,
                    itemSgstAmount,
                    itemIgstAmount,
                    itemTaxable,
                    item.getDiscountPercent(),
                    item.getDiscountAmount()
            );
            saleItem.batchId = item.batchId;

            // Snapshot GST and HSN fields for GSTR-1
            saleItem.hsnCode = item.product.hsnCode;
            saleItem.uqc = !TextUtils.isEmpty(item.product.uqc) ? item.product.uqc : "PCS";
            saleItem.gstRate = itemGstRate;
            saleItem.cessRate = item.product.cessPercent;
            saleItem.cessAmount = (itemTaxable * item.product.cessPercent) / 100.0;
            saleItem.taxableValue = itemTaxable;
            accumulatedCess += saleItem.cessAmount;

            saleItems.add(saleItem);

            SaleItemWithProduct itemWithProduct = new SaleItemWithProduct();
            itemWithProduct.saleItem = saleItem;
            itemWithProduct.product = item.product;
            if (item.batchId != null) {
                ProductBatch batch = new ProductBatch();
                batch.batchId = item.batchId;
                batch.batchNo = item.batchNo;
                itemWithProduct.batch = batch;
            }
            billSaleItems.add(itemWithProduct);
        }

        sale.cessAmount = accumulatedCess;

        Buyer buyerForBill;
        if (isWalkInCustomer) {
            buyerForBill = new Buyer();
            buyerForBill.buyerName = sale.customerName;
            buyerForBill.phone = sale.customerPhone != null ? sale.customerPhone : "";
            buyerForBill.gst = sale.customerGstin != null ? sale.customerGstin : "";
            buyerForBill.stateCode = sale.placeOfSupplyStateCode;
            buyerForBill.city = sale.placeOfSupply;
        } else {
            buyerForBill = selectedBuyer;
        }

        final Sale saleForBill = sale;

        btnSell.setEnabled(false);
        btnSell.setText("Processing...");

        if (getActivity() != null) {
            SaleRepository saleRepository = new SaleRepository(getActivity().getApplication());
            final String finalInvoiceId = invoiceId;
            saleRepository.insertSaleWithItems(sale, saleItems, new SaleRepository.SaleInsertCallback() {
                @Override
                public void onSuccess(long saleId) {
                    if (!isAdded()) return;
                    btnSell.setEnabled(true);
                    btnSell.setText("Sell");

                    saleForBill.saleId = (int) saleId;

                    boolean saveToGallery = GlobalStore.getInstance().isSaveBillToGallery();
                    boolean printBill = GlobalStore.getInstance().isPrintBill();

                    if (getContext() != null) {
                        Bitmap billBitmap = CommonFunctions.createSellBillBitmap(getContext(), saleForBill, buyerForBill, billSaleItems, cachedCompanyConfigs);
                        if (billBitmap != null) {
                            String fileName = "Bill_" + finalInvoiceId + "_" + System.currentTimeMillis() + ".png";
                            String title = "Invoice #" + finalInvoiceId;
                            BillViewerActivity.openBill(requireContext(), billBitmap, fileName, title, saveToGallery, printBill);
                        }
                    }

                    Toast.makeText(getContext(), "Sale completed successfully! (Invoice: " + finalInvoiceId + ")", Toast.LENGTH_LONG).show();

                    // Clear cart and UI inputs
                    paymentMethod = "Cash";
                    SellCartManager.getInstance().clear();
                    inputInvoiceNumber.setText("");
                    inputOtherCharges.setText("");
                    if (isWalkInCustomer) {
                        if (inputWalkinName != null) inputWalkinName.setText("");
                        if (inputWalkinPhone != null) inputWalkinPhone.setText("");
                        if (inputWalkinGstin != null) inputWalkinGstin.setText("");
                    } else {
                        selectedBuyer = null;
                        dropdownCustomer.setText("", false);
                    }
                }

                @Override
                public void onError(String message) {
                    if (!isAdded()) return;
                    btnSell.setEnabled(true);
                    btnSell.setText("Sell");
                    Toast.makeText(getContext(), "Failed to record sale: " + message, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void handleAddByQR() {
        Intent intent = new Intent(getContext(), ScannerActivity.class);
        qrCodeLauncher.launch(intent);
    }

    private double parseDoubleSafe(String text) {
        if (TextUtils.isEmpty(text)) {
            return 0.0;
        }
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
