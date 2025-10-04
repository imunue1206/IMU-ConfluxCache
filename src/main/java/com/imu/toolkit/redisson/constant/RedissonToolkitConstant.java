package com.imu.toolkit.redisson.constant;

/**
 * Redisson相关常量定义
 */
public interface RedissonToolkitConstant {
    
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
    
    /**
     * 缓存默认前缀
     */
    String DEFAULT_CACHE_PREFIX = "cache:";
    
    /**
     * 缓存互斥锁默认前缀
     */
    String CACHE_LOAD_MUTEX_LOCK_PREFIX = "lock:cache:load:mutex:";

    /**
     * 防止缓存穿透用的特殊Null代替存值，防止真的存null，导致因为判空导致每次都取数据库
     */
    String NULL_VALUE_MARKER = "__NULL__VALUE__MARKER__";
}