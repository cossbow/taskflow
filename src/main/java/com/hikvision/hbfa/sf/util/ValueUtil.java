package com.hikvision.hbfa.sf.util;

import java.nio.charset.StandardCharsets;
import java.util.*;

final
public class ValueUtil {
    private ValueUtil() {
    }

    //

    public static boolean isEmpty(String s) {
        return null == s || s.isEmpty();
    }

    public static boolean isBlank(String s) {
        return isEmpty(s) || s.isBlank();
    }

    public static boolean isEmpty(Collection<?> c) {
        return null == c || c.isEmpty();
    }

    public static boolean isEmpty(Map<?, ?> m) {
        return null == m || m.isEmpty();
    }

    public static boolean isEmpty(Object[] a) {
        return null == a || a.length == 0;
    }

    public static boolean isEmpty(byte[] a) {
        return null == a || a.length == 0;
    }

    public static boolean isEmpty(int[] a) {
        return null == a || a.length == 0;
    }

    public static boolean isEmpty(long[] a) {
        return null == a || a.length == 0;
    }

    public static <T> T nullIf(T v, T def) {
        return null != v ? v : def;
    }

    public static int len(Collection<?> c) {
        return null == c ? 0 : c.size();
    }

    public static int len(Map<?, ?> m) {
        return null == m ? 0 : m.size();
    }

    public static int len(Object[] a) {
        return null == a ? 0 : a.length;
    }

    public static <T> T get(List<T> list, int index) {
        return isEmpty(list) ? null : list.get(index);
    }


    public
    static <E, C extends Collection<E>, S extends Collection<E>>
    Set<E> subtract(C minuend, S subtraction) {
        var result = new HashSet<>(minuend);
        result.removeAll(subtraction);
        return result;
    }


    //

    private static final byte[] digits = {
            '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
    };

    /**
     * @see Long#formatUnsignedLong0
     */
    static void format16(long val, byte[] buf, int offset, int len) {
        int pos = offset + len;
        do {
            buf[--pos] = digits[((int) val) & 15];
            val >>>= 4;
        } while (pos > offset);
    }

    public static final int UUID_SIZE = 32;

    /**
     * {@link UUID}转16进制字符串，无横杠
     */
    public static String fastUUID(UUID uuid) {
        var lsb = uuid.getLeastSignificantBits();
        var msb = uuid.getMostSignificantBits();

        byte[] buf = new byte[UUID_SIZE];
        format16(lsb, buf, 20, 12);
        format16(lsb >>> 48, buf, 16, 4);
        format16(msb, buf, 12, 4);
        format16(msb >>> 16, buf, 8, 4);
        format16(msb >>> 32, buf, 0, 8);

        return new String(buf, StandardCharsets.US_ASCII);
    }

    /**
     * 产生一个uuid
     * 与{@link UUID#toString()}不同的是没有横杠
     */
    public static String newUUID() {
        return fastUUID(UUID.randomUUID());
    }


    //

    public static int getSystemProp(String key, int defaultValue) {
        var s = System.getProperty(key);
        if (null == s) return defaultValue;
        return Integer.parseInt(s);
    }

    public static int getThreadCount(String key, int min) {
        assert min >= 0;
        return getSystemProp(key, Math.max(Runtime.getRuntime().availableProcessors(), min));
    }

    public static int getThreadCount(String key) {
        return getThreadCount(key, 1);
    }

}
