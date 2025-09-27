package com.imu.toolkit.redisson.aspect;

import com.imu.toolkit.redisson.annotation.RateLimit;
import com.imu.toolkit.redisson.constant.RedissonConstant;
import com.imu.toolkit.redisson.utils.AspectUtils;
import com.imu.toolkit.redisson.utils.TimeUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面实现
 * 基于Redisson原生的RRateLimiter实现滑动窗口限流
 */
@Aspect
@Component
public class RateLimitAspect {

    @Autowired
    private RedissonClient redissonClient;

    

    @Pointcut("@annotation(com.imu.toolkit.redisson.annotation.RateLimit)")
    public void rateLimitPointCut() {}

    @Around("rateLimitPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RateLimit annotation = method.getAnnotation(RateLimit.class);

        // 解析key
        String key = AspectUtils.resolveSpelExpression(joinPoint, annotation.key());
        
        // 如果未指定prefix或使用默认值，则添加方法路径
        String prefix = annotation.prefix();
        if (prefix.equals(RedissonConstant.DEFAULT_RATE_LIMIT_PREFIX)) {
            // 使用工具类构建带方法路径的前缀
            prefix = AspectUtils.buildPrefixWithMethodPath(prefix, method);
        }
        String fullKey = prefix + key;

        // 获取限流配置
        int limit = annotation.limit();
        // 解析时间窗口
        long timeWindowMillis = TimeUtils.parseTimeToMillis(annotation.timeWindow());
        // 解析等待时间
        long waitTimeMillis = TimeUtils.parseTimeToMillis(annotation.waitTime());

        // 获取Redisson限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(fullKey);
        
        // 配置限流器：设置为滑动窗口模式
        // 注意：这里只在第一次获取时设置，后续获取会复用之前的配置
        rateLimiter.trySetRate(RateType.OVERALL, limit, timeWindowMillis, RateIntervalUnit.MILLISECONDS);

        // 尝试获取令牌
        boolean allowed;
        if (waitTimeMillis == -1) {
            // 不等待，直接拒绝
            allowed = rateLimiter.tryAcquire();
        } else if (waitTimeMillis > 0) {
            // 等待指定时间
            allowed = rateLimiter.tryAcquire(waitTimeMillis, TimeUnit.MILLISECONDS);
        } else {
            // 立即尝试获取，不等待
            allowed = rateLimiter.tryAcquire();
        }

        if (!allowed) {
            throw new RuntimeException(annotation.errorMsg());
        }

        // 执行方法
        return joinPoint.proceed();
    }
}