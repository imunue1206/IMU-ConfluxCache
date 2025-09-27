package com.imu.toolkit.redisson.constant;

/**
 * Redisson相关常量定义
 */
public interface RedissonConstant {
    
    /**
     * 分布式锁默认前缀
     */
    String DEFAULT_LOCK_PREFIX = "lock:";
    
    /**
     * 防重复提交默认前缀
     */
    String DEFAULT_INTERVAL_LOCK_PREFIX = "interval:lock:";
    
    /**
     * 限流默认前缀
     */
    String DEFAULT_RATE_LIMIT_PREFIX = "rate:limit:";
}