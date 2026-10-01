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
}
