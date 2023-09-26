package com.whut.community.config;

import com.whut.community.util.CommunityConstant;
import com.whut.community.util.CommunityUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import java.io.PrintWriter;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter implements CommunityConstant {

    // 1.静态资源放行
    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/resources/**");
    }

    // 2.认证相关，
    // 为了绕过 Spring Security 的认证流程，
    // 不重写 configure(AuthenticationManagerBuilder auth) 方法，
    // 转而在 LoginTicketInterceptor 类中存储某个用户的认证权限

    // 3.授权相关
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 3.1授权处理
        http.authorizeRequests()
                .antMatchers( // 对以下请求，任意身份权限都可以访问
                        "/user/setting",
                        "/user/upload",
                        "/user/updatePassword",
                        "/user/myPost/**",
                        "/user/myComment/**",
                        "/discuss/add",
                        "/comment/add/**",
                        "/letter/**",
                        "/notice/**",
                        "/like",
                        "/follow",
                        "/unFollow",
                        "/followeeList/**",
                        "/followerList/**"
                )
                .hasAnyAuthority(
                        AUTHORITY_USER,
                        AUTHORITY_ADMIN,
                        AUTHORITY_MODERATOR
                )
                .antMatchers(
                        "/discuss/top",
                        "/discuss/wonderful"
                ).hasAnyAuthority(AUTHORITY_MODERATOR) // 置顶和加精的权限只有版主才有
                .antMatchers(
                        "/discuss/delete",
                        "/data/**"
                )
                .hasAnyAuthority(AUTHORITY_ADMIN) // 删除、查看UV和DAU、访问端点 的权限只有管理员才有
                .anyRequest().permitAll() // 其他所有请求直接放行
                .and().csrf().disable();

        // 3.2权限不够时的处理
        http.exceptionHandling()
                .authenticationEntryPoint((request, response, e) -> {
                    // 3.2.1没有登录导致权限不够
                    // 判断请求是否是异步的，根据不同结果进行不同的处理
                    String xRequestedWith = request.getHeader("x-requested-with");
                    if ("XMLHttpRequest".equals(xRequestedWith)) { // 异步的，返回 JSON 字符串
                        response.setContentType("application/plain;charset=utf-8");
                        PrintWriter writer = response.getWriter();
                        writer.write(CommunityUtil.getJSONString(403, "你还没有登录哦！"));
                    } else { // 同步的，返回登录页面
                        response.sendRedirect(request.getContextPath() + "/login");
                    }
                })
                .accessDeniedHandler((request, response, e) -> {
                    // 3.2.2登录了但是权限仍然不够
                    // 判断请求是否是异步的，根据不同结果进行不同的处理
                    String xRequestedWith = request.getHeader("x-requested-with");
                    if ("XMLHttpRequest".equals(xRequestedWith)) { // 异步的，返回 JSON 字符串
                        response.setContentType("application/plain;charset=utf-8");
                        PrintWriter writer = response.getWriter();
                        writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限！"));
                    } else { // 同步的，返回错误页面
                        response.sendRedirect(request.getContextPath() + "/denied");
                    }
                });

        // 3.3 Security 底层默认会拦截 /logout 请求，进行退出处理，
        // 覆盖它默认的逻辑，才能执行我们自己的退出代码
        http.logout().logoutUrl("/securityLogout");
    }
}
