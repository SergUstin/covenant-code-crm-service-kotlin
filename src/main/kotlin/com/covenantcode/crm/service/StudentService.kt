package com.covenantcode.crm.service

import com.covenantcode.crm.dto.student.StudentCreateRequest
import com.covenantcode.crm.dto.student.StudentResponse
import com.covenantcode.crm.dto.student.StudentUpdateRequest
import com.covenantcode.crm.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface StudentService {
    fun create(request: StudentCreateRequest): StudentResponse
    fun getAll(search: String?, pageable: Pageable): Page<StudentResponse>
    fun getById(id: Long, currentUser: User): StudentResponse
    fun update(id: Long, request: StudentUpdateRequest): StudentResponse
    fun deleteById(id: Long)
}
