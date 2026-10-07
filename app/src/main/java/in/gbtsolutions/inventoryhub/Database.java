package in.gbtsolutions.inventoryhub;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import in.gbtsolutions.inventoryhub.dao.AuditTrailDao;
import in.gbtsolutions.inventoryhub.dao.BuyerDao;
import in.gbtsolutions.inventoryhub.dao.CategoryDao;
import in.gbtsolutions.inventoryhub.dao.ConfigDao;
import in.gbtsolutions.inventoryhub.dao.CreditDebitNoteDao;
import in.gbtsolutions.inventoryhub.dao.ProductBatchDao;
import in.gbtsolutions.inventoryhub.dao.ProductDao;
import in.gbtsolutions.inventoryhub.dao.PurchaseDao;
import in.gbtsolutions.inventoryhub.dao.PurchaseItemDao;
import in.gbtsolutions.inventoryhub.dao.ReceiveItemDao;
import in.gbtsolutions.inventoryhub.dao.ReceiveRecordDao;
import in.gbtsolutions.inventoryhub.dao.SaleDao;
import in.gbtsolutions.inventoryhub.dao.SaleItemDao;
import in.gbtsolutions.inventoryhub.dao.SupplierDao;
import in.gbtsolutions.inventoryhub.dao.UnitOfMeasureDao;
import in.gbtsolutions.inventoryhub.dao.UserDao;
import in.gbtsolutions.inventoryhub.models.AuditTrail;
import in.gbtsolutions.inventoryhub.models.Buyer;
import in.gbtsolutions.inventoryhub.models.Category;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.CreditDebitNote;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.ProductBatch;
import in.gbtsolutions.inventoryhub.models.Purchase;
import in.gbtsolutions.inventoryhub.models.PurchaseItem;
import in.gbtsolutions.inventoryhub.models.ReceiveItem;
import in.gbtsolutions.inventoryhub.models.ReceiveRecord;
import in.gbtsolutions.inventoryhub.models.Sale;
import in.gbtsolutions.inventoryhub.models.SaleItem;
import in.gbtsolutions.inventoryhub.models.Suppliers;
import in.gbtsolutions.inventoryhub.models.UnitOfMeasure;
import in.gbtsolutions.inventoryhub.models.User;

@androidx.room.Database(
        entities = {
                User.class,
                Product.class,
                ProductBatch.class,
                Category.class,
                Buyer.class,
                Suppliers.class,
                Sale.class,
                SaleItem.class,
                Config.class,
                Purchase.class,
                PurchaseItem.class,
                ReceiveRecord.class,
                ReceiveItem.class,
                CreditDebitNote.class,
                AuditTrail.class,
                UnitOfMeasure.class
        },
        version = 2,
        exportSchema = false
)
public abstract class Database extends RoomDatabase {
    private static volatile Database INSTANCE;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE sales ADD COLUMN cancelled_by INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE purchases ADD COLUMN cancelled_by INTEGER NOT NULL DEFAULT 0");
        }
    };

    public static Database getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (Database.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), Database.class, "app_database")
                            .addMigrations(MIGRATION_1_2)
                            .fallbackToDestructiveMigration()
                            .addCallback(new Callback() {
                                @Override
                                public void onCreate(@NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    SeedDB.seedDB(context.getApplicationContext());
                                }

                                @Override
                                public void onDestructiveMigration(@NonNull SupportSQLiteDatabase db) {
                                    super.onDestructiveMigration(db);
                                    SeedDB.seedDB(context.getApplicationContext());
                                }
                            })
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void invalidateInstance() {
        synchronized (Database.class) {
            if (INSTANCE != null) {
                if (INSTANCE.isOpen()) {
                    INSTANCE.close();
                }
                INSTANCE = null;
            }
        }
    }

    public abstract UserDao userDao();

    public abstract ProductDao productDao();

    public abstract CategoryDao categoryDao();

    public abstract BuyerDao buyerDao();

    public abstract SupplierDao supplierDao();

    public abstract SaleDao saleDao();

    public abstract SaleItemDao saleItemDao();

    public abstract ConfigDao configDao();

    public abstract ProductBatchDao productBatchDao();

    public abstract PurchaseDao purchaseDao();

    public abstract PurchaseItemDao purchaseItemDao();

    public abstract ReceiveRecordDao receiveRecordDao();

    public abstract ReceiveItemDao receiveItemDao();

    public abstract CreditDebitNoteDao creditDebitNoteDao();

    public abstract AuditTrailDao auditTrailDao();

    public abstract UnitOfMeasureDao unitOfMeasureDao();
}