package in.gbtsolutions.inventoryhub.online.models;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.models.Suppliers;

public class SupplierMapper {

    public static Suppliers toDomain(OnlineSupplierDto dto) {
        if (dto == null) return null;

        Suppliers supplier = new Suppliers();
        supplier.supplierId = dto.supplierId;
        supplier.supplierName = dto.supplierName != null ? dto.supplierName : "";
        supplier.contactPerson = dto.contactPerson != null ? dto.contactPerson : "";
        supplier.phone = dto.phone != null ? dto.phone : "";
        supplier.email = dto.email != null ? dto.email : "";
        supplier.address = dto.address != null ? dto.address : "";
        supplier.city = dto.city != null ? dto.city : "";
        supplier.stateCode = dto.stateCode != null ? dto.stateCode : "";
        supplier.postalCode = dto.postalCode != null ? dto.postalCode : "";
        supplier.country = dto.country != null ? dto.country : "India";
        supplier.gst = dto.gst != null ? dto.gst : "";
        supplier.pan = dto.pan != null ? dto.pan : "";
        supplier.notes = dto.notes != null ? dto.notes : "";
        supplier.isActive = dto.isActive;
        supplier.createdAt = dto.createdAt != null ? dto.createdAt : System.currentTimeMillis();
        supplier.updatedAt = dto.updatedAt != null ? dto.updatedAt : System.currentTimeMillis();

        return supplier;
    }

    public static OnlineSupplierDto toDto(Suppliers supplier) {
        if (supplier == null) return null;

        OnlineSupplierDto dto = new OnlineSupplierDto();
        dto.supplierId = supplier.supplierId;
        dto.supplierName = supplier.supplierName;
        dto.contactPerson = supplier.contactPerson;
        dto.phone = supplier.phone;
        dto.email = supplier.email;
        dto.address = supplier.address;
        dto.city = supplier.city;
        dto.stateCode = supplier.stateCode;
        dto.postalCode = supplier.postalCode;
        dto.country = supplier.country != null && !supplier.country.isEmpty() ? supplier.country : "India";
        dto.gst = supplier.gst;
        dto.pan = supplier.pan;
        dto.notes = supplier.notes;
        dto.isActive = supplier.isActive;
        dto.createdAt = supplier.createdAt;
        dto.updatedAt = supplier.updatedAt;

        return dto;
    }

    public static List<Suppliers> toDomainList(List<OnlineSupplierDto> dtos) {
        List<Suppliers> list = new ArrayList<>();
        if (dtos != null) {
            for (OnlineSupplierDto dto : dtos) {
                Suppliers s = toDomain(dto);
                if (s != null) {
                    list.add(s);
                }
            }
        }
        return list;
    }
}
