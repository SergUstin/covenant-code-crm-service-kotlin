package com.covenantcode.crm.dto.lead

import java.time.OffsetDateTime

data class LeadResponse(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val phone: String,
    val email: String?,
    val source: String?,
    val interestedCourse: CourseShortResponse?,
    val status: String,
    val comment: String?,
    val assignedManager: UserShortResponse?,
    val convertedStudentId: Long?,
    val commentsCount: Int = 0,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)
