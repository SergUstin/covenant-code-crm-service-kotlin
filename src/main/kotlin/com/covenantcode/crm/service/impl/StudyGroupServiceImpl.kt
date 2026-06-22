package com.covenantcode.crm.service.impl

import com.covenantcode.crm.dto.group.AddStudentToGroupRequest
import com.covenantcode.crm.dto.group.GroupStatusUpdateRequest
import com.covenantcode.crm.dto.group.StudyGroupCreateRequest
import com.covenantcode.crm.dto.group.StudyGroupResponse
import com.covenantcode.crm.dto.group.StudyGroupUpdateRequest
import com.covenantcode.crm.entity.StudyGroup
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.exception.BadRequestException
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.dto.student.StudentResponse
import com.covenantcode.crm.mapper.toResponse
import com.covenantcode.crm.repository.CourseRepository
import com.covenantcode.crm.repository.StudentRepository
import com.covenantcode.crm.repository.StudyGroupRepository
import com.covenantcode.crm.repository.StudyGroupSpecifications
import com.covenantcode.crm.repository.UserRepository
import com.covenantcode.crm.service.StudyGroupService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private val ALLOWED_TRANSITIONS: Map<GroupStatus, Set<GroupStatus>> = mapOf(
    GroupStatus.DRAFT     to setOf(GroupStatus.ACTIVE),
    GroupStatus.ACTIVE    to setOf(GroupStatus.COMPLETED, GroupStatus.CANCELLED),
    GroupStatus.COMPLETED to emptySet(),
    GroupStatus.CANCELLED to emptySet(),
)

@Service
class StudyGroupServiceImpl(
    private val studyGroupRepository: StudyGroupRepository,
    private val courseRepository: CourseRepository,
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
) : StudyGroupService {

    @Transactional(readOnly = true)
    override fun getStudentsOfGroup(groupId: Long, currentUser: User): List<StudentResponse> {
        val group = studyGroupRepository.findByIdOrNull(groupId)
            ?: throw ResourceNotFoundException("StudyGroup", groupId)

        if (currentUser.role.name == RoleName.TEACHER && group.teacher.id != currentUser.id) {
            throw AccessDeniedException("Access Denied")
        }

        return group.students.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    override fun getById(id: Long, currentUser: User): StudyGroupResponse {
        val group = studyGroupRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("StudyGroup", id)
        when (currentUser.role.name) {
            RoleName.ADMIN, RoleName.MANAGER -> Unit
            RoleName.TEACHER -> {
                if (group.teacher.id != currentUser.id) throw AccessDeniedException("Access Denied")
            }
            RoleName.STUDENT -> {
                val student = studentRepository.findByUser_Id(currentUser.id!!)
                    ?: throw AccessDeniedException("Access Denied")
                if (!group.students.contains(student)) throw AccessDeniedException("Access Denied")
            }
        }
        return group.toResponse()
    }

    @Transactional(readOnly = true)
    override fun getAll(courseId: Long?, teacherId: Long?, status: GroupStatus?, pageable: Pageable): Page<StudyGroupResponse> {
        val spec = Specification.where(StudyGroupSpecifications.byCourseId(courseId))
            .and(StudyGroupSpecifications.byTeacherId(teacherId))
            .and(StudyGroupSpecifications.byStatus(status))
        return studyGroupRepository.findAll(spec, pageable).map { it.toResponse() }
    }

    @Transactional
    override fun update(id: Long, request: StudyGroupUpdateRequest): StudyGroupResponse {
        val group = studyGroupRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("StudyGroup", id)

        if (group.status == GroupStatus.COMPLETED || group.status == GroupStatus.CANCELLED) {
            throw BadRequestException("Нельзя редактировать группу в статусе ${group.status}")
        }

        val course = courseRepository.findByIdOrNull(request.courseId!!)
            ?: throw ResourceNotFoundException("Course", request.courseId)

        val teacher = userRepository.findByIdOrNull(request.teacherId!!)
            ?: throw ResourceNotFoundException("User", request.teacherId)

        if (teacher.role.name != RoleName.TEACHER) {
            throw BadRequestException("Пользователь с id ${request.teacherId} не является учителем")
        }

        group.name = request.name!!
        group.course = course
        group.teacher = teacher
        group.startDate = request.startDate!!

        return studyGroupRepository.save(group).toResponse()
    }

    @Transactional
    override fun addStudent(groupId: Long, request: AddStudentToGroupRequest): StudyGroupResponse {
        val group = studyGroupRepository.findByIdOrNull(groupId)
            ?: throw ResourceNotFoundException("StudyGroup", groupId)

        if (group.status == GroupStatus.COMPLETED || group.status == GroupStatus.CANCELLED) {
            throw BadRequestException("Нельзя добавить студента в группу в статусе ${group.status}")
        }

        val student = studentRepository.findByIdOrNull(request.studentId!!)
            ?: throw ResourceNotFoundException("Student", request.studentId)

        if (group.students.contains(student)) {
            throw ConflictException("Студент с id ${request.studentId} уже состоит в этой группе")
        }

        group.students.add(student)
        return studyGroupRepository.save(group).toResponse()
    }

    @Transactional
    override fun removeStudent(groupId: Long, studentId: Long) {
        val group = studyGroupRepository.findByIdOrNull(groupId)
            ?: throw ResourceNotFoundException("StudyGroup", groupId)

        if (group.status == GroupStatus.COMPLETED) {
            throw BadRequestException("Нельзя удалить студента из группы в статусе COMPLETED")
        }

        val student = studentRepository.findByIdOrNull(studentId)
            ?: throw ResourceNotFoundException("Student", studentId)

        if (!group.students.contains(student)) {
            throw BadRequestException("Студент с id $studentId не состоит в этой группе")
        }

        group.students.remove(student)
        studyGroupRepository.save(group)
    }

    @Transactional
    override fun updateStatus(id: Long, request: GroupStatusUpdateRequest): StudyGroupResponse {
        val group = studyGroupRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("StudyGroup", id)
        val newStatus = request.status!!
        if (!ALLOWED_TRANSITIONS.getValue(group.status).contains(newStatus)) {
            throw BadRequestException("Переход из ${group.status} в $newStatus недопустим")
        }
        group.status = newStatus
        return studyGroupRepository.save(group).toResponse()
    }

    @Transactional
    override fun create(request: StudyGroupCreateRequest): StudyGroupResponse {
        val course = courseRepository.findByIdOrNull(request.courseId!!)
            ?: throw ResourceNotFoundException("Course", request.courseId)

        val teacher = userRepository.findByIdOrNull(request.teacherId!!)
            ?: throw ResourceNotFoundException("User", request.teacherId)

        if (teacher.role.name != RoleName.TEACHER) {
            throw BadRequestException("Пользователь с id ${request.teacherId} не является учителем")
        }

        val students = request.studentIds
            ?.map { studentId ->
                studentRepository.findByIdOrNull(studentId)
                    ?: throw ResourceNotFoundException("Student", studentId)
            }
            ?.toMutableSet() ?: mutableSetOf()

        val group = studyGroupRepository.save(StudyGroup().apply {
            name = request.name!!
            this.course = course
            this.teacher = teacher
            startDate = request.startDate!!
            status = GroupStatus.DRAFT
            this.students = students
        })

        return group.toResponse()
    }
}
