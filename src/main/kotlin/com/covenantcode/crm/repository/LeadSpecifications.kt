package com.covenantcode.crm.repository

import com.covenantcode.crm.entity.Lead
import com.covenantcode.crm.entity.enums.LeadStatus
import org.springframework.data.jpa.domain.Specification

object LeadSpecifications {

    fun byStatus(status: LeadStatus): Specification<Lead> =
        Specification { root, _, cb -> cb.equal(root.get<LeadStatus>("status"), status) }

    fun byManager(managerId: Long): Specification<Lead> =
        Specification { root, _, cb ->
            cb.equal(root.get<Any>("assignedManager").get<Long>("id"), managerId)
        }

    fun byCourse(courseId: Long): Specification<Lead> =
        Specification { root, _, cb ->
            cb.equal(root.get<Any>("interestedCourse").get<Long>("id"), courseId)
        }

    fun bySearch(search: String): Specification<Lead> =
        Specification { root, _, cb ->
            val pattern = "%${search.lowercase()}%"
            cb.or(
                cb.like(cb.lower(root.get("firstName")), pattern),
                cb.like(cb.lower(root.get("lastName")), pattern),
                cb.like(cb.lower(root.get("phone")), pattern),
                cb.like(cb.lower(root.get("email")), pattern),
            )
        }
}
