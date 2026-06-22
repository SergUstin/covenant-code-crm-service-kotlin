package com.covenantcode.crm.mapper

import com.covenantcode.crm.dto.lead.LeadCommentResponse
import com.covenantcode.crm.dto.lead.UserShortResponse
import com.covenantcode.crm.entity.LeadComment

fun LeadComment.toResponse() = LeadCommentResponse(
    id = id!!,
    leadId = lead.id!!,
    author = UserShortResponse(author.id!!, author.firstName, author.lastName),
    text = text,
    createdAt = createdAt,
)
