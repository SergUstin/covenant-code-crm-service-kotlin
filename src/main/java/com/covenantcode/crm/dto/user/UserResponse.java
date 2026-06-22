package com.covenantcode.crm.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description =  "Ответ с данными пользователя")
public class UserResponse {

    @Schema(description = "Идентификатор пользователя", example = "1")
    private Long id;

    @Schema(description = "Имя", example = "Иван")
    private String firstName;

    @Schema(description = "Фамилия", example = "Петров")
    private String lastName;

    @Schema(description = "Email", example = "ivan.petrov@gamil.com")
    private String email;

    @Schema(description = "Телефон", example = "+79161234567")
    private String phone;

    @Schema(description = "Роль пользователя", example = "ADMIN",
            allowableValues = {"ADMIN", "MANAGER", "TEACHER", "STUDENT"})
    private String role;

    @Schema(description = "Активен ли аккаунт", example = "true")
    private boolean enabled;

    @Schema(description = "Дата создания", example = "2025-01-15T10:00:00Z")
    private String createdAt;
}
