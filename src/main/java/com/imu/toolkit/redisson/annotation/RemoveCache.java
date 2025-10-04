package com.imu.toolkit.redisson.annotation;

import com.imu.toolkit.redisson.constant.RedissonToolkitConstant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 移除缓存注解
 * 用于在方法执行后清除指定的缓存
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoveCache {
    
    /**
     * 缓存键
     * 支持SpEL表达式，可以使用常量或拼接表达式
     * 例如："user:#{#userId}", "order:*"（支持通配符）
     * 为空时自动使用包类方法路径生成
     */
    String key() default "";
    
    /**
     * 缓存前缀
     * 用于自定义缓存键前缀
     * 不能为空
     */
    String prefix() default RedissonToolkitConstant.DEFAULT_CACHE_PREFIX;;
    
    /**
     * 是否在方法执行前清除缓存
     * 默认为false，即在方法执行后清除
     */
    boolean beforeInvocation() default false;
}