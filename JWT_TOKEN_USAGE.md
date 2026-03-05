# JWT Token 使用指南

## 🔑 Token格式要求

### 正确的请求头格式

你的请求头**必须**包含 `Authorization` 字段，格式如下：

```
Authorization: Bearer <你的token>
```

**注意**：
- `Bearer` 后面**必须有一个空格**
- 请求头名称是 `Authorization`（大小写不敏感，但建议用这个）

---

## 📝 示例

### ✅ 正确示例

**你的token**：
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIiwidWlkIjoyLCJ1c2VybmFtZSI6ImFhYSIsImlhdCI6MTc3MjY5NjgxMCwiZXhwIjoxNzczMzAxNjEwfQ.NIb-WUJNsody2von3VdfP2sy1_XAWo2QrbB9v99Qnbg
```

**正确的请求头**：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIiwidWlkIjoyLCJ1c2VybmFtZSI6ImFhYSIsImlhdCI6MTc3MjY5NjgxMCwiZXhwIjoxNzczMzAxNjEwfQ.NIb-WUJNsody2von3VdfP2sy1_XAWo2QrbB9v99Qnbg
```

### ❌ 错误示例

**错误1：缺少 Bearer 前缀**
```
Authorization: eyJhbGciOiJIUzI1NiJ9...
```
❌ 会返回 "未登录"

**错误2：Bearer 后面没有空格**
```
Authorization: BearereyJhbGciOiJIUzI1NiJ9...
```
❌ 会返回 "未登录"

**错误3：请求头名称错误**
```
Token: Bearer eyJhbGciOiJIUzI1NiJ9...
```
❌ 会返回 "未登录"

---

## 🛠️ 不同工具的使用方法

### 1. Postman

1. 打开 Postman
2. 选择请求方法（GET/POST/PUT等）
3. 点击 **Headers** 标签
4. 添加请求头：
   - **Key**: `Authorization`
   - **Value**: `Bearer eyJhbGciOiJIUzI1NiJ9...`（你的完整token）

**截图示例**：
```
Headers
┌─────────────────┬─────────────────────────────────────────────┐
│ Key             │ Value                                       │
├─────────────────┼─────────────────────────────────────────────┤
│ Authorization   │ Bearer eyJhbGciOiJIUzI1NiJ9...             │
└─────────────────┴─────────────────────────────────────────────┘
```

### 2. Swagger/Knife4j

1. 打开 Swagger 文档页面（通常是 `http://localhost:8080/doc.html`）
2. 点击右上角的 **Authorize** 按钮（🔒图标）
3. 在弹出的对话框中：
   - 输入：`Bearer eyJhbGciOiJIUzI1NiJ9...`（你的完整token）
   - 点击 **Authorize**
   - 点击 **Close**

**注意**：Swagger会自动添加 `Bearer ` 前缀，所以你只需要输入token即可。

### 3. curl 命令

```bash
curl -X GET "http://localhost:8080/api/auth/me" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIiwidWlkIjoyLCJ1c2VybmFtZSI6ImFhYSIsImlhdCI6MTc3MjY5NjgxMCwiZXhwIjoxNzczMzAxNjEwfQ.NIb-WUJNsody2von3VdfP2sy1_XAWo2QrbB9v99Qnbg"
```

### 4. JavaScript (Axios)

```javascript
import axios from 'axios';

const token = 'eyJhbGciOiJIUzI1NiJ9...';

axios.get('http://localhost:8080/api/auth/me', {
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => {
  console.log(response.data);
})
.catch(error => {
  console.error(error);
});
```

### 5. JavaScript (Fetch API)

```javascript
const token = 'eyJhbGciOiJIUzI1NiJ9...';

fetch('http://localhost:8080/api/auth/me', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${token}`
  }
})
.then(response => response.json())
.then(data => console.log(data))
.catch(error => console.error(error));
```

---

## 🔍 调试方法

### 1. 检查请求头

如果你不确定请求头是否正确，可以：

**方法1：查看后端日志**
- 启动应用后，查看控制台日志
- 如果看到 `"请求头中未找到Authorization字段"`，说明请求头名称不对
- 如果看到 `"Authorization格式不正确"`，说明格式不对

**方法2：使用浏览器开发者工具**
1. 打开浏览器开发者工具（F12）
2. 切换到 **Network** 标签
3. 发送请求
4. 点击请求，查看 **Request Headers**
5. 检查 `Authorization` 字段是否正确

### 2. 测试Token是否有效

使用 `/api/auth/me` 接口测试：

```bash
curl -X GET "http://localhost:8080/api/auth/me" \
  -H "Authorization: Bearer <你的token>"
```

**成功响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "sub": "2",
    "uid": 2,
    "username": "aaa",
    "iat": 1726968100,
    "exp": 1733301610
  }
}
```

**失败响应**：
```json
{
  "code": 401,
  "message": "未登录",
  "data": null
}
```

---

## ❓ 常见问题

### Q1: 为什么提示"未登录"？

**可能原因**：
1. ❌ 请求头名称不是 `Authorization`
2. ❌ 没有加 `Bearer ` 前缀
3. ❌ `Bearer` 后面没有空格
4. ❌ Token已过期
5. ❌ Token格式不正确

**解决方法**：
- 检查请求头格式：`Authorization: Bearer <token>`
- 检查Token是否过期（查看 `exp` 字段）
- 重新登录获取新Token

### Q2: Token在哪里获取？

**登录接口**：
```bash
POST /api/auth/login
Content-Type: application/json

{
  "username": "aaa",
  "password": "123456"
}
```

**响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 2,
    "username": "aaa"
  }
}
```

从响应中获取 `token` 字段的值。

### Q3: Token有效期多久？

默认是 **7天**（604800秒），可以在 `application.properties` 中配置：

```properties
jaychat.jwt.expire-seconds=604800
```

### Q4: Token过期了怎么办？

重新调用登录接口获取新Token。

### Q5: 如何查看Token内容？

JWT Token由三部分组成（用 `.` 分隔）：
1. **Header**（头部）
2. **Payload**（载荷，包含用户信息）
3. **Signature**（签名）

你可以访问 [jwt.io](https://jwt.io) 解码Token，查看内容。

**你的Token解码后**：
```json
{
  "sub": "2",
  "uid": 2,
  "username": "aaa",
  "iat": 1726968100,  // 签发时间
  "exp": 1733301610   // 过期时间
}
```

---

## 📋 快速检查清单

发送请求前，确认：

- [ ] 请求头名称是 `Authorization`
- [ ] 请求头值是 `Bearer <token>`（注意空格）
- [ ] Token没有过期
- [ ] Token格式正确（三段，用 `.` 分隔）

---

## 🎯 完整示例

### 使用Postman测试修改昵称接口

1. **请求方法**：`PUT`
2. **URL**：`http://localhost:8080/api/auth/nickname`
3. **Headers**：
   ```
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIyIiwidWlkIjoyLCJ1c2VybmFtZSI6ImFhYSIsImlhdCI6MTc3MjY5NjgxMCwiZXhwIjoxNzczMzAxNjEwfQ.NIb-WUJNsody2von3VdfP2sy1_XAWo2QrbB9v99Qnbg
   Content-Type: application/json
   ```
4. **Body**（raw JSON）：
   ```json
   {
     "nickname": "新昵称"
   }
   ```

**成功响应**：
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

现在你知道如何正确使用Token了！如果还有问题，检查后端日志，看看具体是什么错误。
