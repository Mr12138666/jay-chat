package com.sunrisejay.jaychat.mapper;

import com.sunrisejay.jaychat.entity.ChatSessionBot;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 会话机器人关联数据访问层
 */
@Mapper
public interface ChatSessionBotMapper {

    /**
     * 根据会话ID查询关联列表
     */
    @Select("SELECT id, session_id as sessionId, bot_id as botId, added_at as addedAt " +
            "FROM chat_session_bot WHERE session_id = #{sessionId}")
    List<ChatSessionBot> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 根据会话ID和机器人ID查询
     */
    @Select("SELECT id, session_id as sessionId, bot_id as botId, added_at as addedAt " +
            "FROM chat_session_bot WHERE session_id = #{sessionId} AND bot_id = #{botId}")
    ChatSessionBot selectBySessionIdAndBotId(@Param("sessionId") Long sessionId, @Param("botId") Long botId);

    /**
     * 插入关联
     */
    @Insert("INSERT INTO chat_session_bot (session_id, bot_id) VALUES (#{sessionId}, #{botId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatSessionBot chatSessionBot);

    /**
     * 删除关联
     */
    @Delete("DELETE FROM chat_session_bot WHERE session_id = #{sessionId} AND bot_id = #{botId}")
    int deleteBySessionIdAndBotId(@Param("sessionId") Long sessionId, @Param("botId") Long botId);

    /**
     * 根据会话ID删除所有关联
     */
    @Delete("DELETE FROM chat_session_bot WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 根据机器人ID删除所有关联
     */
    @Delete("DELETE FROM chat_session_bot WHERE bot_id = #{botId}")
    int deleteByBotId(@Param("botId") Long botId);

    /**
     * 检查关联是否存在
     */
    @Select("SELECT COUNT(*) FROM chat_session_bot WHERE session_id = #{sessionId} AND bot_id = #{botId}")
    int countBySessionIdAndBotId(@Param("sessionId") Long sessionId, @Param("botId") Long botId);
}
