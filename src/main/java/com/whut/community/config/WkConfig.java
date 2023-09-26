package com.whut.community.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;

@Configuration
public class WkConfig {

    private static final Logger logger = LoggerFactory.getLogger(WkConfig.class);

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @PostConstruct
    public void init() {
        // 创建WK的图片存储目录
        File file = new File(wkImageStorage);
        if (!file.exists()) {
            boolean mkdir = file.mkdir();
            logger.info("创建WK图片目录" + (mkdir ? "成功: " : "失败: ") + wkImageStorage);
        }
    }
}
