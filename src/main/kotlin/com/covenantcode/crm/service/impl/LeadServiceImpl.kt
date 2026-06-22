package com.covenantcode.crm.service.impl

import com.covenantcode.crm.dto.lead.LeadCommentCreateRequest
import com.covenantcode.crm.dto.lead.LeadCommentResponse
import com.covenantcode.crm.dto.lead.LeadConvertRequest
import com.covenantcode.crm.dto.lead.LeadCreateRequest
import com.covenantcode.crm.dto.lead.LeadResponse
import com.covenantcode.crm.dto.lead.LeadStatusUpdateRequest
import com.covenantcode.crm.dto.lead.LeadUpdateRequest
import com.covenantcode.crm.dto.student.StudentResponse
import com.covenantcode.crm.entity.Lead
import com.covenantcode.crm.entity.LeadComment
import com.covenantcode.crm.entity.Student
import com.covenantcode.crm.entity.enums.LeadStatus
import com.covenantcode.crm.exception.BadRequestException
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.mapper.toResponse
import com.covenantcode.crm.repository.CourseRepository
import com.covenantcode.crm.repository.LeadCommentRepository
import com.covenantcode.crm.repository.LeadRepository
import com.covenantcode.crm.repository.LeadSpecifications
import com.covenantcode.crm.repository.StudentRepository
import com.covenantcode.crm.repository.UserRepository
import com.covenantcode.crm.service.LeadService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeadServiceImpl(
    private val leadRepository: LeadRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    private val leadCommentRepository: LeadCommentRepository,
    private val studentRepository: StudentRepository,
) : LeadService {

    @Transactional
    override fun convertToStudent(leadId: Long, request: LeadConvertRequest): StudentResponse {
        val lead = leadRepository.findByIdOrNull(leadId)
            ?: throw ResourceNotFoundException("Lead", leadId)
        if (lead.status == LeadStatus.CONVERTED_TO_STUDENT) {
            throw ConflictException("Лид с id $leadId уже был конвертирован в студента")
        }
        val student = studentRepository.save(Student().apply {
            firstName = request.firstName!!
            lastName = request.lastName!!
            phone = request.phone
            email = request.email
            birthDate = request.birthDate
        })
        lead.status = LeadStatus.CONVERTED_TO_STUDENT
        lead.convertedStudent = student
        leadRepository.save(lead)
        return student.toResponse()
    }

    @Transactional(readOnly = true)
    override fun getComments(leadId: Long): List<LeadCommentResponse> {
        if (!leadRepository.existsById(leadId)) throw ResourceNotFoundException("Lead", leadId)
        return leadCommentRepository.findByLeadIdOrderByCreatedAtAsc(leadId).map { it.toResponse() }
    }

    @Transactional
    override fun addComment(leadId: Long, request: LeadCommentCreateRequest, authorId: Long): LeadCommentResponse {
        val lead = leadRepository.findByIdOrNull(leadId)
            ?: throw ResourceNotFoundException("Lead", leadId)
        val author = userRepository.findByIdOrNull(authorId)
            ?: throw ResourceNotFoundException("User", authorId)
        val comment = LeadComment().apply {
            this.lead = lead
            this.author = author
            this.text = request.text!!
        }
        return leadCommentRepository.save(comment).toResponse()
    }

    @Transactional
    override fun create(request: LeadCreateRequest): LeadResponse {
        val course = request.interestedCourseId?.let {
            courseRepository.findByIdOrNull(it) ?: throw ResourceNotFoundException("Course", it)
        }
        val manager = request.assignedManagerId?.let {
            userRepository.findByIdOrNull(it) ?: throw ResourceNotFoundException("User", it)
        }
        val lead = Lead().apply {
            firstName = request.firstName
            lastName = request.lastName
            phone = request.phone
            email = request.email
            source = request.source
            comment = request.comment
            status = LeadStatus.NEW
            interestedCourse = course
            assignedManager = manager
        }
        return leadRepository.save(lead).toResponse()
    }

    @Transactional
    override fun updateStatus(id: Long, request: LeadStatusUpdateRequest): LeadResponse {
        val lead = leadRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Lead", id)
        if (request.status == LeadStatus.CONVERTED_TO_STUDENT) {
            throw ConflictException(
                "Статус CONVERTED_TO_STUDENT нельзя установить вручную. Используйте POST /api/v1/leads/$id/convert"
            )
        }
        lead.status = request.status!!
        return leadRepository.save(lead).toResponse()
    }

    @Transactional
    override fun update(id: Long, request: LeadUpdateRequest): LeadResponse {
        val lead = leadRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Lead", id)
        if (lead.status == LeadStatus.CONVERTED_TO_STUDENT) {
            throw BadRequestException("Нельзя редактировать конвертированного лида")
        }
        val course = request.interestedCourseId?.let {
            courseRepository.findByIdOrNull(it) ?: throw ResourceNotFoundException("Course", it)
        }
        val manager = request.assignedManagerId?.let {
            userRepository.findByIdOrNull(it) ?: throw ResourceNotFoundException("User", it)
        }
        lead.firstName = request.firstName
        lead.lastName = request.lastName
        lead.phone = request.phone
        lead.email = request.email
        lead.source = request.source
        lead.comment = request.comment
        lead.interestedCourse = course
        lead.assignedManager = manager
        return leadRepository.save(lead).toResponse()
    }

    @Transactional(readOnly = true)
    override fun getById(id: Long): LeadResponse =
        (leadRepository.findByIdOrNull(id) ?: throw ResourceNotFoundException("Lead", id)).toResponse()

    @Transactional(readOnly = true)
    override fun getAll(
        status: LeadStatus?,
        managerId: Long?,
        courseId: Long?,
        search: String?,
        pageable: Pageable,
    ): Page<LeadResponse> {
        var spec = Specification.where<Lead>(null)
        if (status != null) spec = spec.and(LeadSpecifications.byStatus(status))
        if (managerId != null) spec = spec.and(LeadSpecifications.byManager(managerId))
        if (courseId != null) spec = spec.and(LeadSpecifications.byCourse(courseId))
        if (search != null) spec = spec.and(LeadSpecifications.bySearch(search))
        return leadRepository.findAll(spec, pageable).map { it.toResponse() }
    }
}
