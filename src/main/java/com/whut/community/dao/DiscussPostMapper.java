package com.whut.community.dao;

import com.whut.community.entity.DiscussPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository("discussPostMapper")
public interface DiscussPostMapper {

    // orderMode-0：默认排序模式；
    // orderMode-1：按照热度排序
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode);

    //@Param 注解用于给参数取别名,
    //如果只有一个参数，并且在<if>里使用，则必须加别名
    int selectDiscussPostRows(@Param("userId") int userId);

    // 插入一条帖子
    int insertDiscussPost(DiscussPost discussPost);

    // 根据帖子 id 查询一条帖子
    DiscussPost selectDiscussPostById(int id);

    // 更新帖子的评论数量
    int updateCommentCount(int id, int commentCount);

    // 更新帖子的类型(普通 或 置顶)
    int updateType(int id, int type);

    // 更新帖子的状态(正常、精华或删除)
    int updateStatus(int id, int status);

    // 更新帖子分数
    int updateScore(int id, double score);
}
