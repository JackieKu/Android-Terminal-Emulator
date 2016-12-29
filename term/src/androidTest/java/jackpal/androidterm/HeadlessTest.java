package jackpal.androidterm;

import org.junit.Test;

import java.io.File;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class HeadlessTest {
    private static final String TAG = "HeadlessTest";

    @Test
    public void runTest() throws Throwable {
        File f = new File(App.getInstance().getExternalFilesDir(null), "HeadlessTest_runTest");
        f.delete();
        assumeFalse(f.exists());

        Headless.getInstance()
            .flatMap(h -> h.newSession("exec touch '" + f.getAbsolutePath() + "'").start())
            .doOnSuccess(i -> assertThat(i, is(0)))
            .toCompletable()
            .await()
            ;

        assertTrue(f.exists());
    }

    //@Test
    public void runTest2() throws Throwable {
        Headless.getInstance()
            .flatMap(h -> h.newSession("exec top").start())
            .toCompletable()
            .await()
            ;
    }
}
