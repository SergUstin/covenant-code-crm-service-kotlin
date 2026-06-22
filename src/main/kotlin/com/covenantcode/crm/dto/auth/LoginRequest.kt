package com.covenantcode.crm.dto.auth

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "Email обязателен")
    @field:Email(message = "Некорректный формат email")
    @Schema(example = "admin@covenantcode.ru")
    val email: String,

    @field:NotBlank(message = "Пароль обязателен")
    @Schema(example = "Admin123!")
    val password: String,
)
