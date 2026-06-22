package com.covenantcode.crm.service

import java.time.LocalDate
import java.time.LocalTime

interface LessonOverlapService {
    fun checkTeacherOverlap(
        teacherId: Long,
        lessonDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        excludeLessonId: Long?,
    )
}
