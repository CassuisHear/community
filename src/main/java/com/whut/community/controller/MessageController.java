package com.whut.community.controller;

import com.alibaba.fastjson.JSONObject;
import com.whut.community.entity.Message;
import com.whut.community.entity.Page;
import com.whut.community.entity.User;
import com.whut.community.service.MessageService;
import com.whut.community.service.UserService;
import com.whut.community.util.CommunityConstant;
import com.whut.community.util.CommunityUtil;
import com.whut.community.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class MessageController implements CommunityConstant {

    private MessageService messageService;

    private HostHolder hostHolder;

    private UserService userService;

    @Autowired
    public MessageController(MessageService messageService,
                             HostHolder hostHolder,
                             UserService userService) {
        this.messageService = messageService;
        this.hostHolder = hostHolder;
        this.userService = userService;
    }

    @GetMapping("/letter/list")
    public String getLetterList(Model model, Page page,
                                @RequestParam(name = "current", required = false) Integer current) {
        // user 可能为空，异常统一处理
        User user = hostHolder.getUser();

        // 1.填入分页信息
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));
        if (current != null) {
            page.setCurrent(current);
        }

        // 2.查询会话列表
        List<Message> conversationList = messageService.findConversations(
                user.getId(), page.getOffset(), page.getLimit());
        // 根据会话列表封装为 conversationVo 对象集合
        List<Map<String, Object>> conversationVoList = new ArrayList<>();
        if (conversationList != null) {
            for (Message conversation : conversationList) {
                // 创建每个 conversationVo 对象
                Map<String, Object> conversationVo = new HashMap<>();

                // 2.1添加 conversation 对象
                conversationVo.put("conversation", conversation);

                // 2.2添加未读消息数量
                conversationVo.put("unreadCount",
                        messageService.findLetterUnreadCount(user.getId(), conversation.getConversationId()));

                // 2.3添加每个会话的私信总数量
                conversationVo.put("letterCount", messageService.findLetterCount(conversation.getConversationId()));

                // 2.4添加与本用户对话的那个用户，
                // 可能是消息的发送者，也可能是消息的接收者
                int targetId = user.getId() == conversation.getFromId() ? conversation.getToId() : conversation.getFromId();
                conversationVo.put("targetUser", userService.findUserById(targetId));

                // 将每个 conversationVo 对象添加到集合中
                conversationVoList.add(conversationVo);
            }
        }
        // 将 会话对象集合 conversationVoList 存入到 model 中
        model.addAttribute("conversations", conversationVoList);

        // 3.查询并添加本用户的所有未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);

        // 4.查询所有未读通知的数量
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/letter";
    }

    // conversationCurrentPage 是当前会话所在的页码
    @GetMapping("/letter/detail/{conversationId}/{conversationCurrentPage}")
    public String getLetterDetail(@PathVariable("conversationId") String conversationId,
                                  @PathVariable("conversationCurrentPage") Integer conversationCurrentPage,
                                  Model model, Page page,
                                  @RequestParam(name = "current", required = false) Integer current) {

        // 1.填入分页信息
        page.setLimit(5);
        page.setPath("/letter/detail/" + conversationId + "/" + conversationCurrentPage);
        page.setRows(messageService.findLetterCount(conversationId));
        if (current != null) {
            page.setCurrent(current);
        }

        // 2.存入当前会话所在的页码，方便从详情页跳回到这个页面
        model.addAttribute("conversationCurrentPage", conversationCurrentPage);

        // 3.查询当前会话的所有私信构成的私信列表
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());

        // 4.将所有未读消息转换为已读
        List<Integer> ids = getUnreadLetterIds(letterList);
        if (!ids.isEmpty()) {
            messageService.readMessages(ids);
        }

        // 将私信列表转换为 VO 对象集合
        List<Map<String, Object>> letters = new ArrayList<>();
        if (letterList != null) {
            for (Message letter : letterList) {
                // 创建一个 letterVo 对象
                Map<String, Object> letterVo = new HashMap<>();

                // 2.1添加私信本身
                letterVo.put("letter", letter);

                // 2.2添加发送私信的人
                letterVo.put("fromUser", userService.findUserById(letter.getFromId()));

                letters.add(letterVo);
            }
        }
        model.addAttribute("letters", letters);

        // 5.查询并添加当前会话中的 私信目标
        model.addAttribute("targetUser", getLetterTarget(conversationId));

        return "/site/letter-detail";
    }

    /**
     * 获取未读私信的id
     *
     * @param letterList 私信列表
     * @return 未读私信的id
     */
    private List<Integer> getUnreadLetterIds(List<Message> letterList) {
        List<Integer> ids = new ArrayList<>();

        // 获取当前用户的id
        Integer curUserId = hostHolder.getUser().getId();
        if (letterList != null) {
            for (Message message : letterList) {
                // 当且仅当 当前用户是接收者 并且 消息的状态是未读时才添加到答案中
                if (curUserId == message.getToId() && message.getStatus() == 0) {
                    ids.add(message.getId());
                }
            }
        }

        return ids;
    }

    /**
     * 根据 会话 id 查询发送私信的目标用户
     *
     * @param conversationId 会话id
     * @return 目标用户
     */
    private User getLetterTarget(String conversationId) {

        // 1.将会话id拆分
        String[] twoId = conversationId.split("_");

        // 2.将两个id转换为整形
        int id0 = Integer.parseInt(twoId[0]);
        int id1 = Integer.parseInt(twoId[1]);

        // 3.将对应用户返回
        return hostHolder.getUser().getId() == id0 ?
                userService.findUserById(id1) :
                userService.findUserById(id0);
    }

    @PostMapping("/letter/send")
    @ResponseBody
    public String sendLetter(String toName, String content) {
        // 1.根据用户名查询这个目标用户
        User targetUser = userService.findUserByName(toName);
        if (targetUser == null) {
            return CommunityUtil.getJSONString(1, "目标用户不存在！");
        }

        // 2.构造需要插入的 Message 对象
        Message message = new Message();
        Integer fromId = hostHolder.getUser().getId();
        Integer toId = targetUser.getId();
        message.setFromId(fromId);
        message.setToId(toId);
        // 会话id，小的id在前，大的id在后
        if (fromId.compareTo(toId) < 0) {
            message.setConversationId(fromId + "_" + toId);
        } else {
            message.setConversationId(toId + "_" + fromId);
        }
        message.setContent(content);
        message.setCreateTime(new Date());

        // 3.将 message 对象插入到数据库中
        messageService.addMessage(message);

        return CommunityUtil.getJSONString(0);
    }

    @GetMapping("/letter/delete/{letterId}")
    @ResponseBody
    public String deleteLetter(@PathVariable("letterId") Integer letterId) {

        // 根据消息 letter 的 id 将其状态改为2("已删除"状态)
        int res = messageService.updateOneMessageStatus(letterId, 2);
        if (res > 0) {
            return CommunityUtil.getJSONString(0);
        } else { // 更改失败
            return CommunityUtil.getJSONString(1);
        }
    }

    @GetMapping("/notice/list")
    public String getNoticeList(Model model) {
        // 1.获取当前用户
        User user = hostHolder.getUser();

        // 2.查询评论类通知
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        if (message != null) {
            // 转化为 对应的VO对象
            Map<String, Object> messageVo = new HashMap<>();

            // 2.1添加 message 对象本身
            messageVo.put("message", message);

            // 2.2将 message的content字段中的值取出来(注意需要对html字符进行反转义)，
            String content = HtmlUtils.htmlUnescape(message.getContent());
            // 转化为一个 HashMap(在 EventConsumer 中是将 HashMap 转化为 JSONString 的)
            HashMap data = JSONObject.parseObject(content, HashMap.class);

            // 2.3根据 data 中的数据将各个对象存储到 messageVo 对象中
            if (data != null) {
                messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
                messageVo.put("entityType", data.get("entityType"));
                messageVo.put("entityId", data.get("entityId"));
                messageVo.put("postId", data.get("postId"));
            }

            // 2.4查询这一类通知的 总数量 和 未读通知数量
            int noticeCount = messageService.findNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("noticeCount", noticeCount);
            int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("noticeUnreadCount", noticeUnreadCount);

            model.addAttribute("commentNotice", messageVo);
        }

        // 3.查询点赞类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);
        if (message != null) {
            // 转化为 对应的VO对象
            Map<String, Object> messageVo = new HashMap<>();

            // 3.1添加 message 对象本身
            messageVo.put("message", message);

            // 3.2将 message的content字段中的值取出来(注意需要对html字符进行反转义)，
            String content = HtmlUtils.htmlUnescape(message.getContent());
            // 转化为一个 HashMap(在 EventConsumer 中是将 HashMap 转化为 JSONString 的)
            HashMap data = JSONObject.parseObject(content, HashMap.class);

            // 3.3根据 data 中的数据将各个对象存储到 messageVo 对象中
            if (data != null) {
                messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
                messageVo.put("entityType", data.get("entityType"));
                messageVo.put("entityId", data.get("entityId"));
                messageVo.put("postId", data.get("postId"));
            }

            // 3.4查询这一类通知的 总数量 和 未读通知数量
            int noticeCount = messageService.findNoticeCount(user.getId(), TOPIC_LIKE);
            messageVo.put("noticeCount", noticeCount);
            int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_LIKE);
            messageVo.put("noticeUnreadCount", noticeUnreadCount);

            model.addAttribute("likeNotice", messageVo);
        }

        // 4.查询关注类通知
        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        if (message != null) {
            // 转化为 对应的VO对象
            Map<String, Object> messageVo = new HashMap<>();

            // 4.1添加 message 对象本身
            messageVo.put("message", message);

            // 4.2将 message的content字段中的值取出来(注意需要对html字符进行反转义)，
            String content = HtmlUtils.htmlUnescape(message.getContent());
            // 转化为一个 HashMap(在 EventConsumer 中是将 HashMap 转化为 JSONString 的)
            HashMap data = JSONObject.parseObject(content, HashMap.class);

            // 4.3根据 data 中的数据将各个对象存储到 messageVo 对象中
            if (data != null) {
                messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
                messageVo.put("entityType", data.get("entityType"));
                messageVo.put("entityId", data.get("entityId"));
            }

            // 4.4查询这一类通知的 总数量 和 未读通知数量
            int noticeCount = messageService.findNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("noticeCount", noticeCount);
            int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("noticeUnreadCount", noticeUnreadCount);

            model.addAttribute("followNotice", messageVo);
        }

        // 5.查询并添加本用户的所有未读消息数量
        int letterUnreadCount = messageService.findLetterUnreadCount(user.getId(), null);
        model.addAttribute("letterUnreadCount", letterUnreadCount);

        // 6.查询所有未读通知的数量
        int noticeUnreadCount = messageService.findNoticeUnreadCount(user.getId(), null);
        model.addAttribute("noticeUnreadCount", noticeUnreadCount);

        return "/site/notice";
    }

    @GetMapping("/notice/detail/{topic}")
    public String getNoticeDetail(@PathVariable("topic") String topic,
                                  Page page, Model model,
                                  @RequestParam(name = "current", required = false) Integer current) {
        // 1.获取当前用户
        User user = hostHolder.getUser();

        // 2.设置分页数据
        page.setLimit(5);
        page.setPath("/notice/detail/" + topic);
        page.setRows(messageService.findNoticeCount(user.getId(), topic));
        if (current != null) {
            page.setCurrent(current);
        }

        // 3.查询某个话题下的通知列表，将其转化为 VO 对象列表
        List<Message> noticeList =
                messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (noticeList != null) {
            for (Message notice : noticeList) {
                Map<String, Object> noticeVo = new HashMap<>();

                // 3.1添加通知本身
                noticeVo.put("notice", notice);

                // 3.2获取通知的内容，转化为data对象后再添加
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                // 转化为一个 HashMap(在 EventConsumer 中是将 HashMap 转化为 JSONString 的)
                HashMap data = JSONObject.parseObject(content, HashMap.class);

                // 3.3根据 data 中的数据将各个对象存储到 messageVo 对象中
                if (data != null) {
                    noticeVo.put("user", userService.findUserById((Integer) data.get("userId")));
                    noticeVo.put("entityType", data.get("entityType"));
                    noticeVo.put("entityId", data.get("entityId"));
                    // 评论类 和 点赞类 通知还应该包含postId
                    noticeVo.put("postId", data.get("postId"));
                }

                // 3.4查询消息的来源者(就是系统用户)
                noticeVo.put("fromUser", userService.findUserById(notice.getFromId()));

                noticeVoList.add(noticeVo);
            }
        }
        model.addAttribute("noticeVoList", noticeVoList);

        // 4.设置部分消息已读
        List<Integer> unreadIds = getUnreadLetterIds(noticeList);
        if (!unreadIds.isEmpty()) {
            messageService.readMessages(unreadIds);
        }

        return "/site/notice-detail";
    }
}
