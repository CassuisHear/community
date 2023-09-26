package com.whut.community.entity;

import lombok.*;

import java.util.Date;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class User {

    private Integer id;
    private String username;
    private String password;
    private String salt;
    private String email;

    //0-普通用户; 1-超级管理员; 2-版主;
    private int type;
    //0-未激活; 1-已激活;
    private int status;

    private String activationCode;
    private String headerUrl;
    private Date createTime;

}
