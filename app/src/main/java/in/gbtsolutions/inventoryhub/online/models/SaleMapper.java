package in.gbtsolutions.inventoryhub.online.models;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.models.Buyer;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleItem;
import in.gbtsolutions.inventoryhub.models.SaleItemWithProduct;
import in.gbtsolutions.inventoryhub.models.SaleWithBuyer;

public class SaleMapper {

    public static Sale toDomain(OnlineSaleDto dto) {
        if (dto == null) return null;

        Sale sale = new Sale();
        sale.saleId = dto.saleId;
        sale.buyerId = dto.buyerId != null ? dto.buyerId : 0;
        sale.customerName = dto.customerName != null ? dto.customerName : dto.buyerName;
        sale.customerPhone = dto.customerPhone;
        sale.customerGstin = dto.customerGstin;
        sale.isWalkIn = dto.isWalkIn;
        sale.placeOfSupply = dto.placeOfSupply;
        sale.placeOfSupplyStateCode = dto.placeOfSupplyStateCode;
        sale.invoiceId = dto.invoiceNo != null ? dto.invoiceNo : "";
        sale.subtotalAmount = dto.subtotalAmount;
        sale.cgstAmount = dto.cgstAmount;
        sale.sgstAmount = dto.sgstAmount;
        sale.igstAmount = dto.igstAmount;
        sale.totalGst = dto.taxAmount;
        sale.cessAmount = dto.cessAmount;
        sale.otherCharges = dto.otherCharges;
        sale.totalAmount = dto.totalAmount > 0 ? dto.totalAmount : dto.netAmount;
        sale.status = dto.paymentStatus != null ? dto.paymentStatus : "Completed";
        sale.createdBy = dto.createdBy;
        sale.billingDate = dto.saleDate != null ? dto.saleDate : "";
        sale.createdAt = dto.createdAt != null ? dto.createdAt : dto.saleDate;
        sale.updatedAt = dto.updatedAt != null ? dto.updatedAt : dto.saleDate;
        sale.discountAmount = dto.discountAmount;
        sale.discountPercent = dto.discountPercent;
        sale.paymentMethod = dto.paymentType != null ? dto.paymentType : "Cash";
        sale.cancelledBy = dto.cancelledBy;

        return sale;
    }

    public static Buyer toDomainBuyer(OnlineSaleDto dto) {
        if (dto == null) return null;

        if (dto.buyer != null) {
            return BuyerMapper.toDomain(dto.buyer);
        }

        if (dto.buyerId != null && dto.buyerId > 0) {
            Buyer buyer = new Buyer();
            buyer.buyerId = dto.buyerId;
            buyer.buyerName = dto.buyerName != null ? dto.buyerName : (dto.customerName != null ? dto.customerName : "");
            buyer.phone = dto.customerPhone != null ? dto.customerPhone : "";
            buyer.gst = dto.customerGstin != null ? dto.customerGstin : "";
            buyer.isActive = true;
            return buyer;
        }

        if (dto.buyerName != null && !dto.buyerName.isEmpty()) {
            Buyer buyer = new Buyer();
            buyer.buyerId = 0;
            buyer.buyerName = dto.buyerName;
            buyer.phone = dto.customerPhone != null ? dto.customerPhone : "";
            buyer.gst = dto.customerGstin != null ? dto.customerGstin : "";
            buyer.isActive = true;
            return buyer;
        }

        return null;
    }

    public static SaleWithBuyer toDomainSaleWithBuyer(OnlineSaleDto dto) {
        if (dto == null) return null;

        SaleWithBuyer item = new SaleWithBuyer();
        item.sale = toDomain(dto);
        item.buyer = toDomainBuyer(dto);
        return item;
    }

    public static List<SaleWithBuyer> toDomainList(List<OnlineSaleDto> dtos) {
        List<SaleWithBuyer> list = new ArrayList<>();
        if (dtos != null) {
            for (OnlineSaleDto dto : dtos) {
                SaleWithBuyer swb = toDomainSaleWithBuyer(dto);
                if (swb != null && swb.sale != null) {
                    list.add(swb);
                }
            }
        }
        return list;
    }

    public static List<SaleItemWithProduct> toDomainSaleItemsWithProduct(OnlineSaleDto dto) {
        List<SaleItemWithProduct> list = new ArrayList<>();
        if (dto == null || dto.items == null) return list;

        for (OnlineSaleDto.OnlineSaleItemDto it : dto.items) {
            SaleItem item = new SaleItem();
            item.saleItemId = it.saleItemId;
            item.saleId = it.saleId > 0 ? it.saleId : dto.saleId;
            item.productId = it.productId;
            item.batchId = it.batchId;
            item.quantity = it.quantity;
            item.unitPrice = it.unitPrice > 0 ? it.unitPrice : it.sellingPrice;
            item.hsnCode = it.hsnCode;
            item.sacCode = it.sacCode;
            item.uqc = it.uqc != null ? it.uqc : "PCS";
            item.cgstRate = it.cgstRate;
            item.sgstRate = it.sgstRate;
            item.igstRate = it.igstRate;
            item.cgstAmount = it.cgstAmount;
            item.sgstAmount = it.sgstAmount;
            item.igstAmount = it.igstAmount;
            item.cessRate = it.cessRate;
            item.cessAmount = it.cessAmount;
            item.taxableValue = it.taxableValue;
            item.subtotal = it.subtotal > 0 ? it.subtotal : (it.quantity * it.unitPrice);
            item.discountPercent = it.discountPercent;
            item.discountAmount = it.discountAmount;

            Product product = new Product();
            product.productId = it.productId;
            product.productName = it.productName != null ? it.productName : ("Product #" + it.productId);
            product.sku = it.sku != null ? it.sku : "";

            SaleItemWithProduct siwp = new SaleItemWithProduct();
            siwp.saleItem = item;
            siwp.product = product;
            list.add(siwp);
        }

        return list;
    }
}
