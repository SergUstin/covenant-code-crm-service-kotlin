package com.covenantcode.crm.service.impl

import com.covenantcode.crm.dto.course.CourseCreateRequest
import com.covenantcode.crm.dto.course.CourseResponse
import com.covenantcode.crm.dto.course.CourseUpdateRequest
import com.covenantcode.crm.entity.enums.CourseStatus
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.mapper.toEntity
import com.covenantcode.crm.mapper.toResponse
import com.covenantcode.crm.repository.CourseRepository
import com.covenantcode.crm.repository.StudyGroupRepository
import com.covenantcode.crm.service.CourseService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CourseServiceImpl(
    private val courseRepository: CourseRepository,
    private val studyGroupRepository: StudyGroupRepository,
) : CourseService {

    @Transactional
    override fun create(request: CourseCreateRequest): CourseResponse {
        val status = request.status ?: CourseStatus.ACTIVE
        val course = request.toEntity(status)
        return courseRepository.save(course).toResponse()
    }

    @Transactional(readOnly = true)
    override fun getById(id: Long): CourseResponse =
        (courseRepository.findByIdOrNull(id) ?: throw ResourceNotFoundException("Course", id)).toResponse()

    @Transactional
    override fun delete(id: Long) {
        courseRepository.findByIdOrNull(id) ?: throw ResourceNotFoundException("Course", id)
        if (studyGroupRepository.existsByCourseIdAndStatus(id, GroupStatus.ACTIVE)) {
            throw ConflictException("Невозможно удалить курс: существуют активные учебные группы")
        }
        courseRepository.deleteById(id)
    }

    @Transactional
    override fun update(id: Long, request: CourseUpdateRequest): CourseResponse {
        val course = courseRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Course", id)
        course.title = request.title
        course.description = request.description
        course.durationInWeeks = request.durationInWeeks!!
        course.price = request.price!!
        course.status = request.status!!
        return courseRepository.save(course).toResponse()
    }

    @Transactional(readOnly = true)
    override fun getAll(status: CourseStatus?, pageable: Pageable): Page<CourseResponse> {
        val page = if (status != null) {
            courseRepository.findAllByStatus(status, pageable)
        } else {
            courseRepository.findAll(pageable)
        }
        return page.map { it.toResponse() }
    }
}
