# IMU-RedissonToolkit

## 项目介绍

IMU-RedissonToolkit 是一个基于 Redisson 的分布式工具包，提供简洁易用的分布式锁、防重复提交、限流等功能，帮助开发者轻松应对分布式系统中的并发问题。

## 主要特性

- **分布式锁**：支持方法级别的分布式锁，通过注解方式快速集成
- **灵活的 SpEL 表达式**：支持通过 Spring EL 表达式动态生成锁名称
- **静态方法调用**：支持在表达式中调用自定义的 ThreadLocal 静态方法获取上下文信息
- **丰富的时间格式**：支持多种时间单位格式，如 3s、5min、1h 等
- **自动装配**：基于 Spring Boot 自动装配，开箱即用

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

### 2. 配置 Redis

在 `application.properties` 或 `application.yml` 中配置 Redis 连接信息：

```yaml
redisson:
  address: redis://127.0.0.1:6379
  password: your-password (可选)
  database: 0
```

### 3. 启用注解

确保你的 Spring Boot 应用程序已启用注解处理：

```java
@SpringBootApplication
public class YourApplication {
    public static void main(String[] args) {
        SpringApplication.run(YourApplication.class, args);
    }
}
```

## 使用示例

### 1. 分布式锁使用示例

#### 基本使用

```java
@Service
public class OrderService {

    @DistributedLock(name = "order:create:user:#userId")
    public void createOrder(String userId, OrderDTO order) {
        // 处理订单创建逻辑
        System.out.println("创建订单中...");
    }
}
```

#### 使用嵌套对象属性

```java
@Service
public class UserService {

    @DistributedLock(name = "user:update:id:#user.id")
    public void updateUser(User user) {
        // 更新用户信息
        System.out.println("更新用户信息中...");
    }
}
```

#### 使用静态方法获取用户上下文

```java
// 1. 首先创建一个 ThreadLocal 上下文工具类
public class UserContext {
    private static final ThreadLocal<String> USER_ID_HOLDER = new ThreadLocal<>();
    
    public static void setUserId(String userId) {
        USER_ID_HOLDER.set(userId);
    }
    
    public static String getUserId() {
        return USER_ID_HOLDER.get();
    }
    
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}

// 2. 在拦截器或过滤器中设置用户信息
@Component
public class UserContextInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从请求头或会话中获取用户ID
        String userId = request.getHeader("X-User-ID");
        if (userId != null) {
            UserContext.setUserId(userId);
        }
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}

// 3. 在业务方法中使用
@Service
public class PaymentService {
    
    @DistributedLock(name = "payment:process:user:" + 
                        "#{T(com.example.UserContext).getUserId()}")
    public void processPayment(PaymentDTO payment) {
        // 处理支付逻辑
        System.out.println("处理支付中...");
    }
}
```

#### 自定义过期时间和等待时间

```java
@Service
public class InventoryService {
    
    @DistributedLock(
        name = "inventory:deduct:product:#productId",
        expire = "60s",  // 锁过期时间60秒
        waitTime = "10s" // 获取锁最长等待10秒
    )
    public void deductInventory(String productId, int quantity) {
        // 扣减库存逻辑
        System.out.println("扣减库存中...");
    }
}
```

#### 使用SpEL表达式高级特性

```java
@Service
public class ReportService {
    
    @DistributedLock(
        name = "report:generate:user:#{#args[0]}:date:#{#args[1]}"
    )
    public void generateReport(String userId, String date) {
        // 生成报表逻辑
        System.out.println("生成报表中...");
    }
    
    @DistributedLock(
        name = "report:export:" + 
               "#{T(com.example.UserContext).getUserId()}" + 
               ":type:#{#exportDto?.type ?: 'default'}"
    )
    public void exportReport(ExportDTO exportDto) {
        // 导出报表逻辑
        System.out.println("导出报表中...");
    }
}
```

### 2. 防重复提交使用示例

#### 基本使用

```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PreventDuplicateSubmit(key = "#{#userId}")
    @PostMapping("/create")
    public ResponseEntity<String> createOrder(@RequestParam String userId, @RequestBody OrderDTO order) {
        // 创建订单逻辑
        return ResponseEntity.ok("订单创建成功");
    }
}
```

#### 使用嵌套对象属性

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PreventDuplicateSubmit(key = "#{#user.id}")
    @PutMapping("/update")
    public ResponseEntity<String> updateUser(@RequestBody User user) {
        // 更新用户信息
        return ResponseEntity.ok("用户信息更新成功");
    }
}
```

#### 使用静态方法获取用户上下文

```java
@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @PreventDuplicateSubmit(
        key = "#{T(com.example.UserContext).getUserId()}",
        expire = "3s",  // 3秒内禁止重复提交
        errorMsg = "操作过于频繁，请稍后重试"
    )
    @PostMapping("/process")
    public ResponseEntity<String> processPayment(@RequestBody PaymentDTO payment) {
        // 处理支付逻辑
        return ResponseEntity.ok("支付处理成功");
    }
}
```

#### 自定义参数签名行为

```java
@RestController
@RequestMapping("/api/forms")
public class FormController {
    
    @PreventDuplicateSubmit(
        key = "form:submit:#{#formType}",
        includeParams = true,  // 包含参数进行签名
        ignoreParams = {"timestamp", "signature", "nonce"},  // 忽略这些参数
        expire = "2s"
    )
    @PostMapping("/submit")
    public ResponseEntity<String> submitForm(
            @RequestParam String formType,
            @RequestBody FormData formData) {
        // 表单提交逻辑
        return ResponseEntity.ok("表单提交成功");
    }
    
    @PreventDuplicateSubmit(
        key = "form:upload",
        includeParams = false  // 不包含参数进行签名，只基于key判断
    )
    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        // 文件上传逻辑
        return ResponseEntity.ok("文件上传成功");
    }
}
```

## 配置项说明

### 分布式锁注解属性

**注意**：当使用默认的prefix值时，会自动在prefix中包含方法的完整路径（包名+类名+方法名）。

- **prefix**：锁的前缀，默认为 "lock:"，与 name 拼接形成完整的锁键名
- **name**：锁的名称，支持 SpEL 表达式，必填项
- **expire**：锁的过期时间，默认 "30s"，支持格式：3s、5min、1h、1day、-1（永不过期）
- **waitTime**：获取锁的等待时间，默认 "5s"，支持格式同上，-1 表示一直等待

### 防重复提交注解属性

**注意**：当使用默认的prefix值时，会自动在prefix中包含方法的完整路径（包名+类名+方法名）。

- **prefix**：缓存的前缀，默认为 "dup:submit:"，与 key 拼接形成完整的缓存键名
- **key**：缓存的key，支持 SpEL 表达式，默认为 "#{#userId}"
- **expire**：过期时间，在此时间内禁止重复提交，默认 "1s"，支持格式同上
- **errorMsg**：重复提交时的错误消息，默认为 "请勿重复提交，请稍后重试"
- **includeParams**：是否包含请求参数进行签名，默认为 true
- **ignoreParams**：忽略的参数名（当 includeParams 为 true 时生效），默认为 {"timestamp", "_"}

### 限流注解属性

**注意**：当使用默认的prefix值时，会自动在prefix中包含方法的完整路径（包名+类名+方法名）。

- **prefix**：缓存键前缀，默认为 "rate:limit:"，与 key 拼接形成完整的缓存键名
- **key**：缓存键，支持 SpEL 表达式，默认为 "#userId"
- **limit**：时间窗口内允许的最大请求数，默认为 10
- **timeWindow**：时间窗口，支持格式：3s 13min 200ms 4h 7day 1month 2year，默认为 "1s"
- **waitTime**：获取令牌的等待时间，支持格式：3s 13min 200ms 4h 7day 1month 2year -1，默认为 "0s"，-1 表示不等待直接拒绝
- **errorMsg**：限流时的错误信息，默认为 "请求过于频繁，请稍后重试"

### Redis 配置项

- **redisson.address**：Redis 服务器地址，格式：redis://host:port
- **redisson.password**：Redis 密码（可选）
- **redisson.database**：Redis 数据库索引，默认为 0
- **redisson.singleServer**：是否使用单节点模式，默认为 true
- **redisson.sentinel**：是否使用哨兵模式
- **redisson.cluster**：是否使用集群模式

## 注意事项

1. **避免死锁**：请确保设置合理的 expire 时间，避免长时间占用锁
2. **性能考虑**：锁的粒度应该尽可能小，避免长时间持有锁
3. **异常处理**：获取锁失败时会抛出 RuntimeException，请在业务层捕获并处理
4. **线程安全**：确保在使用 ThreadLocal 时正确清理，避免内存泄漏
5. **SpEL 表达式**：请确保 SpEL 表达式能够正确解析，避免语法错误

## 异常处理

当获取锁失败时，默认会抛出以下异常：

```java
throw new RuntimeException("获取分布式锁失败，请稍后重试");
```

建议在控制器层统一捕获并处理：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e) {
        ErrorResponse response = new ErrorResponse();
        response.setCode(503);
        response.setMessage(e.getMessage());
        return ResponseEntity.status(503).body(response);
    }
}
```

## 版本历史

### 1.0-SNAPSHOT

- 提供分布式锁功能
- 提供防重复提交功能
- 增强 SpEL 表达式支持，可访问方法参数、嵌套对象属性
- 支持静态方法调用获取上下文信息（如 ThreadLocal 中的用户信息）
- 支持灵活的时间格式配置
- 防重复提交支持参数签名和自定义错误消息
- 集成Redisson原生限流功能，使用RRateLimiter实现高性能滑动窗口限流

## 3. 限流使用示例

```java
@RestController
@RequestMapping("/api/rate-limit")
public class RateLimitController {
    
    // 基本使用
    @RateLimit(key = "#userId", limit = 5, timeWindow = "1s")
    @GetMapping("/basic")
    public String basicLimit(@RequestParam Long userId) {
        return "请求成功，userId: " + userId;
    }
    
    // 嵌套对象属性
    @RateLimit(key = "#user.id", limit = 3, timeWindow = "2s")
    @PostMapping("/nested")
    public String nestedObject(@RequestBody User user) {
        return "请求成功，用户ID: " + user.getId();
    }
    
    // 静态方法获取用户上下文
    @RateLimit(key = "#{T(com.imu.toolkit.redisson.utils.UserContext).getCurrentUserId()}", limit = 10, timeWindow = "5s")
    @GetMapping("/context")
    public String contextLimit() {
        return "基于上下文的限流";
    }
    
    // 自定义等待时间
    @RateLimit(key = "#apiKey", limit = 2, timeWindow = "1s", waitTime = "500ms")
    @GetMapping("/wait")
    public String waitLimit(@RequestParam String apiKey) {
        return "带等待时间的限流，apiKey: " + apiKey;
    }
    
    // 使用不同时间单位
    @RateLimit(key = "#resourceId", limit = 20, timeWindow = "1min", waitTime = "3s")
    @GetMapping("/resource/{resourceId}")
    public String resourceLimit(@PathVariable Long resourceId) {
        return "资源访问成功，resourceId: " + resourceId;
    }
}

// 用户上下文工具类示例
public class UserContext {
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();
    
    public static void setCurrentUserId(Long userId) {
        currentUserId.set(userId);
    }
    
    public static Long getCurrentUserId() {
        return currentUserId.get();
    }
    
    public static void clear() {
        currentUserId.remove();
    }
}
```

## 许可证

[MIT](LICENSE)
