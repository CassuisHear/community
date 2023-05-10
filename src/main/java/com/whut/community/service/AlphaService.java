package com.whut.community.service;

import com.whut.community.dao.AlphaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
//通过@Scope("prototype")注解控制Bean对象为多例而非单例
//此时Bean对象在使用时而不是项目启动时创建
//@Scope("prototype")
public class AlphaService {

    private final AlphaDao alphaDao;

    @Autowired
    public AlphaService(AlphaDao alphaDao) {
        System.out.println("实例化AlphaService...");
        this.alphaDao = alphaDao;
    }

    @PostConstruct
    public void init() {
        System.out.println("初始化AlphaService...");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("销毁AlphaService...");
    }

    public String serviceSelect() {
        return alphaDao.select();
    }
}
