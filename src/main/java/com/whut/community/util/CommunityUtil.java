package com.whut.community.util;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.Map;
import java.util.UUID;

public class CommunityUtil {

    //生成随机字符串
    public static String generateUUID() {
        //去除字符串中的 '-'
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    //MD5 加密
    public static String md5(String key) {
        //字符串为空(字符串对象引用为空、长度为0或者只含有空格)时返回 null
        if (StringUtils.isBlank(key)) {
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

    // 将各种数据类型转化为JSON字符串，code为0表示查询没有问题，为1表示存在某些问题
    public static String getJSONString(int code, String msg, Map<String, Object> map) {
        JSONObject json = new JSONObject();
        json.put("code", code);
        if (!StringUtils.isBlank(msg)) {
            json.put("msg", msg);
        }
        if (map != null) {
            for (String key : map.keySet()) {
                json.put(key, map.get(key));
            }
        }

        return json.toJSONString();
    }

    public static String getJSONString(int code, String msg) {
        return getJSONString(code, msg, null);
    }

    public static String getJSONString(int code) {
        return getJSONString(code, null, null);
    }
}
