package com.whut.community.service;

import com.whut.community.dao.UserMapper;
import com.whut.community.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserMapper userMapper;

    @Autowired
    public UserService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User findUserById(int id) {
        return userMapper.selectById(id);
    }

}
