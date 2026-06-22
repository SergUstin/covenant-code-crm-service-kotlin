package com.covenantcode.crm.controller;

import com.covenantcode.crm.dto.lead.LeadCreateRequest;
import com.covenantcode.crm.dto.lead.LeadResponse;
import com.covenantcode.crm.service.LeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadService leadService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Создание нового лида", description = "Создаёт с полями и статусом NEW")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Лид успешно создан",
                    content = @Content(schema = @Schema(implementation = LeadResponse.class))),
            @ApiResponse(responseCode = "400", description = "Некорректные данные запроса (ошибка валидации)"),
            @ApiResponse(responseCode = "401", description = "Пользователь не аутентифицирован"),
            @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется ADMIN или MANAGER)"),
            @ApiResponse(responseCode = "404", description = "Курс или менеджер не найдены (передан несуществующий ID)")
    })
    public LeadResponse create(@Valid @RequestBody LeadCreateRequest request) {
        return leadService.create(request);
    }
}





