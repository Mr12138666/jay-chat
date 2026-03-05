# 实现"修改昵称"功能 - 完整指南

## 📋 功能需求

用户登录后可以修改自己的昵称。

**接口规范**：
- URL: `PUT /api/auth/nickname`
- 请求方式: PUT
- 需要认证: 是（需要Token）
- 请求体: `{ "nickname": "新昵称" }`
- 响应: `{ "code": 0, "message": "OK", "data": null }`

---

## 🎯 实现步骤

### 步骤1：创建请求DTO（5分钟）

**文件位置**：`src/main/java/com/sunrisejay/jaychat/dto/request/UpdateNicknameRequest.java`

**任务**：创建一个DTO类，用于接收前端传来的新昵称

**参考代码**：
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

**要点**：
- 使用`@Data`注解（Lombok自动生成getter/setter）
- 使用`@NotBlank`验证昵称不能为空
- 包路径：`com.sunrisejay.jaychat.dto.request`

**检查清单**：
- [ ] 文件创建在正确的包下
- [ ] 有`@Data`注解
- [ ] 有`@NotBlank`验证
- [ ] 字段名是`nickname`

---

### 步骤2：在Controller添加接口（10分钟）

**文件位置**：`src/main/java/com/sunrisejay/jaychat/controller/AuthController.java`

**任务**：在`AuthController`类中添加修改昵称的接口方法

**参考代码**：
```java
/**
 * 修改昵称
 */
@PutMapping("/nickname")
public ApiResponse<Void> updateNickname(
        @Valid @RequestBody UpdateNicknameRequest request,
        HttpServletRequest httpRequest) {
    // TODO: 实现逻辑
    // 1. 从请求中获取用户ID（使用jwtTokenUtil）
    // 2. 调用Service更新昵称
    // 3. 返回成功响应
}
```

**完整实现提示**：
```java
@PutMapping("/nickname")
public ApiResponse<Void> updateNickname(
        @Valid @RequestBody UpdateNicknameRequest request,
        HttpServletRequest httpRequest) {
    // 步骤1：从Token中获取用户ID
    Long userId = jwtTokenUtil.getUserIdFromRequest(httpRequest);
    if (userId == null) {
        return ApiResponse.error(401, "未登录");
    }
    
    // 步骤2：调用Service更新昵称
    authService.updateNickname(userId, request.getNickname());
    
    // 步骤3：返回成功响应
    return ApiResponse.success(null);
}
```

**要点**：
- 使用`@PutMapping`（PUT请求用于更新资源）
- 使用`@Valid`启用参数校验
- 使用`@RequestBody`接收JSON请求体
- 需要从Token中获取用户ID（因为只能修改自己的昵称）
- 如果未登录，返回401错误

**检查清单**：
- [ ] 方法添加在`AuthController`类中
- [ ] 有`@PutMapping("/nickname")`注解
- [ ] 导入了`UpdateNicknameRequest`
- [ ] 从Token获取用户ID
- [ ] 调用Service方法
- [ ] 返回统一响应格式

---

### 步骤3：在Service实现业务逻辑（15分钟）

**文件位置**：`src/main/java/com/sunrisejay/jaychat/service/AuthService.java`

**任务**：在`AuthService`类中添加`updateNickname`方法

**参考代码框架**：
```java
/**
 * 修改昵称
 * 
 * @param userId 用户ID
 * @param nickname 新昵称
 */
public void updateNickname(Long userId, String nickname) {
    // TODO: 实现业务逻辑
    // 1. 验证昵称不为空
    // 2. 验证用户存在
    // 3. 更新数据库
    // 4. 记录日志
}
```

**完整实现提示**：
```java
/**
 * 修改昵称
 */
public void updateNickname(Long userId, String nickname) {
    // 步骤1：验证昵称不为空
    if (!StringUtils.hasText(nickname)) {
        throw new BusinessException("昵称不能为空");
    }
    
    // 步骤2：验证用户存在（可选，但建议加上）
    User user = userMapper.selectById(userId);
    if (user == null) {
        logger.warn("修改昵称失败，用户不存在: userId={}", userId);
        throw new BusinessException("用户不存在");
    }
    
    // 步骤3：更新数据库
    int result = userMapper.updateNickname(userId, nickname);
    if (result <= 0) {
        logger.error("修改昵称失败，更新数据库失败: userId={}, nickname={}", userId, nickname);
        throw new BusinessException("修改昵称失败");
    }
    
    // 步骤4：记录日志
    logger.info("用户修改昵称成功: userId={}, nickname={}", userId, nickname);
}
```

**要点**：
- 使用`StringUtils.hasText()`验证昵称
- 验证用户存在（防御性编程）
- 检查更新结果（`result <= 0`表示更新失败）
- 记录日志（成功和失败都要记录）

**检查清单**：
- [ ] 方法添加在`AuthService`类中
- [ ] 有参数校验
- [ ] 有用户存在性验证
- [ ] 调用Mapper更新数据库
- [ ] 检查更新结果
- [ ] 记录日志

---

### 步骤4：在Mapper添加方法（5分钟）

**文件位置**：`src/main/java/com/sunrisejay/jaychat/mapper/UserMapper.java`

**任务**：在`UserMapper`接口中添加`updateNickname`方法

**参考代码**：
```java
/**
 * 更新昵称
 * 
 * @param id 用户ID
 * @param nickname 新昵称
 * @return 影响的行数（成功返回1，失败返回0）
 */
int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);
```

**要点**：
- 使用`@Param`给参数命名（在XML中使用）
- 返回`int`类型（影响的行数）
- 方法名和XML中的`id`要一致

**检查清单**：
- [ ] 方法添加在`UserMapper`接口中
- [ ] 有`@Param`注解
- [ ] 参数类型正确
- [ ] 有JavaDoc注释

---

### 步骤5：在XML添加SQL（10分钟）

**文件位置**：`src/main/resources/mappers/UserMapper.xml`

**任务**：在`UserMapper.xml`中添加更新昵称的SQL语句

**参考代码**：
```xml
<!-- 更新昵称 -->
<update id="updateNickname">
    UPDATE `user` 
    SET nickname = #{nickname}, 
        updated_at = NOW()
    WHERE id = #{id}
</update>
```

**要点**：
- `<update>`标签用于更新语句
- `id="updateNickname"`必须和接口方法名一致
- `#{nickname}`和`#{id}`是参数占位符
- 同时更新`updated_at`字段（记录更新时间）

**完整XML示例**（在`</mapper>`标签前添加）：
```xml
<mapper namespace="com.sunrisejay.jaychat.mapper.UserMapper">
    
    <!-- 其他SQL语句... -->
    
    <!-- 更新昵称 -->
    <update id="updateNickname">
        UPDATE `user` 
        SET nickname = #{nickname}, 
            updated_at = NOW()
        WHERE id = #{id}
    </update>
    
</mapper>
```

**检查清单**：
- [ ] SQL添加在`<mapper>`标签内
- [ ] `id`和方法名一致
- [ ] 使用`#{nickname}`和`#{id}`参数占位符
- [ ] 更新了`updated_at`字段
- [ ] SQL语法正确

---

## 🧪 测试步骤

### 测试1：使用Swagger测试（推荐）

1. **启动项目**
   ```bash
   mvn spring-boot:run
   ```

2. **访问Swagger文档**
   ```
   http://localhost:8080/doc.html
   ```

3. **获取Token**
   - 先调用登录接口：`POST /api/auth/login`
   - 复制返回的`token`值

4. **测试修改昵称**
   - 找到`PUT /api/auth/nickname`接口
   - 点击"Authorize"，输入：`Bearer <your-token>`
   - 填写请求体：
     ```json
     {
       "nickname": "新昵称"
     }
   - 点击"Execute"
   - 观察响应结果

5. **验证结果**
   - 响应应该是：`{ "code": 0, "message": "OK", "data": null }`
   - 查看数据库，确认昵称已更新
   - 查看日志，确认有成功日志

### 测试2：使用Postman测试

1. **设置请求**
   - Method: `PUT`
   - URL: `http://localhost:8080/api/auth/nickname`
   - Headers:
     ```
     Content-Type: application/json
     Authorization: Bearer <your-token>
     ```
   - Body (raw JSON):
     ```json
     {
       "nickname": "新昵称"
     }
     ```

2. **发送请求**

3. **查看响应**

### 测试3：测试边界情况

**测试用例**：

1. **正常情况**
   - 输入：`{ "nickname": "新昵称" }`
   - 预期：成功，返回`code: 0`

2. **昵称为空**
   - 输入：`{ "nickname": "" }`
   - 预期：失败，返回`code: 400, message: "昵称不能为空"`

3. **未登录**
   - 不传Token
   - 预期：失败，返回`code: 401, message: "未登录"`

4. **Token无效**
   - 传错误的Token
   - 预期：失败，返回`code: 401`

---

## 🔍 调试技巧

### 如果接口不工作，按以下步骤排查：

#### 1. 检查编译错误
```bash
mvn clean compile
```
- 如果有错误，先修复

#### 2. 检查日志
查看控制台或日志文件，寻找：
- `ERROR`级别的日志
- 异常堆栈信息
- 你添加的日志输出

#### 3. 检查数据库
```sql
-- 查看用户表
SELECT id, username, nickname, updated_at FROM `user`;

-- 确认昵称是否更新
```

#### 4. 使用调试器
- 在Controller方法第一行打断点
- 在Service方法第一行打断点
- 观察：
  - 请求参数是否正确
  - 用户ID是否正确
  - 方法是否被调用

#### 5. 检查常见问题

**问题1：404 Not Found**
- 检查URL是否正确：`/api/auth/nickname`
- 检查请求方式：必须是`PUT`

**问题2：401 Unauthorized**
- 检查Token是否正确
- 检查Token是否过期
- 检查请求头格式：`Authorization: Bearer <token>`

**问题3：400 Bad Request**
- 检查请求体格式：必须是JSON
- 检查参数校验：昵称不能为空

**问题4：500 Internal Server Error**
- 查看日志，找到具体错误
- 检查SQL语句是否正确
- 检查数据库连接

---

## 📝 代码检查清单

完成所有步骤后，检查：

### Controller层
- [ ] 方法有`@PutMapping("/nickname")`注解
- [ ] 参数有`@Valid @RequestBody UpdateNicknameRequest`
- [ ] 从Token获取用户ID
- [ ] 调用Service方法
- [ ] 返回统一响应格式

### Service层
- [ ] 方法有参数校验
- [ ] 验证用户存在
- [ ] 调用Mapper更新
- [ ] 检查更新结果
- [ ] 记录日志

### Mapper层
- [ ] 接口方法有`@Param`注解
- [ ] XML中`id`和方法名一致
- [ ] SQL语句正确
- [ ] 更新了`updated_at`字段

### DTO层
- [ ] 有`@Data`注解
- [ ] 有`@NotBlank`验证
- [ ] 字段名正确

---

## 💡 实现提示

### 提示1：参考现有代码

**参考注册功能**：
- `AuthController.register()` - 看如何接收请求
- `AuthService.register()` - 看如何处理业务逻辑
- `UserMapper.insert()` - 看如何操作数据库

**参考登录功能**：
- `AuthController.login()` - 看如何返回数据
- `AuthService.login()` - 看如何验证和记录日志

### 提示2：代码结构

```java
// Controller: 接收请求 → 调用Service → 返回响应
@PutMapping("/nickname")
public ApiResponse<Void> updateNickname(...) {
    // 1. 获取用户ID
    // 2. 调用Service
    // 3. 返回响应
}

// Service: 处理业务逻辑
public void updateNickname(Long userId, String nickname) {
    // 1. 参数校验
    // 2. 业务处理
    // 3. 调用Mapper
    // 4. 记录日志
}

// Mapper: 操作数据库
int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);
```

### 提示3：常见错误

**错误1：忘记导入类**
```java
// 需要导入
import com.sunrisejay.jaychat.dto.request.UpdateNicknameRequest;
```

**错误2：XML的id和方法名不一致**
```xml
<!-- ❌ 错误 -->
<update id="update">  <!-- 应该是 updateNickname -->

<!-- ✅ 正确 -->
<update id="updateNickname">
```

**错误3：参数名不匹配**
```xml
<!-- ❌ 错误 -->
WHERE id = #{userId}  <!-- 应该是 #{id} -->

<!-- ✅ 正确 -->
WHERE id = #{id}
```

---

## 🎯 完整代码参考

如果遇到困难，可以参考以下完整代码（但建议先自己写）：

<details>
<summary>点击查看完整代码（仅作参考）</summary>

### UpdateNicknameRequest.java
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

### AuthController.java（添加方法）
```java
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

### AuthService.java（添加方法）
```java
public void updateNickname(Long userId, String nickname) {
    if (!StringUtils.hasText(nickname)) {
        throw new BusinessException("昵称不能为空");
    }
    
    User user = userMapper.selectById(userId);
    if (user == null) {
        logger.warn("修改昵称失败，用户不存在: userId={}", userId);
        throw new BusinessException("用户不存在");
    }
    
    int result = userMapper.updateNickname(userId, nickname);
    if (result <= 0) {
        logger.error("修改昵称失败，更新数据库失败: userId={}, nickname={}", userId, nickname);
        throw new BusinessException("修改昵称失败");
    }
    
    logger.info("用户修改昵称成功: userId={}, nickname={}", userId, nickname);
}
```

### UserMapper.java（添加方法）
```java
int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);
```

### UserMapper.xml（添加SQL）
```xml
<update id="updateNickname">
    UPDATE `user` 
    SET nickname = #{nickname}, 
        updated_at = NOW()
    WHERE id = #{id}
</update>
```

</details>

---

## ✅ 完成标准

功能完成后，应该能够：

1. ✅ 通过Swagger成功调用接口
2. ✅ 昵称成功更新到数据库
3. ✅ 返回正确的响应格式
4. ✅ 参数校验正常工作（空昵称会报错）
5. ✅ 未登录时返回401错误
6. ✅ 日志中有成功记录

---

## 🎓 学习要点

通过这个练习，你应该理解：

1. **如何添加新接口**：Controller → Service → Mapper
2. **如何接收请求参数**：使用DTO和`@RequestBody`
3. **如何验证参数**：使用`@Valid`和`@NotBlank`
4. **如何获取当前用户**：从JWT Token中提取
5. **如何更新数据库**：使用MyBatis的`<update>`标签
6. **如何记录日志**：使用Logger记录操作

---

## 🆘 遇到问题？

### 问题1：编译错误
- 检查导入语句
- 检查类名是否正确
- 检查包路径是否正确

### 问题2：接口404
- 检查URL路径
- 检查请求方式（PUT）
- 检查Controller是否被扫描到

### 问题3：参数校验失败
- 检查DTO中的注解
- 检查请求体格式
- 查看日志中的错误信息

### 问题4：数据库更新失败
- 检查SQL语句
- 检查参数名是否匹配
- 检查用户ID是否正确

---

## 🚀 开始实现

按照步骤1-5逐步实现，每完成一步就测试一下，确保没问题再继续下一步。

**记住**：
- 不要一次性写完所有代码
- 每写一步就测试
- 遇到问题先看日志
- 参考现有代码（注册、登录）

**祝你实现顺利！** 🎉
