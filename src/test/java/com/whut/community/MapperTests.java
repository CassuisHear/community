package com.whut.community;

import com.whut.community.dao.DiscussPostMapper;
import com.whut.community.dao.LoginTicketMapper;
import com.whut.community.dao.MessageMapper;
import com.whut.community.dao.UserMapper;
import com.whut.community.entity.DiscussPost;
import com.whut.community.entity.LoginTicket;
import com.whut.community.entity.Message;
import com.whut.community.entity.User;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTests {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Autowired
    private MessageMapper messageMapper;

    @Test
    public void testSelectUser() {
        User user = userMapper.selectById(101);
        System.out.println("user = " + user);

        user = userMapper.selectByName("liubei");
        System.out.println("user = " + user);

        user = userMapper.selectByEmail("nowcoder101@sina.com");
        System.out.println("user = " + user);
    }

    @Test
    public void testInsertUser() {
        User user = new User();
        user.setUsername("test");
        user.setPassword("123456");
        user.setSalt("abc");
        user.setEmail("test@qq.com");
        user.setHeaderUrl("http://www.nowcoder.com/101.png");
        user.setCreateTime(new Date());

        int rows = userMapper.insertUser(user);
        System.out.println(rows);
        System.out.println("id = " + user.getId());
    }

    @Test
    public void testUpdateUser() {
        int rows = userMapper.updateStatus(150, 1);
        System.out.println("rows = " + rows);

        rows = userMapper.updateHeader(150, "http://www.nowcoder.com/102.png");
        System.out.println("rows = " + rows);

        rows = userMapper.updatePassword(150, "hello");
        System.out.println("rows = " + rows);
    }

    @Test
    public void testSelectPosts() {
        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(149, 0, 10, 0);
        for (DiscussPost discussPost : discussPosts) {
            System.out.println("discussPost = " + discussPost);
        }

        int rows = discussPostMapper.selectDiscussPostRows(149);
        System.out.println("rows = " + rows);
    }

    @Test
    public void testInsertLoginTicket() {
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(101);
        loginTicket.setTicket("abcdefg");
        loginTicket.setStatus(0);
        // 10 分钟后到期
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000 * 60 * 10));

        int result = loginTicketMapper.insertLoginTicket(loginTicket);
        System.out.println("result = " + result);
    }

    @Test
    public void testSelectByTicket() {
        LoginTicket ticket = loginTicketMapper.selectByTicket("abcdefg");
        System.out.println("ticket = " + ticket);
    }

    @Test
    public void testUpdateStatus() {
        int result = loginTicketMapper.updateStatus("abcdefg", 1);
        System.out.println("result = " + result);
    }

    @Test
    public void testInsertDiscussPost() {
        DiscussPost testDiscussPost = new DiscussPost(null, 149, "测试帖子1", "测试内容1", 0, 0, new Date(), 20, 1755.2095150145426);
        int res = discussPostMapper.insertDiscussPost(testDiscussPost);
        System.out.println("res = " + res);
    }

    @Test
    public void testSelectLetters() {
        List<Message> messages = messageMapper.selectConversations(111, 0, 20);
        for (Message message : messages) {
            System.out.println("message = " + message);
        }

        System.out.println("==============================");

        int conversationCount = messageMapper.selectConversationCount(111);
        System.out.println("conversationCount = " + conversationCount);

        System.out.println("==============================");

        List<Message> letters = messageMapper.selectLetters("111_112", 0, 20);
        for (Message letter : letters) {
            System.out.println("letter = " + letter);
        }

        System.out.println("==============================");

        int letterCount = messageMapper.selectLetterCount("111_112");
        System.out.println("letterCount = " + letterCount);

        System.out.println("==============================");

        int letterUnreadCount = messageMapper.selectLetterUnreadCount(131, "111_131");
        System.out.println("letterUnreadCount = " + letterUnreadCount);
    }
}
