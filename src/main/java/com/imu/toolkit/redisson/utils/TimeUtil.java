package com.imu.toolkit.redisson.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间解析工具类，支持多种时间单位格式解析
 * 支持格式：3s 13min 200ms 4h -1，兼容大小写
 */
public class TimeUtil {

    // 使用CASE_INSENSITIVE标志使正则表达式大小写不敏感
    private static final Pattern TIME_PATTERN = Pattern.compile("^(-?\\d+)(ms|s|min|h)?$", Pattern.CASE_INSENSITIVE);
    
    // 预定义的时间单位，避免每次字符串比较
    private static final String MS = "ms";
    private static final String S = "s";
    private static final String MIN = "min";
    private static final String H = "h";

    /**
     * 解析时间字符串为毫秒（高性能版本）
     * @param timeStr 时间字符串，如3s, 13min, 200ms, 4h, -1
     * @return 毫秒数，-1表示永不过期
     */
    public static long parseTimeToMillis(String timeStr) {
        // 快速空值检查
        if (timeStr == null || timeStr.isEmpty()) {
            throw new IllegalArgumentException("Time string cannot be empty");
        }
        
        // 去除前后空白字符（避免创建过多临时字符串）
        timeStr = timeStr.trim();
        
        // 特殊处理-1，表示永不过期
        if ("-1".equals(timeStr)) {
            return -1;
        }

        // 使用正则表达式解析
        Matcher matcher = TIME_PATTERN.matcher(timeStr);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid time format: " + timeStr + ", supported formats like: 3s 13min 200ms 4h -1");
        }

        long value;
        try {
            value = Long.parseLong(matcher.group(1));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number format: " + matcher.group(1));
        }
        
        String unit = matcher.group(2);
        
        long result;
        if (unit == null) {
            // 默认单位为毫秒
            result = value;
        } else {
            // 转小写后再比较，避免多个case分支
            unit = unit.toLowerCase();
            if (MS.equals(unit)) {
                result = value;
            } else if (S.equals(unit)) {
                result = value * 1000;
            } else if (MIN.equals(unit)) {
                result = value * 60 * 1000;
            } else if (H.equals(unit)) {
                result = value * 3600 * 1000;
            } else {
                throw new IllegalArgumentException("Unsupported time unit: " + unit);
            }
        }
        return result;
    }
    
}