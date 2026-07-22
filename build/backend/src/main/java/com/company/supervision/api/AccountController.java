package com.company.supervision.api;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.company.supervision.application.identity.AccountService;
import com.company.supervision.application.identity.AuthService;
import com.company.supervision.domain.model.identity.Account;
import com.company.supervision.entity.dto.ApiResult;
import com.company.supervision.entity.dto.auth.AccountRequest;
import com.company.supervision.entity.dto.auth.ResetPasswordRequest;
import com.company.supervision.infrastructure.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/accounts")
public class AccountController {
 private final AccountService service; public AccountController(AccountService s){service=s;}
 @GetMapping public ApiResult<IPage<Account>> list(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int size){return ApiResult.ok(service.list(page,size));}
 @PostMapping public ApiResult<Long> create(@Valid @RequestBody AccountRequest req,HttpServletRequest http){AuthService.SessionInfo s=(AuthService.SessionInfo)http.getAttribute(AuthInterceptor.SESSION_ATTRIBUTE);return ApiResult.ok(service.create(req,s.getAccountId()));}
 @PutMapping("/{id}") public ApiResult<Void> update(@PathVariable Long id,@Valid @RequestBody AccountRequest req){service.update(id,req);return ApiResult.ok(null);}
 @PostMapping("/{id}/reset-password") public ApiResult<Void> reset(@PathVariable Long id,@Valid @RequestBody ResetPasswordRequest req){service.resetPassword(id,req.getTemporaryPassword());return ApiResult.ok(null);}
}