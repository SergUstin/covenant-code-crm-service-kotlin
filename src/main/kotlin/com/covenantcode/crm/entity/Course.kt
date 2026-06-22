package com.covenantcode.crm.entity

import com.covenantcode.crm.entity.enums.CourseStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "courses")
class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var title: String = ""

    @Column(columnDefinition = "TEXT")
    var description: String? = null

    @Column(name = "duration_in_weeks", nullable = false)
    var durationInWeeks: Int = 0

    @Column(nullable = false, precision = 10, scale = 2)
    var price: BigDecimal = BigDecimal.ZERO

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: CourseStatus = CourseStatus.ACTIVE

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null
}
