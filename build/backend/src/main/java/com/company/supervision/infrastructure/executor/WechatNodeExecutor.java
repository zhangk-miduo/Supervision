package com.company.supervision.infrastructure.executor;

import com.company.supervision.domain.model.TaskNode;
import com.company.supervision.domain.service.ExecutionContext;
import com.company.supervision.domain.service.NodeExecutor;
import com.company.supervision.domain.service.NodeResult;
import com.company.supervision.domain.model.WechatRobot;
import com.company.supervision.infrastructure.client.WechatClient;
import com.company.supervision.infrastructure.repository.RobotMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class WechatNodeExecutor implements NodeExecutor {

    private final RobotMapper robotMapper;
    private final WechatClient wechatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final Pattern TOKEN = Pattern.compile("\\$\\{([^}]+)}");

    public WechatNodeExecutor(RobotMapper robotMapper, WechatClient wechatClient) {
        this.robotMapper = robotMapper;
        this.wechatClient = wechatClient;
    }

    @Override
    public String nodeType() {
        return "wechat";
    }

    @Override
    public NodeResult execute(TaskNode node, ExecutionContext ctx) {
        try {
            JsonNode cfg = objectMapper.readTree(node.getConfig());
            String robotId = cfg.path("robotId").asText();
            String content = cfg.path("content").asText("");

            WechatRobot robot = robotMapper.selectByRobotId(robotId);
            if (robot == null) {
                return NodeResult.fail("机器人不存在: " + robotId);
            }
            // 节点未配置内容时，回退到机器人模板
            if (content == null || content.isEmpty()) {
                content = robot.getTemplate() == null ? "" : robot.getTemplate();
            }
            content = render(content, ctx);
            String result = wechatClient.send(robot.getWebhookUrl(), content);
            log.info("[WECHAT] sent to robot={} => {}", robotId, result);
            return NodeResult.ok(result, "企微通知已发送");
        } catch (Exception e) {
            log.error("[WECHAT] failed: {}", e.getMessage());
            return NodeResult.fail("企微通知失败: " + e.getMessage());
        }
    }

    private String render(String template, ExecutionContext ctx) {
        if (template == null) return "";
        Matcher m = TOKEN.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String key = m.group(1);
            Object v = ctx.get(key);
            m.appendReplacement(sb, Matcher.quoteReplacement(v == null ? "" : v.toString()));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
