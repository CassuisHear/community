package com.whut.community.dao;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

@Repository
//为了避免 从容器中按照接口类型(AlphaDao)获取Bean对象时出现歧义(此时有两个AlphaDao的实现类)，
//这里需要使用@Primary注解指定优先使用哪一个实现类作为运行时的Bean对象
@Primary
public class AlphaDaoMyBatisImpl implements AlphaDao {
    @Override
    public String select() {
        return "Here is AlphaDaoMyBatisImpl";
    }
}
