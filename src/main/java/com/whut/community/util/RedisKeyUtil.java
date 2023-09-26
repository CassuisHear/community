package com.whut.community.util;

public class RedisKeyUtil {

    // Redis 中，key 的分隔符
    private static final String SPLIT = ":";

    // 实体类型(帖子或评论)，“赞”的前缀
    private static final String PREFIX_ENTITY_LIKE = "like:entity";

    // 用户，“赞”的前缀
    private static final String PREFIX_USER_LIKE = "like:user";

    // 关注功能中，目标 和 粉丝 的前缀
    private static final String PREFIX_FOLLOWEE = "followee";
    private static final String PREFIX_FOLLOWER = "follower";

    // 验证码的前缀
    private static final String PREFIX_KAPTCHA = "kaptcha";

    // 登录凭证的前缀
    private static final String PREFIX_TICKET = "ticket";

    // 用户id的前缀
    private static final String PREFIX_USER = "user";

    // 独立访客的前缀
    private static final String PREFIX_UV = "uv";

    // 日活用户的前缀
    private static final String PREFIX_DAU = "dau";

    // 帖子前缀
    private static final String PREFIX_POST = "post";

    // 获取某个实体的赞在 Redis 中对应的 key，格式为：
    // like:entity:entityType:entityId -> set(userId)
    // 这里的 userId 是点赞者的 id，方便以后查询点赞的人
    public static String getEntityLikeKey(int entityType, int entityId) {
        return PREFIX_ENTITY_LIKE + SPLIT + entityType + SPLIT + entityId;
    }

    // 某个用户获得的赞在 Redis 中对应的 key，格式为：
    // like:user:userId -> string
    public static String getUserLikeKey(int userId) {
        return PREFIX_USER_LIKE + SPLIT + userId;
    }

    // 某个用户所关注的 目标 在 Redis 中对应的 key，格式为：
    // followee:userId:entityType -> zset(entityId, nowTime)
    /*
        userId: 粉丝用户的 id
        entityType: 关注目标的实体类型(用户、帖子或者评论)
        entityId: 关注目标的实体id
        nowTime: 当前时间
     */
    public static String getFolloweeKey(int userId, int entityType) {
        return PREFIX_FOLLOWEE + SPLIT + userId + SPLIT + entityType;
    }

    // 某个实体的 粉丝 在 Redis 中对应的 key，格式为：
    // follower:entityType:entityId -> zset(userId, nowTime)
    // 各字段含义与上同
    public static String getFollowerKey(int entityType, int entityId) {
        return PREFIX_FOLLOWER + SPLIT + entityType + SPLIT + entityId;
    }

    // 登录验证码的 key，格式为：
    // kaptcha:owner
    // 其中 owner 是服务器随机生成的字符串，用于临时唯一标识某个用户，
    // 会存放到 Cookie 中，交给浏览器
    public static String getKaptchaKey(String owner) {
        return PREFIX_KAPTCHA + SPLIT + owner;
    }

    // 登录凭证的 key，格式为：
    // ticket:'ticket'
    // 其中'ticket'是随机字符串，用作登录凭证
    public static String getTicketKey(String ticket) {
        return PREFIX_TICKET + SPLIT + ticket;
    }

    // 用户id的key，格式为：
    // user:userId
    public static String getUserKey(int userId) {
        return PREFIX_USER + SPLIT + userId;
    }

    // 单日 UV，格式为：
    // uv:date
    public static String getUVKey(String date) {
        return PREFIX_UV + SPLIT + date;
    }

    // 某个区间内的 UV，格式为：
    // uv:startDate:endDate
    public static String getUVKey(String startDate, String endDate) {
        return PREFIX_UV + SPLIT + startDate + SPLIT + endDate;
    }

    // 单日活跃用户，格式为：
    // dau:date
    public static String getDAUKey(String date) {
        return PREFIX_DAU + SPLIT + date;
    }

    // 某个区间内的 活跃用户，格式为：
    // dau:startDate:endDate
    public static String getDAUKey(String startDate, String endDate) {
        return PREFIX_DAU + SPLIT + startDate + SPLIT + endDate;
    }

    // 帖子分数变化时的 key(对应于多个帖子)，格式为：
    // post:score
    public static String getPostScoreKey() {
        return PREFIX_POST + SPLIT + "score";
    }
}
