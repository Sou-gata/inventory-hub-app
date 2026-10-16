package in.gbtsolutions.inventoryhub.online.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.models.UnitOfMeasure;

public class OnlineUnitOfMeasureDto {

    @SerializedName(value = "id", alternate = {"uom_id", "uomId"})
    public int id;

    @SerializedName(value = "name", alternate = {"unit_name", "unitName"})
    public String name;

    @SerializedName(value = "description", alternate = {"desc"})
    public String description;

    @SerializedName(value = "isDefault", alternate = {"is_default"})
    public boolean isDefault;

    @SerializedName(value = "createdAt", alternate = {"created_at"})
    public long createdAt;

    @SerializedName(value = "updatedAt", alternate = {"updated_at"})
    public long updatedAt;

    public OnlineUnitOfMeasureDto() {
    }

    public OnlineUnitOfMeasureDto(int id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public UnitOfMeasure toDomain() {
        UnitOfMeasure u = new UnitOfMeasure();
        u.uomId = this.id;
        u.name = this.name != null ? this.name.trim() : "";
        u.description = this.description != null ? this.description.trim() : "";
        u.isDefault = this.isDefault;
        u.createdAt = this.createdAt;
        u.updatedAt = this.updatedAt;
        return u;
    }

    public static UnitOfMeasure toDomain(OnlineUnitOfMeasureDto dto) {
        return dto != null ? dto.toDomain() : null;
    }

    public static List<UnitOfMeasure> toDomainList(List<OnlineUnitOfMeasureDto> dtoList) {
        List<UnitOfMeasure> list = new ArrayList<>();
        if (dtoList != null) {
            for (OnlineUnitOfMeasureDto dto : dtoList) {
                if (dto != null) {
                    list.add(dto.toDomain());
                }
            }
        }
        return list;
    }

    public static OnlineUnitOfMeasureDto toDto(UnitOfMeasure uom) {
        if (uom == null) return null;
        OnlineUnitOfMeasureDto dto = new OnlineUnitOfMeasureDto();
        dto.id = uom.uomId;
        dto.name = uom.name;
        dto.description = uom.description;
        dto.isDefault = uom.isDefault;
        dto.createdAt = uom.createdAt;
        dto.updatedAt = uom.updatedAt;
        return dto;
    }
}
