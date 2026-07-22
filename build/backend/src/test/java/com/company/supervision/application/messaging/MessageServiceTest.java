package com.company.supervision.application.messaging;

import com.company.supervision.application.RobotAppService;
import com.company.supervision.application.organization.WecomIntegrationService;
import com.company.supervision.domain.model.messaging.MessageDelivery;
import com.company.supervision.infrastructure.repository.messaging.*;
import com.company.supervision.infrastructure.security.SecretCipher;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MessageServiceTest {
    private MessageService service(){return new MessageService(mock(MessageDeliveryMapper.class),mock(WecomWebhookMapper.class),mock(WecomGroupMapper.class),mock(SecretCipher.class),mock(StringRedisTemplate.class),new RestTemplate(),new ObjectMapper(),mock(WecomIntegrationService.class),mock(RobotAppService.class));}
    @Test void supportsAllDocumentedTypes(){for(String t:List.of("text","markdown","markdown_v2","image","news","file","voice","template_card")){Map<String,Object>r=new HashMap<>();r.put("messageType",t);r.put("content",t.equals("news")?Map.of("articles",List.of(Map.of("title","a","url","https://x"))):Map.of("content","x"));assertThatCode(()->service().preview(r)).doesNotThrowAnyException();}}
    @Test void rejectsMarkdownV2Mention(){Map<String,Object>r=new HashMap<>();r.put("messageType","markdown_v2");r.put("mentionMode","ALL");r.put("content",Map.of("content","x"));assertThatThrownBy(()->service().preview(r)).isInstanceOf(IllegalArgumentException.class);}
    @Test void recordsRevokedPublicRobotWithoutSending(){MessageDeliveryMapper deliveries=mock(MessageDeliveryMapper.class);RobotAppService robots=mock(RobotAppService.class);when(robots.requireUsable(9L,1L)).thenThrow(new IllegalArgumentException("ROBOT_SHARE_REVOKED"));MessageService service=new MessageService(deliveries,mock(WecomWebhookMapper.class),mock(WecomGroupMapper.class),mock(SecretCipher.class),mock(StringRedisTemplate.class),new RestTemplate(),new ObjectMapper(),mock(WecomIntegrationService.class),robots);Map<String,Object>req=new HashMap<>();req.put("channel","GROUP_WEBHOOK");req.put("messageType","text");req.put("content",Map.of("content","x"));req.put("webhookId",9L);req.put("callerAccountId",1L);MessageDelivery result=service.send(req);assertThat(result.getNormalizedCode()).isEqualTo("ROBOT_SHARE_REVOKED");assertThat(result.getNormalizedMessage()).contains("收回共享");}
}
