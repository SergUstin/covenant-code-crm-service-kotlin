package com.covenantcode.crm.service

import com.covenantcode.crm.BaseIntegrationTest
import com.covenantcode.crm.entity.Course
import com.covenantcode.crm.entity.Lesson
import com.covenantcode.crm.entity.Role
import com.covenantcode.crm.entity.StudyGroup
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.repository.CourseRepository
import com.covenantcode.crm.repository.LessonRepository
import com.covenantcode.crm.repository.RoleRepository
import com.covenantcode.crm.repository.StudyGroupRepository
import com.covenantcode.crm.repository.UserRepository
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalTime

class LessonOverlapServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired private lateinit var lessonOverlapService: LessonOverlapService
    @Autowired private lateinit var lessonRepository: LessonRepository
    @Autowired private lateinit var studyGroupRepository: StudyGroupRepository
    @Autowired private lateinit var courseRepository: CourseRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository

    private val testDate = LocalDate.of(2026, 7, 1)

    private fun doCleanup() {
        lessonRepository.deleteAll()
        studyGroupRepository.deleteAll()
        userRepository.deleteAll(
            userRepository.findAll().filter { it.email != "admin@covenantcode.ru" }
        )
        courseRepository.deleteAll()
    }

    @BeforeEach fun cleanup() = doCleanup()
    @AfterEach fun teardown() = doCleanup()

    private fun saveTeacher(): User {
        val teacherRole = roleRepository.findByName(RoleName.TEACHER)!!
        return userRepository.save(User().apply {
            firstName = "Иван"; lastName = "Петров"
            email = "overlap_teacher@test.com"; passwordHash = "hash"
            role = teacherRole
        })
    }

    private fun saveLesson(teacher: User, start: LocalTime, end: LocalTime): Lesson {
        val course = courseRepository.save(Course().apply { title = "Курс"; durationInWeeks = 8 })
        val group = studyGroupRepository.save(StudyGroup().apply {
            name = "Группа"; this.course = course
            this.teacher = teacher; status = GroupStatus.ACTIVE
            startDate = testDate
        })
        return lessonRepository.save(Lesson().apply {
            studyGroup = group; this.teacher = teacher; topic = "Тема"
            lessonDate = testDate; startTime = start; endTime = end
        })
    }

    @Test
    fun `без конфликта - занятие примыкает после существующего - успех`() {
        val teacher = saveTeacher()
        saveLesson(teacher, LocalTime.of(9, 0), LocalTime.of(10, 0))

        assertThatCode {
            lessonOverlapService.checkTeacherOverlap(
                teacher.id!!, testDate,
                LocalTime.of(10, 30), LocalTime.of(12, 0), null,
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `конфликт реального занятия в БД - ConflictException`() {
        val teacher = saveTeacher()
        saveLesson(teacher, LocalTime.of(9, 0), LocalTime.of(11, 0))

        assertThatThrownBy {
            lessonOverlapService.checkTeacherOverlap(
                teacher.id!!, testDate,
                LocalTime.of(10, 0), LocalTime.of(12, 0), null,
            )
        }.isInstanceOf(ConflictException::class.java)
    }
}
