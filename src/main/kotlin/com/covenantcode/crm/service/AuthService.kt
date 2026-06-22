package com.covenantcode.crm.service

import com.covenantcode.crm.dto.auth.AuthResponse
import com.covenantcode.crm.dto.auth.LoginRequest
import com.covenantcode.crm.dto.auth.RegisterRequest

interface AuthService {
    fun register(request: RegisterRequest): AuthResponse
    fun login(request: LoginRequest): AuthResponse
}
