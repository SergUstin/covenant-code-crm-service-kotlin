package com.covenantcode.crm.mapper

import com.covenantcode.crm.dto.user.UserResponse
import com.covenantcode.crm.entity.User

fun User.toResponse() = UserResponse(
    id = id!!,
    firstName = firstName,
    lastName = lastName,
    email = email,
    phone = phone,
    role = role.name.name,
    enabled = enabled,
    createdAt = createdAt,
)
