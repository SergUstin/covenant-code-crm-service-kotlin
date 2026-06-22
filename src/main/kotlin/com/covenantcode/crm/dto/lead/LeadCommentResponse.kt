package com.covenantcode.crm.dto.lead

import java.time.OffsetDateTime

data class LeadCommentResponse(
    val id: Long,
    val leadId: Long,
    val author: UserShortResponse,
    val text: String,
    val createdAt: OffsetDateTime?,
)
