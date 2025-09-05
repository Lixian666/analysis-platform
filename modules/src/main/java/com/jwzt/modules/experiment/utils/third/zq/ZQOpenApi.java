package com.jwzt.modules.experiment.utils.third.zq;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.joysuch.open.api.AccessTokenApi;
import com.joysuch.open.bo.AccessTokenReq;
import com.joysuch.open.bo.UserReq;
import com.joysuch.open.utils.Md5Util;
import com.joysuch.open.vo.JoySuchResponse;
import com.joysuch.open.vo.TokenEntity;
import com.jwzt.modules.experiment.config.BaseConfg;
import com.jwzt.modules.experiment.utils.DateTimeUtils;
import com.jwzt.modules.experiment.utils.Md5Utils;
import com.jwzt.modules.experiment.utils.http.HttpUtils;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubReceiveData;
import com.jwzt.modules.experiment.utils.third.zq.domain.SubscribeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;


/**
 * 真趣定位api
 */
public class ZQOpenApi {

    private static final Logger log = LoggerFactory.getLogger(HttpUtils.class);

    private static String licence = null;

    private static String accessToken = null;

    private static String signId = null;

    private static String expireAt = null;

    private static JoySuchResponse<TokenEntity> response = null;

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
     * @return 订阅结果
     */
    public static SubscribeResult httpSubscriber(String type, SubReceiveData data){
        Map<String, String> headers = getHeaders();
        String jsonBody = JSONObject.toJSONString(new HashMap<String, Object>() {{
            put("type", type);
            put("data", data);
            put("licence", getLicence(BaseConfg.USER_NAME, BaseConfg.PASSWORD));
        }});
        String result = sendPost(BaseConfg.SUBSCRIBE_URL, headers, jsonBody);
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
    public static SubscribeResult httpUnSubscriber(String subscribeId){
        Map<String, String> headers = getHeaders();
        String jsonBody = JSONObject.toJSONString(new HashMap<String, Object>() {{
            put("subscribeId", subscribeId);
            put("licence", getLicence(BaseConfg.USER_NAME, BaseConfg.PASSWORD));
        }});
        String result = sendPost(BaseConfg.UNSUBSCRIBE_URL, headers, jsonBody);
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
    public static String getListOfPoints(String cardId, String buildId, String startTime, String endTime) {
        Map<String, String> headers = getHeaders();
        headers.put("X-BuildId", buildId);
        String jsonBody = JSONObject.toJSONString(new HashMap<String, Object>() {{
            put("mac", cardId);
            put("startTime", DateTimeUtils.convertToTimestamp(startTime));
            put("endTime", DateTimeUtils.convertToTimestamp(endTime));
            put("locationType", "real");
        }});
        String buildResult = sendPost(BaseConfg.GET_POINTS_URL, headers, jsonBody);
        return buildResult;
    }

    public static JSONObject getListOfCards() {
        Map<String, String> headers = getHeaders();
        String buildResult = sendPost(BaseConfg.GET_BUILDLIST_URL, headers, null);
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

        String result = sendPost(BaseConfg.GET_CARDS_URL, headers, jsonBody);
        return JSONObject.parseObject(result);
    };

    public static Map<String, String> getHeaders() {
        long timestamp = System.currentTimeMillis();
        if (response == null || accessToken == null || signId == null || expireAt == null){
            response = getAccessToken(BaseConfg.USER_NAME, BaseConfg.PASSWORD);
            accessToken = response.getData().getAccessToken();
            signId = response.getData().getSignId();
            expireAt = String.valueOf(response.getData().getExpireAt());
        }
        if (timestamp - Long.parseLong(expireAt) > 0){
            response = refreshAccessToken();
            accessToken = response.getData().getAccessToken();
            signId = response.getData().getSignId();
            expireAt = String.valueOf(response.getData().getExpireAt());

        }
       if (response == null){
           return new HashMap<>();
       }
//        Map<String, String> params = new HashMap<>();
//        params.put("signId", response.getData().getSignId());
//        params.put("accessToken", response.getData().getAccessToken());
//        params.put("expireAt", String.valueOf(response.getData().getExpireAt()));
//        params.put("x-token", getXToken(params.get("accessToken"), params.get("signId"), Long.parseLong(params.get("expireAt"))));

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Token", getXToken(accessToken, signId, timestamp));
        headers.put("X-Timestamp", String.valueOf(timestamp));
        headers.put("X-SignId", signId);
        headers.put("Accept-Charset", "utf-8");
        headers.put("contentType", "utf-8");
        headers.put("X-BuildId", null);
        return headers;
    }

    // 参考签名X-Token生成工具类
    public static String getXToken(String accessToken, String signId, Long timestamp){
        return Md5Utils.md5(accessToken + Md5Util.md5(signId + Md5Util.md5(accessToken)).toLowerCase()+
                timestamp).toLowerCase();
    }

    public static String sendPost(String url, Map<String, String> headers, String jsonBody)
    {
        PrintWriter out = null;
        BufferedReader in = null;
        StringBuilder result = new StringBuilder();
        try
        {
            log.info("sendPost - {}, body: {}", url, jsonBody);
            URL realUrl = new URL(url);
            URLConnection conn = realUrl.openConnection();
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
            // 添加自定义 header
            if (headers != null) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    conn.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }
            // 写入 JSON 请求体
            out = new PrintWriter(conn.getOutputStream());
            out.print(jsonBody);
            out.flush();
            // 读取响应
            in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = in.readLine()) != null)
            {
                result.append(line);
            }
//            log.info("recv - {}", result);
        }
        catch (ConnectException e)
        {
            log.error("调用HttpUtils.sendPost ConnectException, url=" + url + ",param=" + jsonBody, e);
        }
        catch (SocketTimeoutException e)
        {
            log.error("调用HttpUtils.sendPost SocketTimeoutException, url=" + url + ",param=" + jsonBody, e);
        }
        catch (IOException e)
        {
            log.error("调用HttpUtils.sendPost IOException, url=" + url + ",param=" + jsonBody, e);
        }
        catch (Exception e)
        {
            log.error("调用HttpsUtil.sendPost Exception, url=" + url + ",param=" + jsonBody, e);
        }
        finally
        {
            try
            {
                if (out != null)
                {
                    out.close();
                }
                if (in != null)
                {
                    in.close();
                }
            }
            catch (IOException ex)
            {
                log.error("调用in.close Exception, url=" + url + ",param=" + jsonBody, ex);
            }
        }
        return result.toString();
    }

    public static String getLicence(String username, String password) {
        UserReq userReq = UserReq.builder().username(username).password(password).build();
        licence = AccessTokenApi.of(BaseConfg.BASE_URL, "null").getLicence(userReq).getData();
        return licence;
    }

    public static JoySuchResponse<TokenEntity> getAccessToken(String username, String password) {
        AccessTokenReq accessTokenReq = new AccessTokenReq(getLicence(username, password));
        JoySuchResponse<TokenEntity> response = AccessTokenApi.of(BaseConfg.BASE_URL, accessTokenReq.getLicence()).refreshAccessToken();
        return response;
    }

    public static JoySuchResponse<TokenEntity> refreshAccessToken() {
        AccessTokenReq accessTokenReq = new AccessTokenReq(licence);
        JoySuchResponse<TokenEntity> response = AccessTokenApi.of(BaseConfg.BASE_URL, accessTokenReq.getLicence()).refreshAccessToken();
        return response;
    }

    public static void main(String[] args) {
//        getListOfCards();
        String startTime = "2025-08-12 15:00:00";
        String endTime = "2025-08-12 21:00:00";
        JSONObject jsonObject = JSONObject.parseObject(getListOfPoints("1918B3000BA3", "209885", startTime, endTime));
        // 指定要生成的 JSON 文件路径
        String filePath = "D:/PlatformData/定位卡数据/鱼嘴" + "/20250806下午zq.json";

        try (FileWriter fileWriter = new FileWriter(filePath)) {
            fileWriter.write(jsonObject.toJSONString()); // fastjson 中使用 toJSONString() 方法
            System.out.println("JSON 文件已生成，路径为：" + filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
