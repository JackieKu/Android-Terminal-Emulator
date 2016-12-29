package jackpal.androidterm.util;

import java.util.concurrent.Callable;

/**
 * Utility that rethrows any {@code Throwable} as it was an unchecked exception.
 *
 * @see {@link #of(Throwable)}
 * @see {@link #rethrow(Throwable)}
 * @see http://brixomatic.wordpress.com/2010/04/29/hack-of-the-day-unchecking-checked-exceptions/
 */
public final class Unchecked {
    /**
     * Rethrows given {@code Throwable} as it was an unchecked exception.
     *
     * @param checkedException The exception be thrown.
     * @return This function never returns. It is there simply allowing you to write {@code throw Unchecked.of(e);}.
     * <pre><code>
int someFunction() {
    try {
        // ...
        return 42;
    } catch (Throwable e) {
        // Would be a compile error without the "throw" keyword
        // because someFunction() must return int.
        throw Unchecked.of(e);
    }
}
     * </code></pre>
     */
    public static RuntimeException of(Throwable checkedException) {
        rethrow(checkedException);
        return null;
    }

    /**
     * Rethrows given {@code Throwable} as it was an unchecked exception.
     *
     * @param checkedException The exception be thrown.
     */
    public static void rethrow(Throwable checkedException) {
        Unchecked.<RuntimeException>thrownInsteadOf(checkedException);
    }

    /**
     * Invokes given Callable without having to catch or declare the Exception.
     *
     * @return {@code callee.call()}.
     */
    public static <T> T call(Callable<T> callee) {
        try {
            return callee.call();
        } catch (Exception e) {
            throw Unchecked.of(e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void thrownInsteadOf(Throwable t) throws T {
        throw (T) t;
    }

    private Unchecked() {
    }
}
