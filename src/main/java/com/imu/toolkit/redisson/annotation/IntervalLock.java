package com.imu.toolkit.redisson.annotation;

import com.imu.toolkit.redisson.constant.RedissonConstant;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防止一定时间间隔内的相同操作重复
 *
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface IntervalLock {

    /**
     * 缓存的前缀
     * @return 前缀
     */
    String prefix() default RedissonConstant.DEFAULT_INTERVAL_LOCK_PREFIX;

    /**
     * 缓存的key，可以使用Spring EL表达式
     * <p>
     * 支持以下功能：
     * 1. 方法参数访问：#{#userId} 或 #{#user.id}
     * 2. 静态方法调用：#{T(com.example.UserContext).getUserId()}
     * 3. SpEL表达式：#{#args[0]}, #{#user?.address?.city}
     * 4. 字符串拼接：'user:' + #{#userId}
     * @return 缓存key
     */
    String key() default "#userId";

    /**
     * 过期时间，在这段时间内禁止重复操作
     * 支持格式：3s 13min 200ms 4h 7day 1month -1
     * @return 过期时间
     */
    String expire() default "1s";

    /**
     * 重复操作时的错误消息
     * @return 错误消息
     */
    String errorMsg() default "请勿重复操作，请稍后重试";

    /**
     * 是否包含请求参数进行签名
     * @return 是否包含参数签名
     */
    boolean includeParams() default true;

    /**
     * 忽略的参数名（当includeParams为true时生效）
     * @return 忽略的参数列表
     */
    String[] ignoreParams() default {"timestamp", "_"};
}