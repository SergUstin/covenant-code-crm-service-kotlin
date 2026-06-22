package com.covenantcode.crm.dto.lead

import com.covenantcode.crm.entity.enums.LeadStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class LeadStatusUpdateRequest(
    @field:NotNull(message = "Статус обязателен")
    @Schema(example = "IN_PROGRESS")
    val status: LeadStatus?,
)
