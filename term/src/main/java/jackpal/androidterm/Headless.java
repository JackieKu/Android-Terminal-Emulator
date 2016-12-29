package jackpal.androidterm;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

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

import static android.content.Context.BIND_AUTO_CREATE;

public class Headless {
    private final BehaviorSubject<Optional<TermService>> mTermServiceSubject = BehaviorSubject.create(Optional.empty());

    private static final Single<Headless> INSTANCE = newInstance();

    public static Single<Headless> getInstance() {
        return INSTANCE;
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

        public Single<Integer> start() {
            String title = mTitle;
            if (title == null)
                title = isExec(mCmdLine) ? mCmdLine.substring(5).trim() : mCmdLine;

            TermSession session;
            try {
                session = new ShellTermSession(mTermSettings != null ? mTermSettings : defaultSettings(), mCmdLine);
            } catch (IOException e) {
                throw Unchecked.of(e);
            }
            TermService service = mTermServiceSubject.getValue().get();

            final AsyncSubject<Integer> resultSubject = AsyncSubject.create();
            session.setTitle(title);
            session.setFinishCallback(s -> {
                service.onSessionFinish(s);

                resultSubject.onNext(s.getExitCode());
                resultSubject.onCompleted();
            });
            service.getSessions().add(session);

            String handle = UUID.randomUUID().toString();
            ((GenericTermSession)session).setHandle(handle);

            session.initializeEmulator(80, 24);

            return resultSubject.toSingle();
        }
    }

    public static TermSettings defaultSettings() {
        return new TermSettings(App.getInstance().getResources(), PreferenceManager.getDefaultSharedPreferences(App.getInstance()));
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

    private static Single<Headless> newInstance() {
        final Headless h = new Headless();
        return h.mTermServiceSubject
                .first(Optional::isPresent)
                .toSingle()
                .map(x -> h)
                .cache()
                .observeOn(AndroidSchedulers.mainThread())
                ;
    }

    private Headless() {
        ServiceConnection connection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName className, IBinder service) {
                TermService.TSBinder binder = (TermService.TSBinder) service;
                mTermServiceSubject.onNext(Optional.of(binder.getService()));
            }

            @Override
            public void onServiceDisconnected(ComponentName className) {
                mTermServiceSubject.onNext(Optional.empty());
            }
        };
        Intent intent = new Intent(App.getInstance(), TermService.class);
        if (!App.getInstance().bindService(intent, connection, BIND_AUTO_CREATE))
            throw new IllegalStateException("Cannot bind term service.");
    }
}
