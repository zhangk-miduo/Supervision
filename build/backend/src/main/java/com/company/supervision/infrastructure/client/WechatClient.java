package com.company.supervision.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class WechatClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WechatClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 发送企业微信机器人消息（markdown 类型）。
     *
     * @return 企业微信返回的 errmsg
     */
    public String send(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            throw new IllegalArgumentException("webhookUrl 为空");
        }
        Map<String, Object> markdown = new HashMap<>();
        markdown.put("content", content == null ? "" : content);
        Map<String, Object> payload = new HashMap<>();
        payload.put("msgtype", "markdown");
        payload.put("markdown", markdown);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(webhookUrl, entity, String.class);
            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("企微返回 HTTP " + resp.getStatusCodeValue());
            }
            JsonNode node = objectMapper.readTree(resp.getBody());
            int errcode = node.path("errcode").asInt(-1);
            String errmsg = node.path("errmsg").asText("unknown");
            if (errcode != 0) {
                throw new RuntimeException("企微发送失败 errcode=" + errcode + " errmsg=" + errmsg);
            }
            return errmsg;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("企微发送异常: " + e.getMessage());
        }
    }
}
