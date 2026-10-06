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
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.materialswitch.MaterialSwitch;

import in.gbtsolutions.inventoryhub.activities.BillViewerActivity;
import in.gbtsolutions.inventoryhub.activities.ScannerActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import in.gbtsolutions.inventoryhub.GlobalStore;
import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.activities.ScannerActivity;
import in.gbtsolutions.inventoryhub.adapters.PurchaseCartItemAdapter;
import in.gbtsolutions.inventoryhub.adapters.SearchableDropdownAdapter;
import in.gbtsolutions.inventoryhub.helpers.BitmapHelper;
import in.gbtsolutions.inventoryhub.helpers.CommonFunctions;
import in.gbtsolutions.inventoryhub.helpers.HelperMethods;
import in.gbtsolutions.inventoryhub.helpers.PrinterHelper;
import in.gbtsolutions.inventoryhub.helpers.ProductQRHelper;
import in.gbtsolutions.inventoryhub.helpers.PurchaseCartManager;
import in.gbtsolutions.inventoryhub.models.CartItem;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseItem;
import in.gbtsolutions.inventoryhub.models.Suppliers;
import in.gbtsolutions.inventoryhub.repository.ConfigRepository;
import in.gbtsolutions.inventoryhub.repository.PurchaseRepository;
import in.gbtsolutions.inventoryhub.repository.SupplierRepository;

public class PurchaseDetailsFragment extends Fragment {

    private final Map<String, String> cachedCompanyConfigs = new HashMap<>();

    // UI elements
    private RecyclerView recyclerView;
    private LinearLayout layoutEmptyState;
    private LinearLayout layoutSummaryContainer;
    private PurchaseCartItemAdapter adapter;

    // Supplier & Invoice views
    private LinearLayout containerCustomerDropdown;
    private AutoCompleteTextView dropdownCustomer;
    private ImageView btnClearSupplier;
    private ImageView iconSupplierDropdown;
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
    private MaterialSwitch switchSaveAsPending;

    // State
    private long selectedDateMillis = System.currentTimeMillis();
    private Suppliers selectedSupplier = null;
    private ConfigRepository configRepository;
    private String userGst = null;

    private final List<Suppliers> supplierList = new ArrayList<>();
    private SearchableDropdownAdapter<Suppliers> supplierAdapter;

    private final ActivityResultLauncher<Intent> qrCodeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        String scanResult = data.getStringExtra(ScannerActivity.SCAN_RESULT);
                        Product product = ProductQRHelper.parseScannedQR(scanResult);
                        if (product != null) {
                            // For purchases, always add directly — no batch dialog, no stock check
                            PurchaseCartManager.getInstance().addProduct(product);
                            if (getContext() != null) {
                                Toast.makeText(getContext(), product.productName + " added to cart", Toast.LENGTH_SHORT).show();
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

    private final PurchaseCartManager.OnCartChangedListener cartListener = this::updateCartDisplay;

    public static PurchaseDetailsFragment newInstance() {
        return new PurchaseDetailsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_purchase_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupSupplierDropdown();
        setupDatePicker();
        setupCalculationListeners();
        setupConfigObserver();
        updateCartDisplay();
        handlePurchase();

        fabAddByQR.setOnClickListener(v -> handleAddByQR());
        ivClearInvoice.setOnClickListener(v -> inputInvoiceNumber.setText(""));
    }

    @Override
    public void onStart() {
        super.onStart();
        PurchaseCartManager.getInstance().addListener(cartListener);
        updateCartDisplay();
    }

    @Override
    public void onStop() {
        super.onStop();
        PurchaseCartManager.getInstance().removeListener(cartListener);
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recycler_cart_items);
        layoutEmptyState = view.findViewById(R.id.layout_empty_sell_details);
        layoutSummaryContainer = view.findViewById(R.id.layout_sell_summary_container);
        fabAddByQR = view.findViewById(R.id.fab_add_by_qr);

        containerCustomerDropdown = view.findViewById(R.id.container_customer_dropdown);
        dropdownCustomer = view.findViewById(R.id.dropdown_customer);
        btnClearSupplier = view.findViewById(R.id.btn_clear_supplier);
        iconSupplierDropdown = view.findViewById(R.id.icon_supplier_dropdown);
        inputInvoiceNumber = view.findViewById(R.id.input_invoice_number);
        ivClearInvoice = view.findViewById(R.id.iv_clear_invoice);
        containerDatePicker = view.findViewById(R.id.container_date_picker);
        inputSaleDate = view.findViewById(R.id.input_sale_date);

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
        switchSaveAsPending = view.findViewById(R.id.switch_save_as_pending);
    }

    private void setupRecyclerView() {
        adapter = new PurchaseCartItemAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        adapter.setOnRemoveCartItemListener(item -> {
            PurchaseCartManager.getInstance().removeCartItem(item);
            if (getContext() != null) {
                String name = (item.product != null && !TextUtils.isEmpty(item.product.productName))
                        ? item.product.productName.trim()
                        : "Product";
                Toast.makeText(getContext(), name + " removed from cart", Toast.LENGTH_SHORT).show();
            }
        });

        adapter.setOnCartItemUpdateListener(this::recalculateSummary);
    }

    private void setupSupplierDropdown() {
        Context context = getContext();
        if (context == null) return;

        supplierAdapter = new SearchableDropdownAdapter<>(context, supplierList, new SearchableDropdownAdapter.FilterCriterion<Suppliers>() {
            @Override
            public boolean matches(Suppliers s, String query) {
                if (s == null) return false;
                if (s.supplierName != null && s.supplierName.toLowerCase(Locale.getDefault()).contains(query)) return true;
                if (s.phone != null && s.phone.toLowerCase(Locale.getDefault()).contains(query)) return true;
                if (s.contactPerson != null && s.contactPerson.toLowerCase(Locale.getDefault()).contains(query)) return true;
                if (s.city != null && s.city.toLowerCase(Locale.getDefault()).contains(query)) return true;
                if (s.gst != null && s.gst.toLowerCase(Locale.getDefault()).contains(query)) return true;
                return false;
            }

            @Override
            public String getTitle(Suppliers s) {
                return s != null && s.supplierName != null ? s.supplierName.trim() : "Unknown";
            }

            @Override
            public String getSubtitle(Suppliers s) {
                if (s == null) return "";
                StringBuilder sb = new StringBuilder();
                if (!TextUtils.isEmpty(s.phone)) {
                    sb.append(s.phone.trim());
                }
                if (!TextUtils.isEmpty(s.city)) {
                    if (sb.length() > 0) sb.append(" • ");
                    sb.append(s.city.trim());
                }
                return sb.toString();
            }
        });

        dropdownCustomer.setAdapter(supplierAdapter);
        dropdownCustomer.setDropDownBackgroundResource(R.drawable.bg_card);

        View.OnClickListener showDropdownAction = v -> {
            dropdownCustomer.requestFocus();
            if (supplierList.isEmpty()) {
                dropdownCustomer.setHint("No suppliers found");
            }
            dropdownCustomer.showDropDown();
        };

        dropdownCustomer.setOnClickListener(showDropdownAction);
        if (containerCustomerDropdown != null) {
            containerCustomerDropdown.setOnClickListener(showDropdownAction);
        }
        if (iconSupplierDropdown != null) {
            iconSupplierDropdown.setOnClickListener(v -> {
                if (dropdownCustomer.isPopupShowing()) {
                    dropdownCustomer.dismissDropDown();
                } else {
                    showDropdownAction.onClick(v);
                }
            });
        }

        if (btnClearSupplier != null) {
            btnClearSupplier.setOnClickListener(v -> {
                dropdownCustomer.setText("");
                selectedSupplier = null;
                recalculateSummary();
                dropdownCustomer.requestFocus();
                dropdownCustomer.showDropDown();
            });
        }

        dropdownCustomer.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (btnClearSupplier != null) {
                    btnClearSupplier.setVisibility(s != null && s.length() > 0 ? View.VISIBLE : View.GONE);
                }
                if (selectedSupplier != null && (s == null || !TextUtils.equals(s.toString().trim(), selectedSupplier.supplierName))) {
                    selectedSupplier = null;
                    recalculateSummary();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        dropdownCustomer.setOnItemClickListener((parent, view, position, id) -> {
            Suppliers supplier = supplierAdapter.getItem(position);
            if (supplier != null) {
                selectedSupplier = supplier;
                dropdownCustomer.setText(supplier.supplierName, false);
                dropdownCustomer.setSelection(dropdownCustomer.getText().length());
                dropdownCustomer.clearFocus();
                recalculateSummary();
            }
        });

        if (getActivity() != null) {
            SupplierRepository supplierRepository = new SupplierRepository(getActivity().getApplication());
            if (supplierRepository.isOnlineMode()) {
                supplierRepository.fetchSuppliersOnline(new in.gbtsolutions.inventoryhub.online.repository.OnlineSupplierRepository.SupplierListCallback() {
                    @Override
                    public void onSuccess(List<Suppliers> suppliers) {
                        if (getActivity() != null && isAdded()) {
                            getActivity().runOnUiThread(() -> {
                                supplierList.clear();
                                if (suppliers != null) {
                                    for (Suppliers s : suppliers) {
                                        if (s.isActive) {
                                            supplierList.add(s);
                                        }
                                    }
                                }
                                supplierAdapter.updateData(supplierList);
                            });
                        }
                    }

                    @Override
                    public void onError(String message) {
                        // Keep any existing data or fallback
                    }
                });
            } else {
                supplierRepository.getActiveSuppliers().observe(getViewLifecycleOwner(), suppliers -> {
                    supplierList.clear();
                    if (suppliers != null) {
                        supplierList.addAll(suppliers);
                    }
                    supplierAdapter.updateData(supplierList);
                });
            }
        }
    }

    private void setupDatePicker() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        inputSaleDate.setText(sdf.format(new Date(selectedDateMillis)));

        View.OnClickListener openDatePicker = v -> {
            MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                    .setTitleText("Select Purchase Date")
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
                datePicker.show(getParentFragmentManager(), "PURCHASE_DATE_PICKER");
            }
        };

        inputSaleDate.setOnClickListener(openDatePicker);
        if (containerDatePicker != null) {
            containerDatePicker.setOnClickListener(openDatePicker);
        }
    }

    private void setupCalculationListeners() {
        TextWatcher recalculateWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { recalculateSummary(); }
            @Override public void afterTextChanged(Editable s) {}
        };
        inputOtherCharges.addTextChangedListener(recalculateWatcher);
    }

    private void updateCartDisplay() {
        List<CartItem> items = PurchaseCartManager.getInstance().getCartItems();
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
            if (layoutSummaryContainer != null) layoutSummaryContainer.setVisibility(View.VISIBLE);
            recalculateSummary();
        }
    }

    private void recalculateSummary() {
        double grossSubtotal = PurchaseCartManager.getInstance().getGrossSubtotal();
        double totalDiscount = PurchaseCartManager.getInstance().getTotalDiscount();
        double taxableSubtotal = PurchaseCartManager.getInstance().getTaxableSubtotal();
        double totalGst = PurchaseCartManager.getInstance().getTotalGst();

        boolean isSameState = isSameState();
        double cgst = 0.0, sgst = 0.0, igst = 0.0;

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

        double otherCharges = parseDoubleSafe(inputOtherCharges.getText().toString().trim());
        double grandTotal = Math.max(0.0, taxableSubtotal + totalGst + otherCharges);

        if (textCartSubtotal != null) textCartSubtotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", grossSubtotal));
        if (textTotalDiscount != null) textTotalDiscount.setText(String.format(Locale.getDefault(), "-₹ %,.2f", totalDiscount));
        if (textTotalGst != null) textTotalGst.setText(String.format(Locale.getDefault(), "₹ %,.2f", totalGst));
        if (textCgstAmount != null) textCgstAmount.setText(String.format(Locale.getDefault(), "₹ %,.2f", cgst));
        if (textSgstAmount != null) textSgstAmount.setText(String.format(Locale.getDefault(), "₹ %,.2f", sgst));
        if (textIgstAmount != null) textIgstAmount.setText(String.format(Locale.getDefault(), "₹ %,.2f", igst));
        if (textGrandTotal != null) textGrandTotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", grandTotal));
    }

    private void setupConfigObserver() {
        if (getActivity() != null) {
            configRepository = new ConfigRepository(getActivity().getApplication());
            configRepository.getAllConfigs().observe(getViewLifecycleOwner(), configs -> {
                if (configs != null) {
                    cachedCompanyConfigs.clear();
                    for (Config c : configs) {
                        cachedCompanyConfigs.put(c.getConfigKey(), c.getConfigValue());
                    }
                    userGst = cachedCompanyConfigs.get("gst_number");
                    recalculateSummary();
                }
            });
        }
    }

    public String getSupplierGst() {
        return (selectedSupplier != null) ? selectedSupplier.gst : null;
    }

    public boolean isSameState() {
        String supplierGst = getSupplierGst();
        if (!TextUtils.isEmpty(userGst) && !TextUtils.isEmpty(supplierGst)) {
            return HelperMethods.isSameState(userGst, supplierGst);
        }
        return true;
    }

    private void handlePurchase() {
        btnSell.setOnClickListener(v -> {
            List<CartItem> items = PurchaseCartManager.getInstance().getCartItems();
            if (items.isEmpty()) {
                Toast.makeText(getContext(), "Cart is empty. Please add items first.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedSupplier == null) {
                Toast.makeText(getContext(), "Please select a supplier for this purchase.", Toast.LENGTH_SHORT).show();
                dropdownCustomer.requestFocus();
                dropdownCustomer.showDropDown();
                return;
            }

            // Build invoice/PO number
            String invoiceId = inputInvoiceNumber.getText().toString().trim();
            if (TextUtils.isEmpty(invoiceId)) {
                // Auto-generate PO number
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault());
                invoiceId = "PO-" + sdf.format(new Date());
            }

            String billingDate;
            try {
                SimpleDateFormat displayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                Date parsedDate = displayFormat.parse(inputSaleDate.getText().toString().trim());
                billingDate = parsedDate != null
                        ? new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(parsedDate)
                        : new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(selectedDateMillis));
            } catch (Exception e) {
                billingDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(selectedDateMillis));
            }

            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

            long createdBy = 1;
            if (GlobalStore.getInstance().getLoggedInUser() != null) {
                createdBy = GlobalStore.getInstance().getLoggedInUser().id;
            }

            double grossSubtotal = PurchaseCartManager.getInstance().getGrossSubtotal();
            double totalDiscount = PurchaseCartManager.getInstance().getTotalDiscount();
            double discountPercent = grossSubtotal > 0 ? (totalDiscount / grossSubtotal) * 100.0 : 0.0;
            double taxableSubtotal = PurchaseCartManager.getInstance().getTaxableSubtotal();
            double totalGst = PurchaseCartManager.getInstance().getTotalGst();
            double otherCharges = parseDoubleSafe(inputOtherCharges.getText().toString().trim());
            double grandTotal = Math.max(0.0, taxableSubtotal + totalGst + otherCharges);

            boolean sameState = isSameState();
            double cgst = sameState ? totalGst / 2.0 : 0.0;
            double sgst = sameState ? totalGst / 2.0 : 0.0;
            double igst = sameState ? 0.0 : totalGst;

            boolean isPending = switchSaveAsPending != null && switchSaveAsPending.isChecked();
            String status = isPending ? "Pending" : "Completed";

            Purchase purchase = new Purchase(
                    selectedSupplier.supplierId,
                    invoiceId,
                    grossSubtotal,
                    cgst,
                    sgst,
                    igst,
                    totalGst,
                    otherCharges,
                    grandTotal,
                    status,
                    createdBy,
                    billingDate,
                    now,
                    now,
                    totalDiscount,
                    discountPercent
            );

            List<PurchaseItem> purchaseItems = new ArrayList<>();
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

                PurchaseItem purchaseItem = new PurchaseItem(
                        0,
                        item.product.productId,
                        item.quantity,
                        item.sellingPrice,    // Cost price stored here
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
                purchaseItems.add(purchaseItem);
            }

            btnSell.setEnabled(false);
            btnSell.setText("Processing...");

            final List<CartItem> cartItemsForBill = new ArrayList<>(items);

            if (getActivity() != null) {
                PurchaseRepository purchaseRepository = new PurchaseRepository(getActivity().getApplication());
                final String finalInvoiceId = invoiceId;
                purchaseRepository.insertPurchaseWithItems(purchase, purchaseItems, new PurchaseRepository.PurchaseInsertCallback() {
                    @Override
                    public void onSuccess(long purchaseId) {
                        if (!isAdded()) return;
                        btnSell.setEnabled(true);
                        btnSell.setText(R.string.btn_confirm_purchase);

                        purchase.purchaseId = (int) purchaseId;

                        boolean saveToGallery = GlobalStore.getInstance().isSaveBillToGallery();
                        boolean printBill = GlobalStore.getInstance().isPrintBill();

                        if (getContext() != null) {
                            Bitmap billBitmap = CommonFunctions.createPurchaseBillBitmap(
                                    getContext(),
                                    purchase,
                                    selectedSupplier,
                                    cartItemsForBill,
                                    cachedCompanyConfigs
                            );
                            if (billBitmap != null) {
                                String fileName = "PO_" + finalInvoiceId + "_" + System.currentTimeMillis() + ".png";
                                String title = "Purchase Order #" + finalInvoiceId;
                                BillViewerActivity.openBill(requireContext(), billBitmap, fileName, title, saveToGallery, printBill);
                            }
                        }

                        String message = isPending
                                ? "Purchase order saved as Pending! (PO: " + finalInvoiceId + ")"
                                : "Purchase recorded! (PO: " + finalInvoiceId + ")";
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                        // Clear cart and reset UI
                        PurchaseCartManager.getInstance().clear();
                        inputInvoiceNumber.setText("");
                        inputOtherCharges.setText("");
                        selectedSupplier = null;
                        dropdownCustomer.setText("", false);
                        if (switchSaveAsPending != null) {
                            switchSaveAsPending.setChecked(false);
                        }
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        btnSell.setEnabled(true);
                        btnSell.setText(R.string.btn_confirm_purchase);
                        Toast.makeText(getContext(), "Failed to record purchase: " + message, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void handleAddByQR() {
        Intent intent = new Intent(getContext(), ScannerActivity.class);
        qrCodeLauncher.launch(intent);
    }

    private double parseDoubleSafe(String text) {
        if (TextUtils.isEmpty(text)) return 0.0;
        try {
            return Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
