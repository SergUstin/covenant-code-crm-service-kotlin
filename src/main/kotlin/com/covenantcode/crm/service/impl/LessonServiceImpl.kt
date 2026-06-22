package com.covenantcode.crm.service.impl

import com.covenantcode.crm.dto.lesson.LessonCreateRequest
import com.covenantcode.crm.dto.lesson.LessonResponse
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest
import com.covenantcode.crm.entity.Lesson
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.exception.BadRequestException
import com.covenantcode.crm.exception.ForbiddenException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.mapper.toResponse
import com.covenantcode.crm.repository.LessonRepository
import com.covenantcode.crm.repository.LessonSpecifications
import com.covenantcode.crm.repository.StudentRepository
import com.covenantcode.crm.repository.StudyGroupRepository
import com.covenantcode.crm.repository.UserRepository
import com.covenantcode.crm.service.LessonOverlapService
import com.covenantcode.crm.service.LessonService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class LessonServiceImpl(
    private val lessonRepository: LessonRepository,
    private val studyGroupRepository: StudyGroupRepository,
    private val userRepository: UserRepository,
    private val studentRepository: StudentRepository,
    private val lessonOverlapService: LessonOverlapService,
) : LessonService {

    @Transactional
    override fun create(request: LessonCreateRequest): LessonResponse {
        val group = studyGroupRepository.findByIdOrNull(request.groupId!!)
            ?: throw ResourceNotFoundException("StudyGroup", request.groupId)

        if (group.status != GroupStatus.ACTIVE) {
            throw BadRequestException("Занятия можно создавать только для активных групп")
        }

        val teacher = userRepository.findByIdOrNull(request.teacherId!!)
            ?: throw ResourceNotFoundException("User", request.teacherId)

        if (!request.endTime!!.isAfter(request.startTime!!)) {
            throw BadRequestException("Время окончания должно быть позже времени начала")
        }

        lessonOverlapService.checkTeacherOverlap(
            teacherId = teacher.id!!,
            lessonDate = request.lessonDate!!,
            startTime = request.startTime,
            endTime = request.endTime,
            excludeLessonId = null,
        )

        val lesson = lessonRepository.save(Lesson().apply {
            studyGroup = group
            this.teacher = teacher
            topic = request.topic!!
            description = request.description
            lessonDate = request.lessonDate
            startTime = request.startTime
            endTime = request.endTime
        })

        return lesson.toResponse()
    }

    @Transactional(readOnly = true)
    override fun getAll(
        groupId: Long?,
        teacherId: Long?,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        pageable: Pageable,
    ): Page<LessonResponse> {
        val spec = Specification.where(LessonSpecifications.byGroup(groupId))
            .and(LessonSpecifications.byTeacher(teacherId))
            .and(LessonSpecifications.fromDate(dateFrom))
            .and(LessonSpecifications.toDate(dateTo))
        return lessonRepository.findAll(spec, pageable).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    override fun getById(id: Long, currentUser: User): LessonResponse {
        val lesson = lessonRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Lesson", id)
        when (currentUser.role.name) {
            RoleName.ADMIN, RoleName.MANAGER -> Unit
            RoleName.TEACHER -> {
                if (lesson.teacher.id != currentUser.id) throw AccessDeniedException("Access Denied")
            }
            RoleName.STUDENT -> {
                val student = studentRepository.findByUser_Id(currentUser.id!!)
                    ?: throw AccessDeniedException("Access Denied")
                if (!lesson.studyGroup.students.contains(student)) throw AccessDeniedException("Access Denied")
            }
        }
        return lesson.toResponse()
    }

    @Transactional
    override fun update(id: Long, request: LessonUpdateRequest): LessonResponse {
        val lesson = lessonRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Lesson", id)

        if (lesson.studyGroup.status == GroupStatus.COMPLETED) {
            throw BadRequestException("Нельзя редактировать занятия завершённой группы")
        }

        val newGroup = studyGroupRepository.findByIdOrNull(request.groupId!!)
            ?: throw ResourceNotFoundException("StudyGroup", request.groupId)

        if (newGroup.status != GroupStatus.ACTIVE) {
            throw BadRequestException("Занятия можно создавать только для активных групп")
        }

        val teacher = userRepository.findByIdOrNull(request.teacherId!!)
            ?: throw ResourceNotFoundException("User", request.teacherId)

        if (!request.endTime!!.isAfter(request.startTime!!)) {
            throw BadRequestException("Время окончания должно быть позже времени начала")
        }

        lessonOverlapService.checkTeacherOverlap(
            teacherId = teacher.id!!,
            lessonDate = request.lessonDate!!,
            startTime = request.startTime,
            endTime = request.endTime,
            excludeLessonId = id,
        )

        lesson.studyGroup = newGroup
        lesson.teacher = teacher
        lesson.topic = request.topic!!
        lesson.description = request.description
        lesson.lessonDate = request.lessonDate
        lesson.startTime = request.startTime
        lesson.endTime = request.endTime

        return lessonRepository.save(lesson).toResponse()
    }

    @Transactional
    override fun delete(id: Long) {
        val lesson = lessonRepository.findByIdOrNull(id)
            ?: throw ResourceNotFoundException("Lesson", id)

        if (lesson.studyGroup.status == GroupStatus.COMPLETED) {
            throw BadRequestException("Нельзя удалить занятие завершённой группы")
        }

        lessonRepository.delete(lesson)
    }

    @Transactional(readOnly = true)
    override fun getLessonsByGroup(groupId: Long, currentUser: User): List<LessonResponse> {
        val group = studyGroupRepository.findByIdOrNull(groupId)
            ?: throw ResourceNotFoundException("StudyGroup", groupId)

        when (currentUser.role.name) {
            RoleName.ADMIN, RoleName.MANAGER -> Unit
            RoleName.TEACHER -> {
                if (group.teacher.id != currentUser.id)
                    throw ForbiddenException("Нет доступа к расписанию этой группы")
            }
            RoleName.STUDENT -> {
                val student = studentRepository.findByUser_Id(currentUser.id!!)
                    ?: throw ForbiddenException("Нет доступа к расписанию этой группы")
                if (!group.students.contains(student))
                    throw ForbiddenException("Нет доступа к расписанию этой группы")
            }
        }

        return lessonRepository.findByStudyGroupIdOrderByLessonDateAscStartTimeAsc(groupId)
            .map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    override fun getLessonsByTeacher(
        teacherId: Long,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        currentUser: User,
    ): List<LessonResponse> {
        userRepository.findByIdOrNull(teacherId)
            ?: throw ResourceNotFoundException("User", teacherId)

        if (currentUser.role.name == RoleName.TEACHER && currentUser.id != teacherId) {
            throw ForbiddenException("Нет доступа к расписанию этого преподавателя")
        }

        val spec = Specification.where(LessonSpecifications.byTeacher(teacherId))
            .and(LessonSpecifications.fromDate(dateFrom))
            .and(LessonSpecifications.toDate(dateTo))
        val sort = Sort.by("lessonDate").ascending().and(Sort.by("startTime").ascending())

        return lessonRepository.findAll(spec, sort).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    override fun getLessonsByStudent(
        studentId: Long,
        dateFrom: LocalDate?,
        dateTo: LocalDate?,
        currentUser: User,
    ): List<LessonResponse> {
        val student = studentRepository.findByIdOrNull(studentId)
            ?: throw ResourceNotFoundException("Student", studentId)

        if (currentUser.role.name == RoleName.STUDENT && student.user?.id != currentUser.id) {
            throw ForbiddenException("Нет доступа к расписанию этого студента")
        }

        val spec = Specification.where(LessonSpecifications.byStudent(studentId))
            .and(LessonSpecifications.fromDate(dateFrom))
            .and(LessonSpecifications.toDate(dateTo))
        val sort = Sort.by("lessonDate").ascending().and(Sort.by("startTime").ascending())

        return lessonRepository.findAll(spec, sort).map { it.toResponse() }
    }
}
