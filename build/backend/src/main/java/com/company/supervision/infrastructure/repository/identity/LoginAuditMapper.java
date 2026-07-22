package com.company.supervision.infrastructure.repository.identity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.supervision.domain.model.identity.LoginAudit;
import org.apache.ibatis.annotations.Mapper;
@Mapper public interface LoginAuditMapper extends BaseMapper<LoginAudit> {}