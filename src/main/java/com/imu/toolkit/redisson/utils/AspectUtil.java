package com.imu.toolkit.redisson.utils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;

/**
 * 切面工具类
 * 提供切面相关的通用方法
 */
public class AspectUtil {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();

    /**
     * 获取方法的完整路径（包名+类名+方法名）
     * @param method 方法对象
     * @return 方法的完整路径
     */
    public static String getMethodFullPath(Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }
    
    /**
     * 根据默认前缀和方法信息构建带方法路径的前缀
     * @param defaultPrefix 默认前缀
     * @param method 方法对象
     * @return 带方法路径的前缀
     */
    public static String buildPrefixWithMethodPath(String defaultPrefix, Method method) {
        String methodPath = getMethodFullPath(method);
        return defaultPrefix + methodPath + ":";
    }
    
    /**
     * 解析SpEL表达式
     * @param joinPoint 连接点
     * @param expressionString SpEL表达式字符串
     * @return 解析后的字符串
     */
    public static String resolveSpelExpression(ProceedingJoinPoint joinPoint, String expressionString) {
        // 如果不包含#，则不需要解析
        if (!expressionString.contains("#")) {
            return expressionString;
        }
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        
        // 使用MethodBasedEvaluationContext以支持参数名访问
        EvaluationContext context = new MethodBasedEvaluationContext(
                joinPoint.getTarget(), method, args, PARAMETER_NAME_DISCOVERER);
        
        // 解析SpEL表达式
        return PARSER.parseExpression(expressionString).getValue(context, String.class);
    }

    public static String parseKeyOrUsePath(ProceedingJoinPoint joinPoint, Method method, String key, String prefix) {
        String currentKey = AspectUtil.resolveSpelExpression(joinPoint, key);

        if (currentKey == null || key.trim().isEmpty()) {
            currentKey = AspectUtil.getMethodFullPath(method);
        }
        return prefix + currentKey;
    }
}