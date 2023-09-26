package com.whut.community.entity;

import lombok.*;

import java.util.Date;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class LoginTicket {
    private Integer id;
    private Integer userId;

    // 凭证，核心数据
    private String ticket;

    // 0-有效；1-无效
    private int status;
    private Date expired;

}
