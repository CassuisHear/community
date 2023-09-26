package com.whut.community.config;

//import com.whut.community.controller.interceptor.LoginRequiredInterceptor;
import com.whut.community.controller.interceptor.DataStaticsInterceptor;
import com.whut.community.controller.interceptor.LoginTicketInterceptor;
import com.whut.community.controller.interceptor.MessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// MVC 配置类
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginTicketInterceptor loginTicketInterceptor;

    // 使用 Spring Security 代替这个拦截器
    //private final LoginRequiredInterceptor loginRequiredInterceptor;

    private final MessageInterceptor messageInterceptor;

    private final DataStaticsInterceptor dataStaticsInterceptor;

    @Autowired
    public WebMvcConfig(LoginTicketInterceptor loginTicketInterceptor,
                        //LoginRequiredInterceptor loginRequiredInterceptor,
                        MessageInterceptor messageInterceptor,
                        DataStaticsInterceptor dataStaticsInterceptor) {
        this.loginTicketInterceptor = loginTicketInterceptor;
        //this.loginRequiredInterceptor = loginRequiredInterceptor;
        this.messageInterceptor = messageInterceptor;
        this.dataStaticsInterceptor = dataStaticsInterceptor;
    }

    // 添加拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(messageInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg"); // 静态资源排除

        registry.addInterceptor(loginTicketInterceptor) // 默认全部拦截
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg"); // 静态资源排除

        //registry.addInterceptor(loginRequiredInterceptor)
        //        .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg"); // 静态资源排除

        registry.addInterceptor(dataStaticsInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png", "/**/*.jpg", "/**/*.jpeg"); // 静态资源排除
    }
}
