package com.whut.community.controller.interceptor;

// 针对于登录凭证的拦截器

import com.whut.community.entity.LoginTicket;
import com.whut.community.entity.User;
import com.whut.community.service.UserService;
import com.whut.community.util.CookieUtil;
import com.whut.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    private final UserService userService;

    private final HostHolder hostHolder;

    @Autowired
    public LoginTicketInterceptor(UserService userService, HostHolder hostHolder) {
        this.userService = userService;
        this.hostHolder = hostHolder;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        // 查找凭证
        String ticket = CookieUtil.getValue(request, "ticket");

        // 不为空时判断凭证是否有效
        if (ticket != null) {
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            /*
                凭证有效的3个条件：
                1.不为空；
                2.状态 status 为0(有效状态)
                3.没有超时(超时时间晚于当前时间)
             */
            if (loginTicket != null
                    && loginTicket.getStatus() == 0
                    && loginTicket.getExpired().after(new Date(System.currentTimeMillis()))) {
                // 根据凭证中的 user_id 字段找到该用户
                User user = userService.findUserById(loginTicket.getUserId());

                // 在本次请求中持有用户
                hostHolder.setUser(user);

                // 构建用户认证的结果，并存入 SecurityContext，以便于 Security 进行授权
                Authentication authentication =
                        new UsernamePasswordAuthenticationToken(user, user.getPassword(), userService.getAuthorities(user.getId()));
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            }
        }

        return true;
    }

    // 在模板被渲染前向 Model 中存储这个 user 对象
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            modelAndView.addObject("loginUser", user);
        }
    }

    // 在请求结束之后销毁查询到的这个对象，
    // 在 Security 中清理掉之前的授权结果
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        if (hostHolder.getUser() != null) {
            hostHolder.clear();
        }
    }
}
