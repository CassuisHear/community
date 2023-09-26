package com.whut.community.dao;

import com.whut.community.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository("commentMapper")
public interface CommentMapper {

    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);

    int selectCountByEntity(int entityType, int entityId);

    int insertComment(Comment comment);

    int selectCommentCountByUserId(int userId);

    List<Comment> selectCommentByUserId(int userId, int offset, int limit);

    Comment selectCommentById(int id);
}
