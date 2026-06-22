package com.covenantcode.crm.mapper

import com.covenantcode.crm.dto.student.StudentResponse
import com.covenantcode.crm.entity.Student

fun Student.toResponse() = StudentResponse(
    id = id!!,
    firstName = firstName,
    lastName = lastName,
    phone = phone,
    email = email,
    birthDate = birthDate,
    userId = user?.id,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
