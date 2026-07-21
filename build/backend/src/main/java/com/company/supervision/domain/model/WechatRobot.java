package com.company.supervision.domain.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("supervision_wechat_robot")
public class WechatRobot {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 机器人标识 */
    private String robotId;

    private String name;

    private String webhookUrl;

    private String template;

    private LocalDateTime createdAt;
}
