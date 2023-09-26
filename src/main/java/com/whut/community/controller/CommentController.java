package com.whut.community.controller;

import com.whut.community.annotation.LoginRequired;
import com.whut.community.entity.Comment;
import com.whut.community.entity.DiscussPost;
import com.whut.community.entity.Event;
import com.whut.community.entity.User;
import com.whut.community.event.EventProducer;
import com.whut.community.service.CommentService;
import com.whut.community.service.DiscussPostService;
import com.whut.community.util.CommunityConstant;
import com.whut.community.util.HostHolder;
import com.whut.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Date;

@Controller
@RequestMapping("/comment")
public class CommentController implements CommunityConstant {

    private CommentService commentService;

    private HostHolder hostHolder;

    private EventProducer eventProducer;

    private DiscussPostService discussPostService;

    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public CommentController(CommentService commentService,
                             HostHolder hostHolder,
                             EventProducer eventProducer,
                             DiscussPostService discussPostService,
                             RedisTemplate<String, Object> redisTemplate) {
        this.commentService = commentService;
        this.hostHolder = hostHolder;
        this.eventProducer = eventProducer;
        this.discussPostService = discussPostService;
        this.redisTemplate = redisTemplate;
    }

    /*
        注入的 Comment 类对象会接收前端页面传入的 3个 或 4个 属性：
        针对帖子 的评论 或者 没有目标用户的回复 (此时 targetId 为默认值0)：
        1.entityType：由隐藏域传入；
        2.entityId：由隐藏域传入；
        3.content：post请求传入的评论内容

        另外，如果是 针对评论的评论(即回复) 且 该回复还有目标用户，还会有：
        4.targetId：由隐藏域传入；
     */
    @LoginRequired
    @PostMapping("/add/{discussPostId}")
    public String addComment(@PathVariable("discussPostId") int discussPostId,
                             Comment comment) {
        // hostHolder.getUser() 可能为空，异常会统一处理
        User user = hostHolder.getUser();
        comment.setUserId(user.getId());
        // 默认评论为有效状态
        comment.setStatus(0);
        // 设置评论的创建时间
        comment.setCreateTime(new Date());

        // 添加评论
        commentService.addComment(comment);

        // 添加评论后，触发评论事件，
        // 创建一个 Event 对象，由事件的生产者 eventProducer 存入到消息队列
        Event event = new Event();
        event.setUserId(user.getId())
                .setTopic(TOPIC_COMMENT)
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId); // 额外数据，帖子 id

        // 还需要设置 Event 的 entityUserId 属性，
        // 此 comment 可能是针对帖子的，也可能是针对评论的，
        // 需要根据 comment 的 EntityType 属性来判断
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            // 获取 comment 针对的帖子对象
            DiscussPost targetDiscussPost = discussPostService.findDiscussPostById(comment.getEntityId());
            // 存入帖子对象所依附的用户id
            event.setEntityUserId(targetDiscussPost.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT) {
            // 获取 comment 针对的评论对象
            Comment targetComment = commentService.findCommentById(comment.getEntityId());
            // 存入评论对象所依附的用户id
            event.setEntityUserId(targetComment.getUserId());
        }
        // 发布消息到 comment 话题中
        eventProducer.fireEvent(event);

        // 如果是针对帖子的评论，还要发布一个新的消息到 publish 话题中
        if (comment.getEntityType() == ENTITY_TYPE_POST) {
            event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(user.getId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId); // 不用调用 setEntityUserId() 方法
            // 将该事件存储到 Kafka 消息队列中
            eventProducer.fireEvent(event);

            // 计算帖子分数，首先将这个帖子 id 存入 Redis
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, discussPostId);
        }

        return "redirect:/discuss/detail/" + discussPostId;
    }
}
