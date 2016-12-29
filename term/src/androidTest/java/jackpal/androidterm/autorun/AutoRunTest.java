package jackpal.androidterm.autorun;

import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class AutoRunTest {
    @Test
    public void testAutoRun() throws Throwable {
        AutoRunService.startBootScripts().toCompletable().await();
    }
}
