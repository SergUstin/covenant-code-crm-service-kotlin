package com.covenantcode.crm.dto.course;

import com.covenantcode.crm.entity.enums.CourseStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class CourseCreateRequest {

    @NotBlank(message = "Название обязательно")
    @Size(max = 255, message = "Название курса не должно превышать 255 символов")
    private String title;

    private String description;

    @NotNull(message = "Продолжительность обязательна")
    @Positive(message = "Продолжительность не может быть 0 и меньше недель")
    private Integer durationInWeeks;

    @NotNull(message = "Цена обязательна")
    @PositiveOrZero(message = "Цена не может быть отрицательной")
    private BigDecimal price;

    private CourseStatus status;
}