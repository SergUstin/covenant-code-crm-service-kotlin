package com.covenantcode.crm.dto.course

import com.covenantcode.crm.entity.enums.CourseStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CourseCreateRequest(
    @field:NotBlank(message = "Название курса обязательно")
    @field:Size(max = 255, message = "Название не должно превышать 255 символов")
    @Schema(example = "Java для начинающих")
    val title: String,

    @Schema(example = "Полный курс по Java с нуля до уровня Junior.")
    val description: String? = null,

    @field:NotNull(message = "Продолжительность обязательна")
    @field:Positive(message = "Продолжительность курса должна быть положительным числом")
    @Schema(example = "16")
    val durationInWeeks: Int?,

    @field:NotNull(message = "Стоимость обязательна")
    @field:PositiveOrZero(message = "Стоимость не может быть отрицательной")
    @Schema(example = "45000.00")
    val price: BigDecimal?,

    @Schema(example = "ACTIVE")
    val status: CourseStatus? = null,
)
