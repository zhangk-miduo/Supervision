package com.company.supervision.domain.model.identity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("supervision_account")
public class Account {
    @TableId(type = IdType.AUTO) private Long id;
    private String username;
    private String passwordHash;
    private String displayName;
    private Long personId;
    private Integer status;
    private Integer mustChangePassword;
    private Integer failedLoginCount;
    private LocalDateTime lockedUntil;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime lastLoginAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}