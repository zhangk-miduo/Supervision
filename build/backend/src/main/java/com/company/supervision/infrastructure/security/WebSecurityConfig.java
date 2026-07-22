package com.company.supervision.infrastructure.security;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration public class WebSecurityConfig implements WebMvcConfigurer {
 private final AuthInterceptor interceptor; public WebSecurityConfig(AuthInterceptor i){interceptor=i;}
 @Override public void addInterceptors(InterceptorRegistry registry){registry.addInterceptor(interceptor).addPathPatterns("/**").excludePathPatterns("/health","/auth/login","/error","/actuator/**");}
}