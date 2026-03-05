# 后端开发从0学习指南

## 📚 学习路径

本指南将带你从零开始理解这个聊天项目的后端实现，按照以下顺序学习：

1. **项目基础架构** → 2. **Spring Boot核心** → 3. **数据库操作** → 4. **API接口开发** → 5. **WebSocket实时通信** → 6. **安全认证**

---

## 第一部分：项目基础架构

### 1.1 项目结构说明

```
src/main/java/com/sunrisejay/jaychat/
├── controller/     # 控制器层 - 接收HTTP请求
├── service/        # 业务逻辑层 - 处理业务
├── mapper/         # 数据访问层 - 操作数据库
├── dto/            # 数据传输对象 - 请求/响应数据
├── entity/         # 实体类 - 数据库表映射
├── common/         # 公共组件 - 工具类、异常处理
└── config/         # 配置类 - 各种配置
```

**为什么这样分层？**
- **Controller**：只负责接收请求、返回响应，不处理业务逻辑
- **Service**：处理所有业务逻辑，比如验证、计算、流程控制
- **Mapper**：只负责数据库的增删改查，不涉及业务
- **DTO**：定义接口的输入输出格式
- **Entity**：对应数据库表结构

**类比理解**：
- Controller = 前台接待（接收客户需求）
- Service = 业务部门（处理具体业务）
- Mapper = 仓库管理员（存取数据）

---

## 第二部分：Spring Boot核心概念

### 2.1 什么是Spring Boot？

Spring Boot是一个**快速开发框架**，帮你：
- 自动配置（不用写很多配置文件）
- 内嵌服务器（不用单独安装Tomcat）
- 简化依赖管理（Maven自动管理）

### 2.2 核心注解理解

#### @SpringBootApplication
```java
@SpringBootApplication
public class JayChatApplication {
    public static void main(String[] args) {
        SpringApplication.run(JayChatApplication.class, args);
    }
}
```
**作用**：启动Spring Boot应用，自动扫描当前包及子包下的所有组件

#### @RestController
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    // ...
}
```
**作用**：
- `@RestController` = `@Controller` + `@ResponseBody`
- 表示这是一个REST API控制器
- 方法返回值自动转为JSON

#### @RequestMapping
```java
@RequestMapping("/api/auth")  // 类级别：所有方法的URL前缀
public class AuthController {
    
    @PostMapping("/login")     // 方法级别：完整URL = /api/auth/login
    public ApiResponse login() { ... }
}
```
**作用**：定义URL路径映射

**常用变体**：
- `@GetMapping` - 处理GET请求
- `@PostMapping` - 处理POST请求
- `@PutMapping` - 处理PUT请求
- `@DeleteMapping` - 处理DELETE请求

#### @Service
```java
@Service
public class AuthService {
    // ...
}
```
**作用**：标识这是一个业务服务类，Spring会自动创建实例并管理

#### @Autowired / 构造函数注入
```java
// 方式1：字段注入（不推荐）
@Autowired
private UserMapper userMapper;

// 方式2：构造函数注入（推荐，我们项目用的）
public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
    this.userMapper = userMapper;
    this.jwtUtil = jwtUtil;
}
```
**作用**：依赖注入，Spring自动提供依赖对象

**为什么用构造函数注入？**
- 更安全（final字段，不可变）
- 更容易测试（可以手动传入Mock对象）
- 更清晰（依赖关系一目了然）

---

## 第三部分：数据库操作（MyBatis）

### 3.1 什么是MyBatis？

MyBatis是一个**持久层框架**，帮你：
- 将Java对象映射到SQL语句
- 自动处理结果集到对象的转换
- 支持动态SQL

### 3.2 Mapper接口

```java
@Mapper  // 标识这是MyBatis的Mapper接口
public interface UserMapper {
    
    // 根据用户名查找用户
    User findByUsername(@Param("username") String username);
    
    // 插入用户
    int insert(User user);
    
    // 根据ID查找
    User selectById(@Param("id") Long id);
}
```

**关键点**：
- `@Mapper`：告诉MyBatis这是一个数据访问接口
- `@Param`：给SQL参数命名
- 返回值：可以是实体对象、List、int（影响行数）等

### 3.3 XML映射文件

```xml
<!-- UserMapper.xml -->
<mapper namespace="com.sunrisejay.jaychat.mapper.UserMapper">
    
    <!-- 结果映射：数据库字段 → Java对象属性 -->
    <resultMap id="UserResultMap" type="com.sunrisejay.jaychat.entity.User">
        <id column="id" property="id"/>                    <!-- 主键 -->
        <result column="username" property="username"/>    <!-- 普通字段 -->
        <result column="created_at" property="createdAt"/> <!-- 下划线转驼峰 -->
    </resultMap>
    
    <!-- 查询语句 -->
    <select id="findByUsername" resultMap="UserResultMap">
        SELECT id, username, password, nickname, avatar, 
               created_at, updated_at
        FROM `user`
        WHERE username = #{username}  <!-- #{username} 是参数占位符 -->
        LIMIT 1
    </select>
    
    <!-- 插入语句 -->
    <insert id="insert" parameterType="com.sunrisejay.jaychat.entity.User" 
            useGeneratedKeys="true" keyProperty="id">
        INSERT INTO `user` (username, password, nickname, avatar, created_at, updated_at)
        VALUES (#{username}, #{password}, #{nickname}, #{avatar}, NOW(), NOW())
    </insert>
</mapper>
```

**关键点**：
- `namespace`：对应Mapper接口的全限定名
- `id`：对应接口方法名
- `#{}`：参数占位符，MyBatis会自动转义，防止SQL注入
- `useGeneratedKeys="true"`：自动获取数据库生成的主键
- `keyProperty="id"`：将主键值赋给对象的id属性

**#{ } vs ${ } 的区别**：
- `#{username}`：预编译，安全，推荐使用
- `${username}`：字符串拼接，不安全，容易SQL注入

---

## 第四部分：API接口开发实战

### 4.1 完整请求流程

让我们看一个完整的例子：用户登录

```
1. 前端发送请求
   POST /api/auth/login
   {
     "username": "test",
     "password": "123456"
   }

2. Controller接收请求
   ↓
3. Service处理业务逻辑
   ↓
4. Mapper查询数据库
   ↓
5. Service返回结果
   ↓
6. Controller封装响应
   ↓
7. 返回JSON给前端
```

### 4.2 代码逐行解析

#### Step 1: Controller层 - 接收请求

```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthService authService;  // 依赖业务服务
    
    // 构造函数注入
    public AuthController(AuthService authService, JwtTokenUtil jwtTokenUtil) {
        this.authService = authService;
        this.jwtTokenUtil = jwtTokenUtil;
    }
    
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // @Valid: 验证请求参数（根据DTO中的注解）
        // @RequestBody: 将JSON请求体转为Java对象
        // LoginRequest: 请求参数DTO
        
        return ApiResponse.success(authService.login(request));
        // 调用Service处理业务，然后封装成统一响应格式
    }
}
```

**关键注解**：
- `@Valid`：启用参数校验（配合DTO中的`@NotBlank`等）
- `@RequestBody`：将JSON转为Java对象
- `ApiResponse`：统一响应格式

#### Step 2: DTO - 定义请求格式

```java
@Data  // Lombok注解，自动生成getter/setter
public class LoginRequest {
    
    @NotBlank(message = "用户名不能为空")  // 校验：不能为空
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
}
```

**为什么用DTO？**
- 控制接口的输入输出格式
- 可以添加校验规则
- 隐藏实体类的敏感字段（如密码）

#### Step 3: Service层 - 业务逻辑

```java
@Service
public class AuthService {
    
    private final UserMapper userMapper;  // 依赖数据访问层
    private final JwtUtil jwtUtil;        // 依赖JWT工具
    
    public LoginResponse login(LoginRequest req) {
        // 1. 查询用户
        User user = userMapper.findByUsername(req.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");  // 抛出业务异常
        }
        
        // 2. 验证密码
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        
        // 3. 生成Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        
        // 4. 构建响应
        LoginResponse resp = new LoginResponse();
        resp.setToken(token);
        user.setPassword(null);  // 清除密码，不返回给前端
        resp.setUser(user);
        
        return resp;
    }
}
```

**业务逻辑要点**：
1. **验证输入**：检查用户是否存在
2. **业务处理**：验证密码、生成Token
3. **异常处理**：使用`BusinessException`抛出业务异常
4. **数据安全**：不返回敏感信息（密码）

#### Step 4: Mapper层 - 数据库查询

```java
@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
}
```

对应的XML：
```xml
<select id="findByUsername" resultMap="UserResultMap">
    SELECT id, username, password, nickname, avatar, created_at, updated_at
    FROM `user`
    WHERE username = #{username}
    LIMIT 1
</select>
```

---

## 第五部分：异常处理机制

### 5.1 为什么需要全局异常处理？

**问题**：每个Controller方法都要try-catch，代码重复

**解决**：使用`@RestControllerAdvice`统一处理

### 5.2 全局异常处理器

```java
@RestControllerAdvice  // 全局异常处理
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // 处理业务异常
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // 返回400状态码
    public ApiResponse<Void> handleBusinessException(BusinessException e) {
        logger.warn("业务异常: {}", e.getMessage());
        return ApiResponse.error(e.getCode(), e.getMessage());
    }
    
    // 处理参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        // 提取所有校验错误信息
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return ApiResponse.error(400, message);
    }
    
    // 处理其他异常
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)  // 返回500状态码
    public ApiResponse<Void> handleException(Exception e) {
        logger.error("未知异常", e);  // 记录详细错误日志
        return ApiResponse.error(500, "服务器内部错误");
    }
}
```

**工作流程**：
1. Service抛出异常 → 
2. 被`@ExceptionHandler`捕获 → 
3. 转换为统一响应格式 → 
4. 返回给前端

**好处**：
- 代码简洁（Controller不需要try-catch）
- 响应格式统一
- 错误信息友好

---

## 第六部分：JWT认证机制

### 6.1 什么是JWT？

JWT（JSON Web Token）是一种**无状态的认证方式**：
- 用户登录后，服务器生成Token
- 客户端保存Token，每次请求带上
- 服务器验证Token，确认用户身份

**Token结构**：
```
Header.Payload.Signature
```

### 6.2 JWT工具类

```java
@Component  // Spring组件，自动管理
public class JwtUtil {
    
    private SecretKey key;  // 签名密钥
    
    // 生成Token
    public String generateToken(Long userId, String username) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expireSeconds * 1000);
        
        return Jwts.builder()
                .setSubject(String.valueOf(userId))  // 主题：用户ID
                .claim("uid", userId)                // 自定义字段：用户ID
                .claim("username", username)         // 自定义字段：用户名
                .setIssuedAt(now)                    // 签发时间
                .setExpiration(expiry)                // 过期时间
                .signWith(key, SignatureAlgorithm.HS256)  // 签名
                .compact();
    }
    
    // 解析Token
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
```

### 6.3 从请求中提取用户信息

```java
@Component
public class JwtTokenUtil {
    
    private final JwtUtil jwtUtil;
    
    // 从HTTP请求头中提取Token并解析
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        // Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;  // 没有Token
        }
        
        String token = authHeader.substring(7);  // 去掉"Bearer "前缀
        Claims claims = jwtUtil.parseClaims(token);
        return claims.get("uid", Long.class);  // 提取用户ID
    }
}
```

**使用示例**：
```java
@GetMapping("/sessions")
public ApiResponse<List<ChatSession>> getSessions(HttpServletRequest request) {
    Long userId = jwtTokenUtil.getUserIdFromRequest(request);
    if (userId == null) {
        return ApiResponse.error(401, "未登录");
    }
    return ApiResponse.success(chatService.getSessionsByUserId(userId));
}
```

---

## 第七部分：WebSocket实时通信

### 7.1 为什么需要WebSocket？

**HTTP的局限**：
- 只能客户端主动请求
- 服务器无法主动推送消息

**WebSocket的优势**：
- 双向通信
- 服务器可以主动推送
- 适合实时聊天

### 7.2 WebSocket配置

```java
@Configuration
@EnableWebSocketMessageBroker  // 启用WebSocket消息代理
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单消息代理，客户端可以订阅这些前缀的目的地
        config.enableSimpleBroker("/topic", "/user");
        // 客户端发送消息的前缀
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册WebSocket端点
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // 允许跨域
                .withSockJS();  // 使用SockJS（兼容性更好）
    }
}
```

**关键概念**：
- `/topic`：广播消息（群聊）
- `/user`：点对点消息（私聊）
- `/app`：客户端发送消息的前缀

### 7.3 WebSocket认证拦截器

```java
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    private final JwtUtil jwtUtil;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        // 只在连接时进行认证
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 从请求头中获取Token
            String token = accessor.getFirstNativeHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
                Claims claims = jwtUtil.parseClaims(token);
                
                // 创建Principal，存储用户信息
                Long userId = claims.get("uid", Long.class);
                String username = claims.get("username", String.class);
                accessor.setUser(new StompPrincipal(userId, username));
            }
        }
        
        return message;
    }
}
```

### 7.4 处理WebSocket消息

```java
@RestController
public class ChatController {
    
    // 处理客户端发送的消息
    @MessageMapping("/chat.send")  // 完整路径：/app/chat.send
    public void sendMessage(@Payload MessageRequest request, 
                           SimpMessageHeaderAccessor headerAccessor) {
        // @Payload: 消息体
        // headerAccessor: 可以获取用户信息
        
        // 从Principal中获取用户ID
        Principal principal = headerAccessor.getUser();
        Long senderId = getUserIdFromPrincipal(principal);
        
        // 保存消息到数据库
        MessageResponse response = chatService.sendMessage(senderId, request);
        
        // 广播消息到所有订阅者
        String destination = "/topic/session." + request.getSessionId();
        messagingTemplate.convertAndSend(destination, response);
    }
}
```

**消息流程**：
1. 客户端发送 → `/app/chat.send`
2. Controller接收 → 保存到数据库
3. 广播消息 → `/topic/session.123`
4. 所有订阅者收到 → 实时显示

---

## 第八部分：事务管理

### 8.1 什么是事务？

**事务的特性（ACID）**：
- **原子性**：要么全部成功，要么全部失败
- **一致性**：数据保持一致
- **隔离性**：并发事务互不干扰
- **持久性**：提交后永久保存

### 8.2 使用@Transactional

```java
@Service
public class ChatService {
    
    @Transactional  // 开启事务
    public MessageResponse sendMessage(Long senderId, MessageRequest request) {
        // 1. 验证会话
        ChatSession session = sessionMapper.selectById(request.getSessionId());
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        
        // 2. 保存消息
        ChatMessage message = new ChatMessage();
        message.setContent(request.getContent());
        messageMapper.insert(message);  // 如果这里失败，整个方法回滚
        
        // 3. 更新其他数据
        // ...
        
        return response;
    }
}
```

**关键点**：
- `@Transactional`：方法内所有数据库操作在一个事务中
- 如果任何一步失败，所有操作都会回滚
- 保证数据一致性

---

## 第九部分：日志系统

### 9.1 为什么使用日志？

- **调试**：查看程序运行过程
- **监控**：了解系统运行状态
- **排查问题**：出现错误时追踪原因

### 9.2 SLF4J使用

```java
@Service
public class AuthService {
    
    // 创建日志对象
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    public void register(RegisterRequest req) {
        // 记录不同级别的日志
        logger.info("用户注册: {}", req.getUsername());      // 信息
        logger.warn("注册失败，用户名已存在: {}", req.getUsername());  // 警告
        logger.error("注册异常", e);  // 错误（带异常）
        logger.debug("调试信息: {}", someData);  // 调试
    }
}
```

**日志级别（从低到高）**：
- `DEBUG`：调试信息（开发时用）
- `INFO`：重要信息（正常运行）
- `WARN`：警告信息（可能有问题）
- `ERROR`：错误信息（需要处理）

---

## 第十部分：配置文件详解

### 10.1 application.properties

```properties
# 数据源配置
spring.datasource.url=jdbc:mysql://120.53.242.78:3306/jay_chat?useSSL=false&serverTimezone=UTC&characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=123123
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# MyBatis配置
mybatis.mapper-locations=classpath:mappers/*.xml  # XML文件位置
mybatis.type-aliases-package=com.sunrisejay.jaychat  # 实体类包路径

# 服务器端口
server.port=8080

# JWT配置（自定义配置）
jaychat.jwt.secret=ChangeThisSecretToSomethingSecure
jaychat.jwt.expire-seconds=604800  # 7天
```

**配置读取**：
```java
@ConfigurationProperties(prefix = "jaychat.jwt")  // 读取配置
public class JwtProperties {
    private String secret;        // 对应 jaychat.jwt.secret
    private long expireSeconds;   // 对应 jaychat.jwt.expire-seconds
}
```

---

## 实践练习

### 练习1：理解请求流程

**任务**：追踪一个登录请求的完整流程

1. 前端发送：`POST /api/auth/login`
2. 找到：`AuthController.login()`
3. 调用：`AuthService.login()`
4. 查询：`UserMapper.findByUsername()`
5. 返回：`LoginResponse`

**问题**：
- 每个步骤的作用是什么？
- 数据是如何流转的？

### 练习2：添加新接口

**任务**：添加一个"修改昵称"的接口

**步骤**：
1. 在`AuthController`添加方法
2. 创建`UpdateNicknameRequest` DTO
3. 在`AuthService`实现业务逻辑
4. 在`UserMapper`添加更新方法
5. 测试接口

### 练习3：理解异常处理

**任务**：故意触发一个异常，观察处理流程

1. 在Service中抛出`BusinessException`
2. 观察`GlobalExceptionHandler`如何处理
3. 查看返回给前端的响应格式

---

## 常见问题解答

### Q1: Controller、Service、Mapper的区别？

**A**: 
- **Controller**：接收请求，调用Service，返回响应（不处理业务）
- **Service**：处理业务逻辑，调用Mapper（不直接操作数据库）
- **Mapper**：操作数据库（不处理业务）

### Q2: 为什么要用DTO而不是直接用Entity？

**A**:
- Entity可能包含敏感字段（如密码）
- DTO可以只暴露需要的字段
- DTO可以添加校验注解
- 接口更灵活（可以组合多个Entity的数据）

### Q3: @Autowired和构造函数注入哪个好？

**A**: 推荐构造函数注入
- 更安全（final字段）
- 更容易测试
- 依赖关系清晰

### Q4: 什么时候用@Transactional？

**A**: 
- 需要保证多个数据库操作要么全部成功，要么全部失败
- 例如：转账（扣款+加款）、创建会话（创建会话+添加成员）

### Q5: 如何调试后端代码？

**A**:
1. 使用日志：`logger.debug()`输出调试信息
2. 使用断点：IDE中打断点，逐步调试
3. 查看日志文件：`logs/`目录
4. 使用Swagger：`http://localhost:8080/doc.html`测试接口

---

## 学习建议

### 1. 循序渐进
- 先理解基础概念（Controller、Service、Mapper）
- 再学习高级特性（WebSocket、事务）
- 最后学习最佳实践（异常处理、日志）

### 2. 动手实践
- 不要只看代码，要自己写
- 尝试修改代码，观察效果
- 遇到问题，先思考再查资料

### 3. 理解原理
- 不要死记硬背
- 理解为什么这样设计
- 理解数据流转过程

### 4. 查阅文档
- Spring Boot官方文档
- MyBatis官方文档
- Stack Overflow（遇到问题时）

---

## 下一步学习

掌握了这些基础后，可以学习：

1. **Spring Security**：更完善的权限控制
2. **Redis**：缓存和分布式锁
3. **消息队列**：异步处理
4. **微服务**：服务拆分
5. **Docker**：容器化部署

---

## 总结

后端开发的核心：
1. **接收请求**（Controller）
2. **处理业务**（Service）
3. **操作数据**（Mapper）
4. **返回响应**（Controller）

记住这个流程，大部分功能都是这个模式！
