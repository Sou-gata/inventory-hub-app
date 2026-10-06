package in.gbtsolutions.inventoryhub.online.models;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.models.Buyer;

public class BuyerMapper {

    public static Buyer toDomain(OnlineBuyerDto dto) {
        if (dto == null) return null;

        Buyer buyer = new Buyer();
        buyer.buyerId = dto.buyerId;
        buyer.buyerName = dto.buyerName != null ? dto.buyerName : "";
        buyer.contactPerson = dto.contactPerson != null ? dto.contactPerson : "";
        buyer.phone = dto.phone != null ? dto.phone : "";
        buyer.email = dto.email != null ? dto.email : "";
        buyer.address = dto.address != null ? dto.address : "";
        buyer.city = dto.city != null ? dto.city : "";
        buyer.stateCode = dto.stateCode != null ? dto.stateCode : "";
        buyer.billingStateCode = dto.billingStateCode != null ? dto.billingStateCode : "";
        buyer.shippingStateCode = dto.shippingStateCode != null ? dto.shippingStateCode : "";
        buyer.isRegistered = dto.isRegistered;
        buyer.postalCode = dto.postalCode != null ? dto.postalCode : "";
        buyer.country = dto.country != null ? dto.country : "India";
        buyer.gst = dto.gst != null ? dto.gst : "";
        buyer.pan = dto.pan != null ? dto.pan : "";
        buyer.notes = dto.notes != null ? dto.notes : "";
        buyer.isActive = dto.isActive;
        buyer.createdAt = dto.createdAt != null ? dto.createdAt : System.currentTimeMillis();
        buyer.updatedAt = dto.updatedAt != null ? dto.updatedAt : System.currentTimeMillis();

        return buyer;
    }

    public static OnlineBuyerDto toDto(Buyer buyer) {
        if (buyer == null) return null;

        OnlineBuyerDto dto = new OnlineBuyerDto();
        dto.buyerId = buyer.buyerId;
        dto.buyerName = buyer.buyerName;
        dto.contactPerson = buyer.contactPerson;
        dto.phone = buyer.phone;
        dto.email = buyer.email;
        dto.address = buyer.address;
        dto.city = buyer.city;
        dto.stateCode = buyer.stateCode;
        dto.billingStateCode = buyer.billingStateCode;
        dto.shippingStateCode = buyer.shippingStateCode;
        dto.isRegistered = buyer.isRegistered;
        dto.postalCode = buyer.postalCode;
        dto.country = buyer.country != null && !buyer.country.isEmpty() ? buyer.country : "India";
        dto.gst = buyer.gst;
        dto.pan = buyer.pan;
        dto.notes = buyer.notes;
        dto.isActive = buyer.isActive;
        dto.createdAt = buyer.createdAt;
        dto.updatedAt = buyer.updatedAt;

        return dto;
    }

    public static List<Buyer> toDomainList(List<OnlineBuyerDto> dtos) {
        List<Buyer> list = new ArrayList<>();
        if (dtos != null) {
            for (OnlineBuyerDto dto : dtos) {
                Buyer b = toDomain(dto);
                if (b != null) {
                    list.add(b);
                }
            }
        }
        return list;
    }
}
