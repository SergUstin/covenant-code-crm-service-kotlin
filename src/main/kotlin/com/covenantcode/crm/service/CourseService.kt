package com.covenantcode.crm.service

import com.covenantcode.crm.dto.course.CourseCreateRequest
import com.covenantcode.crm.dto.course.CourseResponse
import com.covenantcode.crm.dto.course.CourseUpdateRequest
import com.covenantcode.crm.entity.enums.CourseStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CourseService {
    fun create(request: CourseCreateRequest): CourseResponse
    fun getAll(status: CourseStatus?, pageable: Pageable): Page<CourseResponse>
    fun getById(id: Long): CourseResponse
    fun update(id: Long, request: CourseUpdateRequest): CourseResponse
    fun delete(id: Long)
}
