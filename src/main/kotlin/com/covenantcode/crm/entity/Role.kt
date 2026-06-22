package com.covenantcode.crm.entity

import com.covenantcode.crm.entity.enums.RoleName
import jakarta.persistence.*

@Entity
@Table(name = "roles")
class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true, length = 50)
    var name: RoleName = RoleName.STUDENT
}
