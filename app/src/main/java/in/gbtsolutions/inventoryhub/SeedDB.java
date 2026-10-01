package in.gbtsolutions.inventoryhub;

import android.content.Context;
import android.os.Looper;
import android.util.Log;

import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.helpers.HelperMethods;
import in.gbtsolutions.inventoryhub.models.Buyer;
import in.gbtsolutions.inventoryhub.models.Category;
import in.gbtsolutions.inventoryhub.models.Config;
import in.gbtsolutions.inventoryhub.models.Product;
import in.gbtsolutions.inventoryhub.models.Suppliers;
import in.gbtsolutions.inventoryhub.models.User;

public class SeedDB {

    private static final String TAG = "SeedDB";

    public static void seedDB(Context context) {
        if (context == null) return;
        Context appContext = context.getApplicationContext();

        if (Looper.myLooper() == Looper.getMainLooper()) {
            Executors.newSingleThreadExecutor().execute(() -> doSeed(appContext));
        } else {
            doSeed(appContext);
        }
    }

    private static void doSeed(Context context) {
        try {
            Database db = Database.getInstance(context);

            seedUsers(db);
            seedCategoriesAndProducts(db);
            seedBuyers(db);
            seedSuppliers(db);
            seedConfigs(db);

            Log.d(TAG, "Database seeding completed successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Error seeding database: " + e.getMessage(), e);
        }
    }

    private static void seedUsers(Database db) {
        if (db.userDao().count() == 0) {
            User admin = new User(
                    "Sougata Talukdar",
                    "sougata",
                    HelperMethods.hashPassword("12345678"),
                    "sougatatalukdar77@gmail.com",
                    "7797454561",
                    "admin",
                    true
            );

            User staff = new User(
                    "Sudipto Dutta",
                    "user",
                    HelperMethods.hashPassword("12345678"),
                    "sudiptodutta@gmail.com",
                    "9876543210",
                    "user",
                    true
            );

            User staff2 = new User(
                    "Ananya Roy",
                    "ananya",
                    HelperMethods.hashPassword("12345678"),
                    "ananya.roy@inventoryhub.in",
                    "9830012345",
                    "user",
                    true
            );

            db.userDao().insert(admin);
            db.userDao().insert(staff);
            db.userDao().insert(staff2);
            Log.d(TAG, "Seeded initial users.");
        }
    }

    private static void seedCategoriesAndProducts(Database db) {
        long now = System.currentTimeMillis();

        int catElectronicsId;
        int catFurnitureId;
        int catStationeryId;
        int catNetworkingId;
        int catToolsId;

        if (db.categoryDao().count() == 0) {
            catElectronicsId = (int) db.categoryDao().insert(new Category(
                    "Electronics & IT",
                    "Computers, peripherals, monitors, and smart office devices",
                    "electronics"
            ));
            catFurnitureId = (int) db.categoryDao().insert(new Category(
                    "Office Furniture",
                    "Ergonomic chairs, sit-stand desks, and conference tables",
                    "furniture"
            ));
            catStationeryId = (int) db.categoryDao().insert(new Category(
                    "Stationery & Paper",
                    "Multipurpose paper, notebooks, pens, and desk supplies",
                    "stationery"
            ));
            catNetworkingId = (int) db.categoryDao().insert(new Category(
                    "Networking & Cables",
                    "Ethernet cables, routers, switches, and high-speed adapters",
                    "networking"
            ));
            catToolsId = (int) db.categoryDao().insert(new Category(
                    "Industrial Tools",
                    "Power tools, safety equipment, and precision maintenance hardware",
                    "hardware"
            ));
            Log.d(TAG, "Seeded initial categories.");
        } else {
            catElectronicsId = 1;
            catFurnitureId = 2;
            catStationeryId = 3;
            catNetworkingId = 4;
            catToolsId = 5;
        }

        if (db.productDao().count() == 0) {
            Product p1 = new Product(
                    "Logitech MX Master 3S Wireless Mouse",
                    catElectronicsId,
                    "ELEC-LOGI-MX3S",
                    "Quiet-click ergonomic performance wireless mouse with 8K DPI tracking sensor.",
                    "Logitech",
                    "Pcs",
                    5,
                    15,
                    "Active",
                    "84716060",
                    18.0,
                    25.0,
                    now,
                    now,
                    7200.0,
                    8999.0,
                    24,
                    false
            );

            Product p2 = new Product(
                    "Dell UltraSharp 27 4K USB-C Monitor",
                    catElectronicsId,
                    "ELEC-DELL-U2723QE",
                    "27-inch 4K IPS Black monitor with 90W USB-C hub connectivity and HDR 400.",
                    "Dell",
                    "Unit",
                    3,
                    10,
                    "Active",
                    "85285200",
                    18.0,
                    20.0,
                    now,
                    now,
                    32000.0,
                    38400.0,
                    12,
                    false
            );

            Product p3 = new Product(
                    "Keychron K2 V2 Wireless Mechanical Keyboard",
                    catElectronicsId,
                    "ELEC-KEY-K2V2",
                    "75% compact Bluetooth mechanical keyboard with hot-swappable Gateron Brown switches.",
                    "Keychron",
                    "Pcs",
                    4,
                    12,
                    "Active",
                    "84716040",
                    18.0,
                    22.0,
                    now,
                    now,
                    6500.0,
                    7999.0,
                    18,
                    false
            );

            Product p4 = new Product(
                    "Ergonomic High-Back Mesh Desk Chair",
                    catFurnitureId,
                    "FURN-ERGO-CH01",
                    "Breathable mesh ergonomic chair with adjustable lumbar support, 3D arms, and tilt lock.",
                    "Green Soul",
                    "Unit",
                    2,
                    5,
                    "Active",
                    "94013000",
                    18.0,
                    30.0,
                    now,
                    now,
                    9500.0,
                    12350.0,
                    8,
                    false
            );

            Product p5 = new Product(
                    "Motorized Dual-Motor Height Adjustable Desk",
                    catFurnitureId,
                    "FURN-SITSTAND-D01",
                    "Electric sit-stand desk (140x70cm) with 4-memory digital display and anti-collision sensor.",
                    "ErgoPlus",
                    "Unit",
                    2,
                    5,
                    "Active",
                    "94031090",
                    18.0,
                    25.0,
                    now,
                    now,
                    22000.0,
                    27500.0,
                    4,
                    false
            );

            Product p6 = new Product(
                    "JK Copier A4 Paper 75 GSM (Carton of 5 Reams)",
                    catStationeryId,
                    "STAT-JK-A475",
                    "Premium multipurpose 75 GSM A4 copy paper carton (2500 sheets total) for sharp prints.",
                    "JK Paper",
                    "Box",
                    10,
                    50,
                    "Active",
                    "48025610",
                    12.0,
                    15.0,
                    now,
                    now,
                    1350.0,
                    1550.0,
                    40,
                    false
            );

            Product p7 = new Product(
                    "Cat6 UTP Gigabit Patch Cable 10m",
                    catNetworkingId,
                    "NET-CAT6-10M",
                    "High speed snagless RJ45 1000 Mbps molded ethernet patch cord with gold plated pins.",
                    "D-Link",
                    "Pcs",
                    15,
                    40,
                    "Active",
                    "85444299",
                    18.0,
                    35.0,
                    now,
                    now,
                    180.0,
                    249.0,
                    55,
                    false
            );

            Product p8 = new Product(
                    "Bosch Professional GSB 120-LI Cordless Drill",
                    catToolsId,
                    "TOOL-BOSCH-GSB120",
                    "12V cordless 2-speed impact driver and drill kit with two 2.0Ah lithium-ion battery packs.",
                    "Bosch",
                    "Set",
                    3,
                    8,
                    "Active",
                    "84672100",
                    18.0,
                    20.0,
                    now,
                    now,
                    5200.0,
                    6240.0,
                    7,
                    false
            );

            db.productDao().insert(p1);
            db.productDao().insert(p2);
            db.productDao().insert(p3);
            db.productDao().insert(p4);
            db.productDao().insert(p5);
            db.productDao().insert(p6);
            db.productDao().insert(p7);
            db.productDao().insert(p8);
            Log.d(TAG, "Seeded initial products.");
        }
    }

    private static void seedBuyers(Database db) {
        if (db.buyerDao().count() == 0) {
            Buyer b1 = new Buyer(
                    "Apex Technologies Pvt Ltd",
                    "Rajesh Sharma",
                    "9820154321",
                    "purchase@apextech.co.in",
                    "Plot 42, Sector 18, Electronics City Phase 1",
                    "Gurugram",
                    "HR",
                    "122015",
                    "India",
                    "06AAACA1234A1Z5",
                    "AAACA1234A",
                    "Key corporate IT client - Net 30 payment terms.",
                    true
            );

            Buyer b2 = new Buyer(
                    "Blue Horizon Logistics",
                    "Priya Menon",
                    "9845112233",
                    "procurement@bluehorizon.in",
                    "15 Marine Lines, Nariman Point",
                    "Mumbai",
                    "MH",
                    "400021",
                    "India",
                    "27BBBPC5678B1Z2",
                    "BBBPC5678B",
                    "Supply chain logistics partner - Monthly recurring orders.",
                    true
            );

            Buyer b3 = new Buyer(
                    "Kolkata Workspace Hub",
                    "Amitava Banerjee",
                    "9831098765",
                    "admin@kolkataworkspace.com",
                    "Block EP & GP, Sector V, Salt Lake",
                    "Kolkata",
                    "WB",
                    "700091",
                    "India",
                    "19CCDDE9012C1Z8",
                    "CCDDE9012C",
                    "Co-working facilities manager - Bulk furniture & tech buyer.",
                    true
            );

            db.buyerDao().insert(b1);
            db.buyerDao().insert(b2);
            db.buyerDao().insert(b3);
            Log.d(TAG, "Seeded initial buyers.");
        }
    }

    private static void seedSuppliers(Database db) {
        if (db.supplierDao().count() == 0) {
            Suppliers s1 = new Suppliers(
                    "Zenith Infotech Distributors",
                    "Vikram Singhania",
                    "9811223344",
                    "orders@zenithinfotech.com",
                    "Shop 104-106, Nehru Place IT Complex",
                    "New Delhi",
                    "DL",
                    "110019",
                    "India",
                    "07AAEFZ1122D1Z3",
                    "AAEFZ1122D",
                    "Authorized regional distributor for Logitech, Dell & Keychron.",
                    true
            );

            Suppliers s2 = new Suppliers(
                    "Vanguard Ergonomic Solutions",
                    "Neha Kapoor",
                    "9871100223",
                    "sales@vanguardoffice.in",
                    "Plot 78, Peenya Industrial Area, Phase 2",
                    "Bengaluru",
                    "KA",
                    "560058",
                    "India",
                    "29BBDFV3344E1Z7",
                    "BBDFV3344E",
                    "Direct OEM manufacturer for office desks and high-back mesh chairs.",
                    true
            );

            Suppliers s3 = new Suppliers(
                    "Metro Industrial Hardware Corp",
                    "Harish Patel",
                    "9723344556",
                    "harish@metrohardware.com",
                    "GIDC Industrial Estate, Makarpura",
                    "Vadodara",
                    "GJ",
                    "390010",
                    "India",
                    "24CCFGH5566F1Z1",
                    "CCFGH5566F",
                    "Wholesale distributor for Bosch industrial tools and cables.",
                    true
            );

            db.supplierDao().insert(s1);
            db.supplierDao().insert(s2);
            db.supplierDao().insert(s3);
            Log.d(TAG, "Seeded initial suppliers/sellers.");
        }
    }

    private static void seedConfigs(Database db) {
        if (db.configDao().count() == 0) {
            db.configDao().insert(new Config("company_name", "M/S. Lokenath Traders"));
            db.configDao().insert(new Config("address", "246/2, R. N. Tagore Road (3rd Floor), Kolkata-700 077"));
            db.configDao().insert(new Config("district", "Kolkata"));
            db.configDao().insert(new Config("state", "West Bengal"));
            db.configDao().insert(new Config("postal_code", "700077"));
            db.configDao().insert(new Config("gst_number", "19AFIPD6386B1ZG"));
            db.configDao().insert(new Config("pan_number", "AFIPD6386B"));
            db.configDao().insert(new Config("company_phone", "7797454562"));
            db.configDao().insert(new Config(Configurations.KEY_SHOW_PAYMENT_METHOD_DIALOG, "false"));
            db.configDao().insert(new Config(Configurations.KEY_UPI_ID, ""));
            db.configDao().insert(new Config(Configurations.KEY_SAVE_BILL_TO_GALLERY, "true"));
            db.configDao().insert(new Config(Configurations.KEY_PRINT_BILL, "false"));
            Log.d(TAG, "Seeded initial configs.");
        }
    }
}
