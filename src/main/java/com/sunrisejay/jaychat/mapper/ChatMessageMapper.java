package com.sunrisejay.jaychat.mapper;

import com.sunrisejay.jaychat.entity.ChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天消息数据访问层
 */
@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO chat_message (session_id, sender_id, content, content_type, reply_to_id, sent_at) " +
            "VALUES (#{sessionId}, #{senderId}, #{content}, #{contentType}, #{replyToId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage message);

    @Select("SELECT id, session_id as sessionId, sender_id as senderId, content, " +
            "content_type as contentType, reply_to_id as replyToId, sent_at as sentAt " +
            "FROM chat_message WHERE session_id = #{sessionId} " +
            "ORDER BY sent_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ChatMessage> selectBySessionId(@Param("sessionId") Long sessionId,
                                        @Param("limit") Integer limit,
                                        @Param("offset") Integer offset);

    @Select("SELECT id, session_id as sessionId, sender_id as senderId, content, " +
            "content_type as contentType, reply_to_id as replyToId, sent_at as sentAt " +
            "FROM chat_message WHERE id = #{id}")
    ChatMessage selectById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId}")
    int countBySessionId(Long sessionId);
}
