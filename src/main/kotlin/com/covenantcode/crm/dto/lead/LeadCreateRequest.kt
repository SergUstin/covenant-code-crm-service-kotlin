package com.covenantcode.crm.dto.lead

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LeadCreateRequest(
    @field:NotBlank(message = "Имя лида обязательно")
    @field:Size(max = 100, message = "Имя не должно превышать 100 символов")
    @Schema(example = "Алексей")
    val firstName: String,

    @field:Size(max = 100, message = "Фамилия не должна превышать 100 символов")
    @Schema(example = "Смирнов")
    val lastName: String? = null,

    @field:NotBlank(message = "Телефон лида обязателен")
    @field:Size(max = 20, message = "Телефон не должен превышать 20 символов")
    @Schema(example = "+79161234567")
    val phone: String,

    @field:Email(message = "Некорректный формат email")
    @field:Size(max = 255, message = "Email не должен превышать 255 символов")
    @Schema(example = "aleksey.smirnov@gmail.com")
    val email: String? = null,

    @field:Size(max = 255, message = "Источник не должен превышать 255 символов")
    @Schema(example = "ВКонтакте")
    val source: String? = null,

    @Schema(example = "1")
    val interestedCourseId: Long? = null,

    @Schema(example = "Интересуется вечерними занятиями")
    val comment: String? = null,

    @Schema(example = "2")
    val assignedManagerId: Long? = null,
)
