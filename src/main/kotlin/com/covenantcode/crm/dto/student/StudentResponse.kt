package com.covenantcode.crm.dto.student

import java.time.LocalDate
import java.time.OffsetDateTime

data class StudentResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val phone: String?,
    val email: String?,
    val birthDate: LocalDate?,
    val userId: Long?,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)
