package com.covenantcode.crm.service

import com.covenantcode.crm.dto.course.CourseCreateRequest
import com.covenantcode.crm.dto.course.CourseUpdateRequest
import com.covenantcode.crm.entity.Course
import com.covenantcode.crm.entity.enums.CourseStatus
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.exception.ResourceNotFoundException
import com.covenantcode.crm.repository.CourseRepository
import com.covenantcode.crm.repository.StudyGroupRepository
import com.covenantcode.crm.service.impl.CourseServiceImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.math.BigDecimal
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class CourseServiceImplTest {

    @Mock private lateinit var courseRepository: CourseRepository
    @Mock private lateinit var studyGroupRepository: StudyGroupRepository
    @InjectMocks private lateinit var courseService: CourseServiceImpl

    private fun savedCourse(status: CourseStatus = CourseStatus.ACTIVE) = Course().apply {
        id = 1L
        title = "Java для начинающих"
        description = "Описание"
        durationInWeeks = 16
        price = BigDecimal("45000.00")
        this.status = status
    }

    @Test
    fun `create - успешное создание курса со всеми полями`() {
        val request = CourseCreateRequest(
            title = "Java для начинающих",
            description = "Описание",
            durationInWeeks = 16,
            price = BigDecimal("45000.00"),
            status = CourseStatus.ACTIVE,
        )
        whenever(courseRepository.save(any<Course>())).thenReturn(savedCourse())

        val result = courseService.create(request)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.status).isEqualTo("ACTIVE")
        verify(courseRepository).save(any<Course>())
    }

    @Test
    fun `create - статус не передан - устанавливается ACTIVE`() {
        val request = CourseCreateRequest(
            title = "Вводный курс",
            durationInWeeks = 8,
            price = BigDecimal.ZERO,
            status = null,
        )
        val captor = argumentCaptor<Course>()
        whenever(courseRepository.save(captor.capture())).thenReturn(savedCourse())

        val result = courseService.create(request)

        assertThat(captor.firstValue.status).isEqualTo(CourseStatus.ACTIVE)
        assertThat(result.status).isEqualTo("ACTIVE")
    }

    @Test
    fun `getById - курс найден - возвращается CourseResponse`() {
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(savedCourse()))

        val result = courseService.getById(1L)

        assertThat(result.id).isEqualTo(1L)
        assertThat(result.title).isEqualTo("Java для начинающих")
    }

    @Test
    fun `getById - курс не найден - выбрасывается ResourceNotFoundException`() {
        whenever(courseRepository.findById(999L)).thenReturn(Optional.empty())

        assertThatThrownBy { courseService.getById(999L) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    private fun makeCourse(id: Long, status: CourseStatus) = Course().apply {
        this.id = id
        title = "Course $id"
        durationInWeeks = 8
        price = BigDecimal.ZERO
        this.status = status
    }

    @Test
    fun `getAll - без фильтра возвращает все курсы и не вызывает findAllByStatus`() {
        val pageable = PageRequest.of(0, 20)
        val courses = listOf(makeCourse(1L, CourseStatus.ACTIVE), makeCourse(2L, CourseStatus.ARCHIVED))
        whenever(courseRepository.findAll(any<Pageable>())).thenReturn(PageImpl(courses, pageable, 2))

        val result = courseService.getAll(null, pageable)

        assertThat(result.totalElements).isEqualTo(2)
        verify(courseRepository, never()).findAllByStatus(any(), any())
    }

    @Test
    fun `getAll - фильтр ACTIVE возвращает только активные курсы и не вызывает findAll`() {
        val pageable = PageRequest.of(0, 20)
        val courses = listOf(makeCourse(1L, CourseStatus.ACTIVE))
        whenever(courseRepository.findAllByStatus(eq(CourseStatus.ACTIVE), any<Pageable>()))
            .thenReturn(PageImpl(courses, pageable, 1))

        val result = courseService.getAll(CourseStatus.ACTIVE, pageable)

        assertThat(result.totalElements).isEqualTo(1)
        assertThat(result.content[0].status).isEqualTo("ACTIVE")
        verify(courseRepository, never()).findAll(any<Pageable>())
    }

    @Test
    fun `delete - успешное удаление - deleteById вызван один раз`() {
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(savedCourse()))
        whenever(studyGroupRepository.existsByCourseIdAndStatus(1L, GroupStatus.ACTIVE)).thenReturn(false)

        courseService.delete(1L)

        verify(courseRepository).deleteById(1L)
    }

    @Test
    fun `delete - курс не найден - ResourceNotFoundException, остальные методы не вызываются`() {
        whenever(courseRepository.findById(999L)).thenReturn(Optional.empty())

        assertThatThrownBy { courseService.delete(999L) }
            .isInstanceOf(ResourceNotFoundException::class.java)
        verify(studyGroupRepository, never()).existsByCourseIdAndStatus(any(), any())
        verify(courseRepository, never()).deleteById(any())
    }

    @Test
    fun `delete - есть активные группы - ConflictException, deleteById не вызывается`() {
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(savedCourse()))
        whenever(studyGroupRepository.existsByCourseIdAndStatus(1L, GroupStatus.ACTIVE)).thenReturn(true)

        assertThatThrownBy { courseService.delete(1L) }
            .isInstanceOf(ConflictException::class.java)
        verify(courseRepository, never()).deleteById(any())
    }

    @Test
    fun `update - успешное обновление - поля изменились`() {
        val request = CourseUpdateRequest(
            title = "Java для начинающих — обновлённая версия",
            description = "Курс обновлён с учётом Java 21.",
            durationInWeeks = 20,
            price = BigDecimal("49900.00"),
            status = CourseStatus.ACTIVE,
        )
        val existing = savedCourse()
        val updatedCourse = Course().apply {
            id = 1L
            title = request.title
            description = request.description
            durationInWeeks = request.durationInWeeks!!
            price = request.price!!
            status = request.status!!
        }
        whenever(courseRepository.findById(1L)).thenReturn(Optional.of(existing))
        whenever(courseRepository.save(any<Course>())).thenReturn(updatedCourse)

        val result = courseService.update(1L, request)

        assertThat(result.title).isEqualTo("Java для начинающих — обновлённая версия")
        assertThat(result.price).isEqualByComparingTo(BigDecimal("49900.00"))
        verify(courseRepository).save(any<Course>())
    }

    @Test
    fun `update - курс не найден - выбрасывается ResourceNotFoundException и save не вызывается`() {
        val request = CourseUpdateRequest(
            title = "Курс",
            durationInWeeks = 10,
            price = BigDecimal.ZERO,
            status = CourseStatus.ACTIVE,
        )
        whenever(courseRepository.findById(999L)).thenReturn(Optional.empty())

        assertThatThrownBy { courseService.update(999L, request) }
            .isInstanceOf(ResourceNotFoundException::class.java)
        verify(courseRepository, never()).save(any<Course>())
    }

    @Test
    fun `getAll - фильтр ARCHIVED пустой результат возвращает 0 элементов`() {
        val pageable = PageRequest.of(0, 20)
        whenever(courseRepository.findAllByStatus(eq(CourseStatus.ARCHIVED), any<Pageable>()))
            .thenReturn(PageImpl(emptyList(), pageable, 0))

        val result = courseService.getAll(CourseStatus.ARCHIVED, pageable)

        assertThat(result.totalElements).isEqualTo(0)
        assertThat(result.content).isEmpty()
    }
}
