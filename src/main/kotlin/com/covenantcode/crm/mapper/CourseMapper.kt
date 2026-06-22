package com.covenantcode.crm.mapper

import com.covenantcode.crm.dto.course.CourseCreateRequest
import com.covenantcode.crm.dto.course.CourseResponse
import com.covenantcode.crm.entity.Course
import com.covenantcode.crm.entity.enums.CourseStatus

fun CourseCreateRequest.toEntity(status: CourseStatus): Course = Course().apply {
    title = this@toEntity.title
    description = this@toEntity.description
    durationInWeeks = this@toEntity.durationInWeeks!!
    price = this@toEntity.price!!
    this.status = status
}

fun Course.toResponse() = CourseResponse(
    id = id!!,
    title = title,
    description = description,
    durationInWeeks = durationInWeeks,
    price = price,
    status = status.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
