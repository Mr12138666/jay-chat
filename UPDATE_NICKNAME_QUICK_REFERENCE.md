# 修改昵称功能 - 快速参考卡片

## 📋 实现清单

### ✅ 步骤1：创建DTO
**文件**：`dto/request/UpdateNicknameRequest.java`
```java
@Data
public class UpdateNicknameRequest {
    @NotBlank(message = "昵称不能为空")
    private String nickname;
}
```

### ✅ 步骤2：Controller添加方法
**文件**：`controller/AuthController.java`
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

### ✅ 步骤3：Service添加方法
**文件**：`service/AuthService.java`
```java
public void updateNickname(Long userId, String nickname) {
    if (!StringUtils.hasText(nickname)) {
        throw new BusinessException("昵称不能为空");
    }
    User user = userMapper.selectById(userId);
    if (user == null) {
        throw new BusinessException("用户不存在");
    }
    int result = userMapper.updateNickname(userId, nickname);
    if (result <= 0) {
        throw new BusinessException("修改昵称失败");
    }
    logger.info("用户修改昵称成功: userId={}, nickname={}", userId, nickname);
}
```

### ✅ 步骤4：Mapper添加方法
**文件**：`mapper/UserMapper.java`
```java
int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);
```

### ✅ 步骤5：XML添加SQL
**文件**：`resources/mappers/UserMapper.xml`
```xml
<update id="updateNickname">
    UPDATE `user` 
    SET nickname = #{nickname}, updated_at = NOW()
    WHERE id = #{id}
</update>
```

---

## 🧪 测试

### Swagger测试
1. 访问：`http://localhost:8080/doc.html`
2. 登录获取Token
3. 找到 `PUT /api/auth/nickname`
4. 点击Authorize，输入：`Bearer <token>`
5. 填写请求体：`{ "nickname": "新昵称" }`
6. 执行

### 预期响应
```json
{
  "code": 0,
  "message": "OK",
  "data": null
}
```

---

## 🔍 常见错误

| 错误 | 原因 | 解决 |
|------|------|------|
| 404 Not Found | URL错误或方法名错误 | 检查`@PutMapping("/nickname")` |
| 401 Unauthorized | Token无效或未传 | 检查Authorization头 |
| 400 Bad Request | 参数校验失败 | 检查请求体格式 |
| 500 Internal Server Error | SQL错误 | 查看日志，检查SQL语句 |

---

## 📝 检查点

- [ ] DTO有`@NotBlank`验证
- [ ] Controller从Token获取用户ID
- [ ] Service有参数校验和日志
- [ ] Mapper方法有`@Param`注解
- [ ] XML的id和方法名一致
- [ ] SQL更新了`updated_at`字段

---

详细说明请查看：`IMPLEMENT_UPDATE_NICKNAME.md`
