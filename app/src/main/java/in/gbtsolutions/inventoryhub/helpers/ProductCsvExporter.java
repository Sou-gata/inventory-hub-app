package in.gbtsolutions.inventoryhub.helpers;

import android.database.Cursor;

import androidx.annotation.NonNull;

import com.opencsv.CSVWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ProductCsvExporter {

    public static final List<String> HEADERS =
            Collections.unmodifiableList(
                    Arrays.asList(
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
                    )
            );

    private static final SimpleDateFormat DATE_FORMAT;

    static {
        DATE_FORMAT = new SimpleDateFormat(
                "yyyy-MM-dd",
                Locale.US
        );

        DATE_FORMAT.setLenient(false);
        DATE_FORMAT.setTimeZone(
                TimeZone.getTimeZone("UTC")
        );
    }

    private ProductCsvExporter() {
    }

    public static void export(
            @NonNull Cursor cursor,
            @NonNull OutputStream outputStream
    ) throws IOException {

        try (
                OutputStreamWriter outputStreamWriter =
                        new OutputStreamWriter(
                                outputStream,
                                StandardCharsets.UTF_8
                        );

                CSVWriter writer =
                        new CSVWriter(outputStreamWriter)
        ) {

            // Write CSV header
            writer.writeNext(
                    HEADERS.toArray(new String[0]),
                    false
            );

            int productNameIndex =
                    cursor.getColumnIndexOrThrow("product_name");

            int skuIndex =
                    cursor.getColumnIndexOrThrow("sku");

            int categoryNameIndex =
                    cursor.getColumnIndexOrThrow("category_name");

            int brandIndex =
                    cursor.getColumnIndexOrThrow("brand");

            int descriptionIndex =
                    cursor.getColumnIndexOrThrow("description");

            int unitOfMeasureIndex =
                    cursor.getColumnIndexOrThrow("unit_of_measure");

            int unitPriceIndex =
                    cursor.getColumnIndexOrThrow("unit_price");

            int sellingPriceIndex =
                    cursor.getColumnIndexOrThrow("selling_price");

            int quantityIndex =
                    cursor.getColumnIndexOrThrow("quantity");

            int reorderLevelIndex =
                    cursor.getColumnIndexOrThrow("reorder_level");

            int reorderQuantityIndex =
                    cursor.getColumnIndexOrThrow("reorder_quantity");

            int hsnCodeIndex =
                    cursor.getColumnIndexOrThrow("hsn_code");

            int gstPercentIndex =
                    cursor.getColumnIndexOrThrow("gst_percent");

            int markupPercentIndex =
                    cursor.getColumnIndexOrThrow(
                            "default_markup_percent"
                    );

            int statusIndex =
                    cursor.getColumnIndexOrThrow("status");

            int batchNoIndex =
                    cursor.getColumnIndexOrThrow("batch_no");

            int batchQuantityIndex =
                    cursor.getColumnIndexOrThrow("batch_quantity");

            int batchPurchasePriceIndex =
                    cursor.getColumnIndexOrThrow(
                            "batch_purchase_price"
                    );

            int batchSellingPriceIndex =
                    cursor.getColumnIndexOrThrow(
                            "batch_selling_price"
                    );

            int batchExpiryDateIndex =
                    cursor.getColumnIndexOrThrow(
                            "batch_expiry_date"
                    );

            while (cursor.moveToNext()) {

                String productName =
                        getString(cursor, productNameIndex);

                String sku =
                        getString(cursor, skuIndex);

                String categoryName =
                        getString(cursor, categoryNameIndex);

                String brand =
                        getString(cursor, brandIndex);

                String description =
                        getString(cursor, descriptionIndex);

                String unitOfMeasure =
                        getString(cursor, unitOfMeasureIndex);

                String unitPrice =
                        getDouble(cursor, unitPriceIndex);

                String sellingPrice =
                        getDouble(cursor, sellingPriceIndex);

                String quantity =
                        getInt(cursor, quantityIndex);

                String reorderLevel =
                        getInt(cursor, reorderLevelIndex);

                String reorderQuantity =
                        getInt(cursor, reorderQuantityIndex);

                String hsnCode =
                        getString(cursor, hsnCodeIndex);

                String gstPercent =
                        getDouble(cursor, gstPercentIndex);

                String markupPercent =
                        getDouble(cursor, markupPercentIndex);

                String status =
                        getString(cursor, statusIndex);

                String batchNo =
                        getString(cursor, batchNoIndex);

                String batchQuantity =
                        getIntNullable(cursor, batchQuantityIndex);

                String batchPurchasePrice =
                        getDoubleNullable(
                                cursor,
                                batchPurchasePriceIndex
                        );

                String batchSellingPrice =
                        getDoubleNullable(
                                cursor,
                                batchSellingPriceIndex
                        );

                String batchExpiryDate =
                        getExpiryDate(cursor, batchExpiryDateIndex);

                writer.writeNext(
                        new String[]{
                                productName,
                                sku,
                                categoryName,
                                brand,
                                description,
                                unitOfMeasure,
                                unitPrice,
                                sellingPrice,
                                quantity,
                                reorderLevel,
                                reorderQuantity,
                                hsnCode,
                                gstPercent,
                                markupPercent,
                                status,
                                batchNo,
                                batchQuantity,
                                batchPurchasePrice,
                                batchSellingPrice,
                                batchExpiryDate
                        },
                        false
                );
            }

            writer.flush();
        }
    }

    private static String getString(
            Cursor cursor,
            int index
    ) {
        if (cursor.isNull(index)) {
            return "";
        }

        String value = cursor.getString(index);

        return value == null
                ? ""
                : value.trim();
    }

    private static String getInt(
            Cursor cursor,
            int index
    ) {
        if (cursor.isNull(index)) {
            return "";
        }

        return String.valueOf(
                cursor.getInt(index)
        );
    }

    private static String getIntNullable(
            Cursor cursor,
            int index
    ) {
        if (cursor.isNull(index)) {
            return "";
        }

        return String.valueOf(
                cursor.getInt(index)
        );
    }

    private static String getDouble(
            Cursor cursor,
            int index
    ) {
        if (cursor.isNull(index)) {
            return "";
        }

        return Double.toString(
                cursor.getDouble(index)
        );
    }

    private static String getDoubleNullable(
            Cursor cursor,
            int index
    ) {
        if (cursor.isNull(index)) {
            return "";
        }

        return Double.toString(
                cursor.getDouble(index)
        );
    }

    private static String getExpiryDate(
            Cursor cursor,
            int index
    ) {
        if (cursor.isNull(index)) {
            return "";
        }
        long expiryDate = cursor.getLong(index);
        if (expiryDate <= 0) {
            return "";
        }

        synchronized (DATE_FORMAT) {
            return DATE_FORMAT.format(expiryDate);
        }
    }
}
