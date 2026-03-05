package com.sunrisejay.jaychat.user.mapper;

import com.sunrisejay.jaychat.user.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByUsername(@Param("username") String username);

    int insert(User user);

    User selectById(@Param("id") Long id);
}

