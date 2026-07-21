package com.company.supervision.api;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.supervision.application.TaskAppService;
import com.company.supervision.domain.model.AutomationTask;
import com.company.supervision.domain.model.TaskExecution;
import com.company.supervision.entity.dto.ApiResult;
import com.company.supervision.entity.dto.TaskCreateRequest;
import com.company.supervision.entity.dto.TaskDetail;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskAppService taskAppService;

    public TaskController(TaskAppService taskAppService) {
        this.taskAppService = taskAppService;
    }

    @PostMapping
    public ApiResult<Long> create(@RequestBody TaskCreateRequest req) {
        return ApiResult.ok(taskAppService.createTask(req));
    }

    @GetMapping
    public ApiResult<IPage<AutomationTask>> list(@RequestParam(required = false) String name,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "20") int size) {
        return ApiResult.ok(taskAppService.listTasks(name, page, size));
    }

    @GetMapping("/{id}")
    public ApiResult<TaskDetail> get(@PathVariable Long id) {
        return ApiResult.ok(taskAppService.getTask(id));
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @RequestBody TaskCreateRequest req) {
        taskAppService.updateTask(id, req);
        return ApiResult.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        taskAppService.deleteTask(id);
        return ApiResult.ok();
    }

    @PostMapping("/{id}/execute")
    public ApiResult<Long> execute(@PathVariable Long id) {
        return ApiResult.ok(taskAppService.executeNow(id));
    }
}
