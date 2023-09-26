package com.whut.community.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();

        // 1.添加Redis数据库连接工厂
        template.setConnectionFactory(factory);

        // 2.设置 key 的序列化方式
        template.setKeySerializer(RedisSerializer.string());

        // 3.设置非 hash 的 value 的序列化方式
        template.setValueSerializer(RedisSerializer.json());

        // 4.设置 hash 的 key 的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());

        // 5.设置 hash 的 value 的序列化方式
        template.setHashValueSerializer(RedisSerializer.json());

        // 6.使配置生效
        template.afterPropertiesSet();

        return template;
    }
}
