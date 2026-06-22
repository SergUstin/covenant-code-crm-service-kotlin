package com.covenantcode.crm.service

import com.covenantcode.crm.dto.group.AddStudentToGroupRequest
import com.covenantcode.crm.dto.group.GroupStatusUpdateRequest
import com.covenantcode.crm.dto.group.StudyGroupCreateRequest
import com.covenantcode.crm.dto.group.StudyGroupUpdateRequest
import com.covenantcode.crm.entity.Course
import com.covenantcode.crm.entity.Role
import com.covenantcode.crm.entity.Student
import com.covenantcode.crm.entity.StudyGroup
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.exception.BadRequestException
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.exception.ResourceNotFoundException
import org.mockito.kotlin.never
import org.springframework.security.access.AccessDeniedException
import com.covenantcode.crm.repository.CourseRepository
import com.covenantcode.crm.repository.StudentRepository
import com.covenantcode.crm.repository.StudyGroupRepository
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import com.covenantcode.crm.repository.UserRepository
import com.covenantcode.crm.service.impl.StudyGroupServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional
@ExtendWith(MockitoExtension::class)
class StudyGroupServiceImplTest {

    @Mock private lateinit var studyGroupRepository: StudyGroupRepository
    @Mock private lateinit var courseRepository: CourseRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var studentRepository: StudentRepository
    @InjectMocks private lateinit var studyGroupService: StudyGroupServiceImpl

    private fun makeCourse(id: Long = 1L) = Course().apply {
        this.id = id
        title = "Тестовый курс"
        durationInWeeks = 8
    }

    private fun makeUser(id: Long = 2L, role: RoleName = RoleName.TEACHER) = User().apply {
        this.id = id
        firstName = "Иван"
        lastName = "Петров"
        email = "teacher@test.com"
        passwordHash = "hash"
        this.role = Role().apply { name = role }
    }

    private fun makeStudent(id: Long) = Student().apply {
        this.id = id
        firstName = "Студент"
        lastName = "Тестов"
    }

    private fun baseRequest(studentIds: Set<Long>? = null) = StudyGroupCreateRequest(
        name = "Группа А",
        courseId = 1L,
        teacherId = 2L,
        startDate = LocalDate.of(2026, 9, 1),
        studentIds = studentIds,
    )

    private fun savedGroup(course: Course, teacher: User, students: MutableSet<Student> = mutableSetOf()) =
        StudyGroup().apply {
            id = 1L
            name = "Группа А"
            this.course = course
            this.teacher = teacher
            startDate = LocalDate.of(2026, 9, 1)
            status = GroupStatus.DRAFT
            this.students = students
        }

    @Test
    fun `create - без студентов - статус DRAFT students пустой`() {
        val course = makeCourse()
        val teacher = makeUser()
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(course))
        whenever(userRepository.findById(2L)).thenReturn(Optional.of(teacher))
        whenever(studyGroupRepository.save(any<StudyGroup>())).thenReturn(savedGroup(course, teacher))

        val result = studyGroupService.create(baseRequest())

        assertThat(result.status).isEqualTo(GroupStatus.DRAFT)
        assertThat(result.students).isEmpty()
    }

    @Test
    fun `create - со студентами - студенты присутствуют в ответе`() {
        val course = makeCourse()
        val teacher = makeUser()
        val student1 = makeStudent(10L)
        val student2 = makeStudent(11L)
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(course))
        whenever(userRepository.findById(2L)).thenReturn(Optional.of(teacher))
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student1))
        whenever(studentRepository.findById(11L)).thenReturn(Optional.of(student2))
        whenever(studyGroupRepository.save(any<StudyGroup>()))
            .thenReturn(savedGroup(course, teacher, mutableSetOf(student1, student2)))

        val result = studyGroupService.create(baseRequest(setOf(10L, 11L)))

        assertThat(result.students).hasSize(2)
    }

    @Test
    fun `create - курс не найден - ResourceNotFoundException`() {
        whenever(courseRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studyGroupService.create(baseRequest().copy(courseId = 99L)) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `create - учитель не найден - ResourceNotFoundException`() {
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(makeCourse()))
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studyGroupService.create(baseRequest().copy(teacherId = 99L)) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `create - учитель не имеет роли TEACHER - BadRequestException`() {
        val manager = makeUser(2L, RoleName.MANAGER)
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(makeCourse()))
        whenever(userRepository.findById(2L)).thenReturn(Optional.of(manager))

        assertThatThrownBy { studyGroupService.create(baseRequest()) }
            .isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun `create - студент не найден - ResourceNotFoundException`() {
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(makeCourse()))
        whenever(userRepository.findById(2L)).thenReturn(Optional.of(makeUser()))
        whenever(studentRepository.findById(50L)).thenReturn(Optional.empty())

        assertThatThrownBy { studyGroupService.create(baseRequest(setOf(50L))) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `getById - ADMIN - возвращает группу studentRepository не вызывался`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        val course = makeCourse()
        val teacher = makeUser(2L)
        val group = savedGroup(course, teacher)
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        val result = studyGroupService.getById(1L, admin)

        assertThat(result.id).isEqualTo(1L)
        verify(studentRepository, never()).findByUser_Id(any())
    }

    @Test
    fun `getById - TEACHER своя группа - 200`() {
        val teacher = makeUser(2L, RoleName.TEACHER)
        val group = savedGroup(makeCourse(), teacher)
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        val result = studyGroupService.getById(1L, teacher)

        assertThat(result.id).isEqualTo(1L)
    }

    @Test
    fun `getById - TEACHER чужая группа - AccessDeniedException`() {
        val teacher = makeUser(2L, RoleName.TEACHER)
        val otherTeacher = makeUser(99L, RoleName.TEACHER)
        val group = savedGroup(makeCourse(), otherTeacher)
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThatThrownBy { studyGroupService.getById(1L, teacher) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `getById - STUDENT в группе - 200`() {
        val studentUser = makeUser(3L, RoleName.STUDENT)
        val student = makeStudent(10L).also { it.user = studentUser }
        val group = savedGroup(makeCourse(), makeUser(2L)).also { it.students = mutableSetOf(student) }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studentRepository.findByUser_Id(3L)).thenReturn(student)

        val result = studyGroupService.getById(1L, studentUser)

        assertThat(result.id).isEqualTo(1L)
    }

    @Test
    fun `getById - STUDENT не в группе - AccessDeniedException`() {
        val studentUser = makeUser(3L, RoleName.STUDENT)
        val student = makeStudent(10L).also { it.user = studentUser }
        val group = savedGroup(makeCourse(), makeUser(2L))
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studentRepository.findByUser_Id(3L)).thenReturn(student)

        assertThatThrownBy { studyGroupService.getById(1L, studentUser) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `getById - группа не найдена - ResourceNotFoundException`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        whenever(studyGroupRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studyGroupService.getById(99L, admin) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `getAll - без фильтров - возвращает страницу`() {
        val pageable = PageRequest.of(0, 20)
        val group = savedGroup(makeCourse(), makeUser())
        whenever(studyGroupRepository.findAll(any<Specification<StudyGroup>>(), any<Pageable>()))
            .thenReturn(PageImpl(listOf(group), pageable, 1))

        val result = studyGroupService.getAll(null, null, null, pageable)

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content).hasSize(1)
    }

    @Test
    fun `getAll - фильтр по статусу - репозиторий вызван со спецификацией`() {
        val pageable = PageRequest.of(0, 20)
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.ACTIVE }
        whenever(studyGroupRepository.findAll(any<Specification<StudyGroup>>(), any<Pageable>()))
            .thenReturn(PageImpl(listOf(group), pageable, 1))

        val result = studyGroupService.getAll(null, null, GroupStatus.ACTIVE, pageable)

        assertThat(result.content[0].status).isEqualTo(GroupStatus.ACTIVE)
        verify(studyGroupRepository).findAll(any<Specification<StudyGroup>>(), any<Pageable>())
    }

    @Test
    fun `getAll - пустой результат - возвращает пустую страницу`() {
        val pageable = PageRequest.of(0, 20)
        whenever(studyGroupRepository.findAll(any<Specification<StudyGroup>>(), any<Pageable>()))
            .thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = studyGroupService.getAll(null, null, null, pageable)

        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
    }

    // ─── update ───────────────────────────────────────────────────────────────

    private fun updateRequest(courseId: Long = 1L, teacherId: Long = 2L) = StudyGroupUpdateRequest(
        name = "Обновлённое название",
        courseId = courseId,
        teacherId = teacherId,
        startDate = LocalDate.of(2026, 10, 1),
    )

    @Test
    fun `update - статус DRAFT - поля обновлены успешно`() {
        val course = makeCourse()
        val teacher = makeUser()
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.DRAFT }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(course))
        whenever(userRepository.findById(2L)).thenReturn(Optional.of(teacher))
        whenever(studyGroupRepository.save(any<StudyGroup>())).thenReturn(
            group.also { it.name = "Обновлённое название"; it.course = course; it.teacher = teacher }
        )

        val result = studyGroupService.update(1L, updateRequest())

        assertThat(result.name).isEqualTo("Обновлённое название")
    }

    @Test
    fun `update - статус COMPLETED - BadRequestException`() {
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.COMPLETED }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThatThrownBy { studyGroupService.update(1L, updateRequest()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("COMPLETED")
    }

    @Test
    fun `update - статус CANCELLED - BadRequestException`() {
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.CANCELLED }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThatThrownBy { studyGroupService.update(1L, updateRequest()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("CANCELLED")
    }

    @Test
    fun `update - группа не найдена - ResourceNotFoundException`() {
        whenever(studyGroupRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studyGroupService.update(99L, updateRequest()) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `update - учитель не найден - ResourceNotFoundException`() {
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.DRAFT }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(makeCourse()))
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studyGroupService.update(1L, updateRequest(teacherId = 99L)) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `update - учитель без роли TEACHER - BadRequestException`() {
        val manager = makeUser(2L, RoleName.MANAGER)
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.DRAFT }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(makeCourse()))
        whenever(userRepository.findById(2L)).thenReturn(Optional.of(manager))

        assertThatThrownBy { studyGroupService.update(1L, updateRequest()) }
            .isInstanceOf(BadRequestException::class.java)
    }

    // ─── updateStatus ─────────────────────────────────────────────────────────

    @Test
    fun `updateStatus - DRAFT → ACTIVE - статус изменён`() {
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.DRAFT }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studyGroupRepository.save(any<StudyGroup>())).thenAnswer { it.arguments[0] as StudyGroup }

        val result = studyGroupService.updateStatus(1L, GroupStatusUpdateRequest(GroupStatus.ACTIVE))

        assertThat(result.status).isEqualTo(GroupStatus.ACTIVE)
    }

    @Test
    fun `updateStatus - ACTIVE → COMPLETED - статус изменён`() {
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.ACTIVE }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studyGroupRepository.save(any<StudyGroup>())).thenAnswer { it.arguments[0] as StudyGroup }

        val result = studyGroupService.updateStatus(1L, GroupStatusUpdateRequest(GroupStatus.COMPLETED))

        assertThat(result.status).isEqualTo(GroupStatus.COMPLETED)
    }

    @Test
    fun `updateStatus - ACTIVE → CANCELLED - статус изменён`() {
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.ACTIVE }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studyGroupRepository.save(any<StudyGroup>())).thenAnswer { it.arguments[0] as StudyGroup }

        val result = studyGroupService.updateStatus(1L, GroupStatusUpdateRequest(GroupStatus.CANCELLED))

        assertThat(result.status).isEqualTo(GroupStatus.CANCELLED)
    }

    @Test
    fun `updateStatus - DRAFT → COMPLETED - недопустимый переход - BadRequestException`() {
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.DRAFT }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThatThrownBy { studyGroupService.updateStatus(1L, GroupStatusUpdateRequest(GroupStatus.COMPLETED)) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("DRAFT")
            .hasMessageContaining("COMPLETED")
    }

    @Test
    fun `updateStatus - COMPLETED → ACTIVE - финальный статус - BadRequestException`() {
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.COMPLETED }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThatThrownBy { studyGroupService.updateStatus(1L, GroupStatusUpdateRequest(GroupStatus.ACTIVE)) }
            .isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun `updateStatus - CANCELLED → DRAFT - финальный статус - BadRequestException`() {
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.CANCELLED }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThatThrownBy { studyGroupService.updateStatus(1L, GroupStatusUpdateRequest(GroupStatus.DRAFT)) }
            .isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun `updateStatus - группа не найдена - ResourceNotFoundException`() {
        whenever(studyGroupRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studyGroupService.updateStatus(99L, GroupStatusUpdateRequest(GroupStatus.ACTIVE)) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    // ─── addStudent ───────────────────────────────────────────────────────────

    private fun addStudentRequest(studentId: Long = 10L) = AddStudentToGroupRequest(studentId = studentId)

    @Test
    fun `addStudent - DRAFT группа студент не в группе - студент добавлен`() {
        val course = makeCourse()
        val teacher = makeUser()
        val student = makeStudent(10L)
        val group = savedGroup(course, teacher)
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student))
        whenever(studyGroupRepository.save(any<StudyGroup>())).thenAnswer { it.arguments[0] as StudyGroup }

        val result = studyGroupService.addStudent(1L, addStudentRequest())

        assertThat(result.students).anyMatch { it.id == 10L }
        verify(studyGroupRepository).save(any<StudyGroup>())
    }

    @Test
    fun `addStudent - ACTIVE группа - студент добавлен успешно`() {
        val student = makeStudent(10L)
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.ACTIVE }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student))
        whenever(studyGroupRepository.save(any<StudyGroup>())).thenAnswer { it.arguments[0] as StudyGroup }

        val result = studyGroupService.addStudent(1L, addStudentRequest())

        assertThat(result.students).anyMatch { it.id == 10L }
    }

    @Test
    fun `addStudent - группа не найдена - ResourceNotFoundException`() {
        whenever(studyGroupRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studyGroupService.addStudent(99L, addStudentRequest()) }
            .isInstanceOf(ResourceNotFoundException::class.java)
            .hasMessageContaining("StudyGroup")
    }

    @Test
    fun `addStudent - студент не найден - ResourceNotFoundException`() {
        val group = savedGroup(makeCourse(), makeUser())
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studentRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studyGroupService.addStudent(1L, addStudentRequest(studentId = 99L)) }
            .isInstanceOf(ResourceNotFoundException::class.java)
            .hasMessageContaining("Student")
    }

    @Test
    fun `addStudent - студент уже в группе - ConflictException`() {
        val student = makeStudent(10L)
        val group = savedGroup(makeCourse(), makeUser(), mutableSetOf(student))
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student))

        assertThatThrownBy { studyGroupService.addStudent(1L, addStudentRequest()) }
            .isInstanceOf(ConflictException::class.java)
            .hasMessageContaining("10")
    }

    @Test
    fun `addStudent - группа COMPLETED - BadRequestException`() {
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.COMPLETED }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThatThrownBy { studyGroupService.addStudent(1L, addStudentRequest()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("COMPLETED")
    }

    @Test
    fun `addStudent - группа CANCELLED - BadRequestException`() {
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.CANCELLED }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThatThrownBy { studyGroupService.addStudent(1L, addStudentRequest()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("CANCELLED")
    }

    // ─── removeStudent ────────────────────────────────────────────────────────

    @Test
    fun `removeStudent - DRAFT группа студент в группе - студент удалён`() {
        val student = makeStudent(10L)
        val group = savedGroup(makeCourse(), makeUser(), mutableSetOf(student))
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student))
        whenever(studyGroupRepository.save(any<StudyGroup>())).thenAnswer { it.arguments[0] as StudyGroup }

        studyGroupService.removeStudent(1L, 10L)

        assertThat(group.students).doesNotContain(student)
        verify(studyGroupRepository).save(group)
    }

    @Test
    fun `removeStudent - ACTIVE группа - студент удалён успешно`() {
        val student = makeStudent(10L)
        val group = savedGroup(makeCourse(), makeUser(), mutableSetOf(student)).also { it.status = GroupStatus.ACTIVE }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student))
        whenever(studyGroupRepository.save(any<StudyGroup>())).thenAnswer { it.arguments[0] as StudyGroup }

        studyGroupService.removeStudent(1L, 10L)

        assertThat(group.students).doesNotContain(student)
    }

    @Test
    fun `removeStudent - CANCELLED группа - студент удалён успешно`() {
        val student = makeStudent(10L)
        val group = savedGroup(makeCourse(), makeUser(), mutableSetOf(student)).also { it.status = GroupStatus.CANCELLED }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student))
        whenever(studyGroupRepository.save(any<StudyGroup>())).thenAnswer { it.arguments[0] as StudyGroup }

        studyGroupService.removeStudent(1L, 10L)

        assertThat(group.students).doesNotContain(student)
    }

    @Test
    fun `removeStudent - COMPLETED группа - BadRequestException save не вызывался`() {
        val group = savedGroup(makeCourse(), makeUser()).also { it.status = GroupStatus.COMPLETED }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThatThrownBy { studyGroupService.removeStudent(1L, 10L) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("COMPLETED")
        verify(studyGroupRepository, never()).save(any<StudyGroup>())
    }

    @Test
    fun `removeStudent - группа не найдена - ResourceNotFoundException`() {
        whenever(studyGroupRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studyGroupService.removeStudent(99L, 10L) }
            .isInstanceOf(ResourceNotFoundException::class.java)
            .hasMessageContaining("StudyGroup")
    }

    @Test
    fun `removeStudent - студент не найден - ResourceNotFoundException`() {
        val group = savedGroup(makeCourse(), makeUser())
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studentRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studyGroupService.removeStudent(1L, 99L) }
            .isInstanceOf(ResourceNotFoundException::class.java)
            .hasMessageContaining("Student")
    }

    @Test
    fun `removeStudent - студент не состоит в группе - BadRequestException`() {
        val student = makeStudent(10L)
        val group = savedGroup(makeCourse(), makeUser())
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student))

        assertThatThrownBy { studyGroupService.removeStudent(1L, 10L) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("10")
    }

    // ─── getStudentsOfGroup ───────────────────────────────────────────────────

    @Test
    fun `getStudentsOfGroup - ADMIN - возвращает список студентов`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        val s1 = makeStudent(10L)
        val s2 = makeStudent(11L)
        val group = savedGroup(makeCourse(), makeUser(2L), mutableSetOf(s1, s2))
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        val result = studyGroupService.getStudentsOfGroup(1L, admin)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `getStudentsOfGroup - MANAGER - возвращает список без проверки учителя`() {
        val manager = makeUser(1L, RoleName.MANAGER)
        val student = makeStudent(10L)
        val group = savedGroup(makeCourse(), makeUser(2L), mutableSetOf(student))
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        val result = studyGroupService.getStudentsOfGroup(1L, manager)

        assertThat(result).hasSize(1)
        verify(studentRepository, never()).findByUser_Id(any())
    }

    @Test
    fun `getStudentsOfGroup - TEACHER своя группа - возвращает список`() {
        val teacher = makeUser(2L, RoleName.TEACHER)
        val student = makeStudent(10L)
        val group = savedGroup(makeCourse(), teacher, mutableSetOf(student))
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        val result = studyGroupService.getStudentsOfGroup(1L, teacher)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `getStudentsOfGroup - TEACHER чужая группа - AccessDeniedException`() {
        val teacher = makeUser(2L, RoleName.TEACHER)
        val otherTeacher = makeUser(99L, RoleName.TEACHER)
        val group = savedGroup(makeCourse(), otherTeacher)
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThatThrownBy { studyGroupService.getStudentsOfGroup(1L, teacher) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `getStudentsOfGroup - группа не найдена - ResourceNotFoundException`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        whenever(studyGroupRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studyGroupService.getStudentsOfGroup(99L, admin) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `getStudentsOfGroup - группа без студентов - пустой список`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        val group = savedGroup(makeCourse(), makeUser(2L))
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        val result = studyGroupService.getStudentsOfGroup(1L, admin)

        assertThat(result).isEmpty()
    }
}
