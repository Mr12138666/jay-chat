package com.sunrisejay.jaychat.mapper;

import com.sunrisejay.jaychat.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper {

    /**
     * 获取所有用户（除指定用户外）
     */
    @Select("SELECT id, username, nickname, avatar, created_at as createdAt, updated_at as updatedAt " +
            "FROM user WHERE id != #{excludeUserId} ORDER BY nickname")
    List<User> selectAllExcept(@Param("excludeUserId") Long excludeUserId);

    /**
     * 搜索用户（按用户名或昵称）
     */
    @Select("SELECT id, username, nickname, avatar, created_at as createdAt, updated_at as updatedAt " +
            "FROM user WHERE id != #{excludeUserId} AND (username LIKE CONCAT('%', #{keyword}, '%') " +
            "OR nickname LIKE CONCAT('%', #{keyword}, '%')) ORDER BY nickname")
    List<User> searchByKeyword(@Param("keyword") String keyword, @Param("excludeUserId") Long excludeUserId);

    /**
     * 根据用户名查找用户
     */
    User findByUsername(@Param("username") String username);

    /**
     * 根据ID查找用户
     */
    User selectById(@Param("id") Long id);

    /**
     * 插入用户
     */
    int insert(User user);

    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

    /**
     * 更新头像
     */
    int updateAvatar(@Param("id") Long id, @Param("avatar") String avatar);

    /**
     * 更新上次登录时间
     */
    int updateLastLoginAt(@Param("id") Long id);

    /**
     * 更新上次发言时间
     */
    int updateLastMessageAt(@Param("id") Long id);
}
