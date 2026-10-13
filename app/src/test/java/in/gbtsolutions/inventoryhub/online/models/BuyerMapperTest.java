package in.gbtsolutions.inventoryhub.online.models;

import static org.junit.Assert.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonPrimitive;

import org.junit.Test;

public class BuyerMapperTest {

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
    public void testGsonDeserialization_NumericBooleans() {
        String json = "{\"buyer_id\":1,\"buyer_name\":\"Kolkata Supermart\",\"is_registered\":1,\"is_active\":1,\"created_at\":1791541324388,\"updated_at\":1791541324388}";
        OnlineBuyerDto dto = gson.fromJson(json, OnlineBuyerDto.class);
        assertNotNull(dto);
        assertEquals("Kolkata Supermart", dto.buyerName);
        assertTrue(dto.isActive);
        assertTrue(dto.isRegistered);
    }

    @Test
    public void testGsonDeserialization_ZeroBooleans() {
        String json = "{\"buyer_id\":2,\"buyer_name\":\"Unregistered Buyer\",\"is_registered\":0,\"is_active\":0}";
        OnlineBuyerDto dto = gson.fromJson(json, OnlineBuyerDto.class);
        assertNotNull(dto);
        assertFalse(dto.isActive);
        assertFalse(dto.isRegistered);
    }
}
