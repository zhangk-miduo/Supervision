package com.company.supervision.entity.dto.auth;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class ResetPasswordRequest { @NotBlank private String temporaryPassword; }