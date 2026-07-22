package com.company.supervision.domain.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("supervision_wechat_robot")
public class WechatRobot {
    @TableId(type = IdType.AUTO) private Long id;
    private String robotId;
    private String name;
    private String webhookUrl;
    private String template;
    private LocalDateTime createdAt;
    @TableField(exist=false) private Long groupId;
    @TableField(exist=false) private String groupName;
    @TableField(exist=false) private String pushName;
    @TableField(exist=false) private Integer pushStatus;
    @TableField(exist=false) private LocalDateTime lastTestedAt;
}
