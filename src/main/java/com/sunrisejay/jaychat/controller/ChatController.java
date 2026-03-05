package com.sunrisejay.jaychat.controller;

import com.sunrisejay.jaychat.common.ApiResponse;
import com.sunrisejay.jaychat.common.constant.ApiConstants;
import com.sunrisejay.jaychat.common.constant.WebSocketConstants;
import com.sunrisejay.jaychat.controller.base.BaseController;
import com.sunrisejay.jaychat.dto.request.MessageRequest;
import com.sunrisejay.jaychat.dto.response.MessageResponse;
import com.sunrisejay.jaychat.dto.response.SessionMemberResponse;
import com.sunrisejay.jaychat.dto.response.SessionMemberStatsResponse;
import com.sunrisejay.jaychat.entity.ChatSession;
import com.sunrisejay.jaychat.service.ChatService;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.List;
import java.util.Set;

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

    public ChatController(ChatService chatService, SimpMessagingTemplate messagingTemplate) {
        this.chatService = chatService;
        this.messagingTemplate = messagingTemplate;
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
            @PathVariable Long sessionId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
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
            @PathVariable Long sessionId,
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
            @PathVariable Long sessionId,
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
            @PathVariable Long sessionId,
            HttpServletRequest request) {
        ApiResponse<?> authCheck = checkAuth(request);
        if (authCheck != null) {
            return (ApiResponse<Set<Long>>) authCheck;
        }
        
        Set<Long> onlineUserIds = chatService.getOnlineUserIds(sessionId);
        return ApiResponse.success(onlineUserIds);
    }
}
