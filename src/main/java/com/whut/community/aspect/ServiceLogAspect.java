package com.whut.community.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;

//@Component
//@Aspect
public class ServiceLogAspect {

    private static final Logger logger = LoggerFactory.getLogger(ServiceLogAspect.class);

    // 声明切点
    @Pointcut("execution(* com.whut.community.service.*.*(..))")
    public void pointcut() {
    }

    // 设计通知，即增强方法，
    @Before("pointcut()")
    public void beforeService(JoinPoint joinPoint) {
        // 日志记录格式：
        // 用户[192.168.10.11],在[2022-10-12],访问了[com.whut.community.service.xxx()].
        // 1.获取当前请求的所有参数
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        // 2.获取ip地址
        String ip;
        // 不是从 Controller 中调用 Service 层的方法时(比如 EventConsumer 中的 MessageService)，
        // attributes 参数为空，这时默认 ip 地址为本地 ip 地址，即 0.0.0.1
        ip = attributes != null ?
                attributes.getRequest().getRemoteAddr() :
                "0.0.0.1";

        // 3.取得当前时间
        String nowTime =
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        // 4.获取客户端调用的方法，即连接点 joinPoint，这里是"包名."+"方法名"
        String targetMethod = joinPoint.getSignature().getDeclaringTypeName()
                + "."
                + joinPoint.getSignature().getName();
        // 5.记录日志
        logger.info(String.format("用户[%s],在[%s],访问了[%s].", ip, nowTime, targetMethod));
    }

}
