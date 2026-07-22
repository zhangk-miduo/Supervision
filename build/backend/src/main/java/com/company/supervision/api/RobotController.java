package com.company.supervision.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.supervision.application.RobotAppService;
import com.company.supervision.application.identity.AuthService;
import com.company.supervision.entity.dto.*;
import com.company.supervision.infrastructure.security.AuthInterceptor;
import com.company.supervision.infrastructure.security.DataScope;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/robots")
public class RobotController {
    private final RobotAppService service;public RobotController(RobotAppService service){this.service=service;}
    private DataScope scope(HttpServletRequest r){return DataScope.from((AuthService.SessionInfo)r.getAttribute(AuthInterceptor.SESSION_ATTRIBUTE));}
    @PostMapping public ApiResult<Long>create(@RequestBody RobotRequest req,HttpServletRequest r){return ApiResult.ok(service.createRobot(req,scope(r)));}
    @GetMapping public ApiResult<IPage<RobotView>>list(@RequestParam(required=false)String name,@RequestParam(required=false)String view,@RequestParam(required=false)Long creatorAccountId,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size,HttpServletRequest r){return ApiResult.ok(service.listRobots(name,view,creatorAccountId,page,size,scope(r)));}
    @GetMapping("/selectable")public ApiResult<List<SelectableRobot>>selectable(HttpServletRequest r){return ApiResult.ok(service.listSelectableRobots(scope(r)));}
    @GetMapping("/{id}/usage-impact")public ApiResult<Map<String,Object>>impact(@PathVariable Long id,HttpServletRequest r){return ApiResult.ok(service.usageImpact(id,scope(r)));}
    @GetMapping("/{id}")public ApiResult<RobotView>get(@PathVariable Long id,HttpServletRequest r){return ApiResult.ok(service.getRobot(id,scope(r)));}
    @PutMapping("/{id}")public ApiResult<Void>update(@PathVariable Long id,@RequestBody RobotRequest req,HttpServletRequest r){service.updateRobot(id,req,scope(r));return ApiResult.ok();}
    @DeleteMapping("/{id}")public ApiResult<Void>delete(@PathVariable Long id,HttpServletRequest r){service.deleteRobot(id,scope(r));return ApiResult.ok();}
    @PostMapping("/{id}/test")public ApiResult<String>test(@PathVariable Long id,HttpServletRequest r){return ApiResult.ok(service.testRobot(id,scope(r)));}
}
