package com.whut.community.controller;

import com.whut.community.entity.Event;
import com.whut.community.entity.User;
import com.whut.community.event.EventProducer;
import com.whut.community.service.LikeService;
import com.whut.community.util.CommunityConstant;
import com.whut.community.util.CommunityUtil;
import com.whut.community.util.HostHolder;
import com.whut.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class LikeController implements CommunityConstant {

    private LikeService likeService;

    private HostHolder hostHolder;

    private EventProducer eventProducer;

    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public LikeController(LikeService likeService,
                          HostHolder hostHolder,
                          EventProducer eventProducer,
                          RedisTemplate<String, Object> redisTemplate) {
        this.likeService = likeService;
        this.hostHolder = hostHolder;
        this.eventProducer = eventProducer;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/like")
    @ResponseBody
    public String like(int entityType, int entityId, int entityUserId, int postId) {

        // 1.获取当前用户
        User user = hostHolder.getUser();

        // 2.进行点赞操作，
        // 对 user 是否为空的判断处理之后会统一进行
        likeService.like(user.getId(), entityType, entityId, entityUserId);

        // 3.根据点赞的数量和状态更新页面的值
        long likeCount = likeService.findEntityLikeCount(entityType, entityId);
        int likeStatus = likeService.findEntityLikeStatus(user.getId(), entityType, entityId);
        // 返回的结果
        Map<String, Object> map = new HashMap<>();
        map.put("likeCount", likeCount);
        map.put("likeStatus", likeStatus);

        // 点赞后，触发点赞事件，注意取消点赞时不会触发取消点赞的事件，
        // 创建一个 Event 对象，由事件的生产者 eventProducer 存入到消息队列
        if (likeStatus == 1) {
            Event event = new Event();
            event.setUserId(user.getId())
                    .setTopic(TOPIC_LIKE)
                    .setEntityType(entityType)
                    .setEntityId(entityId)
                    .setEntityUserId(entityUserId)
                    .setData("postId", postId);

            // 发布消息
            eventProducer.fireEvent(event);
        }

        // 无论是点赞还是取消点赞，
        // 只有这个操作的对象是帖子时才重新计算帖子的分数(操作对象是评论就不用管)
        if (entityType == ENTITY_TYPE_POST) {
            // 计算帖子分数，首先将这个帖子 id 存入 Redis
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, postId);
        }

        return CommunityUtil.getJSONString(0, null, map);
    }

}
