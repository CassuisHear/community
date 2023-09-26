package com.whut.community.controller;

import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.whut.community.annotation.LoginRequired;
import com.whut.community.entity.Comment;
import com.whut.community.entity.DiscussPost;
import com.whut.community.entity.Page;
import com.whut.community.entity.User;
import com.whut.community.service.*;
import com.whut.community.util.CommunityConstant;
import com.whut.community.util.CommunityUtil;
import com.whut.community.util.CookieUtil;
import com.whut.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/user")
public class UserController implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.header.name}")
    private String headerBucketName;

    @Value("${qiniu.bucket.header.url}")
    private String headerBucketUrl;

    private final UserService userService;

    private final HostHolder hostHolder;

    private LikeService likeService;

    private FollowService followService;

    private DiscussPostService discussPostService;

    private CommentService commentService;

    @Autowired
    public UserController(UserService userService,
                          HostHolder hostHolder,
                          LikeService likeService,
                          FollowService followService,
                          DiscussPostService discussPostService,
                          CommentService commentService) {
        this.userService = userService;
        this.hostHolder = hostHolder;
        this.likeService = likeService;
        this.followService = followService;
        this.discussPostService = discussPostService;
        this.commentService = commentService;
    }

    // 返回设置页面(在这个页面上传头像)，
    // 在这里将七牛云服务器的凭证添加到模板中，
    // 当表单提交时，顺带将凭证提交给七牛云服务器，文件才能上传成功
    @LoginRequired
    @GetMapping("/setting")
    public String getSettingPage(Model model) {
        // 1.上传头像文件的名称
        String fileName = CommunityUtil.generateUUID();

        // 2.设置响应信息(希望七牛云服务器返回的结果)
        StringMap policy = new StringMap();
        policy.put("returnBody", CommunityUtil.getJSONString(0));

        // 3.生成上传凭证
        Auth auth = Auth.create(accessKey, secretKey);
        // 进入 headerBucketName 空间，
        // 上传文件名为 fileName，凭证过期时间为 3600 秒，
        // 期望返回结果是 policy
        String uploadToken = auth.uploadToken(headerBucketName, fileName, 3600L, policy);

        // 4.向模板添加数据
        model.addAttribute("uploadToken", uploadToken);
        model.addAttribute("fileName", fileName);

        return "/site/setting";
    }

    // 更新头像访问路径
    @PostMapping("/header/url")
    @ResponseBody
    public String updateHeaderUrl(String fileName) {
        if (StringUtils.isBlank(fileName)) {
            return CommunityUtil.getJSONString(1, "文件名不能为空！");
        }

        // 构造 headerUrl 并更新用户的 headerUrl 属性
        String headerUrl = headerBucketUrl + "/" + fileName;
        userService.updateHeader(hostHolder.getUser().getId(), headerUrl);

        return CommunityUtil.getJSONString(0);
    }

    // 上传到七牛云服务器，此方法废弃
    // 上传头像图片
    @LoginRequired
    @PostMapping("/upload")
    public String uploadHeader(MultipartFile headerImage, Model model) {
        // 1.空值处理
        if (headerImage == null) {
            model.addAttribute("error", "您还没有选择图片！");
            return "/site/setting";
        }

        // 2.为了避免用户传入的图片名重复，需要随机生成图片名字
        // 2.1截取文件后缀
        String filename = headerImage.getOriginalFilename();
        String suffix = null;
        if (!StringUtils.isBlank(filename)) {
            suffix = filename.substring(filename.lastIndexOf("."));
        }
        if (StringUtils.isBlank(suffix)) { // 文件格式不正确
            model.addAttribute("error", "文件格式不正确！");
            return "/site/setting";
        }
        // 2.1生成随机文件名
        filename = CommunityUtil.generateUUID() + suffix;

        // 3.创建文件目录，将图片写入到目录中
        File dest = new File(uploadPath + "/" + filename);
        try {
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("文件上传失败 : " + e.getMessage());
            throw new RuntimeException("文件上传失败，服务器发生异常！", e);
        }

        // 4.更新当前用户的头像路径(web 端访问路径)
        // http://localhost:8080//community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/" + filename; // 拼接访问路径
        userService.updateHeader(user.getId(), headerUrl);

        // 重定向以免发送多次请求
        return "redirect:/index";
    }

    // 从七牛云服务器中获取图片，此方法废弃
    // 获取头像图片
    @GetMapping("/header/{fileName}")
    public void getHeaderImage(@PathVariable("fileName") String fileName, HttpServletResponse response) {
        // 1.获取文件后缀
        String suffix = fileName.substring(fileName.lastIndexOf("."));

        // 2.构建该文件在服务器中的位置
        String filePath = uploadPath + "/" + fileName;

        // 3.获取输出流，构建输入流并写出文件
        response.setContentType("image/" + suffix);
        try (ServletOutputStream os = response.getOutputStream();
             FileInputStream fis = new FileInputStream(filePath)) {
            // 响应图片
            byte[] buffer = new byte[2048];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        } catch (IOException e) {
            logger.error("读取头像失败 : " + e.getMessage());
        }
    }

    // 修改密码
    @LoginRequired
    @PostMapping("/updatePassword")
    public String updatePassword(String oldPassword, String newPassword, String confirmPassword,
                                 Model model,
                                 HttpServletRequest request) { // 用于将本次请求对应的 ticket 改为失效状态

        // 从线程中获取该用户
        User user = hostHolder.getUser();

        // 获取结果
        Map<String, Object> map = userService.updatePassword(user, oldPassword, newPassword, confirmPassword);

        if (map.containsKey("user")) { // 处理成功

            // 修改密码后，服务端这边所有和这个用户的凭证应该修改为无效状态
            String ticket = CookieUtil.getValue(request, "ticket");
            userService.logout(ticket);

            // 并且服务端中该线程持有的这个用户也应该移除
            hostHolder.clear();

            // 添加操作结果的信息
            model.addAttribute("msg", "密码修改成功！^_^");
            model.addAttribute("target", "/login");

            // 跳转到 操作成功 界面
            return "/site/operate-result";
        } else { // 处理失败
            model.addAttribute("oldPasswordMsg", map.get("oldPasswordMsg"));
            model.addAttribute("newPasswordMsg", map.get("newPasswordMsg"));
            model.addAttribute("confirmPasswordMsg", map.get("confirmPasswordMsg"));

            model.addAttribute("oldPassword", oldPassword);
            model.addAttribute("newPassword", newPassword);
            model.addAttribute("confirmPassword", confirmPassword);

            // 返回到原本的页面
            return "/site/setting";
        }
    }

    // 返回个人主页的页面
    @GetMapping("/profile/{userId}")
    public String getProfilePage(@PathVariable("userId") Integer userId, Model model) {
        // 1.查询该用户
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("该用户不存在！");
        }

        // 2.添加用户对象和点赞数量
        model.addAttribute("user", user);
        int likeCount = likeService.findUserLikeCount(user.getId());
        model.addAttribute("likeCount", likeCount);

        // 3.添加用户对象的 关注数量、粉丝数量、当前用户是否关注了这个用户
        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // 粉丝数量
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        // 当前用户是否关注了这个用户
        boolean hasFollowed = false;
        User curUser = hostHolder.getUser(); // 当前用户
        if (curUser != null) {
            hasFollowed = followService.hasFollowed(curUser.getId(), ENTITY_TYPE_USER, userId);
        }
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }

    // 返回“我的帖子”页面，支持分页功能
    @GetMapping("/myPost/{userId}")
    public String getMyPostPage(@PathVariable("userId") Integer userId,
                                Model model, Page page,
                                @RequestParam(name = "current", required = false) Integer current) {
        // 1.查询该用户
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("该用户不存在！");
        }

        // 2.查询并添加该用户的帖子数量
        int discussPostCount = discussPostService.findDiscussPostRows(userId);
        model.addAttribute("discussPostCount", discussPostCount);

        // 3.填入分页信息
        page.setLimit(5);
        page.setPath("/user/myPost/" + userId);
        page.setRows(discussPostCount);
        if (current != null) {
            page.setCurrent(current);
        }

        // 4.查询并添加该用户发布的帖子集合(默认的使用方法，不用按照热度排序)
        List<DiscussPost> discussPosts =
                discussPostService.findDiscussPosts(userId, page.getOffset(), page.getLimit(), 0);
        // 需要查询每个帖子收到的赞的数量，将帖子集合转化为 VO 对象集合
        List<Map<String, Object>> discussPostVoList = new ArrayList<>();
        if (discussPosts != null) {
            for (DiscussPost discussPost : discussPosts) {
                // 创建每个帖子的 VO 对象
                Map<String, Object> discussPostVo = new HashMap<>();

                // 4.1添加帖子对象本身
                discussPostVo.put("discussPost", discussPost);

                // 4.2添加帖子的获赞数量
                discussPostVo.put("discussPostLikeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPost.getId()));

                // 添加这个 VO对象
                discussPostVoList.add(discussPostVo);
            }
        }
        model.addAttribute("discussPostVoList", discussPostVoList);

        return "/site/my-post";
    }

    // 返回“我的回复”页面，支持分页功能
    @GetMapping("/myComment/{userId}")
    public String getMyCommentPage(@PathVariable("userId") Integer userId,
                                   Model model, Page page,
                                   @RequestParam(name = "current", required = false) Integer current) {
        // 1.查询该用户
        User user = userService.findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("该用户不存在！");
        }

        // 2.查询并添加该用户的评论数量
        int commentCount = commentService.findCommentCountByUserId(userId);
        model.addAttribute("commentCount", commentCount);

        // 3.填入分页信息
        page.setLimit(5);
        page.setPath("/user/myComment/" + userId);
        page.setRows(commentCount);
        if (current != null) {
            page.setCurrent(current);
        }

        // 4.查询并添加该用户发布的评论集合
        List<Comment> commentList =
                commentService.findCommentsByUserId(userId, page.getOffset(), page.getLimit());
        // 需要查询每个帖子对应的评论标题，将评论集合转换为VO对象集合
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                // 创建一个 评论 VO 对象
                Map<String, Object> commentVo = new HashMap<>();

                // 4.1添加评论对象本身
                commentVo.put("comment", comment);

                // 4.2添加帖子对象，帖子的id 就是 评论的entityId
                commentVo.put("discussPost", discussPostService.findDiscussPostById(comment.getEntityId()));

                // 添加这个 VO 对象
                commentVoList.add(commentVo);
            }
        }
        model.addAttribute("commentVoList", commentVoList);

        return "/site/my-reply";
    }
}
