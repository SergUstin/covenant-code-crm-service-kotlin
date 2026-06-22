package com.covenantcode.crm.controller

import com.covenantcode.crm.dto.lesson.LessonResponse
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.service.LessonService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/teachers")
@Tag(name = "Teachers", description = "Расписание преподавателей")
@SecurityRequirement(name = "bearerAuth")
class TeacherLessonsController(private val lessonService: LessonService) {

    @GetMapping("/{teacherId}/lessons")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEACHER')")
    @Operation(summary = "Расписание преподавателя (ADMIN, MANAGER — любой; TEACHER — только своё)")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Список занятий преподавателя"),
        ApiResponse(responseCode = "401", description = "Токен отсутствует или невалиден"),
        ApiResponse(responseCode = "403", description = "Нет доступа к расписанию этого преподавателя"),
        ApiResponse(responseCode = "404", description = "Преподаватель не найден"),
    )
    fun getLessonsByTeacher(
        @PathVariable teacherId: Long,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) dateTo: LocalDate?,
        @AuthenticationPrincipal currentUser: User,
    ): List<LessonResponse> = lessonService.getLessonsByTeacher(teacherId, dateFrom, dateTo, currentUser)
}
