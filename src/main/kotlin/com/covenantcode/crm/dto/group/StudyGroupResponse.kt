package com.covenantcode.crm.dto.group

import com.covenantcode.crm.entity.enums.GroupStatus
import java.time.LocalDate
import java.time.OffsetDateTime

data class StudyGroupResponse(
    val id: Long,
    val name: String,
    val course: CourseShortResponse,
    val teacher: UserShortResponse,
    val students: List<StudentShortResponse>,
    val startDate: LocalDate,
    val status: GroupStatus,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)
