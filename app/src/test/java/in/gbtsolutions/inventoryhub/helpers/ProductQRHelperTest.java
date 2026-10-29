package in.gbtsolutions.inventoryhub.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Calendar;

import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;

public class ProductQRHelperTest {

    @Test
    public void testBuild27CharPayloadWithBatchAndExpiry() {
        Product product = new Product();
        product.productId = 105;
        product.sellingPrice = 250.50;

        ProductBatch batch = new ProductBatch();
        batch.batchId = 42;
        
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.SEPTEMBER, 16, 12, 0, 0);
        batch.expiryDate = cal.getTimeInMillis();

        product.setBatch(batch);

        String payload = ProductQRHelper.build27CharPayload(product);
        assertNotNull(payload);
        assertEquals(27, payload.length());

        try {
            String encrypted = EncryptionHelper.encrypt(payload);
            assertNotNull(encrypted);
            assertEquals(44, encrypted.length());
            String decrypted = EncryptionHelper.decrypt(encrypted);
            assertEquals(payload, decrypted);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Product ID: 000105 (0..6)
        assertEquals("000105", payload.substring(0, 6));

        // Price: 00025050 (6..14)
        assertEquals("00025050", payload.substring(6, 14));

        // Batch ID: 000042 (14..20)
        assertEquals("000042", payload.substring(14, 20));

        // Expiry Date: YYDDD -> 26259 (20..25)
        int expectedYY = 26;
        int expectedDDD = cal.get(Calendar.DAY_OF_YEAR);
        String expectedYYDDD = String.format("%02d%03d", expectedYY, expectedDDD);
        assertEquals(expectedYYDDD, payload.substring(20, 25));

        // Verify checksum
        String mainData = payload.substring(0, 25);
        byte expectedCheck = EncryptionHelper.xorChecksum(mainData.getBytes(StandardCharsets.UTF_8));
        String expectedChecksumHex = String.format("%02X", expectedCheck & 0xFF);
        assertEquals(expectedChecksumHex, payload.substring(25, 27));
    }

    @Test
    public void testBuild27CharPayloadWithoutBatch() {
        Product product = new Product();
        product.productId = 1;
        product.sellingPrice = 10.0;
        product.setBatch(null);

        String payload = ProductQRHelper.build27CharPayload(product);
        assertNotNull(payload);
        assertEquals(27, payload.length());

        assertEquals("000001", payload.substring(0, 6));
        assertEquals("00001000", payload.substring(6, 14));
        assertEquals("000000", payload.substring(14, 20));
        assertEquals("00000", payload.substring(20, 25));
    }

    @Test
    public void testEncryptionAndDecryptionRoundTrip() throws Exception {
        String testPayload = "00010500025050000042262597F";
        
        // Ensure checksum is valid on the test payload
        String mainData = testPayload.substring(0, 25);
        byte check = EncryptionHelper.xorChecksum(mainData.getBytes(StandardCharsets.UTF_8));
        String validPayload = mainData + String.format("%02X", check & 0xFF);

        String encryptedBase64 = EncryptionHelper.encrypt(validPayload);
        assertNotNull(encryptedBase64);
        assertTrue(encryptedBase64.length() > 0);

        String decrypted = EncryptionHelper.decrypt(encryptedBase64);
        assertNotNull(decrypted);
        assertEquals(validPayload, decrypted);
    }

    @Test
    public void testDecryptionInvalidChecksum() throws Exception {
        String mainData = "0001050002505000004226259";
        // Deliberately incorrect checksum
        String tamperedPayload = mainData + "00";

        String encryptedBase64 = EncryptionHelper.encrypt(tamperedPayload);
        // Decrypt checks the checksum and must return null for invalid checksum
        String decrypted = EncryptionHelper.decrypt(encryptedBase64);
        assertNull(decrypted);
    }

    @Test
    public void testParseJulianExpiry() {
        assertEquals(0L, ProductQRHelper.parseJulianExpiry("00000"));
        assertEquals(0L, ProductQRHelper.parseJulianExpiry(null));
        assertEquals(0L, ProductQRHelper.parseJulianExpiry("123"));

        long epoch = ProductQRHelper.parseJulianExpiry("26259");
        assertTrue(epoch > 0);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(epoch);
        assertEquals(2026, cal.get(Calendar.YEAR));
        assertEquals(259, cal.get(Calendar.DAY_OF_YEAR));
    }

    @Test
    public void testBase36EncodingAndDecoding() {
        assertEquals("000000", ProductQRHelper.encodeBase36(0, 6));
        assertEquals("000001", ProductQRHelper.encodeBase36(1, 6));
        assertEquals("000009", ProductQRHelper.encodeBase36(9, 6));
        assertEquals("00000a", ProductQRHelper.encodeBase36(10, 6));
        assertEquals("00000z", ProductQRHelper.encodeBase36(35, 6));
        assertEquals("000010", ProductQRHelper.encodeBase36(36, 6));
        assertEquals("00002x", ProductQRHelper.encodeBase36(105, 6));

        // Huge numbers: Integer.MAX_VALUE = 2,147,483,647 fits in 6 characters!
        String maxInt = ProductQRHelper.encodeBase36(Integer.MAX_VALUE, 6);
        assertEquals(6, maxInt.length());
        assertEquals("zik0zj", maxInt);

        // Max 6-character Base-36 capacity: 36^6 - 1 = 2,176,782,335
        assertEquals("zzzzzz", ProductQRHelper.encodeBase36(2176782335L, 6));

        // Roundtrip decoding
        assertEquals(0L, ProductQRHelper.decodeBase36("000000"));
        assertEquals(1L, ProductQRHelper.decodeBase36("000001"));
        assertEquals(105L, ProductQRHelper.decodeBase36("00002x"));
        assertEquals(105L, ProductQRHelper.decodeBase36("00002X")); // Case-insensitive
        assertEquals(Integer.MAX_VALUE, (int) ProductQRHelper.decodeBase36("zik0zj"));
    }

    @Test
    public void testBuildBarcodePayloadWithoutBatch() {
        Product product = new Product();
        product.productId = 105;
        product.sku = null; // SKU is non-mandatory and ignored for barcode generation

        String barcode = ProductQRHelper.buildBarcodePayload(product);
        assertEquals(6, barcode.length());
        assertEquals("00002x", barcode);
    }

    @Test
    public void testBuildBarcodePayloadWithBatch() {
        Product product = new Product();
        product.productId = 105;
        product.sku = "IGNORED-SKU";
        ProductBatch batch = new ProductBatch();
        batch.batchId = 15;
        product.setBatch(batch);

        String barcode = ProductQRHelper.buildBarcodePayload(product);
        // Fixed 6-char Base-36 ID + "-B" + batchId
        assertEquals("00002x-B15", barcode);
    }

    @Test
    public void testBuildBarcodePayloadIndependentOfSku() {
        Product productWithSku = new Product();
        productWithSku.productId = 42;
        productWithSku.sku = "SOME-OPTIONAL-SKU-12345";

        Product productWithoutSku = new Product();
        productWithoutSku.productId = 42;
        productWithoutSku.sku = null;

        // Both products must yield the exact 6-char Base-36 ID in the barcode
        String code1 = ProductQRHelper.buildBarcodePayload(productWithSku);
        String code2 = ProductQRHelper.buildBarcodePayload(productWithoutSku);
        assertEquals("000016", code1); // 42 in Base-36 = 1*36 + 6 = "16", padded = "000016"
        assertEquals("000016", code2);
    }

    @Test
    public void testLowDensityBarcodeEncodingAndDecoding() throws Exception {
        Product product = new Product();
        product.productId = 105;
        product.sku = null;

        String payload = ProductQRHelper.buildBarcodePayload(product);
        assertEquals("00002x", payload);

        com.google.zxing.oned.Code128Writer writer = new com.google.zxing.oned.Code128Writer();
        com.google.zxing.common.BitMatrix matrix = writer.encode(payload, com.google.zxing.BarcodeFormat.CODE_128, 600, 180);
        assertNotNull(matrix);
        assertEquals(600, matrix.getWidth());

        int w = matrix.getWidth();
        int h = matrix.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                pixels[y * w + x] = matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }

        com.google.zxing.RGBLuminanceSource source = new com.google.zxing.RGBLuminanceSource(w, h, pixels);
        com.google.zxing.BinaryBitmap binaryBitmap = new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));

        com.google.zxing.MultiFormatReader reader = new com.google.zxing.MultiFormatReader();
        java.util.Map<com.google.zxing.DecodeHintType, Object> hints = new java.util.HashMap<>();
        hints.put(com.google.zxing.DecodeHintType.POSSIBLE_FORMATS, java.util.Collections.singletonList(com.google.zxing.BarcodeFormat.CODE_128));
        hints.put(com.google.zxing.DecodeHintType.TRY_HARDER, Boolean.TRUE);
        reader.setHints(hints);

        com.google.zxing.Result result = reader.decode(binaryBitmap);
        assertNotNull(result);
        assertEquals("00002x", result.getText());
    }
}
