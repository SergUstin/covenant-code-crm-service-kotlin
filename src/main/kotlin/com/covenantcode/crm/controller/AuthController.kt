package com.covenantcode.crm.controller

import com.covenantcode.crm.dto.auth.AuthResponse
import com.covenantcode.crm.dto.auth.LoginRequest
import com.covenantcode.crm.dto.auth.RegisterRequest
import com.covenantcode.crm.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Регистрация и вход в систему")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация нового пользователя", description = "Создаёт пользователя с ролью MANAGER и возвращает JWT-токен")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Пользователь зарегистрирован"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных"),
        ApiResponse(responseCode = "409", description = "Пользователь с таким email уже существует"),
    )
    fun register(@Valid @RequestBody request: RegisterRequest): AuthResponse =
        authService.register(request)

    @PostMapping("/login")
    @Operation(summary = "Вход в систему", description = "Проверяет учётные данные и возвращает JWT-токен")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Успешный вход"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации входных данных"),
        ApiResponse(responseCode = "401", description = "Неверный email или пароль"),
    )
    fun login(@Valid @RequestBody request: LoginRequest): AuthResponse =
        authService.login(request)
}
