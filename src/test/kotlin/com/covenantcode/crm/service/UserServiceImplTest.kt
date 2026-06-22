package com.covenantcode.crm.service

import com.covenantcode.crm.entity.Role
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.exception.BadRequestException
import com.covenantcode.crm.exception.ForbiddenException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.repository.UserRepository
import com.covenantcode.crm.service.impl.UserServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class UserServiceImplTest {

    @Mock private lateinit var userRepository: UserRepository
    @InjectMocks private lateinit var userService: UserServiceImpl

    private fun makeUser(id: Long, email: String, roleName: RoleName): User {
        val role = Role().apply { this.id = id; name = roleName }
        return User().apply {
            this.id = id
            firstName = "User$id"
            lastName = "Test"
            this.email = email
            passwordHash = "hash"
            this.role = role
            enabled = true
        }
    }

    @Test
    fun `getAll - возвращает страницу с пользователями и корректным role в виде строки`() {
        val pageable = PageRequest.of(0, 20)
        val users = listOf(
            makeUser(1L, "admin@test.com", RoleName.ADMIN),
            makeUser(2L, "manager@test.com", RoleName.MANAGER),
        )
        whenever(userRepository.findAll(any<org.springframework.data.domain.Pageable>()))
            .thenReturn(PageImpl(users, pageable, 2))

        val result = userService.getAll(pageable)

        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.content[0].role).isEqualTo("ADMIN")
        assertThat(result.content[1].role).isEqualTo("MANAGER")
        assertThat(result.content[0].email).isEqualTo("admin@test.com")
    }

    @Test
    fun `getAll - возвращает пустую страницу когда пользователей нет`() {
        val pageable = PageRequest.of(0, 20)
        whenever(userRepository.findAll(any<org.springframework.data.domain.Pageable>()))
            .thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = userService.getAll(pageable)

        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
    }

    @Test
    fun `getUserById - ADMIN получает профиль любого пользователя`() {
        val admin = makeUser(1L, "admin@test.com", RoleName.ADMIN)
        val target = makeUser(2L, "manager@test.com", RoleName.MANAGER)
        whenever(userRepository.findById(2L)).thenReturn(Optional.of(target))
        whenever(userRepository.findById(1L)).thenReturn(Optional.of(admin))

        val result = userService.getUserById(2L, 1L)

        assertThat(result.id).isEqualTo(2L)
        assertThat(result.email).isEqualTo("manager@test.com")
    }

    @Test
    fun `getUserById - пользователь получает свой профиль`() {
        val manager = makeUser(5L, "mgr@test.com", RoleName.MANAGER)
        whenever(userRepository.findById(5L)).thenReturn(Optional.of(manager))

        val result = userService.getUserById(5L, 5L)

        assertThat(result.id).isEqualTo(5L)
    }

    @Test
    fun `getUserById - MANAGER запрашивает чужой профиль - бросает ForbiddenException`() {
        val manager = makeUser(5L, "mgr@test.com", RoleName.MANAGER)
        val other = makeUser(7L, "other@test.com", RoleName.MANAGER)
        whenever(userRepository.findById(7L)).thenReturn(Optional.of(other))
        whenever(userRepository.findById(5L)).thenReturn(Optional.of(manager))

        assertThatThrownBy { userService.getUserById(7L, 5L) }
            .isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `getUserById - несуществующий ID бросает ResourceNotFoundException`() {
        whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

        assertThatThrownBy { userService.getUserById(999L, 1L) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `updateEnabled - успешная блокировка пользователя`() {
        val target = makeUser(2L, "mgr@test.com", RoleName.MANAGER).apply { enabled = true }
        val saved = makeUser(2L, "mgr@test.com", RoleName.MANAGER).apply { enabled = false }
        whenever(userRepository.findById(2L)).thenReturn(Optional.of(target))
        whenever(userRepository.save(any<User>())).thenReturn(saved)

        val result = userService.updateEnabled(2L, false, 1L)

        assertThat(result.enabled).isFalse()
        verify(userRepository).save(any<User>())
    }

    @Test
    fun `updateEnabled - самоблокировка бросает BadRequestException без обращения к БД`() {
        assertThatThrownBy { userService.updateEnabled(1L, false, 1L) }
            .isInstanceOf(BadRequestException::class.java)
        verify(userRepository, never()).findById(any())
    }

    @Test
    fun `updateEnabled - несуществующий ID бросает ResourceNotFoundException`() {
        whenever(userRepository.findById(999L)).thenReturn(Optional.empty())

        assertThatThrownBy { userService.updateEnabled(999L, false, 1L) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `updateEnabled - разблокировка уже активного пользователя идемпотентна`() {
        val target = makeUser(2L, "mgr@test.com", RoleName.MANAGER).apply { enabled = true }
        whenever(userRepository.findById(2L)).thenReturn(Optional.of(target))
        whenever(userRepository.save(any<User>())).thenReturn(target)

        val result = userService.updateEnabled(2L, true, 1L)

        assertThat(result.enabled).isTrue()
        verify(userRepository).save(any<User>())
    }
}
