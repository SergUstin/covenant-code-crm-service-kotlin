package com.covenantcode.crm.entity

import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.OffsetDateTime

@Entity
@Table(name = "users")
class User : UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null

    @Column(name = "first_name", nullable = false, length = 100)
    var firstName: String = ""

    @Column(name = "last_name", nullable = false, length = 100)
    var lastName: String = ""

    @Column(nullable = false, unique = true)
    var email: String = ""

    // Named passwordHash to avoid JVM clash with getPassword() override below.
    // @Column(name="password") maps to the correct DB column.
    @Column(name = "password", nullable = false)
    var passwordHash: String = ""

    @Column(length = 20)
    var phone: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    lateinit var role: Role

    // Boolean property var enabled generates isEnabled() → auto-satisfies UserDetails.isEnabled()
    @Column(nullable = false)
    var enabled: Boolean = true

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: OffsetDateTime? = null

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null

    override fun getAuthorities(): Collection<GrantedAuthority> =
        listOf(SimpleGrantedAuthority("ROLE_${role.name.name}"))

    override fun getPassword(): String = passwordHash

    override fun getUsername(): String = email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true
}
