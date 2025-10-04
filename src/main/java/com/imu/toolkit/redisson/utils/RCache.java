package com.imu.toolkit.redisson.utils;

import com.imu.toolkit.redisson.constant.RedissonToolkitConstant;
import org.redisson.api.RBucket;
import org.redisson.api.RKeys;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 基于Redisson的精简缓存工具类
 * 聚焦核心KV和Map操作，提供语义化的API
 */
@Component
public class RCache {

    private final RedissonClient redissonClient;
    
    @Autowired
    public RCache(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }
    
    /**
     * 获取分布式锁
     * @param lockKey 锁键
     * @return 分布式锁实例
     */
    public RLock getLock(String lockKey) {
        return redissonClient.getLock(lockKey);
    }
    
    /**
     * 根据模式获取所有匹配的键
     * @param pattern 键模式，支持通配符
     * @return 键迭代器
     */
    public Iterator<String> getKeysByPattern(String pattern) {
        RKeys keys = redissonClient.getKeys();
        return keys.getKeysByPattern(pattern).iterator();
    }
    
    /**
     * 批量删除匹配模式的键
     * @param pattern 键模式，支持通配符
     * @return 删除成功的数量
     */
    public long deleteByPattern(String pattern) {
        RKeys keys = redissonClient.getKeys();
        return keys.deleteByPattern(pattern);
    }

    // ==================== 核心KV操作 ====================
    
    /**
     * 设置缓存
     * @param key 缓存键
     * @param value 缓存值
     */
    public <V> void set(String key, V value) {
        RBucket<V> bucket = redissonClient.getBucket(key);
        bucket.set(value);
    }

    /**
     * 设置缓存并指定过期时间（语义化时间格式）
     * @param key 缓存键
     * @param value 缓存值
     * @param expireTime 过期时间，支持格式：3s, 5m, 1h, 2day, -1(永不过期)
     */
    public <V> void set(String key, V value, String expireTime) {
        RBucket<V> bucket = redissonClient.getBucket(key);
        long expireMs = TimeUtil.parseTimeToMillis(expireTime);
        if (expireMs == -1) {
            bucket.set(value);
        } else {
            bucket.set(value, expireMs, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 获取缓存
     * @param key 缓存键
     * @param <T> 返回类型
     * @return 缓存值
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        T value = bucket.get();
        
        // 处理空值标记，防止缓存穿透
        if (RedissonToolkitConstant.NULL_VALUE_MARKER.equals(value)) {
            return null;
        }
        return value;
    }

    /**
     * 删除缓存
     * @param key 缓存键
     * @return 是否删除成功
     */
    public boolean delete(String key) {
        return redissonClient.getBucket(key).delete();
    }

    /**
     * 批量删除缓存
     * @param keys 缓存键列表
     * @return 删除成功的数量
     */
    public long delete(Collection<String> keys) {
        return redissonClient.getKeys().delete(keys.toArray(new String[0]));
    }

    /**
     * 检查缓存是否存在
     * @param key 缓存键
     * @return 是否存在
     */
    public boolean exists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    /**
     * 设置过期时间（语义化时间格式）
     * @param key 缓存键
     * @param expireTime 过期时间，支持格式：3s, 5m, 1h, 2day, -1(永不过期)
     * @return 是否设置成功
     */
    public boolean expire(String key, String expireTime) {
        long expireMs = TimeUtil.parseTimeToMillis(expireTime);
        if (expireMs == -1) {
            // 永不过期：移除过期时间
            return redissonClient.getBucket(key).clearExpire();
        }
        return redissonClient.getBucket(key).expire(expireMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 获取剩余过期时间（毫秒）
     * @param key 缓存键
     * @return 剩余过期时间，-1表示永不过期，-2表示不存在
     */
    public long remainTimeToLive(String key) {
        return redissonClient.getBucket(key).remainTimeToLive();
    }

    // ==================== Map哈希操作 ====================

    /**
     * 设置哈希字段值
     * @param key 缓存键
     * @param field 字段名
     * @param value 字段值
     */
    public <T> void hset(String key, String field, T value) {
        RMap<String, T> map = redissonClient.getMap(key);
        map.put(field, value);
    }

    /**
     * 批量设置哈希字段值
     * @param key 缓存键
     * @param values 字段值映射
     */
    public <T> void hsetAll(String key, Map<String, T> values) {
        RMap<String, T> map = redissonClient.getMap(key);
        map.putAll(values);
    }

    /**
     * 获取哈希字段值
     * @param key 缓存键
     * @param field 字段名
     * @param <T> 返回类型
     * @return 字段值
     */
    @SuppressWarnings("unchecked")
    public <T> T hget(String key, String field) {
        RMap<String, T> map = redissonClient.getMap(key);
        T value = map.get(field);
        
        // 处理空值标记
        if (RedissonToolkitConstant.NULL_VALUE_MARKER.equals(value)) {
            return null;
        }
        return value;
    }

    /**
     * 获取哈希所有字段和值
     * @param key 缓存键
     * @param <T> 值类型
     * @return 哈希表
     */
    @SuppressWarnings("unchecked")
    public <T> Map<String, T> hgetAll(String key) {
        RMap<String, T> map = redissonClient.getMap(key);
        Map<String, T> result = new HashMap<>(map);
        
        // 处理空值标记
        result.replaceAll((k, v) -> RedissonToolkitConstant.NULL_VALUE_MARKER.equals(v) ? null : v);
        return result;
    }

    /**
     * 删除哈希字段
     * @param key 缓存键
     * @param fields 字段名数组
     * @return 删除成功的数量
     */
    public long hdel(String key, String... fields) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.fastRemove(fields);
    }

    /**
     * 检查哈希字段是否存在
     * @param key 缓存键
     * @param field 字段名
     * @return 是否存在
     */
    public boolean hexists(String key, String field) {
        RMap<String, Object> map = redissonClient.getMap(key);
        return map.containsKey(field);
    }

}
