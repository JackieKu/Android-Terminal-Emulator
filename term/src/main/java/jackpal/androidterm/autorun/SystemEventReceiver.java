package jackpal.androidterm.autorun;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class SystemEventReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case Intent.ACTION_BOOT_COMPLETED:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                    onBoot(context);
                break;
            case Intent.ACTION_LOCKED_BOOT_COMPLETED:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    onBoot(context);
                break;
        }
    }

    void onBoot(Context context) {
        if (BootId.isBooted()) return;
        BootId.setBooted();

        AutoRunService.onBoot(context);
    }
}
