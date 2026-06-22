package com.covenantcode.crm.dto.group

import com.covenantcode.crm.entity.enums.GroupStatus
import jakarta.validation.constraints.NotNull

data class GroupStatusUpdateRequest(
    @field:NotNull(message = "Новый статус обязателен")
    val status: GroupStatus?,
)
