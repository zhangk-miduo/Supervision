package com.company.supervision.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.supervision.application.RobotAppService;
import com.company.supervision.domain.model.WechatRobot;
import com.company.supervision.entity.dto.ApiResult;
import com.company.supervision.entity.dto.RobotRequest;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/robots")
public class RobotController {

    private final RobotAppService robotAppService;

    public RobotController(RobotAppService robotAppService) {
        this.robotAppService = robotAppService;
    }

    @PostMapping
    public ApiResult<Long> create(@RequestBody RobotRequest req) {
        return ApiResult.ok(robotAppService.createRobot(req.getRobotId(), req.getName(), req.getWebhookUrl(), req.getTemplate()));
    }

    @GetMapping
    public ApiResult<IPage<WechatRobot>> list(@RequestParam(required = false) String name,
                                              @RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResult.ok(robotAppService.listRobots(name, page, size));
    }

    @GetMapping("/{id}")
    public ApiResult<WechatRobot> get(@PathVariable Long id) {
        return ApiResult.ok(robotAppService.getRobot(id));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody RobotRequest req) {
        robotAppService.updateRobot(id, req.getName(), req.getWebhookUrl(), req.getTemplate());
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        robotAppService.deleteRobot(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/test")
    public ApiResult<String> test(@PathVariable Long id) {
        return ApiResult.ok(robotAppService.testRobot(id));
    }
}
