package com.covenantcode.crm.service

import com.covenantcode.crm.dto.auth.LoginRequest
import com.covenantcode.crm.dto.auth.RegisterRequest
import com.covenantcode.crm.entity.Role
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.exception.UnauthorizedException
import com.covenantcode.crm.repository.RoleRepository
import com.covenantcode.crm.repository.UserRepository
import com.covenantcode.crm.security.JwtService
import com.covenantcode.crm.service.impl.AuthServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.password.PasswordEncoder
@ExtendWith(MockitoExtension::class)
class AuthServiceImplTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var roleRepository: RoleRepository
    @Mock private lateinit var passwordEncoder: PasswordEncoder
    @Mock private lateinit var jwtService: JwtService
    @Mock private lateinit var authenticationManager: AuthenticationManager

    @InjectMocks private lateinit var authService: AuthServiceImpl

    private val validRegisterRequest = RegisterRequest(
        firstName = "Иван",
        lastName = "Петров",
        email = "ivan@test.com",
        password = "password123",
        phone = "+79161234567",
    )

    private val loginRequest = LoginRequest(
        email = "ivan@test.com",
        password = "password123",
    )

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    fun `register - успешная регистрация возвращает токен и роль MANAGER`() {
        val managerRole = Role().apply { id = 1L; name = RoleName.MANAGER }
        val savedUser = User().apply {
            id = 2L
            firstName = validRegisterRequest.firstName
            lastName = validRegisterRequest.lastName
            email = validRegisterRequest.email
            passwordHash = "encoded_password"
            role = managerRole
            enabled = true
        }

        whenever(userRepository.existsByEmail(validRegisterRequest.email)).thenReturn(false)
        whenever(roleRepository.findByName(RoleName.MANAGER)).thenReturn(managerRole)
        whenever(passwordEncoder.encode(validRegisterRequest.password)).thenReturn("encoded_password")
        whenever(userRepository.save(any<User>())).thenReturn(savedUser)
        whenever(jwtService.generateToken(any<User>())).thenReturn("mock.token")

        val response = authService.register(validRegisterRequest)

        assertThat(response.token).isEqualTo("mock.token")
        assertThat(response.role).isEqualTo("MANAGER")
        assertThat(response.email).isEqualTo(validRegisterRequest.email)
        assertThat(response.userId).isEqualTo(2L)
        verify(passwordEncoder).encode(validRegisterRequest.password)
    }

    @Test
    fun `register - дубликат email выбрасывает ConflictException и не сохраняет пользователя`() {
        whenever(userRepository.existsByEmail(validRegisterRequest.email)).thenReturn(true)

        assertThrows<ConflictException> { authService.register(validRegisterRequest) }

        verify(userRepository, never()).save(any<User>())
    }

    @Test
    fun `register - отсутствие роли MANAGER выбрасывает ResourceNotFoundException`() {
        whenever(userRepository.existsByEmail(validRegisterRequest.email)).thenReturn(false)
        whenever(roleRepository.findByName(RoleName.MANAGER)).thenReturn(null)

        assertThrows<ResourceNotFoundException> { authService.register(validRegisterRequest) }
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    fun `login - успешный вход возвращает токен и email`() {
        val managerRole = Role().apply { id = 1L; name = RoleName.MANAGER }
        val user = User().apply {
            id = 2L
            firstName = "Иван"
            lastName = "Петров"
            email = loginRequest.email
            passwordHash = "encoded"
            role = managerRole
            enabled = true
        }
        val authToken = UsernamePasswordAuthenticationToken(loginRequest.email, loginRequest.password)

        whenever(authenticationManager.authenticate(any())).thenReturn(authToken)
        whenever(userRepository.findByEmail(loginRequest.email)).thenReturn(user)
        whenever(jwtService.generateToken(any<User>())).thenReturn("mock.token")

        val response = authService.login(loginRequest)

        assertThat(response.token).isEqualTo("mock.token")
        assertThat(response.email).isEqualTo(loginRequest.email)
    }

    @Test
    fun `login - неверный пароль выбрасывает UnauthorizedException и не обращается к репозиторию`() {
        whenever(authenticationManager.authenticate(any()))
            .thenThrow(BadCredentialsException("Bad credentials"))

        val ex = assertThrows<UnauthorizedException> { authService.login(loginRequest) }

        assertThat(ex.message).isEqualTo("Неверный email или пароль")
        verify(userRepository, never()).findByEmail(any())
    }

    @Test
    fun `login - несуществующий email выбрасывает UnauthorizedException с тем же сообщением`() {
        whenever(authenticationManager.authenticate(any()))
            .thenThrow(UsernameNotFoundException("User not found"))

        val ex = assertThrows<UnauthorizedException> { authService.login(loginRequest) }

        assertThat(ex.message).isEqualTo("Неверный email или пароль")
    }
}
