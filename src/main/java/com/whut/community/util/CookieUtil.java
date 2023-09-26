package com.whut.community.util;

// 根据 key 从浏览器发出请求的所有 Cookie 中获取值

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

public class CookieUtil {

    public static String getValue(HttpServletRequest request, String key) {

        // 空值处理
        if (request == null || key == null) {
            throw new IllegalArgumentException("参数为空!");
        }

        // 获取请求的所有 Cookie
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            // 查询是否有一个 Cookie 的 key 和传入的 key 相等
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals(key)) {
                    return cookie.getValue();
                }
            }
        }

        // 没找到这个 key 则返回空
        return null;
    }
}
