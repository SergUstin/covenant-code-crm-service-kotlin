package com.covenantcode.crm.service

import com.covenantcode.crm.dto.student.StudentCreateRequest
import com.covenantcode.crm.dto.student.StudentUpdateRequest
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.entity.Student
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.entity.Role
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.repository.StudentRepository
import com.covenantcode.crm.repository.StudyGroupRepository
import com.covenantcode.crm.repository.UserRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import com.covenantcode.crm.service.impl.StudentServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class StudentServiceImplTest {

    @Mock private lateinit var studentRepository: StudentRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var studyGroupRepository: StudyGroupRepository
    @InjectMocks private lateinit var studentService: StudentServiceImpl

    private fun makeRole(name: RoleName) = Role().apply { this.name = name }

    private fun makeUser(id: Long = 5L, role: RoleName = RoleName.MANAGER) = User().apply {
        this.id = id
        firstName = "Иван"
        lastName = "Петров"
        email = "user@test.com"
        passwordHash = "hash"
        this.role = makeRole(role)
    }

    private fun makeStudent(id: Long = 1L, user: User? = null) = Student().apply {
        this.id = id
        firstName = "Алиса"
        lastName = "Смирнова"
        this.user = user
    }

    private fun createRequest(userId: Long? = null) = StudentCreateRequest(
        firstName = "Алиса",
        lastName = "Смирнова",
        phone = "+79161234567",
        userId = userId,
    )

    @Test
    fun `getById - ADMIN - доступ разрешён studyGroupRepository не вызывался`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        val student = makeStudent(10L)
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student))

        val result = studentService.getById(10L, admin)

        assertThat(result.id).isEqualTo(10L)
        verify(studyGroupRepository, never()).existsByTeacherAndStudentsContaining(any(), any())
    }

    @Test
    fun `getById - TEACHER студент в группе - 200`() {
        val teacher = makeUser(2L, RoleName.TEACHER)
        val student = makeStudent(10L)
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student))
        whenever(studyGroupRepository.existsByTeacherAndStudentsContaining(teacher, student)).thenReturn(true)

        val result = studentService.getById(10L, teacher)

        assertThat(result.id).isEqualTo(10L)
    }

    @Test
    fun `getById - TEACHER студент не в группе - AccessDeniedException`() {
        val teacher = makeUser(2L, RoleName.TEACHER)
        val student = makeStudent(10L)
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student))
        whenever(studyGroupRepository.existsByTeacherAndStudentsContaining(teacher, student)).thenReturn(false)

        assertThatThrownBy { studentService.getById(10L, teacher) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `getById - STUDENT свой профиль - 200`() {
        val user = makeUser(3L, RoleName.STUDENT)
        val student = makeStudent(10L, user)
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student))

        val result = studentService.getById(10L, user)

        assertThat(result.id).isEqualTo(10L)
    }

    @Test
    fun `getById - STUDENT чужой профиль - AccessDeniedException`() {
        val user = makeUser(3L, RoleName.STUDENT)
        val otherUser = makeUser(99L, RoleName.STUDENT)
        val student = makeStudent(10L, otherUser)
        whenever(studentRepository.findById(10L)).thenReturn(Optional.of(student))

        assertThatThrownBy { studentService.getById(10L, user) }
            .isInstanceOf(AccessDeniedException::class.java)
    }

    @Test
    fun `getById - студент не найден - ResourceNotFoundException`() {
        val admin = makeUser(1L, RoleName.ADMIN)
        whenever(studentRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studentService.getById(99L, admin) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `getAll - без поиска - возвращает все элементы`() {
        val pageable = PageRequest.of(0, 20)
        val students = listOf(makeStudent(1L), makeStudent(2L))
        whenever(studentRepository.findAll(any<Specification<Student>>(), any<Pageable>()))
            .thenReturn(PageImpl(students, pageable, 2))

        val result = studentService.getAll(null, pageable)

        assertThat(result.totalElements).isEqualTo(2)
        assertThat(result.content).hasSize(2)
    }

    @Test
    fun `getAll - с поиском - findAll вызван со спецификацией`() {
        val pageable = PageRequest.of(0, 20)
        val students = listOf(makeStudent(1L))
        whenever(studentRepository.findAll(any<Specification<Student>>(), any<Pageable>()))
            .thenReturn(PageImpl(students, pageable, 1))

        val result = studentService.getAll("Смир", pageable)

        assertThat(result.content).hasSize(1)
        verify(studentRepository).findAll(any<Specification<Student>>(), any<Pageable>())
    }

    @Test
    fun `getAll - пустой список - totalElements 0`() {
        val pageable = PageRequest.of(0, 20)
        whenever(studentRepository.findAll(any<Specification<Student>>(), any<Pageable>()))
            .thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = studentService.getAll(null, pageable)

        assertThat(result.content).isEmpty()
        assertThat(result.totalElements).isEqualTo(0)
    }

    private fun updateRequest() = StudentUpdateRequest(
        firstName = "Мария",
        lastName = "Иванова",
        phone = "+79161112233",
        email = "maria@test.com",
    )

    @Test
    fun `update - успешное обновление - поля изменились`() {
        val existing = makeStudent(1L)
        val updated = Student().apply {
            id = 1L; firstName = "Мария"; lastName = "Иванова"; phone = "+79161112233"; email = "maria@test.com"
        }
        whenever(studentRepository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(studentRepository.save(any<Student>())).thenReturn(updated)

        val result = studentService.update(1L, updateRequest())

        assertThat(result.firstName).isEqualTo("Мария")
        assertThat(result.lastName).isEqualTo("Иванова")
        verify(studentRepository).save(any())
    }

    @Test
    fun `update - студент не найден - ResourceNotFoundException save не вызывается`() {
        whenever(studentRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studentService.update(99L, updateRequest()) }
            .isInstanceOf(ResourceNotFoundException::class.java)
        verify(studentRepository, never()).save(any())
    }

    @Test
    fun `update - поле user не изменяется`() {
        val user = makeUser(5L)
        val existing = makeStudent(1L, user)
        whenever(studentRepository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(studentRepository.save(any<Student>())).thenAnswer { invocation ->
            invocation.getArgument<Student>(0)
        }

        studentService.update(1L, updateRequest())

        verify(studentRepository).save(org.mockito.kotlin.check { saved ->
            assertThat(saved.user?.id).isEqualTo(5L)
        })
    }

    @Test
    fun `deleteById - успешное удаление - studentRepository delete вызван`() {
        val student = makeStudent(1L)
        whenever(studentRepository.findById(1L)).thenReturn(Optional.of(student))
        whenever(studyGroupRepository.existsByStudents_IdAndStatus(1L, GroupStatus.ACTIVE)).thenReturn(false)

        studentService.deleteById(1L)

        verify(studentRepository).delete(student)
    }

    @Test
    fun `deleteById - студент не найден - ResourceNotFoundException`() {
        whenever(studentRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studentService.deleteById(99L) }
            .isInstanceOf(ResourceNotFoundException::class.java)
        verify(studyGroupRepository, never()).existsByStudents_IdAndStatus(any(), any())
        verify(studentRepository, never()).delete(any<Student>())
    }

    @Test
    fun `deleteById - студент в активной группе - ConflictException delete не вызывался`() {
        val student = makeStudent(1L)
        whenever(studentRepository.findById(1L)).thenReturn(Optional.of(student))
        whenever(studyGroupRepository.existsByStudents_IdAndStatus(1L, GroupStatus.ACTIVE)).thenReturn(true)

        assertThatThrownBy { studentService.deleteById(1L) }
            .isInstanceOf(ConflictException::class.java)
        verify(studentRepository, never()).delete(any<Student>())
    }

    @Test
    fun `create - без userId - студент создан userId null userRepository не вызывался`() {
        whenever(studentRepository.save(any<Student>())).thenReturn(makeStudent(1L))

        val result = studentService.create(createRequest())

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.userId).isNull()
        verify(userRepository, never()).findById(any())
    }

    @Test
    fun `create - с userId - студент привязан к пользователю`() {
        val user = makeUser(5L)
        val student = makeStudent(2L, user)
        whenever(userRepository.findById(5L)).thenReturn(Optional.of(user))
        whenever(studentRepository.existsByUserId(5L)).thenReturn(false)
        whenever(studentRepository.save(any<Student>())).thenReturn(student)

        val result = studentService.create(createRequest(userId = 5L))

        assertThat(result.userId).isEqualTo(5L)
    }

    @Test
    fun `create - userId не найден - ResourceNotFoundException save не вызывается`() {
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { studentService.create(createRequest(userId = 99L)) }
            .isInstanceOf(ResourceNotFoundException::class.java)
        verify(studentRepository, never()).save(any())
    }

    @Test
    fun `create - userId уже занят - ConflictException save не вызывается`() {
        val user = makeUser(5L)
        whenever(userRepository.findById(5L)).thenReturn(Optional.of(user))
        whenever(studentRepository.existsByUserId(5L)).thenReturn(true)

        assertThatThrownBy { studentService.create(createRequest(userId = 5L)) }
            .isInstanceOf(ConflictException::class.java)
        verify(studentRepository, never()).save(any())
    }
}
