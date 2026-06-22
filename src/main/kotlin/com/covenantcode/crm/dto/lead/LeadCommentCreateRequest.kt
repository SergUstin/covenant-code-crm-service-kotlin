package com.covenantcode.crm.dto.lead

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LeadCommentCreateRequest(
    @field:NotBlank(message = "Текст комментария не может быть пустым")
    @field:Size(max = 1000, message = "Текст не может превышать 1000 символов")
    val text: String?,
)
