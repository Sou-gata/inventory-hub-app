package in.gbtsolutions.inventoryhub.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import in.gbtsolutions.inventoryhub.R;
import in.gbtsolutions.inventoryhub.models.Buyer;
import in.gbtsolutions.inventoryhub.models.CartItem;
import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseItem;
import in.gbtsolutions.inventoryhub.models.PurchaseItemWithProduct;
import in.gbtsolutions.inventoryhub.models.ReceiveItem;
import in.gbtsolutions.inventoryhub.models.ReceiveRecord;
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleItem;
import in.gbtsolutions.inventoryhub.models.SaleItemWithProduct;
import in.gbtsolutions.inventoryhub.models.Suppliers;

public class CommonFunctions {

    public static Bitmap createSellBillBitmap(Context context, Sale sale, Buyer buyer, List<SaleItemWithProduct> saleItems, Map<String, String> cachedCompanyConfigs) {
        if (sale == null || context == null) return null;

        // Inflate the sell bill layout
        View billView = LayoutInflater.from(context).inflate(R.layout.layout_sell_bill, null);

        // 1. Company / Store Details Header
        TextView tvCompanyName = billView.findViewById(R.id.tv_bill_company_name);
        TextView tvCompanyAddress = billView.findViewById(R.id.tv_bill_company_address);
        TextView tvCompanyContact = billView.findViewById(R.id.tv_bill_company_contact);
        TextView tvCompanyGstin = billView.findViewById(R.id.tv_bill_company_gstin);

        String companyName = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("company_name") : null;
        if (!TextUtils.isEmpty(companyName)) {
            tvCompanyName.setText(companyName);
        } else {
            tvCompanyName.setText("INVENTORY HUB");
        }

        StringBuilder addressBuilder = new StringBuilder();
        String addr = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("address") : null;
        String dist = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("district") : null;
        String state = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("state") : null;
        String pin = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("postal_code") : null;
        if (!TextUtils.isEmpty(addr)) addressBuilder.append(addr);
        if (!TextUtils.isEmpty(dist)) {
            if (addressBuilder.length() > 0) addressBuilder.append("\n");
            addressBuilder.append(dist);
        }
        if (!TextUtils.isEmpty(state)) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(state);
        }
        if (!TextUtils.isEmpty(pin)) {
            if (addressBuilder.length() > 0) addressBuilder.append(" - ");
            addressBuilder.append(pin);
        }
        if (addressBuilder.length() > 0) {
            tvCompanyAddress.setVisibility(View.VISIBLE);
            tvCompanyAddress.setText(addressBuilder.toString());
        } else {
            tvCompanyAddress.setVisibility(View.GONE);
        }

        String phone = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("company_phone") : null;
        if (!TextUtils.isEmpty(phone)) {
            tvCompanyContact.setVisibility(View.VISIBLE);
            tvCompanyContact.setText(String.format("Ph: %s", phone));
        } else {
            tvCompanyContact.setVisibility(View.GONE);
        }

        String gst = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("gst_number") : null;
        if (!TextUtils.isEmpty(gst)) {
            tvCompanyGstin.setVisibility(View.VISIBLE);
            tvCompanyGstin.setText(String.format("GSTIN: %s", gst));
        } else {
            tvCompanyGstin.setVisibility(View.GONE);
        }

        // 2. Invoice Meta Row
        TextView tvInvoiceNo = billView.findViewById(R.id.tv_bill_invoice_no);
        TextView tvDateTime = billView.findViewById(R.id.tv_bill_date_time);

        String invoiceDisplay = !TextUtils.isEmpty(sale.invoiceId)
                ? sale.invoiceId
                : String.format(Locale.getDefault(), "#SALE-%d", sale.saleId);
        tvInvoiceNo.setText(invoiceDisplay);

        String dateStr = sale.createdAt;
        if (TextUtils.isEmpty(dateStr)) {
            dateStr = sale.billingDate;
        }
        if (!TextUtils.isEmpty(dateStr)) {
            tvDateTime.setText(dateStr);
        } else {
            tvDateTime.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
        }

        // 3. Customer / Buyer Details
        TextView tvCustName = billView.findViewById(R.id.tv_bill_customer_name);
        TextView tvCustPhone = billView.findViewById(R.id.tv_bill_customer_phone);
        TextView tvCustGst = billView.findViewById(R.id.tv_bill_customer_gstin);
        TextView tvCustAddress = billView.findViewById(R.id.tv_bill_customer_address);

        String custName;
        String custPhone;
        String custGst;
        String custAddress = null;

        if (buyer != null) {
            custName = !TextUtils.isEmpty(buyer.buyerName) ? buyer.buyerName : (!TextUtils.isEmpty(sale.customerName) ? sale.customerName : "Customer");
            custPhone = !TextUtils.isEmpty(buyer.phone) ? buyer.phone : sale.customerPhone;
            custGst = !TextUtils.isEmpty(buyer.gst) ? buyer.gst : sale.customerGstin;

            StringBuilder buyerAddressBuilder = new StringBuilder();
            if (!TextUtils.isEmpty(buyer.address)) buyerAddressBuilder.append(buyer.address);
            if (!TextUtils.isEmpty(buyer.city)) {
                if (buyerAddressBuilder.length() > 0) buyerAddressBuilder.append(", ");
                buyerAddressBuilder.append(buyer.city);
            }
            if (!TextUtils.isEmpty(buyer.stateCode)) {
                if (buyerAddressBuilder.length() > 0) buyerAddressBuilder.append(", ");
                buyerAddressBuilder.append(buyer.stateCode);
            }
            if (!TextUtils.isEmpty(buyer.postalCode)) {
                if (buyerAddressBuilder.length() > 0) buyerAddressBuilder.append(" - ");
                buyerAddressBuilder.append(buyer.postalCode);
            }

            if (buyerAddressBuilder.length() > 0) {
                custAddress = buyerAddressBuilder.toString();
            } else if (!TextUtils.isEmpty(sale.placeOfSupply)) {
                custAddress = sale.placeOfSupply;
            }
        } else {
            custName = !TextUtils.isEmpty(sale.customerName) ? sale.customerName : "Walk-in Customer";
            custPhone = sale.customerPhone;
            custGst = sale.customerGstin;
            if (!TextUtils.isEmpty(sale.placeOfSupply)) {
                custAddress = sale.placeOfSupply;
            }
        }

        tvCustName.setText(!TextUtils.isEmpty(custName) ? custName : "Walk-in Customer");

        if (!TextUtils.isEmpty(custPhone)) {
            tvCustPhone.setVisibility(View.VISIBLE);
            tvCustPhone.setText(custPhone);
        } else {
            tvCustPhone.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(custGst)) {
            tvCustGst.setVisibility(View.VISIBLE);
            tvCustGst.setText(String.format("GSTIN: %s", custGst));
        } else {
            tvCustGst.setVisibility(View.GONE);
        }

        if (!TextUtils.isEmpty(custAddress)) {
            tvCustAddress.setVisibility(View.VISIBLE);
            tvCustAddress.setText(custAddress.startsWith("Address:") ? custAddress : String.format("Address: %s", custAddress));
        } else {
            tvCustAddress.setVisibility(View.GONE);
        }

        // 4. Products Main Body
        LinearLayout containerBillItems = billView.findViewById(R.id.container_bill_items);
        containerBillItems.removeAllViews(); // Clear sample preview items

        LayoutInflater inflater = LayoutInflater.from(context);
        int totalItemsCount = 0;
        int totalQtyCount = 0;

        if (saleItems != null && !saleItems.isEmpty()) {
            for (SaleItemWithProduct itemWithProd : saleItems) {
                SaleItem sItem = itemWithProd.saleItem;
                if (sItem == null) continue;

                totalItemsCount++;
                totalQtyCount += sItem.quantity;

                View itemView = inflater.inflate(R.layout.item_thermal_bill_product, containerBillItems, false);
                TextView tvItemName = itemView.findViewById(R.id.tv_bill_item_name);
                TextView tvItemSubtext = itemView.findViewById(R.id.tv_bill_item_subtext);
                TextView tvItemQty = itemView.findViewById(R.id.tv_bill_item_qty);
                TextView tvItemRate = itemView.findViewById(R.id.tv_bill_item_rate);
                TextView tvItemAmount = itemView.findViewById(R.id.tv_bill_item_amount);
                TextView tvItemTaxDisc = itemView.findViewById(R.id.tv_bill_item_tax_disc);

                String prodName = (itemWithProd.product != null && !TextUtils.isEmpty(itemWithProd.product.productName))
                        ? itemWithProd.product.productName
                        : String.format(Locale.getDefault(), "Product #%d", sItem.productId);
                tvItemName.setText(prodName);

                StringBuilder metaBuilder = new StringBuilder();
                if (itemWithProd.product != null && !TextUtils.isEmpty(itemWithProd.product.sku)) {
                    metaBuilder.append("SKU: ").append(itemWithProd.product.sku);
                }
                if (itemWithProd.batch != null && !TextUtils.isEmpty(itemWithProd.batch.batchNo)) {
                    if (metaBuilder.length() > 0) metaBuilder.append(" | ");
                    metaBuilder.append("B: ").append(itemWithProd.batch.batchNo);
                } else if (sItem.batchId != null && sItem.batchId > 0) {
                    if (metaBuilder.length() > 0) metaBuilder.append(" | ");
                    metaBuilder.append("B: #").append(sItem.batchId);
                }

                if (metaBuilder.length() > 0) {
                    tvItemSubtext.setVisibility(View.VISIBLE);
                    tvItemSubtext.setText(metaBuilder.toString());
                } else {
                    tvItemSubtext.setVisibility(View.GONE);
                }

                tvItemQty.setText(String.format(Locale.getDefault(), "%d.00", sItem.quantity));
                tvItemRate.setText(String.format(Locale.getDefault(), "%,.2f", sItem.unitPrice));
                tvItemAmount.setText(String.format(Locale.getDefault(), "%,.2f", sItem.subtotal));

                if (sItem.discountAmount > 0) {
                    tvItemTaxDisc.setVisibility(View.VISIBLE);
                    tvItemTaxDisc.setText(String.format(Locale.getDefault(), "Disc: ₹ %,.2f", sItem.discountAmount));
                } else {
                    tvItemTaxDisc.setVisibility(View.GONE);
                }

                containerBillItems.addView(itemView);
            }
        }

        // 5. Footer Summary
        TextView tvTotalItems = billView.findViewById(R.id.tv_bill_total_items);
        TextView tvTotalQty = billView.findViewById(R.id.tv_bill_total_qty);
        tvTotalItems.setText(String.valueOf(totalItemsCount));
        tvTotalQty.setText(String.format(Locale.getDefault(), "%d.00", totalQtyCount));

        TextView tvSubtotal = billView.findViewById(R.id.tv_bill_subtotal);
        tvSubtotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.subtotalAmount));

        View layoutDiscount = billView.findViewById(R.id.layout_bill_discount);
        TextView tvDiscount = billView.findViewById(R.id.tv_bill_discount);
        if (sale.discountAmount > 0) {
            layoutDiscount.setVisibility(View.VISIBLE);
            tvDiscount.setText(String.format(Locale.getDefault(), "- ₹ %,.2f", sale.discountAmount));
        } else {
            layoutDiscount.setVisibility(View.GONE);
        }

        View layoutCgst = billView.findViewById(R.id.layout_bill_cgst);
        TextView tvCgst = billView.findViewById(R.id.tv_bill_cgst);
        if (sale.cgstAmount > 0) {
            layoutCgst.setVisibility(View.VISIBLE);
            tvCgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.cgstAmount));
        } else {
            layoutCgst.setVisibility(View.GONE);
        }

        View layoutSgst = billView.findViewById(R.id.layout_bill_sgst);
        TextView tvSgst = billView.findViewById(R.id.tv_bill_sgst);
        if (sale.sgstAmount > 0) {
            layoutSgst.setVisibility(View.VISIBLE);
            tvSgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.sgstAmount));
        } else {
            layoutSgst.setVisibility(View.GONE);
        }

        View layoutIgst = billView.findViewById(R.id.layout_bill_igst);
        TextView tvIgst = billView.findViewById(R.id.tv_bill_igst);
        if (sale.igstAmount > 0) {
            layoutIgst.setVisibility(View.VISIBLE);
            tvIgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.igstAmount));
        } else {
            layoutIgst.setVisibility(View.GONE);
        }

        View layoutOther = billView.findViewById(R.id.layout_bill_other_charges);
        TextView tvOther = billView.findViewById(R.id.tv_bill_other_charges);
        if (sale.otherCharges > 0) {
            layoutOther.setVisibility(View.VISIBLE);
            tvOther.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.otherCharges));
        } else {
            layoutOther.setVisibility(View.GONE);
        }

        View layoutRoundOff = billView.findViewById(R.id.layout_bill_round_off);
        if (layoutRoundOff != null) {
            layoutRoundOff.setVisibility(View.GONE);
        }

        TextView tvGrandTotal = billView.findViewById(R.id.tv_bill_grand_total);
        tvGrandTotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", sale.totalAmount));

        TextView tvPayMode = billView.findViewById(R.id.tv_bill_payment_mode);
        if (tvPayMode != null) {
            String payMode = !TextUtils.isEmpty(sale.paymentMethod) ? sale.paymentMethod.toUpperCase(Locale.getDefault()) : "CASH";
            tvPayMode.setText(payMode);
        }

        TextView tvPayStatus = billView.findViewById(R.id.tv_bill_payment_status);
        String status = !TextUtils.isEmpty(sale.status) ? sale.status.toUpperCase(Locale.getDefault()) : "PAID";
        if (tvPayStatus != null) {
            tvPayStatus.setText(String.format("[ %s ]", status));
            if ("CANCELLED".equalsIgnoreCase(status)) {
                tvPayStatus.setTextColor(ContextCompat.getColor(context, R.color.status_red));
            }
        }

        // 6. Measure and create bitmap of width 360dp using BitmapHelper
        int widthPx = BitmapHelper.convertDpToPx(context, 360f);
        return BitmapHelper.createBitmapFromView(billView, widthPx, 0);
    }

    public static Bitmap createReceiveBillBitmap(
            Context context,
            ReceiveRecord record,
            List<ReceiveItem> receiveItems,
            Purchase purchase,
            Suppliers supplier,
            List<PurchaseItemWithProduct> allPurchaseItems,
            Map<String, String> cachedCompanyConfigs
    ) {
        if (record == null || purchase == null || context == null) return null;

        View billView = LayoutInflater.from(context).inflate(R.layout.layout_receive_bill, null);

        // 1. Company info
        TextView tvCompanyName = billView.findViewById(R.id.tv_bill_company_name);
        TextView tvCompanyAddress = billView.findViewById(R.id.tv_bill_company_address);
        TextView tvCompanyContact = billView.findViewById(R.id.tv_bill_company_contact);
        TextView tvCompanyGstin = billView.findViewById(R.id.tv_bill_company_gstin);

        String companyName = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("company_name") : null;
        tvCompanyName.setText(!TextUtils.isEmpty(companyName) ? companyName : "INVENTORY HUB");

        StringBuilder addressBuilder = new StringBuilder();
        String addr = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("address") : null;
        String dist = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("district") : null;
        String state = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("state") : null;
        String pin = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("postal_code") : null;
        if (!TextUtils.isEmpty(addr)) addressBuilder.append(addr);
        if (!TextUtils.isEmpty(dist)) {
            if (addressBuilder.length() > 0) addressBuilder.append("\n");
            addressBuilder.append(dist);
        }
        if (!TextUtils.isEmpty(state)) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(state);
        }
        if (!TextUtils.isEmpty(pin)) {
            if (addressBuilder.length() > 0) addressBuilder.append(" - ");
            addressBuilder.append(pin);
        }
        if (addressBuilder.length() > 0) {
            tvCompanyAddress.setVisibility(View.VISIBLE);
            tvCompanyAddress.setText(addressBuilder.toString());
        } else {
            tvCompanyAddress.setVisibility(View.GONE);
        }

        String phone = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("company_phone") : null;
        if (!TextUtils.isEmpty(phone)) {
            tvCompanyContact.setVisibility(View.VISIBLE);
            tvCompanyContact.setText(String.format("Ph: %s", phone));
        } else {
            tvCompanyContact.setVisibility(View.GONE);
        }

        String gst = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("gst_number") : null;
        if (tvCompanyGstin != null) {
            if (!TextUtils.isEmpty(gst)) {
                tvCompanyGstin.setVisibility(View.VISIBLE);
                tvCompanyGstin.setText(String.format("GSTIN: %s", gst));
            } else {
                tvCompanyGstin.setVisibility(View.GONE);
            }
        }

        // 2. Receipt metadata
        TextView tvReceiptNo = billView.findViewById(R.id.tv_bill_receipt_no);
        TextView tvReceiveDate = billView.findViewById(R.id.tv_bill_receive_date);
        TextView tvPoNo = billView.findViewById(R.id.tv_bill_po_no);
        TextView tvOrderStatus = billView.findViewById(R.id.tv_bill_order_status);

        String receiptDisplay = String.format(Locale.getDefault(), "GRN-%s-%03d",
                new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date()),
                record.receiveRecordId);
        tvReceiptNo.setText(receiptDisplay);

        String dateStr = record.createdAt;
        if (TextUtils.isEmpty(dateStr)) dateStr = record.receiveDate;
        if (!TextUtils.isEmpty(dateStr)) {
            tvReceiveDate.setText(dateStr);
        } else {
            tvReceiveDate.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
        }

        tvPoNo.setText(!TextUtils.isEmpty(purchase.invoiceId) ? purchase.invoiceId : String.format(Locale.getDefault(), "#PO-%d", purchase.purchaseId));
        tvOrderStatus.setText(String.format("[ %s ]", !TextUtils.isEmpty(purchase.status) ? purchase.status.toUpperCase(Locale.getDefault()) : "COMPLETED"));

        // 3. Supplier info
        TextView tvSupplierName = billView.findViewById(R.id.tv_bill_supplier_name);
        TextView tvSupplierPhone = billView.findViewById(R.id.tv_bill_supplier_phone);
        TextView tvSupplierGstin = billView.findViewById(R.id.tv_bill_supplier_gstin);
        TextView tvSupplierAddress = billView.findViewById(R.id.tv_bill_supplier_address);

        if (supplier != null) {
            tvSupplierName.setText(!TextUtils.isEmpty(supplier.supplierName) ? supplier.supplierName : "Supplier");
            if (!TextUtils.isEmpty(supplier.phone)) {
                tvSupplierPhone.setVisibility(View.VISIBLE);
                tvSupplierPhone.setText(String.format("Ph: %s", supplier.phone));
            } else {
                tvSupplierPhone.setVisibility(View.GONE);
            }
            if (!TextUtils.isEmpty(supplier.gst)) {
                tvSupplierGstin.setVisibility(View.VISIBLE);
                tvSupplierGstin.setText(String.format("GSTIN: %s", supplier.gst));
            } else {
                tvSupplierGstin.setVisibility(View.GONE);
            }
            if (tvSupplierAddress != null) {
                StringBuilder supplierAddressBuilder = new StringBuilder();
                if (!TextUtils.isEmpty(supplier.address)) supplierAddressBuilder.append(supplier.address);
                if (!TextUtils.isEmpty(supplier.city)) {
                    if (supplierAddressBuilder.length() > 0) supplierAddressBuilder.append(", ");
                    supplierAddressBuilder.append(supplier.city);
                }
                if (!TextUtils.isEmpty(supplier.stateCode)) {
                    if (supplierAddressBuilder.length() > 0) supplierAddressBuilder.append(", ");
                    supplierAddressBuilder.append(supplier.stateCode);
                }
                if (!TextUtils.isEmpty(supplier.postalCode)) {
                    if (supplierAddressBuilder.length() > 0) supplierAddressBuilder.append(" - ");
                    supplierAddressBuilder.append(supplier.postalCode);
                }
                if (supplierAddressBuilder.length() > 0) {
                    tvSupplierAddress.setVisibility(View.VISIBLE);
                    tvSupplierAddress.setText(String.format("Address: %s", supplierAddressBuilder.toString()));
                } else {
                    tvSupplierAddress.setVisibility(View.GONE);
                }
            }
        } else {
            tvSupplierName.setText("Supplier");
            tvSupplierPhone.setVisibility(View.GONE);
            tvSupplierGstin.setVisibility(View.GONE);
            if (tvSupplierAddress != null) tvSupplierAddress.setVisibility(View.GONE);
        }

        // 4. Items table
        LinearLayout containerItems = billView.findViewById(R.id.container_bill_items);
        containerItems.removeAllViews();

        int totalUnitsReceived = 0;
        int totalItemsCount = 0;
        if (receiveItems != null && !receiveItems.isEmpty()) {
            LayoutInflater inflater = LayoutInflater.from(context);
            int sl = 1;
            for (ReceiveItem rItem : receiveItems) {
                View row = inflater.inflate(R.layout.item_receive_bill_product, containerItems, false);

                TextView tvSl = row.findViewById(R.id.tv_bill_item_sl);
                TextView tvName = row.findViewById(R.id.tv_bill_item_name);
                TextView tvSku = row.findViewById(R.id.tv_bill_item_sku);
                TextView tvOrdered = row.findViewById(R.id.tv_bill_item_ordered_qty);
                TextView tvRecNow = row.findViewById(R.id.tv_bill_item_received_now);
                TextView tvTotalRec = row.findViewById(R.id.tv_bill_item_total_received);

                tvSl.setText(String.format(Locale.getDefault(), "%d.", sl++));
                totalItemsCount++;

                // Find corresponding purchase item
                PurchaseItemWithProduct matchedItem = null;
                if (allPurchaseItems != null) {
                    for (PurchaseItemWithProduct piwp : allPurchaseItems) {
                        if (piwp.purchaseItem != null && piwp.purchaseItem.purchaseItemId == rItem.purchaseItemId) {
                            matchedItem = piwp;
                            break;
                        }
                    }
                }

                if (matchedItem != null) {
                    if (matchedItem.product != null && !TextUtils.isEmpty(matchedItem.product.productName)) {
                        tvName.setText(matchedItem.product.productName);
                    } else {
                        tvName.setText(String.format(Locale.getDefault(), "Product #%d", rItem.productId));
                    }

                    if (matchedItem.product != null && !TextUtils.isEmpty(matchedItem.product.sku)) {
                        tvSku.setVisibility(View.VISIBLE);
                        tvSku.setText(String.format("SKU: %s", matchedItem.product.sku));
                    } else {
                        tvSku.setVisibility(View.INVISIBLE);
                    }

                    tvOrdered.setText(String.valueOf(matchedItem.purchaseItem.quantity));
                    tvRecNow.setText(String.valueOf(rItem.quantityReceived));
                    tvTotalRec.setText(String.format(Locale.getDefault(), "%d / %d",
                            matchedItem.purchaseItem.receivedQuantity,
                            matchedItem.purchaseItem.quantity));
                } else {
                    tvName.setText(String.format(Locale.getDefault(), "Product #%d", rItem.productId));
                    tvSku.setVisibility(View.INVISIBLE);
                    tvOrdered.setText("-");
                    tvRecNow.setText(String.valueOf(rItem.quantityReceived));
                    tvTotalRec.setText(String.valueOf(rItem.quantityReceived));
                }

                totalUnitsReceived += rItem.quantityReceived;
                containerItems.addView(row);
            }
        }

        // 5. Total counts & summary
        TextView tvTotalItems = billView.findViewById(R.id.tv_bill_total_items);
        if (tvTotalItems != null) {
            tvTotalItems.setText(String.valueOf(totalItemsCount));
        }

        TextView tvTotalQty = billView.findViewById(R.id.tv_bill_total_qty);
        if (tvTotalQty != null) {
            tvTotalQty.setText(String.format(Locale.getDefault(), "%d Units", totalUnitsReceived));
        }

        TextView tvTotalUnits = billView.findViewById(R.id.tv_bill_total_units_received);
        if (tvTotalUnits != null) {
            tvTotalUnits.setText(String.format(Locale.getDefault(), "%d Units", totalUnitsReceived));
        }

        // 6. Notes
        View layoutNotes = billView.findViewById(R.id.layout_bill_notes);
        TextView tvNotes = billView.findViewById(R.id.tv_bill_notes);
        if (!TextUtils.isEmpty(record.notes)) {
            layoutNotes.setVisibility(View.VISIBLE);
            tvNotes.setText(record.notes);
        } else {
            layoutNotes.setVisibility(View.GONE);
        }

        // 7. Footer time
        TextView tvFooter = billView.findViewById(R.id.tv_bill_footer_time);
        if (tvFooter != null) {
            tvFooter.setText(String.format("Generated electronically via Inventory Hub on %s",
                    new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date())));
        }

        // 8. Render view to bitmap
        int widthPx = BitmapHelper.convertDpToPx(context, 360f);
        return BitmapHelper.createBitmapFromView(billView, widthPx, 0);
    }

    public static Bitmap createPurchaseBillBitmap(
            Context context,
            Purchase purchase,
            Suppliers supplier,
            List<CartItem> cartItems,
            Map<String, String> cachedCompanyConfigs
    ) {
        if (purchase == null || context == null) return null;

        View billView = LayoutInflater.from(context).inflate(R.layout.layout_purchase_bill, null);

        // 1. Company details
        TextView tvCompanyName = billView.findViewById(R.id.tv_bill_company_name);
        TextView tvCompanyAddress = billView.findViewById(R.id.tv_bill_company_address);
        TextView tvCompanyContact = billView.findViewById(R.id.tv_bill_company_contact);
        TextView tvCompanyGstin = billView.findViewById(R.id.tv_bill_company_gstin);

        String companyName = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("company_name") : null;
        tvCompanyName.setText(!TextUtils.isEmpty(companyName) ? companyName : "INVENTORY HUB");

        StringBuilder addressBuilder = new StringBuilder();
        String addr = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("address") : null;
        String dist = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("district") : null;
        String state = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("state") : null;
        String pin = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("postal_code") : null;
        if (!TextUtils.isEmpty(addr)) addressBuilder.append(addr);
        if (!TextUtils.isEmpty(dist)) {
            if (addressBuilder.length() > 0) addressBuilder.append("\n");
            addressBuilder.append(dist);
        }
        if (!TextUtils.isEmpty(state)) {
            if (addressBuilder.length() > 0) addressBuilder.append(", ");
            addressBuilder.append(state);
        }
        if (!TextUtils.isEmpty(pin)) {
            if (addressBuilder.length() > 0) addressBuilder.append(" - ");
            addressBuilder.append(pin);
        }
        if (addressBuilder.length() > 0) {
            tvCompanyAddress.setVisibility(View.VISIBLE);
            tvCompanyAddress.setText(addressBuilder.toString());
        } else {
            tvCompanyAddress.setVisibility(View.GONE);
        }

        String phone = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("company_phone") : null;
        if (!TextUtils.isEmpty(phone)) {
            tvCompanyContact.setVisibility(View.VISIBLE);
            tvCompanyContact.setText(String.format("Ph: %s", phone));
        } else {
            tvCompanyContact.setVisibility(View.GONE);
        }

        String gst = cachedCompanyConfigs != null ? cachedCompanyConfigs.get("gst_number") : null;
        if (!TextUtils.isEmpty(gst)) {
            tvCompanyGstin.setVisibility(View.VISIBLE);
            tvCompanyGstin.setText(String.format("GSTIN: %s", gst));
        } else {
            tvCompanyGstin.setVisibility(View.GONE);
        }

        // 2. PO Meta
        TextView tvInvoiceNo = billView.findViewById(R.id.tv_bill_invoice_no);
        TextView tvDateTime = billView.findViewById(R.id.tv_bill_date_time);

        String poDisplay = !TextUtils.isEmpty(purchase.invoiceId)
                ? purchase.invoiceId
                : String.format(Locale.getDefault(), "#PO-%d", purchase.purchaseId);
        tvInvoiceNo.setText(poDisplay);

        String dateStr = purchase.createdAt;
        if (TextUtils.isEmpty(dateStr)) {
            dateStr = purchase.billingDate;
        }
        if (!TextUtils.isEmpty(dateStr)) {
            tvDateTime.setText(dateStr);
        } else {
            tvDateTime.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date()));
        }

        // 3. Supplier Details
        TextView tvSupplierName = billView.findViewById(R.id.tv_bill_customer_name);
        TextView tvSupplierPhone = billView.findViewById(R.id.tv_bill_customer_phone);
        TextView tvSupplierGst = billView.findViewById(R.id.tv_bill_customer_gstin);
        TextView tvSupplierAddress = billView.findViewById(R.id.tv_bill_customer_address);

        if (supplier != null) {
            tvSupplierName.setText(!TextUtils.isEmpty(supplier.supplierName) ? supplier.supplierName : "Supplier");
            if (!TextUtils.isEmpty(supplier.phone)) {
                tvSupplierPhone.setVisibility(View.VISIBLE);
                tvSupplierPhone.setText(supplier.phone);
            } else {
                tvSupplierPhone.setVisibility(View.GONE);
            }
            if (!TextUtils.isEmpty(supplier.gst)) {
                tvSupplierGst.setVisibility(View.VISIBLE);
                tvSupplierGst.setText(String.format("GSTIN: %s", supplier.gst));
            } else {
                tvSupplierGst.setVisibility(View.GONE);
            }

            StringBuilder suppAddr = new StringBuilder();
            if (!TextUtils.isEmpty(supplier.address)) suppAddr.append(supplier.address);
            if (!TextUtils.isEmpty(supplier.city)) {
                if (suppAddr.length() > 0) suppAddr.append(", ");
                suppAddr.append(supplier.city);
            }
            if (!TextUtils.isEmpty(supplier.stateCode)) {
                if (suppAddr.length() > 0) suppAddr.append(", ");
                suppAddr.append(supplier.stateCode);
            }
            if (!TextUtils.isEmpty(supplier.postalCode)) {
                if (suppAddr.length() > 0) suppAddr.append(" - ");
                suppAddr.append(supplier.postalCode);
            }
            if (suppAddr.length() > 0) {
                tvSupplierAddress.setVisibility(View.VISIBLE);
                tvSupplierAddress.setText(String.format("Address: %s", suppAddr.toString()));
            } else {
                tvSupplierAddress.setVisibility(View.GONE);
            }
        } else {
            tvSupplierName.setText("Supplier");
            tvSupplierPhone.setVisibility(View.GONE);
            tvSupplierGst.setVisibility(View.GONE);
            tvSupplierAddress.setVisibility(View.GONE);
        }

        // 4. Products table
        LinearLayout containerBillItems = billView.findViewById(R.id.container_bill_items);
        containerBillItems.removeAllViews();

        LayoutInflater inflater = LayoutInflater.from(context);
        int totalItemsCount = 0;
        int totalQtyCount = 0;

        if (cartItems != null && !cartItems.isEmpty()) {
            for (CartItem item : cartItems) {
                if (item.product == null) continue;
                totalItemsCount++;
                totalQtyCount += item.quantity;

                View itemView = inflater.inflate(R.layout.item_thermal_bill_product, containerBillItems, false);
                TextView tvItemName = itemView.findViewById(R.id.tv_bill_item_name);
                TextView tvItemSubtext = itemView.findViewById(R.id.tv_bill_item_subtext);
                TextView tvItemQty = itemView.findViewById(R.id.tv_bill_item_qty);
                TextView tvItemRate = itemView.findViewById(R.id.tv_bill_item_rate);
                TextView tvItemAmount = itemView.findViewById(R.id.tv_bill_item_amount);
                TextView tvItemTaxDisc = itemView.findViewById(R.id.tv_bill_item_tax_disc);

                tvItemName.setText(item.product.productName);

                StringBuilder meta = new StringBuilder();
                if (!TextUtils.isEmpty(item.product.sku)) {
                    meta.append("SKU: ").append(item.product.sku);
                }
                if (!TextUtils.isEmpty(item.product.hsnCode)) {
                    if (meta.length() > 0) meta.append(" | ");
                    meta.append("HSN: ").append(item.product.hsnCode);
                }
                if (meta.length() > 0) {
                    tvItemSubtext.setVisibility(View.VISIBLE);
                    tvItemSubtext.setText(meta.toString());
                } else {
                    tvItemSubtext.setVisibility(View.GONE);
                }

                tvItemQty.setText(String.format(Locale.getDefault(), "%d.00", item.quantity));
                tvItemRate.setText(String.format(Locale.getDefault(), "%,.2f", item.sellingPrice));
                tvItemAmount.setText(String.format(Locale.getDefault(), "%,.2f", item.getTaxableAmount()));

                if (item.getDiscountAmount() > 0) {
                    tvItemTaxDisc.setVisibility(View.VISIBLE);
                    tvItemTaxDisc.setText(String.format(Locale.getDefault(), "Disc: ₹ %,.2f", item.getDiscountAmount()));
                } else {
                    tvItemTaxDisc.setVisibility(View.GONE);
                }

                containerBillItems.addView(itemView);
            }
        }

        // 5. Footer Summary
        TextView tvTotalItems = billView.findViewById(R.id.tv_bill_total_items);
        TextView tvTotalQty = billView.findViewById(R.id.tv_bill_total_qty);
        tvTotalItems.setText(String.valueOf(totalItemsCount));
        tvTotalQty.setText(String.format(Locale.getDefault(), "%d.00", totalQtyCount));

        TextView tvSubtotal = billView.findViewById(R.id.tv_bill_subtotal);
        tvSubtotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.subtotalAmount));

        View layoutDiscount = billView.findViewById(R.id.layout_bill_discount);
        TextView tvDiscount = billView.findViewById(R.id.tv_bill_discount);
        if (purchase.discountAmount > 0) {
            layoutDiscount.setVisibility(View.VISIBLE);
            tvDiscount.setText(String.format(Locale.getDefault(), "- ₹ %,.2f", purchase.discountAmount));
        } else {
            layoutDiscount.setVisibility(View.GONE);
        }

        View layoutCgst = billView.findViewById(R.id.layout_bill_cgst);
        TextView tvCgst = billView.findViewById(R.id.tv_bill_cgst);
        if (purchase.cgstAmount > 0) {
            layoutCgst.setVisibility(View.VISIBLE);
            tvCgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.cgstAmount));
        } else {
            layoutCgst.setVisibility(View.GONE);
        }

        View layoutSgst = billView.findViewById(R.id.layout_bill_sgst);
        TextView tvSgst = billView.findViewById(R.id.tv_bill_sgst);
        if (purchase.sgstAmount > 0) {
            layoutSgst.setVisibility(View.VISIBLE);
            tvSgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.sgstAmount));
        } else {
            layoutSgst.setVisibility(View.GONE);
        }

        View layoutIgst = billView.findViewById(R.id.layout_bill_igst);
        TextView tvIgst = billView.findViewById(R.id.tv_bill_igst);
        if (purchase.igstAmount > 0) {
            layoutIgst.setVisibility(View.VISIBLE);
            tvIgst.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.igstAmount));
        } else {
            layoutIgst.setVisibility(View.GONE);
        }

        View layoutOther = billView.findViewById(R.id.layout_bill_other_charges);
        TextView tvOther = billView.findViewById(R.id.tv_bill_other_charges);
        if (purchase.otherCharges > 0) {
            layoutOther.setVisibility(View.VISIBLE);
            tvOther.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.otherCharges));
        } else {
            layoutOther.setVisibility(View.GONE);
        }

        TextView tvGrandTotal = billView.findViewById(R.id.tv_bill_grand_total);
        tvGrandTotal.setText(String.format(Locale.getDefault(), "₹ %,.2f", purchase.totalAmount));

        TextView tvStatus = billView.findViewById(R.id.tv_bill_payment_status);
        String status = !TextUtils.isEmpty(purchase.status) ? purchase.status.toUpperCase(Locale.getDefault()) : "COMPLETED";
        if (tvStatus != null) {
            tvStatus.setText(String.format("[ %s ]", status));
            if ("PENDING".equalsIgnoreCase(status)) {
                tvStatus.setTextColor(ContextCompat.getColor(context, R.color.status_orange));
            }
        }

        int widthPx = BitmapHelper.convertDpToPx(context, 360f);
        return BitmapHelper.createBitmapFromView(billView, widthPx, 0);
    }
}
