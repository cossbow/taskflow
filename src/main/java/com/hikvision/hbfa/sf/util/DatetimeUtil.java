package com.hikvision.hbfa.sf.util;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.TemporalAccessor;

import static java.time.temporal.ChronoField.*;

final
public class DatetimeUtil {
    private DatetimeUtil() {
    }

    public static final DateTimeFormatter ISO8601 = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ISO_LOCAL_DATE)
            .optionalStart()
            .appendLiteral('T')
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(':')
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(':')
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .parseLenient()
            .appendOffsetId()
            .parseStrict()
            .toFormatter();


    public static String iso8601(TemporalAccessor temporal) {
        return ISO8601.format(temporal);
    }

    public static OffsetDateTime iso8601(String time) {
        return OffsetDateTime.parse(time, ISO8601);
    }


    public static long currentSeconds() {
        return System.currentTimeMillis() / 1000;
    }


}
