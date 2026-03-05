package com.sunrisejay.jaychat.mapper;

import com.sunrisejay.jaychat.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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
}
