package com.company.supervision.domain.model.identity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
@Data @TableName("supervision_role")
public class Role { @TableId(type=IdType.AUTO) private Long id; private String code; private String name; }