# 项目架构说明

## 三层架构

本项目采用经典的三层架构设计，代码结构清晰，职责分明。

### 包结构

```
com.sunrisejay.jaychat
├── controller/          # 控制器层 - 处理HTTP请求
│   ├── AuthController   # 认证控制器
│   └── ChatController   # 聊天控制器
│
├── service/              # 业务逻辑层 - 处理业务逻辑
│   ├── AuthService      # 认证服务
│   └── ChatService      # 聊天服务
│
├── mapper/              # 数据访问层 - 数据库操作
│   ├── UserMapper       # 用户数据访问
│   ├── ChatMessageMapper      # 消息数据访问
│   ├── ChatSessionMapper       # 会话数据访问
│   └── ChatSessionMemberMapper # 会话成员数据访问
│
├── dto/                 # 数据传输对象
│   ├── request/         # 请求DTO
│   │   ├── LoginRequest
│   │   ├── RegisterRequest
│   │   └── MessageRequest
│   └── response/        # 响应DTO
│       ├── LoginResponse
│       └── MessageResponse
│
├── entity/              # 实体类 - 数据库表映射
│   ├── User
│   ├── ChatMessage
│   └── ChatSession
│
├── common/              # 公共工具类
│   ├── ApiResponse      # 统一响应格式
│   ├── exception/       # 异常类
│   │   └── BusinessException
│   ├── GlobalExceptionHandler  # 全局异常处理
│   └── util/            # 工具类
│       ├── JwtUtil      # JWT工具
│       └── JwtTokenUtil # JWT Token工具
│
└── config/              # 配置类
    ├── MyBatisConfig
    ├── SecurityConfig
    ├── SwaggerConfig
    ├── WebSocketConfig
    ├── WebSocketAuthInterceptor
    └── JwtProperties
```

## 架构层次说明

### 1. Controller层（控制器层）

**职责：**
- 接收HTTP请求
- 参数校验
- 调用Service层处理业务
- 返回响应

**特点：**
- 不包含业务逻辑
- 只负责请求转发和响应封装
- 使用`@RestController`和`@RequestMapping`注解

**示例：**
```java
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }
}
```

### 2. Service层（业务逻辑层）

**职责：**
- 实现业务逻辑
- 调用Mapper层进行数据操作
- 事务管理
- 异常处理

**特点：**
- 不直接操作数据库
- 通过Mapper接口访问数据
- 使用`@Service`注解
- 可以使用`@Transactional`管理事务

**示例：**
```java
@Service
public class AuthService {
    private final UserMapper userMapper;
    
    public LoginResponse login(LoginRequest req) {
        // 业务逻辑处理
        User user = userMapper.findByUsername(req.getUsername());
        // ... 验证密码、生成Token等
        return response;
    }
}
```

### 3. Mapper层（数据访问层）

**职责：**
- 数据库操作
- SQL映射
- 数据持久化

**特点：**
- 使用MyBatis注解或XML配置
- 使用`@Mapper`注解
- 只负责数据访问，不包含业务逻辑

**示例：**
```java
@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    int insert(User user);
}
```

## 数据传输对象（DTO）

### Request DTO
- 用于接收客户端请求参数
- 包含参数校验注解（`@NotBlank`, `@NotNull`等）
- 位于`dto.request`包

### Response DTO
- 用于返回给客户端的数据
- 不包含敏感信息（如密码）
- 位于`dto.response`包

## 实体类（Entity）

- 对应数据库表结构
- 使用Lombok简化代码
- 位于`entity`包
- 与数据库表字段一一对应

## 公共组件

### ApiResponse
统一响应格式：
```java
{
    "code": 0,
    "message": "OK",
    "data": {...}
}
```

### GlobalExceptionHandler
全局异常处理，统一异常响应格式。

### JWT工具
- `JwtUtil`: JWT生成和解析
- `JwtTokenUtil`: 从HTTP请求中提取JWT信息

## 配置类

- `MyBatisConfig`: MyBatis配置，Mapper扫描
- `SecurityConfig`: Spring Security配置
- `WebSocketConfig`: WebSocket配置
- `SwaggerConfig`: API文档配置

## 优势

1. **职责清晰**: 每层职责明确，易于维护
2. **易于测试**: 各层可以独立测试
3. **代码复用**: Service层可以被多个Controller调用
4. **易于扩展**: 新增功能只需在对应层添加代码
5. **标准化**: 符合Java企业级开发规范

## 注意事项

1. **不要跨层调用**: Controller不能直接调用Mapper
2. **事务管理**: 事务注解应放在Service层
3. **异常处理**: 业务异常使用`BusinessException`，系统异常由`GlobalExceptionHandler`统一处理
4. **DTO转换**: Controller接收Request DTO，Service返回Entity或Response DTO

## 与旧架构对比

### 旧架构问题
- `auth`包包含Controller、Service、DTO、工具类等，职责混乱
- `chat`包虽然分层，但包结构不够统一
- `user`包只有实体和Mapper，结构不完整

### 新架构优势
- 统一的三层架构，所有模块都遵循相同结构
- 包结构清晰，易于查找和维护
- 符合Java企业级开发最佳实践
