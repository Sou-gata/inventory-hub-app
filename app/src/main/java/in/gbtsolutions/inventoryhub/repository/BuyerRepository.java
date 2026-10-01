package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.BuyerDao;
import in.gbtsolutions.inventoryhub.models.Buyer;

public class BuyerRepository {
    private final BuyerDao buyerDao;
    private final LiveData<List<Buyer>> allBuyers;
    private final ExecutorService executorService;
    private final Application application;

    public BuyerRepository(Application application) {
        this.application = application;
        Database db = Database.getInstance(application);
        buyerDao = db.buyerDao();
        allBuyers = buyerDao.getAllBuyers();
        executorService = Executors.newFixedThreadPool(4);
    }

    public LiveData<List<Buyer>> getAllBuyers() {
        return allBuyers;
    }

    public LiveData<List<Buyer>> getActiveBuyers() {
        return buyerDao.getActiveBuyers();
    }

    public LiveData<List<Buyer>> searchBuyers(String query) {
        return buyerDao.searchBuyers(query);
    }

    public void insert(Buyer buyer) {
        executorService.execute(() -> {
            long id = buyerDao.insert(buyer);
            in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logAddition(application,
                    in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_BUYER,
                    buyer.buyerName,
                    "Added buyer: " + buyer.buyerName + " (Phone: " + (buyer.phone != null ? buyer.phone : "N/A") + ")");
        });
    }

    public void update(Buyer buyer) {
        executorService.execute(() -> {
            buyerDao.update(buyer);
            in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                    in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_BUYER,
                    buyer.buyerName,
                    "Updated buyer: " + buyer.buyerName + " (Phone: " + (buyer.phone != null ? buyer.phone : "N/A") + ")");
        });
    }

    public void updateStatus(int buyerId, boolean isActive) {
        executorService.execute(() -> {
            buyerDao.updateStatus(buyerId, isActive, System.currentTimeMillis());
            Buyer b = buyerDao.getBuyerById(buyerId);
            String name = b != null ? b.buyerName : ("ID:" + buyerId);
            in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                    in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_BUYER,
                    name,
                    (isActive ? "Activated" : "Deactivated") + " buyer: " + name);
        });
    }
}
