package in.gbtsolutions.inventoryhub.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import in.gbtsolutions.inventoryhub.Database;
import in.gbtsolutions.inventoryhub.dao.BuyerDao;
import in.gbtsolutions.inventoryhub.models.Buyer;
import in.gbtsolutions.inventoryhub.online.config.AppModeManager;
import in.gbtsolutions.inventoryhub.online.repository.OnlineBuyerRepository;

public class BuyerRepository {
    private final BuyerDao buyerDao;
    private final LiveData<List<Buyer>> allBuyers;
    private final ExecutorService executorService;
    private final Application application;
    private final OnlineBuyerRepository onlineBuyerRepository;
    private final AppModeManager appModeManager;

    public interface BuyerActionCallback {
        void onSuccess();
        void onError(String message);
    }

    public BuyerRepository(Application application) {
        this.application = application;
        Database db = Database.getInstance(application);
        buyerDao = db.buyerDao();
        allBuyers = buyerDao.getAllBuyers();
        executorService = Executors.newFixedThreadPool(4);
        onlineBuyerRepository = new OnlineBuyerRepository(application);
        appModeManager = AppModeManager.getInstance(application);
    }

    public boolean isOnlineMode() {
        return appModeManager.isOnlineMode();
    }

    public OnlineBuyerRepository getOnlineRepository() {
        return onlineBuyerRepository;
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

    public void fetchBuyersOnline(OnlineBuyerRepository.BuyerListCallback callback) {
        fetchBuyersOnline(null, callback);
    }

    public void fetchBuyersOnline(String search, OnlineBuyerRepository.BuyerListCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineBuyerRepository.fetchBuyers(search, callback);
        }
    }

    public void insert(Buyer buyer) {
        insert(buyer, null);
    }

    public void insert(Buyer buyer, BuyerActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineBuyerRepository.createBuyer(buyer, new OnlineBuyerRepository.BuyerActionCallback() {
                @Override
                public void onSuccess(Buyer b) {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) callback.onError(errorMessage);
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                long id = buyerDao.insert(buyer);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logAddition(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_BUYER,
                        buyer.buyerName,
                        "Added buyer: " + buyer.buyerName + " (Phone: " + (buyer.phone != null ? buyer.phone : "N/A") + ")");
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        String msg = e.getMessage() != null ? e.getMessage() : "Failed to add buyer.";
                        if (e instanceof android.database.sqlite.SQLiteConstraintException) {
                            msg = "A buyer with this phone number, email, or GST already exists.";
                        }
                        callback.onError(msg);
                    });
                }
            }
        });
    }

    public void update(Buyer buyer) {
        update(buyer, null);
    }

    public void update(Buyer buyer, BuyerActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineBuyerRepository.updateBuyer(buyer, new OnlineBuyerRepository.BuyerActionCallback() {
                @Override
                public void onSuccess(Buyer b) {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) callback.onError(errorMessage);
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                buyerDao.update(buyer);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_BUYER,
                        buyer.buyerName,
                        "Updated buyer: " + buyer.buyerName + " (Phone: " + (buyer.phone != null ? buyer.phone : "N/A") + ")");
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        String msg = e.getMessage() != null ? e.getMessage() : "Failed to update buyer.";
                        if (e instanceof android.database.sqlite.SQLiteConstraintException) {
                            msg = "A buyer with this phone number, email, or GST already exists.";
                        }
                        callback.onError(msg);
                    });
                }
            }
        });
    }

    public void delete(int buyerId, BuyerActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            onlineBuyerRepository.deleteBuyer(buyerId, new OnlineBuyerRepository.BuyerDeleteCallback() {
                @Override
                public void onSuccess() {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) callback.onError(errorMessage);
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                Buyer b = buyerDao.getBuyerById(buyerId);
                if (b != null) {
                    buyerDao.delete(b);
                }
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to delete buyer."));
                }
            }
        });
    }

    public void updateStatus(int buyerId, boolean isActive) {
        updateStatus(buyerId, isActive, null);
    }

    public void updateStatus(int buyerId, boolean isActive, BuyerActionCallback callback) {
        if (appModeManager.isOnlineMode()) {
            Buyer buyer = new Buyer();
            buyer.buyerId = buyerId;
            buyer.isActive = isActive;
            onlineBuyerRepository.updateBuyer(buyer, new OnlineBuyerRepository.BuyerActionCallback() {
                @Override
                public void onSuccess(Buyer b) {
                    if (callback != null) callback.onSuccess();
                }

                @Override
                public void onError(String errorMessage) {
                    if (callback != null) callback.onError(errorMessage);
                }
            });
            return;
        }

        executorService.execute(() -> {
            try {
                buyerDao.updateStatus(buyerId, isActive, System.currentTimeMillis());
                Buyer b = buyerDao.getBuyerById(buyerId);
                String name = b != null ? b.buyerName : ("ID:" + buyerId);
                in.gbtsolutions.inventoryhub.helpers.AuditTrailHelper.logEdit(application,
                        in.gbtsolutions.inventoryhub.models.AuditTrail.MODULE_BUYER,
                        name,
                        (isActive ? "Activated" : "Deactivated") + " buyer: " + name);
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(callback::onSuccess);
                }
            } catch (Exception e) {
                if (callback != null) {
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            callback.onError(e.getMessage() != null ? e.getMessage() : "Failed to update buyer status."));
                }
            }
        });
    }
}
