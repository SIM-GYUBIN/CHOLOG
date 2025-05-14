package com.example.logserver.util;

import com.example.logserver.model.LogEntry;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LogEntry 객체를 역직렬화하기 위한 Deserializer 클래스
 * JSON 데이터를 LogEntry 객체로 변환하는 로직을 담당
 */
public class LogEntryDeserializer extends JsonDeserializer<LogEntry> {
    @Override
    public LogEntry deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        LogEntry entry = new LogEntry();

        // 표준 필드 처리
        if (node.has("level")) entry.setLevel(node.get("level").asText(null));
        if (node.has("message")) entry.setMessage(node.get("message").asText(null));
        if (node.has("timestamp")) entry.setTimestamp(node.get("timestamp").asText(null));
        if (node.has("logger")) entry.setLogger(node.get("logger").asText(null));
        if (node.has("thread")) entry.setThread(node.get("thread").asText(null));
        if (node.has("sequence")) entry.setSequence(node.get("sequence").asText(null));
        if (node.has("serviceName")) entry.setServiceName(node.get("serviceName").asText(null));
        if (node.has("environment")) entry.setEnvironment(node.get("environment").asText(null));
        if (node.has("profiles")) entry.setProfiles(node.get("profiles").asText(null)); // 프로필 정보 추가
        if (node.has("version")) entry.setVersion(node.get("version").asText(null));
        if (node.has("hostName")) entry.setHostName(node.get("hostName").asText(null));
        if (node.has("apiKey")) entry.setApiKey(node.get("apiKey").asText(null));
        if (node.has("requestId")) entry.setRequestId(node.get("requestId").asText(null));
        if (node.has("requestMethod")) entry.setRequestMethod(node.get("requestMethod").asText(null));
        if (node.has("requestUri")) entry.setRequestUri(node.get("requestUri").asText(null));
        if (node.has("clientIp")) entry.setClientIp(node.get("clientIp").asText(null));
        if (node.has("userAgent")) entry.setUserAgent(node.get("userAgent").asText(null));
        if (node.has("httpStatus") && !node.get("httpStatus").isNull()) entry.setHttpStatus(node.get("httpStatus").asInt());
        if (node.has("uaMobile") && !node.get("uaMobile").isNull()) entry.setUaMobile(node.get("uaMobile").asBoolean());
        if (node.has("uaPlatform")) entry.setUaPlatform(node.get("uaPlatform").asText(null));
        if (node.has("filtered") && !node.get("filtered").isNull()) entry.setFiltered(node.get("filtered").asBoolean());
        if (node.has("requestParams")) entry.setRequestParams(node.get("requestParams").asText(null));
        if (node.has("responseHeaders")) entry.setResponseHeaders(readObjectMap(node.get("responseHeaders")));

        // 객체 필드 처리
        if (node.has("performanceMetrics")) entry.setPerformanceMetrics(readObjectMap(node.get("performanceMetrics")));
        if (node.has("mdcContext")) entry.setMdcContext(readObjectMap(node.get("mdcContext")));
        if (node.has("headers")) entry.setHeaders(readObjectMap(node.get("headers")));
        if (node.has("throwable")) entry.setThrowable(readObjectMap(node.get("throwable")));

        // 불필요한 빈 필드 제거
        entry.optimizeFields();

        return entry;
    }

    // JsonNode를 Map<String, Object>로 변환
    private Map<String, Object> readObjectMap(JsonNode node) {
        if (node == null || node.isNull()) return null;

        Map<String, Object> map = new HashMap<>();
        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            if (value.isTextual()) map.put(key, value.asText());
            else if (value.isNumber()) {
                if (value.isInt()) map.put(key, value.asInt());
                else if (value.isLong()) map.put(key, value.asLong());
                else if (value.isDouble()) map.put(key, value.asDouble());
                else map.put(key, value.asText());
            }
            else if (value.isBoolean()) map.put(key, value.asBoolean());
            else if (value.isObject()) map.put(key, readObjectMap(value));
            else if (value.isArray()) {
                List<Object> list = new ArrayList<>();
                value.elements().forEachRemaining(element -> {
                    if (element.isTextual()) list.add(element.asText());
                    else if (element.isNumber()) list.add(element.asDouble());
                    else if (element.isBoolean()) list.add(element.asBoolean());
                    else if (element.isObject()) list.add(readObjectMap(element));
                });
                map.put(key, list);
            }
        });

        return map;
    }
} 