package in.gbtsolutions.inventoryhub.online.models;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseItem;
import in.gbtsolutions.inventoryhub.models.PurchaseItemWithProduct;
import in.gbtsolutions.inventoryhub.models.PurchaseWithSupplier;
import in.gbtsolutions.inventoryhub.models.ReceiveItem;
import in.gbtsolutions.inventoryhub.models.ReceiveRecord;
import in.gbtsolutions.inventoryhub.models.ReceiveRecordWithItems;
import in.gbtsolutions.inventoryhub.models.Suppliers;

public class PurchaseMapper {

    public static Purchase toDomain(OnlinePurchaseDto dto) {
        if (dto == null) return null;

        Purchase purchase = new Purchase();
        purchase.purchaseId = dto.purchaseId;
        purchase.supplierId = dto.supplierId != null ? dto.supplierId : 0;
        purchase.invoiceId = dto.invoiceId != null ? dto.invoiceId : "";
        purchase.billingDate = dto.purchaseDate != null ? dto.purchaseDate : "";
        purchase.subtotalAmount = dto.subtotalAmount;
        purchase.cgstAmount = dto.cgstAmount;
        purchase.sgstAmount = dto.sgstAmount;
        purchase.igstAmount = dto.igstAmount;
        purchase.totalGst = dto.totalGst;
        purchase.otherCharges = dto.otherCharges;
        purchase.totalAmount = dto.totalAmount;
        purchase.discountAmount = dto.discountAmount;
        purchase.discountPercent = dto.discountPercent;
        purchase.status = dto.status != null ? dto.status : "Completed";
        purchase.createdBy = dto.createdBy;
        purchase.cancelledBy = dto.cancelledBy;
        purchase.createdAt = dto.createdAt != null ? dto.createdAt : dto.purchaseDate;
        purchase.updatedAt = dto.updatedAt != null ? dto.updatedAt : dto.createdAt;

        return purchase;
    }

    public static Suppliers toDomainSupplier(OnlinePurchaseDto dto) {
        if (dto == null) return null;

        if (dto.supplier != null) {
            return SupplierMapper.toDomain(dto.supplier);
        }

        if (dto.supplierId != null && dto.supplierId > 0) {
            Suppliers supplier = new Suppliers();
            supplier.supplierId = dto.supplierId;
            supplier.supplierName = dto.supplierName != null ? dto.supplierName : "";
            supplier.isActive = true;
            return supplier;
        }

        if (dto.supplierName != null && !dto.supplierName.isEmpty()) {
            Suppliers supplier = new Suppliers();
            supplier.supplierId = 0;
            supplier.supplierName = dto.supplierName;
            supplier.isActive = true;
            return supplier;
        }

        return null;
    }

    public static List<PurchaseItemWithProduct> toDomainPurchaseItemsWithProduct(OnlinePurchaseDto dto) {
        List<PurchaseItemWithProduct> list = new ArrayList<>();
        if (dto == null || dto.items == null) return list;

        for (OnlinePurchaseDto.OnlinePurchaseItemDto it : dto.items) {
            PurchaseItem item = new PurchaseItem();
            item.purchaseItemId = it.purchaseItemId != null ? it.purchaseItemId : 0;
            item.purchaseId = dto.purchaseId;
            item.productId = it.productId;
            item.quantity = it.quantity;
            item.receivedQuantity = it.receivedQuantity;
            item.unitPrice = it.unitPrice;
            item.subtotal = it.unitPrice * it.quantity;

            if (it.taxPercent > 0) {
                item.cgstRate = it.taxPercent / 2.0;
                item.sgstRate = it.taxPercent / 2.0;
                item.cgstAmount = (item.subtotal * item.cgstRate) / 100.0;
                item.sgstAmount = (item.subtotal * item.sgstRate) / 100.0;
            }

            Product product = new Product();
            product.productId = it.productId;
            product.productName = it.productName != null ? it.productName : ("Product #" + it.productId);
            product.unitOfMeasure = it.unitOfMeasure != null ? it.unitOfMeasure : "PCS";
            product.batchEnabled = it.batchEnabled;

            PurchaseItemWithProduct piwp = new PurchaseItemWithProduct();
            piwp.purchaseItem = item;
            piwp.product = product;
            list.add(piwp);
        }

        return list;
    }

    public static List<ReceiveRecordWithItems> toDomainReceiveRecordsWithItems(OnlinePurchaseDto dto) {
        List<ReceiveRecordWithItems> list = new ArrayList<>();
        if (dto == null || dto.stages == null) return list;

        for (OnlinePurchaseDto.OnlineReceiveRecordDto rDto : dto.stages) {
            ReceiveRecord record = new ReceiveRecord();
            record.receiveRecordId = rDto.receiveRecordId;
            record.purchaseId = rDto.purchaseId > 0 ? rDto.purchaseId : dto.purchaseId;
            record.receiveDate = rDto.receiveDate;
            record.receivedBy = rDto.receivedBy;
            record.notes = rDto.notes;
            record.createdAt = rDto.createdAt;

            List<ReceiveItem> receiveItems = new ArrayList<>();
            if (rDto.items != null) {
                for (OnlinePurchaseDto.OnlineReceiveItemDto itemDto : rDto.items) {
                    ReceiveItem rItem = new ReceiveItem();
                    rItem.receiveItemId = itemDto.receiveItemId;
                    rItem.receiveRecordId = rDto.receiveRecordId;
                    rItem.purchaseItemId = itemDto.purchaseItemId;
                    rItem.productId = itemDto.productId;
                    rItem.quantityReceived = itemDto.quantityReceived;
                    receiveItems.add(rItem);
                }
            }

            ReceiveRecordWithItems rrwi = new ReceiveRecordWithItems();
            rrwi.record = record;
            rrwi.items = receiveItems;
            list.add(rrwi);
        }

        return list;
    }

    public static PurchaseWithSupplier toDomainPurchaseWithSupplier(OnlinePurchaseDto dto) {
        if (dto == null) return null;

        PurchaseWithSupplier item = new PurchaseWithSupplier();
        item.purchase = toDomain(dto);
        item.supplier = toDomainSupplier(dto);
        item.createdByName = dto.createdByName;
        item.createdByRole = dto.createdByRole;
        item.items = toDomainPurchaseItemsWithProduct(dto);
        item.stages = toDomainReceiveRecordsWithItems(dto);
        return item;
    }

    public static List<PurchaseWithSupplier> toDomainList(List<OnlinePurchaseDto> dtos) {
        List<PurchaseWithSupplier> list = new ArrayList<>();
        if (dtos != null) {
            for (OnlinePurchaseDto dto : dtos) {
                PurchaseWithSupplier pws = toDomainPurchaseWithSupplier(dto);
                if (pws != null && pws.purchase != null) {
                    list.add(pws);
                }
            }
        }
        return list;
    }
}
