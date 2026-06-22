package com.covenantcode.crm.controller

import com.covenantcode.crm.dto.lesson.LessonResponse
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.service.LessonService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/groups")
@Tag(name = "Groups", description = "Управление учебными группами")
@SecurityRequirement(name = "bearerAuth")
class StudyGroupLessonsController(private val lessonService: LessonService) {

    @GetMapping("/{groupId}/lessons")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER', 'STUDENT')")
    @Operation(summary = "Расписание занятий группы (ADMIN, MANAGER — любая; TEACHER, STUDENT — только свои)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список занятий группы"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Нет доступа к этой группе"),
        ApiResponse(responseCode = "404", description = "Группа не найдена"),
    )
    fun getLessonsByGroup(
        @PathVariable groupId: Long,
        @AuthenticationPrincipal currentUser: User,
    ): List<LessonResponse> = lessonService.getLessonsByGroup(groupId, currentUser)
}
