package com.covenantcode.crm.controller;


import com.covenantcode.crm.dto.auth.AuthResponse;
import com.covenantcode.crm.dto.auth.LoginRequest;
import com.covenantcode.crm.dto.auth.RegisterRequest;
import com.covenantcode.crm.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Регистрация нового сотрудника")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Пользователь успешно зарегистрирован"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации"),
            @ApiResponse(responseCode = "409", description = "Пользователь с таким email уже существует")
    })
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Аутентификация пользователя")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Аутентификация успешна"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Ошибка валидации"),
            @ApiResponse(
                    responseCode = "401",
                    description = "Неверный логин или пароль")
    })
    public AuthResponse login(@Valid @RequestBody LoginRequest request){
        return  authService.login(request);
    }
}
