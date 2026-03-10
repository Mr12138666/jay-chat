package com.sunrisejay.jaychat.ai.controller;

import com.sunrisejay.jaychat.common.ApiResponse;
import com.sunrisejay.jaychat.controller.base.BaseController;
import com.sunrisejay.jaychat.dto.request.AIBotRequest;
import com.sunrisejay.jaychat.dto.response.AIBotResponse;
import com.sunrisejay.jaychat.service.AIBotService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI机器人控制器
 */
@RestController
@RequestMapping("/api/ai/bots")
public class AIBotController extends BaseController {

    private final AIBotService aiBotService;

    public AIBotController(AIBotService aiBotService) {
        this.aiBotService = aiBotService;
    }

    /**
     * 创建机器人
     */
    @PostMapping
    public ApiResponse<AIBotResponse> createBot(@RequestBody AIBotRequest request, HttpServletRequest httpRequest) {
        ApiResponse<?> authCheck = checkAuth(httpRequest);
        if (authCheck != null) {
            return (ApiResponse<AIBotResponse>) authCheck;
        }

        Long userId = getCurrentUserId(httpRequest);
        AIBotResponse response = aiBotService.createBot(userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 获取用户的机器人列表
     */
    @GetMapping
    public ApiResponse<List<AIBotResponse>> getBots(HttpServletRequest httpRequest) {
        ApiResponse<?> authCheck = checkAuth(httpRequest);
        if (authCheck != null) {
            return (ApiResponse<List<AIBotResponse>>) authCheck;
        }

        Long userId = getCurrentUserId(httpRequest);
        List<AIBotResponse> bots = aiBotService.getBotsByUserId(userId);
        return ApiResponse.success(bots);
    }

    /**
     * 获取会话中的机器人列表
     */
    @GetMapping("/session/{sessionId}")
    public ApiResponse<List<AIBotResponse>> getSessionBots(@PathVariable("sessionId") Long sessionId) {
        List<AIBotResponse> bots = aiBotService.getBotsBySessionId(sessionId);
        return ApiResponse.success(bots);
    }

    /**
     * 获取机器人详情
     */
    @GetMapping("/{botId}")
    public ApiResponse<AIBotResponse> getBot(@PathVariable("botId") Long botId) {
        AIBotResponse bot = aiBotService.getBotById(botId);
        return ApiResponse.success(bot);
    }

    /**
     * 更新机器人
     */
    @PutMapping("/{botId}")
    public ApiResponse<AIBotResponse> updateBot(
            @PathVariable("botId") Long botId,
            @RequestBody AIBotRequest request,
            HttpServletRequest httpRequest) {
        ApiResponse<?> authCheck = checkAuth(httpRequest);
        if (authCheck != null) {
            return (ApiResponse<AIBotResponse>) authCheck;
        }

        Long userId = getCurrentUserId(httpRequest);
        AIBotResponse response = aiBotService.updateBot(botId, userId, request);
        return ApiResponse.success(response);
    }

    /**
     * 删除机器人
     */
    @DeleteMapping("/{botId}")
    public ApiResponse<Void> deleteBot(@PathVariable("botId") Long botId, HttpServletRequest httpRequest) {
        ApiResponse<?> authCheck = checkAuth(httpRequest);
        if (authCheck != null) {
            return (ApiResponse<Void>) authCheck;
        }

        Long userId = getCurrentUserId(httpRequest);
        aiBotService.deleteBot(botId, userId);
        return ApiResponse.success(null);
    }

    /**
     * 添加机器人到会话
     */
    @PostMapping("/{botId}/add-to-session")
    public ApiResponse<Void> addBotToSession(
            @PathVariable("botId") Long botId,
            @RequestParam("sessionId") Long sessionId,
            HttpServletRequest httpRequest) {
        ApiResponse<?> authCheck = checkAuth(httpRequest);
        if (authCheck != null) {
            return (ApiResponse<Void>) authCheck;
        }

        Long userId = getCurrentUserId(httpRequest);
        aiBotService.addBotToSession(botId, sessionId, userId);
        return ApiResponse.success(null);
    }

    /**
     * 从会话移除机器人
     */
    @DeleteMapping("/{botId}/remove-from-session")
    public ApiResponse<Void> removeBotFromSession(
            @PathVariable("botId") Long botId,
            @RequestParam("sessionId") Long sessionId,
            HttpServletRequest httpRequest) {
        ApiResponse<?> authCheck = checkAuth(httpRequest);
        if (authCheck != null) {
            return (ApiResponse<Void>) authCheck;
        }

        Long userId = getCurrentUserId(httpRequest);
        aiBotService.removeBotFromSession(botId, sessionId, userId);
        return ApiResponse.success(null);
    }
}
