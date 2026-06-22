package com.covenantcode.crm.dto.group

import jakarta.validation.constraints.NotNull

data class AddStudentToGroupRequest(
    @field:NotNull(message = "ID студента обязателен")
    val studentId: Long?,
)
