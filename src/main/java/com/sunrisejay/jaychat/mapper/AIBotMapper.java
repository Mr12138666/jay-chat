package com.sunrisejay.jaychat.mapper;

import com.sunrisejay.jaychat.entity.AIBot;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * AI机器人数据访问层
 */
@Mapper
public interface AIBotMapper {

    /**
     * 根据ID查询机器人
     */
    @Select("SELECT id, user_id as userId, name, avatar, description, system_prompt as systemPrompt, " +
            "model, created_at as createdAt, updated_at as updatedAt FROM ai_bot WHERE id = #{id}")
    AIBot selectById(@Param("id") Long id);

    /**
     * 根据用户ID查询机器人列表
     */
    @Select("SELECT id, user_id as userId, name, avatar, description, system_prompt as systemPrompt, " +
            "model, created_at as createdAt, updated_at as updatedAt FROM ai_bot WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC")
    List<AIBot> selectByUserId(@Param("userId") Long userId);

    /**
     * 根据会话ID查询机器人列表
     */
    @Select("SELECT b.id, b.user_id as userId, b.name, b.avatar, b.description, b.system_prompt as systemPrompt, " +
            "b.model, b.created_at as createdAt, b.updated_at as updatedAt " +
            "FROM chat_session_bot sb " +
            "INNER JOIN ai_bot b ON sb.bot_id = b.id " +
            "WHERE sb.session_id = #{sessionId}")
    List<AIBot> selectBySessionId(@Param("sessionId") Long sessionId);

    /**
     * 插入机器人
     */
    @Insert("INSERT INTO ai_bot (user_id, name, avatar, description, system_prompt, model) " +
            "VALUES (#{userId}, #{name}, #{avatar}, #{description}, #{systemPrompt}, #{model})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AIBot bot);

    /**
     * 更新机器人
     */
    @Update("UPDATE ai_bot SET name = #{name}, avatar = #{avatar}, description = #{description}, " +
            "system_prompt = #{systemPrompt}, model = #{model} WHERE id = #{id}")
    int update(AIBot bot);

    /**
     * 删除机器人
     */
    @Delete("DELETE FROM ai_bot WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /**
     * 检查机器人是否属于指定用户
     */
    @Select("SELECT COUNT(*) FROM ai_bot WHERE id = #{id} AND user_id = #{userId}")
    int countByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    /**
     * 批量查询机器人
     */
    @Select("<script>" +
            "SELECT id, user_id as userId, name, avatar, description, system_prompt as systemPrompt, " +
            "model, created_at as createdAt, updated_at as updatedAt FROM ai_bot " +
            "WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>" +
            "#{id}" +
            "</foreach>" +
            "</script>")
    List<AIBot> selectByIds(@Param("ids") List<Long> ids);
}
