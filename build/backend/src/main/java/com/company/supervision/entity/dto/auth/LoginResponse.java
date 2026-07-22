package com.company.supervision.entity.dto.auth;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
@Data @AllArgsConstructor
public class LoginResponse { private String token; private Long accountId; private String username; private String displayName; private boolean mustChangePassword; private List<String> roles; }