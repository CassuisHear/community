package com.whut.community;

import com.whut.community.util.MailClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MailTests {

    @Autowired
    private MailClient mailClient;

    @Autowired(required = false)
    private TemplateEngine templateEngine;

    //测试向网易邮箱发送邮件
    @Test
    public void testSendMail() {
        mailClient.sendMail("yun15907187327@163.com", "TEST", "This is a test of send a mail");
    }

    @Test
    public void testSendHTML() {

        //添加域内上下文数据
        Context context = new Context();
        context.setVariable("username", "张三");

        //使用模板引擎生成邮件内容
        String content = templateEngine.process("/mail/demo", context);
        System.out.println("content = " + content);

        //使用工具方法发送邮件
        mailClient.sendMail("yun15907187327@163.com", "TEST_HTML", content);
    }
}
