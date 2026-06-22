package com.covenantcode.crm.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Ответ с данными о курсе")
public class CourseResponse {

    @Schema(description = "Идентификатор курса", example = "1")
    private Long id;

    @Schema(description = "Название", example = "Java-developer pro max plus platinum")
    private String title;

    @Schema(description = "Описание")
    private String description;

    @Schema(description = "Продолжительность курса", example = "6")
    private Integer durationInWeeks;

    @Schema(description = "Цена", example = "100000")
    private BigDecimal price;

    @Schema(description = "Статус курса", example = "ACTIVE",
            allowableValues = {"ACTIVE", "ARCHIVED"})
    private String status;

    @Schema(description = "Дата создания", example = "2025-01-15T10:00:00Z")
    private OffsetDateTime createdAt;

    @Schema(description = "Дата обновления", example = "2025-01-15T10:00:00Z")
    private OffsetDateTime updatedAt;
}
