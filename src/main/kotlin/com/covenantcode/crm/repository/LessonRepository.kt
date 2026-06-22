package com.covenantcode.crm.repository

import com.covenantcode.crm.entity.Lesson
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.time.LocalDate

interface LessonRepository : JpaRepository<Lesson, Long>, JpaSpecificationExecutor<Lesson> {
    fun findByTeacher_IdAndLessonDate(teacherId: Long, lessonDate: LocalDate): List<Lesson>
    fun findByStudyGroupIdOrderByLessonDateAscStartTimeAsc(groupId: Long): List<Lesson>

}
