package com.covenantcode.crm.repository

import com.covenantcode.crm.entity.Student
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface StudentRepository : JpaRepository<Student, Long>, JpaSpecificationExecutor<Student> {
    fun existsByUserId(userId: Long): Boolean
    fun findByUser_Id(userId: Long): Student?
}
