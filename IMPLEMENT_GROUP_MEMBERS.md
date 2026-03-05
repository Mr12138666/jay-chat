# 群成员列表功能 - 后端实现文档

## 📋 需求概述

实现群成员列表功能，包括：
1. 获取群成员列表（包含用户信息）
2. 获取在线用户列表
3. 获取群成员统计（总人数、在线人数）

---

## 🎯 API 接口设计

### 1. 获取群成员列表

**接口**：`GET /api/chat/sessions/{sessionId}/members`

**功能**：获取指定会话的所有成员信息

**请求参数**：
- `sessionId` (Path): 会话ID

**响应数据**：
```json
{
  "code": 0,
  "message": "success",
  "data": [
    {
      "userId": 1,
      "username": "user1",
      "nickname": "用户1",
      "avatar": null,
      "joinedAt": "2024-01-01T10:00:00"
    },
    {
      "userId": 2,
      "username": "user2",
      "nickname": "用户2",
      "avatar": null,
      "joinedAt": "2024-01-01T11:00:00"
    }
  ]
}
```

**DTO定义**：
```java
// dto/response/SessionMemberResponse.java
@Data
public class SessionMemberResponse {
    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private LocalDateTime joinedAt;
}
```

---

### 2. 获取群成员统计

**接口**：`GET /api/chat/sessions/{sessionId}/members/stats`

**功能**：获取指定会话的成员统计信息（总人数、在线人数）

**请求参数**：
- `sessionId` (Path): 会话ID

**响应数据**：
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "totalMembers": 182,
    "onlineMembers": 28
  }
}
```

**DTO定义**：
```java
// dto/response/SessionMemberStatsResponse.java
@Data
public class SessionMemberStatsResponse {
    private Integer totalMembers;    // 总人数
    private Integer onlineMembers;   // 在线人数
}
```

---

### 3. 获取在线用户ID列表（可选）

**接口**：`GET /api/chat/sessions/{sessionId}/members/online`

**功能**：获取指定会话的在线用户ID列表

**请求参数**：
- `sessionId` (Path): 会话ID

**响应数据**：
```json
{
  "code": 0,
  "message": "success",
  "data": [1, 2, 3, 5, 8, 10]  // 在线用户ID列表
}
```

---

## 📝 实现步骤

### 步骤1：创建DTO类

#### 1.1 SessionMemberResponse.java

**文件**：`src/main/java/com/sunrisejay/jaychat/dto/response/SessionMemberResponse.java`

```java
package com.sunrisejay.jaychat.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话成员响应DTO
 */
@Data
public class SessionMemberResponse {
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像URL
     */
    private String avatar;
    
    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;
}
```

#### 1.2 SessionMemberStatsResponse.java

**文件**：`src/main/java/com/sunrisejay/jaychat/dto/response/SessionMemberStatsResponse.java`

```java
package com.sunrisejay.jaychat.dto.response;

import lombok.Data;

/**
 * 会话成员统计响应DTO
 */
@Data
public class SessionMemberStatsResponse {
    /**
     * 总人数
     */
    private Integer totalMembers;
    
    /**
     * 在线人数
     */
    private Integer onlineMembers;
}
```

---

### 步骤2：扩展Mapper

#### 2.1 ChatSessionMemberMapper.java

**文件**：`src/main/java/com/sunrisejay/jaychat/mapper/ChatSessionMemberMapper.java`

添加以下方法：

```java
package com.sunrisejay.jaychat.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ChatSessionMemberMapper {
    
    // ... 现有方法 ...
    
    /**
     * 查询会话成员列表（带用户信息）
     * 返回用户ID、用户名、昵称、头像、加入时间
     */
    List<SessionMemberResponse> selectMembersWithUserInfo(@Param("sessionId") Long sessionId);
    
    /**
     * 统计会话成员总数
     */
    @Select("SELECT COUNT(*) FROM chat_session_member WHERE session_id = #{sessionId}")
    Integer countMembers(@Param("sessionId") Long sessionId);
}
```

**注意**：如果使用 `@Results` 注解，需要创建一个结果映射类，或者使用XML映射。

**推荐方式**：使用XML映射（更灵活）

#### 2.2 ChatSessionMemberMapper.xml

**文件**：`src/main/resources/mappers/ChatSessionMemberMapper.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.sunrisejay.jaychat.mapper.ChatSessionMemberMapper">

    <!-- 结果映射：会话成员信息 -->
    <resultMap id="SessionMemberInfoMap" type="com.sunrisejay.jaychat.dto.response.SessionMemberResponse">
        <result column="userId" property="userId"/>
        <result column="username" property="username"/>
        <result column="nickname" property="nickname"/>
        <result column="avatar" property="avatar"/>
        <result column="joinedAt" property="joinedAt"/>
    </resultMap>

    <!-- 查询会话成员列表（带用户信息） -->
    <select id="selectMembersWithUserInfo" resultMap="SessionMemberResponseMap">
        SELECT 
            u.id as userId,
            u.username,
            u.nickname,
            u.avatar,
            csm.joined_at as joinedAt
        FROM chat_session_member csm
        INNER JOIN user u ON csm.user_id = u.id
        WHERE csm.session_id = #{sessionId}
        ORDER BY csm.joined_at ASC
    </select>

    <!-- 统计会话成员总数 -->
    <select id="countMembers" resultType="java.lang.Integer">
        SELECT COUNT(*)
        FROM chat_session_member
        WHERE session_id = #{sessionId}
    </select>

</mapper>
```

**更新接口**：

```java
@Mapper
public interface ChatSessionMemberMapper {
    
    // ... 现有方法 ...
    
    /**
     * 查询会话成员列表（带用户信息）
     */
    List<SessionMemberResponse> selectMembersWithUserInfo(@Param("sessionId") Long sessionId);
    
    /**
     * 统计会话成员总数
     */
    Integer countMembers(@Param("sessionId") Long sessionId);
}
```

---

### 步骤3：实现在线状态管理

#### 3.1 在线状态管理器

**文件**：`src/main/java/com/sunrisejay/jaychat/service/OnlineUserService.java`

```java
package com.sunrisejay.jaychat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线用户管理服务
 * 管理每个会话的在线用户列表
 */
@Service
public class OnlineUserService {

    private static final Logger logger = LoggerFactory.getLogger(OnlineUserService.class);

    /**
     * 存储每个会话的在线用户ID集合
     * Key: sessionId, Value: Set<userId>
     */
    private final Map<Long, Set<Long>> onlineUsersBySession = new ConcurrentHashMap<>();

    /**
     * 用户上线（加入会话）
     */
    public void userOnline(Long sessionId, Long userId) {
        onlineUsersBySession.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet()).add(userId);
        logger.debug("用户上线: sessionId={}, userId={}, 当前在线人数={}", 
                sessionId, userId, getOnlineCount(sessionId));
    }

    /**
     * 用户下线（离开会话）
     */
    public void userOffline(Long sessionId, Long userId) {
        Set<Long> onlineUsers = onlineUsersBySession.get(sessionId);
        if (onlineUsers != null) {
            onlineUsers.remove(userId);
            if (onlineUsers.isEmpty()) {
                onlineUsersBySession.remove(sessionId);
            }
        }
        logger.debug("用户下线: sessionId={}, userId={}, 当前在线人数={}", 
                sessionId, userId, getOnlineCount(sessionId));
    }

    /**
     * 用户完全离线（断开WebSocket连接）
     */
    public void userDisconnect(Long userId) {
        // 从所有会话中移除该用户
        onlineUsersBySession.values().forEach(users -> users.remove(userId));
        // 清理空会话
        onlineUsersBySession.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        logger.debug("用户完全离线: userId={}", userId);
    }

    /**
     * 获取会话的在线用户ID列表
     */
    public Set<Long> getOnlineUsers(Long sessionId) {
        return new HashSet<>(onlineUsersBySession.getOrDefault(sessionId, Collections.emptySet()));
    }

    /**
     * 获取会话的在线人数
     */
    public Integer getOnlineCount(Long sessionId) {
        return onlineUsersBySession.getOrDefault(sessionId, Collections.emptySet()).size();
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(Long sessionId, Long userId) {
        return onlineUsersBySession.getOrDefault(sessionId, Collections.emptySet()).contains(userId);
    }
}
```

#### 3.2 在WebSocket连接时更新在线状态

**文件**：`src/main/java/com/sunrisejay/jaychat/config/WebSocketAuthInterceptor.java`

```java
// 在连接成功时，调用 OnlineUserService.userOnline()
// 在断开连接时，调用 OnlineUserService.userDisconnect()
```

**或者**：在 `ChatController` 的 WebSocket 消息处理中更新

---

### 步骤4：扩展Service

#### 4.1 ChatService.java

**文件**：`src/main/java/com/sunrisejay/jaychat/service/ChatService.java`

添加以下方法：

```java
@Service
public class ChatService {
    
    private final OnlineUserService onlineUserService;
    
    // ... 现有代码 ...
    
    /**
     * 获取会话成员列表
     */
    public List<SessionMemberResponse> getSessionMembers(Long sessionId) {
        return memberMapper.selectMembersWithUserInfo(sessionId);
    }
    
    /**
     * 获取会话成员统计
     */
    public SessionMemberStatsResponse getSessionMemberStats(Long sessionId) {
        Integer totalMembers = memberMapper.countMembers(sessionId);
        Integer onlineMembers = onlineUserService.getOnlineCount(sessionId);
        
        SessionMemberStatsResponse stats = new SessionMemberStatsResponse();
        stats.setTotalMembers(totalMembers);
        stats.setOnlineMembers(onlineMembers);
        return stats;
    }
    
    /**
     * 获取在线用户ID列表
     */
    public Set<Long> getOnlineUserIds(Long sessionId) {
        return onlineUserService.getOnlineUsers(sessionId);
    }
}
```

---

### 步骤5：扩展Controller

#### 5.1 ChatController.java

**文件**：`src/main/java/com/sunrisejay/jaychat/controller/ChatController.java`

添加以下接口：

```java
@RestController
@RequestMapping("/api/chat")
public class ChatController {
    
    // ... 现有代码 ...
    
    /**
     * 获取会话成员列表
     */
    @GetMapping("/sessions/{sessionId}/members")
    public ApiResponse<List<SessionMemberResponse>> getSessionMembers(
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        Long userId = jwtTokenUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        
        // 可选：检查用户是否是该会话的成员
        // if (!chatService.isMember(sessionId, userId)) {
        //     return ApiResponse.error(403, "无权访问");
        // }
        
        List<SessionMemberResponse> members = chatService.getSessionMembers(sessionId);
        return ApiResponse.success(members);
    }
    
    /**
     * 获取会话成员统计
     */
    @GetMapping("/sessions/{sessionId}/members/stats")
    public ApiResponse<SessionMemberStatsResponse> getSessionMemberStats(
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        Long userId = jwtTokenUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        
        SessionMemberStatsResponse stats = chatService.getSessionMemberStats(sessionId);
        return ApiResponse.success(stats);
    }
    
    /**
     * 获取在线用户ID列表
     */
    @GetMapping("/sessions/{sessionId}/members/online")
    public ApiResponse<Set<Long>> getOnlineUserIds(
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        Long userId = jwtTokenUtil.getUserIdFromRequest(request);
        if (userId == null) {
            return ApiResponse.error(401, "未登录");
        }
        
        Set<Long> onlineUserIds = chatService.getOnlineUserIds(sessionId);
        return ApiResponse.success(onlineUserIds);
    }
}
```

---

## 🔄 WebSocket在线状态更新

### 方案1：在WebSocket连接/断开时更新

**文件**：`src/main/java/com/sunrisejay/jaychat/config/WebSocketAuthInterceptor.java`

```java
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    private final OnlineUserService onlineUserService;
    private final ChatService chatService;
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // 连接成功，获取用户ID和会话ID
            Long userId = getUserId(accessor);
            Long sessionId = getSessionId(accessor); // 需要从连接参数中获取
            
            if (userId != null && sessionId != null) {
                onlineUserService.userOnline(sessionId, userId);
            }
        } else if (accessor != null && StompCommand.DISCONNECT.equals(accessor.getCommand())) {
            // 断开连接
            Long userId = getUserId(accessor);
            if (userId != null) {
                onlineUserService.userDisconnect(userId);
            }
        }
        
        return message;
    }
}
```

### 方案2：在订阅/取消订阅时更新（推荐）

在客户端订阅会话时，更新在线状态。这需要在WebSocket消息处理中实现。

---

## 📊 数据库查询优化

如果需要频繁查询成员列表，可以考虑添加索引：

```sql
-- 为会话成员表添加索引
ALTER TABLE `chat_session_member` 
ADD INDEX `idx_session_id` (`session_id`);
```

---

## 🧪 测试建议

### 测试1：获取成员列表
```bash
GET /api/chat/sessions/1/members
Authorization: Bearer <token>
```

### 测试2：获取成员统计
```bash
GET /api/chat/sessions/1/members/stats
Authorization: Bearer <token>
```

### 测试3：获取在线用户
```bash
GET /api/chat/sessions/1/members/online
Authorization: Bearer <token>
```

---

## 📝 实现清单

- [ ] 创建 `SessionMemberResponse` DTO
- [ ] 创建 `SessionMemberStatsResponse` DTO
- [ ] 扩展 `ChatSessionMemberMapper` 接口
- [ ] 创建 `ChatSessionMemberMapper.xml` 映射文件
- [ ] 创建 `OnlineUserService` 服务
- [ ] 扩展 `ChatService` 添加成员相关方法
- [ ] 扩展 `ChatController` 添加成员相关接口
- [ ] 在WebSocket连接/断开时更新在线状态
- [ ] 测试所有接口

---

## 💡 注意事项

1. **在线状态管理**：需要在实际的WebSocket连接/断开时更新，确保数据准确
2. **性能优化**：如果成员很多，考虑分页或缓存
3. **权限控制**：确保只有会话成员才能查看成员列表
4. **实时更新**：可以考虑通过WebSocket推送在线状态变化

---

现在你可以按照这个文档实现后端功能了！
