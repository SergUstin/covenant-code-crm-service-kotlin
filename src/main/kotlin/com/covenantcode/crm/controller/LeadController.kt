package com.covenantcode.crm.controller

import com.covenantcode.crm.dto.lead.LeadCommentCreateRequest
import com.covenantcode.crm.dto.lead.LeadCommentResponse
import com.covenantcode.crm.dto.lead.LeadConvertRequest
import com.covenantcode.crm.dto.lead.LeadCreateRequest
import com.covenantcode.crm.dto.lead.LeadResponse
import com.covenantcode.crm.dto.lead.LeadStatusUpdateRequest
import com.covenantcode.crm.dto.lead.LeadUpdateRequest
import com.covenantcode.crm.dto.student.StudentResponse
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.LeadStatus
import com.covenantcode.crm.service.LeadService
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
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/leads")
@Tag(name = "Leads", description = "Управление лидами")
@SecurityRequirement(name = "bearerAuth")
class LeadController(private val leadService: LeadService) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Создать лида (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Лид успешно создан"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Курс или менеджер не найден"),
    )
    fun create(@Valid @RequestBody request: LeadCreateRequest): LeadResponse =
        leadService.create(request)

    @PostMapping("/{id}/convert")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Конвертировать лид в студента (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Студент создан, лид переведён в CONVERTED_TO_STUDENT"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Лид не найден"),
        ApiResponse(responseCode = "409", description = "Лид уже конвертирован"),
    )
    fun convertToStudent(@PathVariable id: Long, @Valid @RequestBody request: LeadConvertRequest): StudentResponse =
        leadService.convertToStudent(id, request)

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Добавить комментарий к лиду (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Комментарий добавлен"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Лид не найден"),
    )
    fun addComment(
        @PathVariable id: Long,
        @Valid @RequestBody request: LeadCommentCreateRequest,
        authentication: Authentication,
    ): LeadCommentResponse {
        val currentUser = authentication.principal as User
        return leadService.addComment(id, request, currentUser.id!!)
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Получить комментарии к лиду (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список комментариев"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Лид не найден"),
    )
    fun getComments(@PathVariable id: Long): List<LeadCommentResponse> =
        leadService.getComments(id)

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Получить лида по ID (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Лид найден"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Лид не найден"),
    )
    fun getById(@PathVariable id: Long): LeadResponse = leadService.getById(id)

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Обновить данные лида (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Лид успешно обновлён"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации или конвертированный лид"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Лид, курс или менеджер не найден"),
    )
    fun update(@PathVariable id: Long, @Valid @RequestBody request: LeadUpdateRequest): LeadResponse =
        leadService.update(id, request)

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Изменить статус лида (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Статус обновлён"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации или неверный статус"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Лид не найден"),
        ApiResponse(responseCode = "409", description = "Попытка установить CONVERTED_TO_STUDENT вручную"),
    )
    fun updateStatus(@PathVariable id: Long, @Valid @RequestBody request: LeadStatusUpdateRequest): LeadResponse =
        leadService.updateStatus(id, request)

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Список лидов с фильтрацией (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список лидов"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    )
    fun getAll(
        @RequestParam(required = false) status: LeadStatus?,
        @RequestParam(required = false) assignedManagerId: Long?,
        @RequestParam(required = false) interestedCourseId: Long?,
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<LeadResponse> = leadService.getAll(status, assignedManagerId, interestedCourseId, search, pageable)
}
