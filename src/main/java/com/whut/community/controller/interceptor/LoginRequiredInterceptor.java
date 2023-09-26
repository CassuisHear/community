package com.whut.community.controller.interceptor;

import com.whut.community.annotation.LoginRequired;
import com.whut.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    private HostHolder hostHolder;

    @Autowired
    public LoginRequiredInterceptor(HostHolder hostHolder) {
        this.hostHolder = hostHolder;
    }

    /*
        在控制器方法处理请求之前，
         判断 handler 是否为控制器方法，
         如果是，判断这个方法上面是否加了 @LoginRequired 注解，
         如果加了这个注解并且 此时 登录状态为空，
         则重定向到登录页面
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (handler instanceof HandlerMethod) {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class);
            if (loginRequired != null && hostHolder.getUser() == null) {
                response.sendRedirect(request.getContextPath() + "/login");
                return false;
            }
        }

        return true;
    }
}
