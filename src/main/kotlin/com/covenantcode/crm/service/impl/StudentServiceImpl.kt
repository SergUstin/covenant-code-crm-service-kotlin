package com.covenantcode.crm.service.impl

import com.covenantcode.crm.dto.student.StudentCreateRequest
import com.covenantcode.crm.dto.student.StudentResponse
import com.covenantcode.crm.dto.student.StudentUpdateRequest
import com.covenantcode.crm.entity.Student
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.mapper.toResponse
import com.covenantcode.crm.repository.StudentRepository
import com.covenantcode.crm.repository.StudentSpecifications
import com.covenantcode.crm.repository.StudyGroupRepository
import com.covenantcode.crm.repository.UserRepository
import com.covenantcode.crm.service.StudentService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StudentServiceImpl(
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository,
    private val studyGroupRepository: StudyGroupRepository,
) : StudentService {

    @Transactional(readOnly = true)
    override fun getById(id: Long, currentUser: User): StudentResponse {
        val student = studentRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Student", id)
        when (currentUser.role.name) {
            RoleName.ADMIN, RoleName.MANAGER -> Unit
            RoleName.TEACHER -> {
                if (!studyGroupRepository.existsByTeacherAndStudentsContaining(currentUser, student)) {
                    throw AccessDeniedException("Access Denied")
                }
            }
            RoleName.STUDENT -> {
                if (student.user?.id != currentUser.id) {
                    throw AccessDeniedException("Access Denied")
                }
            }
        }
        return student.toResponse()
    }

    @Transactional(readOnly = true)
    override fun getAll(search: String?, pageable: Pageable): Page<StudentResponse> {
        var spec = Specification.where<Student>(null)
        if (!search.isNullOrBlank()) spec = spec.and(StudentSpecifications.bySearch(search))
        return studentRepository.findAll(spec, pageable).map { it.toResponse() }
    }

    @Transactional
    override fun update(id: Long, request: StudentUpdateRequest): StudentResponse {
        val student = studentRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Student", id)
        student.firstName = request.firstName!!
        student.lastName = request.lastName!!
        student.phone = request.phone
        student.email = request.email
        student.birthDate = request.birthDate
        return studentRepository.save(student).toResponse()
    }

    @Transactional
    override fun deleteById(id: Long) {
        val student = studentRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Student", id)
        if (studyGroupRepository.existsByStudents_IdAndStatus(id, GroupStatus.ACTIVE)) {
            throw ConflictException("Студент с id $id состоит в активной учебной группе и не может быть удалён")
        }
        studentRepository.delete(student)
    }

    @Transactional
    override fun create(request: StudentCreateRequest): StudentResponse {
        val user = request.userId?.let { userId ->
            val u = userRepository.findByIdOrNull(userId)
                ?: throw ResourceNotFoundException("User", userId)
            if (studentRepository.existsByUserId(userId)) {
                throw ConflictException("Пользователь с id $userId уже привязан к другому студенту")
            }
            u
        }
        val student = studentRepository.save(Student().apply {
            firstName = request.firstName!!
            lastName = request.lastName!!
            phone = request.phone
            email = request.email
            birthDate = request.birthDate
            this.user = user
        })
        return student.toResponse()
    }
}
