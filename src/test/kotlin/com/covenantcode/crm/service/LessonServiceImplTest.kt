package com.covenantcode.crm.service

import com.covenantcode.crm.dto.lesson.LessonCreateRequest
import com.covenantcode.crm.dto.lesson.LessonUpdateRequest
import com.covenantcode.crm.entity.Course
import com.covenantcode.crm.entity.Lesson
import com.covenantcode.crm.entity.Role
import com.covenantcode.crm.entity.Student
import com.covenantcode.crm.entity.StudyGroup
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.exception.BadRequestException
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.repository.LessonRepository
import com.covenantcode.crm.repository.StudentRepository
import com.covenantcode.crm.repository.StudyGroupRepository
import com.covenantcode.crm.repository.UserRepository
import com.covenantcode.crm.service.impl.LessonServiceImpl
import org.springframework.security.access.AccessDeniedException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class LessonServiceImplTest {

    @Mock private lateinit var lessonRepository: LessonRepository
    @Mock private lateinit var studyGroupRepository: StudyGroupRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var studentRepository: StudentRepository
    @Mock private lateinit var lessonOverlapService: LessonOverlapService
    @InjectMocks private lateinit var lessonService: LessonServiceImpl

    private val date = LocalDate.of(2026, 6, 10)
    private val start = LocalTime.of(18, 0)
    private val end = LocalTime.of(19, 30)

    private fun makeGroup(status: GroupStatus = GroupStatus.ACTIVE) = StudyGroup().apply {
        id = 1L; name = "Группа"
        course = Course().apply { id = 1L; title = "Курс"; durationInWeeks = 8 }
        teacher = makeUser()
        this.status = status
        startDate = date
    }

    private fun makeUser(id: Long = 3L, roleName: RoleName = RoleName.TEACHER) = User().apply {
        this.id = id; firstName = "Иван"; lastName = "Петров"
        email = "$id@test.com"; passwordHash = "hash"
        role = Role().apply { name = roleName }
    }

    private fun makeLesson(group: StudyGroup, teacher: User) = Lesson().apply {
        id = 1L; studyGroup = group; this.teacher = teacher
        topic = "Тема"; lessonDate = date; startTime = start; endTime = end
    }

    private fun request(
        groupId: Long = 1L, teacherId: Long = 3L,
        startTime: LocalTime = start, endTime: LocalTime = end,
    ) = LessonCreateRequest(
        groupId = groupId, teacherId = teacherId,
        topic = "Тема занятия", description = null,
        lessonDate = date, startTime = startTime, endTime = endTime,
    )

    // ─── getById ──────────────────────────────────────────────────────────────

    @Test
    fun `getById - ADMIN получает занятие - успех`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        val lesson = makeLesson(makeGroup(), makeUser(3L))
        whenever(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson))

        val result = lessonService.getById(1L, admin)

        assertThat(result.id).isEqualTo(1L)
    }

    @Test
    fun `getById - TEACHER своё занятие - успех`() {
        val teacher = makeUser(3L, RoleName.TEACHER)
        val lesson = makeLesson(makeGroup(), teacher)
        whenever(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson))

        val result = lessonService.getById(1L, teacher)

        assertThat(result.id).isEqualTo(1L)
    }

    @Test
    fun `getById - TEACHER чужое занятие - AccessDeniedException`() {
        val otherTeacher = makeUser(99L, RoleName.TEACHER)
        val lesson = makeLesson(makeGroup(), makeUser(3L))
        whenever(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson))

        assertThatThrownBy { lessonService.getById(1L, otherTeacher) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `getById - STUDENT в группе занятия - успех`() {
        val studentUser = makeUser(10L, RoleName.STUDENT)
        val student = Student().apply { id = 1L; user = studentUser; firstName = "А"; lastName = "Б"; phone = "+7" }
        val group = makeGroup().apply { students.add(student) }
        val lesson = makeLesson(group, makeUser(3L))
        whenever(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson))
        whenever(studentRepository.findByUser_Id(10L)).thenReturn(student)

        val result = lessonService.getById(1L, studentUser)

        assertThat(result.id).isEqualTo(1L)
    }

    @Test
    fun `getById - занятие не найдено - ResourceNotFoundException`() {
        whenever(lessonRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { lessonService.getById(99L, makeUser(1L, RoleName.ADMIN)) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    fun `create - успешное создание занятия`() {
        val group = makeGroup()
        val teacher = makeUser()
        val lesson = makeLesson(group, teacher)
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(userRepository.findById(3L)).thenReturn(Optional.of(teacher))
        whenever(lessonRepository.save(any<Lesson>())).thenReturn(lesson)

        val result = lessonService.create(request())

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.topic).isEqualTo("Тема")
        assertThat(result.studyGroup.id).isEqualTo(1L)
    }

    @Test
    fun `create - группа не ACTIVE - BadRequestException lessonRepository не вызывался`() {
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(makeGroup(GroupStatus.DRAFT)))

        assertThatThrownBy { lessonService.create(request()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("активных")
        verify(lessonRepository, never()).save(any<Lesson>())
    }

    @Test
    fun `create - группа не найдена - ResourceNotFoundException`() {
        whenever(studyGroupRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { lessonService.create(request(groupId = 99L)) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `create - endTime раньше startTime - BadRequestException overlapService не вызывался`() {
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(makeGroup()))
        whenever(userRepository.findById(3L)).thenReturn(Optional.of(makeUser()))

        assertThatThrownBy { lessonService.create(request(startTime = LocalTime.of(18, 0), endTime = LocalTime.of(17, 0))) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("позже")
        verify(lessonOverlapService, never()).checkTeacherOverlap(any(), any(), any(), any(), any())
    }

    @Test
    fun `getAll - без фильтров - возвращает Page из двух занятий`() {
        val group = makeGroup()
        val teacher = makeUser()
        val lesson1 = makeLesson(group, teacher)
        val lesson2 = makeLesson(group, teacher).apply { id = 2L }
        val pageable = PageRequest.of(0, 20)
        whenever(lessonRepository.findAll(any<Specification<Lesson>>(), any<PageRequest>()))
            .thenReturn(PageImpl(listOf(lesson1, lesson2)))

        val result = lessonService.getAll(null, null, null, null, pageable)

        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.content).hasSize(2)
    }

    @Test
    fun `getAll - с фильтрами groupId и teacherId - findAll вызван один раз`() {
        val pageable = PageRequest.of(0, 20)
        whenever(lessonRepository.findAll(any<Specification<Lesson>>(), any<PageRequest>()))
            .thenReturn(PageImpl(emptyList()))

        lessonService.getAll(groupId = 1L, teacherId = 3L, dateFrom = null, dateTo = null, pageable = pageable)

        verify(lessonRepository).findAll(any<Specification<Lesson>>(), any<PageRequest>())
    }

    @Test
    fun `create - пересечение занятий - ConflictException пробрасывается`() {
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(makeGroup()))
        whenever(userRepository.findById(3L)).thenReturn(Optional.of(makeUser()))
        whenever(lessonOverlapService.checkTeacherOverlap(any(), any(), any(), any(), anyOrNull()))
            .thenThrow(ConflictException("конфликт"))

        assertThatThrownBy { lessonService.create(request()) }
            .isInstanceOf(ConflictException::class.java)
    }

    // ─── update ───────────────────────────────────────────────────────────────

    private fun updateRequest(
        groupId: Long = 1L, teacherId: Long = 3L,
        startTime: LocalTime = start, endTime: LocalTime = end,
    ) = LessonUpdateRequest(
        groupId = groupId, teacherId = teacherId,
        topic = "Обновлённая тема", description = "Описание",
        lessonDate = date, startTime = startTime, endTime = endTime,
    )

    @Test
    fun `update - успешное обновление занятия`() {
        val teacher = makeUser()
        val group = makeGroup()
        val lesson = makeLesson(group, teacher)
        val savedLesson = makeLesson(group, teacher).apply { topic = "Обновлённая тема" }
        whenever(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson))
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(userRepository.findById(3L)).thenReturn(Optional.of(teacher))
        whenever(lessonRepository.save(any<Lesson>())).thenReturn(savedLesson)

        val result = lessonService.update(1L, updateRequest())

        assertThat(result.id).isEqualTo(1L)
        verify(lessonOverlapService).checkTeacherOverlap(any(), any(), any(), any(), anyOrNull())
    }

    @Test
    fun `update - группа занятия COMPLETED - BadRequestException`() {
        val completedGroup = makeGroup(GroupStatus.COMPLETED)
        val lesson = makeLesson(completedGroup, makeUser())
        whenever(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson))

        assertThatThrownBy { lessonService.update(1L, updateRequest()) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("завершённой")
        verify(lessonRepository, never()).save(any<Lesson>())
    }

    @Test
    fun `update - занятие не найдено - ResourceNotFoundException`() {
        whenever(lessonRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { lessonService.update(99L, updateRequest()) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `update - excludeLessonId равен id обновляемого занятия`() {
        val teacher = makeUser()
        val group = makeGroup()
        val lesson = makeLesson(group, teacher)
        whenever(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson))
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(userRepository.findById(3L)).thenReturn(Optional.of(teacher))
        whenever(lessonRepository.save(any<Lesson>())).thenReturn(lesson)

        lessonService.update(1L, updateRequest())

        verify(lessonOverlapService).checkTeacherOverlap(any(), any(), any(), any(), org.mockito.kotlin.eq(1L))
    }

    @Test
    fun `update - пересечение с другим занятием - ConflictException пробрасывается`() {
        val teacher = makeUser()
        val group = makeGroup()
        val lesson = makeLesson(group, teacher)
        whenever(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson))
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(userRepository.findById(3L)).thenReturn(Optional.of(teacher))
        whenever(lessonOverlapService.checkTeacherOverlap(any(), any(), any(), any(), anyOrNull()))
            .thenThrow(ConflictException("конфликт"))

        assertThatThrownBy { lessonService.update(1L, updateRequest()) }
            .isInstanceOf(ConflictException::class.java)
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    fun `delete - активная группа - lessonRepository delete вызван`() {
        val lesson = makeLesson(makeGroup(GroupStatus.ACTIVE), makeUser())
        whenever(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson))

        lessonService.delete(1L)

        verify(lessonRepository).delete(lesson)
    }

    @Test
    fun `delete - группа COMPLETED - BadRequestException delete не вызван`() {
        val lesson = makeLesson(makeGroup(GroupStatus.COMPLETED), makeUser())
        whenever(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson))

        assertThatThrownBy { lessonService.delete(1L) }
            .isInstanceOf(BadRequestException::class.java)
            .hasMessageContaining("завершённой")
        verify(lessonRepository, never()).delete(any<Lesson>())
    }

    @Test
    fun `delete - группа DRAFT - успешное удаление`() {
        val lesson = makeLesson(makeGroup(GroupStatus.DRAFT), makeUser())
        whenever(lessonRepository.findById(1L)).thenReturn(Optional.of(lesson))

        lessonService.delete(1L)

        verify(lessonRepository).delete(lesson)
    }

    @Test
    fun `delete - занятие не найдено - ResourceNotFoundException delete не вызван`() {
        whenever(lessonRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { lessonService.delete(99L) }
            .isInstanceOf(ResourceNotFoundException::class.java)
        verify(lessonRepository, never()).delete(any<Lesson>())
    }

    // ─── getLessonsByGroup ────────────────────────────────────────────────────

    @Test
    fun `getLessonsByGroup - ADMIN получает расписание - возвращает 2 занятия`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        val group = makeGroup()
        val lesson1 = makeLesson(group, makeUser()).apply { id = 1L }
        val lesson2 = makeLesson(group, makeUser()).apply { id = 2L }
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(lessonRepository.findByStudyGroupIdOrderByLessonDateAscStartTimeAsc(1L))
            .thenReturn(listOf(lesson1, lesson2))

        val result = lessonService.getLessonsByGroup(1L, admin)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `getLessonsByGroup - TEACHER своя группа - успех`() {
        val teacher = makeUser(3L, RoleName.TEACHER)
        val group = makeGroup().apply { this.teacher = teacher }
        val lesson = makeLesson(group, teacher)
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))
        whenever(lessonRepository.findByStudyGroupIdOrderByLessonDateAscStartTimeAsc(1L))
            .thenReturn(listOf(lesson))

        val result = lessonService.getLessonsByGroup(1L, teacher)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `getLessonsByGroup - TEACHER чужая группа - ForbiddenException`() {
        val otherTeacher = makeUser(5L, RoleName.TEACHER)
        val group = makeGroup()
        whenever(studyGroupRepository.findById(1L)).thenReturn(Optional.of(group))

        assertThatThrownBy { lessonService.getLessonsByGroup(1L, otherTeacher) }
            .isInstanceOf(com.covenantcode.crm.exception.ForbiddenException::class.java)
    }

    @Test
    fun `getLessonsByGroup - группа не найдена - ResourceNotFoundException`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        whenever(studyGroupRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { lessonService.getLessonsByGroup(99L, admin) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    // ─── getLessonsByTeacher ──────────────────────────────────────────────────

    @Test
    fun `getLessonsByTeacher - ADMIN получает расписание - возвращает 2 занятия`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        val teacher = makeUser(3L, RoleName.TEACHER)
        val group = makeGroup()
        val lesson1 = makeLesson(group, teacher).apply { id = 1L }
        val lesson2 = makeLesson(group, teacher).apply { id = 2L }
        whenever(userRepository.findById(3L)).thenReturn(Optional.of(teacher))
        whenever(lessonRepository.findAll(any<Specification<Lesson>>(), any<Sort>()))
            .thenReturn(listOf(lesson1, lesson2))

        val result = lessonService.getLessonsByTeacher(3L, null, null, admin)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `getLessonsByTeacher - TEACHER своё расписание - успех`() {
        val teacher = makeUser(3L, RoleName.TEACHER)
        val group = makeGroup()
        val lesson = makeLesson(group, teacher)
        whenever(userRepository.findById(3L)).thenReturn(Optional.of(teacher))
        whenever(lessonRepository.findAll(any<Specification<Lesson>>(), any<Sort>()))
            .thenReturn(listOf(lesson))

        val result = lessonService.getLessonsByTeacher(3L, null, null, teacher)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `getLessonsByTeacher - TEACHER чужое расписание - ForbiddenException`() {
        val teacher = makeUser(3L, RoleName.TEACHER)
        val otherUser = makeUser(5L, RoleName.TEACHER)
        whenever(userRepository.findById(5L)).thenReturn(Optional.of(otherUser))

        assertThatThrownBy { lessonService.getLessonsByTeacher(5L, null, null, teacher) }
            .isInstanceOf(com.covenantcode.crm.exception.ForbiddenException::class.java)
    }

    @Test
    fun `getLessonsByTeacher - преподаватель не найден - ResourceNotFoundException`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { lessonService.getLessonsByTeacher(99L, null, null, admin) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    // ─── getLessonsByStudent ──────────────────────────────────────────────────

    private fun makeStudent(userId: Long = 10L) = Student().apply {
        id = 7L
        user = makeUser(userId, RoleName.STUDENT)
        firstName = "Анна"; lastName = "Сидорова"; phone = "+7"
    }

    @Test
    fun `getLessonsByStudent - ADMIN запрашивает расписание - возвращает 2 занятия`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        val student = makeStudent()
        val group = makeGroup()
        val lesson1 = makeLesson(group, makeUser()).apply { id = 1L }
        val lesson2 = makeLesson(group, makeUser()).apply { id = 2L }
        whenever(studentRepository.findById(7L)).thenReturn(Optional.of(student))
        whenever(lessonRepository.findAll(any<Specification<Lesson>>(), any<Sort>()))
            .thenReturn(listOf(lesson1, lesson2))

        val result = lessonService.getLessonsByStudent(7L, null, null, admin)

        assertThat(result).hasSize(2)
    }

    @Test
    fun `getLessonsByStudent - STUDENT своё расписание - успех`() {
        val studentUser = makeUser(10L, RoleName.STUDENT)
        val student = makeStudent(userId = 10L)
        val lesson = makeLesson(makeGroup(), makeUser())
        whenever(studentRepository.findById(7L)).thenReturn(Optional.of(student))
        whenever(lessonRepository.findAll(any<Specification<Lesson>>(), any<Sort>()))
            .thenReturn(listOf(lesson))

        val result = lessonService.getLessonsByStudent(7L, null, null, studentUser)

        assertThat(result).hasSize(1)
    }

    @Test
    fun `getLessonsByStudent - STUDENT чужое расписание - ForbiddenException`() {
        val otherUser = makeUser(20L, RoleName.STUDENT)
        val student = makeStudent(userId = 10L)
        whenever(studentRepository.findById(7L)).thenReturn(Optional.of(student))

        assertThatThrownBy { lessonService.getLessonsByStudent(7L, null, null, otherUser) }
            .isInstanceOf(com.covenantcode.crm.exception.ForbiddenException::class.java)
    }

    @Test
    fun `getLessonsByStudent - студент не найден - ResourceNotFoundException`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        whenever(studentRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { lessonService.getLessonsByStudent(99L, null, null, admin) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `getLessonsByStudent - студент в двух группах - возвращает занятия обеих групп`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        val student = makeStudent()
        val group1 = makeGroup().apply { id = 1L }
        val group2 = makeGroup().apply { id = 2L }
        val teacher = makeUser()
        val lessons = listOf(
            makeLesson(group1, teacher).apply { id = 1L },
            makeLesson(group2, teacher).apply { id = 2L },
            makeLesson(group2, teacher).apply { id = 3L },
        )
        whenever(studentRepository.findById(7L)).thenReturn(Optional.of(student))
        whenever(lessonRepository.findAll(any<Specification<Lesson>>(), any<Sort>()))
            .thenReturn(lessons)

        val result = lessonService.getLessonsByStudent(7L, null, null, admin)

        assertThat(result).hasSize(3)
    }
}
