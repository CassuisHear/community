package com.whut.community.controller.advice;

import com.whut.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/*
    对全局 Controller 的异常进行处理，
    只扫描标明了 @Controller 注解的 类，
    也就是 Controller 层的各个 controller 组件
 */
@ControllerAdvice(annotations = Controller.class)
public class ExceptionAdvice {

    // 用于记录异常日志
    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    // 服务端的所有异常都在这里处理，所以注解中是异常的总父类 Exception
    @ExceptionHandler({Exception.class})
    public void handleException(Exception exception, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // 1.记录异常的概况信息
        logger.error("服务器发生异常: " + exception.getMessage());

        // 2.将 异常栈 的所有信息都记录下来
        for (StackTraceElement stackTraceElement : exception.getStackTrace()) {
            logger.error(stackTraceElement.toString());
        }

        // 3.判断这次出现异常的 请求机制 是 同步的(想要页面) 还是 异步的(想要得到JSON数据)
        String requestMechanism = request.getHeader("x-requested-with");
        if ("XMLHttpRequest".equals(requestMechanism)) { // 是 异步的，返回异常对应的JSON字符串
            // 设置返回内容为 plain 普通对象，
            // 也可以是 application/json，表明返回类型是JSON字符串
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            writer.write(CommunityUtil.getJSONString(1, "服务器发生异常..."));
        } else { // 是同步的，发出 /error 请求，返回500页面
            response.sendRedirect(request.getContextPath() + "/error");
        }
    }
}
