package com.whut.community.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.whut.community.dao.DiscussPostMapper;
import com.whut.community.entity.DiscussPost;
import com.whut.community.util.SensitiveFilter;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);

    private static final String SPLIT = ":";

    private DiscussPostMapper discussPostMapper;

    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;

    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // Caffeine 核心接口：Cache；子接口：LoadingCache(同步)，AsyncLoadingCache(异步，支持并发)

    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;

    // 帖子总数缓存
    private LoadingCache<Integer, Integer> postRowsCache;

    @PostConstruct
    public void init() {
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Nullable
                    @Override
                    public List<DiscussPost> load(@NonNull String key) throws Exception { // 缓存未命中时的查询策略
                        if (StringUtils.isBlank(key)) {
                            throw new IllegalArgumentException("参数错误！");
                        }

                        // 取出分页的两个参数并查询数据库
                        String[] params = key.split(SPLIT);
                        int offset = Integer.parseInt(params[0]);
                        int limit = Integer.parseInt(params[1]);

                        // 可以在这里添加二级缓存的逻辑：Redis -> MySQL

                        logger.debug("load [post list] from DB...");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });

        // 初始化帖子总数缓存
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<>() {
                    @Nullable
                    @Override
                    public Integer load(@NonNull Integer key) throws Exception {
                        logger.debug("load [post rows] from DB...");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });
    }

    @Autowired
    public DiscussPostService(DiscussPostMapper discussPostMapper,
                              SensitiveFilter sensitiveFilter) {
        this.discussPostMapper = discussPostMapper;
        this.sensitiveFilter = sensitiveFilter;
    }

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode) {
        // 只有查询 首页的、热门的帖子时才查询缓存，
        // 即 userId = 0, orderMode = 1
        if (userId == 0 && orderMode == 1) {
            return postListCache.get(offset + SPLIT + limit);
        }

        logger.debug("load [post list] from DB...");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    public int findDiscussPostRows(int userId) {
        // 同样地，只有查询 首页的 帖子时才查询 帖子总数 缓存
        if (userId == 0) {
            return postRowsCache.get(userId);
        }

        logger.debug("load [post rows] from DB...");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost discussPost) {
        // 1.空值处理
        if (discussPost == null) {
            throw new IllegalArgumentException("参数不能为空！");
        }

        // 2.将 HTML 内容进行转义，获取转义后的内容
        String title = HtmlUtils.htmlEscape(discussPost.getTitle());
        String content = HtmlUtils.htmlEscape(discussPost.getContent());

        // 3.对标题和内容进行敏感词过滤，将过滤后的结果设置到 discussPost 对象中
        String newTitle = sensitiveFilter.filter(title);
        String newContent = sensitiveFilter.filter(content);
        discussPost.setTitle(newTitle);
        discussPost.setContent(newContent);

        // 4.将帖子插入到数据库中
        return discussPostMapper.insertDiscussPost(discussPost);
    }

    public DiscussPost findDiscussPostById(int id) {
        return discussPostMapper.selectDiscussPostById(id);
    }

    // 根据帖子的 id 和 评论数量 更新该帖子的评论数量
    public int updateCommentCount(int id, int commentCount) {
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    public int updateType(int id, int type) {
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int status) {
        return discussPostMapper.updateStatus(id, status);
    }

    public int updateScore(int id, double score) {
        return discussPostMapper.updateScore(id, score);
    }
}
