package com.covenantcode.crm.dto.user

import java.time.OffsetDateTime

data class UserResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String?,
    val role: String,
    val enabled: Boolean,
    val createdAt: OffsetDateTime?,
)
