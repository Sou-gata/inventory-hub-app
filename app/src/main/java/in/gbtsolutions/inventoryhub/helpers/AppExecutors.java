package in.gbtsolutions.inventoryhub.helpers;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Global executor pools for the whole application.
 * Centralizes thread creation to avoid thread proliferation, excessive native stack allocations,
 * and unmanaged executor leaks.
 */
public class AppExecutors {

    private static final Object LOCK = new Object();
    private static volatile AppExecutors sInstance;

    private final ExecutorService diskIO;
    private final ExecutorService networkIO;
    private final Executor mainThread;

    private AppExecutors(ExecutorService diskIO, ExecutorService networkIO, Executor mainThread) {
        this.diskIO = diskIO;
        this.networkIO = networkIO;
        this.mainThread = mainThread;
    }

    public static AppExecutors getInstance() {
        if (sInstance == null) {
            synchronized (LOCK) {
                if (sInstance == null) {
                    sInstance = new AppExecutors(
                            Executors.newFixedThreadPool(4),
                            Executors.newFixedThreadPool(4),
                            new MainThreadExecutor()
                    );
                }
            }
        }
        return sInstance;
    }

    public ExecutorService diskIO() {
        return diskIO;
    }

    public ExecutorService networkIO() {
        return networkIO;
    }

    public Executor mainThread() {
        return mainThread;
    }

    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
}

