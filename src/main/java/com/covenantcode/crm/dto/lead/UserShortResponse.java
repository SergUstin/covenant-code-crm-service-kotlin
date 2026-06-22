package com.covenantcode.crm.dto.lead;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserShortResponse {

    private Long id;
    private String firstName;
    private String lastName;

}
