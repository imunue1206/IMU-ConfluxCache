package com.imu.toolkit.redisson.aspect;

import com.imu.toolkit.redisson.annotation.RemoveCache;
import com.imu.toolkit.redisson.constant.RedissonToolkitConstant;
import com.imu.toolkit.redisson.utils.AspectUtils;
import com.imu.toolkit.redisson.utils.RCache;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 清除缓存切面实现
 * 用于清除指定的缓存数据
 */
@Aspect
@Component
public class RemoveCacheAspect {

    private static final Logger logger = LoggerFactory.getLogger(RemoveCacheAspect.class);
    
    @Autowired
    private RCache rCache;

    @Pointcut("@annotation(com.imu.toolkit.redisson.annotation.RemoveCache)")
    public void removeCachePointCut() {}

    @Around("removeCachePointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RemoveCache annotation = method.getAnnotation(RemoveCache.class);

        // 获取前置清除标识
        boolean beforeInvocation = annotation.beforeInvocation();
        
        // 处理前置清除
        if (beforeInvocation) {
            deleteCache(joinPoint, method, annotation);
        }

        // 执行原方法
        Object result = joinPoint.proceed();

        // 处理后置清除
        if (!beforeInvocation) {
            deleteCache(joinPoint, method, annotation);
        }

        return result;
    }

    /**
     * 删除缓存
     * @param joinPoint 连接点
     * @param method 方法
     * @param annotation 注解
     */
    private void deleteCache(ProceedingJoinPoint joinPoint, Method method, RemoveCache annotation) {
        try {
            String key = annotation.key();
            String prefix = annotation.prefix();
            
            // 确定缓存前缀，如果用户未提供则使用默认前缀
            String actualCachePrefix = prefix != null && !prefix.isEmpty() ? prefix : RedissonToolkitConstant.DEFAULT_CACHE_PREFIX;
            
            // 使用工具类解析缓存键
            String cacheKey = AspectUtils.parseKeyOrUsePath(joinPoint, method, key, actualCachePrefix);
            
            // 删除缓存
            rCache.delete(cacheKey);
            logger.debug("成功删除缓存: {}", cacheKey);
        } catch (Exception e) {
            logger.error("删除缓存失败: {}", e.getMessage(), e);
            // 忽略异常，不影响原方法执行
        }
    }
}