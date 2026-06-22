package com.covenantcode.crm.service.impl

import com.covenantcode.crm.exception.ConflictException
import com.covenantcode.crm.repository.LessonRepository
import com.covenantcode.crm.service.LessonOverlapService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalTime

@Service
class LessonOverlapServiceImpl(
    private val lessonRepository: LessonRepository,
) : LessonOverlapService {

    @Transactional(readOnly = true)
    override fun checkTeacherOverlap(
        teacherId: Long,
        lessonDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        excludeLessonId: Long?,
    ) {
        val existing = lessonRepository.findByTeacher_IdAndLessonDate(teacherId, lessonDate)
            .filter { excludeLessonId == null || it.id != excludeLessonId }

        for (lesson in existing) {
            // Two intervals [A,B) and [C,D) overlap iff A < D && C < B
            if (startTime < lesson.endTime && lesson.startTime < endTime) {
                throw ConflictException(
                    "У преподавателя уже есть занятие $lessonDate с ${lesson.startTime} до ${lesson.endTime}"
                )
            }
        }
    }
}
