package com.sunrisejay.jaychat.controller;

import com.sunrisejay.jaychat.common.ApiResponse;
import com.sunrisejay.jaychat.common.constant.ApiConstants;
import com.sunrisejay.jaychat.common.constant.WebSocketConstants;
import com.sunrisejay.jaychat.common.converter.UserConverter;
import com.sunrisejay.jaychat.controller.base.BaseController;
import com.sunrisejay.jaychat.dto.request.CreateGroupRequest;
import com.sunrisejay.jaychat.dto.request.InviteMemberRequest;
import com.sunrisejay.jaychat.dto.request.MessageRequest;
import com.sunrisejay.jaychat.dto.request.UpdateGroupInfoRequest;
import com.sunrisejay.jaychat.dto.response.MessageResponse;
import com.sunrisejay.jaychat.dto.response.SessionMemberResponse;
import com.sunrisejay.jaychat.dto.response.SessionMemberStatsResponse;
import com.sunrisejay.jaychat.dto.response.UserDetailResponse;
import com.sunrisejay.jaychat.entity.ChatSession;
import com.sunrisejay.jaychat.entity.User;
import com.sunrisejay.jaychat.service.ChatService;
import com.sunrisejay.jaychat.service.OssService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 聊天控制器
 * 处理聊天相关的HTTP请求
 * 注意：WebSocket消息处理已移至 WebSocketMessageHandler
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController extends BaseController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final OssService ossService;
    private final UserConverter userConverter;

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate, OssService ossService, UserConverter userConverter) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
        this.ossService = ossService;
        this.userConverter = userConverter;
    }

    /**
     * 创建或获取默认会话（公共聊天室）
     */
    @PostMapping("/sessions/default")
    public ApiResponse<ChatSession> getOrCreateDefaultSession(HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<ChatSession>) authCheck;
        }
        
        Long userId = getCurrentUserId(request);
        ChatSession session = chatService.getOrCreateDefaultSession(userId);
        return ApiResponse.success(session);
    }

    /**
     * 获取用户的会话列表
     */
    @GetMapping("/sessions")
    public ApiResponse<List<ChatSession>> getSessions(HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<List<ChatSession>>) authCheck;
        }
        
        Long userId = getCurrentUserId(request);
        return ApiResponse.success(chatService.getSessionsByUserId(userId));
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable("sessionId") Long sessionId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<List<MessageResponse>>) authCheck;
        }
        
        return ApiResponse.success(chatService.getMessages(sessionId, page, pageSize));
    }

    /**
     * HTTP接口：发送消息（用于测试，实际通过WebSocket）
     */
    @PostMapping("/messages")
    public ApiResponse<MessageResponse> sendMessage(@Valid @RequestBody MessageRequest request,
                                                     HttpServletRequest httpRequest) {
        ApiResponse<?> authCheck = checkAuth(httpRequest);
        if (authCheck != null) {
            return (ApiResponse<MessageResponse>) authCheck;
        }

        Long senderId = getCurrentUserId(httpRequest);
        MessageResponse response = chatService.sendMessage(senderId, request);
        
        // 广播消息到会话的所有成员
        String destination = WebSocketConstants.TOPIC_SESSION_PREFIX + request.getSessionId();
        messagingTemplate.convertAndSend(destination, response);

        return ApiResponse.success(response);
    }

    /**
     * 获取会话成员列表
     */
    @GetMapping("/sessions/{sessionId}/members")
    public ApiResponse<List<SessionMemberResponse>> getSessionMembers(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<List<SessionMemberResponse>>) authCheck;
        }
        
        List<SessionMemberResponse> members = chatService.getSessionMembers(sessionId);
        return ApiResponse.success(members);
    }
    
    /**
     * 获取会话成员统计
     */
    @GetMapping("/sessions/{sessionId}/members/stats")
    public ApiResponse<SessionMemberStatsResponse> getSessionMemberStats(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<SessionMemberStatsResponse>) authCheck;
        }
        
        SessionMemberStatsResponse stats = chatService.getSessionMemberStats(sessionId);
        return ApiResponse.success(stats);
    }
    
    /**
     * 获取在线用户ID列表
     */
    @GetMapping("/sessions/{sessionId}/members/online")
    public ApiResponse<Set<Long>> getOnlineUserIds(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<Set<Long>>) authCheck;
        }
        
        Set<Long> onlineUserIds = chatService.getOnlineUserIds(sessionId);
        return ApiResponse.success(onlineUserIds);
    }

    /**
     * 上传聊天图片
     */
    @PostMapping("/images/upload")
    public ApiResponse<String> uploadImage(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<String>) authCheck;
        }

        Long userId = getCurrentUserId(request);
        String imageUrl = ossService.uploadChatImage(file, userId);
        return ApiResponse.success(imageUrl);
    }

    /**
     * 获取用户列表（除当前用户外）
     */
    @GetMapping("/users")
    public ApiResponse<List<UserDetailResponse>> getUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<List<UserDetailResponse>>) authCheck;
        }

        Long userId = getCurrentUserId(request);
        List<User> users;

        if (keyword != null && !keyword.trim().isEmpty()) {
            users = chatService.searchUsers(keyword, userId);
        } else {
            users = chatService.getAllUsersExcept(userId);
        }

        // 使用 UserConverter 转换为 UserDetailResponse
        List<UserDetailResponse> userResponses = users.stream()
                .map(userConverter::toDetailResponse)
                .collect(Collectors.toList());

        return ApiResponse.success(userResponses);
    }

    /**
     * 创建或获取私人会话
     */
    @PostMapping("/sessions/private")
    public ApiResponse<ChatSession> createPrivateSession(
            @RequestParam("targetUserId") Long targetUserId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<ChatSession>) authCheck;
        }

        Long userId = getCurrentUserId(request);
        ChatSession session = chatService.getOrCreatePrivateSession(userId, targetUserId);
        return ApiResponse.success(session);
    }

    /**
     * 获取私人会话的其他成员信息
     */
    @GetMapping("/sessions/{sessionId}/other-member")
    public ApiResponse<UserDetailResponse> getPrivateSessionOtherMember(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<UserDetailResponse>) authCheck;
        }

        Long userId = getCurrentUserId(request);
        User otherUser = chatService.getPrivateSessionOtherMember(sessionId, userId);

        if (otherUser == null) {
            return ApiResponse.success(null);
        }

        // 使用 UserConverter 转换为 UserDetailResponse
        return ApiResponse.success(userConverter.toDetailResponse(otherUser));
    }

    /**
     * 删除会话（退出会话）
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<Void>) authCheck;
        }

        Long userId = getCurrentUserId(request);
        chatService.deleteSession(sessionId, userId);
        return ApiResponse.success(null);
    }

    /**
     * 撤回消息
     */
    @PostMapping("/messages/{messageId}/recall")
    public ApiResponse<MessageResponse> recallMessage(
            @PathVariable("messageId") Long messageId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<MessageResponse>) authCheck;
        }

        Long userId = getCurrentUserId(request);
        MessageResponse response = chatService.recallMessage(messageId, userId);

        // 广播撤回通知到会话的所有成员
        String destination = WebSocketConstants.TOPIC_SESSION_PREFIX + response.getSessionId();
        messagingTemplate.convertAndSend(destination, response);

        return ApiResponse.success(response);
    }

    /**
     * 创建群聊
     */
    @PostMapping("/groups")
    public ApiResponse<ChatSession> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            HttpServletRequest httpRequest) {
        ApiResponse<?> authCheck = checkAuth(httpRequest);
        if (authCheck != null) {
            return (ApiResponse<ChatSession>) authCheck;
        }

        Long userId = getCurrentUserId(httpRequest);
        ChatSession group = chatService.createGroup(userId, request.getGroupName(), request.getMemberIds());
        return ApiResponse.success(group);
    }

    /**
     * 获取群信息
     */
    @GetMapping("/groups/{sessionId}")
    public ApiResponse<ChatSession> getGroupInfo(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<ChatSession>) authCheck;
        }

        ChatSession group = chatService.getGroupInfo(sessionId);
        return ApiResponse.success(group);
    }

    /**
     * 更新群信息（群主）
     */
    @PutMapping("/groups/{sessionId}")
    public ApiResponse<Void> updateGroupInfo(
            @PathVariable("sessionId") Long sessionId,
            @RequestBody UpdateGroupInfoRequest request,
            HttpServletRequest httpRequest) {
        ApiResponse<?> authCheck = checkAuth(httpRequest);
        if (authCheck != null) {
            return (ApiResponse<Void>) authCheck;
        }

        Long userId = getCurrentUserId(httpRequest);
        chatService.updateGroupInfo(sessionId, userId, request.getGroupName(), request.getNotice());
        return ApiResponse.success(null);
    }

    /**
     * 邀请成员
     */
    @PostMapping("/groups/{sessionId}/members")
    public ApiResponse<Void> inviteMember(
            @PathVariable("sessionId") Long sessionId,
            @RequestBody InviteMemberRequest request,
            HttpServletRequest httpRequest) {
        ApiResponse<?> authCheck = checkAuth(httpRequest);
        if (authCheck != null) {
            return (ApiResponse<Void>) authCheck;
        }

        Long userId = getCurrentUserId(httpRequest);
        chatService.inviteMember(sessionId, userId, request.getUserIds());
        return ApiResponse.success(null);
    }

    /**
     * 移除成员（群主操作）
     */
    @DeleteMapping("/groups/{sessionId}/members/{userId}")
    public ApiResponse<Void> removeMember(
            @PathVariable("sessionId") Long sessionId,
            @PathVariable("userId") Long userId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<Void>) authCheck;
        }

        Long operatorId = getCurrentUserId(request);
        chatService.removeMember(sessionId, operatorId, userId);
        return ApiResponse.success(null);
    }

    /**
     * 退群
     */
    @PostMapping("/groups/{sessionId}/leave")
    public ApiResponse<Void> leaveGroup(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<Void>) authCheck;
        }

        Long userId = getCurrentUserId(request);
        chatService.leaveGroup(sessionId, userId);
        return ApiResponse.success(null);
    }

    /**
     * 解散群聊
     */
    @DeleteMapping("/groups/{sessionId}")
    public ApiResponse<Void> dissolveGroup(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<Void>) authCheck;
        }

        Long userId = getCurrentUserId(request);
        chatService.dissolveGroup(sessionId, userId);
        return ApiResponse.success(null);
    }

    /**
     * 转让群主
     */
    @PostMapping("/groups/{sessionId}/transfer")
    public ApiResponse<Void> transferOwner(
            @PathVariable("sessionId") Long sessionId,
            @RequestParam("newOwnerId") Long newOwnerId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<Void>) authCheck;
        }

        Long userId = getCurrentUserId(request);
        chatService.transferOwner(sessionId, userId, newOwnerId);
        return ApiResponse.success(null);
    }

    /**
     * 检查用户是否是群主
     */
    @GetMapping("/groups/{sessionId}/is-owner")
    public ApiResponse<Boolean> isGroupOwner(
            @PathVariable("sessionId") Long sessionId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<Boolean>) authCheck;
        }

        Long userId = getCurrentUserId(request);
        boolean isOwner = chatService.isGroupOwner(sessionId, userId);
        return ApiResponse.success(isOwner);
    }
}
