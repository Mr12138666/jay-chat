package com.sunrisejay.jaychat.mapper;

import com.sunrisejay.jaychat.entity.ChatMessage;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 聊天消息数据访问层
 */
@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO chat_message (session_id, sender_id, bot_id, content, content_type, reply_to_id, is_recalled, sent_at) " +
            "VALUES (#{sessionId}, #{senderId}, #{botId}, #{content}, #{contentType}, #{replyToId}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage message);

    @Select("SELECT id, session_id as sessionId, sender_id as senderId, bot_id as botId, content, " +
            "content_type as contentType, reply_to_id as replyToId, is_recalled as isRecalled, sent_at as sentAt " +
            "FROM chat_message WHERE session_id = #{sessionId} " +
            "ORDER BY sent_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ChatMessage> selectBySessionId(@Param("sessionId") Long sessionId,
                                        @Param("limit") Integer limit,
                                        @Param("offset") Integer offset);

    @Select("SELECT id, session_id as sessionId, sender_id as senderId, bot_id as botId, content, " +
            "content_type as contentType, reply_to_id as replyToId, is_recalled as isRecalled, sent_at as sentAt " +
            "FROM chat_message WHERE id = #{id}")
    ChatMessage selectById(@Param("id") Long id);

    @Update("UPDATE chat_message SET is_recalled = 1 WHERE id = #{id}")
    int updateRecalled(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId}")
    int countBySessionId(Long sessionId);

    @Delete("DELETE FROM chat_message WHERE session_id = #{sessionId}")
    int deleteBySessionId(Long sessionId);
}
