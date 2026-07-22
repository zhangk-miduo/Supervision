package com.company.supervision.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WechatClientTest {
    private final WechatClient client = new WechatClient(new RestTemplate(), new ObjectMapper());

    @Test
    void buildsTextPayloadWithMentions() {
        Map<String, Object> payload = client.buildPayload("text", "hello", List.of("u1", "u2"), false);
        assertThat(payload.get("msgtype")).isEqualTo("text");
        @SuppressWarnings("unchecked")
        Map<String, Object> text = (Map<String, Object>) payload.get("text");
        assertThat(text).containsEntry("content", "hello");
        assertThat(text).containsEntry("mentioned_list", List.of("u1", "u2"));
    }

    @Test
    void rejectsMentionsForMarkdownV2() {
        assertThatThrownBy(() -> client.buildPayload("markdown_v2", "hello", List.of("u1"), false))
                .isInstanceOf(IllegalArgumentException.class);
    }
}