package com.company.supervision.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.supervision.domain.model.TaskNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface TaskNodeMapper extends BaseMapper<TaskNode> {

    @Select("SELECT * FROM supervision_task_node WHERE task_id = #{taskId} ORDER BY node_order ASC")
    List<TaskNode> selectByTaskIdOrdered(@Param("taskId") Long taskId);
}
