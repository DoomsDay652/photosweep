package com.dominic.photosweep;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.Lifecycle;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.appupdate.AppUpdateOptions;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;

/** Play-owned consent/download flow; unavailable Play services never block photo review. */
final class PlayUpdateController implements AutoCloseable {
    private static final String PROMPTED_VERSION = "play_update_prompted_version";
    private final ComponentActivity activity;
    private final AppUpdateManager manager;
    private final ActivityResultLauncher<IntentSenderRequest> launcher;
    private final InstallStateUpdatedListener listener;
    private int promptedVersion;
    private boolean querying, flowActive, closed, restartOffered;
    private AlertDialog restartDialog;

    PlayUpdateController(ComponentActivity activity, Bundle savedState) {
        this.activity = activity;
        promptedVersion = savedState == null ? -1 : savedState.getInt(PROMPTED_VERSION, -1);
        manager = AppUpdateManagerFactory.create(activity);
        // Register before STARTED, using the same registration order after rotation.
        launcher = activity.registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(), result -> {
                    flowActive = false;
                    // A decline/failure is respected until the next app launch.
                    // onResume checks for a completed background download separately.
                });
        listener = state -> {
            if (state.installStatus() == InstallStatus.DOWNLOADED) offerRestart();
        };
        manager.registerListener(listener);
    }

    void onResume() {
        if (closed || querying || flowActive) return;
        querying = true;
        manager.getAppUpdateInfo().addOnCompleteListener(task -> {
            querying = false;
            if (!isVisible() || !task.isSuccessful()) return;
            AppUpdateInfo info = task.getResult();
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                offerRestart();
            } else if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                start(info, AppUpdateType.IMMEDIATE);
            } else if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && info.availableVersionCode() > BuildConfig.VERSION_CODE
                    && promptedVersion != info.availableVersionCode()
                    && info.installStatus() != InstallStatus.DOWNLOADING
                    && info.installStatus() != InstallStatus.PENDING) {
                int type = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) ? AppUpdateType.FLEXIBLE
                        : info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ? AppUpdateType.IMMEDIATE : -1;
                if (type != -1) {
                    promptedVersion = info.availableVersionCode();
                    start(info, type);
                }
            }
        });
    }

    private void start(AppUpdateInfo info, int type) {
        if (!isVisible() || flowActive) return;
        flowActive = true;
        try {
            if (!manager.startUpdateFlowForResult(info, launcher,
                    AppUpdateOptions.newBuilder(type).build())) flowActive = false;
        } catch (RuntimeException error) {
            flowActive = false;
            Log.w("PhotoSweepUpdate", "Play update prompt could not start", error);
        }
    }

    private void offerRestart() {
        if (!isVisible() || restartOffered) return;
        restartOffered = true;
        restartDialog = new AlertDialog.Builder(activity)
                .setTitle("Update ready")
                .setMessage("Restart Photo Sweep to finish updating.")
                .setPositiveButton("Restart", (dialog, which) -> manager.completeUpdate()
                        .addOnFailureListener(error -> {
                            restartOffered = false;
                            if (isVisible()) Toast.makeText(activity,
                                    "Could not install the update. Try again later.", Toast.LENGTH_SHORT).show();
                        }))
                .setNegativeButton("Later", null)
                .create();
        restartDialog.setOnDismissListener(dialog -> restartDialog = null);
        restartDialog.show();
    }

    private boolean isVisible() {
        return !closed && !activity.isFinishing() && !activity.isDestroyed()
                && activity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED);
    }

    boolean isFlowActive() { return flowActive; }

    // When the host returns, query fresh info to resume an interrupted immediate flow.
    void onPause() { flowActive = false; }

    void saveState(Bundle state) { state.putInt(PROMPTED_VERSION, promptedVersion); }

    @Override public void close() {
        closed = true;
        manager.unregisterListener(listener);
        if (restartDialog != null) restartDialog.dismiss();
    }
}
