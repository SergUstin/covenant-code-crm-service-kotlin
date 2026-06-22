package com.covenantcode.crm.service

import com.covenantcode.crm.dto.lesson.LessonCreateRequest
import com.covenantcode.crm.dto.lesson.LessonResponse
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest
import com.covenantcode.crm.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDate

interface LessonService {
    fun create(request: LessonCreateRequest): LessonResponse
    fun getAll(groupId: Long?, teacherId: Long?, dateFrom: LocalDate?, dateTo: LocalDate?, pageable: Pageable): Page<LessonResponse>
    fun getById(id: Long, currentUser: User): LessonResponse
    fun update(id: Long, request: LessonUpdateRequest): LessonResponse
    fun delete(id: Long)
    fun getLessonsByGroup(groupId: Long, currentUser: User): List<LessonResponse>
    fun getLessonsByTeacher(teacherId: Long, dateFrom: LocalDate?, dateTo: LocalDate?, currentUser: User): List<LessonResponse>
    fun getLessonsByStudent(studentId: Long, dateFrom: LocalDate?, dateTo: LocalDate?, currentUser: User): List<LessonResponse>
}
