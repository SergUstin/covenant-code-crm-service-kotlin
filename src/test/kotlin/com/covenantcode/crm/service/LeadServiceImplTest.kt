package com.covenantcode.crm.service

import com.covenantcode.crm.dto.lead.LeadCommentCreateRequest
import com.covenantcode.crm.dto.lead.LeadConvertRequest
import com.covenantcode.crm.dto.lead.LeadCreateRequest
import com.covenantcode.crm.dto.lead.LeadUpdateRequest
import com.covenantcode.crm.entity.Course
import com.covenantcode.crm.entity.Lead
import com.covenantcode.crm.entity.LeadComment
import com.covenantcode.crm.entity.Student
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.CourseStatus
import com.covenantcode.crm.entity.enums.LeadStatus
import com.covenantcode.crm.dto.lead.LeadStatusUpdateRequest
import com.covenantcode.crm.exception.BadRequestException
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.repository.CourseRepository
import com.covenantcode.crm.repository.LeadCommentRepository
import com.covenantcode.crm.repository.LeadRepository
import com.covenantcode.crm.repository.StudentRepository
import com.covenantcode.crm.repository.UserRepository
import com.covenantcode.crm.service.impl.LeadServiceImpl
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
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class LeadServiceImplTest {

    @Mock private lateinit var leadRepository: LeadRepository
    @Mock private lateinit var courseRepository: CourseRepository
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var leadCommentRepository: LeadCommentRepository
    @Mock private lateinit var studentRepository: StudentRepository
    @InjectMocks private lateinit var leadService: LeadServiceImpl

    private fun makeCourse() = Course().apply {
        id = 1L
        title = "Java для начинающих"
        durationInWeeks = 16
        price = BigDecimal("45000.00")
        status = CourseStatus.ACTIVE
    }

    private fun makeManager() = User().apply {
        id = 2L
        firstName = "Иван"
        lastName = "Петров"
        email = "manager@test.com"
        passwordHash = "hash"
    }

    private fun makeLead(id: Long = 1L, course: Course? = null, manager: User? = null) = Lead().apply {
        this.id = id
        firstName = "Алексей"
        phone = "+79161234567"
        status = LeadStatus.NEW
        interestedCourse = course
        assignedManager = manager
    }

    @Test
    fun `create - полный набор полей - статус NEW и вложенные объекты заполнены`() {
        val course = makeCourse()
        val manager = makeManager()
        val request = LeadCreateRequest(
            firstName = "Алексей",
            phone = "+79161234567",
            interestedCourseId = 1L,
            assignedManagerId = 2L,
        )
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(course))
        whenever(userRepository.findById(2L)).thenReturn(Optional.of(manager))
        whenever(leadRepository.save(any<Lead>())).thenReturn(makeLead(course = course, manager = manager))

        val result = leadService.create(request)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.status).isEqualTo("NEW")
        assertThat(result.interestedCourse!!.id).isEqualTo(1L)
    }

    @Test
    fun `create - только обязательные поля - статус NEW вложенные объекты null`() {
        val request = LeadCreateRequest(firstName = "Мария", phone = "+79260001122")
        whenever(leadRepository.save(any<Lead>())).thenReturn(makeLead(id = 2L))

        val result = leadService.create(request)

        assertThat(result.status).isEqualTo("NEW")
        assertThat(result.interestedCourse).isNull()
        assertThat(result.assignedManager).isNull()
    }

    @Test
    fun `create - несуществующий interestedCourseId - ResourceNotFoundException save не вызывается`() {
        val request = LeadCreateRequest(firstName = "Тест", phone = "+70000000000", interestedCourseId = 99L)
        whenever(courseRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { leadService.create(request) }
            .isInstanceOf(ResourceNotFoundException::class.java)
        verify(leadRepository, never()).save(any())
    }

    private fun convertRequest() = LeadConvertRequest(
        firstName = "Иван", lastName = "Петров", phone = "+79001234567"
    )

    private fun makeStudent(id: Long = 10L) = Student().apply {
        this.id = id
        firstName = "Иван"
        lastName = "Петров"
        phone = "+79001234567"
    }

    @Test
    fun `convertToStudent - успех - студент создан лид обновлён userId null`() {
        val lead = makeLead(1L)
        val student = makeStudent()
        whenever(leadRepository.findById(1L)).thenReturn(Optional.of(lead))
        whenever(studentRepository.save(any<Student>())).thenReturn(student)
        whenever(leadRepository.save(any<Lead>())).thenReturn(lead)

        val result = leadService.convertToStudent(1L, convertRequest())

        assertThat(result.id).isEqualTo(10L)
        assertThat(result.userId).isNull()
        assertThat(lead.status).isEqualTo(LeadStatus.CONVERTED_TO_STUDENT)
        assertThat(lead.convertedStudent).isEqualTo(student)
        verify(studentRepository).save(any<Student>())
        verify(leadRepository).save(any<Lead>())
    }

    @Test
    fun `convertToStudent - лид не найден - ResourceNotFoundException studentRepository не вызывается`() {
        whenever(leadRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { leadService.convertToStudent(99L, convertRequest()) }
            .isInstanceOf(ResourceNotFoundException::class.java)
        verify(studentRepository, never()).save(any())
    }

    @Test
    fun `convertToStudent - лид уже конвертирован - ConflictException studentRepository не вызывается`() {
        val lead = makeLead(1L).apply { status = LeadStatus.CONVERTED_TO_STUDENT }
        whenever(leadRepository.findById(1L)).thenReturn(Optional.of(lead))

        assertThatThrownBy { leadService.convertToStudent(1L, convertRequest()) }
            .isInstanceOf(ConflictException::class.java)
        verify(studentRepository, never()).save(any())
    }

    @Test
    fun `getComments - лид найден с комментариями - возвращается отсортированный список`() {
        val lead = makeLead(1L)
        val author = makeManager()
        val c1 = LeadComment().apply { id = 1L; this.lead = lead; this.author = author; text = "Первый" }
        val c2 = LeadComment().apply { id = 2L; this.lead = lead; this.author = author; text = "Второй" }
        whenever(leadRepository.existsById(1L)).thenReturn(true)
        whenever(leadCommentRepository.findByLeadIdOrderByCreatedAtAsc(1L)).thenReturn(listOf(c1, c2))

        val result = leadService.getComments(1L)

        assertThat(result).hasSize(2)
        assertThat(result[0].text).isEqualTo("Первый")
        verify(leadCommentRepository).findByLeadIdOrderByCreatedAtAsc(1L)
    }

    @Test
    fun `getComments - лид найден без комментариев - возвращается пустой список`() {
        whenever(leadRepository.existsById(1L)).thenReturn(true)
        whenever(leadCommentRepository.findByLeadIdOrderByCreatedAtAsc(1L)).thenReturn(emptyList())

        val result = leadService.getComments(1L)

        assertThat(result).isEmpty()
    }

    @Test
    fun `getComments - лид не найден - ResourceNotFoundException findByLead не вызывается`() {
        whenever(leadRepository.existsById(99L)).thenReturn(false)

        assertThatThrownBy { leadService.getComments(99L) }
            .isInstanceOf(ResourceNotFoundException::class.java)
        verify(leadCommentRepository, never()).findByLeadIdOrderByCreatedAtAsc(any())
    }

    @Test
    fun `addComment - успешный сценарий - save вызван один раз поля заполнены`() {
        val lead = makeLead(1L)
        val author = makeManager()
        val savedComment = LeadComment().apply {
            id = 10L
            this.lead = lead
            this.author = author
            text = "Тестовый комментарий"
        }
        whenever(leadRepository.findById(1L)).thenReturn(Optional.of(lead))
        whenever(userRepository.findById(2L)).thenReturn(Optional.of(author))
        whenever(leadCommentRepository.save(any<LeadComment>())).thenReturn(savedComment)

        val result = leadService.addComment(1L, LeadCommentCreateRequest("Тестовый комментарий"), 2L)

        assertThat(result.id).isEqualTo(10L)
        assertThat(result.leadId).isEqualTo(1L)
        assertThat(result.text).isEqualTo("Тестовый комментарий")
        assertThat(result.author.id).isEqualTo(2L)
        verify(leadCommentRepository).save(any<LeadComment>())
    }

    @Test
    fun `addComment - лид не найден - ResourceNotFoundException save не вызывается`() {
        whenever(leadRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { leadService.addComment(99L, LeadCommentCreateRequest("Текст"), 2L) }
            .isInstanceOf(ResourceNotFoundException::class.java)
        verify(leadCommentRepository, never()).save(any())
    }

    @Test
    fun `addComment - автор не найден - ResourceNotFoundException save не вызывается`() {
        whenever(leadRepository.findById(1L)).thenReturn(Optional.of(makeLead(1L)))
        whenever(userRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { leadService.addComment(1L, LeadCommentCreateRequest("Текст"), 99L) }
            .isInstanceOf(ResourceNotFoundException::class.java)
        verify(leadCommentRepository, never()).save(any())
    }

    @Test
    fun `updateStatus - успешная смена статуса - save вызван один раз статус обновлён`() {
        val lead = makeLead(1L).apply { status = LeadStatus.NEW }
        val updated = makeLead(1L).apply { status = LeadStatus.IN_PROGRESS }
        whenever(leadRepository.findById(1L)).thenReturn(Optional.of(lead))
        whenever(leadRepository.save(any<Lead>())).thenReturn(updated)

        val result = leadService.updateStatus(1L, LeadStatusUpdateRequest(LeadStatus.IN_PROGRESS))

        assertThat(result.status).isEqualTo("IN_PROGRESS")
        verify(leadRepository).save(any<Lead>())
    }

    @Test
    fun `updateStatus - лид не найден - ResourceNotFoundException save не вызывается`() {
        whenever(leadRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { leadService.updateStatus(99L, LeadStatusUpdateRequest(LeadStatus.IN_PROGRESS)) }
            .isInstanceOf(ResourceNotFoundException::class.java)
        verify(leadRepository, never()).save(any())
    }

    @Test
    fun `updateStatus - статус CONVERTED_TO_STUDENT - ConflictException save не вызывается`() {
        val lead = makeLead(1L)
        whenever(leadRepository.findById(1L)).thenReturn(Optional.of(lead))

        assertThatThrownBy {
            leadService.updateStatus(1L, LeadStatusUpdateRequest(LeadStatus.CONVERTED_TO_STUDENT))
        }.isInstanceOf(ConflictException::class.java)
        verify(leadRepository, never()).save(any())
    }

    private fun updateRequest() = LeadUpdateRequest(firstName = "Алексей", phone = "+79161111111")

    @Test
    fun `update - успешное обновление - save вызван статус не изменился`() {
        val existing = makeLead(1L).apply { status = LeadStatus.IN_PROGRESS }
        val updated = makeLead(1L).apply { phone = "+79161111111"; status = LeadStatus.IN_PROGRESS }
        whenever(leadRepository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(leadRepository.save(any<Lead>())).thenReturn(updated)

        val result = leadService.update(1L, updateRequest())

        verify(leadRepository).save(any<Lead>())
        assertThat(result.status).isEqualTo("IN_PROGRESS")
    }

    @Test
    fun `update - конвертированный лид - BadRequestException save не вызывается`() {
        val converted = makeLead(1L).apply { status = LeadStatus.CONVERTED_TO_STUDENT }
        whenever(leadRepository.findById(1L)).thenReturn(Optional.of(converted))

        assertThatThrownBy { leadService.update(1L, updateRequest()) }
            .isInstanceOf(BadRequestException::class.java)
        verify(leadRepository, never()).save(any())
    }

    @Test
    fun `update - лид не найден - ResourceNotFoundException`() {
        whenever(leadRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { leadService.update(99L, updateRequest()) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `getById - лид найден - возвращается LeadResponse`() {
        whenever(leadRepository.findById(1L)).thenReturn(Optional.of(makeLead()))

        val result = leadService.getById(1L)

        assertThat(result).isNotNull
        assertThat(result.id).isEqualTo(1L)
        verify(leadRepository).findById(1L)
    }

    @Test
    fun `getById - лид не найден - выбрасывается ResourceNotFoundException`() {
        whenever(leadRepository.findById(99L)).thenReturn(Optional.empty())

        assertThatThrownBy { leadService.getById(99L) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `getAll - без фильтров - возвращает все лиды постранично`() {
        val pageable = PageRequest.of(0, 20)
        val leads = listOf(makeLead(1L), makeLead(2L))
        whenever(leadRepository.findAll(any<Specification<Lead>>(), any<Pageable>()))
            .thenReturn(PageImpl(leads, pageable, 2))

        val result = leadService.getAll(null, null, null, null, pageable)

        assertThat(result.totalElements).isEqualTo(2)
    }

    @Test
    fun `getAll - фильтр status NEW - findAll вызван один раз`() {
        val pageable = PageRequest.of(0, 20)
        whenever(leadRepository.findAll(any<Specification<Lead>>(), any<Pageable>()))
            .thenReturn(PageImpl(emptyList(), pageable, 0))

        leadService.getAll(LeadStatus.NEW, null, null, null, pageable)

        verify(leadRepository).findAll(any<Specification<Lead>>(), any<Pageable>())
    }
}
