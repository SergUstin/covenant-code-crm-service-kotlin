package com.covenantcode.crm.repository

import com.covenantcode.crm.entity.Role
import com.covenantcode.crm.entity.enums.RoleName
import org.springframework.data.jpa.repository.JpaRepository

interface RoleRepository : JpaRepository<Role, Long> {
    fun findByName(name: RoleName): Role?
}
