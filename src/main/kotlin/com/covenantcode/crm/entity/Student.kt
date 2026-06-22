package com.covenantcode.crm.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "students")
class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    // nullable: student can exist without a system account
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    var user: User? = null

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String = ""

    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String = ""

    @Column(length = 20)
    var phone: String? = null

    @Column
    var email: String? = null

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null
}
