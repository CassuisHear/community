package com.whut.community.config;

import com.google.code.kaptcha.Producer;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig {

    // 创建 Producer 对象，用于生成验证码字符串和图像
    @Bean
    public Producer kaptchaProducer() {

        // 创建 Properties 对象，用于 Producer 设置的 Config 类对象
        Properties properties = new Properties();
        // 验证码图片的长宽
        properties.setProperty("kaptcha.image.width", "100");
        properties.setProperty("kaptcha.image.height", "40");
        // 字体大小和颜色
        properties.setProperty("kaptcha.textproducer.font.size", "32");
        properties.setProperty("kaptcha.textproducer.font.color", "0,0,0");
        // 用于生成验证码的字符以及字符个数
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz");
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        // 验证码的模糊处理策略
        properties.setProperty("kaptcha.noise.impl", "com.google.code.kaptcha.impl.NoNoise");

        DefaultKaptcha kaptcha = new DefaultKaptcha();
        // 对这个 Producer 类对象进行配置
        Config config = new Config(properties);
        kaptcha.setConfig(config);
        return kaptcha;
    }
}
