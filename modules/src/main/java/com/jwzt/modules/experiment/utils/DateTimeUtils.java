package com.jwzt.modules.experiment.utils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class DateTimeUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    // 常用时间格式，可以按需扩展
    private static final List<String> DATE_PATTERNS = Arrays.asList(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss",  // ISO 风格
            "yyyyMMdd HHmmss"        // 紧凑格式
    );

    public static LocalDateTime str2DateTime(String dateTimeStr) {
        for (String pattern : DATE_PATTERNS) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDateTime.parse(dateTimeStr, formatter);
            } catch (DateTimeParseException ignored) {
                // 尝试下一个格式
            }
        }
        throw new IllegalArgumentException("无法解析时间字符串: " + dateTimeStr);
    }

    /**
     * LocalDateTime -> 字符串
     */
    public static String localDateTime2String(LocalDateTime dateTime) {
        return dateTime.format(FORMATTER);
    }

    /**
     * Date -> 字符串
     */
    public static String Date2String(Date date) {
        LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return ldt.format(FORMATTER);
    }

    /**
     * 在指定时间字符串上增加指定秒数
     * @param str 原始时间字符串，例如 "2025-07-24 08:36:42:000"
     * @param seconds 要增加的秒数
     * @return 增加后的时间字符串，格式保持为 "yyyy-MM-dd HH:mm:ss:SSS"
     * @throws Exception
     */
    public static String addSeconds(String str, int seconds){
        // 替换最后一个冒号为点，方便SimpleDateFormat解析
        String formattedStr = str.replaceFirst(":(\\d{3})$", ".$1");
        Date date = null;
        // 解析时间
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        try {
            date = sdf.parse(formattedStr);
        }catch (Exception e){
            date = new Date();
        }

        // 使用 Calendar 增加秒数
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.SECOND, seconds);

        // 再转回原始格式
        String result = sdf.format(cal.getTime()).replace(".", ":");
        return result;
    }

    /**
     * 计算两个时间戳之间的时间差
     * @param timestamp1 时间戳1
     * @param timestamp2 时间戳2
     * @return 时间差，格式为 "天 时:分:秒.毫秒"
     */

    public static String calculateTimeDifference(long timestamp1, long timestamp2) {
        long diffMillis = Math.abs(timestamp2 - timestamp1);
        Duration duration = Duration.ofMillis(diffMillis);

        long totalMillis = duration.toMillis();
        long days = totalMillis / (24 * 60 * 60 * 1000);
        long hours = (totalMillis / (60 * 60 * 1000)) % 24;
        long minutes = (totalMillis / (60 * 1000)) % 60;
        long seconds = (totalMillis / 1000) % 60;
        long millis = totalMillis % 1000;

        StringBuilder result = new StringBuilder();
        if (days > 0) {
            result.append(days).append("天");
        }
        if (hours > 0 || days > 0) {
            result.append(hours).append("小时");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            result.append(minutes).append("分");
        }
        if (seconds > 0 || minutes > 0 || hours > 0 || days > 0) {
            result.append(seconds).append("秒");
        }
        result.append(millis).append("毫秒");

        return result.toString();
    }

    /**
     * 将时间戳转换为日期时间字符串
     * @param dateTimeStr 时间字符串
     * @return 日期时间字符串
     */
    public static long convertToTimestamp(String dateTimeStr) {
        // 先尝试修复错误格式（比如形如 2025-07-05 09:34:14:000）
        if (dateTimeStr == null) {
            throw new IllegalArgumentException("时间字符串不能为空");
        }
        String normalized = dateTimeStr.trim();
        int lastColonIndex = normalized.lastIndexOf(':');
        if (lastColonIndex > 18) { // 确保是毫秒部分的冒号
            normalized = normalized.substring(0, lastColonIndex) + "." + normalized.substring(lastColonIndex + 1);
        }

        // 统一毫秒位数（截断或补齐至 3 位），便于使用 yyyy-MM-dd HH:mm:ss.SSS 解析
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < normalized.length() - 1) {
            String fractional = normalized.substring(dotIndex + 1);
            // 提取连续数字，忽略潜在的尾随字符（如时区标记）
            int digitCount = 0;
            while (digitCount < fractional.length() && Character.isDigit(fractional.charAt(digitCount))) {
                digitCount++;
            }
            String digits = fractional.substring(0, digitCount);
            if (!digits.isEmpty()) {
                if (digits.length() > 3) {
                    digits = digits.substring(0, 3);
                } else if (digits.length() < 3) {
                    digits = String.format("%-3s", digits).replace(' ', '0');
                }
                normalized = normalized.substring(0, dotIndex + 1) + digits + fractional.substring(digitCount);
            }
        }

        // 定义多个可能的日期格式
        DateTimeFormatter formatterWithMillis = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        DateTimeFormatter formatterWithoutMillis = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime localDateTime;

        // 尝试使用带毫秒格式解析
        try {
            localDateTime = LocalDateTime.parse(normalized, formatterWithMillis);
        } catch (Exception e) {
            // 若失败则尝试使用不带毫秒格式解析
            localDateTime = LocalDateTime.parse(normalized, formatterWithoutMillis);
        }

        // 转换为时间戳（毫秒）
        return localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    /**
     *  时间戳转日期时间字符串
     * @param timestamp
     * @return
     */
    public static String timestampToDateTimeStr(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

    /**
     * 将带有毫秒/微秒的小数秒时间字符串转换为不带小数秒的时间字符串
     * 支持格式：
     *  - yyyy-MM-dd HH:mm:ss.SSS
     *  - yyyy-MM-dd HH:mm:ss.SSSSSS（会截断为毫秒）
     *  - 以及 convertToTimestamp 可解析的其他变体（如最后一段为 :SSS）
     *
     * 解析失败时返回 null，不抛出异常
     *
     * @param dateTimeStr 原始时间字符串
     * @return 不带小数秒的时间字符串（yyyy-MM-dd HH:mm:ss），或解析失败时返回 null
     */
    public static String dateTimeSSSStrToDateTimeStr(String dateTimeStr) {
        if (dateTimeStr == null) {
            return null;
        }
        String normalized = dateTimeStr.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        try {
            long timestamp = convertToTimestamp(normalized);
            return timestampToDateTimeStr(timestamp);
        } catch (Exception e) {
            // 解析失败时按约定返回 null，而不是向外抛出异常，避免影响现有调用逻辑
            return null;
        }
    }

    /**
     *  时间戳转日期时间字符串(yyyy-MM-dd HH:mm:ss:SSS)
     * @param timestamp
     * @return
     */
    public static String timestampToDateTimeSSSStr(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS");
        return sdf.format(new Date(timestamp));
    }

    /**
     *  时间戳转日期字符串
     * @param timestamp
     * @return
     */
    public static String timestampToDateStr(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date(timestamp));
    }

    /**
     * 格式化时间显示
     * @param seconds
     * @return
     */
    public static String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + " 秒";
        } else if (seconds < 3600) {
            return (seconds / 60) + " 分 " + (seconds % 60) + " 秒";
        } else {
            return (seconds / 3600) + " 小时 " + ((seconds % 3600) / 60) + " 分 " + (seconds % 60) + " 秒";
        }
    }
}
