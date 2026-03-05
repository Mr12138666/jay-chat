# 参数校验详解 - @NotBlank注解

## 🤔 什么是@NotBlank？

`@NotBlank` 是Java Bean Validation（JSR-303）规范中的一个**校验注解**，用于验证字符串字段。

**作用**：确保字符串字段：
- ✅ 不能为 `null`
- ✅ 不能是空字符串 `""`
- ✅ 不能只包含空格 `"   "`

**简单理解**：必须是一个有实际内容的字符串。

---

## 📝 使用示例

### 示例1：基本用法

```java
@Data
public class UpdateNicknameRequest {
    @NotBlank(message = "昵称不能为空")
    private String nickname;
}
```

**解释**：
- `@NotBlank`：验证昵称不能为空
- `message = "昵称不能为空"`：如果校验失败，返回这个错误信息

### 示例2：对比其他校验注解

```java
@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;  // 不能为null、空字符串、只包含空格
    
    @NotNull(message = "年龄不能为null")
    private Integer age;  // 只能为null，但可以是0
    
    @NotEmpty(message = "列表不能为空")
    private List<String> tags;  // 不能为null、不能是空列表，但列表里可以有null元素
    
    @Min(value = 18, message = "年龄不能小于18")
    private Integer age;
    
    @Email(message = "邮箱格式不正确")
    private String email;
}
```

---

## 🔍 @NotBlank的工作原理

### 工作流程

```
1. 前端发送请求
   POST /api/auth/nickname
   { "nickname": "" }  // 空字符串
   
2. Spring接收请求
   ↓
3. @Valid 触发校验
   ↓
4. @NotBlank 检查 nickname
   - 发现是空字符串
   - 校验失败！
   ↓
5. 抛出 MethodArgumentNotValidException
   ↓
6. GlobalExceptionHandler 捕获
   ↓
7. 返回错误响应
   {
     "code": 400,
     "message": "昵称不能为空",
     "data": null
   }
```

### 代码层面

```java
// Controller
@PutMapping("/nickname")
public ApiResponse<Void> updateNickname(
        @Valid @RequestBody UpdateNicknameRequest request) {
    // 👆 @Valid 是关键！
    // 如果没有 @Valid，@NotBlank 不会生效
    
    // 如果 nickname 为空，代码不会执行到这里
    // 会直接抛出异常，被 GlobalExceptionHandler 捕获
}
```

---

## 📊 校验注解对比表

| 注解 | 作用 | 适用类型 | 示例 |
|------|------|----------|------|
| `@NotBlank` | 不能为null、空字符串、只包含空格 | String | `@NotBlank private String name;` |
| `@NotNull` | 不能为null | 任何类型 | `@NotNull private Integer age;` |
| `@NotEmpty` | 不能为null、不能为空（集合/数组/字符串） | String, Collection, Map, Array | `@NotEmpty private List<String> list;` |
| `@NotBlank` | 字符串必须有内容 | String | `@NotBlank private String email;` |
| `@Min` | 数值最小值 | 数值类型 | `@Min(18) private Integer age;` |
| `@Max` | 数值最大值 | 数值类型 | `@Max(100) private Integer age;` |
| `@Email` | 邮箱格式 | String | `@Email private String email;` |
| `@Size` | 长度限制 | String, Collection | `@Size(min=6, max=20) private String password;` |

---

## 🎯 实际例子

### 例子1：注册功能中的使用

```java
@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    private String password;
    
    @NotBlank(message = "昵称不能为空")
    private String nickname;
}
```

**测试场景**：

**场景1：正常请求**
```json
{
  "username": "test",
  "password": "123456",
  "nickname": "测试用户"
}
```
✅ 校验通过，继续执行

**场景2：昵称为空**
```json
{
  "username": "test",
  "password": "123456",
  "nickname": ""
}
```
❌ 校验失败，返回：`{ "code": 400, "message": "昵称不能为空" }`

**场景3：昵称只有空格**
```json
{
  "username": "test",
  "password": "123456",
  "nickname": "   "
}
```
❌ 校验失败，返回：`{ "code": 400, "message": "昵称不能为空" }`

**场景4：昵称为null**
```json
{
  "username": "test",
  "password": "123456",
  "nickname": null
}
```
❌ 校验失败，返回：`{ "code": 400, "message": "昵称不能为空" }`

---

## 🔧 如何启用校验？

### 关键：@Valid注解

```java
@PostMapping("/register")
public ApiResponse<Void> register(
        @Valid @RequestBody RegisterRequest request) {
    //      ^^^^^ 这个 @Valid 是关键！
    // 没有它，@NotBlank 不会生效
}
```

**@Valid的作用**：
- 告诉Spring：请校验这个对象
- Spring会检查对象中所有带校验注解的字段
- 如果校验失败，抛出`MethodArgumentNotValidException`

---

## 🛠️ 异常处理

### GlobalExceptionHandler如何处理？

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
@ResponseStatus(HttpStatus.BAD_REQUEST)
public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
    // 提取所有校验错误
    String message = e.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)  // 获取 @NotBlank 中的 message
            .collect(Collectors.joining(", "));
    
    return ApiResponse.error(400, message);
}
```

**如果有多个字段校验失败**：
```json
{
  "username": "",
  "password": "",
  "nickname": ""
}
```

**响应**：
```json
{
  "code": 400,
  "message": "用户名不能为空, 密码不能为空, 昵称不能为空",
  "data": null
}
```

---

## 💡 常见问题

### Q1: 为什么我的@NotBlank不生效？

**可能原因**：
1. ❌ 忘记加`@Valid`注解
   ```java
   // ❌ 错误
   public ApiResponse register(@RequestBody RegisterRequest request)
   
   // ✅ 正确
   public ApiResponse register(@Valid @RequestBody RegisterRequest request)
   ```

2. ❌ 请求方式不对
   - `@RequestBody`只对POST/PUT请求有效
   - GET请求的参数在URL中，需要用`@RequestParam` + `@Valid`

3. ❌ 依赖缺失
   - 检查`pom.xml`中是否有：
     ```xml
     <dependency>
         <groupId>org.springframework.boot</groupId>
         <artifactId>spring-boot-starter-validation</artifactId>
     </dependency>
     ```

### Q2: @NotBlank 和 @NotNull 有什么区别？

```java
@NotBlank  // 只能用于String
private String name;  // 不能为null、""、"   "

@NotNull   // 可以用于任何类型
private Integer age;  // 不能为null，但可以是0
```

**区别**：
- `@NotBlank`：专门用于String，更严格（不能为空字符串）
- `@NotNull`：用于任何类型，只检查null

### Q3: 如何自定义校验规则？

```java
// 自定义校验注解
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneValidator.class)
public @interface ValidPhone {
    String message() default "手机号格式不正确";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

// 使用
@ValidPhone
private String phone;
```

---

## 🎯 在你的项目中使用

### 修改昵称功能的DTO

```java
package com.sunrisejay.jaychat.dto.request;

import lombok.Data;
import javax.validation.constraints.NotBlank;

@Data
public class UpdateNicknameRequest {
    /**
     * @NotBlank 的作用：
     * - 如果前端传 ""（空字符串），会返回错误
     * - 如果前端传 "   "（只有空格），会返回错误
     * - 如果前端传 null，会返回错误
     * - 只有传 "实际内容" 才会通过
     */
    @NotBlank(message = "昵称不能为空")
    private String nickname;
}
```

### Controller中使用

```java
@PutMapping("/nickname")
public ApiResponse<Void> updateNickname(
        @Valid @RequestBody UpdateNicknameRequest request,
        // ^^^^^ 必须加这个，@NotBlank才会生效
        HttpServletRequest httpRequest) {
    
    // 如果 nickname 校验失败，代码不会执行到这里
    // 会直接返回错误响应
    
    Long userId = jwtTokenUtil.getUserIdFromRequest(httpRequest);
    authService.updateNickname(userId, request.getNickname());
    return ApiResponse.success(null);
}
```

---

## 🧪 测试校验

### 测试1：正常情况
```bash
PUT /api/auth/nickname
{
  "nickname": "新昵称"
}
```
✅ 预期：成功，返回 `{ "code": 0 }`

### 测试2：空字符串
```bash
PUT /api/auth/nickname
{
  "nickname": ""
}
```
❌ 预期：失败，返回 `{ "code": 400, "message": "昵称不能为空" }`

### 测试3：只有空格
```bash
PUT /api/auth/nickname
{
  "nickname": "   "
}
```
❌ 预期：失败，返回 `{ "code": 400, "message": "昵称不能为空" }`

### 测试4：null值
```bash
PUT /api/auth/nickname
{
  "nickname": null
}
```
❌ 预期：失败，返回 `{ "code": 400, "message": "昵称不能为空" }`

---

## 📚 总结

### @NotBlank的核心要点

1. **作用**：验证字符串不能为空（null、空字符串、只包含空格都不行）
2. **使用位置**：DTO类的字段上
3. **生效条件**：Controller方法参数必须有`@Valid`注解
4. **错误处理**：由`GlobalExceptionHandler`统一处理
5. **错误信息**：通过`message`属性自定义

### 完整流程

```
前端请求 → Controller(@Valid) → 校验@NotBlank → 
  ↓ 通过
继续执行
  ↓ 失败
抛出异常 → GlobalExceptionHandler → 返回错误响应
```

---

## 🎓 学习要点

通过`@NotBlank`，你应该理解：

1. **参数校验的重要性**：防止无效数据进入系统
2. **@Valid的作用**：启用校验机制
3. **异常处理**：校验失败如何返回友好错误
4. **防御性编程**：在数据进入系统前就验证

---

## 💬 类比理解

**@NotBlank就像门卫**：
- 检查每个进入的人（数据）
- 如果不符合要求（为空），不让进（返回错误）
- 只有符合要求的才能通过（继续执行）

**@Valid就像门禁系统**：
- 没有@Valid，门卫（@NotBlank）不会工作
- 有了@Valid，门卫才会检查

---

现在你理解`@NotBlank`了吗？如果还有疑问，告诉我！
