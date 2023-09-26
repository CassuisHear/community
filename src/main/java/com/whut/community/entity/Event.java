package com.whut.community.entity;



import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
@NoArgsConstructor
@EqualsAndHashCode
// 事件模型(评论、点赞或者关注)
public class Event {

    // 事件的话题 topic
    private String topic;

    // 事件的发起者 useId
    private int userId;

    // 事件发生的实体对象(发生在帖子、评论还是用户上面)
    private int entityType;
    private int entityId;
    // 实体对象 所依附的 用户id
    private int entityUserId;

    // 存储其他数据，使得 Event 类具有一定的拓展性
    private Map<String, Object> data = new HashMap<>();

    public String getTopic() {
        return topic;
    }

    // 使set方法支持链式编程
    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }

    public int getUserId() {
        return userId;
    }

    public Event setUserId(int userId) {
        this.userId = userId;
        return this;
    }

    public int getEntityType() {
        return entityType;
    }

    public Event setEntityType(int entityType) {
        this.entityType = entityType;
        return this;
    }

    public int getEntityId() {
        return entityId;
    }

    public Event setEntityId(int entityId) {
        this.entityId = entityId;
        return this;
    }

    public int getEntityUserId() {
        return entityUserId;
    }

    public Event setEntityUserId(int entityUserId) {
        this.entityUserId = entityUserId;
        return this;
    }

    public Map<String, Object> getData() {
        return data;
    }

    // 将 键值对 存入到当前对象的 data 属性
    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }
}
