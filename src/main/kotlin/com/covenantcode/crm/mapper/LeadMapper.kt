package com.covenantcode.crm.mapper

import com.covenantcode.crm.dto.lead.CourseShortResponse
import com.covenantcode.crm.dto.lead.LeadResponse
import com.covenantcode.crm.dto.lead.UserShortResponse
import com.covenantcode.crm.entity.Lead

fun Lead.toResponse() = LeadResponse(
    id = id!!,
    firstName = firstName,
    lastName = lastName,
    phone = phone,
    email = email,
    source = source,
    interestedCourse = interestedCourse?.let { CourseShortResponse(it.id!!, it.title) },
    status = status.name,
    comment = comment,
    assignedManager = assignedManager?.let { UserShortResponse(it.id!!, it.firstName, it.lastName) },
    convertedStudentId = convertedStudent?.id,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
