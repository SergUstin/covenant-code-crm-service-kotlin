package com.covenantcode.crm.repository

import com.covenantcode.crm.entity.Course
import com.covenantcode.crm.entity.enums.CourseStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface CourseRepository : JpaRepository<Course, Long> {
    fun findAllByStatus(status: CourseStatus, pageable: Pageable): Page<Course>
}
