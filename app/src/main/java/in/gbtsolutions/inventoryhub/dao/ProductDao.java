package in.gbtsolutions.inventoryhub.dao;

import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;


import java.util.List;

import in.gbtsolutions.inventoryhub.models.Product;

@Dao
public interface ProductDao {

    @Insert
    long insert(Product product);

    @Insert
    void insertAll(List<Product> products);

    @Update
    void update(Product product);

    @Delete
    void delete(Product product);

    @Query("SELECT * FROM products WHERE product_id = :productId")
    Product getProductById(int productId);

    @Query("SELECT * FROM products WHERE sku IS NOT NULL AND TRIM(sku) != '' AND UPPER(TRIM(sku)) = UPPER(TRIM(:sku)) LIMIT 1")
    Product getProductBySku(String sku);

    @Query("SELECT * FROM products WHERE hsn_code IS NOT NULL AND TRIM(hsn_code) != '' AND UPPER(TRIM(hsn_code)) = UPPER(TRIM(:hsnCode)) LIMIT 1")
    Product getProductByHsn(String hsnCode);

    @Query("SELECT * FROM products ORDER BY product_name ASC")
    LiveData<List<Product>> getAllProducts();

    @Query("SELECT * FROM products WHERE category_id = :categoryId")
    LiveData<List<Product>> getProductsByCategory(int categoryId);

    @Query("SELECT * FROM products WHERE quantity <= reorder_level")
    LiveData<List<Product>> getProductsNeedingReorder();

    @Query("SELECT * FROM products WHERE product_name LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%'")
    LiveData<List<Product>> searchProducts(String query);

    @Query("SELECT * FROM products WHERE status = :status")
    LiveData<List<Product>> getProductsByStatus(String status);

    @Query("SELECT COUNT(*) FROM products")
    int count();

    @Query(
        "SELECT " +
        "p.product_name AS product_name, " +
        "p.sku AS sku, " +
        "c.category_name AS category_name, " +
        "p.brand AS brand, " +
        "p.description AS description, " +
        "p.unit_of_measure AS unit_of_measure, " +
        "p.unit_price AS unit_price, " +
        "p.selling_price AS selling_price, " +
        "p.quantity AS quantity, " +
        "p.reorder_level AS reorder_level, " +
        "p.reorder_quantity AS reorder_quantity, " +
        "p.hsn_code AS hsn_code, " +
        "p.gst_percent AS gst_percent, " +
        "p.default_markup_percent AS default_markup_percent, " +
        "p.status AS status, " +

        "b.batch_no AS batch_no, " +
        "b.quantity AS batch_quantity, " +
        "b.purchase_price AS batch_purchase_price, " +
        "b.selling_price AS batch_selling_price, " +
        "b.expiry_date AS batch_expiry_date " +

        "FROM products p " +

        "LEFT JOIN categories c " +
        "ON p.category_id = c.category_id " +

        "LEFT JOIN product_batches b " +
        "ON p.product_id = b.product_id " +

        "ORDER BY p.product_id ASC, b.batch_id ASC"
    )
    Cursor getProductExportCursor();

    @Query("SELECT * FROM products ORDER BY product_name ASC")
    List<Product> getAllProductsSync();

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    void upsert(Product product);
}