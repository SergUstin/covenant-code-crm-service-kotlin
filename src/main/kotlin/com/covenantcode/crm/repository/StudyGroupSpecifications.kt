package com.covenantcode.crm.repository

import com.covenantcode.crm.entity.StudyGroup
import com.covenantcode.crm.entity.enums.GroupStatus
import org.springframework.data.jpa.domain.Specification

object StudyGroupSpecifications {

    fun byCourseId(courseId: Long?): Specification<StudyGroup> = Specification { root, _, cb ->
        if (courseId == null) cb.conjunction()
        else cb.equal(root.get<Any>("course").get<Any>("id"), courseId)
    }

    fun byTeacherId(teacherId: Long?): Specification<StudyGroup> = Specification { root, _, cb ->
        if (teacherId == null) cb.conjunction()
        else cb.equal(root.get<Any>("teacher").get<Any>("id"), teacherId)
    }

    fun byStatus(status: GroupStatus?): Specification<StudyGroup> = Specification { root, _, cb ->
        if (status == null) cb.conjunction()
        else cb.equal(root.get<Any>("status"), status)
    }
}
