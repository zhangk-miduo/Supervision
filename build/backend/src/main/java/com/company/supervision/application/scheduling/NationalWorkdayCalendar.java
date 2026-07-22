package com.company.supervision.application.scheduling;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.supervision.domain.model.scheduling.*;
import com.company.supervision.infrastructure.repository.scheduling.*;
import org.springframework.stereotype.Service;
import java.time.*;import java.util.*;

@Service public class NationalWorkdayCalendar{
 public enum Decision{WORKDAY,HOLIDAY,UNKNOWN}
 private final WorkdayCalendarYearMapper years;private final WorkdayCalendarExceptionMapper exceptions;
 public NationalWorkdayCalendar(WorkdayCalendarYearMapper y,WorkdayCalendarExceptionMapper e){years=y;exceptions=e;}
 public Decision decision(LocalDate date){WorkdayCalendarYear y=years.selectById(date.getYear());if(y==null||!"ACTIVE".equals(y.getStatus()))return Decision.UNKNOWN;WorkdayCalendarException x=exceptions.byDate(date.getYear(),date);if(x!=null)return"WORKDAY".equals(x.getDayType())?Decision.WORKDAY:Decision.HOLIDAY;DayOfWeek d=date.getDayOfWeek();return d==DayOfWeek.SATURDAY||d==DayOfWeek.SUNDAY?Decision.HOLIDAY:Decision.WORKDAY;}
 public String reason(LocalDate date){WorkdayCalendarException x=exceptions.byDate(date.getYear(),date);if(x!=null)return first(x.getHolidayName(),x.getNote(),x.getDayType());return switch(decision(date)){case WORKDAY->"国家工作日";case HOLIDAY->"周末休息日";case UNKNOWN->"缺少"+date.getYear()+"年度国家工作日日历";};}
 public List<WorkdayCalendarYear>coverage(){return years.selectList(new LambdaQueryWrapper<WorkdayCalendarYear>().orderByDesc(WorkdayCalendarYear::getYearValue));}
 public List<LocalDateTime>next(List<String>times,ZoneId zone,int count,LocalDateTime end){List<LocalTime>clock=(times==null||times.isEmpty()?List.of("09:00"):times).stream().map(LocalTime::parse).sorted().toList();LocalDate date=LocalDate.now(zone);LocalDateTime now=LocalDateTime.now(zone);List<LocalDateTime>out=new ArrayList<>();for(int guard=0;guard<800&&out.size()<count;guard++,date=date.plusDays(1)){if(decision(date)!=Decision.WORKDAY)continue;for(LocalTime time:clock){LocalDateTime candidate=LocalDateTime.of(date,time);if(candidate.isAfter(now)&&(end==null||!candidate.isAfter(end)))out.add(candidate);if(out.size()>=count)break;}}if(out.size()<count)throw new IllegalStateException("国家工作日日历覆盖不足，无法计算未来"+count+"次执行时间");return out;}
 private String first(String...v){for(String s:v)if(s!=null&&!s.isBlank())return s;return null;}
}
