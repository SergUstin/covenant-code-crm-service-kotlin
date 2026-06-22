package com.covenantcode.crm.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class EnabledUpdateRequest {

    @NotNull
    private Boolean enabled;

}