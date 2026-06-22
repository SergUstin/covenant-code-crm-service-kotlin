package com.covenantcode.crm.controller

import com.covenantcode.crm.dto.lesson.LessonCreateRequest
import com.covenantcode.crm.dto.lesson.LessonResponse
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.service.LessonService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
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
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/lessons")
@Tag(name = "Lessons", description = "Управление занятиями")
@SecurityRequirement(name = "bearerAuth")
class LessonController(private val lessonService: LessonService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Создать занятие (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Занятие создано"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации, некорректное время или группа не ACTIVE"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Группа или преподаватель не найдены"),
        ApiResponse(responseCode = "409", description = "Пересечение занятий у преподавателя"),
    )
    fun create(@Valid @RequestBody request: LessonCreateRequest): LessonResponse =
        lessonService.create(request)

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Получить занятие по ID (ADMIN, MANAGER, TEACHER — своё, STUDENT — своей группы)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Данные занятия"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Занятие не найдено"),
    )
    fun getById(
        @PathVariable id: Long,
        @AuthenticationPrincipal currentUser: User,
    ): LessonResponse = lessonService.getById(id, currentUser)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Удалить занятие (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "204", description = "Занятие удалено"),
        ApiResponse(responseCode = "400", description = "Нельзя удалить занятие завершённой группы"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Занятие не найдено"),
    )
    fun delete(@PathVariable id: Long) = lessonService.delete(id)

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Обновить занятие (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Занятие обновлено"),
        ApiResponse(responseCode = "400", description = "Некорректные данные или группа в статусе COMPLETED"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Занятие, группа или преподаватель не найдены"),
        ApiResponse(responseCode = "409", description = "Пересечение занятий у преподавателя"),
    )
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: LessonUpdateRequest,
    ): LessonResponse = lessonService.update(id, request)

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Список занятий (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список занятий"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    )
    fun getAll(
        @RequestParam(required = false) groupId: Long?,
        @RequestParam(required = false) teacherId: Long?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateTo: LocalDate?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<LessonResponse> = lessonService.getAll(groupId, teacherId, dateFrom, dateTo, pageable)
}
