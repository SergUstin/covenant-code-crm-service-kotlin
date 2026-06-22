package com.covenantcode.crm.mapper;

import com.covenantcode.crm.dto.lead.CourseShortResponse;
import com.covenantcode.crm.dto.lead.LeadResponse;
import com.covenantcode.crm.dto.lead.UserShortResponse;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Lead;
import com.covenantcode.crm.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Mapper
public interface LeadMapper {

    @Mapping(target = "convertedStudentId", source = "convertedStudent.id")
    LeadResponse toResponse(Lead lead);

    CourseShortResponse toCourseShortResponse(Course course);

    UserShortResponse toUserShortResponse(User user);

    default LocalDateTime map(OffsetDateTime offsetDateTime) {
        return offsetDateTime != null ? offsetDateTime.toLocalDateTime() : null;
    }
}
