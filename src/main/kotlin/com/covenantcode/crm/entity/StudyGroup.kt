package com.covenantcode.crm.entity

import com.covenantcode.crm.entity.enums.GroupStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.OffsetDateTime

@Entity
@Table(name = "study_groups")
class StudyGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(nullable = false)
    var name: String = ""

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    lateinit var course: Course

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    lateinit var teacher: User

    // MutableSet prevents duplicates and matches composite PK in study_group_students
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "study_group_students",
        joinColumns = [JoinColumn(name = "study_group_id")],
        inverseJoinColumns = [JoinColumn(name = "student_id")],
    )
    var students: MutableSet<Student> = mutableSetOf()

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate = LocalDate.now()

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    var status: GroupStatus = GroupStatus.DRAFT

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null
}
