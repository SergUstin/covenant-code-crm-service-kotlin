package com.covenantcode.crm.dto.group

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class StudyGroupCreateRequest(
    @field:NotBlank(message = "Название группы обязательно")
    @field:Size(max = 255, message = "Название не может превышать 255 символов")
    val name: String?,

    @field:NotNull(message = "ID курса обязателен")
    val courseId: Long?,

    @field:NotNull(message = "ID учителя обязателен")
    val teacherId: Long?,

    @field:NotNull(message = "Дата начала обязательна")
    val startDate: LocalDate?,

    val studentIds: Set<Long>? = null,
)
