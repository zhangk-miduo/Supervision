package com.company.supervision.domain.model.scheduling;
import com.baomidou.mybatisplus.annotation.*;import lombok.Data;import java.time.*;
@Data @TableName("supervision_workday_calendar_year") public class WorkdayCalendarYear{@TableId(value="year_value",type=IdType.INPUT)Integer yearValue;String status;String sourceUrl;String sourceDocument;String versionValue;LocalDate publishedAt;LocalDateTime importedAt;String checksumValue;}
