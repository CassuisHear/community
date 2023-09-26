package com.whut.community.util;

import com.whut.community.entity.User;
import org.springframework.stereotype.Component;

@Component
public class HostHolder {

    // 使用 ThreadLocal 类来保证多线程下可以存储不同的用户信息
    private ThreadLocal<User> users = new ThreadLocal<>();

    public void setUser(User user) {
        users.set(user);
    }

    public User getUser() {
        return users.get();
    }

    // 用于清理本次请求的存储内容
    public void clear() {
        users.remove();
    }
}
