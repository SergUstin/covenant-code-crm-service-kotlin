package com.covenantcode.crm.controller

import com.covenantcode.crm.dto.group.GroupStatusUpdateRequest
import com.covenantcode.crm.dto.group.StudyGroupCreateRequest
import com.covenantcode.crm.dto.group.StudyGroupResponse
import com.covenantcode.crm.dto.group.StudyGroupUpdateRequest
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.service.StudyGroupService
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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups", description = "Управление учебными группами")
@SecurityRequirement(name = "bearerAuth")
class StudyGroupController(private val studyGroupService: StudyGroupService) {

    @GetMapping("/{id}")
    @Operation(summary = "Получить группу по ID (ADMIN/MANAGER — любую; TEACHER — свою; STUDENT — в которой состоит)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Группа найдена"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Нет прав на просмотр этой группы"),
        ApiResponse(responseCode = "404", description = "Группа не найдена"),
    )
    fun getById(@PathVariable id: Long, @AuthenticationPrincipal currentUser: User): StudyGroupResponse =
        studyGroupService.getById(id, currentUser)

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Список учебных групп с фильтрацией (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список групп"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
    )
    fun getAll(
        @RequestParam(required = false) courseId: Long?,
        @RequestParam(required = false) teacherId: Long?,
        @RequestParam(required = false) status: GroupStatus?,
        @PageableDefault(size = 20) pageable: Pageable,
    ): Page<StudyGroupResponse> = studyGroupService.getAll(courseId, teacherId, status, pageable)

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Изменить статус группы (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Статус изменён"),
        ApiResponse(responseCode = "400", description = "Переход недопустим или ошибка валидации"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Группа не найдена"),
    )
    fun updateStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: GroupStatusUpdateRequest,
    ): StudyGroupResponse = studyGroupService.updateStatus(id, request)

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Обновить учебную группу (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Группа обновлена"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации, финальный статус группы, или учитель не имеет роли TEACHER"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Группа, курс или учитель не найдены"),
    )
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: StudyGroupUpdateRequest,
    ): StudyGroupResponse = studyGroupService.update(id, request)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Создать учебную группу (ADMIN, MANAGER)")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Группа создана"),
        ApiResponse(responseCode = "400", description = "Ошибка валидации или учитель не имеет роли TEACHER"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        ApiResponse(responseCode = "404", description = "Курс, учитель или студент не найден"),
    )
    fun create(@Valid @RequestBody request: StudyGroupCreateRequest): StudyGroupResponse =
        studyGroupService.create(request)
}
