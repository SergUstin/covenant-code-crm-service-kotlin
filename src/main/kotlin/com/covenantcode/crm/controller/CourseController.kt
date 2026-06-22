package com.covenantcode.crm.controller

import com.covenantcode.crm.dto.course.CourseCreateRequest
import com.covenantcode.crm.dto.course.CourseResponse
import com.covenantcode.crm.dto.course.CourseUpdateRequest
import com.covenantcode.crm.entity.enums.CourseStatus
import com.covenantcode.crm.service.CourseService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/courses")
@Tag(name = "Courses", description = "Управление учебными курсами")
@SecurityRequirement(name = "bearerAuth")
class CourseController(private val courseService: CourseService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Создать курс (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Курс успешно создан"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    )
    fun create(@Valid @RequestBody request: CourseCreateRequest): CourseResponse =
        courseService.create(request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить курс (только ADMIN)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Курс успешно удалён"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Курс не найден"),
        ApiResponse(responseCode = "409", description = "У курса есть активные учебные группы"),
    )
    fun delete(@PathVariable id: Long) = courseService.delete(id)

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Обновить курс полностью (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Курс успешно обновлён"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Курс не найден"),
    )
    fun update(@PathVariable id: Long, @Valid @RequestBody request: CourseUpdateRequest): CourseResponse =
        courseService.update(id, request)

    @GetMapping("/{id}")
    @Operation(summary = "Получить курс по ID (все авторизованные)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Курс найден"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "404", description = "Курс не найден"),
    )
    fun getById(@PathVariable id: Long): CourseResponse = courseService.getById(id)

    @GetMapping
    @Operation(summary = "Список курсов с фильтрацией по статусу (все авторизованные)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список курсов"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
    )
    fun getAll(
        @RequestParam(required = false) status: CourseStatus?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<CourseResponse> = courseService.getAll(status, pageable)
}
