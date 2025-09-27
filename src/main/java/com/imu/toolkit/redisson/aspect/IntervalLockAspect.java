package com.imu.toolkit.redisson.aspect;

import com.imu.toolkit.redisson.annotation.IntervalLock;
import com.imu.toolkit.redisson.constant.RedissonConstant;
import com.imu.toolkit.redisson.utils.AspectUtils;
import com.imu.toolkit.redisson.utils.TimeUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Objects;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交切面实现
 */
@Aspect
@Component
public class IntervalLockAspect {

    @Autowired
    private RedissonClient redissonClient;



    @Pointcut("@annotation(com.imu.toolkit.redisson.annotation.IntervalLock)")
    public void preventDuplicateSubmitPointCut() {}

    @Around("preventDuplicateSubmitPointCut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        IntervalLock annotation = method.getAnnotation(IntervalLock.class);

        // 解析key
        String key = AspectUtils.resolveSpelExpression(joinPoint, annotation.key());
        
        // 如果需要包含参数签名
        if (annotation.includeParams()) {
            String paramsSignature = generateParamsSignature(joinPoint, annotation);
            key = key + ":" + paramsSignature;
        }

        // 如果未指定prefix或使用默认值，则添加方法路径
        String prefix = annotation.prefix();
        if (prefix.equals(RedissonConstant.DEFAULT_INTERVAL_LOCK_PREFIX)) {
            // 使用工具类构建带方法路径的前缀
            prefix = AspectUtils.buildPrefixWithMethodPath(prefix, method);
        }
        String fullKey = prefix + key;

        // 解析过期时间
        long expireTime = TimeUtils.parseTimeToMillis(annotation.expire());

        // 检查是否已经提交过
        RBucket<String> bucket = redissonClient.getBucket(fullKey);
        if (bucket.isExists()) {
            throw new RuntimeException(annotation.errorMsg());
        }

        // 设置提交标记
        bucket.set("1", expireTime, TimeUnit.MILLISECONDS);

        try {
            // 执行方法
            return joinPoint.proceed();
        } finally {
            // 注意：这里不主动删除，让它自然过期，避免业务逻辑执行失败时的重复提交问题
        }
    }

    /**
     * 解析key，支持SpEL表达式
     * 使用AspectUtils中的SpEL解析方法
     */
    private String resolveKey(ProceedingJoinPoint joinPoint, IntervalLock annotation) {
        return AspectUtils.resolveSpelExpression(joinPoint, annotation.key());
    }

    /**
     * 生成参数签名
     */
    private String generateParamsSignature(ProceedingJoinPoint joinPoint, IntervalLock annotation) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return "no_params";
        }

        // 过滤掉需要忽略的参数
        Set<String> ignoreParams = new HashSet<>(Arrays.asList(annotation.ignoreParams()));
        StringBuilder paramsBuilder = new StringBuilder();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();

        for (Object arg : args) {
            if (arg != null) {
                // 简单处理：将参数转换为字符串并进行MD5
                String argStr = arg.toString();
                // 检查是否需要忽略
                boolean shouldIgnore = false;
                for (String ignoreParam : ignoreParams) {
                    if (argStr.contains(ignoreParam)) {
                        shouldIgnore = true;
                        break;
                    }
                }
                if (!shouldIgnore) {
                    paramsBuilder.append(argStr).append(",");
                }
            }
        }

        String paramsStr = paramsBuilder.toString();
        if (paramsStr != null && !paramsStr.isEmpty()) {
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] digest = md.digest(paramsStr.getBytes(StandardCharsets.UTF_8));
                StringBuilder hexString = new StringBuilder(2 * digest.length);
                for (byte b : digest) {
                    hexString.append(String.format("%02x", b & 0xFF));
                }
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                // 不可能发生的异常，因为MD5是标准算法
                throw new RuntimeException("MD5 algorithm not found", e);
            }
        }
        return "no_params";
    }
}