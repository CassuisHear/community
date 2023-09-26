package com.whut.community.controller.interceptor;

import com.whut.community.entity.User;
import com.whut.community.service.MessageService;
import com.whut.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MessageInterceptor implements HandlerInterceptor {

    private HostHolder hostHolder;

    private MessageService messageService;

    @Autowired
    public MessageInterceptor(HostHolder hostHolder, MessageService messageService) {
        this.hostHolder = hostHolder;
        this.messageService = messageService;
    }

    /*
        在某次请求 controller 方法调用之后、渲染模板之前，
        将该用户的所有未读消息数量(包括 私信 和 通知)添加到 modelAndView 中即可
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user != null && modelAndView != null) {
            int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
            int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
            int allUnreadCount = letterUnreadCount + noticeUnreadCount;
            modelAndView.addObject("allUnreadCount", allUnreadCount);
        }
    }
}
