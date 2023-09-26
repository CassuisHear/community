package com.whut.community;

import org.junit.runner.RunWith;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

//为了在测试环境中模拟项目运行时的场景，
//使用@ContextConfiguration注解指定整个应用程序的配置类
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
//为了获取整个应用程序的Bean工厂，测试类需要实现ApplicationContextAware接口，
//在测试类中为成员变量的Bean工厂进行赋值
public class CommunityApplicationTests implements ApplicationContextAware {
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

    }

    //private ApplicationContext applicationContext;
    //
    //@Override
    //public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    //    this.applicationContext = applicationContext;
    //}
    //
    //@Test
    //void testApplicationContext() {
    //
    //    //打印这个Bean工厂的相关信息
    //    System.out.println(applicationContext);
    //
    //    //获取指定的Bean对象，调用其方法
    //    AlphaDao alphaDao = applicationContext.getBean(AlphaDao.class);
    //    System.out.println("AlphaDao 的 select 方法:" + alphaDao.select());
    //
    //    //通过别名获取指定的Bean对象，调用其方法
    //    alphaDao = (AlphaDao) applicationContext.getBean("alphaHibernate");
    //    System.out.println("根据别名更换实现类后 AlphaDao 的 select 方法:" + alphaDao.select());
    //}
    //
    ////@Test
    ////void testBeanManagement() {
    ////
    ////    //Spring不仅可以帮助我们管理Bean对象的创建，
    ////    //还可以帮助我们管理Bean对象的生存周期与行为
    ////    //AlphaService alphaService = applicationContext.getBean(AlphaService.class);
    ////    System.out.println(alphaService);
    ////
    ////    //两次获取的Bean对象是相同的，说明各个Bean对象在应用程序中只被实例化一次，是个单例对象
    ////    alphaService = applicationContext.getBean(AlphaService.class);
    ////    System.out.println(alphaService);
    ////}
    //
    //@Test
    //void testBeanConfig() {
    //
    //    //获取配置类中的日期转换类对象
    //    SimpleDateFormat dateFormat = applicationContext.getBean(SimpleDateFormat.class);
    //    System.out.println("当前时间为:" + dateFormat.format(new Date()));
    //}

    //@Autowired
    ////指定AlphaDao的实现类为AlphaDaoHibernateImpl，使用@Qualifier("alphaHibernate")注解
    //@Qualifier("alphaHibernate")
    //private AlphaDao alphaDao;
    //
    //@Autowired
    //private AlphaService alphaService;
    //
    //@Autowired
    //private SimpleDateFormat simpleDateFormat;
    //
    //@Test
    //void testDI() {
    //
    //    //测试依赖注入
    //    System.out.println(alphaDao);
    //    System.out.println(alphaService);
    //    System.out.println(simpleDateFormat);
    //}
}
