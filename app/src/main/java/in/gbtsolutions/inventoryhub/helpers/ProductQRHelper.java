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

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.InventoryHubApp;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;

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

    public static Product parseScannedQR(String scannedData) {
        return parseScannedQR(InventoryHubApp.getInstance(), scannedData);
    }

    public static Product parseScannedQR(Context context, String scannedData) {
        if (scannedData == null || scannedData.trim().isEmpty()) {
            return null;
        }

        try {
            String decrypted = EncryptionHelper.decrypt(scannedData.trim());
            if (decrypted == null) {
                return null;
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
                // Legacy 32-character payload compatibility
                productId = Integer.parseInt(decrypted.substring(0, 6));
                long intPrice = Long.parseLong(decrypted.substring(6, 12));
                int decPrice = Integer.parseInt(decrypted.substring(12, 14));
                sellingPrice = intPrice + (decPrice / 100.0);
            } else {
                return null;
            }

            Context appContext = context != null ? context.getApplicationContext() : InventoryHubApp.getInstance();
            Product product = null;

            if (appContext != null) {
                final int finalBatchId = batchId;
                final String finalExpDate = expDateYYDDD;
                boolean isOnline = in.gbtsolutions.inventoryhub.online.config.AppModeManager.getInstance(appContext).isOnlineMode();

                if (isOnline) {
                    final Product[] holder = new Product[1];
                    final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
                    in.gbtsolutions.inventoryhub.online.models.OnlineQrLookupRequest req =
                            new in.gbtsolutions.inventoryhub.online.models.OnlineQrLookupRequest(
                                    decrypted, productId, batchId, sellingPrice, expDateYYDDD);
                    new in.gbtsolutions.inventoryhub.online.repository.OnlineProductRepository(appContext)
                            .lookupProductByQr(req, new in.gbtsolutions.inventoryhub.online.repository.OnlineProductRepository.ProductActionCallback() {
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
                    } catch (InterruptedException ignored) {}
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

            return product;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
        boolean isOnline = appContext != null && in.gbtsolutions.inventoryhub.online.config.AppModeManager.getInstance(appContext).isOnlineMode();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String decrypted = EncryptionHelper.decrypt(scannedData.trim());
                if (decrypted == null) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (callback != null) callback.onError("Invalid QR code encryption");
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
                    in.gbtsolutions.inventoryhub.online.models.OnlineQrLookupRequest req =
                            new in.gbtsolutions.inventoryhub.online.models.OnlineQrLookupRequest(
                                    decrypted, productId, batchId, sellingPrice, expDateYYDDD);
                    new in.gbtsolutions.inventoryhub.online.repository.OnlineProductRepository(appContext)
                            .lookupProductByQr(req, new in.gbtsolutions.inventoryhub.online.repository.OnlineProductRepository.ProductActionCallback() {
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
                    if (callback != null) callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to scan QR");
                });
            }
        });
    }

    private static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
