package com.jwzt.modules.experiment.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Date;

public class DateTimeUtils {

    public static long convertToTimestamp(String dateTimeStr) {
        // 先尝试修复错误格式（比如形如 2025-07-05 09:34:14:000）
        int lastColonIndex = dateTimeStr.lastIndexOf(':');
        if (lastColonIndex > 18) { // 确保是毫秒部分的冒号
            dateTimeStr = dateTimeStr.substring(0, lastColonIndex) + "." + dateTimeStr.substring(lastColonIndex + 1);
        }

        // 定义多个可能的日期格式
        DateTimeFormatter formatterWithMillis = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        DateTimeFormatter formatterWithoutMillis = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime localDateTime;

        // 尝试使用带毫秒格式解析
        try {
            localDateTime = LocalDateTime.parse(dateTimeStr, formatterWithMillis);
        } catch (Exception e) {
            // 若失败则尝试使用不带毫秒格式解析
            localDateTime = LocalDateTime.parse(dateTimeStr, formatterWithoutMillis);
        }

        // 转换为时间戳（毫秒）
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

//    public static long convertToTimestamp(String dateTimeStr) {
//        // 定义日期格式，根据 acceptTime 的实际格式调整
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//
//        // 解析字符串为 LocalDateTime
//        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);
//
//        // 转换为时间戳（毫秒）
//        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
//    }

    public static String timestampToDateTimeStr(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }
}
