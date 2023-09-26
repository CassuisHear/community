package com.whut.community.controller.interceptor;

import com.whut.community.entity.User;
import com.whut.community.service.DataStaticsService;
import com.whut.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class DataStaticsInterceptor implements HandlerInterceptor {

    private DataStaticsService dataStaticsService;

    private HostHolder hostHolder;

    @Autowired
    public DataStaticsInterceptor(DataStaticsService dataStaticsService, HostHolder hostHolder) {
        this.dataStaticsService = dataStaticsService;
        this.hostHolder = hostHolder;
    }

    // 在所有请求处理之前，
    // 统计其 UV，
    // 如果当前有用户登陆了，进一步统计 DAU
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 统计 UV
        String ip = request.getRemoteAddr();
        dataStaticsService.recordUV(ip);

        // 统计 DAU
        User user = hostHolder.getUser();
        if (user != null) {
            dataStaticsService.recordDAU(user.getId());
        }

        return true;
    }
}
