package com.whut.community.actuator;

import com.whut.community.util.CommunityUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Endpoint(id = "database")
public class DatabaseEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseEndpoint.class);

    private DataSource dataSource;

    @Autowired
    public DatabaseEndpoint(@Qualifier("dataSource") DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @ReadOperation // 该方法通过 get 请求访问
    public String checkConnection() {
        try (
                Connection connection = dataSource.getConnection()
        ) {
            return CommunityUtil.getJSONString(0, "获取链接成功!");
        } catch (SQLException e) {
            logger.error("获取链接失败: " + e.getMessage());
            return CommunityUtil.getJSONString(1, "获取链接失败!");
        }
    }
}
