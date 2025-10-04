package com.imu.toolkit.redisson.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 时间解析工具类，支持多种时间单位格式解析
 * 支持格式：3s 13min 200ms 4h -1，兼容大小写
 */
public class TimeUtils {

    // 使用CASE_INSENSITIVE标志使正则表达式大小写不敏感
    private static final Pattern TIME_PATTERN = Pattern.compile("^(-?\\d+)(ms|s|min|h)?$", Pattern.CASE_INSENSITIVE);
    
    // 缓存常用时间解析结果，减少重复计算
    private static final Map<String, Long> TIME_CACHE = new ConcurrentHashMap<>(64);
    
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
        
        // 尝试从缓存获取结果
        Long cachedResult = TIME_CACHE.get(timeStr);
        if (cachedResult != null) {
            return cachedResult;
        }
        
        // 特殊处理-1，表示永不过期
        if ("-1".equals(timeStr)) {
            return -1;
        }
        
        // 快速路径：纯数字（默认毫秒）
        if (timeStr.matches("^-?\\d+$")) {
            try {
                long result = Long.parseLong(timeStr);
                cacheResult(timeStr, result);
                return result;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid number format: " + timeStr);
            }
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
        
        // 缓存结果
        cacheResult(timeStr, result);
        return result;
    }
    
    /**
     * 缓存解析结果，但限制缓存大小
     */
    private static void cacheResult(String timeStr, long result) {
        // 限制缓存大小，避免内存溢出
        if (TIME_CACHE.size() < 1024) {
            TIME_CACHE.put(timeStr, result);
        }
    }
    
    /**
     * 清除缓存（用于测试或特定场景）
     */
    public static void clearCache() {
        TIME_CACHE.clear();
    }
}