package com.covenantcode.crm.dto.lesson

import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

data class LessonResponse(
    val id: Long,
    val studyGroup: GroupShortResponse,
    val teacher: UserShortResponse,
    val topic: String,
    val description: String?,
    val lessonDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)
