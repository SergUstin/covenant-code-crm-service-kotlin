package com.covenantcode.crm.service.impl

import com.covenantcode.crm.dto.auth.AuthResponse
import com.covenantcode.crm.dto.auth.LoginRequest
import com.covenantcode.crm.dto.auth.RegisterRequest
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.exception.UnauthorizedException
import com.covenantcode.crm.repository.RoleRepository
import com.covenantcode.crm.repository.UserRepository
import com.covenantcode.crm.security.JwtService
import com.covenantcode.crm.service.AuthService
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.AuthenticationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthServiceImpl(
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val authenticationManager: AuthenticationManager,
) : AuthService {

    @Transactional
    override fun register(request: RegisterRequest): AuthResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw ConflictException("Пользователь с email ${request.email} уже существует")
        }

        val managerRole = roleRepository.findByName(RoleName.MANAGER)
            ?: throw ResourceNotFoundException("Роль MANAGER не найдена в БД")

        val user = User().apply {
            firstName = request.firstName
            lastName = request.lastName
            email = request.email
            passwordHash = passwordEncoder.encode(request.password)
            phone = request.phone
            role = managerRole
            enabled = true
        }

        val saved = userRepository.save(user)
        val token = jwtService.generateToken(saved)

        return AuthResponse(
            token = token,
            userId = saved.id!!,
            email = saved.email,
            firstName = saved.firstName,
            lastName = saved.lastName,
            role = saved.role.name.name,
        )
    }

    @Transactional(readOnly = true)
    override fun login(request: LoginRequest): AuthResponse {
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.email, request.password)
            )
        } catch (ex: AuthenticationException) {
            throw UnauthorizedException("Неверный email или пароль")
        }

        val user = userRepository.findByEmail(request.email)
            ?: throw ResourceNotFoundException("Пользователь не найден: ${request.email}")

        val token = jwtService.generateToken(user)

        return AuthResponse(
            token = token,
            userId = user.id!!,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role.name.name,
        )
    }
}
