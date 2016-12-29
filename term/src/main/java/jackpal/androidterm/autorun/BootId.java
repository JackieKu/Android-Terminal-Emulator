package jackpal.androidterm.autorun;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;

import jackpal.androidterm.App;

class BootId {
    private static final String KEY_BOOT_ID = "boot_id";
    private static final String KEY_BOOT_TIME = "boot_time";

    private static final long TIME_THRESHOLD_NS = 300_000_000_000L;

    private static final String KERNEL_BOOT_ID = onErrorDefault(BootId::getKernelBootId, "");
    private static boolean sIsBooted;

    public static boolean isBooted() {
        if (sIsBooted) // Luckily the process keeps running since last time we checked.
            return true;

        try {
            String bootId = getKernelBootId();
            if (!bootId.equals(getPreferences().getString(KEY_BOOT_ID, "N/A")))
                return false; // Absolutely there was a hot reset
        } catch (Throwable ignored) {
        }

        long d = System.currentTimeMillis() - getPreferences().getLong(KEY_BOOT_TIME, 0);
        return d >= 0 && d < TIME_THRESHOLD_NS;
    }

    public static void setBooted() {
        sIsBooted = true;
        getPreferences().edit()
            .putString(KEY_BOOT_ID, KERNEL_BOOT_ID)
            .putLong(KEY_BOOT_TIME, System.currentTimeMillis())
            .commit();
    }

    @VisibleForTesting
    @NonNull
    static String getKernelBootId() throws IOException {
        String id = Files.asCharSource(new File("/proc/sys/kernel/random/boot_id"), StandardCharsets.US_ASCII).readFirstLine();
        if (TextUtils.isEmpty(id))
            throw new IOException("boot_id is null or empty.");
        return id;
    }

    private static <T> T onErrorDefault(Callable<? extends T> func, T defaultValue) {
        try {
            return func.call();
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    private static SharedPreferences getPreferences() {
        return App.getInstance().getSharedPreferences("BootId.xml", Context.MODE_PRIVATE);
    }

    private BootId() {
    }
}
