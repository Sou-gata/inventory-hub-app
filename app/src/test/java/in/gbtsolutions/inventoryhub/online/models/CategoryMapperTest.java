package in.gbtsolutions.inventoryhub.online.models;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import org.junit.Test;

import in.gbtsolutions.inventoryhub.helpers.CategoryIconHelper;
import in.gbtsolutions.inventoryhub.models.Category;

public class CategoryMapperTest {

    private final Gson gson = new Gson();

    @Test
    public void testToDto_MapsIconAndSanitizes() {
        Category category = new Category("Electronics", "Gadgets", "cat_ic_electronics");
        category.categoryId = 10;

        OnlineCategoryDto dto = CategoryMapper.toDto(category);

        assertNotNull(dto);
        assertEquals(10, dto.id);
        assertEquals("Electronics", dto.name);
        assertEquals("Gadgets", dto.description);
        assertEquals("electronics", dto.icon);
    }

    @Test
    public void testToDto_NullCategoryReturnsNull() {
        assertNull(CategoryMapper.toDto(null));
    }

    @Test
    public void testToDomain_MapsIconAndSanitizes() {
        OnlineCategoryDto dto = new OnlineCategoryDto(5, "Hardware", "Tools", "cat_ic_hardware");

        Category domain = CategoryMapper.toDomain(dto);

        assertNotNull(domain);
        assertEquals(5, domain.categoryId);
        assertEquals("Hardware", domain.categoryName);
        assertEquals("Tools", domain.description);
        assertEquals("hardware", domain.icon);
    }

    @Test
    public void testToDomain_NullIconDefaults() {
        OnlineCategoryDto dto = new OnlineCategoryDto(5, "General", "Misc", null);

        Category domain = CategoryMapper.toDomain(dto);

        assertNotNull(domain);
        assertEquals(CategoryIconHelper.DEFAULT_ICON_KEY, domain.icon);
    }

    @Test
    public void testGsonSerialization_IncludesIcon() {
        OnlineCategoryDto dto = new OnlineCategoryDto(1, "Food", "Edibles", "groceries");
        String json = gson.toJson(dto);

        assertTrue(json.contains("\"icon\":\"groceries\""));
    }

    @Test
    public void testGsonDeserialization_AcceptsAlternativeKeys() {
        String jsonWithCategoryIcon = "{\"id\":2,\"name\":\"Phones\",\"description\":\"Smartphones\",\"category_icon\":\"phone\"}";
        OnlineCategoryDto dto = gson.fromJson(jsonWithCategoryIcon, OnlineCategoryDto.class);

        assertNotNull(dto);
        assertEquals("phone", dto.icon);

        Category domain = CategoryMapper.toDomain(dto);
        assertNotNull(domain);
        assertEquals("phone", domain.icon);
    }
}
