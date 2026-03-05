# 后端开发实践练习

## 🎯 练习目标

通过实际编写代码，加深对后端开发的理解。

---

## 练习1：理解代码结构（必做）

### 任务
阅读以下文件，理解每一行的作用：

1. `AuthController.java` - 控制器
2. `AuthService.java` - 业务逻辑
3. `UserMapper.java` - 数据访问
4. `UserMapper.xml` - SQL映射

### 问题
1. `@RestController`和`@Controller`有什么区别？
2. 为什么用构造函数注入而不是`@Autowired`？
3. `@Valid`注解做了什么？
4. `#{username}`和`${username}`有什么区别？

### 答案提示
- 查看`BACKEND_LEARNING_GUIDE.md`
- 或者运行代码，观察效果

---

## 练习2：添加"修改昵称"功能（推荐）

### 需求
用户登录后可以修改自己的昵称。

### 步骤

#### Step 1: 创建请求DTO

在`dto/request/`包下创建`UpdateNicknameRequest.java`：

```java
package com.sunrisejay.jaychat.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class UpdateNicknameRequest {
    @NotBlank(message = "昵称不能为空")
    private String nickname;
}
```

#### Step 2: 在Controller添加接口

在`AuthController.java`中添加：

```java
/**
 * 修改昵称
 */
@PutMapping("/nickname")
public ApiResponse<Void> updateNickname(
        @Valid @RequestBody UpdateNicknameRequest request,
        HttpServletRequest httpRequest) {
    Long userId = jwtTokenUtil.getUserIdFromRequest(httpRequest);
    if (userId == null) {
        return ApiResponse.error(401, "未登录");
    }
    authService.updateNickname(userId, request.getNickname());
    return ApiResponse.success(null);
}
```

#### Step 3: 在Service实现业务逻辑

在`AuthService.java`中添加：

```java
/**
 * 修改昵称
 */
public void updateNickname(Long userId, String nickname) {
    // TODO: 实现业务逻辑
    // 1. 验证昵称不为空
    // 2. 更新数据库
    // 3. 记录日志
}
```

**提示**：
```java
if (!StringUtils.hasText(nickname)) {
    throw new BusinessException("昵称不能为空");
}
userMapper.updateNickname(userId, nickname);
logger.info("用户修改昵称: userId={}, nickname={}", userId, nickname);
```

#### Step 4: 在Mapper添加方法

在`UserMapper.java`中添加：

```java
/**
 * 更新昵称
 */
int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);
```

#### Step 5: 在XML添加SQL

在`UserMapper.xml`中添加：

```xml
<update id="updateNickname">
    UPDATE `user` 
    SET nickname = #{nickname}, updated_at = NOW()
    WHERE id = #{id}
</update>
```

#### Step 6: 测试

使用Swagger或Postman测试：
- URL: `PUT /api/auth/nickname`
- Headers: `Authorization: Bearer <your-token>`
- Body: `{ "nickname": "新昵称" }`

---

## 练习3：理解异常处理

### 任务
故意触发异常，观察处理流程。

### 步骤

1. **在Service中抛出异常**：
   ```java
   public void register(RegisterRequest req) {
       throw new BusinessException("测试异常");
   }
   ```

2. **发送注册请求**，观察响应：
   ```json
   {
     "code": 400,
     "message": "测试异常",
     "data": null
   }
   ```

3. **查看日志**，找到异常处理记录

4. **理解流程**：
   - Service抛出异常 →
   - GlobalExceptionHandler捕获 →
   - 转换为统一响应 →
   - 返回给前端

### 问题
1. 如果抛出`RuntimeException`会怎样？
2. 如果抛出`IllegalArgumentException`会怎样？
3. 异常信息在哪里被记录？

---

## 练习4：理解JWT认证

### 任务
手动解析JWT Token，理解其结构。

### 步骤

1. **登录获取Token**：
   ```bash
   POST /api/auth/login
   {
     "username": "test",
     "password": "123456"
   }
   ```

2. **复制返回的Token**

3. **访问需要认证的接口**：
   ```bash
   GET /api/chat/sessions
   Headers: Authorization: Bearer <your-token>
   ```

4. **不传Token访问**，观察响应：
   ```json
   {
     "code": 401,
     "message": "未登录"
   }
   ```

5. **传错误的Token**，观察响应

### 问题
1. Token包含哪些信息？
2. Token过期了会怎样？
3. 如何刷新Token？

---

## 练习5：理解WebSocket

### 任务
观察WebSocket消息的完整流程。

### 步骤

1. **打开浏览器开发者工具** → Network → WS

2. **登录并连接WebSocket**

3. **发送一条消息**

4. **观察**：
   - WebSocket连接建立
   - 消息发送到服务器
   - 服务器广播消息
   - 客户端接收消息

### 问题
1. WebSocket和HTTP有什么区别？
2. 消息是如何广播的？
3. 如何确保只有会话成员收到消息？

---

## 练习6：理解事务

### 任务
创建一个需要事务的方法。

### 需求
实现"创建会话并添加成员"，要求：
- 如果添加成员失败，会话也不应该创建
- 使用`@Transactional`保证原子性

### 步骤

1. **在ChatService中添加方法**：
   ```java
   @Transactional
   public ChatSession createSessionAndAddMember(Long userId, String sessionName) {
       // 1. 创建会话
       ChatSession session = new ChatSession();
       session.setName(sessionName);
       sessionMapper.insert(session);
       
       // 2. 添加成员（如果这里失败，上面的insert会回滚）
       memberMapper.insert(session.getId(), userId);
       
       return session;
   }
   ```

2. **测试**：
   - 正常情况：两个操作都成功
   - 异常情况：故意让第二步失败，观察第一步是否回滚

### 问题
1. 如果不用`@Transactional`会怎样？
2. 事务什么时候提交？什么时候回滚？

---

## 练习7：添加日志

### 任务
在关键位置添加日志，理解日志的作用。

### 步骤

1. **在Service方法中添加日志**：
   ```java
   public LoginResponse login(LoginRequest req) {
       logger.debug("收到登录请求: username={}", req.getUsername());
       
       User user = userMapper.findByUsername(req.getUsername());
       logger.debug("查询用户结果: {}", user != null ? "存在" : "不存在");
       
       if (user == null) {
           logger.warn("登录失败: 用户不存在, username={}", req.getUsername());
           throw new BusinessException("用户不存在");
       }
       
       // ... 继续
       
       logger.info("登录成功: userId={}, username={}", user.getId(), req.getUsername());
       return resp;
   }
   ```

2. **运行并观察日志输出**

3. **调整日志级别**（在application.properties）：
   ```properties
   logging.level.com.sunrisejay.jaychat=DEBUG  # 显示DEBUG日志
   logging.level.com.sunrisejay.jaychat=INFO   # 只显示INFO及以上
   ```

### 问题
1. DEBUG、INFO、WARN、ERROR有什么区别？
2. 什么时候用哪个级别？

---

## 练习8：理解依赖注入

### 任务
理解Spring如何管理对象。

### 步骤

1. **观察构造函数**：
   ```java
   public AuthService(UserMapper userMapper, JwtUtil jwtUtil) {
       this.userMapper = userMapper;  // Spring自动提供
       this.jwtUtil = jwtUtil;        // Spring自动提供
   }
   ```

2. **思考**：为什么不需要`new UserMapper()`？

3. **理解**：Spring容器管理所有`@Service`、`@Component`等注解的类

### 问题
1. Spring如何知道要注入哪个对象？
2. 如果有多个实现类怎么办？
3. 对象是什么时候创建的？

---

## 练习9：理解MyBatis映射

### 任务
理解数据库字段如何映射到Java对象。

### 步骤

1. **查看UserMapper.xml**：
   ```xml
   <resultMap id="UserResultMap" type="com.sunrisejay.jaychat.entity.User">
       <result column="created_at" property="createdAt"/>
   </resultMap>
   ```

2. **理解**：
   - `column="created_at"`：数据库字段名（下划线）
   - `property="createdAt"`：Java属性名（驼峰）

3. **测试**：查询用户，观察`createdAt`字段是否有值

### 问题
1. 如果数据库字段和Java属性名不一致怎么办？
2. 如何映射关联对象（如用户的消息列表）？

---

## 练习10：完整功能实现

### 任务
实现"获取用户信息"接口。

### 需求
- 接口：`GET /api/auth/user`
- 需要Token认证
- 返回当前登录用户的完整信息（不含密码）

### 提示

1. **Controller**：
   ```java
   @GetMapping("/user")
   public ApiResponse<User> getUser(HttpServletRequest request) {
       // TODO: 获取用户ID，查询用户信息
   }
   ```

2. **Service**：
   ```java
   public User getUserById(Long userId) {
       // TODO: 查询用户，清除密码
   }
   ```

3. **Mapper**：已有`selectById`方法

### 完整代码

<details>
<summary>点击查看答案</summary>

```java
// Controller
@GetMapping("/user")
public ApiResponse<User> getUser(HttpServletRequest request) {
    Long userId = jwtTokenUtil.getUserIdFromRequest(request);
    if (userId == null) {
        return ApiResponse.error(401, "未登录");
    }
    User user = authService.getUserById(userId);
    return ApiResponse.success(user);
}

// Service
public User getUserById(Long userId) {
    User user = userMapper.selectById(userId);
    if (user == null) {
        throw new BusinessException("用户不存在");
    }
    user.setPassword(null);  // 清除密码
    return user;
}
```

</details>

---

## 🎓 学习检查清单

完成以下检查，确保理解：

- [ ] 理解Controller、Service、Mapper的职责
- [ ] 理解依赖注入的原理
- [ ] 理解异常处理机制
- [ ] 理解JWT认证流程
- [ ] 理解WebSocket工作原理
- [ ] 理解事务的作用
- [ ] 理解MyBatis映射
- [ ] 能够独立添加新接口
- [ ] 能够调试和排查问题

---

## 💡 学习建议

1. **不要只看不练**：每个练习都要动手做
2. **遇到问题先思考**：理解为什么会这样
3. **查看日志**：日志是最好的老师
4. **使用调试器**：打断点，观察数据流转
5. **查阅文档**：官方文档最权威

---

## 📞 遇到问题？

1. **查看日志**：`logs/`目录或控制台
2. **查看Swagger**：`http://localhost:8080/doc.html`
3. **查看代码注释**：代码中有详细说明
4. **查阅文档**：`BACKEND_LEARNING_GUIDE.md`

---

## 下一步

完成这些练习后，你应该能够：
- ✅ 理解后端架构
- ✅ 独立开发新功能
- ✅ 调试和排查问题
- ✅ 优化代码性能

然后可以学习：
- Redis缓存
- 消息队列
- 微服务架构
- 性能优化
