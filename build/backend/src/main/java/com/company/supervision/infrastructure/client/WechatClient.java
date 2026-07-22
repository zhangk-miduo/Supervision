package com.company.supervision.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.supervision.infrastructure.security.SensitiveDataRedactor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class WechatClient {
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public WechatClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public String send(String webhookUrl, String content) {
        return send(webhookUrl, "markdown", content, List.of(), false);
    }

    public String send(String webhookUrl, String msgType, String content,
                       List<String> mentionedUserIds, boolean mentionAll) {
        if (webhookUrl == null || webhookUrl.isBlank()) throw new IllegalArgumentException("webhookUrl is required");
        Map<String, Object> payload = buildPayload(msgType, content, mentionedUserIds, mentionAll);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(webhookUrl,
                    new HttpEntity<>(payload, headers), String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("WeCom returned HTTP " + response.getStatusCode().value());
            }
            JsonNode body = objectMapper.readTree(response.getBody());
            int errcode = body.path("errcode").asInt(-1);
            String errmsg = body.path("errmsg").asText("unknown");
            if (errcode != 0) throw new IllegalStateException("WeCom send failed errcode=" + errcode + " errmsg=" + errmsg);
            return errmsg;
        } catch (RuntimeException e) {
            throw new IllegalStateException(SensitiveDataRedactor.redact(e.getMessage()), e);
        } catch (Exception e) {
            throw new IllegalStateException("WeCom send failed: " + SensitiveDataRedactor.redact(e.getMessage()), e);
        }
    }

    Map<String, Object> buildPayload(String msgType, String content,
                                     List<String> mentionedUserIds, boolean mentionAll) {
        String normalized = msgType == null ? "text" : msgType.trim().toLowerCase();
        if (!normalized.equals("text") && !normalized.equals("markdown") && !normalized.equals("markdown_v2")) {
            throw new IllegalArgumentException("Unsupported baseline message type: " + normalized);
        }
        Map<String, Object> body = new HashMap<>();
        body.put("content", content == null ? "" : content);
        if (normalized.equals("text")) {
            List<String> mentions = new ArrayList<>();
            if (mentionAll) mentions.add("@all");
            else if (mentionedUserIds != null) mentions.addAll(mentionedUserIds);
            if (!mentions.isEmpty()) body.put("mentioned_list", mentions);
        } else if (mentionAll || (mentionedUserIds != null && !mentionedUserIds.isEmpty())) {
            if (normalized.equals("markdown_v2")) {
                throw new IllegalArgumentException("markdown_v2 does not support member mentions");
            }
            StringBuilder rendered = new StringBuilder(String.valueOf(body.get("content")));
            List<String> mentions = mentionAll ? List.of("all") : mentionedUserIds;
            for (String userId : mentions) rendered.append(" <@").append(userId).append(">");
            body.put("content", rendered.toString());
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("msgtype", normalized);
        payload.put(normalized, body);
        return payload;
    }
}