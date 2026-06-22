package com.covenantcode.crm.service.impl

import com.covenantcode.crm.dto.user.UserResponse
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.exception.BadRequestException
import com.covenantcode.crm.exception.ForbiddenException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.mapper.toResponse
import com.covenantcode.crm.repository.UserRepository
import com.covenantcode.crm.service.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class UserServiceImpl(
    private val userRepository: UserRepository,
) : UserService {

    override fun getAll(pageable: Pageable): Page<UserResponse> =
        userRepository.findAll(pageable).map { it.toResponse() }

    override fun getUserById(id: Long, currentUserId: Long): UserResponse {
        val target = userRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("User", id)
        val current = userRepository.findByIdOrNull(currentUserId)
            ?: throw ResourceNotFoundException("User", currentUserId)
        if (current.role.name != RoleName.ADMIN && current.id != id) {
            throw ForbiddenException("Доступ запрещён")
        }
        return target.toResponse()
    }

    @Transactional
    override fun updateEnabled(id: Long, enabled: Boolean, currentUserId: Long): UserResponse {
        if (id == currentUserId) {
            throw BadRequestException("Нельзя заблокировать собственный аккаунт")
        }
        val user = userRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("User", id)
        user.enabled = enabled
        return userRepository.save(user).toResponse()
    }
}
