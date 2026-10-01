package in.gbtsolutions.inventoryhub.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import java.io.File;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.res.ColorStateList;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;

import java.text.ParseException;
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
import in.gbtsolutions.inventoryhub.adapters.PendingReceiveAdapter;
import in.gbtsolutions.inventoryhub.helpers.BitmapHelper;
import in.gbtsolutions.inventoryhub.helpers.CommonFunctions;
import in.gbtsolutions.inventoryhub.helpers.PrinterHelper;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseItem;
import in.gbtsolutions.inventoryhub.models.PurchaseItemWithProduct;
import in.gbtsolutions.inventoryhub.models.PurchaseWithSupplier;
import in.gbtsolutions.inventoryhub.models.ReceiveItem;
import in.gbtsolutions.inventoryhub.models.ReceiveRecord;
import in.gbtsolutions.inventoryhub.models.ReceiveRecordWithItems;
import in.gbtsolutions.inventoryhub.models.Suppliers;
import in.gbtsolutions.inventoryhub.repository.ConfigRepository;
import in.gbtsolutions.inventoryhub.repository.PurchaseRepository;

public class PendingReceiveActivity extends BaseActivity implements PendingReceiveAdapter.OnPendingPurchaseClickListener {

    private final SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());

    private EditText editSearchPending;
    private ImageView btnClearSearch;
    private TextView textPendingCount;
    private RecyclerView recyclerPendingReceive;
    private LinearLayout layoutEmptyState;

    private PendingReceiveAdapter adapter;
    private PurchaseRepository purchaseRepository;
    private LiveData<List<PurchaseWithSupplier>> currentLiveData;
    private final Observer<List<PurchaseWithSupplier>> pendingPurchasesObserver = this::updatePurchaseList;

    private final Map<String, String> cachedCompanyConfigs = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_receive);

        purchaseRepository = new PurchaseRepository(getApplication());
        ConfigRepository configRepository = new ConfigRepository(getApplication());
        configRepository.getAllConfigs().observe(this, configs -> {
            if (configs != null) {
                cachedCompanyConfigs.clear();
                for (Config cfg : configs) {
                    if (cfg != null && cfg.configKey != null) {
                        cachedCompanyConfigs.put(cfg.configKey, cfg.configValue);
                    }
                }
            }
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        View headerContainer = findViewById(R.id.header_container);
        View contentContainer = findViewById(R.id.pending_receive_content_container);

        applyDrawerInsets(drawerLayout, headerContainer, contentContainer, navView);
        setupDrawerNavigation(drawerLayout, toolbar, navView, R.id.nav_receive);

        initViews();
        setupSearch();
        setupRecyclerView();
        loadPendingPurchases("");
    }

    private void initViews() {
        editSearchPending = findViewById(R.id.edit_search_pending);
        btnClearSearch = findViewById(R.id.btn_clear_search);
        textPendingCount = findViewById(R.id.text_pending_count);
        recyclerPendingReceive = findViewById(R.id.recycler_pending_receive);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
    }

    private void setupSearch() {
        editSearchPending.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                btnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void afterTextChanged(Editable s) {
                loadPendingPurchases(s.toString().trim());
            }
        });

        btnClearSearch.setOnClickListener(v -> {
            editSearchPending.setText("");
            hideKeyboard();
        });

        editSearchPending.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });
    }

    private void setupRecyclerView() {
        adapter = new PendingReceiveAdapter(this);
        recyclerPendingReceive.setLayoutManager(new LinearLayoutManager(this));
        recyclerPendingReceive.setAdapter(adapter);
    }

    private final Map<Integer, in.gbtsolutions.inventoryhub.online.models.OnlinePendingReceiveDto> cachedOnlinePendingMap = new HashMap<>();

    private void loadPendingPurchases(String query) {
        if (currentLiveData != null) {
            currentLiveData.removeObserver(pendingPurchasesObserver);
        }

        if (purchaseRepository != null && purchaseRepository.isOnlineMode()) {
            purchaseRepository.getOnlineRepository().fetchPendingReceives(query, new in.gbtsolutions.inventoryhub.online.repository.OnlinePurchaseRepository.PendingReceivesCallback() {
                @Override
                public void onSuccess(List<in.gbtsolutions.inventoryhub.online.models.OnlinePendingReceiveDto> pendingList) {
                    cachedOnlinePendingMap.clear();
                    List<PurchaseWithSupplier> list = new ArrayList<>();
                    if (pendingList != null) {
                        for (in.gbtsolutions.inventoryhub.online.models.OnlinePendingReceiveDto dto : pendingList) {
                            cachedOnlinePendingMap.put(dto.purchaseId, dto);

                            PurchaseWithSupplier pws = new PurchaseWithSupplier();
                            pws.purchase = new Purchase();
                            pws.purchase.purchaseId = dto.purchaseId;
                            pws.purchase.invoiceId = dto.invoiceId;
                            pws.purchase.billingDate = dto.purchaseDate;
                            pws.purchase.totalAmount = dto.totalAmount;
                            pws.purchase.status = dto.status;

                            pws.supplier = new Suppliers();
                            pws.supplier.supplierName = dto.supplierName != null ? dto.supplierName : "Supplier";
                            list.add(pws);
                        }
                    }
                    updatePurchaseList(list);
                }

                @Override
                public void onError(String errorMessage) {
                    Toast.makeText(PendingReceiveActivity.this, "Cloud: " + errorMessage, Toast.LENGTH_SHORT).show();
                    updatePurchaseList(new ArrayList<>());
                }
            });
            return;
        }

        if (TextUtils.isEmpty(query)) {
            currentLiveData = purchaseRepository.getPendingPurchasesWithSupplier();
        } else {
            currentLiveData = purchaseRepository.getFilteredPendingPurchases(query);
        }
        currentLiveData.observe(this, pendingPurchasesObserver);
    }

    private void updatePurchaseList(List<PurchaseWithSupplier> purchases) {
        if (purchases == null || purchases.isEmpty()) {
            adapter.setPurchaseList(null);
            recyclerPendingReceive.setVisibility(View.GONE);
            layoutEmptyState.setVisibility(View.VISIBLE);
            textPendingCount.setText("0 orders awaiting receive");
        } else {
            adapter.setPurchaseList(purchases);
            recyclerPendingReceive.setVisibility(View.VISIBLE);
            layoutEmptyState.setVisibility(View.GONE);
            int count = purchases.size();
            textPendingCount.setText(String.format(Locale.getDefault(), "%d order%s awaiting receive",
                    count, count == 1 ? "" : "s"));
        }
    }

    @Override
    public void onPurchaseClick(PurchaseWithSupplier item) {
        if (item == null || item.purchase == null) return;
        showReceiveBottomSheet(item);
    }

    private void showReceiveBottomSheet(PurchaseWithSupplier item) {
        Purchase purchase = item.purchase;
        Suppliers supplier = item.supplier;

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_receive_items, null);
        dialog.setContentView(sheetView);

        // Header Views
        TextView textInvoiceId = sheetView.findViewById(R.id.text_detail_invoice_id);
        TextView textDateTime = sheetView.findViewById(R.id.text_detail_date_time);
        View btnClose = sheetView.findViewById(R.id.btn_close_receive);
        TextView textStatus = sheetView.findViewById(R.id.text_detail_status);
        TextView textSaleId = sheetView.findViewById(R.id.text_detail_sale_id);

        // Supplier Views
        TextView textSupplierName = sheetView.findViewById(R.id.text_detail_supplier_name);
        TextView textSupplierPhone = sheetView.findViewById(R.id.text_detail_supplier_phone);
        TextView textSupplierAddress = sheetView.findViewById(R.id.text_detail_supplier_address);
        View btnCallSupplier = sheetView.findViewById(R.id.btn_action_call_supplier);

        // Items Views
        TextView textItemsCount = sheetView.findViewById(R.id.text_detail_items_count);
        TextView btnReceiveAll = sheetView.findViewById(R.id.btn_receive_all_items);
        ProgressBar progressItems = sheetView.findViewById(R.id.progress_detail_items);
        LinearLayout containerItems = sheetView.findViewById(R.id.container_receive_items);

        // Notes & Actions
        EditText editNotes = sheetView.findViewById(R.id.edit_receive_notes);
        Button btnCancel = sheetView.findViewById(R.id.btn_cancel_order);
        Button btnConfirmReceive = sheetView.findViewById(R.id.btn_confirm_receive);

        // Populate Header
        textInvoiceId.setText(!TextUtils.isEmpty(purchase.invoiceId)
                ? purchase.invoiceId
                : String.format(Locale.getDefault(), "#PO-%d", purchase.purchaseId));
        textSaleId.setText(String.format(Locale.getDefault(), "#PO-%d", purchase.purchaseId));

        LinearLayout layoutStatusBadge = sheetView.findViewById(R.id.layout_status_badge);
        View viewStatusDot = sheetView.findViewById(R.id.view_status_dot);

        String status = !TextUtils.isEmpty(purchase.status) ? purchase.status : "Pending";
        textStatus.setText(status);

        int colorText;
        int colorBg;
        if ("Completed".equalsIgnoreCase(status)) {
            colorText = ContextCompat.getColor(this, R.color.status_green);
            colorBg = ContextCompat.getColor(this, R.color.status_green_bg);
        } else if ("Partially Received".equalsIgnoreCase(status) || "Partial Completed".equalsIgnoreCase(status)) {
            colorText = ContextCompat.getColor(this, R.color.status_blue);
            colorBg = ContextCompat.getColor(this, R.color.status_blue_bg);
        } else if ("Pending".equalsIgnoreCase(status)) {
            colorText = ContextCompat.getColor(this, R.color.status_orange);
            colorBg = ContextCompat.getColor(this, R.color.status_orange_bg);
        } else {
            colorText = ContextCompat.getColor(this, R.color.status_red);
            colorBg = ContextCompat.getColor(this, R.color.status_red_bg);
        }

        textStatus.setTextColor(colorText);
        if (viewStatusDot != null) {
            viewStatusDot.setBackgroundTintList(ColorStateList.valueOf(colorText));
        }
        if (layoutStatusBadge != null) {
            layoutStatusBadge.setBackgroundTintList(ColorStateList.valueOf(colorBg));
        }

        String formattedDate = purchase.billingDate;
        if (!TextUtils.isEmpty(purchase.billingDate)) {
            try {
                Date date = isoDateFormat.parse(purchase.billingDate);
                if (date != null) formattedDate = displayDateFormat.format(date);
            } catch (ParseException ignored) {}
        }
        textDateTime.setText(String.format("Ordered on %s", formattedDate != null ? formattedDate : ""));

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // Populate Supplier
        if (supplier != null) {
            textSupplierName.setText(!TextUtils.isEmpty(supplier.supplierName) ? supplier.supplierName : "Supplier");
            if (!TextUtils.isEmpty(supplier.phone)) {
                textSupplierPhone.setVisibility(View.VISIBLE);
                textSupplierPhone.setText(String.format("Phone: %s", supplier.phone));
                btnCallSupplier.setVisibility(View.VISIBLE);
                btnCallSupplier.setOnClickListener(v -> {
                    Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + supplier.phone));
                    startActivity(dialIntent);
                });
            } else {
                textSupplierPhone.setVisibility(View.GONE);
                btnCallSupplier.setVisibility(View.GONE);
            }

            StringBuilder addr = new StringBuilder();
            if (!TextUtils.isEmpty(supplier.address)) addr.append(supplier.address);
            if (!TextUtils.isEmpty(supplier.city)) {
                if (addr.length() > 0) addr.append(", ");
                addr.append(supplier.city);
            }
            if (addr.length() > 0) {
                textSupplierAddress.setVisibility(View.VISIBLE);
                textSupplierAddress.setText(String.format("Address: %s", addr));
            } else {
                textSupplierAddress.setVisibility(View.GONE);
            }
        } else {
            textSupplierName.setText("Unknown Supplier");
            textSupplierPhone.setVisibility(View.GONE);
            textSupplierAddress.setVisibility(View.GONE);
            btnCallSupplier.setVisibility(View.GONE);
        }

        // Map to keep track of EditText views for each purchase_item_id
        final Map<Integer, EditText> itemInputMap = new HashMap<>();
        final Map<Integer, Integer> itemRemainingMap = new HashMap<>();
        final Map<Integer, Product> itemProductMap = new HashMap<>();
        final Map<Integer, EditText> itemBatchNoMap = new HashMap<>();
        final Map<Integer, Long> itemExpiryEpochMap = new HashMap<>();

        // Load items for this purchase
        progressItems.setVisibility(View.VISIBLE);
        containerItems.removeAllViews();

        if (purchaseRepository != null && purchaseRepository.isOnlineMode()) {
            List<PurchaseItemWithProduct> onlineItems = new ArrayList<>();
            in.gbtsolutions.inventoryhub.online.models.OnlinePendingReceiveDto pendingDto =
                    cachedOnlinePendingMap.get(purchase.purchaseId);
            if (pendingDto != null && pendingDto.items != null) {
                for (in.gbtsolutions.inventoryhub.online.models.OnlinePendingReceiveDto.PendingItemDto pDto : pendingDto.items) {
                    PurchaseItemWithProduct piwp = new PurchaseItemWithProduct();
                    piwp.purchaseItem = new PurchaseItem();
                    piwp.purchaseItem.purchaseItemId = pDto.productId;
                    piwp.purchaseItem.purchaseId = purchase.purchaseId;
                    piwp.purchaseItem.productId = pDto.productId;
                    piwp.purchaseItem.quantity = pDto.quantityOrdered;
                    piwp.purchaseItem.receivedQuantity = pDto.quantityReceived;
                    piwp.purchaseItem.unitPrice = pDto.unitPrice;

                    piwp.product = new Product();
                    piwp.product.productId = pDto.productId;
                    piwp.product.productName = pDto.productName;
                    piwp.product.batchEnabled = pDto.batchEnabled;
                    onlineItems.add(piwp);
                }
            }
            renderReceiveItems(onlineItems, containerItems, progressItems, textItemsCount, itemInputMap,
                    itemRemainingMap, itemProductMap, itemBatchNoMap, itemExpiryEpochMap);
        } else {
            purchaseRepository.getPurchaseItemsWithProduct(purchase.purchaseId).observe(this, new Observer<List<PurchaseItemWithProduct>>() {
                @Override
                public void onChanged(List<PurchaseItemWithProduct> purchaseItems) {
                    purchaseRepository.getPurchaseItemsWithProduct(purchase.purchaseId).removeObserver(this);
                    renderReceiveItems(purchaseItems, containerItems, progressItems, textItemsCount, itemInputMap,
                            itemRemainingMap, itemProductMap, itemBatchNoMap, itemExpiryEpochMap);
                }
            });
        }

        // Load Previous Receive Stages (if any)
        LinearLayout sectionPreviousStages = sheetView.findViewById(R.id.section_previous_stages);
        LinearLayout containerPreviousStages = sheetView.findViewById(R.id.container_previous_stages);

        if (purchaseRepository != null && purchaseRepository.isOnlineMode()) {
            sectionPreviousStages.setVisibility(View.GONE);
        } else {
            purchaseRepository.getReceiveRecordsWithItems(purchase.purchaseId).observe(this, new Observer<List<ReceiveRecordWithItems>>() {
                @Override
                public void onChanged(List<ReceiveRecordWithItems> stageRecords) {
                    containerPreviousStages.removeAllViews();
                    if (stageRecords == null || stageRecords.isEmpty()) {
                        sectionPreviousStages.setVisibility(View.GONE);
                        return;
                    }

                    sectionPreviousStages.setVisibility(View.VISIBLE);
                    LayoutInflater inflater = LayoutInflater.from(PendingReceiveActivity.this);
                    SimpleDateFormat dtDisplayFormat = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault());
                    SimpleDateFormat parseFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

                    int stageNumber = 1;
                    for (ReceiveRecordWithItems recWithItems : stageRecords) {
                        ReceiveRecord record = recWithItems.record;
                        if (record == null) continue;

                        View stageView = inflater.inflate(R.layout.item_receive_stage, containerPreviousStages, false);

                        TextView textStageTitle = stageView.findViewById(R.id.text_stage_title);
                        TextView textStageDateTime = stageView.findViewById(R.id.text_stage_date_time);
                        LinearLayout containerStageItems = stageView.findViewById(R.id.container_stage_items);
                        TextView textStageNotes = stageView.findViewById(R.id.text_stage_notes);
                        TextView textStageTotalUnits = stageView.findViewById(R.id.text_stage_total_units);
                        View btnDownloadBill = stageView.findViewById(R.id.btn_download_stage_bill);

                        final int currentStageNumber = stageNumber;
                        textStageTitle.setText(String.format(Locale.getDefault(), "STAGE %d", currentStageNumber));

                        String dateDisplay = record.createdAt;
                        if (!TextUtils.isEmpty(record.createdAt)) {
                            try {
                                Date d = parseFormat.parse(record.createdAt);
                                if (d != null) dateDisplay = dtDisplayFormat.format(d);
                            } catch (ParseException ignored) {}
                        } else if (!TextUtils.isEmpty(record.receiveDate)) {
                            dateDisplay = record.receiveDate;
                        }
                        textStageDateTime.setText(dateDisplay != null ? dateDisplay : "");

                        int totalStageUnits = 0;
                        containerStageItems.removeAllViews();

                        if (recWithItems.items != null) {
                            for (ReceiveItem rItem : recWithItems.items) {
                                totalStageUnits += rItem.quantityReceived;

                                Product prod = itemProductMap.get(rItem.purchaseItemId);
                                String prodName = (prod != null && !TextUtils.isEmpty(prod.productName))
                                        ? prod.productName
                                        : "Product #" + rItem.productId;

                                TextView itemRow = new TextView(PendingReceiveActivity.this);
                                itemRow.setText(String.format(Locale.getDefault(), "• %d × %s", rItem.quantityReceived, prodName));
                                itemRow.setTextColor(ContextCompat.getColor(PendingReceiveActivity.this, R.color.fg));
                                itemRow.setTextSize(12f);
                                itemRow.setPadding(0, 2, 0, 2);
                                containerStageItems.addView(itemRow);
                            }
                        }

                        textStageTotalUnits.setText(String.format(Locale.getDefault(), "Total Received: %d Unit%s",
                                totalStageUnits, totalStageUnits == 1 ? "" : "s"));

                        if (!TextUtils.isEmpty(record.notes)) {
                            textStageNotes.setVisibility(View.VISIBLE);
                            textStageNotes.setText(String.format("Notes: %s", record.notes));
                        } else {
                            textStageNotes.setVisibility(View.GONE);
                        }

                        final int recordId = record.receiveRecordId;
                        btnDownloadBill.setOnClickListener(v -> {
                            btnDownloadBill.setEnabled(false);
                            purchaseRepository.getReceiveDetailsForBill(recordId, new PurchaseRepository.ReceiveDetailsCallback() {
                                @Override
                                public void onLoaded(ReceiveRecord r, List<ReceiveItem> items, Purchase p, List<PurchaseItemWithProduct> allItems) {
                                    btnDownloadBill.setEnabled(true);
                                    Bitmap billBitmap = CommonFunctions.createReceiveBillBitmap(
                                            PendingReceiveActivity.this,
                                            r,
                                            items,
                                            p != null ? p : purchase,
                                            supplier,
                                            allItems,
                                            cachedCompanyConfigs
                                    );

                                    if (billBitmap != null) {
                                        String fileName = "GRN_Stage" + currentStageNumber + "_" + r.receiveRecordId + "_" + System.currentTimeMillis() + ".png";
                                        BitmapHelper.saveBitmapToDownloads(billBitmap, PendingReceiveActivity.this, fileName);
                                        Toast.makeText(PendingReceiveActivity.this,
                                                String.format(Locale.getDefault(), "Stage %d GRN Bill saved to Downloads!", currentStageNumber),
                                                Toast.LENGTH_LONG).show();
                                        File pngFile = BitmapHelper.saveBitmapAsPng(PendingReceiveActivity.this, billBitmap, fileName);
                                        if (pngFile != null) {
                                            String title = "GRN Stage " + currentStageNumber;
                                            BillViewerActivity.start(PendingReceiveActivity.this, pngFile.getAbsolutePath(), title, fileName);
                                        }
                                    } else {
                                        Toast.makeText(PendingReceiveActivity.this, "Failed to create bill bitmap.", Toast.LENGTH_SHORT).show();
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                    btnDownloadBill.setEnabled(true);
                                    Toast.makeText(PendingReceiveActivity.this, "Error generating bill: " + message, Toast.LENGTH_LONG).show();
                                }
                            });
                        });

                        containerPreviousStages.addView(stageView);
                        stageNumber++;
                    }
                }
            });
        }

        // Top 'Receive All' button
        btnReceiveAll.setOnClickListener(v -> {
            for (Map.Entry<Integer, EditText> entry : itemInputMap.entrySet()) {
                int itemId = entry.getKey();
                int remaining = itemRemainingMap.containsKey(itemId) ? itemRemainingMap.get(itemId) : 0;
                entry.getValue().setText(String.valueOf(remaining));
            }
        });

        // Confirm Receive Button
        btnConfirmReceive.setOnClickListener(v -> {
            Map<Integer, Integer> receiveQtyMap = new HashMap<>();
            Map<Integer, PurchaseRepository.BatchReceiveInput> batchInputMap = new HashMap<>();
            int totalToReceive = 0;

            for (Map.Entry<Integer, EditText> entry : itemInputMap.entrySet()) {
                int itemId = entry.getKey();
                int qty = parseQty(entry.getValue().getText().toString());
                if (qty > 0) {
                    receiveQtyMap.put(itemId, qty);
                    totalToReceive += qty;

                    Product prod = itemProductMap.get(itemId);
                    if (prod != null && prod.batchEnabled) {
                        EditText editBatch = itemBatchNoMap.get(itemId);
                        String bNo = editBatch != null ? editBatch.getText().toString().trim() : "";
                        Long expEpoch = itemExpiryEpochMap.get(itemId);

                        if (TextUtils.isEmpty(bNo)) {
                            Toast.makeText(this, "Batch number is required for " + prod.productName, Toast.LENGTH_LONG).show();
                            if (editBatch != null) editBatch.requestFocus();
                            return;
                        }

                        if (expEpoch == null || expEpoch <= 0) {
                            Toast.makeText(this, "Expiry date is required for " + prod.productName, Toast.LENGTH_LONG).show();
                            return;
                        }

                        batchInputMap.put(itemId, new PurchaseRepository.BatchReceiveInput(bNo, expEpoch));
                    }
                }
            }

            if (totalToReceive <= 0) {
                Toast.makeText(this, "Please specify received quantity (> 0) for at least one item.", Toast.LENGTH_SHORT).show();
                return;
            }

            String notes = editNotes.getText().toString().trim();
            executeReceive(purchase, supplier, receiveQtyMap, batchInputMap, notes, dialog, btnConfirmReceive);
        });

        // Cancel Order Button
        btnCancel.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Cancel Order")
                    .setMessage("Are you sure you want to cancel this order? Any items already received will remain in inventory, and remaining items will be cancelled.")
                    .setPositiveButton("Yes, Cancel Order", (d, which) -> {
                        d.dismiss();
                        btnCancel.setEnabled(false);
                        purchaseRepository.cancelPurchase(purchase.purchaseId, new PurchaseRepository.PurchaseActionCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(PendingReceiveActivity.this, "Order cancelled.", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            }

                            @Override
                            public void onError(String message) {
                                btnCancel.setEnabled(true);
                                Toast.makeText(PendingReceiveActivity.this, "Failed to cancel: " + message, Toast.LENGTH_LONG).show();
                            }
                        });
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        dialog.show();
    }

    private void executeReceive(
            Purchase purchase,
            Suppliers supplier,
            Map<Integer, Integer> receiveQtyMap,
            Map<Integer, PurchaseRepository.BatchReceiveInput> batchInputMap,
            String notes,
            BottomSheetDialog dialog,
            Button btnConfirmReceive
    ) {
        btnConfirmReceive.setEnabled(false);
        btnConfirmReceive.setText("Processing...");

        long currentUserId = 1;
        if (GlobalStore.getInstance().getLoggedInUser() != null) {
            currentUserId = GlobalStore.getInstance().getLoggedInUser().id;
        }

        final int purchaseId = purchase.purchaseId;

        if (purchaseRepository != null && purchaseRepository.isOnlineMode()) {
            List<in.gbtsolutions.inventoryhub.online.models.OnlineReceiveItemsRequest.ReceiveItemPayload> payloads = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : receiveQtyMap.entrySet()) {
                int itemId = entry.getKey();
                int qty = entry.getValue();
                String bNo = null;
                String expDateStr = null;
                if (batchInputMap.containsKey(itemId)) {
                    PurchaseRepository.BatchReceiveInput bi = batchInputMap.get(itemId);
                    if (bi != null) {
                        bNo = bi.batchNo;
                        if (bi.expiryDate > 0) {
                            expDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(bi.expiryDate));
                        }
                    }
                }
                payloads.add(new in.gbtsolutions.inventoryhub.online.models.OnlineReceiveItemsRequest.ReceiveItemPayload(
                        itemId, qty, bNo, expDateStr));
            }

            purchaseRepository.getOnlineRepository().receiveItems(purchaseId, notes, payloads, new in.gbtsolutions.inventoryhub.online.repository.OnlinePurchaseRepository.ReceiveActionCallback() {
                @Override
                public void onSuccess() {
                    btnConfirmReceive.setEnabled(true);
                    btnConfirmReceive.setText(R.string.btn_receive_items);
                    Toast.makeText(PendingReceiveActivity.this, "Stock received successfully on cloud!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadPendingPurchases("");
                }

                @Override
                public void onError(String errorMessage) {
                    btnConfirmReceive.setEnabled(true);
                    btnConfirmReceive.setText(R.string.btn_receive_items);
                    Toast.makeText(PendingReceiveActivity.this, "Cloud Error: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        purchaseRepository.processReceive(purchaseId, receiveQtyMap, batchInputMap, notes, currentUserId, new PurchaseRepository.ReceiveOrderCallback() {
            @Override
            public void onSuccess(long receiveRecordId, boolean isFullReceived, Purchase updatedPurchase) {
                btnConfirmReceive.setEnabled(true);
                btnConfirmReceive.setText(R.string.btn_receive_items);

                boolean saveToGallery = GlobalStore.getInstance().isSaveBillToGallery();
                boolean printBill = GlobalStore.getInstance().isPrintBill();

                purchaseRepository.getReceiveDetailsForBill((int) receiveRecordId, new PurchaseRepository.ReceiveDetailsCallback() {
                    @Override
                    public void onLoaded(ReceiveRecord record, List<ReceiveItem> items, Purchase p, List<PurchaseItemWithProduct> allItems) {
                        Bitmap billBitmap = CommonFunctions.createReceiveBillBitmap(
                                PendingReceiveActivity.this,
                                record,
                                items,
                                p != null ? p : updatedPurchase,
                                supplier,
                                allItems,
                                cachedCompanyConfigs
                        );

                        if (billBitmap != null) {
                            String fileName = "GRN_" + record.receiveRecordId + "_" + System.currentTimeMillis() + ".png";
                            String title = "Goods Receipt #" + record.receiveRecordId;
                            BillViewerActivity.openBill(PendingReceiveActivity.this, billBitmap, fileName, title, saveToGallery, printBill);
                        }
                        Toast.makeText(PendingReceiveActivity.this,
                                isFullReceived ? "Order completed and fully received!" : "Goods received successfully!",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(PendingReceiveActivity.this,
                                isFullReceived ? "Order completed and fully received!" : "Goods received successfully!",
                                Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }
                });
            }

            @Override
            public void onError(String message) {
                btnConfirmReceive.setEnabled(true);
                btnConfirmReceive.setText(R.string.btn_receive_items);

                // Show clear error dialog when batch number exists but expiry date doesn't match
                new MaterialAlertDialogBuilder(PendingReceiveActivity.this)
                        .setTitle("Receive Error")
                        .setMessage(message)
                        .setPositiveButton("OK", null)
                        .show();
            }
        });
    }

    private void renderReceiveItems(
            List<PurchaseItemWithProduct> purchaseItems,
            LinearLayout containerItems,
            ProgressBar progressItems,
            TextView textItemsCount,
            Map<Integer, EditText> itemInputMap,
            Map<Integer, Integer> itemRemainingMap,
            Map<Integer, Product> itemProductMap,
            Map<Integer, EditText> itemBatchNoMap,
            Map<Integer, Long> itemExpiryEpochMap
    ) {
        progressItems.setVisibility(View.GONE);
        containerItems.removeAllViews();

        if (purchaseItems == null || purchaseItems.isEmpty()) {
            textItemsCount.setText("0 items");
            return;
        }

        textItemsCount.setText(String.format(Locale.getDefault(), "%d item%s",
                purchaseItems.size(), purchaseItems.size() == 1 ? "" : "s"));

        LayoutInflater inflater = LayoutInflater.from(this);
        SimpleDateFormat expiryDisplayFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        expiryDisplayFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (PurchaseItemWithProduct itemWithProduct : purchaseItems) {
            PurchaseItem pItem = itemWithProduct.purchaseItem;
            Product prod = itemWithProduct.product;
            if (pItem == null) continue;

            final int itemId = pItem.purchaseItemId;
            int ordered = pItem.quantity;
            int received = pItem.receivedQuantity;
            int remaining = Math.max(0, ordered - received);

            itemRemainingMap.put(itemId, remaining);
            if (prod != null) {
                itemProductMap.put(itemId, prod);
            }

            View itemView = inflater.inflate(R.layout.item_receive_product, containerItems, false);

            TextView textProdName = itemView.findViewById(R.id.text_item_product_name);
            TextView textRemainingBadge = itemView.findViewById(R.id.text_item_remaining_badge);
            TextView textSku = itemView.findViewById(R.id.text_item_sku);
            TextView textOrderedQty = itemView.findViewById(R.id.text_item_ordered_qty);
            TextView textReceivedQty = itemView.findViewById(R.id.text_item_received_qty);
            TextView textRemainingQty = itemView.findViewById(R.id.text_item_remaining_qty);
            View layoutReceiveContainer = itemView.findViewById(R.id.layout_receive_input_container);
            ImageView btnQtyMinus = itemView.findViewById(R.id.btn_qty_minus);
            EditText editReceiveQty = itemView.findViewById(R.id.edit_receive_qty);
            ImageView btnQtyPlus = itemView.findViewById(R.id.btn_qty_plus);
            TextView btnReceiveAllItem = itemView.findViewById(R.id.btn_receive_all);
            View layoutBatchContainer = itemView.findViewById(R.id.layout_batch_container);
            EditText editBatchNo = itemView.findViewById(R.id.edit_batch_number);
            View boxExpiryDate = itemView.findViewById(R.id.box_expiry_date);
            TextView textExpiryDate = itemView.findViewById(R.id.text_batch_expiry_date);
            View textFullyReceived = itemView.findViewById(R.id.text_item_fully_received);

            String name = (prod != null && !TextUtils.isEmpty(prod.productName))
                    ? prod.productName
                    : "Product #" + pItem.productId;
            textProdName.setText(name);

            StringBuilder skuText = new StringBuilder();
            if (prod != null && !TextUtils.isEmpty(prod.sku)) {
                skuText.append("SKU: ").append(prod.sku);
            }
            if (pItem.unitPrice > 0) {
                if (skuText.length() > 0) skuText.append(" • ");
                skuText.append(String.format(Locale.getDefault(), "Rate: ₹ %.2f", pItem.unitPrice));
            }
            textSku.setText(skuText.toString());

            textOrderedQty.setText(String.format(Locale.getDefault(), "Ordered: %d", ordered));
            textReceivedQty.setText(String.format(Locale.getDefault(), "Received: %d", received));
            textRemainingQty.setText(String.format(Locale.getDefault(), "Remaining: %d", remaining));

            if (remaining <= 0) {
                textRemainingBadge.setText("Completed");
                textRemainingBadge.setTextColor(ContextCompat.getColor(this, R.color.status_green));
                layoutReceiveContainer.setVisibility(View.GONE);
                layoutBatchContainer.setVisibility(View.GONE);
                textFullyReceived.setVisibility(View.VISIBLE);
            } else {
                textRemainingBadge.setText(String.format(Locale.getDefault(), "%d Pending", remaining));
                textRemainingBadge.setTextColor(ContextCompat.getColor(this, R.color.status_orange));
                layoutReceiveContainer.setVisibility(View.VISIBLE);
                textFullyReceived.setVisibility(View.GONE);

                itemInputMap.put(itemId, editReceiveQty);
                editReceiveQty.setText(String.valueOf(remaining));

                btnQtyMinus.setOnClickListener(v -> {
                    int currentVal = parseQty(editReceiveQty.getText().toString());
                    if (currentVal > 0) {
                        editReceiveQty.setText(String.valueOf(currentVal - 1));
                    }
                });

                btnQtyPlus.setOnClickListener(v -> {
                    int currentVal = parseQty(editReceiveQty.getText().toString());
                    if (currentVal < remaining) {
                        editReceiveQty.setText(String.valueOf(currentVal + 1));
                    }
                });

                btnReceiveAllItem.setOnClickListener(v -> {
                    editReceiveQty.setText(String.valueOf(remaining));
                });

                editReceiveQty.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        int entered = parseQty(s.toString());
                        if (entered > remaining) {
                            editReceiveQty.setText(String.valueOf(remaining));
                            editReceiveQty.setSelection(String.valueOf(remaining).length());
                        }
                    }
                });

                if (prod != null && prod.batchEnabled) {
                    layoutBatchContainer.setVisibility(View.VISIBLE);
                    itemBatchNoMap.put(itemId, editBatchNo);

                    boxExpiryDate.setOnClickListener(v -> {
                        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                                .setTitleText("Select Expiry Date")
                                .setSelection(itemExpiryEpochMap.containsKey(itemId)
                                        ? itemExpiryEpochMap.get(itemId)
                                        : MaterialDatePicker.todayInUtcMilliseconds())
                                .build();

                        datePicker.addOnPositiveButtonClickListener(selection -> {
                            itemExpiryEpochMap.put(itemId, selection);
                            textExpiryDate.setText(expiryDisplayFormat.format(new Date(selection)));
                        });

                        datePicker.show(getSupportFragmentManager(), "EXPIRY_" + itemId);
                    });
                } else {
                    layoutBatchContainer.setVisibility(View.GONE);
                }
            }

            containerItems.addView(itemView);
        }
    }

    private int parseQty(String text) {
        if (TextUtils.isEmpty(text)) return 0;
        try {
            return Math.max(0, Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }
}