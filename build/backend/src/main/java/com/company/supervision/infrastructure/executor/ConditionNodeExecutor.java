package com.company.supervision.infrastructure.executor;

import com.company.supervision.domain.model.TaskNode;
import com.company.supervision.domain.model.enumeration.ConditionOperator;
import com.company.supervision.domain.service.ExecutionContext;
import com.company.supervision.domain.service.NodeExecutor;
import com.company.supervision.domain.service.NodeResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConditionNodeExecutor implements NodeExecutor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String nodeType() {
        return "condition";
    }

    @Override
    public NodeResult execute(TaskNode node, ExecutionContext ctx) {
        try {
            JsonNode cfg = objectMapper.readTree(node.getConfig());
            String field = cfg.path("field").asText();
            ConditionOperator operator = ConditionOperator.of(cfg.path("operator").asText("="));
            String expected = cfg.path("value").asText("");

            String actual = resolveField(field, ctx);
            boolean pass = evaluate(actual, operator, expected);
            log.info("[CONDITION] field={} op={} expected={} actual={} => {}", field, operator, expected, actual, pass);
            if (pass) {
                return NodeResult.ok("条件满足，继续");
            }
            return NodeResult.fail("条件不满足（" + field + " " + operator.getCode() + " " + expected + "），链路停止");
        } catch (Exception e) {
            log.error("[CONDITION] failed: {}", e.getMessage());
            return NodeResult.fail("条件判断异常: " + e.getMessage());
        }
    }

    private String resolveField(String field, ExecutionContext ctx) {
        if (field == null || field.isEmpty()) return null;
        if (field.contains(".")) {
            String[] parts = field.split("\\.", 2);
            Object raw = ctx.get(parts[0]);
            if (raw == null) return null;
            try {
                JsonNode root = objectMapper.readTree(raw.toString());
                JsonNode cur = root;
                for (String token : parts[1].split("\\.")) {
                    if (cur == null || cur.isMissingNode()) return null;
                    cur = cur.get(token);
                }
                return cur == null || cur.isMissingNode() ? null : cur.asText();
            } catch (Exception e) {
                return raw.toString();
            }
        }
        Object v = ctx.get(field);
        return v == null ? null : v.toString();
    }

    private boolean evaluate(String actual, ConditionOperator op, String expected) {
        if (op == ConditionOperator.EMPTY) {
            return actual == null || actual.isEmpty();
        }
        if (actual == null) actual = "";
        switch (op) {
            case EQ:
                return actual.equals(expected);
            case NEQ:
                return !actual.equals(expected);
            case CONTAINS:
                return actual.contains(expected);
            case GT:
            case LT:
            case GTE:
            case LTE: {
                try {
                    double a = Double.parseDouble(actual);
                    double b = Double.parseDouble(expected);
                    if (op == ConditionOperator.GT) return a > b;
                    if (op == ConditionOperator.LT) return a < b;
                    if (op == ConditionOperator.GTE) return a >= b;
                    return a <= b;
                } catch (NumberFormatException e) {
                    int c = actual.compareTo(expected);
                    if (op == ConditionOperator.GT) return c > 0;
                    if (op == ConditionOperator.LT) return c < 0;
                    if (op == ConditionOperator.GTE) return c >= 0;
                    return c <= 0;
                }
            }
            default:
                return false;
        }
    }
}
