package com.sunrisejay.jaychat.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 聊天会话成员数据访问层
 */
@Mapper
public interface ChatSessionMemberMapper {

    @Insert("INSERT INTO chat_session_member (session_id, user_id, joined_at) " +
            "VALUES (#{sessionId}, #{userId}, NOW()) " +
            "ON DUPLICATE KEY UPDATE joined_at = joined_at")
    int insert(@Param("sessionId") Long sessionId, @Param("userId") Long userId);

    @Select("SELECT user_id FROM chat_session_member WHERE session_id = #{sessionId}")
    List<Long> selectUserIdsBySessionId(Long sessionId);

    @Select("SELECT COUNT(*) > 0 FROM chat_session_member " +
            "WHERE session_id = #{sessionId} AND user_id = #{userId}")
    boolean exists(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
}
