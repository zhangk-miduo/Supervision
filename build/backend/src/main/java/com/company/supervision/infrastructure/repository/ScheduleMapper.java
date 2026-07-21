package com.company.supervision.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.supervision.domain.model.TaskSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ScheduleMapper extends BaseMapper<TaskSchedule> {

    @Select("SELECT * FROM supervision_task_schedule WHERE task_id = #{taskId} LIMIT 1")
    TaskSchedule selectByTaskId(@Param("taskId") Long taskId);
}
