package com.covenantcode.crm.repository

import com.covenantcode.crm.entity.Lesson
import com.covenantcode.crm.entity.Student
import com.covenantcode.crm.entity.StudyGroup
import org.springframework.data.jpa.domain.Specification
import java.time.LocalDate

object LessonSpecifications {

    fun byStudent(studentId: Long): Specification<Lesson> = Specification { root, query, cb ->
        val sub = query!!.subquery(StudyGroup::class.java)
        val group = sub.from(StudyGroup::class.java)
        val student = group.join<StudyGroup, Student>("students")
        sub.select(group).where(cb.equal(student.get<Long>("id"), studentId))
        cb.`in`(root.get<Any>("studyGroup")).value(sub)
    }

    fun byGroup(groupId: Long?): Specification<Lesson> = Specification { root, _, cb ->
        if (groupId == null) cb.conjunction()
        else cb.equal(root.get<Any>("studyGroup").get<Any>("id"), groupId)
    }

    fun byTeacher(teacherId: Long?): Specification<Lesson> = Specification { root, _, cb ->
        if (teacherId == null) cb.conjunction()
        else cb.equal(root.get<Any>("teacher").get<Any>("id"), teacherId)
    }

    fun fromDate(dateFrom: LocalDate?): Specification<Lesson> = Specification { root, _, cb ->
        if (dateFrom == null) cb.conjunction()
        else cb.greaterThanOrEqualTo(root.get("lessonDate"), dateFrom)
    }

    fun toDate(dateTo: LocalDate?): Specification<Lesson> = Specification { root, _, cb ->
        if (dateTo == null) cb.conjunction()
        else cb.lessThanOrEqualTo(root.get("lessonDate"), dateTo)
    }
}
