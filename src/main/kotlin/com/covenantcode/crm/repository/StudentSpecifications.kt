package com.covenantcode.crm.repository

import com.covenantcode.crm.entity.Student
import org.springframework.data.jpa.domain.Specification

object StudentSpecifications {
    fun bySearch(search: String): Specification<Student> = Specification { root, _, cb ->
        val pattern = "%${search.lowercase()}%"
        cb.or(
            cb.like(cb.lower(root.get("firstName")), pattern),
            cb.like(cb.lower(root.get("lastName")), pattern),
            cb.like(cb.lower(root.get("phone")), pattern),
            cb.like(cb.lower(root.get("email")), pattern),
        )
    }
}
