package com.whut.community.dao;

import com.whut.community.entity.LoginTicket;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

@Mapper
@Repository("loginTicketMapper")
@Deprecated // 使用 Redis 存储登录凭证后，这个组件被废弃掉
public interface LoginTicketMapper {

    @Insert({
            "insert into login_ticket(user_id, ticket, status, expired) ",
            "values(#{userId}, #{ticket}, #{status}, #{expired}) "
    })
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertLoginTicket(LoginTicket loginTicket);

    @Select({
            "select id, user_id, ticket, status, expired ",
            "from login_ticket ",
            "where ticket = #{ticket} "
    })
    LoginTicket selectByTicket(String ticket);

    @Update({
            "<script>",
            "update login_ticket set status = #{status} ",
            "where ticket = #{ticket} ",
            "<if test=\"ticket!=null\">",
            "and 1 = 1 ",
            "</if>",
            "</script>"
    })
    int updateStatus(String ticket, int status);
}
