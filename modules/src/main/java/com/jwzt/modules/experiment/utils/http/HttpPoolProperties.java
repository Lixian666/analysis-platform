package com.jwzt.modules.experiment.utils.http;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource(value= {"classpath:config/httpRequestConfig.properties"})
@ConfigurationProperties(prefix = "http.pool.conn")
public class HttpPoolProperties {
	
	/**
	 * 总的连接数
	 */
	private Integer maxTotal;
	
	/**
	 * 每个请求主机的最大并发数
	 */
    private Integer defaultMaxPerRoute;
    
    /**
     * 请求对方主机获取到连接的最大时间 
     */
    private Integer connectTimeout;
    
    /**
     * 从连接池中拿到连接的最长时间
     */
    private Integer connectionRequestTimeout;
    
    /**
     * 对方响应数据到本地的最大持续时间
     */
    private Integer socketTimeout;
    
    /**
     * 校验不活跃的连接
     */
    private Integer validateAfterInactivity;
	public Integer getMaxTotal() {
		return maxTotal;
	}
	public void setMaxTotal(Integer maxTotal) {
		this.maxTotal = maxTotal;
	}
	public Integer getDefaultMaxPerRoute() {
		return defaultMaxPerRoute;
	}
	public void setDefaultMaxPerRoute(Integer defaultMaxPerRoute) {
		this.defaultMaxPerRoute = defaultMaxPerRoute;
	}
	public Integer getConnectTimeout() {
		return connectTimeout;
	}
	public void setConnectTimeout(Integer connectTimeout) {
		this.connectTimeout = connectTimeout;
	}
	public Integer getConnectionRequestTimeout() {
		return connectionRequestTimeout;
	}
	public void setConnectionRequestTimeout(Integer connectionRequestTimeout) {
		this.connectionRequestTimeout = connectionRequestTimeout;
	}
	public Integer getSocketTimeout() {
		return socketTimeout;
	}
	public void setSocketTimeout(Integer socketTimeout) {
		this.socketTimeout = socketTimeout;
	}
	public Integer getValidateAfterInactivity() {
		return validateAfterInactivity;
	}
	public void setValidateAfterInactivity(Integer validateAfterInactivity) {
		this.validateAfterInactivity = validateAfterInactivity;
	}
    
    
    

}
