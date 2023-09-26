package com.whut.community.service;

import com.whut.community.entity.User;
import com.whut.community.util.CommunityConstant;
import com.whut.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService implements CommunityConstant {

    private RedisTemplate<String, Object> redisTemplate;

    private UserService userService;

    @Autowired
    public FollowService(RedisTemplate<String, Object> redisTemplate,
                         UserService userService) {
        this.redisTemplate = redisTemplate;
        this.userService = userService;
    }

    /**
     * **关注** 功能
     *
     * @param userId     粉丝用户的id
     * @param entityType 关注实体的类型
     * @param entityId   关注实体的id
     */
    public void follow(int userId, int entityType, int entityId) {
        /*
            关注功能涉及两方面的操作：
            1.粉丝的关注目标增加；
            2.被关注目标的粉丝增加
            所以这里要使用 Redis 的事务操作
         */
        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {

                // 1.构造关注双方的 key
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                // 2.获取对 ZSet 的操作
                ZSetOperations<String, Object> zSetOperations = operations.opsForZSet();

                // 启动事务
                operations.multi();
                // 3.进行事务中的操作
                zSetOperations.add(followeeKey, entityId, System.currentTimeMillis()); // 粉丝角度，增加到关注目标的集合
                zSetOperations.add(followerKey, userId, System.currentTimeMillis()); // 目标角度，增加到粉丝的集合
                return operations.exec();
            }
        });
    }

    /**
     * **取关** 功能，
     * 和关注功能的操作相反
     *
     * @param userId     粉丝用户的id
     * @param entityType 关注实体的类型
     * @param entityId   关注实体的id
     */
    public void unFollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {

                // 1.构造关注双方的 key
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

                // 2.获取对 ZSet 的操作
                ZSetOperations<String, Object> zSetOperations = operations.opsForZSet();

                // 启动事务
                operations.multi();
                // 3.进行事务中的操作
                zSetOperations.remove(followeeKey, entityId); // 粉丝角度，从关注目标的集合中移除
                zSetOperations.remove(followerKey, userId); // 目标角度，从粉丝的集合中移除
                return operations.exec();
            }
        });
    }

    // 查询某个用户所关注某类实体的数量
    public long findFolloweeCount(int userId, int entityType) {
        // 1.获取在 Redis 中的 key
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);

        // 2.查询这类实体的关注数量
        Long followeeCount = redisTemplate.opsForZSet().zCard(followeeKey);
        return followeeCount == null ? 0L : followeeCount;
    }

    // 查询某个实体的粉丝数量
    public long findFollowerCount(int entityType, int entityId) {
        // 1.获取在 Redis 中的 key
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);

        // 2.查询这个实体的粉丝数量
        Long followerCount = redisTemplate.opsForZSet().zCard(followerKey);
        return followerCount == null ? 0L : followerCount;
    }

    // 查询当前用户是否关注了该实体
    public boolean hasFollowed(int userId, int entityType, int entityId) {
        // 1.获取 该用户的某类关注集合 在 Redis 中的 key
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);

        // 2.判断该用户的 关注集合 中是否含有这个实体
        Double score = redisTemplate.opsForZSet().score(followeeKey, entityId);
        return score != null; // 分数不为空表明 这类关注集合 中有这个实体
    }

    // 查询某个用户关注的人(实体类型为 ENTITY_TYPE_USER == 3)，
    // 包括 User 本体 和 关注的时间 两项，粉丝也是如此
    public List<Map<String, Object>> findFolloweeList(int userId, int offset, int limit) {
        // 1.获取该用户关注的人在 Redis 中对应的 key
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ENTITY_TYPE_USER);

        // 2.在 Redis 中将所有的 关注者 id 查出来(按照关注的时间倒序)
        ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();
        // 所有关注者的id，这里的 Set 是 Redis 实现的有序集合子类，不是无序集合
        Set<Object> followeeIdSet = zSetOperations.reverseRange(followeeKey, offset, offset + limit - 1);
        if (followeeIdSet == null) {
            return null;
        }

        // 3.构造并返回 VO 对象集合
        List<Map<String, Object>> followeeVoList = new ArrayList<>();
        for (Object followeeId : followeeIdSet) {
            // 创建一个 关注者 的VO对象
            Map<String, Object> followeeVo = new HashMap<>();

            // 3.1添加这个关注者
            User followee = userService.findUserById((Integer) followeeId);
            followeeVo.put("followee", followee);

            // 3.2添加关注者被 userId 这个用户关注的时间
            Double score = zSetOperations.score(followeeKey, followeeId);
            Date followedTime = score != null ? new Date(score.longValue()) : new Date(); // score 为空时添加的是当前时间
            followeeVo.put("followedTime", followedTime);

            // 添加这个 VO 对象
            followeeVoList.add(followeeVo);
        }
        return followeeVoList;
    }

    // 查询某个用户的粉丝
    public List<Map<String, Object>> findFollowerList(int userId, int offset, int limit) {
        // 1.获取该用户关注的人在 Redis 中对应的 key
        String followerKey = RedisKeyUtil.getFollowerKey(ENTITY_TYPE_USER, userId);

        // 2.在 Redis 中将所有的 粉丝 id 查出来(按照关注的时间倒序)
        ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();
        Set<Object> followerIdSet = zSetOperations.reverseRange(followerKey, offset, offset + limit - 1);
        if (followerIdSet == null) {
            return null;
        }

        // 3.构造并返回 VO 对象集合
        List<Map<String, Object>> followerVoList = new ArrayList<>();
        for (Object followerId : followerIdSet) {
            // 创建一个 粉丝 的VO对象
            Map<String, Object> followerVo = new HashMap<>();

            // 3.1添加这个粉丝
            User follower = userService.findUserById((Integer) followerId);
            followerVo.put("follower", follower);

            // 3.2添加粉丝关注 userId 这个用户的时间
            Double score = zSetOperations.score(followerKey, followerId);
            Date followedTime = score != null ? new Date(score.longValue()) : new Date(); // score 为空时添加的是当前时间
            followerVo.put("followedTime", followedTime);

            // 添加这个 VO 对象
            followerVoList.add(followerVo);
        }

        return followerVoList;
    }
}
