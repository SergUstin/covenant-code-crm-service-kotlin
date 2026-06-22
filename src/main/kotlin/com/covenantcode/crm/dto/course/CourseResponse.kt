package com.covenantcode.crm.dto.course

import java.math.BigDecimal
import java.time.OffsetDateTime

data class CourseResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val durationInWeeks: Int,
    val price: BigDecimal,
    val status: String,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)
