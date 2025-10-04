package com.imu.toolkit.redisson.annotation;

import com.imu.toolkit.redisson.constant.RedissonToolkitConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 添加缓存注解
 * 用于声明方法返回结果需要缓存
 * 支持防雪崩、防击穿和防穿透机制
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AddCache {
    
    /**
     * 缓存键
     * 支持SpEL表达式，可以使用常量或拼接表达式
     * 例如："user:#{#userId}", "order:#{#order.id}"
     * 为空时自动使用包类方法路径生成
     */
    String key() default "";

    /**
     * 缓存前缀
     * 用于自定义缓存键前缀
     * 不能为空
     */
    String prefix() default RedissonToolkitConstant.DEFAULT_CACHE_PREFIX;

    /**
     * 缓存过期时间
     * 默认5分钟
     * 支持格式：数字+s/m/h/day/year
     * 例如：5s, 10m, 1h, 1day, 1year
     */
    String expire() default "5m";
    
    /**
     * 防雪崩时间浮动范围
     * 用于随机增加过期时间，防止缓存雪崩
     * 默认200ms
     */
    String expireRange() default "200ms";

    /**
     * 防止缓存击穿的互斥等待最大时间
     * 默认400ms
     */
    String loadMutexMaxWait() default "400ms";
    /**
     * 防止缓存击穿的互斥等待最大时间
     * 默认400ms
     */
    String loadMutexLockLeaseTime() default "500ms";

    /**
     * 超过最大缓存击穿的互斥等待时间时的错误提示
     */
    String loadMutexTimeoutMsg() default "load data fail";
}