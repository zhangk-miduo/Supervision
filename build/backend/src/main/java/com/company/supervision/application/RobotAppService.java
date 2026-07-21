package com.company.supervision.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.supervision.domain.model.WechatRobot;
import com.company.supervision.infrastructure.client.WechatClient;
import com.company.supervision.infrastructure.repository.RobotMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class RobotAppService {

    private final RobotMapper robotMapper;
    private final WechatClient wechatClient;

    public RobotAppService(RobotMapper robotMapper, WechatClient wechatClient) {
        this.robotMapper = robotMapper;
        this.wechatClient = wechatClient;
    }

    @Transactional
    public Long createRobot(String robotId, String name, String webhookUrl, String template) {
        WechatRobot robot = new WechatRobot();
        robot.setRobotId(robotId == null || robotId.isEmpty() ? "robot-" + UUID.randomUUID().toString().substring(0, 8) : robotId);
        robot.setName(name);
        robot.setWebhookUrl(webhookUrl);
        robot.setTemplate(template);
        robotMapper.insert(robot);
        return robot.getId();
    }

    @Transactional
    public void updateRobot(Long id, String name, String webhookUrl, String template) {
        WechatRobot robot = robotMapper.selectById(id);
        if (robot == null) throw new IllegalArgumentException("机器人不存在: " + id);
        if (name != null) robot.setName(name);
        if (webhookUrl != null) robot.setWebhookUrl(webhookUrl);
        if (template != null) robot.setTemplate(template);
        robotMapper.updateById(robot);
    }

    @Transactional
    public void deleteRobot(Long id) {
        robotMapper.deleteById(id);
    }

    public IPage<WechatRobot> listRobots(String name, int page, int size) {
        Page<WechatRobot> p = new Page<>(Math.max(page, 1), Math.max(size, 1));
        LambdaQueryWrapper<WechatRobot> w = new LambdaQueryWrapper<>();
        if (name != null && !name.isEmpty()) w.like(WechatRobot::getName, name);
        w.orderByDesc(WechatRobot::getId);
        return robotMapper.selectPage(p, w);
    }

    public WechatRobot getRobot(Long id) {
        WechatRobot robot = robotMapper.selectById(id);
        if (robot == null) throw new IllegalArgumentException("机器人不存在: " + id);
        return robot;
    }

    /**
     * 发送一条测试消息验证 Webhook 连通性。
     */
    public String testRobot(Long id) {
        WechatRobot robot = getRobot(id);
        return wechatClient.send(robot.getWebhookUrl(),
                "**Supervision 连通性测试**\n机器人：" + robot.getName() + "\n> 如果你看到这条消息，说明配置正确 ✅");
    }
}
