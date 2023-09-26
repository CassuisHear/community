package com.whut.community.quartz;

import com.whut.community.entity.DiscussPost;
import com.whut.community.service.DiscussPostService;
import com.whut.community.service.ElasticsearchService;
import com.whut.community.service.LikeService;
import com.whut.community.util.CommunityConstant;
import com.whut.community.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);

    private RedisTemplate<String, Object> redisTemplate;

    private DiscussPostService discussPostService;

    private LikeService likeService;

    private ElasticsearchService elasticsearchService;

    // 初始纪元，即网站创建的时间
    private static final Date epoch;

    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                    .parse("2016-06-07 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException("初始化纪元时间失败: ", e);
        }
    }

    public PostScoreRefreshJob(RedisTemplate<String, Object> redisTemplate,
                               DiscussPostService discussPostService,
                               LikeService likeService,
                               ElasticsearchService elasticsearchService) {
        this.redisTemplate = redisTemplate;
        this.discussPostService = discussPostService;
        this.likeService = likeService;
        this.elasticsearchService = elasticsearchService;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // 1.从 Redis 中取出所有需要重新计算分数的 postId
        String redisKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations<String, Object> setOperations = redisTemplate.boundSetOps(redisKey);

        // 空值判断
        if (setOperations == null || setOperations.size() == null || setOperations.size() == 0) {
            logger.info("[任务取消] 没有需要刷新的帖子!");
            return;
        }

        // 2.执行刷新的批处理操作
        logger.info("[任务开始] 正在刷新帖子分数: " + setOperations.size());

        while (setOperations.size() > 0) {
            this.refresh((Integer) setOperations.pop());
        }

        logger.info("[任务结束] 帖子分数刷新完毕!");
    }

    /**
     * 刷新帖子分数
     *
     * @param postId 帖子id
     */
    private void refresh(int postId) {
        // 1.查询帖子对象
        DiscussPost discussPost = discussPostService.findDiscussPostById(postId);
        if (discussPost == null || discussPost.getStatus() == 2) {
            logger.error("该帖子不存在或者已被删除: id = " + postId);
            return;
        }

        // 2.计算分数，先查询对应的各个因素
        // 2.1是否精华
        boolean isWonderful = discussPost.getStatus() == 1;
        // 2.2评论数
        Integer commentCount = discussPost.getCommentCount();
        // 2.3点赞数量
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);
        // 2.4使用公式计算分数，即 权重 + 日期差
        // 权重
        double weight = 1.0 * (isWonderful ? 75 : 0) + commentCount * 10 + likeCount * 2;
        // 日期差(以“天”为单位)
        double dateDiff = 1.0 * (discussPost.getCreateTime().getTime() - epoch.getTime()) / (1000 * 3600 * 24);
        double score = Math.log10((Math.max(weight, 1))) + dateDiff;

        // 3.更新帖子分数
        discussPostService.updateScore(postId, score);

        // 4.因为可能是新增评论导致帖子分数改变，
        // 所以要在 ES 中同步地更新帖子的相关信息
        discussPost.setScore(score);
        elasticsearchService.saveDiscussPost(discussPost);
    }
}
