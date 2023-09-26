package com.whut.community.service;

import com.whut.community.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Service;

@Service
public class LikeService {

    private RedisTemplate<String, Object> redisTemplate;

    // 从容器中注入 redisTemplate(已在 RedisConfig 配置类中添加)，
    // 相当于之前的 xxxMapper，之后对数据库的操作都由 redisTemplate 来完成
    @Autowired
    public LikeService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 点赞功能的业务实现
     *
     * @param userId       点赞者的id
     * @param entityType   被点赞的实体类型(帖子或者评论)
     * @param entityId     被点赞的实体id
     * @param entityUserId 被点赞实体(帖子或者评论)对应的用户id
     */
    public void like(int userId, int entityType, int entityId, int entityUserId) {
        // 由于会对 Redis 数据库进行多次操作，这里使用事务操作
        redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {

                // 1.获取实体数据和实体数据对应user的 key
                String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);
                String userLikeKey = RedisKeyUtil.getUserLikeKey(entityUserId);

                // 2.获取操作对象
                SetOperations<String, Object> setOperations = operations.opsForSet();
                ValueOperations<String, Object> valueOperations = operations.opsForValue();

                // 3.在事务开启前，获取 Redis 对 Set 类型的操作对象，判断 userId 是否在对应的集合中
                Boolean isHasLiked = setOperations.isMember(entityLikeKey, userId); // 是否已经点赞

                // 开启事务
                operations.multi();
                // 4.根据是否已点赞进行不同操作
                if (isHasLiked != null) {
                    if (isHasLiked) { // 已点赞，取消点赞
                        setOperations.remove(entityLikeKey, userId);
                        valueOperations.decrement(userLikeKey);
                    } else { // 未点赞，进行点赞
                        setOperations.add(entityLikeKey, userId);
                        valueOperations.increment(userLikeKey);
                    }
                }
                // 提交事务
                return operations.exec();
            }
        });
    }

    /**
     * 查询某个实体被点赞的数量
     *
     * @param entityType 实体类型
     * @param entityId   实体id
     * @return 被点赞的数量
     */
    public long findEntityLikeCount(int entityType, int entityId) {
        // 1.获取数据的 key
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);

        // 2.获取 Redis 对 Set 类型的操作对象，计算并返回集合的大小即可
        Long likeCount = redisTemplate.opsForSet().size(entityLikeKey);
        return likeCount != null ? likeCount : 0L; // 操作失败时返回0
    }

    /**
     * 查询某用户对某个实体的点赞状态
     *
     * @param userId     某用户的id
     * @param entityType 实体类型
     * @param entityId   实体id
     * @return 1-该用户对该实体点了赞；0-该用户对该实体没有点赞
     */
    public int findEntityLikeStatus(int userId, int entityType, int entityId) {
        // 1.获取数据的 key
        String entityLikeKey = RedisKeyUtil.getEntityLikeKey(entityType, entityId);

        // 2.获取 Redis 对 Set 类型的操作对象，判断该用户是否在集合中
        Boolean isLiked = redisTemplate.opsForSet().isMember(entityLikeKey, userId);
        if (isLiked != null) {
            return isLiked ? 1 : 0;
        } else { // 操作失败，判断为没有对该实体点赞
            return 0;
        }
    }

    /**
     * 查询某个用户获得的赞的数量
     *
     * @param userId 用户 id
     * @return 这个用户获得的赞的数量
     */
    public int findUserLikeCount(int userId) {
        // 1.获取这个用户在 Redis 中的 key
        String userLikeKey = RedisKeyUtil.getUserLikeKey(userId);

        // 2.获取操作对象
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        // 3.获取并返回这个用户获得的赞的数量
        Integer userLikeCount = (Integer) valueOperations.get(userLikeKey);
        return userLikeCount == null ? 0 : userLikeCount;
    }
}
