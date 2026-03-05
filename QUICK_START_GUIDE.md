# 后端开发快速入门指南

## 🚀 5分钟快速理解

### 核心概念（一句话理解）

1. **Controller** = 前台接待（接收请求，返回响应）
2. **Service** = 业务部门（处理具体业务逻辑）
3. **Mapper** = 仓库管理员（存取数据库）
4. **DTO** = 申请表（定义接口的输入输出格式）
5. **Entity** = 档案（对应数据库表）

### 数据流转（3步理解）

```
前端请求 → Controller接收 → Service处理 → Mapper查询数据库
                                    ↓
前端响应 ← Controller返回 ← Service返回 ← Mapper返回数据
```

---

## 📝 代码示例对比

### 示例：用户登录

#### 1. Controller层（接收请求）

```java
@PostMapping("/login")
public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request) {
    // 👆 接收JSON请求体，转为LoginRequest对象
    
    LoginResponse response = authService.login(request);
    // 👆 调用Service处理业务
    
    return ApiResponse.success(response);
    // 👆 返回JSON响应
}
```

**类比**：前台收到客户需求，交给业务部门处理，然后把结果告诉客户

#### 2. Service层（处理业务）

```java
public LoginResponse login(LoginRequest req) {
    // 1. 查询用户
    User user = userMapper.findByUsername(req.getUsername());
    
    // 2. 验证密码
    if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
        throw new BusinessException("密码错误");
    }
    
    // 3. 生成Token
    String token = jwtUtil.generateToken(user.getId(), user.getUsername());
    
    // 4. 返回结果
    return new LoginResponse(token, user);
}
```

**类比**：业务部门处理具体业务，需要数据时找仓库管理员

#### 3. Mapper层（操作数据库）

```java
// 接口定义
User findByUsername(String username);

// XML实现
<select id="findByUsername">
    SELECT * FROM user WHERE username = #{username}
</select>
```

**类比**：仓库管理员根据需求，从仓库中取数据

---

## 🎯 关键注解速查

| 注解 | 作用 | 示例 |
|------|------|------|
| `@RestController` | 标识REST API控制器 | `@RestController` |
| `@RequestMapping` | 定义URL路径 | `@RequestMapping("/api/auth")` |
| `@PostMapping` | 处理POST请求 | `@PostMapping("/login")` |
| `@GetMapping` | 处理GET请求 | `@GetMapping("/me")` |
| `@Service` | 标识业务服务类 | `@Service` |
| `@Mapper` | 标识数据访问接口 | `@Mapper` |
| `@RequestBody` | 将JSON转为Java对象 | `@RequestBody LoginRequest` |
| `@Valid` | 启用参数校验 | `@Valid @RequestBody` |
| `@Transactional` | 开启事务 | `@Transactional` |

---

## 🔍 调试技巧

### 1. 查看日志

```java
logger.info("用户登录: {}", username);
logger.debug("查询用户: {}", user);
logger.warn("登录失败: {}", error);
logger.error("系统异常", e);
```

**查看位置**：
- 控制台输出
- 日志文件：`logs/`目录

### 2. 使用Swagger测试

访问：`http://localhost:8080/doc.html`

可以：
- 查看所有接口
- 测试接口
- 查看请求/响应格式

### 3. 打断点调试

在IDE中：
1. 在代码行号左侧点击，设置断点
2. 启动应用（Debug模式）
3. 发送请求
4. 程序会在断点处暂停
5. 可以查看变量值、单步执行

---

## 📚 学习路径

### 第1天：理解基础

1. ✅ 阅读 `BACKEND_LEARNING_GUIDE.md` 第一部分
2. ✅ 理解Controller、Service、Mapper的作用
3. ✅ 运行项目，观察日志

### 第2天：理解数据流转

1. ✅ 追踪一个登录请求的完整流程
2. ✅ 理解每个步骤的作用
3. ✅ 查看数据库中的数据

### 第3天：理解认证

1. ✅ 理解JWT Token的作用
2. ✅ 观察Token的生成和验证
3. ✅ 理解为什么需要Token

### 第4天：理解WebSocket

1. ✅ 理解WebSocket和HTTP的区别
2. ✅ 观察消息的发送和接收
3. ✅ 理解广播机制

### 第5天：实践练习

1. ✅ 完成 `PRACTICE_EXERCISES.md` 中的练习
2. ✅ 尝试添加新功能
3. ✅ 调试和排查问题

---

## 💡 学习建议

### ✅ 应该做的

1. **动手实践**：不要只看代码，要自己写
2. **理解原理**：不要死记硬背，理解为什么
3. **查看日志**：日志是最好的老师
4. **使用调试器**：打断点，观察数据流转
5. **查阅文档**：遇到问题先查文档

### ❌ 不应该做的

1. **不要跳过基础**：基础不牢，地动山摇
2. **不要只看不练**：理论必须结合实践
3. **不要害怕错误**：错误是最好的学习机会
4. **不要复制粘贴**：理解后再使用

---

## 🆘 遇到问题怎么办？

### 问题1：代码看不懂

**解决**：
1. 查看 `BACKEND_LEARNING_GUIDE.md`
2. 查看 `LEARNING_EXAMPLES.md`
3. 查看代码注释（带_ANNOTATED的文件）

### 问题2：功能不工作

**解决**：
1. 查看日志（最重要！）
2. 使用Swagger测试接口
3. 检查数据库数据
4. 使用调试器

### 问题3：不知道怎么写

**解决**：
1. 参考现有代码（注册、登录）
2. 查看 `PRACTICE_EXERCISES.md`
3. 查阅Spring Boot文档

---

## 📖 推荐阅读顺序

1. **BACKEND_LEARNING_GUIDE.md** - 系统学习（必读）
2. **LEARNING_EXAMPLES.md** - 代码示例（必读）
3. **PRACTICE_EXERCISES.md** - 实践练习（必做）
4. **ARCHITECTURE.md** - 架构说明（参考）
5. **CODE_OPTIMIZATION.md** - 优化记录（参考）

---

## 🎓 学习检查

完成以下检查，确保理解：

- [ ] 能说出Controller、Service、Mapper的作用
- [ ] 能追踪一个请求的完整流程
- [ ] 能理解JWT认证的原理
- [ ] 能理解WebSocket的作用
- [ ] 能独立添加一个新接口
- [ ] 能调试和排查问题

---

## 🚀 下一步

掌握基础后，可以学习：

1. **Redis缓存**：提升性能
2. **消息队列**：异步处理
3. **微服务**：服务拆分
4. **性能优化**：提升响应速度

---

## 💬 学习交流

遇到问题：
1. 查看日志
2. 查阅文档
3. 查看代码注释
4. 参考示例代码

记住：**实践是最好的老师！**
