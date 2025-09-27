package com.imu.toolkit.redisson.aspect;

import com.imu.toolkit.redisson.annotation.DistributedLock;
import com.imu.toolkit.redisson.constant.RedissonConstant;
import com.imu.toolkit.redisson.utils.AspectUtils;
import com.imu.toolkit.redisson.utils.TimeUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁切面实现
 */
@Aspect
@Component
public class DistributedLockAspect {

    @Autowired
    private RedissonClient redissonClient;

    

    @Pointcut("@annotation(com.imu.toolkit.redisson.annotation.DistributedLock)")
    public void distributedLockPointCut() {}

    @Around("distributedLockPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        DistributedLock annotation = method.getAnnotation(DistributedLock.class);

        // 解析锁名称（支持SpEL表达式）
        String key = AspectUtils.resolveSpelExpression(joinPoint, annotation.key());
        
        // 如果未指定prefix或使用默认值，则添加方法路径
        String prefix = annotation.prefix();
        if (prefix.equals(RedissonConstant.DEFAULT_LOCK_PREFIX)) {
            // 使用工具类构建带方法路径的前缀
            prefix = AspectUtils.buildPrefixWithMethodPath(prefix, method);
        }
        String fullLockName = prefix + key;

        // 获取锁
        RLock lock = redissonClient.getLock(fullLockName);

        // 解析过期时间和等待时间
        long expireTime = TimeUtils.parseTimeToMillis(annotation.expire());
        long waitTime = TimeUtils.parseTimeToMillis(annotation.waitTime());

        boolean locked = false;
        try {
            // 尝试获取锁
            if (waitTime == -1) {
                // 不等待，立即尝试获取锁
                locked = lock.tryLock(0, expireTime != -1 ? expireTime : Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            } else {
                // 等待指定时间
                locked = lock.tryLock(waitTime, expireTime != -1 ? expireTime : Long.MAX_VALUE, TimeUnit.MILLISECONDS);
            }

            if (locked) {
                // 获取锁成功，执行方法
                return joinPoint.proceed();
            } else {
                // 获取锁失败，默认抛出异常
                throw new RuntimeException("获取分布式锁失败，请稍后重试");
            }
        } finally {
            // 释放锁
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 解析锁名称
     * 使用AspectUtils中的SpEL解析方法
     */
    private String resolveLockName(ProceedingJoinPoint joinPoint, DistributedLock annotation) {
        return AspectUtils.resolveSpelExpression(joinPoint, annotation.key());
    }

}