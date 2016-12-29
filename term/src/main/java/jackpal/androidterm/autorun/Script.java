package jackpal.androidterm.autorun;

import android.support.annotation.NonNull;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import jackpal.androidterm.Headless;
import rx.Single;

import static jackpal.androidterm.RemoteInterface.quoteForBash;

public final class Script implements Comparable<Script> {
    private final File mPath;

    public Script(File path) {
        mPath = Objects.requireNonNull(path);
    }

    String getName() {
        return mPath.getName();
    }

    File getPath() {
        return mPath;
    }

    boolean isEnabled() {
        return mPath.canExecute();
    }

    void toggleEnabled() {
        setEnabled(!isEnabled());
    }

    void setEnabled(boolean enabled) {
        if (!mPath.setExecutable(enabled))
            throw new IllegalStateException("setExecutable() failed on " + mPath);
    }

    String getContent() throws IOException {
        return Files.toString(mPath, StandardCharsets.UTF_8);
    }

    void setContent(CharSequence content) throws IOException {
        Files.write(content, mPath, StandardCharsets.UTF_8);
    }

    public Single<Integer> run() {
        return Headless.getInstance()
                .flatMap(h -> h.newSession(". " + quoteForBash(getPath().getAbsolutePath()))
                    .setTitle(getName())
                    .start()
                );
    }

    @Override
    public int compareTo(@NonNull Script o) {
        if (o == this) return 0;
        return mPath.compareTo(o.mPath);
    }

    @Override
    public String toString() {
        return "Script [" + mPath + ']';
    }
}
