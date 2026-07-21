package com.company.supervision.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.supervision.application.ExecutionAppService;
import com.company.supervision.domain.model.TaskExecution;
import com.company.supervision.entity.dto.ApiResult;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/executions")
public class ExecutionController {

    private final ExecutionAppService executionAppService;

    public ExecutionController(ExecutionAppService executionAppService) {
        this.executionAppService = executionAppService;
    }

    @GetMapping
    public ApiResult<IPage<TaskExecution>> list(@RequestParam(required = false) Long taskId,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size) {
        return ApiResult.ok(executionAppService.listExecutions(taskId, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResult<TaskExecution> get(@PathVariable Long id) {
        return ApiResult.ok(executionAppService.getExecution(id));
    }
}
