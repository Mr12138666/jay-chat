# 后端代码详细解析 - 实战示例

## 📖 示例1：用户注册完整流程（逐行解析）

让我们通过用户注册功能，完整理解整个后端流程。

### 第一步：前端发送请求

```javascript
// 前端代码（参考）
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "username": "testuser",
  "password": "123456",
  "nickname": "测试用户"
}
```

### 第二步：Controller接收请求

```java
@RestController                    // 👈 这是什么？
@RequestMapping("/api/auth")       // 👈 这是什么？
public class AuthController {
    
    // 👇 为什么用final？为什么用构造函数？
    private final AuthService authService;
    
    // 👇 构造函数注入 - 这是依赖注入的一种方式
    public AuthController(AuthService authService, JwtTokenUtil jwtTokenUtil) {
        this.authService = authService;  // Spring自动提供AuthService实例
        this.jwtTokenUtil = jwtTokenUtil;
    }
    
    /**
     * 用户注册接口
     * 
     * @PostMapping("/register") 
     *   👆 完整URL = /api/auth/register
     *   只接受POST请求
     * 
     * @Valid @RequestBody RegisterRequest request
     *   👆 @Valid: 自动验证请求参数（根据RegisterRequest中的注解）
     *   👆 @RequestBody: 将JSON请求体转为RegisterRequest对象
     * 
     * ApiResponse<Void>
     *   👆 统一响应格式，Void表示没有返回数据
     */
    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        // 👇 调用Service处理业务逻辑
        authService.register(request);
        
        // 👇 返回成功响应（没有数据，所以是null）
        return ApiResponse.success(null);
    }
}
```

**关键点解释**：

1. **@RestController**：
   - = `@Controller` + `@ResponseBody`
   - 方法返回值自动转为JSON
   - 不需要在每个方法上加`@ResponseBody`

2. **@RequestMapping("/api/auth")**：
   - 类级别的路径前缀
   - 所有方法的URL都会加上这个前缀
   - `/register` → 完整路径是 `/api/auth/register`

3. **构造函数注入**：
   ```java
   // Spring会自动创建AuthService实例并传入
   // 你不需要手动 new AuthService()
   public AuthController(AuthService authService) {
       this.authService = authService;
   }
   ```

4. **@Valid**：
   - 触发参数校验
   - 如果RegisterRequest中的`@NotBlank`校验失败
   - 会自动抛出异常，被`GlobalExceptionHandler`捕获

### 第三步：DTO定义请求格式

```java
@Data  // 👈 Lombok注解，自动生成getter/setter/toString等方法
public class RegisterRequest {
    
    @NotBlank(message = "用户名不能为空")  // 👈 校验规则
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    @NotBlank(message = "昵称不能为空")
    private String nickname;
}
```

**关键点**：

1. **@Data**（Lombok）：
   ```java
   // 等价于手动写：
   public String getUsername() { return username; }
   public void setUsername(String username) { this.username = username; }
   // ... 还有equals、hashCode、toString等方法
   ```

2. **@NotBlank**：
   - 不能为null
   - 不能为空字符串
   - 不能只有空格
   - 如果校验失败，message会作为错误信息返回

### 第四步：Service处理业务逻辑

```java
@Service  // 👈 标识这是业务服务类，Spring会自动管理
public class AuthService {
    
    // 👇 日志对象 - 用于记录运行信息
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    // 👇 依赖的数据访问层和工具类
    private final UserMapper userMapper;      // 操作数据库
    private final JwtUtil jwtUtil;            // JWT工具（注册暂时不用）
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    /**
     * 用户注册业务逻辑
     */
    public void register(RegisterRequest req) {
        // ========== 步骤1：参数校验 ==========
        if (!StringUtils.hasText(req.getUsername()) || !StringUtils.hasText(req.getPassword())) {
            // 👇 抛出业务异常，会被GlobalExceptionHandler捕获
            throw new BusinessException("用户名和密码不能为空");
        }
        
        // ========== 步骤2：检查用户名是否已存在 ==========
        User existing = userMapper.findByUsername(req.getUsername());
        if (existing != null) {
            logger.warn("注册失败，用户名已存在: {}", req.getUsername());
            throw new BusinessException("用户名已存在");
        }
        
        // ========== 步骤3：创建新用户对象 ==========
        User user = new User();
        user.setUsername(req.getUsername());
        
        // 👇 密码加密 - 不能直接存储明文密码！
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        
        // ========== 步骤4：处理昵称 ==========
        String nickname = req.getNickname();
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = req.getUsername();  // 如果没填昵称，用用户名
        }
        user.setNickname(nickname);
        
        // ========== 步骤5：保存到数据库 ==========
        userMapper.insert(user);
        
        // ========== 步骤6：记录日志 ==========
        logger.info("用户注册成功: {}", req.getUsername());
    }
}
```

**关键点解释**：

1. **密码加密**：
   ```java
   // ❌ 错误：直接存储明文密码
   user.setPassword(req.getPassword());
   
   // ✅ 正确：使用BCrypt加密
   user.setPassword(passwordEncoder.encode(req.getPassword()));
   ```
   - BCrypt是单向加密，无法解密
   - 登录时用`matches()`方法验证

2. **异常处理**：
   ```java
   throw new BusinessException("用户名已存在");
   ```
   - 抛出异常后，方法立即停止执行
   - 异常被`GlobalExceptionHandler`捕获
   - 自动转为错误响应返回给前端

3. **日志记录**：
   ```java
   logger.info("用户注册成功: {}", req.getUsername());
   ```
   - `{}`是占位符，会被后面的参数替换
   - 等价于：`"用户注册成功: " + req.getUsername()`
   - 但性能更好，且只在需要时才拼接字符串

### 第五步：Mapper查询数据库

```java
@Mapper  // 👈 告诉MyBatis这是一个数据访问接口
public interface UserMapper {
    
    /**
     * 根据用户名查找用户
     * @Param("username") - 给SQL参数命名
     */
    User findByUsername(@Param("username") String username);
    
    /**
     * 插入用户
     * @return 影响的行数（成功返回1，失败返回0）
     */
    int insert(User user);
}
```

**对应的XML文件**：

```xml
<mapper namespace="com.sunrisejay.jaychat.mapper.UserMapper">
    
    <!-- 结果映射：数据库字段 → Java对象属性 -->
    <resultMap id="UserResultMap" type="com.sunrisejay.jaychat.entity.User">
        <id column="id" property="id"/>                    <!-- 主键 -->
        <result column="username" property="username"/>    <!-- 普通字段 -->
        <result column="password" property="password"/>
        <result column="nickname" property="nickname"/>
        <result column="avatar" property="avatar"/>
        <result column="created_at" property="createdAt"/> <!-- 下划线转驼峰 -->
        <result column="updated_at" property="updatedAt"/>
    </resultMap>
    
    <!-- 查询：根据用户名查找 -->
    <select id="findByUsername" resultMap="UserResultMap">
        SELECT id, username, password, nickname, avatar, created_at, updated_at
        FROM `user`
        WHERE username = #{username}  <!-- #{username} 会被替换为实际参数值 -->
        LIMIT 1
    </select>
    
    <!-- 插入：创建新用户 -->
    <insert id="insert" 
            parameterType="com.sunrisejay.jaychat.entity.User" 
            useGeneratedKeys="true" 
            keyProperty="id">
        <!-- 
        useGeneratedKeys="true": 使用数据库自动生成的主键
        keyProperty="id": 将生成的主键值赋给User对象的id属性
        -->
        INSERT INTO `user` (username, password, nickname, avatar, created_at, updated_at)
        VALUES (#{username}, #{password}, #{nickname}, #{avatar}, NOW(), NOW())
    </insert>
</mapper>
```

**关键点**：

1. **#{username}**：
   ```sql
   -- MyBatis会自动处理：
   WHERE username = ?  -- 预编译语句
   -- 参数值：'testuser'
   -- 这样防止SQL注入攻击
   ```

2. **useGeneratedKeys**：
   ```java
   // 插入后，user.getId()会自动有值
   userMapper.insert(user);
   Long newUserId = user.getId();  // 数据库自动生成的主键
   ```

3. **resultMap**：
   - 将数据库的`created_at`映射到Java的`createdAt`
   - 自动处理下划线转驼峰命名

### 第六步：异常处理

如果注册时用户名已存在，会发生什么？

```java
// Service中抛出异常
throw new BusinessException("用户名已存在");

// ↓ 被GlobalExceptionHandler捕获

@ExceptionHandler(BusinessException.class)
public ApiResponse<Void> handleBusinessException(BusinessException e) {
    return ApiResponse.error(e.getCode(), e.getMessage());
    // 返回：{ "code": 400, "message": "用户名已存在", "data": null }
}
```

---

## 📖 示例2：用户登录流程（对比注册）

### 登录的特殊之处

```java
public LoginResponse login(LoginRequest req) {
    // 1. 查找用户
    User user = userMapper.findByUsername(req.getUsername());
    if (user == null) {
        throw new BusinessException("用户不存在");
    }
    
    // 2. 验证密码（重点！）
    // 👇 BCrypt的验证方法
    if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
        throw new BusinessException("密码错误");
    }
    
    // 3. 生成JWT Token（登录特有）
    String token = jwtUtil.generateToken(user.getId(), user.getUsername());
    
    // 4. 构建响应
    LoginResponse resp = new LoginResponse();
    resp.setToken(token);
    user.setPassword(null);  // 清除密码，不返回
    resp.setUser(user);
    
    return resp;
}
```

**密码验证原理**：
```java
// 注册时：加密存储
String encrypted = passwordEncoder.encode("123456");
// 结果：$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy

// 登录时：验证
boolean matches = passwordEncoder.matches("123456", encrypted);
// matches = true（密码正确）
// matches = false（密码错误）
```

**为什么不能解密？**
- BCrypt是**单向哈希**，无法反向解密
- 只能通过`matches()`方法验证
- 即使数据库泄露，攻击者也无法得到原始密码

---

## 📖 示例3：获取会话列表（理解认证）

```java
@GetMapping("/sessions")
public ApiResponse<List<ChatSession>> getSessions(HttpServletRequest request) {
    // 👇 从请求头中提取Token并解析用户ID
    Long userId = jwtTokenUtil.getUserIdFromRequest(request);
    
    if (userId == null) {
        // 没有Token或Token无效
        return ApiResponse.error(401, "未登录");
    }
    
    // 👇 调用Service，传入用户ID
    return ApiResponse.success(chatService.getSessionsByUserId(userId));
}
```

**请求头格式**：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**JwtTokenUtil的工作流程**：
```java
public Long getUserIdFromRequest(HttpServletRequest request) {
    // 1. 获取请求头
    String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
    // 结果：Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
    
    // 2. 检查格式
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return null;  // 没有Token
    }
    
    // 3. 提取Token（去掉"Bearer "前缀）
    String token = authHeader.substring(7);
    // 结果：eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
    
    // 4. 解析Token
    Claims claims = jwtUtil.parseClaims(token);
    
    // 5. 提取用户ID
    return claims.get("uid", Long.class);
}
```

---

## 📖 示例4：发送消息（理解WebSocket）

### HTTP方式发送消息

```java
@PostMapping("/messages")
public ApiResponse<MessageResponse> sendMessage(
        @Valid @RequestBody MessageRequest request,
        HttpServletRequest httpRequest) {
    
    // 1. 获取发送者ID（从Token中）
    Long senderId = jwtTokenUtil.getUserIdFromRequest(httpRequest);
    if (senderId == null) {
        return ApiResponse.error(401, "未登录");
    }
    
    // 2. 保存消息到数据库
    MessageResponse response = chatService.sendMessage(senderId, request);
    
    // 3. 广播消息（通过WebSocket）
    String destination = "/topic/session." + request.getSessionId();
    messagingTemplate.convertAndSend(destination, response);
    
    return ApiResponse.success(response);
}
```

### WebSocket方式发送消息

```java
@MessageMapping("/chat.send")  // 完整路径：/app/chat.send
public void sendMessage(
        @Payload MessageRequest request,  // 消息体
        SimpMessageHeaderAccessor headerAccessor) {  // 可以获取用户信息
    
    // 1. 从WebSocket连接中获取用户ID
    Principal principal = headerAccessor.getUser();
    Long senderId = getUserIdFromPrincipal(principal);
    
    // 2. 保存消息
    MessageResponse response = chatService.sendMessage(senderId, request);
    
    // 3. 广播给所有订阅者
    String destination = "/topic/session." + request.getSessionId();
    messagingTemplate.convertAndSend(destination, response);
}
```

**两种方式的区别**：
- **HTTP**：需要前端主动请求，适合测试
- **WebSocket**：实时双向通信，适合生产环境

---

## 📖 示例5：事务管理（理解@Transactional）

```java
@Transactional  // 👈 开启事务
public ChatSession getOrCreateDefaultSession(Long userId) {
    // 以下所有数据库操作都在一个事务中
    
    // 1. 查找会话
    ChatSession session = findGlobalDefaultSession();
    
    if (session == null) {
        // 2. 创建新会话
        session = new ChatSession();
        session.setName("公共聊天室");
        sessionMapper.insert(session);  // 数据库操作1
        
        // 3. 添加用户到会话
        memberMapper.insert(session.getId(), userId);  // 数据库操作2
        
        // 👆 如果这里失败，上面的insert也会回滚
    } else {
        // 4. 确保用户在会话中
        if (!memberMapper.exists(session.getId(), userId)) {
            memberMapper.insert(session.getId(), userId);  // 数据库操作
        }
    }
    
    return session;
    // 👆 方法正常结束，事务提交（所有操作生效）
    // 👆 如果抛出异常，事务回滚（所有操作撤销）
}
```

**事务的作用**：
```java
// 场景：创建会话时，如果添加成员失败，会话也不应该创建

@Transactional
public void createSession() {
    sessionMapper.insert(session);      // 操作1：创建会话
    memberMapper.insert(sessionId, userId);  // 操作2：添加成员（如果这里失败）
    // 👆 操作1也会自动回滚，保证数据一致性
}
```

---

## 🎯 实践练习

### 练习1：理解数据流转

**任务**：追踪一个登录请求，画出数据流转图

```
前端 → Controller → Service → Mapper → 数据库
  ↑                                    ↓
  └────────── 响应返回 ←──────────────┘
```

**问题**：
1. 每个步骤的数据格式是什么？
2. 异常在哪里被处理？
3. Token在哪里生成？

### 练习2：添加新功能

**任务**：实现"修改用户昵称"功能

**步骤**：
1. 在`AuthController`添加方法：
   ```java
   @PutMapping("/nickname")
   public ApiResponse<Void> updateNickname(...) {
       // TODO
   }
   ```

2. 创建DTO：
   ```java
   public class UpdateNicknameRequest {
       @NotBlank
       private String nickname;
   }
   ```

3. 在`AuthService`实现：
   ```java
   public void updateNickname(Long userId, String nickname) {
       // TODO: 更新数据库
   }
   ```

4. 在`UserMapper`添加方法：
   ```java
   int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);
   ```

5. 在XML中添加SQL：
   ```xml
   <update id="updateNickname">
       UPDATE `user` SET nickname = #{nickname} WHERE id = #{id}
   </update>
   ```

### 练习3：调试技巧

**任务**：在代码中添加日志，观察执行流程

```java
public LoginResponse login(LoginRequest req) {
    logger.debug("开始登录，用户名: {}", req.getUsername());  // 👈 添加这行
    
    User user = userMapper.findByUsername(req.getUsername());
    logger.debug("查询到用户: {}", user != null ? user.getId() : "null");  // 👈 添加这行
    
    if (user == null) {
        logger.warn("用户不存在: {}", req.getUsername());
        throw new BusinessException("用户不存在");
    }
    
    // ... 继续添加日志
}
```

**查看日志**：
- 控制台输出
- 日志文件：`logs/`目录

---

## ❓ 常见疑问解答

### Q1: 为什么Controller不直接操作数据库？

**A**: 
- **职责分离**：Controller只负责接收请求，不处理业务
- **代码复用**：Service可以被多个Controller调用
- **易于测试**：可以单独测试Service逻辑

### Q2: 为什么Service不直接写SQL？

**A**:
- **关注点分离**：Service关注业务，Mapper关注数据
- **易于维护**：SQL集中管理，修改方便
- **支持多种数据库**：MyBatis可以适配不同数据库

### Q3: DTO和Entity有什么区别？

**A**:
```java
// Entity：对应数据库表
public class User {
    private Long id;
    private String username;
    private String password;  // 敏感信息
    // ...
}

// DTO：只暴露需要的字段
public class LoginResponse {
    private String token;
    private User user;  // 但user的password会被置为null
}
```

### Q4: 什么时候用@Transactional？

**A**: 
- 需要保证多个操作要么全部成功，要么全部失败
- 例如：转账、创建会话+添加成员

### Q5: 如何学习更多？

**A**:
1. **阅读官方文档**：Spring Boot、MyBatis
2. **看源码**：理解框架工作原理
3. **实践**：自己写代码，遇到问题再查资料
4. **调试**：打断点，观察数据流转

---

## 📚 推荐学习资源

1. **Spring Boot官方文档**：https://spring.io/projects/spring-boot
2. **MyBatis官方文档**：https://mybatis.org/mybatis-3/
3. **Spring WebSocket文档**：https://docs.spring.io/spring-framework/reference/web/websocket.html

---

## 总结

记住这个核心流程：

```
请求 → Controller → Service → Mapper → 数据库
  ↑                                    ↓
  └────────── 响应 ←──────────────────┘
```

每一层都有明确的职责，不要越界！
