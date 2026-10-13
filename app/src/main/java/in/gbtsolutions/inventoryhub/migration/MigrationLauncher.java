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

    /**
     * Enqueues the one-time migration worker uniquely so multiple concurrent migrations cannot run.
     */
    public static LiveData<WorkInfo> startMigration(Context context) {
        OneTimeWorkRequest migrationRequest = new OneTimeWorkRequest.Builder(MigrationWorker.class)
                .addTag(UNIQUE_WORK_NAME)
                .build();

        WorkManager.getInstance(context.getApplicationContext())
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, migrationRequest);

        return WorkManager.getInstance(context.getApplicationContext())
                .getWorkInfoByIdLiveData(migrationRequest.getId());
    }

    /**
     * Retrieves the current LiveData for observing an active or completed migration.
     */
    public static LiveData<WorkInfo> getActiveMigrationLiveData(Context context) {
        return Transformations.map(
                WorkManager.getInstance(context.getApplicationContext())
                        .getWorkInfosForUniqueWorkLiveData(UNIQUE_WORK_NAME),
                workInfos -> (workInfos != null && !workInfos.isEmpty()) ? workInfos.get(0) : null
        );
    }
}




// import in.gbtsolutions.inventoryhub.migration.MigrationLauncher;
// import in.gbtsolutions.inventoryhub.migration.MigrationWorker;

// // Trigger migration and observe progress
// MigrationLauncher.startMigration(this).observe(this, workInfo -> {
//     if (workInfo != null) {
//         if (workInfo.getState() == WorkInfo.State.RUNNING) {
//             String progressMsg = workInfo.getProgress().getString(MigrationWorker.KEY_PROGRESS);
//             if (progressMsg != null) {
//                 // Update your ProgressBar or Status TextView
//                 statusTextView.setText(progressMsg);
//             }
//         } else if (workInfo.getState() == WorkInfo.State.SUCCEEDED) {
//             Toast.makeText(this, "Successfully migrated to Online mode!", Toast.LENGTH_LONG).show();
//             // App is now in ONLINE mode; navigate to main dashboard
//         } else if (workInfo.getState() == WorkInfo.State.FAILED) {
//             String errorMsg = workInfo.getOutputData().getString(MigrationWorker.KEY_ERROR);
//             Toast.makeText(this, "Migration failed: " + errorMsg, Toast.LENGTH_LONG).show();
//         }
//     }
// });
