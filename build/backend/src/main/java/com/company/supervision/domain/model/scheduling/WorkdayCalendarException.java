package com.company.supervision.domain.model.scheduling;
import com.baomidou.mybatisplus.annotation.*;import lombok.Data;import java.time.LocalDate;
@Data @TableName("supervision_workday_calendar_exception") public class WorkdayCalendarException{@TableId(type=IdType.AUTO)Long id;Integer yearValue;LocalDate dateValue;String dayType;String holidayName;String note;}
