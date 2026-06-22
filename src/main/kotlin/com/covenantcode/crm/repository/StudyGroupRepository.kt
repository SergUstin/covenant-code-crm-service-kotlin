package com.covenantcode.crm.repository

import com.covenantcode.crm.entity.Student
import com.covenantcode.crm.entity.StudyGroup
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.GroupStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface StudyGroupRepository : JpaRepository<StudyGroup, Long>, JpaSpecificationExecutor<StudyGroup> {
    fun existsByCourseIdAndStatus(courseId: Long, status: GroupStatus): Boolean
    fun existsByTeacherAndStudentsContaining(teacher: User, student: Student): Boolean
    fun existsByStudents_IdAndStatus(studentId: Long, status: GroupStatus): Boolean
}
