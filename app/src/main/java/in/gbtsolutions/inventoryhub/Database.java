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
                AuditTrail.class
        },
        version = 4,
        exportSchema = false
)
public abstract class Database extends RoomDatabase {

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE sales ADD COLUMN payment_method TEXT DEFAULT 'Cash'");
        }
    };

    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `audit_trails` (" +
                    "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "`action_type` TEXT, " +
                    "`module` TEXT, " +
                    "`record_id` TEXT, " +
                    "`details` TEXT, " +
                    "`performed_by` TEXT, " +
                    "`timestamp` INTEGER NOT NULL)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_trails_timestamp` ON `audit_trails` (`timestamp`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_trails_action_type` ON `audit_trails` (`action_type`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_audit_trails_module` ON `audit_trails` (`module`)");
        }
    };

    private static volatile Database INSTANCE;

    public static Database getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (Database.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(), Database.class, "app_database")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
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
}