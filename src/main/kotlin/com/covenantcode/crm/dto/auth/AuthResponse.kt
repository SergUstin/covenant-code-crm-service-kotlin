package com.covenantcode.crm.dto.auth

data class AuthResponse(
    val token: String,
    val userId: Long,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
)
