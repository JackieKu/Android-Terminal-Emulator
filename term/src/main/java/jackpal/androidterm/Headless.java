package jackpal.androidterm;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

import jackpal.androidterm.emulatorview.TermSession;
import jackpal.androidterm.java8.Optional;
import jackpal.androidterm.util.TermSettings;
import jackpal.androidterm.util.Unchecked;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.subjects.AsyncSubject;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

import static android.content.Context.BIND_AUTO_CREATE;

public class Headless {
    private final static Headless INSTANCE = new Headless();

    private int mSessions;
    private TermService mService;

    private final PublishSubject<TermService> mServiceSubject = PublishSubject.create();
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            TermService.TSBinder binder = (TermService.TSBinder) service;
            mServiceSubject.onNext(mService = binder.getService());
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };

    public static Single<Headless> getInstance() {
        return Single.just(INSTANCE).observeOn(AndroidSchedulers.mainThread());
    }

    public SessionBuilder newSession(String cmdLine) {
        return new SessionBuilder(cmdLine);
    }

    public class SessionBuilder {
        private final String mCmdLine;
        private TermSettings mTermSettings;
        private String mTitle;

        private SessionBuilder(String cmdLine) {
            mCmdLine = Objects.requireNonNull(cmdLine);
        }

        public SessionBuilder setTitle(String title) {
            mTitle = title;
            return this;
        }

        public SessionBuilder setTermSettings(TermSettings termSettings) {
            mTermSettings = termSettings;
            return this;
        }

        public Single<Session> start() {
            if (mService == null) {
                Intent intent = new Intent(App.getInstance(), TermService.class);
                if (!App.getInstance().bindService(intent, mConnection, BIND_AUTO_CREATE))
                    throw new IllegalStateException("Cannot bind term service.");

                return mServiceSubject.first().toSingle()
                    .map(termService -> startSession())
                    .cache()
                    ;
            }

            return Single.just(startSession());
        }

        private Session startSession() {
            String title = mTitle;
            if (title == null)
                title = isExec(mCmdLine) ? mCmdLine.substring(5).trim() : mCmdLine;

            ShellTermSession session;
            try {
                session = new ShellTermSession(mTermSettings != null ? mTermSettings : defaultSettings(), mCmdLine);
            } catch (IOException e) {
                throw Unchecked.of(e);
            }

            final AsyncSubject<Integer> resultSubject = AsyncSubject.create();
            session.setTitle(title);
            session.setFinishCallback(s -> {
                mService.onSessionFinish(s);

                resultSubject.onNext(s.getExitCode());
                resultSubject.onCompleted();

                mSessions--;
                if (mSessions == 0) {
                    App.getInstance().unbindService(mConnection);
                    mService = null;
                }
            });
            mService.getSessions().add(session);
            mSessions++;

            String handle = UUID.randomUUID().toString();
            session.setHandle(handle);

            session.initializeEmulator(80, 24);

            return new Session(session, resultSubject.toSingle());
        }
    }

    public static class Session {
        public final ShellTermSession termSession;
        public final Single<Integer> exitCode;

        public void showWindow(Context context) {
            context.startActivity(selectWindow(context, termSession.getHandle()));
        }

        private Session(ShellTermSession termSession, Single<Integer> exitCode) {
            this.termSession = termSession;
            this.exitCode = exitCode;
        }
    }

    public static TermSettings defaultSettings() {
        return new TermSettings(App.getInstance().getResources(), PreferenceManager.getDefaultSharedPreferences(App.getInstance()));
    }

    private static Intent selectWindow(Context context, String windowHandle) {
        return new Intent(RemoteInterface.PRIVACT_SWITCH_WINDOW)
                .setComponent(new ComponentName(context, RemoteInterface.PRIVACT_ACTIVITY_ALIAS))
                .putExtra(RemoteInterface.EXTRA_WINDOW_HANDLE, windowHandle);
    }

    private static boolean isExec(String cmdline) {
        return cmdline.startsWith("exec") && isShellSpace(cmdline.charAt(4));
    }

    private static boolean isShellSpace(char ch) {
        switch (ch) {
            // _default_ characters set of $IFS
            case ' ':
            case '\t':
            case '\n':
                return true;
        }
        return false;
    }

    private Headless() {
    }
}
