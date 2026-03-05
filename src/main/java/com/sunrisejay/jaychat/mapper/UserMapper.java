package com.sunrisejay.jaychat.mapper;

import com.sunrisejay.jaychat.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import javax.validation.constraints.NotBlank;

/**
 * 用户数据访问层
 */
@Mapper
public interface UserMapper {

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
