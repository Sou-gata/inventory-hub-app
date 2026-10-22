package in.gbtsolutions.inventoryhub.migration;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import androidx.lifecycle.Transformations;

public class MigrationLauncher {

    public static final String UNIQUE_WORK_NAME = "offline_to_online_migration_work";

    // Enqueues the one-time migration worker uniquely so multiple concurrent migrations cannot run.
    public static LiveData<WorkInfo> startMigration(Context context) {
        OneTimeWorkRequest migrationRequest = new OneTimeWorkRequest.Builder(MigrationWorker.class).addTag(UNIQUE_WORK_NAME).build();

        WorkManager.getInstance(context.getApplicationContext()).enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, migrationRequest);

        return WorkManager.getInstance(context.getApplicationContext()).getWorkInfoByIdLiveData(migrationRequest.getId());
    }

    // Retrieves the current LiveData for observing an active or completed migration.
    public static LiveData<WorkInfo> getActiveMigrationLiveData(Context context) {
        return Transformations.map(WorkManager.getInstance(context.getApplicationContext()).getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_NAME), workInfos -> (workInfos != null && !workInfos.isEmpty()) ? workInfos.get(0) : null);
    }
}

