package jackpal.androidterm.autorun;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import rx.Observable;

public class AutoRunService extends IntentService {
    private static final String TAG = "AutoRunService";

    public AutoRunService() {
        super(TAG);
    }

    public static void onBoot(Context context) {
        context.startService(new Intent(context, AutoRunService.class).setAction(Intent.ACTION_BOOT_COMPLETED));
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        switch (intent.getAction()) {
            case Intent.ACTION_BOOT_COMPLETED:
                startBootScripts();
                break;
        }
    }

    @VisibleForTesting
    static Observable<Scripts.RunResult> startBootScripts() {
        Observable<Scripts.RunResult> runners = Scripts.getAutoRun().runAll().cache();
        runners.subscribe(r -> {
            Log.d(TAG, r.script + " exit with " + r.exitCode);
        });
        return runners;
    }
}
