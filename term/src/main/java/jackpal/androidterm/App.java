package jackpal.androidterm;

import android.app.Application;
import android.content.res.Configuration;
import android.preference.PreferenceManager;

import im.delight.android.languages.Language;

public class App extends Application {
    private static App sInstance;

    public static App getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        setupLanguage();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setupLanguage();
    }

    private void setupLanguage() {
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("use_english", false))
            Language.set(this, "en-rUS");
    }
}
