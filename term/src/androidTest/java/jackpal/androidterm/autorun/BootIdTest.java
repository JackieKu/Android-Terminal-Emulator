package jackpal.androidterm.autorun;

import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class BootIdTest {
    @Test
    public void testKernelBootId() throws Throwable {
        String id = BootId.getKernelBootId();
        assertThat(id, not(isEmptyOrNullString()));
        assertThat(UUID.fromString(id), notNullValue());
    }
}
