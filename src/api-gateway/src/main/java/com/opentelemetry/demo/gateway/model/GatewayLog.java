package com.opentelemetry.demo.gateway.model;

import java.util.Date;

/**
 * 网关日志数据模型
 */
public class GatewayLog {

    /** 目标服务标识 */
    private String targetServer;
    /** 请求路径 */
    private String requestPath;
    /** 请求方法 */
    private String requestMethod;
    /** 协议（HTTP/HTTPS） */
    private String schema;
    /** 请求体 */
    private String requestBody;
    /** 响应体 */
    private String responseData;
    /** 客户端 IP */
    private String ip;
    /** 请求时间 */
    private Date requestTime;
    /** 响应时间 */
    private Date responseTime;
    /** 执行耗时（毫秒） */
    private long executeTime;

    // ---------------- getters & setters ----------------

    public String getTargetServer() {
        return targetServer;
    }

    public void setTargetServer(String targetServer) {
        this.targetServer = targetServer;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Date getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(Date requestTime) {
        this.requestTime = requestTime;
    }

    public Date getResponseTime() {
        return responseTime;
    }

    public void setResponseTime(Date responseTime) {
        this.responseTime = responseTime;
    }

    public long getExecuteTime() {
        return executeTime;
    }

    public void setExecuteTime(long executeTime) {
        this.executeTime = executeTime;
    }

    @Override
    public String toString() {
        return "GatewayLog{" +
                "targetServer='" + targetServer + '\'' +
                ", requestPath='" + requestPath + '\'' +
                ", requestMethod='" + requestMethod + '\'' +
                ", schema='" + schema + '\'' +
                ", requestBody='" + requestBody + '\'' +
                ", responseData='" + responseData + '\'' +
                ", ip='" + ip + '\'' +
                ", requestTime=" + requestTime +
                ", responseTime=" + responseTime +
                ", executeTime=" + executeTime +
                '}';
    }
}
