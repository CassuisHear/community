package com.whut.community.service;

import com.whut.community.dao.MessageMapper;
import com.whut.community.entity.Message;
import com.whut.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class MessageService {

    private MessageMapper messageMapper;

    private SensitiveFilter sensitiveFilter;

    @Autowired
    public MessageService(MessageMapper messageMapper,
                          SensitiveFilter sensitiveFilter) {
        this.messageMapper = messageMapper;
        this.sensitiveFilter = sensitiveFilter;
    }

    public List<Message> findConversations(int userId, int offset, int limit) {
        return messageMapper.selectConversations(userId, offset, limit);
    }

    public int findConversationCount(int userId) {
        return messageMapper.selectConversationCount(userId);
    }

    public List<Message> findLetters(String conversationId, int offset, int limit) {
        return messageMapper.selectLetters(conversationId, offset, limit);
    }

    public int findLetterCount(String conversationId) {
        return messageMapper.selectLetterCount(conversationId);
    }

    public int findLetterUnreadCount(int userId, String conversationId) {
        return messageMapper.selectLetterUnreadCount(userId, conversationId);
    }

    // 添加一条私信，在添加之前需要对私信的内容进行 HTML 转义和过滤
    public int addMessage(Message message) {
        message.setContent(HtmlUtils.htmlEscape(message.getContent()));
        message.setContent(sensitiveFilter.filter(message.getContent()));
        return messageMapper.insertMessage(message);
    }

    // 将消息的状态改为已读
    public int readMessages(List<Integer> ids) {
        return messageMapper.updateStatus(ids, 1);
    }

    // 更改一条消息的状态
    public int updateOneMessageStatus(int id, int status) {
        return messageMapper.updateOneMessageStatus(id, status);
    }

    // 查询某个topic下最新的通知
    public Message findLatestNotice(int userId, String topic) {
        return messageMapper.selectLatestNotice(userId, topic);
    }

    // 查询某个topic下通知的数量
    public int findNoticeCount(int userId, String topic) {
        return messageMapper.selectNoticeCount(userId, topic);
    }

    // 查询某个topic下未读消息的数量，topic为空时查询所有的未读通知的数量
    public int findNoticeUnreadCount(int userId, String topic) {
        return messageMapper.selectNoticeUnreadCount(userId, topic);
    }

    // 查询某个主题包含的通知列表
    public List<Message> findNotices(int userId, String topic, int offset, int limit) {
        return messageMapper.selectNotices(userId, topic, offset, limit);
    }
}
