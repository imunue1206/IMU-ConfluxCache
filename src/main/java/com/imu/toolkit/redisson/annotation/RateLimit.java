package com.imu.toolkit.redisson.annotation;

import com.imu.toolkit.redisson.constant.RedissonToolkitConstant;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 限流注解
 * 用于方法级别的流量控制
 * 基于Redisson原生的RRateLimiter实现
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流的前缀
     * @return 前缀
     */
    String prefix() default RedissonToolkitConstant.DEFAULT_RATE_LIMIT_PREFIX;

    /**
     * 限流的key，可以使用Spring EL表达式
     * <p>
     * 支持以下功能：
     * 1. 方法参数访问：#{#userId} 或 #{#user.id}
     * 2. 静态方法调用：#{T(com.example.UserContext).getUserId()}
     * 3. SpEL表达式：#{#args[0]}, #{#user?.address?.city}
     * 4. 字符串拼接：'user:' + #{#userId}
     * @return 限流key
     */
    String key();

    /**
     * 单位时间内允许的请求数
     * @return 请求数
     */
    int limit() default 10;

    /**
     * 时间窗口
     * 支持格式：3s 13min 200ms 4h 7day 1month
     * @return 时间窗口
     */
    String timeWindow() default "1s";

    /**
     * 获取令牌的等待时间
     * 支持格式：3s 13min 200ms 4h 7day 1month -1
     * -1 表示不等待，直接拒绝
     * @return 等待时间
     */
    String waitTime() default "0s";

    /**
     * 限流失败时的错误消息
     * @return 错误消息
     */
    String errorMsg() default "请求过于频繁，请稍后重试";
}