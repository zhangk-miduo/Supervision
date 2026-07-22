package com.company.supervision.infrastructure.repository.messaging;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.supervision.domain.model.messaging.WecomGroup;
import org.apache.ibatis.annotations.*;

@Mapper
public interface WecomGroupMapper extends BaseMapper<WecomGroup> {
    @Select("SELECT * FROM supervision_wecom_group WHERE tenant_key=#{tenant} AND owner_account_id=#{ownerAccountId} AND group_name=#{name} LIMIT 1")
    WecomGroup byName(String tenant, Long ownerAccountId, String name);
}
