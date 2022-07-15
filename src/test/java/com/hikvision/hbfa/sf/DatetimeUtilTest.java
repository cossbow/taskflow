package com.hikvision.hbfa.sf;

import com.hikvision.hbfa.sf.util.DatetimeUtil;

import java.time.OffsetDateTime;

public class DatetimeUtilTest {
    public static void main(String[] args) {
        var s = DatetimeUtil.iso8601(OffsetDateTime.now());
        System.out.println(s);
        System.out.println(DatetimeUtil.iso8601(s));
    }
}
