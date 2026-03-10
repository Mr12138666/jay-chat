package com.sunrisejay.jaychat.service;

import com.sunrisejay.jaychat.common.exception.BusinessException;
import com.sunrisejay.jaychat.dto.request.AIBotRequest;
import com.sunrisejay.jaychat.dto.response.AIBotResponse;
import com.sunrisejay.jaychat.entity.AIBot;
import com.sunrisejay.jaychat.entity.ChatSessionBot;
import com.sunrisejay.jaychat.mapper.AIBotMapper;
import com.sunrisejay.jaychat.mapper.ChatSessionBotMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI机器人服务
 */
@Service
public class AIBotService {

    private static final Logger logger = LoggerFactory.getLogger(AIBotService.class);

    private static final String DEFAULT_MODEL = "deepseek-chat";
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\S+)");

    private final AIBotMapper aiBotMapper;
    private final ChatSessionBotMapper chatSessionBotMapper;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIBotService(AIBotMapper aiBotMapper,
                        ChatSessionBotMapper chatSessionBotMapper,
                        ChatClient chatClient) {
        this.aiBotMapper = aiBotMapper;
        this.chatSessionBotMapper = chatSessionBotMapper;
        this.chatClient = chatClient;
    }

    /**
     * 创建机器人
     */
    @Transactional
    public AIBotResponse createBot(Long userId, AIBotRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new BusinessException("机器人名称不能为空");
        }

        AIBot bot = new AIBot();
        bot.setUserId(userId);
        bot.setName(request.getName().trim());
        bot.setAvatar(request.getAvatar());
        bot.setDescription(request.getDescription());
        bot.setSystemPrompt(request.getSystemPrompt());
        bot.setModel(request.getModel() != null ? request.getModel() : DEFAULT_MODEL);

        aiBotMapper.insert(bot);
        logger.info("创建AI机器人成功: botId={}, userId={}, name={}", bot.getId(), userId, bot.getName());

        return toResponse(bot);
    }

    /**
     * 获取用户的所有机器人
     */
    public List<AIBotResponse> getBotsByUserId(Long userId) {
        List<AIBot> bots = aiBotMapper.selectByUserId(userId);
        return bots.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取会话中的所有机器人
     */
    public List<AIBotResponse> getBotsBySessionId(Long sessionId) {
        List<AIBot> bots = aiBotMapper.selectBySessionId(sessionId);
        return bots.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取机器人
     */
    public AIBotResponse getBotById(Long botId) {
        AIBot bot = aiBotMapper.selectById(botId);
        if (bot == null) {
            throw new BusinessException("机器人不存在");
        }
        return toResponse(bot);
    }

    /**
     * 更新机器人
     */
    @Transactional
    public AIBotResponse updateBot(Long botId, Long userId, AIBotRequest request) {
        AIBot bot = aiBotMapper.selectById(botId);
        if (bot == null) {
            throw new BusinessException("机器人不存在");
        }

        // 验证权限
        if (!bot.getUserId().equals(userId)) {
            throw new BusinessException("无权限操作");
        }

        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            bot.setName(request.getName().trim());
        }
        if (request.getAvatar() != null) {
            bot.setAvatar(request.getAvatar());
        }
        if (request.getDescription() != null) {
            bot.setDescription(request.getDescription());
        }
        if (request.getSystemPrompt() != null) {
            bot.setSystemPrompt(request.getSystemPrompt());
        }
        if (request.getModel() != null) {
            bot.setModel(request.getModel());
        }

        aiBotMapper.update(bot);
        logger.info("更新AI机器人成功: botId={}, userId={}", botId, userId);

        return toResponse(bot);
    }

    /**
     * 删除机器人
     */
    @Transactional
    public void deleteBot(Long botId, Long userId) {
        AIBot bot = aiBotMapper.selectById(botId);
        if (bot == null) {
            throw new BusinessException("机器人不存在");
        }

        // 验证权限
        if (!bot.getUserId().equals(userId)) {
            throw new BusinessException("无权限操作");
        }

        // 删除会话关联
        chatSessionBotMapper.deleteByBotId(botId);

        // 删除机器人
        aiBotMapper.deleteById(botId);
        logger.info("删除AI机器人成功: botId={}, userId={}", botId, userId);
    }

    /**
     * 添加机器人到会话
     */
    @Transactional
    public void addBotToSession(Long botId, Long sessionId, Long userId) {
        AIBot bot = aiBotMapper.selectById(botId);
        if (bot == null) {
            throw new BusinessException("机器人不存在");
        }

        // 验证权限（只有机器人创建者可以添加）
        if (!bot.getUserId().equals(userId)) {
            throw new BusinessException("无权限操作");
        }

        // 检查是否已添加
        if (chatSessionBotMapper.countBySessionIdAndBotId(sessionId, botId) > 0) {
            throw new BusinessException("机器人已在群聊中");
        }

        ChatSessionBot chatSessionBot = new ChatSessionBot();
        chatSessionBot.setSessionId(sessionId);
        chatSessionBot.setBotId(botId);
        chatSessionBotMapper.insert(chatSessionBot);

        logger.info("添加机器人到会话: botId={}, sessionId={}, userId={}", botId, sessionId, userId);
    }

    /**
     * 从会话移除机器人
     */
    @Transactional
    public void removeBotFromSession(Long botId, Long sessionId, Long userId) {
        AIBot bot = aiBotMapper.selectById(botId);
        if (bot == null) {
            throw new BusinessException("机器人不存在");
        }

        // 验证权限（只有机器人创建者可以移除）
        if (!bot.getUserId().equals(userId)) {
            throw new BusinessException("无权限操作");
        }

        chatSessionBotMapper.deleteBySessionIdAndBotId(sessionId, botId);
        logger.info("从会话移除机器人: botId={}, sessionId={}, userId={}", botId, sessionId, userId);
    }

    /**
     * 机器人对话
     */
    public String chatWithBot(Long botId, String message) {
        AIBot bot = aiBotMapper.selectById(botId);
        if (bot == null) {
            throw new BusinessException("机器人不存在");
        }

        // 使用机器人的系统提示词
        String systemPrompt = bot.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            systemPrompt = "你是一个AI助手";
        }

        String response = chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .call()
                .content();

        return response;
    }

    /**
     * 机器人流式对话
     */
    public Flux<String> chatWithBotStream(Long botId, String message) {
        AIBot bot = aiBotMapper.selectById(botId);
        if (bot == null) {
            throw new BusinessException("机器人不存在");
        }

        // 使用机器人的系统提示词
        String systemPrompt = bot.getSystemPrompt();
        if (systemPrompt == null || systemPrompt.trim().isEmpty()) {
            systemPrompt = "你是一个AI助手";
        }

        return chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .stream()
                .content()
                .map(content -> {
                    // 将内容包装成 JSON 格式
                    String contentStr = content != null ? content : "";
                    Map<String, String> jsonMap = Map.of("v", contentStr);
                    try {
                        return objectMapper.writeValueAsString(jsonMap);
                    } catch (JsonProcessingException e) {
                        // 如果 JSON 序列化失败，返回默认格式
                        String escaped = contentStr
                                .replace("\\", "\\\\")
                                .replace("\"", "\\\"")
                                .replace("\n", "\\n")
                                .replace("\r", "\\r")
                                .replace("\t", "\\t");
                        return "{\"v\":\"" + escaped + "\"}";
                    }
                });
    }

    /**
     * 检测消息中是否提到了机器人，并返回被@的机器人列表
     * @param content 消息内容
     * @param sessionId 会话ID
     * @return 被@的机器人列表
     */
    public List<AIBot> extractMentionedBots(String content, Long sessionId) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        // 获取会话中的所有机器人
        List<AIBot> sessionBots = aiBotMapper.selectBySessionId(sessionId);
        if (sessionBots.isEmpty()) {
            return new ArrayList<>();
        }

        // 查找消息中@的用户名
        Matcher matcher = MENTION_PATTERN.matcher(content);
        List<String> mentionedNames = new ArrayList<>();
        while (matcher.find()) {
            mentionedNames.add(matcher.group(1));
        }

        if (mentionedNames.isEmpty()) {
            return new ArrayList<>();
        }

        // 匹配机器人
        return sessionBots.stream()
                .filter(bot -> mentionedNames.contains(bot.getName()))
                .collect(Collectors.toList());
    }

    /**
     * 从消息中提取去掉@机器人部分的内容
     * @param content 消息内容
     * @param botName 机器人名称
     * @return 去掉@机器人后的内容
     */
    public String extractMessageContent(String content, String botName) {
        if (content == null || botName == null) {
            return content;
        }

        // 移除 @机器人名称
        String mention = "@" + botName;
        String result = content.replaceFirst(mention, "").trim();

        // 移除可能的空格
        return result;
    }

    /**
     * 转换为响应DTO
     */
    private AIBotResponse toResponse(AIBot bot) {
        AIBotResponse response = new AIBotResponse();
        response.setId(bot.getId());
        response.setUserId(bot.getUserId());
        response.setName(bot.getName());
        response.setAvatar(bot.getAvatar());
        response.setDescription(bot.getDescription());
        response.setSystemPrompt(bot.getSystemPrompt());
        response.setModel(bot.getModel());
        response.setCreatedAt(bot.getCreatedAt());
        response.setUpdatedAt(bot.getUpdatedAt());
        return response;
    }
}
