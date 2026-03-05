package com.sunrisejay.jaychat.mapper;

import com.sunrisejay.jaychat.entity.ChatSession;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天会话数据访问层
 */
@Mapper
public interface ChatSessionMapper {

    @Insert("INSERT INTO chat_session (type, name, owner_id, created_at) " +
            "VALUES (#{type}, #{name}, #{ownerId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ChatSession session);

    @Select("SELECT id, type, name, owner_id as ownerId, created_at as createdAt " +
            "FROM chat_session WHERE id = #{id}")
    ChatSession selectById(Long id);

    @Select("SELECT cs.id, cs.type, cs.name, cs.owner_id as ownerId, cs.created_at as createdAt " +
            "FROM chat_session cs " +
            "INNER JOIN chat_session_member csm ON cs.id = csm.session_id " +
            "WHERE csm.user_id = #{userId} ORDER BY cs.created_at DESC")
    List<ChatSession> selectByUserId(Long userId);

    @Select("SELECT id, type, name, owner_id as ownerId, created_at as createdAt " +
            "FROM chat_session " +
            "WHERE type = 'group' AND name = #{name} " +
            "ORDER BY created_at ASC " +
            "LIMIT 1")
    ChatSession selectByName(@Param("name") String name);
    
    @Select("SELECT id, type, name, owner_id as ownerId, created_at as createdAt " +
            "FROM chat_session " +
            "WHERE type = 'group' AND name = #{name} " +
            "ORDER BY created_at ASC")
    List<ChatSession> selectAllByName(@Param("name") String name);
}
