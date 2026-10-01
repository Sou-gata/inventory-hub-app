package in.gbtsolutions.inventoryhub.online.models;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.models.Category;

public class CategoryMapper {

    public static Category toDomain(OnlineCategoryDto dto) {
        if (dto == null) return null;
        Category category = new Category();
        category.categoryId = dto.id;
        category.categoryName = dto.name != null ? dto.name : "";
        category.description = dto.description != null ? dto.description : "";
        return category;
    }

    public static OnlineCategoryDto toDto(Category category) {
        if (category == null) return null;
        OnlineCategoryDto dto = new OnlineCategoryDto();
        dto.id = category.categoryId;
        dto.name = category.categoryName;
        dto.description = category.description;
        return dto;
    }

    public static List<Category> toDomainList(List<OnlineCategoryDto> dtos) {
        List<Category> list = new ArrayList<>();
        if (dtos != null) {
            for (OnlineCategoryDto dto : dtos) {
                Category c = toDomain(dto);
                if (c != null) {
                    list.add(c);
                }
            }
        }
        return list;
    }
}
