package com.whut.community.controller;

import com.whut.community.entity.*;
import com.whut.community.event.EventProducer;
import com.whut.community.service.CommentService;
import com.whut.community.service.DiscussPostService;
import com.whut.community.service.LikeService;
import com.whut.community.service.UserService;
import com.whut.community.util.CommunityConstant;
import com.whut.community.util.CommunityUtil;
import com.whut.community.util.HostHolder;
import com.whut.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/discuss")
public class DiscussPostController implements CommunityConstant {

    private DiscussPostService discussPostService;

    private HostHolder hostHolder;

    private UserService userService;

    private CommentService commentService;

    private LikeService likeService;

    private EventProducer eventProducer;

    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public DiscussPostController(DiscussPostService discussPostService,
                                 HostHolder hostHolder,
                                 UserService userService,
                                 CommentService commentService,
                                 LikeService likeService,
                                 EventProducer eventProducer,
                                 RedisTemplate<String, Object> redisTemplate) {
        this.discussPostService = discussPostService;
        this.hostHolder = hostHolder;
        this.userService = userService;
        this.commentService = commentService;
        this.likeService = likeService;
        this.eventProducer = eventProducer;
        this.redisTemplate = redisTemplate;
    }

    /*
        由于这里使用的是异步请求，
        对页面的修改结果是局部的，
        所以不能使用 @LoginRequired 注解来要求用户必须登录，
        (这个注解最好用在同步的请求中)
     */
    @PostMapping("/add")
    @ResponseBody
    public String addDiscussPost(String title, String content) {
        // 判断用户是否登录
        User user = hostHolder.getUser();
        if (user == null) {
            return CommunityUtil.getJSONString(403, "未登录或登陆过期，请重新登陆");
        }

        // 构建帖子对象并添加到数据库中
        DiscussPost discussPost = new DiscussPost();
        discussPost.setUserId(user.getId());
        discussPost.setTitle(title);
        discussPost.setContent(content);
        discussPost.setCreateTime(new Date());
        discussPost.setCommentCount(0);
        discussPost.setScore(0.0);

        // 出现的异常在外部统一处理
        discussPostService.addDiscussPost(discussPost);

        // 触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(discussPost.getId()); // 不用调用 setEntityUserId() 方法
        // 将该事件存储到 Kafka 消息队列中
        eventProducer.fireEvent(event);

        // 计算帖子分数，首先将这个帖子 id 存入 Redis
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, discussPost.getId());

        return CommunityUtil.getJSONString(0, "发布成功！");
    }

    @GetMapping("/detail/{discussPostId}")
    public String getDiscussPost(@PathVariable("discussPostId") int discussPostId, Model model,
                                 Page page, @RequestParam(name = "current", required = false) Integer current) {

        // 1.查询帖子本体
        DiscussPost post = discussPostService.findDiscussPostById(discussPostId);
        model.addAttribute("post", post);

        // 2.根据帖子的 userId 查询对应的作者
        User user = userService.findUserById(post.getUserId());
        model.addAttribute("user", user);

        // 3.查询点赞相关的信息
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeCount", likeCount);
        User curUser = hostHolder.getUser();
        int likeStatus = curUser == null ? 0 :
                likeService.findEntityLikeStatus(curUser.getId(), ENTITY_TYPE_POST, discussPostId);
        model.addAttribute("likeStatus", likeStatus);

        // 4.评论的分页信息
        page.setLimit(5);
        page.setPath("/discuss/detail/" + discussPostId);
        // 这里可以调用 commentService 中的方法来查询对应的评论数量，
        // 但是帖子本体 post 中有 commentCount 这一属性，可以直接使用
        page.setRows(post.getCommentCount());
        // ※Page 类的设计，需要先设置rows属性再设置current属性※
        if (current != null) {
            page.setCurrent(current);
        }

        // 5.根据帖子本体 post 的属性 和 分页信息
        // 查询所有 **针对帖子的** 评论
        List<Comment> commentList = commentService.findCommentsByEntity(
                ENTITY_TYPE_POST, post.getId(), page.getOffset(), page.getLimit());
        // 根据此 List 集合，查询 评论 的 VO 对象(即视图对象)集合
        List<Map<String, Object>> commentVoList = new ArrayList<>();
        if (commentList != null) {
            for (Comment comment : commentList) {
                // 创建这个评论的 VO 对象
                Map<String, Object> commentVo = new HashMap<>();

                // =============================================
                // 对于每个 **针对帖子的** 评论，
                // 5.0除了评论自身之外，
                commentVo.put("comment", comment);

                // 还要存储：
                // 5.1这个评论对应的用户
                commentVo.put("user", userService.findUserById(comment.getUserId()));

                // 5.2这个评论的点赞信息
                likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeCount", likeCount);
                likeStatus = curUser == null ? 0 :
                        likeService.findEntityLikeStatus(curUser.getId(), ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("likeStatus", likeStatus);

                // 5.3**针对评论的** 评论，即 回复 构成的集合 replyVoList
                // 所有回复不用分页查询，所以偏移量 offset 从0开始，直接查询所有数据
                List<Comment> replyList = commentService.findCommentsByEntity(
                        ENTITY_TYPE_COMMENT, comment.getId(), 0, Integer.MAX_VALUE);
                // 对于所有的回复，也需要做 和评论类似的操作，即查询 回复 的 VO 对象(即视图对象)集合
                List<Map<String, Object>> replyVoList = new ArrayList<>();
                if (replyList != null) {
                    for (Comment reply : replyList) {
                        // 创建这个回复对象
                        Map<String, Object> replyVo = new HashMap<>();

                        // ======================
                        // 需要存储的内容有：
                        // 5.3.1回复本身
                        replyVo.put("reply", reply);

                        // 5.3.2回复的作者
                        replyVo.put("user", userService.findUserById(reply.getUserId()));

                        // 5.3.3回复的目标用户，
                        // targetId 为0表示没有回复的目标用户
                        int targetId = reply.getTargetId();
                        User targetUser = targetId == 0 ? null : userService.findUserById(targetId);
                        replyVo.put("targetUser", targetUser);

                        // 5.3.4回复中的点赞信息
                        likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeCount", likeCount);
                        likeStatus = curUser == null ? 0 :
                                likeService.findEntityLikeStatus(curUser.getId(), ENTITY_TYPE_COMMENT, reply.getId());
                        replyVo.put("likeStatus", likeStatus);
                        // ======================

                        // 将此回复 replyVo 添加到 回复集合 replyVoList 中
                        replyVoList.add(replyVo);
                    }
                }
                // 添加 回复集合 replyVoList
                commentVo.put("replies", replyVoList);

                // 5.4添加这个评论包含的 回复 数量
                int replyCount = commentService.findCommentCountByEntity(ENTITY_TYPE_COMMENT, comment.getId());
                commentVo.put("replyCount", replyCount);
                // =============================================

                // 将此评论 commentVo 添加到 评论集合 commentVoList 中
                commentVoList.add(commentVo);
            }
        }
        // 6.将评论集合 commentVoList 添加到 model 中
        model.addAttribute("comments", commentVoList);

        return "/site/discuss-detail";
    }

    // 置顶、取消置顶
    @PostMapping("/top")
    @ResponseBody
    public String setTop(int id) {
        // 1.获取当前用户
        User user = hostHolder.getUser();

        // 2.查询帖子对象
        DiscussPost discussPost = discussPostService.findDiscussPostById(id);
        // 获取置顶类型，1为置顶，0为正常，1 ^ 1 = 0 ; 0 ^ 1 = 1
        // 通过这种 ^ 方式来更改帖子类型为对立面
        int type = discussPost.getType() ^ 1;
        // 3.更新帖子类型
        discussPostService.updateType(id, type);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);

        // 4.触发发帖事件(更改帖子状态)
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id); // 不用调用 setEntityUserId() 方法
        // 将该事件存储到 Kafka 消息队列中
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, type == 1 ? "置顶成功！" : "取消置顶成功！", map);
    }

    // 加精、取消加精
    @PostMapping("/wonderful")
    @ResponseBody
    public String setWonderful(int id) {
        // 1.获取当前用户
        User user = hostHolder.getUser();

        // 2.查询帖子对象
        DiscussPost discussPost = discussPostService.findDiscussPostById(id);
        // 获取帖子状态，1为加精，0为正常， 1 ^ 1 = 0 ; 0 ^ 1 = 1
        // 通过这种 ^ 方式来更改帖子状态为对立面
        int status = discussPost.getStatus() ^ 1;
        // 3.更新帖子状态
        discussPostService.updateStatus(id, status);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("status", status);

        // 3.触发发帖事件
        Event event = new Event()
                .setTopic(TOPIC_PUBLISH)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id); // 不用调用 setEntityUserId() 方法
        // 将该事件存储到 Kafka 消息队列中
        eventProducer.fireEvent(event);

        // 无论是加精还是取消加精，帖子的分数都应该保持在最新状态来计算
        String redisKey = RedisKeyUtil.getPostScoreKey();
        redisTemplate.opsForSet().add(redisKey, id);

        return CommunityUtil.getJSONString(0, status == 1 ? "加精成功！" : "取消加精成功！", map);
    }

    // 删除
    @PostMapping("/delete")
    @ResponseBody
    public String setDelete(int id) {
        // 1.获取当前用户
        User user = hostHolder.getUser();

        // 2.更新帖子状态 为 删除
        discussPostService.updateStatus(id, 2);

        // 3.触发删帖事件
        Event event = new Event()
                .setTopic(TOPIC_DELETE)
                .setUserId(user.getId())
                .setEntityType(ENTITY_TYPE_POST)
                .setEntityId(id); // 不用调用 setEntityUserId() 方法
        // 将该事件存储到 Kafka 消息队列中
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0);
    }
}
