package com.covenantcode.crm.controller

import com.covenantcode.crm.dto.student.StudentCreateRequest
import com.covenantcode.crm.dto.student.StudentResponse
import com.covenantcode.crm.dto.student.StudentUpdateRequest
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.service.StudentService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/students")
@Tag(name = "Students", description = "Управление студентами")
@SecurityRequirement(name = "bearerAuth")
class StudentController(private val studentService: StudentService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Создать студента (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Студент успешно создан"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Пользователь с userId не найден"),
        ApiResponse(responseCode = "409", description = "userId уже привязан к другому студенту"),
    )
    fun create(@Valid @RequestBody request: StudentCreateRequest): StudentResponse =
        studentService.create(request)

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Обновить студента (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Студент обновлён"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Студент не найден"),
    )
    fun update(@PathVariable id: Long, @Valid @RequestBody request: StudentUpdateRequest): StudentResponse =
        studentService.update(id, request)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Удалить студента (только ADMIN)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Студент удалён"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Студент не найден"),
        ApiResponse(responseCode = "409", description = "Студент состоит в активной учебной группе"),
    )
    fun delete(@PathVariable id: Long) = studentService.deleteById(id)

    @GetMapping("/{id}")
    @Operation(summary = "Получить студента по ID (ADMIN, MANAGER — любой; TEACHER — из своих групп; STUDENT — свой профиль)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Студент найден"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Нет прав на просмотр этого студента"),
        ApiResponse(responseCode = "404", description = "Студент не найден"),
    )
    fun getById(@PathVariable id: Long, @AuthenticationPrincipal currentUser: User): StudentResponse =
        studentService.getById(id, currentUser)

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Список студентов с поиском (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список студентов"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    )
    fun getAll(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<StudentResponse> = studentService.getAll(search, pageable)
}
