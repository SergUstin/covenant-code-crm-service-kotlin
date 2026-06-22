package com.covenantcode.crm.service

import com.covenantcode.crm.dto.group.GroupStatusUpdateRequest
import com.covenantcode.crm.dto.group.StudyGroupCreateRequest
import com.covenantcode.crm.dto.group.StudyGroupResponse
import com.covenantcode.crm.dto.group.StudyGroupUpdateRequest
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.GroupStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface StudyGroupService {
    fun create(request: StudyGroupCreateRequest): StudyGroupResponse
    fun update(id: Long, request: StudyGroupUpdateRequest): StudyGroupResponse
    fun updateStatus(id: Long, request: GroupStatusUpdateRequest): StudyGroupResponse
    fun getById(id: Long, currentUser: User): StudyGroupResponse
    fun getAll(courseId: Long?, teacherId: Long?, status: GroupStatus?, pageable: Pageable): Page<StudyGroupResponse>
}
