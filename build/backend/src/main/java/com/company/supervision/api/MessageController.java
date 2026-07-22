package com.company.supervision.api;

import com.company.supervision.application.identity.AuthService;
import com.company.supervision.application.messaging.MessageService;
import com.company.supervision.entity.dto.ApiResult;
import com.company.supervision.infrastructure.security.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/messages")
public class MessageController {
    private final MessageService service;public MessageController(MessageService service){this.service=service;}
    private DataScope scope(HttpServletRequest r){return DataScope.from((AuthService.SessionInfo)r.getAttribute(AuthInterceptor.SESSION_ATTRIBUTE));}
    @PostMapping("/preview")public ApiResult<?>preview(@RequestBody Map<String,Object>req){return ApiResult.ok(service.preview(req));}
    @PostMapping("/test-send")public ApiResult<?>test(@RequestBody Map<String,Object>req,HttpServletRequest r){return ApiResult.ok(service.test(req,scope(r)));}
    @GetMapping("/deliveries")public ApiResult<?>list(HttpServletRequest r){return ApiResult.ok(service.list(scope(r)));}
}
