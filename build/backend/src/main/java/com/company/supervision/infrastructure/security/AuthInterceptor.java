package com.company.supervision.infrastructure.security;

import com.company.supervision.application.identity.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String SESSION_ATTRIBUTE="authenticatedSession";
    private final AuthService auth;
    public AuthInterceptor(AuthService auth){this.auth=auth;}
    @Override public boolean preHandle(HttpServletRequest request,HttpServletResponse response,Object handler){
        String path=request.getRequestURI().substring(request.getContextPath().length());
        String header=request.getHeader("Authorization");
        String token=header!=null&&header.startsWith("Bearer ")?header.substring(7):null;
        boolean changePassword=path.equals("/auth/change-password")||path.equals("/auth/logout");
        AuthService.SessionInfo session=auth.requireSession(token,changePassword);
        if((path.startsWith("/accounts")||path.startsWith("/settings")||path.startsWith("/wecom/sync"))&&!session.isAdmin()) throw new SecurityException("Administrator permission required");
        request.setAttribute(SESSION_ATTRIBUTE,session); request.setAttribute("sessionToken",token); return true;
    }
}