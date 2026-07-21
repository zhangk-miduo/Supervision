package com.company.supervision.infrastructure.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.supervision.domain.model.WechatRobot;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RobotMapper extends BaseMapper<WechatRobot> {

    @Select("SELECT * FROM supervision_wechat_robot WHERE robot_id = #{robotId} LIMIT 1")
    WechatRobot selectByRobotId(@Param("robotId") String robotId);
}
