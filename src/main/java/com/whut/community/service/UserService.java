package com.whut.community.service;

//import com.whut.community.dao.LoginTicketMapper;

import com.whut.community.dao.UserMapper;
import com.whut.community.entity.LoginTicket;
import com.whut.community.entity.User;
import com.whut.community.util.CommunityConstant;
import com.whut.community.util.CommunityUtil;
import com.whut.community.util.MailClient;
import com.whut.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserService implements CommunityConstant {

    //注册邮件中的激活链接，包括 服务器的域名 和 项目路径
    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    //发送邮件的客户端和 Thymeleaf 引擎
    private final MailClient mailClient;

    private final TemplateEngine templateEngine;

    private final UserMapper userMapper;

    private RedisTemplate<String, Object> redisTemplate;

    //private final LoginTicketMapper loginTicketMapper;

    @Autowired(required = false)
    public UserService(MailClient mailClient,
                       TemplateEngine templateEngine,
                       UserMapper userMapper,
                       RedisTemplate<String, Object> redisTemplate
            /*LoginTicketMapper loginTicketMapper*/) {
        this.mailClient = mailClient;
        this.templateEngine = templateEngine;
        this.userMapper = userMapper;
        this.redisTemplate = redisTemplate;
        //this.loginTicketMapper = loginTicketMapper;
    }

    public User findUserById(int id) {
        //return userMapper.selectById(id);
        // 重构：查询用户时，按照以下方法：
        // 从缓存中查询该用户，为空则需要初始化缓存
        User user = getCacheUser(id);
        if (user == null) {
            user = initCacheUser(id);
        }
        return user;
    }

    //注册的核心业务处理方法，返回应当携带的各种信息
    public Map<String, Object> register(User user) {
        Map<String, Object> map = new HashMap<>();

        //对空值进行处理
        if (user == null) {
            throw new IllegalArgumentException("用户参数不能为空！");
        }
        if (StringUtils.isBlank(user.getUsername())) {
            map.put("usernameMsg", "账号不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())) {
            map.put("passwordMsg", "密码不能为空！");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())) {
            map.put("emailMsg", "邮箱不能为空！");
            return map;
        }

        //验证账号，账号已经存在则添加错误信息并返回
        User tempUser = userMapper.selectByName(user.getUsername());
        if (tempUser != null) {
            map.put("usernameMsg", "该账号已存在！");
            return map;
        }

        //验证邮箱，邮箱已经存在则添加错误信息并返回
        tempUser = userMapper.selectByEmail(user.getEmail());
        if (tempUser != null) {
            map.put("emailMsg", "该邮箱已被注册！");
            return map;
        }

        //注册用户，这里需要先设置 user 的各个字段
        //1.设置 salt 字段，这里截取随机串的前5位为salt
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));

        //2.对原密码进行加密并重新设置password
        user.setPassword(CommunityUtil.md5(user.getPassword() + user.getSalt()));

        //3.类型为 普通用户
        user.setType(0);

        //4.状态为 未激活
        user.setStatus(0);

        //5.设置激活码
        user.setActivationCode(CommunityUtil.generateUUID());

        //6.设置头像链接，这里引用了牛客网的头像链接：https://images.nowcoder.com/head/xxxt.png
        //其中xxx数字的范围是 [0,1000] 共1001个数字
        user.setHeaderUrl(String.format("https://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));

        //7.设置用户创建时间
        user.setCreateTime(new Date());

        //8.往user表中插入该用户数据，
        //插入前该 user 对象没有id，但是插入后就有id了
        userMapper.insertUser(user);

        //激活邮件，首先在域内添加数据
        Context context = new Context();
        //1.添加 email 数据
        context.setVariable("email", user.getEmail());

        //2.添加 url 数据，这个 url 将由服务器进行处理
        //示例：http://localhost:8080/community/activation/{userId}/{activationCode}
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url", url);

        //然后用模板引擎去渲染，得到发送的内容
        String content = templateEngine.process("/mail/activation.html", context);
        //发送邮件
        mailClient.sendMail(user.getEmail(), "激活账号", content);

        //当注册完成时，这个 map 中没有保存任何内容
        return map;
    }

    // 根据用户id和激活码确定激活状态(共3种)
    public int activation(int userId, String activationCode) {
        // 查询用户
        User user = userMapper.selectById(userId);

        // 用户已激活
        if (1 == user.getStatus()) {
            return ACTIVATION_REPEAT;
        }

        // 激活成功，需要修改并更新用户的状态
        if (activationCode.equals(user.getActivationCode())) {
            userMapper.updateStatus(userId, 1);
            // 重构：修改用户信息时清除该用户的缓存
            clearCacheUser(userId);
            return ACTIVATION_SUCCESS;
        } else { // 激活失败
            return ACTIVATION_FAILURE;
        }
    }

    /*
        登录逻辑：
        1.由于返回的结果可能需要携带很多不同的信息，返回值使用 Map<String, Object>；
        2.需要传入的参数是 username、password 和 expiredSeconds，其中 expiredSeconds 是凭证过期的秒数；
        3.由于用户注册时，插入到数据库中的密码 password 是经过 md5 加密过的，
        这里传入的明文密码也需要通过同样的方式加密后再和查询到的密码比对
     */
    public Map<String, Object> login(String username, String password, long expiredSeconds) {
        Map<String, Object> res = new HashMap<>();

        // 1.空值处理
        if (StringUtils.isBlank(username)) {
            res.put("usernameMsg", "用户名不能为空!");
            return res;
        }
        if (StringUtils.isBlank(password)) {
            res.put("passwordMsg", "密码不能为空!");
            return res;
        }

        // 2.验证账号是否存在
        User user = userMapper.selectByName(username);
        if (user == null) {
            res.put("usernameMsg", "该账号不存在!");
            return res;
        }

        // 3.账号存在，验证是否处于已激活状态
        if (user.getStatus() == 0) {
            res.put("usernameMsg", "该账号未激活!");
            return res;
        }

        // 4.账号存在并且已激活，验证密码是否正确
        // 首先对明文密码进行加密，需要原明文密码和salt
        password = CommunityUtil.md5(password + user.getSalt());
        // 密码 不一致 则添加响应信息并返回
        if (!user.getPassword().equals(password)) {
            res.put("passwordMsg", "密码不正确!");
            return res;
        }

        // 5.通过全部的验证步骤，生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        // 有效
        loginTicket.setStatus(0);
        // 结合传入的形参设置到期时间
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        //// 插入到数据库中，这里数据库的功能相当于 Session(后面将使用 Redis 优化)
        //loginTicketMapper.insertLoginTicket(loginTicket);

        // 重构：将登录凭证存入到 Redis 中
        String ticketKey = RedisKeyUtil.getTicketKey(loginTicket.getTicket());
        redisTemplate.opsForValue().set(ticketKey, loginTicket); // loginTicket 对象将会被 Redis 自动序列化为 JSON 字符串

        // 将 ticket 添加到结果 map 中去
        res.put("ticket", loginTicket.getTicket());
        // 验证成功时这里的 res 只存储了生成的凭证 ticket 信息
        return res;
    }

    // 登出功能，根据登录凭证修改状态即可，
    // 重构：这里只需要把登录凭证对象从 Redis 中取出，
    // 将其状态改为1之后再存到 Redis 中即可
    public void logout(String ticket) {
        //loginTicketMapper.updateStatus(ticket, 1);
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        LoginTicket loginTicket = (LoginTicket) redisTemplate.opsForValue().get(ticket);
        if (loginTicket != null) {
            loginTicket.setStatus(1); // 无效
        }
        redisTemplate.opsForValue().set(ticketKey, loginTicket);
    }

    // 重置密码钱先校验邮箱是否存在，邮箱存在才发送包含验证码的邮件
    public Map<String, Object> verifyEmail(String email) {
        Map<String, Object> res = new HashMap<>();

        // 1.空值处理
        if (StringUtils.isBlank(email)) {
            res.put("emailMsg", "邮箱不能为空！");
            return res;
        }

        // 2.检查邮箱是否存在
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            res.put("emailMsg", "邮箱尚未注册！");
            return res;
        }

        // 3.邮箱存在，生成验证码并发送邮件
        Context context = new Context();
        context.setVariable("email", email);
        // 生成验证码，这里取随机字符的前6位作为答案
        String verifyCode = CommunityUtil.generateUUID().substring(0, 6);
        context.setVariable("verifyCode", verifyCode);
        String content = templateEngine.process("/mail/forget.html", context);
        mailClient.sendMail(email, "重置密码", content);

        // 4.发送完成，将验证码和查询得到的用户都添加到结果中
        res.put("verifyCode", verifyCode);
        res.put("user", user);
        return res;
    }

    // 根据邮箱和输入的密码来重置密码
    public Map<String, Object> resetPassword(String email, String password) {
        Map<String, Object> res = new HashMap<>();

        // 1. 空值处理
        if (StringUtils.isBlank(email)) {
            res.put("emailMsg", "邮箱不能为空！");
            return res;
        }
        if (StringUtils.isBlank(password)) {
            res.put("passwordMsg", "密码不能为空！");
            return res;
        }

        // 2.检查邮箱是否存在
        User user = userMapper.selectByEmail(email);
        if (user == null) {
            res.put("emailMsg", "邮箱尚未注册！");
            return res;
        }

        // 3.重置密码，注意要和用户的salt一起加密
        password = CommunityUtil.md5(password + user.getSalt());
        user.setPassword(password);
        userMapper.updatePassword(user.getId(), password);

        // 密码修改完成，将用户添加到结果中
        res.put("user", user);
        return res;
    }

    // 根据凭证字符串查找 凭证对象 LoginTicket
    // 重构：从 Redis 中取出这个对象即可
    public LoginTicket findLoginTicket(String ticket) {
        //return loginTicketMapper.selectByTicket(ticket);
        String ticketKey = RedisKeyUtil.getTicketKey(ticket);
        return (LoginTicket) redisTemplate.opsForValue().get(ticketKey);
    }

    // 根据 userId 修改头部链接
    // 重构：用户信息被修改时，如果成功，则清除缓存
    public int updateHeader(int userId, String headerUrl) {
        int res = userMapper.updateHeader(userId, headerUrl);
        if (res > 0) {
            clearCacheUser(userId);
        }
        return res;
    }

    // 根据旧密码、新密码、确认密码修改用户的密码
    public Map<String, Object> updatePassword(User user,
                                              String oldPassword,
                                              String newPassword,
                                              String confirmPassword) {
        Map<String, Object> map = new HashMap<>();

        // 1.空值处理
        if (StringUtils.isBlank(oldPassword)) {
            map.put("oldPasswordMsg", "原密码为空！");
            return map;
        }
        if (StringUtils.isBlank(newPassword)) {
            map.put("newPasswordMsg", "新密码为空！");
            return map;
        }
        if (StringUtils.isBlank(confirmPassword)) {
            map.put("confirmPasswordMsg", "确认密码为空！");
            return map;
        }

        // 2.判断输入的旧密码是否正确(需要先加上salt并经过md5加密)
        oldPassword = CommunityUtil.md5(oldPassword + user.getSalt());
        if (!user.getPassword().equals(oldPassword)) {
            map.put("oldPasswordMsg", "输入的原密码有误！");
            return map;
        }

        // 3.直接修改用户的salt和密码即可(新旧密码是否相同以及新密码和确认密码是否相同已经在前端验证过)
        user.setSalt(CommunityUtil.generateUUID().substring(0, 5));
        // 新密码需要加密
        newPassword = CommunityUtil.md5(newPassword + user.getSalt());
        user.setPassword(newPassword);
        // 将salt和新密码写入到数据库中
        userMapper.updateSalt(user.getId(), user.getSalt());
        userMapper.updatePassword(user.getId(), user.getPassword());

        map.put("user", user);
        return map;
    }

    // 根据用户名查询用户
    public User findUserByName(String username) {
        return userMapper.selectByName(username);
    }

    // 1.优先从缓存中取值(内部方法)
    private User getCacheUser(int userId) {
        String userKey = RedisKeyUtil.getUserKey(userId);
        return (User) redisTemplate.opsForValue().get(userKey);
    }

    // 2.取不到时初始化缓存数据(内部方法)
    private User initCacheUser(int userId) {
        User user = userMapper.selectById(userId);
        String userKey = RedisKeyUtil.getUserKey(userId);
        // 将查询到的 user 对象存入到 Redis，设定有效时间为3600秒
        redisTemplate.opsForValue().set(userKey, user, 3600, TimeUnit.SECONDS);
        return user;
    }

    // 3.数据变更时清除缓存数据(内部方法)
    private void clearCacheUser(int userId) {
        String userKey = RedisKeyUtil.getUserKey(userId);
        redisTemplate.delete(userKey);
    }

    // 根据 userId 获取该用户的权限
    public Collection<? extends GrantedAuthority> getAuthorities(int userId) {
        User user = this.findUserById(userId);

        List<GrantedAuthority> list = new ArrayList<>();

        // 根据 user 的 type 字段返回对应的权限
        list.add(() -> {
            switch (user.getType()) {
                case 1:
                    return AUTHORITY_ADMIN;
                case 2:
                    return AUTHORITY_MODERATOR;
                default:
                    return AUTHORITY_USER;
            }
        });

        return list;
    }
}