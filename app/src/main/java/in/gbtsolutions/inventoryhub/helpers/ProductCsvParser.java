package in.gbtsolutions.inventoryhub.helpers;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Case table:
 * 1. hsn missing/different AND sku missing/different  -> different products (no error)
 * 2. hsn same, sku different (both present)            -> ERROR, row skipped
 * 3. hsn different, sku same (both present)             -> ERROR, row skipped
 * 4. hsn same AND sku same                              -> same product, new batch
 * 5. sku missing on one row, hsn same on both           -> same product, new batch
 * 6. hsn missing on one row, sku same on both           -> same product, new batch
 * 7. (safeguard, not in the original list) sku matches one existing product while hsn
 * matches a DIFFERENT existing product -> ERROR, row skipped
 */
public class ProductCsvParser {
    public static final List<String> EXPECTED_HEADERS = Collections.unmodifiableList(Arrays.asList(
            "product_name",
            "sku",
            "category_name",
            "brand",
            "description",
            "unit_of_measure",
            "unit_price",
            "selling_price",
            "quantity",
            "reorder_level",
            "reorder_quantity",
            "hsn_code",
            "gst_percent",
            "default_markup_percent",
            "status",
            "batch_no",
            "batch_quantity",
            "batch_purchase_price",
            "batch_selling_price",
            "batch_expiry_date"
    ));

    public interface CategoryResolver {
        int getOrCreateCategoryId(String categoryName) throws Exception;
    }

    public static class HeaderValidationResult {
        public final boolean isValid;
        public final List<String> missingHeaders;
        public final List<String> detectedHeaders;
        public final String errorMessage;

        public HeaderValidationResult(boolean isValid,
                                      List<String> missingHeaders,
                                      List<String> detectedHeaders,
                                      String errorMessage) {
            this.isValid = isValid;
            this.missingHeaders = missingHeaders != null ? missingHeaders : Collections.emptyList();
            this.detectedHeaders = detectedHeaders != null ? detectedHeaders : Collections.emptyList();
            this.errorMessage = errorMessage;
        }
    }

    private static final SimpleDateFormat DATE_FMT;

    static {
        DATE_FMT = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        DATE_FMT.setLenient(false);
        DATE_FMT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static final String MANDATORY_SKU = "sku";
    public static final String MANDATORY_HSN = "hsn";
    public static final String MANDATORY_BOTH = "both";

    private final Map<String, Integer> categoryLookup;
    private final CategoryResolver categoryResolver;
    private String mandatoryField = MANDATORY_SKU;

    public ProductCsvParser(@Nullable CategoryResolver categoryResolver) {
        this.categoryLookup = null;
        this.categoryResolver = categoryResolver;
    }

    public ProductCsvParser(@Nullable Map<String, Integer> categoryLookupCaseInsensitive) {
        this.categoryLookup = categoryLookupCaseInsensitive;
        this.categoryResolver = null;
    }

    public String getMandatoryField() {
        return mandatoryField;
    }

    public void setMandatoryField(@Nullable String mandatoryField) {
        if (mandatoryField == null || mandatoryField.trim().isEmpty()) {
            this.mandatoryField = MANDATORY_SKU;
        } else {
            this.mandatoryField = mandatoryField.trim().toLowerCase(Locale.ROOT);
        }
    }

    public static HeaderValidationResult validateHeaders(@NonNull InputStream inputStream) throws IOException {
        try (com.opencsv.CSVReader reader = new com.opencsv.CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] headerRow = reader.readNext();
            if (headerRow == null || headerRow.length == 0) {
                return new HeaderValidationResult(false, EXPECTED_HEADERS, Collections.emptyList(), "CSV file is empty or missing a header row.");
            }

            List<String> detected = new ArrayList<>();
            Set<String> normalizedDetected = new HashSet<>();
            for (String h : headerRow) {
                if (h != null) {
                    String clean = h.replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT).replace(" ", "_");
                    if (!clean.isEmpty()) {
                        detected.add(clean);
                        normalizedDetected.add(clean);
                    }
                }
            }

            List<String> missing = new ArrayList<>();
            for (String expected : EXPECTED_HEADERS) {
                if (!normalizedDetected.contains(expected)) {
                    missing.add(expected);
                }
            }

            if (!missing.isEmpty()) {
                String errorMsg = "Missing required CSV headers (" + missing.size() + "): " + String.join(", ", missing);
                return new HeaderValidationResult(false, missing, detected, errorMsg);
            }

            return new HeaderValidationResult(true, Collections.emptyList(), detected, null);
        } catch (CsvValidationException e) {
            return new HeaderValidationResult(false, EXPECTED_HEADERS, Collections.emptyList(), "Malformed CSV header: " + e.getMessage());
        }
    }

    private static double parseDouble(String raw, String field, int line) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            throw new NumberFormatException(field + " is not a valid number on line " + line + ": '" + raw + "'");
        }
    }

    private static int parseInt(String raw, String field, int line) {
        try {
            return (int) Double.parseDouble(raw.trim()); // tolerates "50.0"
        } catch (Exception e) {
            throw new NumberFormatException(field + " is not a valid integer on line " + line + ": '" + raw + "'");
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String emptyToNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    private static String trimOrNull(String s) {
        return isBlank(s) ? null : s.trim();
    }

    public ParseResult parse(@NonNull InputStream inputStream) throws IOException, CsvValidationException {
        ParseResult result = new ParseResult();

        try (com.opencsv.CSVReader reader = new com.opencsv.CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null || headers.length == 0) return result;

            Map<String, ProductGroup> skuIndex = new HashMap<>();
            Map<String, ProductGroup> hsnIndex = new HashMap<>();
            List<ProductGroup> orderedGroups = new ArrayList<>();

            String[] rawValues;
            int lineNumber = 1;
            while ((rawValues = reader.readNext()) != null) {
                lineNumber++;
                boolean allEmpty = true;
                for (String v : rawValues) {
                    if (v != null && !v.trim().isEmpty()) {
                        allEmpty = false;
                        break;
                    }
                }
                if (allEmpty) continue;

                Map<String, String> rawRow = new HashMap<>();
                for (int i = 0; i < headers.length; i++) {
                    String val = (i < rawValues.length && rawValues[i] != null) ? rawValues[i] : "";
                    rawRow.put(headers[i], val);
                }

                Row row = toRow(rawRow);

                String rowError = validateRequired(row);
                if (rowError != null) {
                    result.errors.add(new RowError(lineNumber, row.sku, row.hsnCode, rowError));
                    continue;
                }

                String sku = emptyToNull(row.sku);
                String hsn = emptyToNull(row.hsnCode);

                ProductGroup groupBySku = (sku != null) ? skuIndex.get(sku) : null;
                ProductGroup groupByHsn = (hsn != null) ? hsnIndex.get(hsn) : null;

                ProductGroup target;

                if (groupBySku != null && groupByHsn != null) {
                    if (groupBySku == groupByHsn) {
                        target = groupBySku;
                    } else {
                        result.errors.add(new RowError(lineNumber, row.sku, row.hsnCode, "SKU and HSN belong to two different existing products (conflicting identity)."));
                        continue;
                    }
                } else if (groupBySku != null) {
                    target = groupBySku;
                    if (hsn != null) {
                        if (target.establishedHsn != null && !target.establishedHsn.equalsIgnoreCase(hsn)) {
                            result.errors.add(new RowError(lineNumber, row.sku, row.hsnCode, "HSN code differs from the HSN already recorded for SKU '" + sku + "'."));
                            continue;
                        }
                        if (target.establishedHsn == null) {
                            target.establishedHsn = hsn;
                            hsnIndex.put(hsn, target);
                        }
                    }
                } else if (groupByHsn != null) {
                    target = groupByHsn;
                    if (sku != null) {
                        if (target.establishedSku != null && !target.establishedSku.equalsIgnoreCase(sku)) {
                            result.errors.add(new RowError(lineNumber, row.sku, row.hsnCode, "SKU differs from the SKU already recorded for HSN '" + hsn + "'."));
                            continue;
                        }
                        if (target.establishedSku == null) {
                            target.establishedSku = sku;
                            skuIndex.put(sku, target);
                        }
                    }
                } else {
                    target = new ProductGroup();
                    target.establishedSku = sku;
                    target.establishedHsn = hsn;
                    if (sku != null) skuIndex.put(sku, target);
                    if (hsn != null) hsnIndex.put(hsn, target);
                    orderedGroups.add(target);
                }

                target.sourceLines.add(lineNumber);

                if (target.product == null) {
                    Product p = buildProduct(row, lineNumber, result);
                    if (p == null) continue; // buildProduct already recorded the error
                    target.product = p;
                }

                String batchNo = emptyToNull(row.batchNo);
                if (batchNo != null) {
                    ProductBatch batch = buildBatch(row, lineNumber, result);
                    if (batch != null) target.batches.add(batch);
                }
            }

            for (ProductGroup g : orderedGroups) {
                if (g.product == null) continue;
                if (!g.batches.isEmpty()) {
                    int total = 0;
                    for (ProductBatch b : g.batches) total += b.quantity;
                    g.product.quantity = total;
                    g.product.batchEnabled = true;
                }
                result.groups.add(g);
            }
        }

        return result;
    }

    private static String getCaseInsensitive(Map<String, String> m, String key) {
        if (m == null || m.isEmpty()) return null;
        String val = m.get(key);
        if (val != null) return val;
        String normalizedTarget = key.trim().toLowerCase(Locale.ROOT).replace(" ", "_");
        for (Map.Entry<String, String> entry : m.entrySet()) {
            if (entry.getKey() != null) {
                String clean = entry.getKey().replace("\uFEFF", "").trim().toLowerCase(Locale.ROOT).replace(" ", "_");
                if (clean.equals(normalizedTarget)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private Row toRow(Map<String, String> m) {
        Row r = new Row();
        r.productName = getCaseInsensitive(m, "product_name");
        r.sku = getCaseInsensitive(m, "sku");
        r.categoryName = getCaseInsensitive(m, "category_name");
        r.brand = getCaseInsensitive(m, "brand");
        r.description = getCaseInsensitive(m, "description");
        r.unitOfMeasure = getCaseInsensitive(m, "unit_of_measure");
        r.unitPriceRaw = getCaseInsensitive(m, "unit_price");
        r.sellingPriceRaw = getCaseInsensitive(m, "selling_price");
        r.quantityRaw = getCaseInsensitive(m, "quantity");
        r.reorderLevelRaw = getCaseInsensitive(m, "reorder_level");
        r.reorderQtyRaw = getCaseInsensitive(m, "reorder_quantity");
        r.hsnCode = getCaseInsensitive(m, "hsn_code");
        r.gstRaw = getCaseInsensitive(m, "gst_percent");
        r.markupRaw = getCaseInsensitive(m, "default_markup_percent");
        r.status = getCaseInsensitive(m, "status");
        r.batchNo = getCaseInsensitive(m, "batch_no");
        r.batchQtyRaw = getCaseInsensitive(m, "batch_quantity");
        r.batchPurchaseRaw = getCaseInsensitive(m, "batch_purchase_price");
        r.batchSellingRaw = getCaseInsensitive(m, "batch_selling_price");
        r.batchExpiryRaw = getCaseInsensitive(m, "batch_expiry_date");
        return r;
    }

    private String validateRequired(Row r) {
        if (isBlank(r.productName)) return "product_name is required.";
        if (isBlank(r.categoryName)) return "category_name is required.";
        if (isBlank(r.unitPriceRaw)) return "unit_price is required.";
        if (isBlank(r.sellingPriceRaw)) return "selling_price is required.";
        if (!isBlank(r.batchNo) && isBlank(r.batchQtyRaw))
            return "batch_quantity is required when batch_no is set.";

        boolean skuBlank = isBlank(r.sku);
        boolean hsnBlank = isBlank(r.hsnCode);

        // If both HSN and SKU are absent, the row is always skipped
        if (skuBlank && hsnBlank) {
            return "Both SKU and HSN are absent. At least one must be provided.";
        }

        if (MANDATORY_HSN.equalsIgnoreCase(mandatoryField)) {
            if (hsnBlank) {
                return "HSN code is mandatory but missing.";
            }
        } else if (MANDATORY_BOTH.equalsIgnoreCase(mandatoryField)) {
            if (skuBlank && hsnBlank) {
                return "Both SKU and HSN are absent.";
            } else if (skuBlank) {
                return "SKU is mandatory but missing.";
            } else if (hsnBlank) {
                return "HSN code is mandatory but missing.";
            }
        } else {
            // Default: SKU is mandatory
            if (skuBlank) {
                return "SKU is mandatory but missing.";
            }
        }

        return null;
    }

    private Product buildProduct(Row row, int lineNumber, ParseResult result) {
        try {
            Product p = new Product();
            p.productName = row.productName.trim();
            p.sku = trimOrNull(row.sku);
            p.brand = nullToEmpty(row.brand);
            p.description = nullToEmpty(row.description);
            p.unitOfMeasure = nullToEmpty(row.unitOfMeasure);
            p.hsnCode = trimOrNull(row.hsnCode);
            p.status = isBlank(row.status) ? "active" : row.status.trim();
            p.unitPrice = parseDouble(row.unitPriceRaw, "unit_price", lineNumber);
            p.sellingPrice = parseDouble(row.sellingPriceRaw, "selling_price", lineNumber);
            p.quantity = isBlank(row.quantityRaw) ? 0 : parseInt(row.quantityRaw, "quantity", lineNumber);
            p.reorderLevel = isBlank(row.reorderLevelRaw) ? 0 : parseInt(row.reorderLevelRaw, "reorder_level", lineNumber);
            p.reorderQuantity = isBlank(row.reorderQtyRaw) ? 0 : parseInt(row.reorderQtyRaw, "reorder_quantity", lineNumber);
            p.gstPercent = isBlank(row.gstRaw) ? 0 : parseDouble(row.gstRaw, "gst_percent", lineNumber);
            p.defaultMarkupPercent = isBlank(row.markupRaw) ? 0 : parseDouble(row.markupRaw, "default_markup_percent", lineNumber);

            if (categoryResolver != null) {
                try {
                    int catId = categoryResolver.getOrCreateCategoryId(row.categoryName.trim());
                    if (catId <= 0) {
                        result.errors.add(new RowError(lineNumber, row.sku, row.hsnCode, "Unable to resolve or create category: '" + row.categoryName + "'."));
                        return null;
                    }
                    p.categoryId = catId;
                } catch (Exception e) {
                    result.errors.add(new RowError(lineNumber, row.sku, row.hsnCode, "Error creating category '" + row.categoryName + "': " + e.getMessage()));
                    return null;
                }
            } else if (categoryLookup != null) {
                Integer catId = categoryLookup.get(row.categoryName.trim().toLowerCase(Locale.ROOT));
                if (catId == null) {
                    result.errors.add(new RowError(lineNumber, row.sku, row.hsnCode, "Unknown category_name: '" + row.categoryName + "'."));
                    return null;
                }
                p.categoryId = catId;
            }

            long now = System.currentTimeMillis();
            p.createdAt = now;
            p.updatedAt = now;
            return p;
        } catch (NumberFormatException nfe) {
            result.errors.add(new RowError(lineNumber, row.sku, row.hsnCode, nfe.getMessage()));
            return null;
        }
    }

    private ProductBatch buildBatch(Row row, int lineNumber, ParseResult result) {
        try {
            ProductBatch b = new ProductBatch();
            b.batchNo = row.batchNo.trim();
            b.quantity = parseInt(row.batchQtyRaw, "batch_quantity", lineNumber);
            b.purchasePrice = isBlank(row.batchPurchaseRaw) ? 0 : parseDouble(row.batchPurchaseRaw, "batch_purchase_price", lineNumber);
            b.sellingPrice = isBlank(row.batchSellingRaw) ? 0 : parseDouble(row.batchSellingRaw, "batch_selling_price", lineNumber);

            if (isBlank(row.batchExpiryRaw)) {
                b.expiryDate = 0L;
            } else {
                try {
                    b.expiryDate = DATE_FMT.parse(row.batchExpiryRaw.trim()).getTime();
                } catch (ParseException pe) {
                    result.errors.add(new RowError(lineNumber, row.sku, row.hsnCode, "batch_expiry_date must be YYYY-MM-DD, got '" + row.batchExpiryRaw + "'."));
                    return null;
                }
            }

            long now = System.currentTimeMillis();
            b.createdAt = now;
            b.updatedAt = now;
            return b;
        } catch (NumberFormatException nfe) {
            result.errors.add(new RowError(lineNumber, row.sku, row.hsnCode, nfe.getMessage()));
            return null;
        }
    }

    public static class ProductGroup {
        public final List<ProductBatch> batches = new ArrayList<>();
        public final List<Integer> sourceLines = new ArrayList<>();
        public Product product;
        String establishedSku;
        String establishedHsn;
    }

    public static class RowError {
        public final int lineNumber;
        public final String sku;
        public final String hsnCode;
        public final String reason;

        RowError(int lineNumber, String sku, String hsnCode, String reason) {
            this.lineNumber = lineNumber;
            this.sku = sku;
            this.hsnCode = hsnCode;
            this.reason = reason;
        }

        @NonNull
        @Override
        public String toString() {
            return "Line " + lineNumber + " [sku=" + sku + ", hsn=" + hsnCode + "]: " + reason;
        }
    }

    public static class ParseResult {
        public final List<ProductGroup> groups = new ArrayList<>();
        public final List<RowError> errors = new ArrayList<>();

        public int totalProducts() {
            return groups.size();
        }

        public int totalBatches() {
            int c = 0;
            for (ProductGroup g : groups) c += g.batches.size();
            return c;
        }

        public String summary() {
            return totalProducts() + " products, " + totalBatches() + " batches inserted, " + errors.size() + " row(s) skipped.";
        }
    }

    private static class Row {
        String productName, sku, categoryName, brand, description, unitOfMeasure, hsnCode, status, batchNo;
        String unitPriceRaw, sellingPriceRaw, quantityRaw, reorderLevelRaw, reorderQtyRaw, gstRaw, markupRaw, batchQtyRaw, batchPurchaseRaw, batchSellingRaw, batchExpiryRaw;
    }
}