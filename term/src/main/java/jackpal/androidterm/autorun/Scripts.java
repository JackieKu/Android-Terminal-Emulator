package jackpal.androidterm.autorun;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import jackpal.androidterm.App;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

public class Scripts {
    private static final String TAG = "Scripts";

    public static final Scripts EMPTY = new Scripts();

    public static final File AUTORUN_DIR = App.getInstance().getDir("autorun", 0);

    private final File mDir;
    private final List<Script> mScripts;

    public static Single<Scripts> forAutoRun() {
        return Single.fromCallable(Scripts::getAutoRun)
            .subscribeOn(Schedulers.io());
    }

    public static Scripts getAutoRun() {
        Log.v(TAG, "AUTORUN_DIR: " + AUTORUN_DIR);
        AUTORUN_DIR.mkdirs();
        return new Scripts(AUTORUN_DIR);
    }

    public List<Script> list() {
        return mScripts;
    }

    public static class RunResult {
        public static final int EXIT_TIMEOUT = 0xFFFFFFFE;
        public static final int EXIT_EXECUTION_ERROR = 0xFFFFFFFF;

        public final Script script;
        public final int exitCode;

        public RunResult(Script script, int exitCode) {
            this.script = script;
            this.exitCode = exitCode;
        }
    }

    public Observable<RunResult> runAll() {
        return Observable.from(list())
            .filter(Script::isEnabled)
            .concatMap(script -> script.run()
                .flatMap(session -> session.exitCode)
                .onErrorReturn(e -> {
                    Log.e(TAG, "Cannot run " + script, e);
                    return RunResult.EXIT_EXECUTION_ERROR;
                })
                .timeout(8, TimeUnit.SECONDS, Single.just(RunResult.EXIT_TIMEOUT))
                .map(exitCode -> new RunResult(script, exitCode))
                .toObservable()
            )
            ;
    }

    private Scripts() {
        mDir = null;
        mScripts = Collections.emptyList();
    }

    private Scripts(File dir) {
        mDir = Objects.requireNonNull(dir);
        mScripts = new ArrayList<>();
        File[] files = dir.listFiles(file -> file.isFile() && file.getName().endsWith(Script.EXTENSION));
        if (files != null) {
            for (File f : files)
                mScripts.add(new Script(f));
            Collections.sort(mScripts);
        }
    }
}
