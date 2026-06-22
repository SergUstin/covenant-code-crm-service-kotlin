package com.covenantcode.crm.service

import com.covenantcode.crm.entity.Lesson
import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.repository.LessonRepository
import com.covenantcode.crm.service.impl.LessonOverlapServiceImpl
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalTime

@ExtendWith(MockitoExtension::class)
class LessonOverlapServiceImplTest {

    @Mock private lateinit var lessonRepository: LessonRepository
    @InjectMocks private lateinit var overlapService: LessonOverlapServiceImpl

    private val date = LocalDate.of(2026, 6, 10)
    private val teacherId = 1L

    private fun makeLesson(id: Long, start: LocalTime, end: LocalTime) = Lesson().apply {
        this.id = id
        startTime = start
        endTime = end
    }

    @Test
    fun `нет занятий в этот день - исключение не выбрасывается`() {
        whenever(lessonRepository.findByTeacher_IdAndLessonDate(any(), any())).thenReturn(emptyList())

        assertThatCode {
            overlapService.checkTeacherOverlap(teacherId, date, LocalTime.of(10, 0), LocalTime.of(11, 30), null)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `занятие примыкает вплотную - конфликта нет (граничный случай)`() {
        val existing = makeLesson(1L, LocalTime.of(9, 0), LocalTime.of(10, 0))
        whenever(lessonRepository.findByTeacher_IdAndLessonDate(any(), any())).thenReturn(listOf(existing))

        assertThatCode {
            overlapService.checkTeacherOverlap(teacherId, date, LocalTime.of(10, 0), LocalTime.of(11, 0), null)
        }.doesNotThrowAnyException()
    }

    @Test
    fun `занятия пересекаются - ConflictException`() {
        val existing = makeLesson(1L, LocalTime.of(9, 0), LocalTime.of(11, 0))
        whenever(lessonRepository.findByTeacher_IdAndLessonDate(any(), any())).thenReturn(listOf(existing))

        assertThatThrownBy {
            overlapService.checkTeacherOverlap(teacherId, date, LocalTime.of(10, 0), LocalTime.of(12, 0), null)
        }.isInstanceOf(ConflictException::class.java)
    }

    @Test
    fun `частичное перекрытие в начале - ConflictException`() {
        val existing = makeLesson(1L, LocalTime.of(10, 0), LocalTime.of(12, 0))
        whenever(lessonRepository.findByTeacher_IdAndLessonDate(any(), any())).thenReturn(listOf(existing))

        assertThatThrownBy {
            overlapService.checkTeacherOverlap(teacherId, date, LocalTime.of(9, 0), LocalTime.of(10, 30), null)
        }.isInstanceOf(ConflictException::class.java)
    }

    @Test
    fun `новое занятие полностью внутри существующего - ConflictException`() {
        val existing = makeLesson(1L, LocalTime.of(9, 0), LocalTime.of(12, 0))
        whenever(lessonRepository.findByTeacher_IdAndLessonDate(any(), any())).thenReturn(listOf(existing))

        assertThatThrownBy {
            overlapService.checkTeacherOverlap(teacherId, date, LocalTime.of(10, 0), LocalTime.of(11, 0), null)
        }.isInstanceOf(ConflictException::class.java)
    }

    @Test
    fun `excludeLessonId исключает занятие из проверки - конфликта нет`() {
        val existing = makeLesson(5L, LocalTime.of(9, 0), LocalTime.of(11, 0))
        whenever(lessonRepository.findByTeacher_IdAndLessonDate(any(), any())).thenReturn(listOf(existing))

        assertThatCode {
            overlapService.checkTeacherOverlap(teacherId, date, LocalTime.of(9, 0), LocalTime.of(11, 0), excludeLessonId = 5L)
        }.doesNotThrowAnyException()
    }
}
