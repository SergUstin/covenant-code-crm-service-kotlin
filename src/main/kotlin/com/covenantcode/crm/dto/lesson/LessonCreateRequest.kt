package com.covenantcode.crm.dto.lesson

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.LocalTime

data class LessonCreateRequest(
    @field:NotNull(message = "ID группы обязателен")
    val groupId: Long?,
    @field:NotNull(message = "ID преподавателя обязателен")
    val teacherId: Long?,
    @field:NotBlank(message = "Тема занятия обязательна")
    @field:Size(max = 255)
    val topic: String?,
    @field:Size(max = 1000)
    val description: String?,
    @field:NotNull(message = "Дата занятия обязательна")
    val lessonDate: LocalDate?,
    @field:NotNull(message = "Время начала обязательно")
    val startTime: LocalTime?,
    @field:NotNull(message = "Время окончания обязательно")
    val endTime: LocalTime?,
)
