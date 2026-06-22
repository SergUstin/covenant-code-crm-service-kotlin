package com.covenantcode.crm.service

import com.covenantcode.crm.dto.lead.LeadCommentCreateRequest
import com.covenantcode.crm.dto.lead.LeadCommentResponse
import com.covenantcode.crm.dto.lead.LeadConvertRequest
import com.covenantcode.crm.dto.lead.LeadCreateRequest
import com.covenantcode.crm.dto.lead.LeadResponse
import com.covenantcode.crm.dto.lead.LeadStatusUpdateRequest
import com.covenantcode.crm.dto.lead.LeadUpdateRequest
import com.covenantcode.crm.dto.student.StudentResponse
import com.covenantcode.crm.entity.enums.LeadStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface LeadService {
    fun create(request: LeadCreateRequest): LeadResponse
    fun addComment(leadId: Long, request: LeadCommentCreateRequest, authorId: Long): LeadCommentResponse
    fun getComments(leadId: Long): List<LeadCommentResponse>
    fun convertToStudent(leadId: Long, request: LeadConvertRequest): StudentResponse
    fun getById(id: Long): LeadResponse
    fun update(id: Long, request: LeadUpdateRequest): LeadResponse
    fun updateStatus(id: Long, request: LeadStatusUpdateRequest): LeadResponse
    fun getAll(
        status: LeadStatus?,
        managerId: Long?,
        courseId: Long?,
        search: String?,
        pageable: Pageable,
    ): Page<LeadResponse>
}
