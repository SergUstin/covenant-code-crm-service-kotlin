package com.covenantcode.crm.dto.auth

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "Имя обязательно")
    @field:Size(max = 100)
    @Schema(example = "Иван")
    val firstName: String,

    @field:NotBlank(message = "Фамилия обязательна")
    @field:Size(max = 100)
    @Schema(example = "Иванов")
    val lastName: String,

    @field:NotBlank(message = "Email обязателен")
    @field:Email(message = "Некорректный формат email")
    @Schema(example = "admin@covenantcode.ru")
    val email: String,

    @field:NotBlank(message = "Пароль обязателен")
    @field:Size(min = 8, message = "Пароль должен содержать минимум 8 символов")
    @Schema(example = "Admin123!")
    val password: String,

    @field:Size(max = 20)
    @Schema(example = "+79001234567")
    val phone: String? = null,
)
