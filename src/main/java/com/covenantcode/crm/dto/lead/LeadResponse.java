package com.covenantcode.crm.dto.lead;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String phone;

    private String email;

    private String source;

    private CourseShortResponse interestedCourse;

    private String status;

    private String comment;

    private UserShortResponse assignedManager;

    private Long convertedStudentId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
