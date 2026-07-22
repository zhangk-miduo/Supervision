package com.company.supervision.domain.model.identity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;
@Data @TableName("supervision_login_audit")
public class LoginAudit { @TableId(type=IdType.AUTO) private Long id; private Long accountId; private String username; private String eventType; private Integer success; private String ipAddress; private String detail; private LocalDateTime createdAt; }