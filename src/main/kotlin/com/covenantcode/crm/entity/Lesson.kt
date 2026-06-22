package com.covenantcode.crm.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

@Entity
@Table(name = "lessons")
class Lesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "study_group_id", nullable = false)
    lateinit var studyGroup: StudyGroup

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    lateinit var teacher: User

    @Column(nullable = false, length = 500)
    var topic: String = ""

    @Column(columnDefinition = "TEXT")
    var description: String? = null

    @Column(name = "lesson_date", nullable = false)
    var lessonDate: LocalDate = LocalDate.now()

    @Column(name = "start_time", nullable = false)
    var startTime: LocalTime = LocalTime.MIDNIGHT

    @Column(name = "end_time", nullable = false)
    var endTime: LocalTime = LocalTime.MIDNIGHT

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null
}
