package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;
import android.database.Cursor;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.ProductDao;
import in.gbtsolutions.inventoryhub.models.Product;

import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.online.repository.OnlineProductRepository;

public class ProductRepository {

    private final ProductDao productDao;
    private final LiveData<List<Product>> allProducts;
    private final ExecutorService executorService;
    private final OnlineProductRepository onlineProductRepository;
    private final AppModeManager appModeManager;
    private final Application application;

    public ProductRepository(Application application) {
        this.application = application;
        Database db = Database.getInstance(application);
        productDao = db.productDao();
        allProducts = productDao.getAllProducts();
        executorService = Executors.newFixedThreadPool(4);
        onlineProductRepository = new OnlineProductRepository(application);
        appModeManager = AppModeManager.getInstance(application);
    }

    public boolean isOnlineMode() {
        return appModeManager.isOnlineMode();
    }

    public OnlineProductRepository getOnlineRepository() {
        return onlineProductRepository;
    }

    public LiveData<List<Product>> getAllProducts() {
        return allProducts;
    }

    public interface ProductInsertCallback {
        void onSuccess(long rowId);
        void onError(String message);
    }

    public void insert(Product product) {
        if (appModeManager.isOnlineMode()) {
            onlineProductRepository.createProduct(product, null);
        } else {
            executorService.execute(() -> {
                long id = productDao.insert(product);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logAddition(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_PRODUCT,
                        product.sku != null && !product.sku.isEmpty() ? product.sku : ("ID:" + id),
                        "Added product: " + product.productName + " (Price: ₹" + product.sellingPrice + ", Qty: " + product.quantity + ")");
            });
        }
    }

    public void insert(Product product, ProductInsertCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineProductRepository.createProduct(product, new OnlineProductRepository.ProductActionCallback() {
                @Override
                public void onSuccess(Product created) {
                    if (callback != null) {
                        callback.onSuccess(created != null ? created.productId : 0);
                    }
                }

                @Override
                public void onError(String message) {
                    if (callback != null) {
                        callback.onError(message);
                    }
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                if (product.sku != null && !product.sku.trim().isEmpty()) {
                    Product existing = productDao.getProductBySku(product.sku.trim());
                    if (existing != null) {
                        if (callback != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                                    callback.onError("A product with SKU '" + product.sku.trim() + "' already exists."));
                        }
                        return;
                    }
                }
                long id = productDao.insert(product);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logAddition(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_PRODUCT,
                        product.sku != null && !product.sku.isEmpty() ? product.sku : ("ID:" + id),
                        "Added product: " + product.productName + " (Price: ₹" + product.sellingPrice + ", Qty: " + product.quantity + ")");
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onSuccess(id));
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to save product."));
                }
            }
        });
    }

    public interface ProductUpdateCallback {
        void onSuccess();
        void onError(String message);
    }

    public void update(Product product) {
        if (appModeManager.isOnlineMode()) {
            onlineProductRepository.updateProduct(product, null);
        } else {
            executorService.execute(() -> {
                productDao.update(product);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_PRODUCT,
                        product.sku != null && !product.sku.isEmpty() ? product.sku : ("ID:" + product.productId),
                        "Updated product: " + product.productName + " (Price: ₹" + product.sellingPrice + ", Qty: " + product.quantity + ")");
            });
        }
    }

    public void update(Product product, ProductUpdateCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineProductRepository.updateProduct(product, new OnlineProductRepository.ProductActionCallback() {
                @Override
                public void onSuccess(Product updated) {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                }

                @Override
                public void onError(String message) {
                    if (callback != null) {
                        callback.onError(message);
                    }
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                if (product.sku != null && !product.sku.trim().isEmpty()) {
                    Product existing = productDao.getProductBySku(product.sku.trim());
                    if (existing != null && existing.productId != product.productId) {
                        if (callback != null) {
                            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                                    callback.onError("Another product with SKU '" + product.sku.trim() + "' already exists."));
                        }
                        return;
                    }
                }
                productDao.update(product);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_PRODUCT,
                        product.sku != null && !product.sku.isEmpty() ? product.sku : ("ID:" + product.productId),
                        "Updated product: " + product.productName + " (Price: ₹" + product.sellingPrice + ", Qty: " + product.quantity + ")");
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to update product."));
                }
            }
        });
    }

    public interface ProductDeleteCallback {
        void onSuccess();
        void onError(String message);
    }

    public void delete(Product product) {
        delete(product, null);
    }

    public void delete(Product product, ProductDeleteCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineProductRepository.deleteProduct(product, new OnlineProductRepository.ProductActionCallback() {
                @Override
                public void onSuccess(Product p) {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                }

                @Override
                public void onError(String message) {
                    if (callback != null) {
                        callback.onError(message);
                    }
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                productDao.delete(product);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logDeletion(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_PRODUCT,
                        product.sku != null && !product.sku.isEmpty() ? product.sku : ("ID:" + product.productId),
                        "Deleted product: " + product.productName);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to delete product."));
                }
            }
        });
    }

    public void fetchProductsPaged(int page, int limit, String search, String stockStatus,
                                   Integer categoryId, OnlineProductRepository.PagedProductCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineProductRepository.fetchProducts(page, limit, search, stockStatus, categoryId, callback);
        }
    }

    public void getProductById(int productId, OnlineProductRepository.ProductActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineProductRepository.getProductById(productId, callback);
        } else {
            executorService.execute(() -> {
                Product product = productDao.getProductById(productId);
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        if (product != null) {
                            callback.onSuccess(product);
                        } else {
                            callback.onError("Product not found in local database");
                        }
                    }
                });
            });
        }
    }

    public Cursor getProductExportCursor() {
        return productDao.getProductExportCursor();
    }
}
