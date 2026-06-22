package com.covenantcode.crm.dto.lead;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LeadCreateRequest {

    @NotBlank(message = "Имя обязательно для заполнения")
    @Size(max = 100, message = "Имя не должно превышать 100 символов")
    private String firstName;

    @Size(max = 100, message = "Фамилия не должна превышать 100 символов")
    private String lastName;

    @NotBlank(message = "Телефон обязателен для заполнения")
    @Size(max = 20, message = "Телефон не должен превышать 20 символов")
    private String phone;

    @Email(message = "Некорректный формат email")
    @Size(max = 255, message = "Email не должен превышать 255 символов")
    private String email;

    @Size(max = 255, message = "Источник не должен превышать 255 символов")
    private String source;

    private Long interestedCourseId;

    private String comment;

    private Long assignedManagerId;
}
