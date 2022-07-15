package com.hikvision.hbfa.sf.util;

final
public class ThrowsUtil {
    private ThrowsUtil() {
    }

    public static RuntimeException unchecked(Throwable t) {
        if (null == t) throw new NullPointerException();
        return unchecked0(t);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E unchecked0(Throwable t) throws E {
        throw (E) t;
    }


    /**
     * <h3>sleep线程</h3>
     * 悄悄抛出异常{@link InterruptedException}
     *
     * @param millis 毫秒
     */
    public static void sleepSilent(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            throw ThrowsUtil.unchecked(ex);
        }
    }

}
