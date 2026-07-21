package com.company.supervision.infrastructure.executor;

import com.company.supervision.domain.model.TaskNode;
import com.company.supervision.domain.model.enumeration.NodeType;
import com.company.supervision.domain.service.ExecutionContext;
import com.company.supervision.domain.service.NodeExecutor;
import com.company.supervision.domain.service.NodeResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;
import java.util.Map;

@Slf4j
@Component
public class HttpNodeExecutor implements NodeExecutor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpNodeExecutor(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public NodeResult execute(TaskNode node, ExecutionContext ctx) {
        try {
            JsonNode cfg = objectMapper.readTree(node.getConfig());
            String url = cfg.path("url").asText();
            String method = cfg.path("method").asText("GET");
            HttpHeaders headers = new HttpHeaders();
            JsonNode headerNode = cfg.get("headers");
            if (headerNode != null && headerNode.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = headerNode.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    headers.add(e.getKey(), e.getValue().asText());
                }
            }
            String body = cfg.has("body") ? cfg.get("body").toString() : null;

            ResponseEntity<String> resp;
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) {
                HttpEntity<String> entity = new HttpEntity<>(body, headers);
                resp = restTemplate.exchange(url, HttpMethod.valueOf(method.toUpperCase()), entity, String.class);
            } else {
                resp = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
            }

            String respBody = resp.getBody();
            ctx.put("http_" + node.getId(), respBody);
            ctx.put("http_" + node.getId() + "_status", resp.getStatusCodeValue());
            log.info("[HTTP] node={} status={}", node.getId(), resp.getStatusCodeValue());
            return NodeResult.ok(respBody, "HTTP " + resp.getStatusCodeValue());
        } catch (Exception e) {
            log.error("[HTTP] node={} failed: {}", node.getId(), e.getMessage());
            return NodeResult.fail("HTTP 请求失败: " + e.getMessage());
        }
    }

    @Override
    public String nodeType() {
        return NodeType.HTTP.getCode();
    }
}
