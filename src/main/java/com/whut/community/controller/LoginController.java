package com.whut.community.controller;

import com.google.code.kaptcha.Producer;
import com.whut.community.entity.User;
import com.whut.community.service.UserService;
import com.whut.community.util.CommunityConstant;
import com.whut.community.util.CommunityUtil;
import com.whut.community.util.HostHolder;
import com.whut.community.util.RedisKeyUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private final Producer kaptchaProducer;

    private final UserService userService;

    private RedisTemplate<String, Object> redisTemplate;

    private HostHolder hostHolder;

    // 注入工程路径
    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    public LoginController(UserService userService,
                           Producer kaptchaProducer,
                           RedisTemplate<String, Object> redisTemplate,
                           HostHolder hostHolder) {
        this.userService = userService;
        this.kaptchaProducer = kaptchaProducer;
        this.redisTemplate = redisTemplate;
        this.hostHolder = hostHolder;
    }

    // 处理注册请求
    @GetMapping("/register")
    public String getRegisterPage() {
        return "/site/register";
    }

    // 处理登录请求
    @GetMapping("/login")
    public String getLoginPage() {
        return "/site/login";
    }

    // 处理"忘记密码"的请求
    @GetMapping("/forget")
    public String getForgetPage() {
        return "/site/forget";
    }

    @PostMapping("/register")
    public String register(Model model, User user) {

        Map<String, Object> map = userService.register(user);

        //注册完成则跳转到首页，
        //激活成功后(此激活链接由用户在收到的邮件中点击)再跳转到登录页面
        if (map == null || map.isEmpty()) {
            model.addAttribute("msg", "注册成功，我们已经向您的邮箱发送了一封激活邮件，请尽快激活！");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        } else {//注册失败则添加错误信息并回退到注册页面
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    // 处理激活结果，根据激活结果添加不同的信息并跳转到不同的页面
    // http://localhost:8080/community/activation/{userId}/{activationCode}
    @GetMapping("/activation/{userId}/{activationCode}")
    public String activation(Model model, @PathVariable("userId") int userId,
                             @PathVariable("activationCode") String activationCode) {
        // 获取激活结果
        int result = userService.activation(userId, activationCode);

        if (ACTIVATION_SUCCESS == result) {
            //激活成功后(此激活链接由用户在收到的邮件中点击)跳转到登录页面
            model.addAttribute("msg", "激活成功，您的账号已经可以正常使用了！");
            model.addAttribute("target", "/login");
        } else if (ACTIVATION_REPEAT == result) {
            // 重复激活或者激活失败则提示相关信息并跳转到首页
            model.addAttribute("msg", "无效操作，该账号已经激活过了！");
            model.addAttribute("target", "/index");
        } else { // 激活失败
            model.addAttribute("msg", "激活失败，您提供的激活码不正确！");
            model.addAttribute("target", "/index");
        }

        return "/site/operate-result";
    }

    // 用于响应生成验证码，由于返回的是一个图片，返回值类型是void
    @GetMapping("/kaptcha")
    public void getKaptcha(HttpServletResponse response/*, HttpSession session*/) {
        // 生成验证码和图片
        String kaptcha = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(kaptcha);

        //// 将验证码存入 session
        //session.setAttribute("kaptcha", text);

        // 重构：使用 Redis 存储验证码
        // 1.生成验证码的归属，即 owner，将其通过 Session 传给客户端
        String kaptchaOwner = CommunityUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(120); // 设置时限为120秒
        cookie.setPath(contextPath); // 全路径有效
        response.addCookie(cookie);
        // 2.将验证码文本存入 Redis，时限也设置为 120 秒
        String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(kaptchaKey, kaptcha, 120, TimeUnit.SECONDS);

        // 将图片输出给浏览器
        response.setContentType("image/png");
        try {
            ServletOutputStream os = response.getOutputStream();
            // 将图片写出
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("响应验证码失败: " + e.getMessage());
        }
    }

    // 处理登录逻辑
    @PostMapping("/login")
    public String login(String username, String password, String code, // 填入的3个基本信息
                        boolean rememberMe, // 是否点击了"记住我"
                        Model model, // 用来存放结果数据
                        /*HttpSession session, // 用来取出之前生成的验证码*/
                        HttpServletResponse response, // 用来将凭证存放到 Cookie 中，保证会话状态一致性
                        @CookieValue(value = "kaptchaOwner", required = false) String kaptchaOwner) { // 重构，用来取出验证码的归属，即 owner

        //// 1.检查验证码
        //String kaptcha = (String) session.getAttribute("kaptcha");
        //if (StringUtils.isBlank(code) || StringUtils.isBlank(kaptcha) || !code.equalsIgnoreCase(kaptcha)) {
        //    model.addAttribute("codeMsg", "验证码为空或者不正确!");
        //    return "/site/login";
        //}

        // 重构：检查验证码
        String kaptcha = null;
        // 1.1验证码的归属 未失效 时才尝试从 Redis 中取出验证码文本
        if (StringUtils.isNotBlank(kaptchaOwner)) {
            String kaptchaKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(kaptchaKey);
        }
        // 1.2判断验证码是否成功取出，以及验证码输入是否正确
        if (StringUtils.isBlank(code) || StringUtils.isBlank(kaptcha) || !code.equalsIgnoreCase(kaptcha)) {
            model.addAttribute("codeMsg", "验证码已超过120秒失效为空 或者 不正确!");
            return "/site/login";
        }

        // 2.根据是否选择了"记住我"来得到凭证的超时时间
        int expiredSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS : DEFAULT_EXPIRED_SECONDS;
        // 3.检查账号密码是否正确
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if (map.containsKey("ticket")) { // 账号密码正确时会在 map 中生成唯一的凭证 ticket
            // 将这个凭证存放到 Cookie 中，给浏览器保存
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);

            // 登录成功则跳转到首页，为了防止重复提交表单，这里使用请求重定向
            return "redirect:/index";
        } else {
            // 获取错误信息，放到 model 对象中
            // 这里使用 map 的 get() 方法之后不用使用 toString() 方法，前端页面可以自动转换
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));

            // 登录失败则返回登录页面
            return "/site/login";
        }
    }

    // 处理登出逻辑
    @GetMapping("/logout")
    public String logout(@CookieValue("ticket") String ticket) {
        userService.logout(ticket);

        // 消除当前线程存有的用户
        hostHolder.clear();

        // 在 Security 中清理掉之前的授权结果
        SecurityContextHolder.clearContext();

        // 重定向到登录请求
        return "redirect:/login";
    }

    // 处理获取验证码的请求
    @GetMapping("/forget/getCode")
    @ResponseBody
    public String getForgetCode(String email, HttpSession session) {
        Map<String, Object> map = userService.verifyEmail(email);
        if (map.containsKey("user")) { // 查询成功，保存验证码
            // 保存验证码
            session.setAttribute("verifyCode", map.get("verifyCode").toString());
            return CommunityUtil.getJSONString(0);
        } else { // 查询失败，返回错误信息
            return CommunityUtil.getJSONString(1, "邮箱查询失败", map);
        }
    }

    // 处理重置密码的请求
    @PostMapping("/forget/resetPassword")
    public String resetPassword(String email, String verifyCode, String password,
                                Model model, // 存放数据
                                HttpSession session) { // 获取之前保存的验证码
        // 核对验证码
        String code = (String) session.getAttribute("verifyCode");
        if (StringUtils.isBlank(verifyCode) || StringUtils.isBlank(code) || !code.equalsIgnoreCase(verifyCode)) {
            model.addAttribute("codeMsg", "验证码为空或者错误!");
            return "/site/forget";
        }

        // 验证码正确，重置密码
        Map<String, Object> map = userService.resetPassword(email, password);
        if (map.containsKey("user")) { // 重置密码成功，跳转到操作结果页面
            model.addAttribute("msg", "修改密码成功！请重新登录^_^");
            model.addAttribute("target", "/login");
            return "/site/operate-result";
        } else { // 重置密码失败则返回重置密码页面
            model.addAttribute("emailMsg", map.get("emailMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/forget";
        }
    }
}
