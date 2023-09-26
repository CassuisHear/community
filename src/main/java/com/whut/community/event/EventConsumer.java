package com.whut.community.event;

import com.alibaba.fastjson.JSONObject;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import com.whut.community.entity.DiscussPost;
import com.whut.community.entity.Event;
import com.whut.community.entity.Message;
import com.whut.community.service.DiscussPostService;
import com.whut.community.service.ElasticsearchService;
import com.whut.community.service.MessageService;
import com.whut.community.util.CommunityConstant;
import com.whut.community.util.CommunityUtil;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Component
public class EventConsumer implements CommunityConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    private MessageService messageService;

    private DiscussPostService discussPostService;

    private ElasticsearchService elasticService;

    @Value("${wk.image.command}")
    private String wkImageCommand;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${qiniu.key.access}")
    private String accessKey;

    @Value("${qiniu.key.secret}")
    private String secretKey;

    @Value("${qiniu.bucket.share.name}")
    private String shareBucketName;

    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    public EventConsumer(MessageService messageService,
                         DiscussPostService discussPostService,
                         ElasticsearchService elasticService,
                         ThreadPoolTaskScheduler taskScheduler) {
        this.messageService = messageService;
        this.discussPostService = discussPostService;
        this.elasticService = elasticService;
        this.taskScheduler = taskScheduler;
    }

    // 消费 评论、点赞、关注事件
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record) {
        // 1.空值处理
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        // 2.将消息(JSON 格式的字符串)转化为一个 Event 对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息的格式错误！");
            return;
        }

        // 3.发送站内通知(构造一个 Message 对象)
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID); // from_id
        message.setToId(event.getEntityUserId()); // to_id
        message.setConversationId(event.getTopic()); // conversation_id
        // 消息的状态不用设置，默认为0有效状态 // status
        message.setCreateTime(new Date()); // create_time
        // 设置消息的内容 content，
        // 这里将内容设置为事件发生的实体对象(发生在帖子、评论或者用户上面)，
        // 将这个实体对象转化为JSON字符串即可，方便查询各种数据
        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId()); // 事件触发者 id
        content.put("entityType", event.getEntityType()); // 实体类型
        content.put("entityId", event.getEntityId()); // 实体 id
        // 其他数据
        Map<String, Object> data = event.getData();
        if (!data.isEmpty()) {
            for (String key : data.keySet()) {
                content.put(key, data.get(key));
            }
        }
        message.setContent(JSONObject.toJSONString(content)); // content

        // 4.将这个 Message 对象添加到 MySQL 数据库
        messageService.addMessage(message);
    }

    // 消费 发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record) {
        // 1.空值处理
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        // 2.将消息(JSON 格式的字符串)转化为一个 Event 对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息的格式错误！");
            return;
        }

        // 3.消息非空时，从 event 中获取帖子id，
        // 查询到这个帖子之后将其存储到 ES 中即可
        DiscussPost discussPost =
                discussPostService.findDiscussPostById(event.getEntityId());
        elasticService.saveDiscussPost(discussPost);
    }

    // 消费 删帖事件
    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record) {
        // 1.空值处理
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        // 2.将消息(JSON 格式的字符串)转化为一个 Event 对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息的格式错误！");
            return;
        }

        // 3.消息非空时，从 event 中获取帖子id，
        // 根据这个id在 ES 中删除这个帖子即可
        elasticService.deleteDiscussPost(event.getEntityId());
    }

    // 消费 分享事件
    @KafkaListener(topics = TOPIC_SHARE)
    public void handleShareEvent(ConsumerRecord record) {
        // 1.空值处理
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空！");
            return;
        }

        // 2.将消息(JSON 格式的字符串)转化为一个 Event 对象
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if (event == null) {
            logger.error("消息的格式错误！");
            return;
        }

        // 3.处理分享事件
        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String) event.getData().get("fileName");
        String suffix = (String) event.getData().get("suffix");
        // 命令，格式为：
        // E:/wkhtmltox/wkhtmltopdf/bin/wkhtmltoimage --quality 75
        // https://www.nowcoder.com E:/work/data/wk-images/nowcoder.png
        String cmd = wkImageCommand + " --quality 75 "
                + htmlUrl + " " + wkImageStorage + "/" + fileName + suffix;
        try {
            // 执行命令，在本地生成文件
            Runtime.getRuntime().exec(cmd);
            logger.info("生成长图成功: " + cmd);
        } catch (IOException e) {
            logger.error("生成长图失败: " + e.getMessage());
        }

        // 为了避免第188行代码执行命令时的速度过慢(执行命令的线程和当前线程是并行的)，
        // 在这里设置一个定时器，不断监视文件是否已经生成，
        // 一旦分享的图片文件生成了，就把这个文件上传至七牛云服务器
        UploadTask uploadTask = new UploadTask(fileName, suffix);
        Future future = taskScheduler.scheduleAtFixedRate(uploadTask, 5000);
        uploadTask.setFuture(future);
    }

    // 执行上传任务(上传至七牛云服务器)
    class UploadTask implements Runnable {

        // 文件名称
        private String fileName;

        // 文件后缀
        private String suffix;

        // 启动任务的返回值，用来停止计时器
        private Future future;

        /*
            以下两个属性用来处理极端情况(文件生成失败、文件上传失败等)：
            startTime：用来记录开始上传的时间，一旦上传的时间间隔超过了30秒，
            默认上传失败，停止定时任务；
            uploadTimes：用来记录上传次数，一旦上传次数超过了3次，
            默认上传失败，停止定时任务；
         */
        // 开始时间
        private long startTime;
        // 上传次数
        private int uploadTimes;

        public UploadTask(String fileName, String suffix) {
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
            this.uploadTimes = 0;
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            // 1.生成图片失败，停止定时任务
            if (System.currentTimeMillis() - startTime > 30000) {
                logger.error("上传文件时间过长，中止任务: " + fileName);
                future.cancel(true);
                return;
            }

            // 2.上传文件失败，停止定时任务
            if (uploadTimes >= 3) {
                logger.error("上传次数过多，中止任务: " + fileName);
                future.cancel(true);
                return;
            }

            // 3.找到本地存储的这个文件，进一步处理上传逻辑
            String path = wkImageStorage + "/" + fileName + suffix;
            File file = new File(path);
            if (file.exists()) {
                logger.info(String.format("开始第%d次上传[%s].", ++uploadTimes, fileName));

                // 3.1设置响应信息(希望七牛云服务器返回的结果)
                StringMap policy = new StringMap();
                policy.put("returnBody", CommunityUtil.getJSONString(0));

                // 3.2生成上传凭证
                Auth auth = Auth.create(accessKey, secretKey);
                // 进入 shareBucketName 空间，
                // 上传文件名为 fileName，凭证过期时间为 3600 秒，
                // 期望返回结果是 policy
                String uploadToken = auth.uploadToken(shareBucketName, fileName, 3600L, policy);

                // 3.3指定上传的机房(七牛云中的配置)
                UploadManager uploadManager = new UploadManager(new Configuration(Zone.zone1()));
                try {
                    // 开始上传文件(七牛云中的配置)
                    Response response = uploadManager.put(
                            path, fileName, uploadToken, null,
                            "image/" + suffix.substring(suffix.lastIndexOf(".") + 1), false
                    );
                    // 处理上传结果
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                    } else { // 上传成功，停止定时任务
                        logger.info(String.format("第%d次上传成功[%s].", uploadTimes, fileName));
                        future.cancel(true);
                    }
                } catch (QiniuException e) {
                    logger.info(String.format("第%d次上传失败[%s].", uploadTimes, fileName));
                }
            } else { // 本地文件不存在
                logger.info(String.format("等待图片生成[%s].", fileName));
            }
        }
    }

    // 定时方法，清理用户生成的长图，第一次延迟 3分钟 执行，
    // 之后每隔 4分钟 执行一次(清理1分钟之前本地创建的图片)
    // 每台主机都可以执行这个任务，因此就用 Spring 定时任务线程池
    @Scheduled(initialDelay = 1000 * 180, fixedRate = 1000 * 240)
    public void clearShareImage() {
        logger.info("[任务开始] 清理服务器上用户分享生成的长图...");
        File dir = new File(wkImageStorage);
        if (dir.listFiles() != null && dir.listFiles().length > 0) {
            for (File file : dir.listFiles()) {
                try {
                    Path path = Paths.get(file.getAbsolutePath());
                    FileTime creationTime = Files.readAttributes(path, BasicFileAttributes.class).creationTime();
                    if (System.currentTimeMillis() - creationTime.toMillis() >= 60 * 1000) {
                        boolean hasDeleted = file.delete();
                        if (hasDeleted) {
                            logger.info("[删除成功] 文件[{}]已删除！", file.getName());
                        } else {
                            logger.error("[删除失败] 文件[{}]未删除！", file.getName());
                        }
                    }
                } catch (IOException e) {
                    logger.error("[删除失败] 删除文件[{}]出现异常！", file.getName());
                    logger.error(e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        logger.info("[任务完成] 用户长图清理结束！");
    }
}
