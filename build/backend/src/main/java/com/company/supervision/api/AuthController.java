package com.company.supervision.api;
import com.company.supervision.application.identity.AuthService;
import com.company.supervision.entity.dto.ApiResult;
import com.company.supervision.entity.dto.auth.ChangePasswordRequest;
import com.company.supervision.entity.dto.auth.LoginRequest;
import com.company.supervision.entity.dto.auth.LoginResponse;
import com.company.supervision.infrastructure.security.AuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/auth")
public class AuthController {
 private final AuthService auth; public AuthController(AuthService auth){this.auth=auth;}
 @PostMapping("/login") public ApiResult<LoginResponse> login(@Valid @RequestBody LoginRequest req,HttpServletRequest http){return ApiResult.ok(auth.login(req,http.getRemoteAddr()));}
 @PostMapping("/change-password") public ApiResult<Void> change(@Valid @RequestBody ChangePasswordRequest req,HttpServletRequest http){auth.changePassword((String)http.getAttribute("sessionToken"),req,http.getRemoteAddr());return ApiResult.ok(null);}
 @PostMapping("/logout") public ApiResult<Void> logout(HttpServletRequest http){auth.revoke((String)http.getAttribute("sessionToken"));return ApiResult.ok(null);}
}