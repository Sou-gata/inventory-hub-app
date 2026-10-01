package in.gbtsolutions.inventoryhub.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ProductCsvParserTest {

    private static final String FULL_HEADERS =
            "product_name,sku,category_name,brand,description,unit_of_measure,unit_price,selling_price," +
            "quantity,reorder_level,reorder_quantity,hsn_code,gst_percent,default_markup_percent,status," +
            "batch_no,batch_quantity,batch_purchase_price,batch_selling_price,batch_expiry_date";

    @Test
    public void testHeaderValidation_Success() throws Exception {
        String csv = FULL_HEADERS + "\n" +
                "Laptop,SKU-001,Electronics,Dell,Gaming Laptop,pcs,500.0,750.0,10,2,5,8471,18.0,50.0,active,B1,10,500.0,750.0,2028-12-31\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
        ProductCsvParser.HeaderValidationResult result = ProductCsvParser.validateHeaders(stream);

        assertTrue("Header validation should pass", result.isValid);
        assertTrue("Missing headers should be empty", result.missingHeaders.isEmpty());
        assertEquals(20, result.detectedHeaders.size());
    }

    @Test
    public void testHeaderValidation_MissingHeaders() throws Exception {
        String partialHeaders = "product_name,sku,category_name,brand";
        ByteArrayInputStream stream = new ByteArrayInputStream(partialHeaders.getBytes(StandardCharsets.UTF_8));
        ProductCsvParser.HeaderValidationResult result = ProductCsvParser.validateHeaders(stream);

        assertFalse("Header validation should fail when headers are missing", result.isValid);
        assertFalse("Missing headers list should not be empty", result.missingHeaders.isEmpty());
        assertTrue("Should identify selling_price as missing", result.missingHeaders.contains("selling_price"));
        assertTrue("Should identify unit_price as missing", result.missingHeaders.contains("unit_price"));
    }

    @Test
    public void testHeaderValidation_WithBOMAndCaseVariations() throws Exception {
        String csvWithBom = "\uFEFFProduct_Name,SKU,Category_Name,Brand,Description,Unit_Of_Measure,Unit_Price,Selling_Price," +
                "Quantity,Reorder_Level,Reorder_Quantity,HSN_Code,GST_Percent,Default_Markup_Percent,Status," +
                "Batch_No,Batch_Quantity,Batch_Purchase_Price,Batch_Selling_Price,Batch_Expiry_Date\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(csvWithBom.getBytes(StandardCharsets.UTF_8));
        ProductCsvParser.HeaderValidationResult result = ProductCsvParser.validateHeaders(stream);

        assertTrue("Should tolerate BOM and uppercase letters", result.isValid);
    }

    @Test
    public void testParseWithCategoryResolver_CreatesCategory() throws Exception {
        String csv = FULL_HEADERS + "\n" +
                "Apple iPhone 15,IPHONE-15,Smartphones,Apple,Pro 256GB,pcs,800.0,999.0,5,1,2,8517,18.0,25.0,active,,,,,\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        Map<String, Integer> createdCategories = new HashMap<>();
        ProductCsvParser.CategoryResolver resolver = categoryName -> {
            createdCategories.put(categoryName, 42);
            return 42;
        };

        ProductCsvParser parser = new ProductCsvParser(resolver);
        ProductCsvParser.ParseResult result = parser.parse(stream);

        assertTrue("Errors should be empty", result.errors.isEmpty());
        assertEquals(1, result.groups.size());
        assertEquals(1, createdCategories.size());
        assertTrue(createdCategories.containsKey("Smartphones"));
        assertNotNull(result.groups.get(0).product);
        assertEquals(42, result.groups.get(0).product.categoryId);
        assertEquals("Apple iPhone 15", result.groups.get(0).product.productName);
        assertEquals("IPHONE-15", result.groups.get(0).product.sku);
    }

    @Test
    public void testParseWithBatches() throws Exception {
        String csv = FULL_HEADERS + "\n" +
                "Test Item,SKU-B1,General,BrandX,Desc,pcs,10.0,15.0,0,0,0,1234,5.0,50.0,active,BATCH-1,20,10.0,15.0,2027-01-01\n" +
                "Test Item,SKU-B1,General,BrandX,Desc,pcs,10.0,15.0,0,0,0,1234,5.0,50.0,active,BATCH-2,30,10.0,15.0,2027-06-01\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ProductCsvParser.CategoryResolver resolver = categoryName -> 1;
        ProductCsvParser parser = new ProductCsvParser(resolver);
        ProductCsvParser.ParseResult result = parser.parse(stream);

        assertTrue(result.errors.isEmpty());
        assertEquals(1, result.groups.size());
        assertEquals(2, result.groups.get(0).batches.size());
        assertEquals(50, result.groups.get(0).product.quantity);
        assertTrue(result.groups.get(0).product.batchEnabled);
    }

    @Test
    public void testBothSkuAndHsnAbsent_RowSkipped() throws Exception {
        String csv = FULL_HEADERS + "\n" +
                "Item No Identifiers,,General,BrandX,Desc,pcs,10.0,15.0,5,0,0,,5.0,50.0,active,,,,,\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ProductCsvParser parser = new ProductCsvParser(categoryName -> 1);
        ProductCsvParser.ParseResult result = parser.parse(stream);

        assertEquals(0, result.groups.size());
        assertEquals(1, result.errors.size());
        assertTrue(result.errors.get(0).reason.contains("Both SKU and HSN are absent"));
    }

    @Test
    public void testMandatorySku_MissingSkuSkipped_MissingHsnAllowed() throws Exception {
        // Line 2 has SKU and no HSN (should succeed)
        // Line 3 has HSN but no SKU (should fail because SKU is mandatory by default)
        String csv = FULL_HEADERS + "\n" +
                "Item SkuOnly,SKU-ONLY,General,BrandX,Desc,pcs,10.0,15.0,5,0,0,,5.0,50.0,active,,,,,\n" +
                "Item HsnOnly,,General,BrandX,Desc,pcs,10.0,15.0,5,0,0,8471,5.0,50.0,active,,,,,\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ProductCsvParser parser = new ProductCsvParser(categoryName -> 1);
        parser.setMandatoryField(ProductCsvParser.MANDATORY_SKU);
        ProductCsvParser.ParseResult result = parser.parse(stream);

        assertEquals(1, result.groups.size());
        assertEquals("SKU-ONLY", result.groups.get(0).product.sku);
        assertEquals(1, result.errors.size());
        assertTrue(result.errors.get(0).reason.contains("SKU is mandatory but missing"));
    }

    @Test
    public void testMandatoryHsn_MissingHsnSkipped_MissingSkuAllowed() throws Exception {
        // Line 2 has HSN and no SKU (should succeed)
        // Line 3 has SKU and no HSN (should fail because HSN is mandatory)
        String csv = FULL_HEADERS + "\n" +
                "Item HsnOnly,,General,BrandX,Desc,pcs,10.0,15.0,5,0,0,8471,5.0,50.0,active,,,,,\n" +
                "Item SkuOnly,SKU-ONLY,General,BrandX,Desc,pcs,10.0,15.0,5,0,0,,5.0,50.0,active,,,,,\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ProductCsvParser parser = new ProductCsvParser(categoryName -> 1);
        parser.setMandatoryField(ProductCsvParser.MANDATORY_HSN);
        ProductCsvParser.ParseResult result = parser.parse(stream);

        assertEquals(1, result.groups.size());
        assertEquals("8471", result.groups.get(0).product.hsnCode);
        assertEquals(1, result.errors.size());
        assertTrue(result.errors.get(0).reason.contains("HSN code is mandatory but missing"));
    }

    @Test
    public void testMandatoryBoth_RequiresBothSkuAndHsn() throws Exception {
        // Line 2 has both (should succeed)
        // Line 3 has only SKU (should fail)
        // Line 4 has only HSN (should fail)
        String csv = FULL_HEADERS + "\n" +
                "Item Both,SKU-BOTH,General,BrandX,Desc,pcs,10.0,15.0,5,0,0,8471,5.0,50.0,active,,,,,\n" +
                "Item SkuOnly,SKU-ONLY,General,BrandX,Desc,pcs,10.0,15.0,5,0,0,,5.0,50.0,active,,,,,\n" +
                "Item HsnOnly,,General,BrandX,Desc,pcs,10.0,15.0,5,0,0,8471,5.0,50.0,active,,,,,\n";
        ByteArrayInputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        ProductCsvParser parser = new ProductCsvParser(categoryName -> 1);
        parser.setMandatoryField(ProductCsvParser.MANDATORY_BOTH);
        ProductCsvParser.ParseResult result = parser.parse(stream);

        assertEquals(1, result.groups.size());
        assertEquals("SKU-BOTH", result.groups.get(0).product.sku);
        assertEquals("8471", result.groups.get(0).product.hsnCode);
        assertEquals(2, result.errors.size());
    }
}
