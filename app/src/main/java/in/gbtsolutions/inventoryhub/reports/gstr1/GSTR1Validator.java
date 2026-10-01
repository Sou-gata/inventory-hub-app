package in.gbtsolutions.inventoryhub.reports.gstr1;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.helpers.HelperMethods;
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleItemWithProduct;

public class GSTR1Validator {

    public static List<GSTR1Models.ValidationIssue> validateReport(List<Sale> sales, List<SaleItemWithProduct> items) {
        List<GSTR1Models.ValidationIssue> issues = new ArrayList<>();

        if (sales == null) return issues;

        for (Sale sale : sales) {
            if ("Cancelled".equalsIgnoreCase(sale.status)) continue;

            String inv = !TextUtils.isEmpty(sale.invoiceId) ? sale.invoiceId : "Sale #" + sale.saleId;

            // B2B Validation
            if (!TextUtils.isEmpty(sale.customerGstin)) {
                String gstin = sale.customerGstin.trim().toUpperCase();
                if (!HelperMethods.validateGST(gstin)) {
                    issues.add(new GSTR1Models.ValidationIssue(
                            GSTR1Models.ValidationIssue.Severity.ERROR,
                            inv,
                            "Invalid GSTIN Format",
                            "Customer GSTIN '" + gstin + "' does not match standard 15-character GSTIN pattern."
                    ));
                }
            }

            // POS Validation
            if (TextUtils.isEmpty(sale.placeOfSupplyStateCode)) {
                issues.add(new GSTR1Models.ValidationIssue(
                        GSTR1Models.ValidationIssue.Severity.WARNING,
                        inv,
                        "Missing Place of Supply",
                        "Place of supply state code is not specified; defaulted to home state."
                ));
            }
        }

        // HSN Validation
        if (items != null) {
            for (SaleItemWithProduct item : items) {
                if (item.saleItem == null) continue;
                String hsn = item.saleItem.hsnCode != null ? item.saleItem.hsnCode.trim() : "";
                if (item.product != null && TextUtils.isEmpty(hsn)) {
                    hsn = item.product.hsnCode != null ? item.product.hsnCode.trim() : "";
                }

                double rate = item.saleItem.gstRate;
                if (rate == 0.0 && item.product != null) rate = item.product.gstPercent;

                if (rate > 0 && TextUtils.isEmpty(hsn)) {
                    String prodName = item.product != null ? item.product.productName : "Product #" + item.saleItem.productId;
                    issues.add(new GSTR1Models.ValidationIssue(
                            GSTR1Models.ValidationIssue.Severity.WARNING,
                            prodName,
                            "Missing HSN / SAC Code",
                            "Taxable item is being reported without an HSN/SAC code."
                    ));
                }
            }
        }

        return issues;
    }
}
