package in.gbtsolutions.inventoryhub.helpers;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.InventoryHubApp;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.online.models.OnlineQrLookupRequest;
import in.gbtsolutions.inventoryhub.online.repository.OnlineProductRepository;

public class ProductQRHelper {

    private static final int DEFAULT_QR_SIZE = 128;

    public interface OnProductScannedCallback {
        void onProductLoaded(Product product);

        void onError(String message);
    }

    // Payload format (27 characters):
    // - 6 digits: Product ID (padded to 6 digits, e.g. %06d)
    // - 6 digits: Integer price (padded to 6 digits, e.g. %06d)
    // - 2 digits: Decimal price (2 digits, e.g. %02d)
    // - 6 digits: Batch ID (padded to 6 digits, e.g. %06d, 000000 if none)
    // - 5 digits: Expiry date in YYDDD format (Year and Julian day of year, 00000 if none)
    // - 2 hex digits: 1-byte XOR Checksum of the first 25 ASCII characters
    // Total = 6 + 6 + 2 + 6 + 5 + 2 = 27 characters.

    public static String build27CharPayload(Product product) {
        if (product == null) {
            return null;
        }

        int id = Math.max(0, product.productId) % 1000000;
        String idStr = String.format(Locale.US, "%06d", id);

        double price = Math.max(0.0, product.sellingPrice);
        long priceInPaise = Math.round(price * 100.0) % 100000000L;
        long intPrice = priceInPaise / 100;
        long decPrice = priceInPaise % 100;
        String priceStr = String.format(Locale.US, "%06d%02d", intPrice, decPrice);

        ProductBatch batch = product.getBatch();
        int batchId = (batch != null && batch.batchId > 0) ? (batch.batchId % 1000000) : 0;
        String batchIdStr = String.format(Locale.US, "%06d", batchId);

        String expDateStr = "00000";
        if (batch != null && batch.expiryDate > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(batch.expiryDate);
            int year = cal.get(Calendar.YEAR) % 100;
            int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);
            expDateStr = String.format(Locale.US, "%02d%03d", year, dayOfYear);
        }

        String mainData = idStr + priceStr + batchIdStr + expDateStr;

        byte checksum = EncryptionHelper.xorChecksum(mainData.getBytes(StandardCharsets.UTF_8));
        String checksumHex = String.format(Locale.US, "%02X", checksum & 0xFF);

        return (mainData + checksumHex).toUpperCase();
    }

    public static String build32HexPayload(Product product) {
        return build27CharPayload(product);
    }

    public static Bitmap generateProductQR(Product product) throws Exception {
        return generateProductQR(product, DEFAULT_QR_SIZE);
    }

    public static Bitmap generateProductQR(Product product, int size) throws Exception {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        String payload = build27CharPayload(product);
        String encryptedBase64 = EncryptionHelper.encrypt(payload);
        return QRCodeHelper.createQRCode(encryptedBase64, size);
    }

    public static final int BARCODE_ID_LENGTH = 6;
    public static final int BASE_36_RADIX = 36;

    /**
     * Encodes a positive number into Base-36 (0-9, a-z) padded with '0' to the specified length.
     * With length 6, Base-36 can encode values up to 36^6 - 1 = 2,176,782,335, covering
     * the entire positive range of 32-bit signed integers.
     */
    public static String encodeBase36(long value, int length) {
        String str = Long.toString(Math.max(0, value), BASE_36_RADIX).toLowerCase(Locale.US);
        if (str.length() >= length) {
            return str;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = str.length(); i < length; i++) {
            sb.append('0');
        }
        sb.append(str);
        return sb.toString();
    }

    /**
     * Decodes a Base-36 string (0-9, a-z / A-Z) to a long.
     */
    public static long decodeBase36(String str) {
        if (str == null || str.trim().isEmpty()) {
            return 0;
        }
        return Long.parseLong(str.trim(), BASE_36_RADIX);
    }

    public static String buildBarcodePayload(Product product) {
        if (product == null) {
            return "";
        }

        // Fixed-length 6-character Base-36 string (0-9, a-z)
        // Completely independent of the non-mandatory SKU field.
        int id = Math.max(1, product.productId);
        String prodCode = encodeBase36(id, BARCODE_ID_LENGTH);

        ProductBatch batch = product.getBatch();
        if (batch != null && batch.batchId > 0) {
            return prodCode + "-B" + batch.batchId;
        }

        return prodCode;
    }

    public static Bitmap generateProductBarcode(Product product) throws Exception {
        return generateProductBarcode(product, 600, 180);
    }

    public static Bitmap generateProductBarcode(Product product, int width, int height) throws Exception {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        String payload = buildBarcodePayload(product);
        return BarcodeHelper.createBarcode(payload, width, height);
    }

    public static Product parseScannedQR(String scannedData) {
        return parseScannedQR(InventoryHubApp.getInstance(), scannedData);
    }

    public static Product parseScannedQR(Context context, String scannedData) {
        if (scannedData == null || scannedData.trim().isEmpty()) {
            return null;
        }

        String rawScanned = scannedData.trim();
        Context appContext = context != null ? context.getApplicationContext() : InventoryHubApp.getInstance();

        // 1. Try decrypting encoded QR/Barcode payload
        try {
            String decrypted = EncryptionHelper.decrypt(rawScanned);
            if (decrypted != null) {
                int productId;
                double sellingPrice;
                int batchId = 0;
                String expDateYYDDD = "00000";

                if (decrypted.length() == 27) {
                    productId = Integer.parseInt(decrypted.substring(0, 6));
                    long intPrice = Long.parseLong(decrypted.substring(6, 12));
                    int decPrice = Integer.parseInt(decrypted.substring(12, 14));
                    sellingPrice = intPrice + (decPrice / 100.0);
                    batchId = Integer.parseInt(decrypted.substring(14, 20));
                    expDateYYDDD = decrypted.substring(20, 25);
                } else if (decrypted.length() == 32) {
                    // Legacy 32-character payload compatibility
                    productId = Integer.parseInt(decrypted.substring(0, 6));
                    long intPrice = Long.parseLong(decrypted.substring(6, 12));
                    int decPrice = Integer.parseInt(decrypted.substring(12, 14));
                    sellingPrice = intPrice + (decPrice / 100.0);
                } else {
                    return null;
                }

                Product product = null;

                if (appContext != null) {
                    final int finalBatchId = batchId;
                    final String finalExpDate = expDateYYDDD;
                    boolean isOnline = AppModeManager.getInstance(appContext).isOnlineMode();

                    if (isOnline) {
                        final Product[] holder = new Product[1];
                        final CountDownLatch latch = new CountDownLatch(1);
                        OnlineQrLookupRequest req = new OnlineQrLookupRequest(decrypted, productId, batchId, sellingPrice, expDateYYDDD);
                        new OnlineProductRepository(appContext).lookupProductByQr(req, new OnlineProductRepository.ProductActionCallback() {
                            @Override
                            public void onSuccess(Product p) {
                                holder[0] = p;
                                latch.countDown();
                            }

                            @Override
                            public void onError(String errorMessage) {
                                latch.countDown();
                            }
                        });
                        try {
                            latch.await(4, TimeUnit.SECONDS);
                        } catch (InterruptedException ignored) {
                        }
                        product = holder[0];
                    } else {
                        if (Looper.myLooper() == Looper.getMainLooper()) {
                            Future<Product> future = Executors.newSingleThreadExecutor().submit(() -> {
                                return loadProductWithBatch(appContext, productId, finalBatchId, finalExpDate);
                            });
                            product = future.get(3, TimeUnit.SECONDS);
                        } else {
                            product = loadProductWithBatch(appContext, productId, finalBatchId, finalExpDate);
                        }
                    }
                }

                if (product != null && sellingPrice > 0) {
                    product.sellingPrice = sellingPrice;
                }

                if (product != null) {
                    return product;
                }
            }
        } catch (Exception ignored) {
        }

        // 2. Fallback: try raw 1D barcode matching (SKU, HSN, Product ID)
        if (appContext != null) {
            try {
                if (Looper.myLooper() == Looper.getMainLooper()) {
                    Future<Product> future = Executors.newSingleThreadExecutor().submit(() -> {
                        return lookupProductByRawCode(appContext, rawScanned);
                    });
                    return future.get(3, TimeUnit.SECONDS);
                } else {
                    return lookupProductByRawCode(appContext, rawScanned);
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    public static Product lookupProductByRawCode(Context context, String rawCode) {
        if (context == null || rawCode == null || rawCode.trim().isEmpty()) {
            return null;
        }
        Database db = Database.getInstance(context);

        String code = rawCode.trim();
        String upper = code.toUpperCase(Locale.US);

        int parsedProductId = -1;
        int parsedBatchId = 0;

        // Clean legacy prefixes like "PROD-", "PROD", "P-" if present
        String cleanCode = upper;
        if (cleanCode.startsWith("PROD-")) {
            cleanCode = cleanCode.substring(5).trim();
        } else if (cleanCode.startsWith("PROD")) {
            cleanCode = cleanCode.substring(4).trim();
        } else if (cleanCode.startsWith("P-")) {
            cleanCode = cleanCode.substring(2).trim();
        } else if (cleanCode.startsWith("P") && cleanCode.length() > 1 && Character.isDigit(cleanCode.charAt(1))) {
            cleanCode = cleanCode.substring(1).trim();
        }

        // 1. Check for fixed-length 6-character Base-36 Product ID (with optional batch next to it)
        // Example: "00002x", "00002x-B15", "00002xB15", "00002x-15"
        if (cleanCode.length() >= BARCODE_ID_LENGTH) {
            String idCandidate = cleanCode.substring(0, BARCODE_ID_LENGTH);
            try {
                long val = Long.parseLong(idCandidate, BASE_36_RADIX);
                if (val > 0 && val <= Integer.MAX_VALUE) {
                    int candidateId = (int) val;
                    int candidateBatchId = 0;

                    if (cleanCode.length() > BARCODE_ID_LENGTH) {
                        String batchRemainder = cleanCode.substring(BARCODE_ID_LENGTH).trim();
                        if (batchRemainder.startsWith("-B") || batchRemainder.startsWith("-b")) {
                            batchRemainder = batchRemainder.substring(2);
                        } else if (batchRemainder.startsWith("B") || batchRemainder.startsWith("b")
                                || batchRemainder.startsWith("-") || batchRemainder.startsWith(":")) {
                            batchRemainder = batchRemainder.substring(1);
                        }
                        batchRemainder = batchRemainder.trim();
                        if (!batchRemainder.isEmpty()) {
                            try {
                                candidateBatchId = Integer.parseInt(batchRemainder);
                            } catch (NumberFormatException e) {
                                try {
                                    candidateBatchId = (int) Long.parseLong(batchRemainder, BASE_36_RADIX);
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }

                    // Look up product in database using decoded Base-36 ID
                    Product p = db.productDao().getProductById(candidateId);
                    if (p != null) {
                        if (candidateBatchId > 0) {
                            ProductBatch batch = db.productBatchDao().getBatchById(candidateBatchId);
                            if (batch != null) {
                                p.setBatch(batch);
                                if (batch.sellingPrice > 0) {
                                    p.sellingPrice = batch.sellingPrice;
                                }
                            }
                        }
                        return p;
                    }
                }
            } catch (Exception ignored) {}
        }

        // 2. Fallback: check for standard decimal format, e.g. "105", "105-B15", "000105"
        if (cleanCode.contains("-B") || cleanCode.contains("-b")) {
            int bIdx = cleanCode.indexOf("-B");
            if (bIdx < 0) bIdx = cleanCode.indexOf("-b");
            String prodPart = cleanCode.substring(0, bIdx).trim();
            String batchPart = cleanCode.substring(bIdx + 2).trim();
            try {
                parsedProductId = Integer.parseInt(prodPart);
                parsedBatchId = Integer.parseInt(batchPart);
            } catch (NumberFormatException ignored) {}
        } else if (cleanCode.contains(":") || cleanCode.contains("#")) {
            int sepIdx = cleanCode.indexOf(':');
            if (sepIdx < 0) sepIdx = cleanCode.indexOf('#');
            String prodPart = cleanCode.substring(0, sepIdx).trim();
            String batchPart = cleanCode.substring(sepIdx + 1).trim();
            try {
                parsedProductId = Integer.parseInt(prodPart);
                parsedBatchId = Integer.parseInt(batchPart);
            } catch (NumberFormatException ignored) {}
        } else {
            try {
                parsedProductId = Integer.parseInt(cleanCode);
            } catch (NumberFormatException ignored) {}
        }

        // 3. If Product ID was parsed in decimal, look up directly from database
        if (parsedProductId > 0) {
            Product p = db.productDao().getProductById(parsedProductId);
            if (p != null) {
                if (parsedBatchId > 0) {
                    ProductBatch batch = db.productBatchDao().getBatchById(parsedBatchId);
                    if (batch != null) {
                        p.setBatch(batch);
                        if (batch.sellingPrice > 0) {
                            p.sellingPrice = batch.sellingPrice;
                        }
                    }
                }
                return p;
            }
        }

        // 4. Fallback for external/manufacturer barcodes: try SKU match
        Product p = db.productDao().getProductBySku(code);
        if (p != null) return p;

        // 5. Fallback for external/manufacturer barcodes: try HSN match
        p = db.productDao().getProductByHsn(code);
        if (p != null) return p;

        return null;
    }

    private static Product loadProductWithBatch(Context context, int productId, int batchId, String expDateYYDDD) {
        Database db = Database.getInstance(context);
        Product prod = db.productDao().getProductById(productId);
        if (prod != null && batchId > 0) {
            ProductBatch batch = db.productBatchDao().getBatchById(batchId);
            if (batch != null) {
                prod.setBatch(batch);
            } else {
                // Fallback batch if not present locally
                ProductBatch fallback = new ProductBatch();
                fallback.batchId = batchId;
                fallback.productId = productId;
                fallback.batchNo = "BATCH-" + batchId;
                fallback.expiryDate = parseJulianExpiry(expDateYYDDD);
                prod.setBatch(fallback);
            }
        }
        return prod;
    }

    public static long parseJulianExpiry(String expDateYYDDD) {
        if (expDateYYDDD == null || expDateYYDDD.equals("00000") || expDateYYDDD.length() != 5) {
            return 0L;
        }
        try {
            int yy = Integer.parseInt(expDateYYDDD.substring(0, 2));
            int ddd = Integer.parseInt(expDateYYDDD.substring(2, 5));
            Calendar cal = Calendar.getInstance();
            cal.clear();
            cal.set(Calendar.YEAR, 2000 + yy);
            cal.set(Calendar.DAY_OF_YEAR, ddd);
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return 0L;
        }
    }

    public static void parseScannedQRAsync(Context context, String scannedData, OnProductScannedCallback callback) {
        if (scannedData == null || scannedData.trim().isEmpty()) {
            if (callback != null) callback.onError("Empty QR code");
            return;
        }

        Context appContext = context != null ? context.getApplicationContext() : InventoryHubApp.getInstance();
        boolean isOnline = appContext != null && AppModeManager.getInstance(appContext).isOnlineMode();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String raw = scannedData.trim();
                String decrypted = null;
                try {
                    decrypted = EncryptionHelper.decrypt(raw);
                } catch (Exception ignored) {
                }

                if (decrypted == null) {
                    Product rawProduct = lookupProductByRawCode(appContext, raw);
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (callback != null) {
                            if (rawProduct != null) {
                                callback.onProductLoaded(rawProduct);
                            } else {
                                callback.onError("Product not found for code: " + raw);
                            }
                        }
                    });
                    return;
                }

                int productId;
                double sellingPrice;
                int batchId = 0;
                String expDateYYDDD = "00000";

                if (decrypted.length() == 27) {
                    productId = Integer.parseInt(decrypted.substring(0, 6));
                    long intPrice = Long.parseLong(decrypted.substring(6, 12));
                    int decPrice = Integer.parseInt(decrypted.substring(12, 14));
                    sellingPrice = intPrice + (decPrice / 100.0);
                    batchId = Integer.parseInt(decrypted.substring(14, 20));
                    expDateYYDDD = decrypted.substring(20, 25);
                } else if (decrypted.length() == 32) {
                    productId = Integer.parseInt(decrypted.substring(0, 6));
                    long intPrice = Long.parseLong(decrypted.substring(6, 12));
                    int decPrice = Integer.parseInt(decrypted.substring(12, 14));
                    sellingPrice = intPrice + (decPrice / 100.0);
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (callback != null) callback.onError("Invalid QR code format");
                    });
                    return;
                }

                if (isOnline) {
                    OnlineQrLookupRequest req = new OnlineQrLookupRequest(decrypted, productId, batchId, sellingPrice, expDateYYDDD);
                    new OnlineProductRepository(appContext).lookupProductByQr(req, new OnlineProductRepository.ProductActionCallback() {
                        @Override
                        public void onSuccess(Product product) {
                            if (product != null && sellingPrice > 0) {
                                product.sellingPrice = sellingPrice;
                            }
                            if (callback != null) callback.onProductLoaded(product);
                        }

                        @Override
                        public void onError(String errorMessage) {
                            if (callback != null) callback.onError(errorMessage);
                        }
                    });
                } else {
                    Product product = loadProductWithBatch(appContext, productId, batchId, expDateYYDDD);
                    if (product != null && sellingPrice > 0) {
                        product.sellingPrice = sellingPrice;
                    }
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (callback != null) {
                            if (product != null) {
                                callback.onProductLoaded(product);
                            } else {
                                callback.onError("Product not found in local database");
                            }
                        }
                    });
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null)
                        callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to scan QR");
                });
            }
        });
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
