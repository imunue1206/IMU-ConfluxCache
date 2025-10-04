package com.imu.toolkit.redisson.aspect;

import com.imu.toolkit.redisson.annotation.AddCache;
import com.imu.toolkit.redisson.constant.RedissonToolkitConstant;
import com.imu.toolkit.redisson.utils.AspectUtils;
import com.imu.toolkit.redisson.utils.RCache;
import com.imu.toolkit.redisson.utils.TimeUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * 添加缓存切面实现
 * 提供缓存功能，支持防雪崩、防击穿和防穿透机制
 * 纯基于Redisson实现，不使用本地缓存
 */
@Aspect
@Component
public class AddCacheAspect {

    private static final Random RANDOM = new Random();
    
    @Autowired
    private RCache rCache;

    @Pointcut("@annotation(com.imu.toolkit.redisson.annotation.AddCache)")
    public void addCachePointCut() {}

    @Around("addCachePointCut()")
    @SuppressWarnings("unchecked")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AddCache annotation = method.getAnnotation(AddCache.class);

        // 生成缓存键
        String cacheKey = AspectUtils.parseKeyOrUsePath(joinPoint, method, annotation.key(), annotation.prefix());

        // 尝试从缓存获取
        Object cacheValue = rCache.get(cacheKey);
        if (cacheValue != null) {
            return cacheValue;
        }

        // 获取分布式锁防止缓存击穿
        RLock mutexLock = rCache.getLock(RedissonToolkitConstant.CACHE_LOAD_MUTEX_LOCK_PREFIX + cacheKey);
        
        boolean locked = false;
        try {
            // 解析锁参数
            long maxWaitMs = TimeUtils.parseTimeToMillis(annotation.loadMutexMaxWait());
            long maxLeaseMs = TimeUtils.parseTimeToMillis(annotation.loadMutexLockLeaseTime());

            // 尝试获取锁
            locked = mutexLock.tryLock(maxWaitMs, Math.max(maxLeaseMs, maxWaitMs), TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new RuntimeException(annotation.loadMutexTimeoutMsg());
            }

            // 双重检查缓存
            cacheValue = rCache.get(cacheKey);
            if (cacheValue != null) {
                return cacheValue;
            }

            // 执行原方法
            Object value = joinPoint.proceed();

            // 设置缓存，支持防雪崩的过期时间随机抖动
            String expire = annotation.expire();
            if (!expire.isEmpty()) {
                // 添加随机抖动，防止缓存雪崩
                String expireWithRandom = addRandomJitter(expire, annotation.expireRange());
                rCache.set(cacheKey, value, expireWithRandom);
            } else {
                rCache.set(cacheKey, value);
            }

            return value;
        } finally {
            // 释放锁
            if (locked && mutexLock.isHeldByCurrentThread()) {
                mutexLock.unlock();
            }
        }
    }
    
    /**
     * 添加过期时间随机抖动，防止缓存雪崩
     * @param expireTime 原始过期时间字符串
     * @param range 随机范围时间字符串（毫秒）
     * @return 带随机抖动的过期时间字符串
     */
    private String addRandomJitter(String expireTime, String range) {
        long rangeMs = TimeUtils.parseTimeToMillis(range);
        if (rangeMs <= 0) {
            return expireTime;
        }
        
        long expireMs = TimeUtils.parseTimeToMillis(expireTime);
        if (expireMs <= 0) {
            return expireTime;
        }
        
        // 计算随机偏移量
        long jitter = RANDOM.nextLong(rangeMs) + 1; // 确保至少1毫秒的偏移
        
        // 随机增加或减少，50%概率
        expireMs += jitter;
        
        return expireMs + "ms";
    }
}