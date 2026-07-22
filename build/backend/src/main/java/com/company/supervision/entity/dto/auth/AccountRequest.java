package com.company.supervision.entity.dto.auth;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;
@Data public class AccountRequest { @NotBlank private String username; @NotBlank private String displayName; private String temporaryPassword; private Long personId; private Integer status=1; private List<String> roles; }