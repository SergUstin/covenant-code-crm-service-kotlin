package com.covenantcode.crm.dto.lead

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class LeadConvertRequest(
    @field:NotBlank(message = "Имя обязательно")
    @field:Size(max = 100, message = "Имя не может превышать 100 символов")
    val firstName: String?,

    @field:NotBlank(message = "Фамилия обязательна")
    @field:Size(max = 100, message = "Фамилия не может превышать 100 символов")
    val lastName: String?,

    @field:NotBlank(message = "Телефон обязателен")
    @field:Size(max = 20, message = "Телефон не может превышать 20 символов")
    val phone: String?,

    @field:Email(message = "Некорректный формат email")
    @field:Size(max = 255, message = "Email не может превышать 255 символов")
    val email: String? = null,

    val birthDate: LocalDate? = null,
)
