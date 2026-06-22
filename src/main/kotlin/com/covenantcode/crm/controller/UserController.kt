package com.covenantcode.crm.controller

import com.covenantcode.crm.dto.user.EnabledUpdateRequest
import com.covenantcode.crm.dto.user.UserResponse
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Управление пользователями")
@SecurityRequirement(name = "bearerAuth")
class UserController(private val userService: UserService) {

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Список всех пользователей (только ADMIN)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список пользователей"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    )
    fun getAll(@PageableDefault(size = 20) pageable: Pageable): Page<UserResponse> =
        userService.getAll(pageable)

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по ID (ADMIN — любой, остальные — только свой)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Данные пользователя"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Доступ к чужому профилю запрещён"),
        ApiResponse(responseCode = "404", description = "Пользователь не найден"),
    )
    fun getUserById(@PathVariable id: Long, authentication: Authentication): UserResponse {
        val currentUser = authentication.principal as User
        return userService.getUserById(id, currentUser.id!!)
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Заблокировать / разблокировать пользователя (только ADMIN)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Статус успешно изменён"),
        ApiResponse(responseCode = "400", description = "Попытка заблокировать себя или ошибка валидации"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Пользователь не найден"),
    )
    fun updateEnabled(
        @PathVariable id: Long,
        @Valid @RequestBody request: EnabledUpdateRequest,
        authentication: Authentication,
    ): UserResponse {
        val currentUser = authentication.principal as User
        return userService.updateEnabled(id, request.enabled!!, currentUser.id!!)
    }
}
