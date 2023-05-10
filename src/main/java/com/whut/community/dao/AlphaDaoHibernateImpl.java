package com.whut.community.dao;

import org.springframework.stereotype.Repository;

//给这个实现类取别名为alphaHibernate
@Repository("alphaHibernate")
public class AlphaDaoHibernateImpl implements AlphaDao {
    @Override
    public String select() {
        return "Here is AlphaDaoHibernateImpl";
    }
}
