package com.company.supervision.infrastructure.executor;

import com.company.supervision.domain.model.TaskNode;
import com.company.supervision.domain.model.WechatRobot;
import com.company.supervision.domain.model.messaging.WecomWebhook;
import com.company.supervision.domain.service.ExecutionContext;
import com.company.supervision.domain.service.NodeExecutor;
import com.company.supervision.domain.service.NodeResult;
import com.company.supervision.infrastructure.client.WechatClient;
import com.company.supervision.infrastructure.repository.RobotMapper;
import com.company.supervision.infrastructure.repository.messaging.WecomWebhookMapper;
import com.company.supervision.infrastructure.security.SecretCipher;
import com.company.supervision.infrastructure.security.SensitiveDataRedactor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.regex.*;

@Slf4j
@Component
public class WechatNodeExecutor implements NodeExecutor {
    private final RobotMapper robots; private final WecomWebhookMapper hooks; private final SecretCipher cipher;
    private final WechatClient client; private final ObjectMapper json;
    private static final Pattern TOKEN = Pattern.compile("\\$\\{([^}]+)}");
    public WechatNodeExecutor(RobotMapper robots, WecomWebhookMapper hooks, SecretCipher cipher, WechatClient client, ObjectMapper json) {
        this.robots=robots; this.hooks=hooks; this.cipher=cipher; this.client=client; this.json=json;
    }
    @Override public String nodeType(){ return "wechat"; }
    @Override public NodeResult execute(TaskNode node, ExecutionContext ctx) {
        try {
            JsonNode cfg=json.readTree(node.getConfig());
            WechatRobot robot=robots.selectByRobotId(cfg.path("robotId").asText());
            if(robot==null) return NodeResult.fail("Robot not found");
            WecomWebhook hook=hooks.selectById(robot.getId());
            if(hook==null) return NodeResult.fail("Encrypted webhook configuration not found");
            String content=cfg.path("content").asText("");
            if(content.isEmpty()) content=robot.getTemplate()==null?"":robot.getTemplate();
            List<String> mentions=new ArrayList<>(); cfg.path("mentionedUserIds").forEach(v->mentions.add(v.asText()));
            String result=client.send(cipher.decrypt(hook.getWebhookCipher()),cfg.path("msgType").asText("text"),render(content,ctx),mentions,cfg.path("mentionAll").asBoolean(false));
            return NodeResult.ok(result,"WeCom notification sent");
        } catch(Exception e) {
            log.error("[WECHAT] failed: {}",SensitiveDataRedactor.redact(e.getMessage()));
            return NodeResult.fail("WeCom notification failed");
        }
    }
    private String render(String template,ExecutionContext ctx){Matcher m=TOKEN.matcher(template==null?"":template);StringBuffer out=new StringBuffer();while(m.find()){Object v=ctx.get(m.group(1));m.appendReplacement(out,Matcher.quoteReplacement(v==null?"":v.toString()));}m.appendTail(out);return out.toString();}
}