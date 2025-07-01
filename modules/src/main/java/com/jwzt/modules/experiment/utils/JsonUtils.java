package com.jwzt.modules.experiment.utils;

import com.alibaba.fastjson2.JSONObject;

public class JsonUtils {
    public static String toJson(Object obj) {
        return JSONObject.toJSONString(obj);
    }

    public static JSONObject loadJson(String filePath) {
        try {
            // 1. 读取文件为字符串
            String jsonContent = FileUtils.readFileAsString(filePath);

            // 2. 使用 JsonUtils 将 JSON 字符串转换为 LocationPoint 对象
            return JSONObject.parseObject(jsonContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
