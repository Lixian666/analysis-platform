package com.jwzt.modules.experiment.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.util.Date;

public class DateTimeUtils {
    public static long convertToTimestamp(String dateTimeStr) {
        // 定义日期格式，根据 acceptTime 的实际格式调整
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // 解析字符串为 LocalDateTime
        LocalDateTime localDateTime = LocalDateTime.parse(dateTimeStr, formatter);

        // 转换为时间戳（毫秒）
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public static String timestampToDateTimeStr(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }
}
