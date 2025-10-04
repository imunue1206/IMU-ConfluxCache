# IMU-RedissonToolkit

## 项目介绍

IMU-RedissonToolkit 是一个专注于解决分布式缓存问题的注解驱动型工具包，基于 Redisson 实现。它通过简洁的注解 API，优雅地解决了分布式系统中缓存三大常见问题（缓存穿透、缓存击穿、缓存雪崩），同时提供了分布式锁、限流、防重复提交等企业级功能。本工具包专为 Spring Boot 应用设计，让开发者能够以极低的学习成本构建高性能、高可用的分布式应用。

## 核心价值

- **注解驱动开发**：通过注解实现一行代码集成分布式功能，降低学习和使用成本
- **缓存问题克星**：内置完整解决方案应对缓存穿透、缓存击穿、缓存雪崩三大问题
- **语义化时间表达**：支持 `3s`、`5m`、`1h`、`-1` 等直观时间格式，告别 TimeUnit 枚举
- **类型安全**：泛型支持和封装优化，减少运行时错误
- **开箱即用**：基于 Spring Boot 自动装配，无需复杂配置
- **企业级可靠性**：经过生产环境验证的分布式锁、缓存和限流机制

## 快速开始

### 1. 添加依赖

在你的 Spring Boot 项目中添加以下依赖：

```xml
<dependency>
    <groupId>com.imu.stock</groupId>
    <artifactId>IMU-RedissonToolkit</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

### 2. 配置 Redisson

在 `application.yml` 或 `application.properties` 中配置 Redis 连接信息：

```yaml
redisson:
  address: redis://127.0.0.1:6379
  password: your-password # 可选
  database: 0
```

### 3. 启动应用

确保你的 Spring Boot 应用程序已启用注解处理：

```java
@SpringBootApplication
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

## 注解驱动开发 - 核心能力

### 1. @AddCache - 智能缓存注解（解决缓存三大问题）

`@AddCache` 是本工具包的核心注解，专为解决缓存三大常见问题设计，通过一行代码实现方法返回结果的智能缓存。

#### 缓存三大问题解决方案

**1. 缓存穿透（Cache Penetration）**
- **问题**：查询不存在的数据，导致每次请求都直击数据库
- **解决方案**：对 null 值进行特殊标记缓存，设置较短过期时间
- **实现方式**：使用 `RedissonToolkitConstant.NULL_VALUE_MARKER` 标记不存在的值

**2. 缓存击穿（Cache Breakdown）**
- **问题**：热点数据缓存失效时，大量并发请求直击数据库
- **解决方案**：分布式互斥锁 + 双重检查锁定模式
- **实现方式**：使用 `@AddCache` 中的 `loadMutexMaxWait` 和 `loadMutexLockLeaseTime` 参数

**3. 缓存雪崩（Cache Avalanche）**
- **问题**：大量缓存同时过期，导致数据库压力激增
- **解决方案**：随机过期时间抖动
- **实现方式**：使用 `@AddCache` 中的 `expireRange` 参数添加随机偏移

#### 注解参数详解

| 参数 | 类型 | 必选 | 默认值 | 说明 |
|------|------|------|--------|------|
| `key` | String | 是 | - | 缓存键，支持 SpEL 表达式 |
| `prefix` | String | 否 | 空 | 缓存键前缀，可用于分组管理 |
| `expire` | String | 否 | "10m" | 缓存过期时间，支持语义化格式 |
| `expireRange` | String | 否 | "1s" | 过期时间随机抖动范围 |
| `loadMutexMaxWait` | String | 否 | "400ms" | 获取互斥锁最大等待时间（防击穿） |
| `loadMutexLockLeaseTime` | String | 否 | "500ms" | 互斥锁持有时间（防击穿） |
| `loadMutexTimeoutMsg` | String | 否 | "load data fail" | 互斥锁获取超时提示 |

#### 使用示例

```java
@Service
public class ProductService {
    
    @AddCache(
        key = "product:#{#id}",          // 使用 SpEL 表达式动态生成缓存键
        expire = "10m",                 // 缓存10分钟
        expireRange = "500ms",          // 添加±500ms随机抖动防止雪崩
        loadMutexMaxWait = "500ms",     // 获取锁最多等待500ms
        loadMutexLockLeaseTime = "1s"    // 锁过期时间1秒
    )
    public Product getProductById(String id) {
        // 数据库查询逻辑 - 只在缓存未命中且成功获取锁时执行一次
        return productRepository.findById(id).orElse(null);
    }
}
```

### 2. @RemoveCache - 缓存清理注解

自动清理指定的缓存，确保缓存一致性。

#### 注解参数详解

| 参数 | 类型 | 必选 | 默认值 | 说明 |
|------|------|------|--------|------|
| `key` | String | 是 | - | 缓存键，支持 SpEL 表达式和通配符 |
| `beforeInvocation` | boolean | 否 | false | 是否在方法执行前清理缓存 |

#### 使用示例

```java
@Service
public class ProductService {
    
    @RemoveCache(
        key = "product:#{#product.id}", // 清理特定产品缓存
        beforeInvocation = false        // 方法执行后清理
    )
    public void updateProduct(Product product) {
        // 更新产品逻辑
        productRepository.save(product);
    }
    
    @RemoveCache(
        key = "product:*",            // 使用通配符清理所有产品缓存
        beforeInvocation = true       // 方法执行前清理
    )
    public void refreshAllProductCaches() {
        // 刷新逻辑
    }
}
```

### 3. @DistributedLock - 分布式锁注解

通过简单的注解实现方法级别的分布式锁，防止并发问题。

#### 注解参数详解

| 参数 | 类型 | 必选 | 默认值 | 说明 |
|------|------|------|--------|------|
| `key` | String | 是 | - | 锁名称，支持 SpEL 表达式 |
| `expire` | String | 否 | "30s" | 锁过期时间 |
| `waitTime` | String | 否 | "5s" | 获取锁最大等待时间 |

#### 使用示例

```java
@Service
public class OrderService {
    
    @DistributedLock(
        key = "order:create:#{#userId}", // 动态生成锁名
        expire = "30s",                 // 锁30秒后自动释放
        waitTime = "5s"                 // 最多等待5秒获取锁
    )
    public Order createOrder(String userId, OrderDTO order) {
        // 订单创建逻辑 - 确保同一用户并发请求顺序执行
        return orderRepository.save(order);
    }
}
```

### 4. @RateLimit - 分布式限流注解

基于Redisson限流实现封装，提供方法级别的分布式限流功能，防止系统过载。

#### 注解参数详解

| 参数 | 类型 | 必选 | 默认值 | 说明 |
|------|------|------|--------|------|
| `key` | String | 是 | - | 限流键，支持 SpEL 表达式 |
| `limit` | int | 否 | 100 | 时间窗口内允许的最大请求数 |
| `timeWindow` | String | 否 | "3s" | 限流统计时间窗口 |
| `waitTime` | String | 否 | "500ms" | 拥挤等待最大时间 |

#### 使用示例

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    @RateLimit(
        key = "api:access:#{#userId}", // 按用户ID限流
        rate = 10,                     // 每秒最多10次请求
        timeWindow = "1s"                // 1秒统计窗口
    )
    @GetMapping("/resource")
    public ResponseEntity<Resource> getResource(@RequestParam String userId) {
        // API 处理逻辑
        return ResponseEntity.ok(resourceService.getResource(userId));
    }
}
```

### 5. @IntervalLock - 防重复提交注解

防止短时间内重复提交表单或请求。

#### 注解参数详解

| 参数 | 类型 | 必选 | 默认值 | 说明 |
|------|------|------|--------|------|
| `key` | String | 是 | - | 防重复键，支持 SpEL 表达式 |
| `interval` | String | 否 | "5s" | 禁止重复提交的时间间隔 |

#### 使用示例

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @IntervalLock(
        key = "order:submit:#{#userId}-#{#order.type}", // 组合键
        interval = "10s"                              // 10秒内禁止重复提交
    )
    @PostMapping("/submit")
    public ResponseEntity<OrderResult> submitOrder(@RequestParam String userId, @RequestBody OrderDTO order) {
        // 订单提交逻辑
        return ResponseEntity.ok(orderService.submitOrder(userId, order));
    }
}
```

## 核心工具类

从注解驱动的实现中，我们抽象出了两个核心工具类，它们是注解功能的基础支撑：

### 1. RCache - 智能缓存工具

RCache 是 @AddCache 和 @RemoveCache 注解的底层实现，提供了丰富的缓存操作 API，并内置了缓存问题解决方案。

#### 主要特性
- 支持泛型的类型安全 API
- 语义化时间格式配置过期时间
- 内置防缓存穿透的空值处理
- 提供分布式锁获取功能
- 键模式匹配和批量删除

#### 使用示例

```java
@Service
public class UserService {
    
    @Autowired
    private RCache rCache;
    
    public void cacheUserInfo(User user) {
        // 设置缓存，5分钟过期
        rCache.set("user:" + user.getId(), user, "5m");
    }
    
    public User getUserById(String userId) {
        // 获取缓存，自动处理空值标记
        return rCache.get("user:" + userId);
    }
    
    public void clearUserCache(String userId) {
        // 批量删除匹配的缓存键
        rCache.deleteByPattern("user:" + userId + ":*");
    }
}
```

### 2. TimeUtils - 语义化时间解析

TimeUtils 提供了强大的时间解析功能，支持多种时间单位格式，是注解中时间参数的核心解析工具。

#### 支持的时间格式
- `3s` - 3秒
- `5m` - 5分钟
- `1h` - 1小时
- `200ms` - 200毫秒
- `-1` - 永不过期

#### 实现原理
```java
// TimeUtils 核心解析逻辑
dublic long parseTimeToMillis(String timeStr) {
    // 支持 -1 表示永不过期
    if ("-1".equals(timeStr)) {
        return -1;
    }
    
    // 使用正则表达式匹配时间格式
    Pattern pattern = Pattern.compile("(\\d+)(ms|s|m|h)?");
    Matcher matcher = pattern.matcher(timeStr);
    
    if (matcher.find()) {
        long value = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2) != null ? matcher.group(2) : "ms";
        
        // 根据单位转换为毫秒
        switch (unit) {
            case "s": return value * 1000;
            case "m": return value * 60 * 1000;
            case "h": return value * 3600 * 1000;
            default: return value; // 默认毫秒
        }
    }
    
    // 纯数字默认为毫秒
    return Long.parseLong(timeStr);
}
```

## 缓存三大问题解决详解

### 1. 缓存穿透解决方案

**问题描述**：当查询一个不存在的数据时，由于缓存未命中，请求会直接访问数据库。如果短时间内有大量这样的请求，会对数据库造成压力。

**实现方案**：
- 当方法返回 null 时，使用特殊标记 `NULL_VALUE_MARKER` 缓存该结果
- 设置较短的过期时间（通常为几十秒），避免长期占用缓存空间
- 获取缓存时，自动识别并转换特殊标记为 null 返回

**代码实现**：
```java
// AddCacheAspect 中的关键逻辑
if (result == null) {
    // 缓存空值标记，设置较短过期时间
    String nullExpire = TimeUtils.parseTimeToMillis("5s");
    rCache.set(cacheKey, RedissonToolkitConstant.NULL_VALUE_MARKER, nullExpire);
} else {
    // 正常缓存结果，添加随机抖动
    String expireWithJitter = addRandomJitter(expire, expireRange);
    rCache.set(cacheKey, result, expireWithJitter);
}
```

### 2. 缓存击穿解决方案

**问题描述**：当热点数据的缓存过期时，大量并发请求会同时访问数据库，导致数据库压力激增。

**实现方案**：
- 采用分布式互斥锁 + 双重检查锁定模式
- 只有第一个获取到锁的请求去查询数据库，其他请求等待或直接返回
- 使用 `RedissonToolkitConstant.CACHE_LOAD_MUTEX_LOCK_PREFIX` 构建锁键

**代码实现**：
```java
// AddCacheAspect 中的关键逻辑
String lockKey = RedissonToolkitConstant.CACHE_LOAD_MUTEX_LOCK_PREFIX + cacheKey;
RLock lock = rCache.getLock(lockKey);

if (lock.tryLock(loadMutexMaxWaitMs, loadMutexLockLeaseTimeMs, TimeUnit.MILLISECONDS)) {
    try {
        // 双重检查，避免锁等待期间缓存已被更新
        Object doubleCheckResult = rCache.get(cacheKey);
        if (doubleCheckResult != null) {
            return convertCacheResult(doubleCheckResult);
        }
        
        // 执行原方法获取数据
        Object result = pjp.proceed();
        // 缓存结果
        // ...
        return result;
    } finally {
        lock.unlock();
    }
} else {
    throw new RuntimeException(loadMutexTimeoutMsg);
}
```

### 3. 缓存雪崩解决方案

**问题描述**：当大量缓存同时过期时，会导致大量请求同时访问数据库，造成数据库压力过大甚至崩溃。

**实现方案**：
- 为缓存过期时间添加随机抖动，避免大量缓存同时失效
- 使用 `expireRange` 参数控制抖动范围

**代码实现**：
```java
// AddCacheAspect 中的关键逻辑
private String addRandomJitter(String expire, String expireRange) {
    // 解析过期时间和抖动范围
    long expireMs = TimeUtils.parseTimeToMillis(expire);
    long rangeMs = TimeUtils.parseTimeToMillis(expireRange);
    
    if (rangeMs <= 0) {
        return expire; // 无抖动
    }
    
    // 生成随机偏移量（-rangeMs 到 +rangeMs）
    long jitter = ThreadLocalRandom.current().nextLong(-rangeMs, rangeMs + 1);
    // 确保结果至少为1毫秒
    long finalExpireMs = Math.max(1, expireMs + jitter);
    
    // 转换回语义化格式
    return formatTimeMs(finalExpireMs);
}
```

## 最佳实践

### 缓存设计最佳实践

1. **键命名规范**：采用 `业务模块:实体类型:ID` 的格式，如 `product:detail:123`
2. **过期时间策略**：
   - 热点数据：设置较短过期时间（5-15分钟）+ 较大抖动范围
   - 一般数据：适中过期时间（1-2小时）
   - 静态数据：较长过期时间（24小时+）或配合手动更新机制
3. **缓存预热**：系统启动时预先加载热点数据到缓存
4. **异步更新**：使用消息队列异步更新缓存，避免阻塞主流程
5. **监控告警**：监控缓存命中率、延迟等指标，及时发现问题

### 分布式锁使用注意事项

1. **锁粒度**：尽量使用细粒度锁，避免大锁影响并发性能
2. **超时设置**：合理设置锁过期时间，避免死锁
3. **异常处理**：确保在 finally 块中释放锁
4. **重试机制**：考虑为关键操作添加重试逻辑

## 故障排查

### 常见问题及解决方案

1. **缓存更新不及时**：检查 `@RemoveCache` 注解是否正确配置，键表达式是否准确
2. **分布式锁失效**：确保 Redis 连接正常，检查锁过期时间是否合理
3. **内存占用过高**：检查缓存键是否过多或过大，考虑增加过期时间或使用更合理的数据结构
4. **性能问题**：检查是否频繁获取锁或执行复杂的 SpEL 表达式，考虑优化缓存策略

## 总结

IMU-RedissonToolkit 通过注解驱动的方式，优雅地解决了分布式系统中缓存三大常见问题，同时提供了丰富的分布式能力。它的设计理念是让开发者能够以最小的代价获得企业级的分布式解决方案，专注于业务逻辑开发，而不是底层技术实现细节。

通过使用本工具包，你可以轻松构建高性能、高可用的分布式应用，有效避免缓存问题带来的系统风险，提升用户体验和系统稳定性。
