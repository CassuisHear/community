package com.whut.community.service;

import com.whut.community.dao.CommentMapper;
import com.whut.community.entity.Comment;
import com.whut.community.util.CommunityConstant;
import com.whut.community.util.SensitiveFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.util.List;

@Service
public class CommentService implements CommunityConstant {

    private CommentMapper commentMapper;

    private SensitiveFilter sensitiveFilter;

    private DiscussPostService discussPostService;

    @Autowired
    public CommentService(CommentMapper commentMapper,
                          SensitiveFilter sensitiveFilter,
                          DiscussPostService discussPostService) {
        this.commentMapper = commentMapper;
        this.sensitiveFilter = sensitiveFilter;
        this.discussPostService = discussPostService;
    }

    public List<Comment> findCommentsByEntity(int entityType, int entityId, int offset, int limit) {
        return commentMapper.selectCommentsByEntity(entityType, entityId, offset, limit);
    }

    public int findCommentCountByEntity(int entityType, int entityId) {
        return commentMapper.selectCountByEntity(entityType, entityId);
    }

    /*
        新添一条评论
        这里对数据库进行了两次操作，
        所以需要使用事务管理，以便保证数据的正确性
     */
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public int addComment(Comment comment) {

        if (comment == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }

        // 对评论的内容进行 HTML 转义 和 敏感词过滤
        comment.setContent(HtmlUtils.htmlEscape(comment.getContent()));
        comment.setContent(sensitiveFilter.filter(comment.getContent()));
        // 将该评论插入到数据库中，如果操作成功则 rows 为1
        int rows = commentMapper.insertComment(comment);

        // 如果这个评论是 针对帖子 而 不是针对评论 的，
        // 那么应该使用 discussPostService 更新该帖子的评论数量
        if (ENTITY_TYPE_POST == comment.getEntityType()) {
            // 获取该帖子的 id
            int discussPostId = comment.getEntityId();
            // 获取该帖子的评论数量
            int commentCount = commentMapper.selectCountByEntity(ENTITY_TYPE_POST, discussPostId);
            // 更新该帖子的评论数量
            discussPostService.updateCommentCount(discussPostId, commentCount);
        }

        return rows;
    }

    // 根据 userId 查询评论数量(仅查询针对帖子的评论数量)
    public int findCommentCountByUserId(int userId) {
        return commentMapper.selectCommentCountByUserId(userId);
    }

    // 根据 userId 和 分页数据查询帖子集合(仅查询针对帖子的评论)
    public List<Comment> findCommentsByUserId(int userId, int offset, int limit) {
        return commentMapper.selectCommentByUserId(userId, offset, limit);
    }

    public Comment findCommentById(int id) {
        return commentMapper.selectCommentById(id);
    }

}
