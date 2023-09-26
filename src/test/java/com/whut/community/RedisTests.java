package com.whut.community;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class RedisTests {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    public void testStrings() {
        String redisKey = "test:count";
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();
        valueOperations.set(redisKey, 1);
        System.out.println(valueOperations.get(redisKey));
        System.out.println(valueOperations.increment(redisKey));
        System.out.println(valueOperations.decrement(redisKey));
    }

    @Test
    public void testHashes() {
        String redisKey = "test:user";
        HashOperations<String, Object, Object> hashOperations = redisTemplate.opsForHash();
        hashOperations.put(redisKey, "id", 1);
        hashOperations.put(redisKey, "username", "Tom");
        System.out.println(hashOperations.get(redisKey, "id"));
        System.out.println(hashOperations.get(redisKey, "username"));
    }

    @Test
    public void testLists() {
        String redisKey = "test:ids";
        ListOperations<String, Object> listOperations = redisTemplate.opsForList();
        listOperations.leftPush(redisKey, "101");
        listOperations.leftPush(redisKey, "102");
        listOperations.leftPush(redisKey, "103");
        System.out.println(listOperations.size(redisKey));
        System.out.println(listOperations.index(redisKey, 1));
        System.out.println(listOperations.range(redisKey, 0, 1));
        System.out.println(listOperations.leftPop(redisKey));
        System.out.println(listOperations.leftPop(redisKey));
        System.out.println(listOperations.size(redisKey));
    }

    @Test
    public void testSets() {
        String redisKey = "test:teachers";
        SetOperations<String, Object> setOperations = redisTemplate.opsForSet();
        setOperations.add(redisKey, "Green", "Blue", "Pink", "Yellow");
        System.out.println(setOperations.size(redisKey));
        System.out.println(setOperations.pop(redisKey));
        System.out.println(setOperations.members(redisKey));
    }

    @Test
    public void testSortedSets() {
        String redisKey = "test:students";
        ZSetOperations<String, Object> zSetOperations = redisTemplate.opsForZSet();
        zSetOperations.add(redisKey, "aaa", 100);
        zSetOperations.add(redisKey, "bbb", 110);
        zSetOperations.add(redisKey, "ccc", 120);
        zSetOperations.add(redisKey, "ddd", 130);
        zSetOperations.add(redisKey, "eee", 140);
        System.out.println(zSetOperations.zCard(redisKey));
        System.out.println(zSetOperations.score(redisKey, "ddd"));
        System.out.println(zSetOperations.rank(redisKey, "aaa"));
        System.out.println(zSetOperations.reverseRank(redisKey, "aaa"));
        System.out.println(zSetOperations.range(redisKey, 0, 2));
        System.out.println(zSetOperations.reverseRange(redisKey, 0, 2));
    }

    @Test
    public void testKeys() {
        redisTemplate.delete("test:user");
        System.out.println(redisTemplate.hasKey("test:user"));
        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);
    }

    // 多次访问同一个 key，可以使用绑定的方式指定 key
    @Test
    public void testBindKey() {
        String redisKey = "test:count";
        BoundValueOperations<String, Object> boundValueOperations = redisTemplate.boundValueOps(redisKey);
        // 接下来的操作不用指定 key，都是针对 "test:count" 执行的
        boundValueOperations.increment(); // 2
        boundValueOperations.increment(); // 3
        boundValueOperations.increment(); // 4
        System.out.println(boundValueOperations.get()); // 4
        boundValueOperations.decrement(); // 3
        boundValueOperations.decrement(); // 2
        System.out.println(boundValueOperations.get()); // 2
    }

    // 编程式事务
    @Test
    public void testTransactional() {
        // 在 execute() 方法中进行事务的操作
        Object res = redisTemplate.execute(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String redisKey = "test:tx";
                // 1.开启 Redis 事务
                operations.multi();

                // 2.取出对某类型数据的操作 并 对 Redis 数据库 执行事务操作
                SetOperations<String, Object> setOperations = operations.opsForSet();
                setOperations.add(redisKey, "aaa");
                setOperations.add(redisKey, "bbb");
                setOperations.add(redisKey, "ccc");
                // 下面的查询不会有结果
                System.out.println(setOperations.members(redisKey));

                // 3.提交 Redis 事务
                return operations.exec();
            }
        });
        System.out.println("res = " + res);
    }

    // 统计20万个重复数据的独立总数
    @Test
    public void testHyperLogLog() {
        String redisKey = "test:hll:01";
        HyperLogLogOperations<String, Object> operationsForHLL = redisTemplate.opsForHyperLogLog();
        for (int i = 1; i <= 100000; i++) {
            operationsForHLL.add(redisKey, i);
        }
        for (int i = 1; i <= 100000; i++) {
            operationsForHLL.add(redisKey, (int) (Math.random() * 100000 + 1));
        }

        Long size = operationsForHLL.size(redisKey);
        System.out.println("size = " + size);
    }

    // 将3组数据进行合并，
    // 再统计合并后的重复数据的独立总数
    @Test
    public void testHyperLogLogUnion() {
        HyperLogLogOperations<String, Object> operationsForHLL = redisTemplate.opsForHyperLogLog();

        String redisKey2 = "test:hll:02";
        for (int i = 1; i <= 10000; i++) {
            operationsForHLL.add(redisKey2, i);
        }
        String redisKey3 = "test:hll:03";
        for (int i = 5001; i <= 15000; i++) {
            operationsForHLL.add(redisKey3, i);
        }
        String redisKey4 = "test:hll:04";
        for (int i = 10001; i <= 20000; i++) {
            operationsForHLL.add(redisKey4, i);
        }

        String unionKey = "test:hll:union";
        operationsForHLL.union(unionKey, redisKey2, redisKey3, redisKey4);
        Long size = operationsForHLL.size(unionKey);
        System.out.println("size = " + size);
    }

    // 统计一组数据的 布尔值
    @Test
    public void testBitMap() {
        String redisKey = "test:bm:01";
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        // 记录
        valueOperations.setBit(redisKey, 1, true);
        valueOperations.setBit(redisKey, 4, true);
        valueOperations.setBit(redisKey, 7, true);

        // 查询
        System.out.println(valueOperations.getBit(redisKey, 0));
        System.out.println(valueOperations.getBit(redisKey, 1));
        System.out.println(valueOperations.getBit(redisKey, 2));

        // 统计
        Object res = redisTemplate.execute((RedisCallback<Object>) redisConnection -> {
            return redisConnection.bitCount(redisKey.getBytes());
        });
        System.out.println("res = " + res);
    }

    // 统计3组数据的 布尔值，
    // 并对这3组数据进行 or 运算
    @Test
    public void testBitMapOperation() {
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        String redisKey2 = "test:bm:02";
        valueOperations.setBit(redisKey2, 0, true);
        valueOperations.setBit(redisKey2, 1, true);
        valueOperations.setBit(redisKey2, 2, true);

        String redisKey3 = "test:bm:03";
        valueOperations.setBit(redisKey3, 2, true);
        valueOperations.setBit(redisKey3, 3, true);
        valueOperations.setBit(redisKey3, 4, true);

        String redisKey4 = "test:bm:04";
        valueOperations.setBit(redisKey4, 4, true);
        valueOperations.setBit(redisKey4, 5, true);
        valueOperations.setBit(redisKey4, 6, true);

        String redisKey = "test:bm:or";
        Object res = redisTemplate.execute((RedisCallback<Object>) redisConnection -> {
            // 对 2、3、4共三个 key 中的 bit 进行 or 运算，
            // 并将结果存储到 redisKey 中
            redisConnection.bitOp(RedisStringCommands.BitOperation.OR, redisKey.getBytes(),
                    redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes());
            // 返回 redisKey 中的 bit 计数(bit 为 true 的个数)
            return redisConnection.bitCount(redisKey.getBytes());
        });
        System.out.println("res = " + res);
        System.out.println(valueOperations.getBit(redisKey, 0));
        System.out.println(valueOperations.getBit(redisKey, 1));
        System.out.println(valueOperations.getBit(redisKey, 2));
        System.out.println(valueOperations.getBit(redisKey, 3));
        System.out.println(valueOperations.getBit(redisKey, 4));
        System.out.println(valueOperations.getBit(redisKey, 5));
        System.out.println(valueOperations.getBit(redisKey, 6));
    }
}
