package com.imu.toolkit.redisson.utils;

import java.time.Duration;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间解析工具类，支持多种时间单位格式解析
 * 支持格式：3s 13min 200ms 4h 7day -1
 */
public class TimeUtils {

    private static final Pattern TIME_PATTERN = Pattern.compile("^(-?\\d+)(ms|s|min|h)?$");

    /**
     * 解析时间字符串为毫秒
     * @param timeStr 时间字符串，如3s, 13min, 200ms, 4h, 7day, 1month, 2year, -1
     * @return 毫秒数，-1表示永不过期
     */
    public static long parseTimeToMillis(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("Time string cannot be empty");
        }

        // 特殊处理-1，表示永不过期
        if ("-1".equals(timeStr.trim())) {
            return -1;
        }

        Matcher matcher = TIME_PATTERN.matcher(timeStr.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time format: " + timeStr + ", supported formats like: 3s 13min 200ms 4h -1");
        }

        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);

        if (unit == null) {
            // 默认单位为毫秒
            return value;
        }

        switch (unit) {
            case "ms":
                return value;
            case "s":
                return value * 1000;
            case "min":
                return value * 60 * 1000;
            case "h":
                return value * 60 * 60 * 1000;
            default:
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
        }
    }
}