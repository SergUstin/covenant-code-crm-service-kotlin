package com.covenantcode.crm.service

import com.covenantcode.crm.dto.user.UserResponse
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserService {
    fun getAll(pageable: Pageable): Page<UserResponse>
    fun getUserById(id: Long, currentUserId: Long): UserResponse
    fun updateEnabled(id: Long, enabled: Boolean, currentUserId: Long): UserResponse
}
