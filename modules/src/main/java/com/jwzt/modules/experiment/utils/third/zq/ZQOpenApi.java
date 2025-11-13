package com.jwzt.modules.experiment.utils.third.zq;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.joysuch.open.api.AccessTokenApi;
import com.joysuch.open.bo.AccessTokenReq;
import com.joysuch.open.bo.UserReq;
import com.joysuch.open.utils.Md5Util;
import com.joysuch.open.vo.JoySuchResponse;
import com.joysuch.open.vo.TokenEntity;
import com.jwzt.modules.experiment.config.BaseConfig;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.Md5Utils;
import com.jwzt.modules.experiment.utils.http.HttpUtils;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubReceiveData;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubscribeResult;
import com.jwzt.modules.experiment.utils.third.zq.domain.TagScanUwbData;
import com.ruoyi.common.core.redis.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 真趣定位api
 */
@Service
public class ZQOpenApi {

    @Autowired
    private BaseConfig baseConfig;

    @Autowired
    private RedisCache redisCache;

    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private static final String TOKEN_KEY = "ZQ:TOKEN";   // token缓存key
    private static final String SIGNID_KEY = "ZQ:SIGNID"; // signId缓存key

    private static String licence = null;

    /**
     * 获取UWB信标列表
     */
    public String getListOfUWBBeacons() {
        Map<String, String> headers = getHeaders();
        headers.put("X-BuildId", baseConfig.getJoysuch().getBuildingId());
        String jsonBody = JSONObject.toJSONString(new HashMap<String, Object>() {{
            put("pageSize", 1000);
        }});
        return sendPost(baseConfig.getJoysuch().getApi().getBeacons(), headers, jsonBody);
    }

    public String getTagStateHistoryOfTagID(String buildingID, String tagID, String startTime, String endTime){
        long startTimestamp = DateTimeUtils.convertToTimestamp(startTime);
        long endTimestamp = DateTimeUtils.convertToTimestamp(endTime);
        Map<String, String> headers = getHeaders();
        if (buildingID == null){
            buildingID = baseConfig.getJoysuch().getBuildingId();
        }
        headers.put("X-BuildId", buildingID);
        String jsonBody = JSONObject.toJSONString(new HashMap<String, Object>() {{
            put("mac", tagID);
            put("startTime", startTimestamp);
            put("endTime", endTimestamp);
        }});
        String result = sendPost(baseConfig.getJoysuch().getApi().getTagScanUwbHistory(), headers, jsonBody);
        JSONObject jsonObject = JSONObject.parseObject(result);
        JSONArray data = jsonObject.getJSONArray("data");
        List<TagScanUwbData> tagScanUwbDataList = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            TagScanUwbData tag = data.getObject(i, TagScanUwbData.class);
            tag.setDateTime(DateTimeUtils.timestampToDateTimeStr(tag.getTime()));
            tagScanUwbDataList.add(tag);
        }
        JSONArray newData = new JSONArray(tagScanUwbDataList);
        jsonObject.put("data", newData);
        return jsonObject.toJSONString();
    }

    /**
     * 发送订阅请求
     *
     * 订阅类型
     * location：位置结果
     * zeroDimEnterLeave：终端进出圈事件
     * rail：围栏事件
     * area：区域事件
     * oneKeyAlarm：一键报警消息
     * bltOnOffLine：终端定位在离线消息
     * lowpower：终端低电量
     * textCmdData：终端下行结果
     * bltOnOffLineV2：终端通信离线消息
     * bltProtocolData：标签上报信息
     * dataReportingData：终端特有数据消息
     * cmdResult：终端下行cmd结果
     *
     * @param type 订阅类型
     * @param data 订阅数据
     * @return 订阅结果,
     */
    public SubscribeResult httpSubscriber(String type, SubReceiveData data){
        Map<String, String> headers = getHeaders();
        String jsonBody = JSONObject.toJSONString(new HashMap<String, Object>() {{
            put("type", type);
            put("data", data);
            put("licence", getLicence(baseConfig.getJoysuch().getUsername(), baseConfig.getJoysuch().getPassword()));
        }});
        String result = sendPost(baseConfig.getJoysuch().getApi().getSubscribe(), headers, jsonBody);
        return JSONObject.parseObject(result, SubscribeResult.class);
    }

    /**
     * 取消订阅请求
     *
     * 订阅类型
     * location：位置结果
     * zeroDimEnterLeave：终端进出圈事件
     * rail：围栏事件
     * area：区域事件
     * oneKeyAlarm：一键报警消息
     * bltOnOffLine：终端定位在离线消息
     * lowpower：终端低电量
     * textCmdData：终端下行结果
     * bltOnOffLineV2：终端通信离线消息
     * bltProtocolData：标签上报信息
     * dataReportingData：终端特有数据消息
     * cmdResult：终端下行cmd结果
     *
     * @param subscribeId 订阅数据
     * @return 取消订阅结果
     */
    public SubscribeResult httpUnSubscriber(String subscribeId){
        Map<String, String> headers = getHeaders();
        String jsonBody = JSONObject.toJSONString(new HashMap<String, Object>() {{
            put("subscribeId", subscribeId);
            put("licence", getLicence(baseConfig.getJoysuch().getUsername(), baseConfig.getJoysuch().getPassword()));
        }});
        String result = sendPost(baseConfig.getJoysuch().getApi().getUnsubscribe(), headers, jsonBody);
        return JSONObject.parseObject(result, SubscribeResult.class);
    }

    /**
     * 获取指定卡片在指定时间段内的定位点列表
     *
     * @param cardId 卡片ID
     * @param buildId 建筑ID
     * @param startTime 开始时间，格式为"yyyy-MM-dd HH:mm:ss"
     * @param endTime 结束时间，格式为"yyyy-MM-dd HH:mm:ss"
     * @return 定位点列表的JSON字符串结果
     */
    public String getListOfPoints(String cardId, String buildId, String startTime, String endTime) {
        Map<String, String> headers = getHeaders();
        headers.put("X-BuildId", buildId);
        String jsonBody = JSONObject.toJSONString(new HashMap<String, Object>() {{
            put("mac", cardId);
            put("startTime", DateTimeUtils.convertToTimestamp(startTime));
            put("endTime", DateTimeUtils.convertToTimestamp(endTime));
//            put("locationType", "real");
            put("locationType", "gps");
        }});
        return sendPost(baseConfig.getJoysuch().getApi().getPoints(), headers, jsonBody);
    }

    public JSONObject getListOfCards() {
        Map<String, String> headers = getHeaders();
        String buildResult = sendPost(baseConfig.getJoysuch().getApi().getBuildList(), headers, null);
        JSONObject resultJson = JSONObject.parseObject(buildResult);
        JSONArray dataArray = resultJson.getJSONArray("data");
        String buildId = null;
        if (dataArray != null && !dataArray.isEmpty()) {
            String firstItemStr = dataArray.getString(0);
            JSONObject firstItemJson = JSONObject.parseObject(firstItemStr);
            buildId = firstItemJson.getString("uuid");
            // 使用 buildId
        } else {
            // 处理 data 为空的情况
            throw new RuntimeException("data 数组为空，无法提取 buildId");
        }

        headers = getHeaders();
        headers.put("X-BuildId", buildId);

        String jsonBody = JSONObject.toJSONString(new HashMap<String, Object>() {{
            put("pageNum", 1);
            put("pageSize", 10);
            put("simIccid", "");
        }});

        String result = sendPost(baseConfig.getJoysuch().getApi().getCards(), headers, jsonBody);
        return JSONObject.parseObject(result);
    }

    /**
     * 核心：构建请求头，使用 Redis 缓存 token
     */
    public Map<String, String> getHeaders() {
        long timestamp = System.currentTimeMillis();

        // 从Redis取
        String accessToken = redisCache.getCacheObject(TOKEN_KEY);
        String signId = redisCache.getCacheObject(SIGNID_KEY);

        // 如果为空或已过期，重新获取
        if (accessToken == null || signId == null) {
            JoySuchResponse<TokenEntity> response = getAccessToken(baseConfig.getJoysuch().getUsername(), baseConfig.getJoysuch().getPassword());
            if (response == null || response.getData() == null) {
                throw new RuntimeException("获取AccessToken失败");
            }
            accessToken = response.getData().getAccessToken();
            signId = response.getData().getSignId();
            long expireAt = response.getData().getExpireAt();

//            // 提前60秒过期，避免临界点失效
//            long ttl = expireAt - System.currentTimeMillis() - 60 * 1000;
//            if (ttl < 0) ttl = 60 * 1000; // 最少缓存1分钟
            long ttl = 10 * 60 * 1000;
            redisCache.setCacheObject(TOKEN_KEY, accessToken, (int)(ttl/1000), TimeUnit.SECONDS);
            redisCache.setCacheObject(SIGNID_KEY, signId, (int)(ttl/1000), TimeUnit.SECONDS);
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Token", getXToken(accessToken, signId, timestamp));
        headers.put("X-Timestamp", String.valueOf(timestamp));
        headers.put("X-SignId", signId);
        headers.put("Accept-Charset", "utf-8");
        headers.put("contentType", "utf-8");
        return headers;
    }

    // X-Token生成
    public static String getXToken(String accessToken, String signId, Long timestamp){
        return Md5Utils.md5(accessToken + Md5Util.md5(signId + Md5Util.md5(accessToken)).toLowerCase() +
                timestamp).toLowerCase();
    }

    // 通用POST
    public static String sendPost(String url, Map<String, String> headers, String jsonBody) {
        PrintWriter out = null;
        BufferedReader in = null;
        StringBuilder result = new StringBuilder();
        try {
            log.info("sendPost - {}, body: {}", url, jsonBody);
            URL realUrl = new URL(url);
            URLConnection conn = realUrl.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    if (entry.getValue() != null) {
                        conn.setRequestProperty(entry.getKey(), entry.getValue());
                    }
                }
            }
            out = new PrintWriter(conn.getOutputStream());
            out.print(jsonBody);
            out.flush();
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (ConnectException e) {
            log.error("调用HttpUtils.sendPost ConnectException, url=" + url + ",param=" + jsonBody, e);
        } catch (SocketTimeoutException e) {
            log.error("调用HttpUtils.sendPost SocketTimeoutException, url=" + url + ",param=" + jsonBody, e);
        } catch (IOException e) {
            log.error("调用HttpUtils.sendPost IOException, url=" + url + ",param=" + jsonBody, e);
        } catch (Exception e) {
            log.error("调用HttpsUtil.sendPost Exception, url=" + url + ",param=" + jsonBody, e);
        } finally {
            try {
                if (out != null) out.close();
                if (in != null) in.close();
            } catch (IOException ex) {
                log.error("调用in.close Exception, url=" + url + ",param=" + jsonBody, ex);
            }
        }
        return result.toString();
    }

    public String getLicence(String username, String password) {
        UserReq userReq = UserReq.builder().username(username).password(password).build();
        licence = AccessTokenApi.of(baseConfig.getJoysuch().getBaseUrl(), "null").getLicence(userReq).getData();
        return licence;
    }

    public JoySuchResponse<TokenEntity> getAccessToken(String username, String password) {
        AccessTokenReq accessTokenReq = new AccessTokenReq(getLicence(username, password));
        return AccessTokenApi.of(baseConfig.getJoysuch().getBaseUrl(), accessTokenReq.getLicence()).refreshAccessToken();
    }

    public JoySuchResponse<TokenEntity> refreshAccessToken() {
        AccessTokenReq accessTokenReq = new AccessTokenReq(licence);
        return AccessTokenApi.of(baseConfig.getJoysuch().getBaseUrl(), accessTokenReq.getLicence()).refreshAccessToken();
    }
}
