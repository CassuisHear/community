package com.whut.community.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

@Component
public class MailClient {

    private static final Logger logger = LoggerFactory.getLogger(MailClient.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    //将发件人的域名从配置文件中注入
    @Value("${spring.mail.username}")
    private String from;

    /**
     * 封装的发送邮件的方法
     *
     * @param to      收件人的域名
     * @param subject 邮件的主题
     * @param content 邮件的内容
     */
    public void sendMail(String to, String subject, String content) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            //通过 MimeMessageHelper 类对象设置邮件相关内容
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage);
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            //设置内容为支持 html 页面
            helper.setText(content, true);
            mailSender.send(helper.getMimeMessage());
        } catch (MessagingException e) {
            logger.error("邮件发送失败:" + e.getMessage());
        }

    }
}
