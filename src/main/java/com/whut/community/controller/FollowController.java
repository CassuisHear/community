package com.whut.community.controller;

import com.whut.community.entity.Event;
import com.whut.community.entity.Page;
import com.whut.community.entity.User;
import com.whut.community.event.EventProducer;
import com.whut.community.service.FollowService;
import com.whut.community.service.UserService;
import com.whut.community.util.CommunityConstant;
import com.whut.community.util.CommunityUtil;
import com.whut.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class FollowController implements CommunityConstant {

    private FollowService followService;

    private HostHolder hostHolder;

    private UserService userService;

    private EventProducer eventProducer;

    @Autowired
    public FollowController(FollowService followService,
                            HostHolder hostHolder,
                            UserService userService,
                            EventProducer eventProducer) {
        this.followService = followService;
        this.hostHolder = hostHolder;
        this.userService = userService;
        this.eventProducer = eventProducer;
    }

    // 关注
    @PostMapping("/follow")
    @ResponseBody
    public String follow(int entityType, int entityId) {
        // 1.获取当前用户
        User user = hostHolder.getUser();

        // 2.执行关注逻辑
        followService.follow(user.getId(), entityType, entityId);

        // 关注后，触发关注事件，注意取关时不会触发取关事件
        // 创建一个 Event 对象，由事件的生产者 eventProducer 存入到消息队列
        Event event = new Event();
        event.setUserId(user.getId())
                .setTopic(TOPIC_FOLLOW)
                .setEntityType(entityType)
                .setEntityId(entityId)
                .setEntityUserId(entityId); // 关注的是人，实体id 就是 实体所依附的用户id

        // 发布消息
        eventProducer.fireEvent(event);

        return CommunityUtil.getJSONString(0, "已关注！");
    }

    // 取关
    @PostMapping("/unFollow")
    @ResponseBody
    public String unFollow(int entityType, int entityId) {
        // 1.获取当前用户
        User user = hostHolder.getUser();

        // 2.执行取关逻辑
        followService.unFollow(user.getId(), entityType, entityId);

        return CommunityUtil.getJSONString(0, "已取消关注！");
    }

    /**
     * 展示关注列表：
     * 行为：点击某个用户的关注列表(包括自己)，
     * 点击的用户为 目标用户，
     * 发起点击行为的用户为 当前用户，
     * 关注列表中的用户为 目标用户的关注者
     *
     * @param userId  目标用户的id
     * @param page    分页对象
     * @param model   Model对象
     * @param current 当前页码(默认为1)
     * @return 渲染为关注列表的页面
     */
    @GetMapping("/followeeList/{userId}")
    public String getFolloweeList(@PathVariable("userId") Integer userId,
                                  Page page, Model model,
                                  @RequestParam(name = "current", required = false) Integer current) {
        // 1.查询目标用户
        User targetUser = userService.findUserById(userId);
        if (targetUser == null) {
            throw new RuntimeException("该用户不存在！");
        }

        // 2.添加用户数据
        model.addAttribute("targetUser", targetUser);

        // 3.设置并添加分页数据
        page.setLimit(5);
        page.setPath("/followeeList/" + userId);
        page.setRows((int) followService.findFolloweeCount(userId, ENTITY_TYPE_USER));
        if (current != null) {
            page.setCurrent(current);
        }

        // 4.查询并添加所有的关注者数据
        List<Map<String, Object>> followeeVoList =
                followService.findFolloweeList(userId, page.getOffset(), page.getLimit());
        // 需要补充每个 关注者 和 当前用户 的关系(当前用户 是否关注了列表中的 关注者)
        if (followeeVoList != null) {
            for (Map<String, Object> followeeVo : followeeVoList) {
                User followee = (User) followeeVo.get("followee");
                followeeVo.put("hasFollowed", hasFollowed(followee.getId()));
            }
        }
        model.addAttribute("followeeVoList", followeeVoList);

        return "/site/followee";
    }

    /**
     * 展示粉丝列表：
     * 行为：点击某个用户的粉丝列表(包括自己)，
     * 点击的用户为 目标用户，
     * 发起点击行为的用户为 当前用户，
     * 粉丝列表中的用户为 目标用户的粉丝
     *
     * @param userId  目标用户的id
     * @param page    分页对象
     * @param model   Model对象
     * @param current 当前页码(默认为1)
     * @return 渲染为粉丝列表的页面
     */
    @GetMapping("/followerList/{userId}")
    public String getFollowerList(@PathVariable("userId") Integer userId,
                                  Page page, Model model,
                                  @RequestParam(name = "current", required = false) Integer current) {
        // 1.查询目标用户
        User targetUser = userService.findUserById(userId);
        if (targetUser == null) {
            throw new RuntimeException("该用户不存在！");
        }

        // 2.添加用户数据
        model.addAttribute("targetUser", targetUser);

        // 3.设置并添加分页数据
        page.setLimit(5);
        page.setPath("/followerList/" + userId);
        page.setRows((int) followService.findFollowerCount(ENTITY_TYPE_USER, userId));
        if (current != null) {
            page.setCurrent(current);
        }

        // 4.查询并添加所有的粉丝数据
        List<Map<String, Object>> followerVoList =
                followService.findFollowerList(userId, page.getOffset(), page.getLimit());
        // 需要补充每个 粉丝 和 当前用户 的关系(当前用户 是否关注了列表中的 粉丝)
        if (followerVoList != null) {
            for (Map<String, Object> followerVo : followerVoList) {
                User follower = (User) followerVo.get("follower");
                followerVo.put("hasFollowed", hasFollowed(follower.getId()));
            }
        }
        model.addAttribute("followerVoList", followerVoList);

        return "/site/follower";
    }

    /**
     * 判断 当前用户 是否关注了列表中的 关注者或者粉丝
     *
     * @param followeeId 关注者id
     * @return 是否关注了这个关注者
     */
    private boolean hasFollowed(int followeeId) {
        // 1.获取当前用户
        User curUser = hostHolder.getUser();
        if (curUser == null) {
            return false;
        }

        // 2.调用方法返回结果
        return followService.hasFollowed(curUser.getId(), ENTITY_TYPE_USER, followeeId);
    }
}
