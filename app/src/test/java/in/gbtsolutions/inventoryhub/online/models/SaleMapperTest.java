package in.gbtsolutions.inventoryhub.online.models;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;

import in.gbtsolutions.inventoryhub.models.SaleItemWithProduct;
import in.gbtsolutions.inventoryhub.models.SaleWithBuyer;

public class SaleMapperTest {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Boolean.class, (JsonDeserializer<Boolean>) (json, typeOfT, context) -> {
                if (json == null || json.isJsonNull()) return false;
                if (json.isJsonPrimitive()) {
                    JsonPrimitive p = json.getAsJsonPrimitive();
                    if (p.isBoolean()) return p.getAsBoolean();
                    if (p.isNumber()) return p.getAsInt() != 0;
                    if (p.isString()) {
                        String s = p.getAsString().trim();
                        return "1".equals(s) || "true".equalsIgnoreCase(s);
                    }
                }
                return false;
            })
            .registerTypeAdapter(boolean.class, (JsonDeserializer<Boolean>) (json, typeOfT, context) -> {
                if (json == null || json.isJsonNull()) return false;
                if (json.isJsonPrimitive()) {
                    JsonPrimitive p = json.getAsJsonPrimitive();
                    if (p.isBoolean()) return p.getAsBoolean();
                    if (p.isNumber()) return p.getAsInt() != 0;
                    if (p.isString()) {
                        String s = p.getAsString().trim();
                        return "1".equals(s) || "true".equalsIgnoreCase(s);
                    }
                }
                return false;
            })
            .create();

    @Test
    public void testSaleDtoDeserialization_AndMapping() {
        String json = "{\n" +
                "  \"saleId\": 1,\n" +
                "  \"invoiceId\": \"INV-23456\",\n" +
                "  \"buyerId\": 2,\n" +
                "  \"buyerName\": \"North Bengal Distributors\",\n" +
                "  \"totalAmount\": 168.0,\n" +
                "  \"billingDate\": \"2026-10-09\",\n" +
                "  \"paymentStatus\": \"Completed\",\n" +
                "  \"paymentMethod\": \"Cash\",\n" +
                "  \"isWalkIn\": false,\n" +
                "  \"items\": [\n" +
                "    {\n" +
                "      \"saleItemId\": 1,\n" +
                "      \"productId\": 9,\n" +
                "      \"productName\": \"Basmati Rice 5kg\",\n" +
                "      \"quantity\": 5.0,\n" +
                "      \"unitPrice\": 30.0,\n" +
                "      \"subtotal\": 150.0\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        OnlineSaleDto dto = gson.fromJson(json, OnlineSaleDto.class);
        assertNotNull(dto);
        assertEquals(1, dto.saleId);
        assertEquals("INV-23456", dto.invoiceNo);
        assertEquals("North Bengal Distributors", dto.buyerName);
        assertEquals(168.0, dto.totalAmount, 0.001);

        SaleWithBuyer swb = SaleMapper.toDomainSaleWithBuyer(dto);
        assertNotNull(swb);
        assertNotNull(swb.sale);
        assertEquals("INV-23456", swb.sale.invoiceId);
        assertEquals(168.0, swb.sale.totalAmount, 0.001);
        assertEquals("North Bengal Distributors", swb.buyer != null ? swb.buyer.buyerName : swb.sale.customerName);

        List<SaleItemWithProduct> items = SaleMapper.toDomainSaleItemsWithProduct(dto);
        assertNotNull(items);
        assertEquals(1, items.size());
        assertEquals("Basmati Rice 5kg", items.get(0).product.productName);
        assertEquals(5.0, items.get(0).saleItem.quantity, 0.001);
    }
}
