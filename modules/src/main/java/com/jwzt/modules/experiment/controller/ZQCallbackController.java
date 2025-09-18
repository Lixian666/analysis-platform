package com.jwzt.modules.experiment.controller;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/subscribe/callback")
public class ZQCallbackController {


    @RequestMapping("/zqTagScanUwbBeacon")
    public String zqTagScanUwbBeacon(@RequestBody String requestBody) {
        try {
            // 打印接收到的回调数据
//            System.out.println("Received callback data: " + requestBody);

//            // 解析 JSON 数据
//            JSONObject jsonResponse = JSONObject.parseObject(requestBody);
//            String buildId = jsonResponse.getJSONArray("resultList")
//                    .getJSONObject(0).getString("buildId");
//            String subscribeId = jsonResponse.getJSONArray("resultList")
//                    .getJSONObject(0).getString("subscribeId");

            // 将数据保存到数据库
//            saveToDatabase(buildId, subscribeId);

            // 返回成功响应
            return "Callback received successfully!";
        } catch (Exception e) {
            e.printStackTrace();
            return "Error processing callback!";
        }
    }

    private void saveToDatabase(String buildId, String subscribeId) {

    }
}
