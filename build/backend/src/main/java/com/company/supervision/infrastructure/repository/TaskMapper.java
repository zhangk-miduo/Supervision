package com.company.supervision.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.supervision.domain.model.AutomationTask;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<AutomationTask> {
}
