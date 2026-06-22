package com.covenantcode.crm.repository

import com.covenantcode.crm.entity.LeadComment
import org.springframework.data.jpa.repository.JpaRepository

interface LeadCommentRepository : JpaRepository<LeadComment, Long> {
    fun findByLeadIdOrderByCreatedAtAsc(leadId: Long): List<LeadComment>
}
