package com.covenantcode.crm.dto.user

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class EnabledUpdateRequest(
    @field:NotNull(message = "Поле enabled обязательно")
    @Schema(example = "false")
    val enabled: Boolean?,
)
