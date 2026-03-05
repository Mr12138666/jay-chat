package com.sunrisejay.jaychat.chat.mapper;

import com.sunrisejay.jaychat.chat.ChatMessage;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO chat_message (session_id, sender_id, content, content_type, sent_at) " +
            "VALUES (#{sessionId}, #{senderId}, #{content}, #{contentType}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatMessage message);

    @Select("SELECT id, session_id as sessionId, sender_id as senderId, content, " +
            "content_type as contentType, sent_at as sentAt " +
            "FROM chat_message WHERE session_id = #{sessionId} " +
            "ORDER BY sent_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<ChatMessage> selectBySessionId(@Param("sessionId") Long sessionId, 
                                        @Param("limit") Integer limit, 
                                        @Param("offset") Integer offset);

    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId}")
    int countBySessionId(Long sessionId);
}
