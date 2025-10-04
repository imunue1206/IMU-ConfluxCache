package com.imu.toolkit.redisson.annotation;

import com.imu.toolkit.redisson.constant.RedissonToolkitConstant;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式锁注解
 * 用于方法级别的分布式锁控制
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {

    /**
     * 锁的前缀
     * @return 锁前缀
     */
    String prefix() default RedissonToolkitConstant.DEFAULT_LOCK_PREFIX;

    /**
     * 锁的名称，可以使用Spring EL表达式
     * 支持以下功能：
     * 1. 方法参数访问：#userId, #user.id
     * 2. 静态方法调用：T(com.example.UserContext).getUserId()
     * 3. SpEL表达式：#args[0], #user?.address?.city
     * 4. 字符串拼接：'user:' + #userId
     * @return 锁名称
     */
    String key();

    /**
     * 锁的过期时间
     * 支持格式：3s 13min 200ms 4h 7day  -1
     * -1表示永不过期
     * @return 过期时间
     */
    String expire() default "30s";

    /**
     * 获取锁的等待时间
     * 支持格式：3s 13min 200ms 4h 7day   -1
     * -1表示不等待，立即返回
     * @return 等待时间
     */
    String waitTime() default "5s";
}