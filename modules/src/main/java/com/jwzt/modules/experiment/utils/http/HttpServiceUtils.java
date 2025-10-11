package com.jwzt.modules.experiment.utils.http;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jwzt.modules.experiment.utils.http.RestTemplateConfig;

@Component
public class HttpServiceUtils {
	@Autowired
	private RestTemplateConfig restTemplateConfig;
	private Logger log = LoggerFactory.getLogger(this.getClass());
	
	/**
	 * GET请求
	 * @param url 请求地址
	 * @return
	 */
	public JSONObject GETForEntity(String url){
		RestTemplate restTemplate = restTemplateConfig.restTemplate();
		log.info("对接第三方数据=========>请求地址：{}", url);
		ResponseEntity<JSONObject> response = restTemplate.getForEntity(url, JSONObject.class);
//		log.info("对接第三方数据=========>响应数据：{}", response.getBody().toString());
		JSONObject returnStr = response.getBody();
		return returnStr;
	}
	/**
	 * POST请求 - application/json;charset=UTF-8
	 * @param map 请求参数
	 * @param url 请求地址
	 * @return
	 * @throws Exception
	 */
	public JSONObject POSTForEntityJson(Map<String, Object> map,String url) throws Exception {
		RestTemplate restTemplate = restTemplateConfig.restTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON_UTF8);
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(map, headers);
		log.info("对接第三方数据=========>请求地址：{}", url);
		log.info("对接第三方数据=========>请求参数：{}", JSON.toJSONString(map));
		ResponseEntity<JSONObject> response = restTemplate.postForEntity(url, request, JSONObject.class);
//		log.info("对接第三方数据=========>响应数据：{}", response.getBody().toString());
		JSONObject returnStr = response.getBody();
		return returnStr;
	}
}
