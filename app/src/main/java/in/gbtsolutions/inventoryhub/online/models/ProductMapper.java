package in.gbtsolutions.inventoryhub.online.models;

import java.util.ArrayList;
import java.util.List;

import in.gbtsolutions.inventoryhub.models.Product;

public class ProductMapper {

    public static Product toDomain(OnlineProductDto dto) {
        if (dto == null) return null;

        Product product = new Product();
        product.productId = dto.productId;
        product.productName = dto.productName != null ? dto.productName : "";
        product.categoryId = dto.categoryId;
        product.sku = dto.sku != null ? dto.sku : "";
        product.description = dto.description != null ? dto.description : "";
        product.brand = dto.brand != null ? dto.brand : "";
        product.unitOfMeasure = dto.unitOfMeasure != null ? dto.unitOfMeasure : "PCS";
        product.reorderLevel = dto.reorderLevel;
        product.reorderQuantity = dto.reorderQuantity;
        product.status = dto.status != null ? dto.status : "ACTIVE";
        product.hsnCode = dto.hsnCode != null ? dto.hsnCode : "";
        product.gstPercent = dto.gstPercent;
        product.gstCategory = dto.gstCategory != null ? dto.gstCategory : "TAXABLE";
        product.uqc = dto.uqc != null ? dto.uqc : "PCS";
        product.cessPercent = dto.cessPercent;
        product.defaultMarkupPercent = dto.defaultMarkupPercent;
        product.unitPrice = dto.unitPrice;
        product.sellingPrice = dto.sellingPrice;
        product.quantity = dto.quantity;
        product.batchEnabled = dto.batchEnabled;
        product.createdAt = dto.createdAt != null ? dto.createdAt : System.currentTimeMillis();
        product.updatedAt = dto.updatedAt != null ? dto.updatedAt : System.currentTimeMillis();

        if (dto.batches != null && !dto.batches.isEmpty()) {
            List<in.gbtsolutions.inventoryhub.models.ProductBatch> batchList = new ArrayList<>();
            for (OnlineProductDto.OnlineBatchDto bDto : dto.batches) {
                if (bDto == null) continue;
                in.gbtsolutions.inventoryhub.models.ProductBatch b = new in.gbtsolutions.inventoryhub.models.ProductBatch();
                b.batchId = bDto.batchId != null ? bDto.batchId : 0;
                b.productId = bDto.productId != null ? bDto.productId : dto.productId;
                b.batchNo = bDto.batchNo != null ? bDto.batchNo : ("BATCH-" + b.batchId);
                b.quantity = bDto.quantity;
                b.purchasePrice = bDto.purchasePrice > 0 ? bDto.purchasePrice : dto.unitPrice;
                b.sellingPrice = bDto.sellingPrice > 0 ? bDto.sellingPrice : dto.sellingPrice;
                b.expiryDate = bDto.expiryDate != null ? bDto.expiryDate : 0;
                batchList.add(b);
            }
            product.setBatches(batchList);
        } else if (dto.batchId != null && dto.batchId > 0) {
            in.gbtsolutions.inventoryhub.models.ProductBatch batch = new in.gbtsolutions.inventoryhub.models.ProductBatch();
            batch.batchId = dto.batchId;
            batch.productId = dto.productId;
            batch.batchNo = dto.batchNo != null ? dto.batchNo : ("BATCH-" + dto.batchId);
            batch.quantity = dto.batchQuantity != null ? dto.batchQuantity : dto.quantity;
            batch.purchasePrice = dto.unitPrice;
            batch.sellingPrice = dto.sellingPrice;
            batch.expiryDate = dto.expiryDate != null ? dto.expiryDate : 0;
            product.setBatch(batch);
        }

        return product;
    }

    public static OnlineProductDto toDto(Product product) {
        if (product == null) return null;

        OnlineProductDto dto = new OnlineProductDto();
        dto.productId = product.productId;
        dto.productName = product.productName;
        dto.categoryId = product.categoryId;
        dto.sku = product.sku;
        dto.description = product.description;
        dto.brand = product.brand;
        dto.unitOfMeasure = product.unitOfMeasure;
        dto.reorderLevel = product.reorderLevel;
        dto.reorderQuantity = product.reorderQuantity;
        dto.status = product.status;
        dto.hsnCode = product.hsnCode;
        dto.gstPercent = product.gstPercent;
        dto.gstCategory = product.gstCategory;
        dto.uqc = product.uqc;
        dto.cessPercent = product.cessPercent;
        dto.defaultMarkupPercent = product.defaultMarkupPercent;
        dto.unitPrice = product.unitPrice;
        dto.sellingPrice = product.sellingPrice;
        dto.quantity = product.quantity;
        dto.batchEnabled = product.batchEnabled;
        dto.createdAt = product.createdAt;
        dto.updatedAt = product.updatedAt;

        if (product.batches != null && !product.batches.isEmpty()) {
            dto.batchEnabled = true;
            dto.batches = new ArrayList<>();
            for (in.gbtsolutions.inventoryhub.models.ProductBatch b : product.batches) {
                if (b == null) continue;
                Integer batchId = b.batchId > 0 ? Integer.valueOf(b.batchId) : null;
                Integer productId = b.productId > 0 ? Integer.valueOf(b.productId) : (dto.productId > 0 ? Integer.valueOf(dto.productId) : null);
                Long expiryDate = b.expiryDate > 0 ? Long.valueOf(b.expiryDate) : null;
                double purchasePrice = b.purchasePrice > 0 ? b.purchasePrice : product.unitPrice;
                double sellingPrice = b.sellingPrice > 0 ? b.sellingPrice : product.sellingPrice;
                OnlineProductDto.OnlineBatchDto bDto = new OnlineProductDto.OnlineBatchDto(
                        batchId, productId, b.batchNo, b.quantity, purchasePrice, sellingPrice, expiryDate
                );
                dto.batches.add(bDto);
            }
            // Populate primary batch fields for backward compatibility
            in.gbtsolutions.inventoryhub.models.ProductBatch first = product.batches.get(0);
            dto.batchId = first.batchId > 0 ? Integer.valueOf(first.batchId) : null;
            dto.batchNo = first.batchNo;
            dto.batchQuantity = first.quantity;
            dto.expiryDate = first.expiryDate > 0 ? Long.valueOf(first.expiryDate) : null;
        } else if (product.batch != null) {
            dto.batchEnabled = true;
            Integer batchId = product.batch.batchId > 0 ? Integer.valueOf(product.batch.batchId) : null;
            Integer productId = product.productId > 0 ? Integer.valueOf(product.productId) : (dto.productId > 0 ? Integer.valueOf(dto.productId) : null);
            Long expiryDate = product.batch.expiryDate > 0 ? Long.valueOf(product.batch.expiryDate) : null;
            double purchasePrice = product.batch.purchasePrice > 0 ? product.batch.purchasePrice : product.unitPrice;
            double sellingPrice = product.batch.sellingPrice > 0 ? product.batch.sellingPrice : product.sellingPrice;
            dto.batchId = batchId;
            dto.batchNo = product.batch.batchNo;
            dto.batchQuantity = product.batch.quantity;
            dto.expiryDate = expiryDate;

            dto.batches = new ArrayList<>();
            dto.batches.add(new OnlineProductDto.OnlineBatchDto(
                    batchId, productId, product.batch.batchNo,
                    product.batch.quantity, purchasePrice, sellingPrice,
                    expiryDate
            ));
        }

        return dto;
    }

    public static List<Product> toDomainList(List<OnlineProductDto> dtos) {
        List<Product> list = new ArrayList<>();
        if (dtos != null) {
            for (OnlineProductDto dto : dtos) {
                Product p = toDomain(dto);
                if (p != null) {
                    list.add(p);
                }
            }
        }
        return list;
    }
}
