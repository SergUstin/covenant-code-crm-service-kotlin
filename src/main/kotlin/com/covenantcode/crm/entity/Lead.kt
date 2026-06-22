package com.covenantcode.crm.entity

import com.covenantcode.crm.entity.enums.LeadStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.OffsetDateTime

@Entity
@Table(name = "leads")
class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String = ""

    @Column(name = "last_name", length = 100)
    var lastName: String? = null

    @Column(nullable = false, length = 20)
    var phone: String = ""

    @Column
    var email: String? = null

    @Column
    var source: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interested_course_id")
    var interestedCourse: Course? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: LeadStatus = LeadStatus.NEW

    @Column(columnDefinition = "TEXT")
    var comment: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_manager_id")
    var assignedManager: User? = null

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_student_id", unique = true)
    var convertedStudent: Student? = null

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null
}
