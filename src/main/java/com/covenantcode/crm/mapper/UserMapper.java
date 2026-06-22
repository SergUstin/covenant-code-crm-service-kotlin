package com.covenantcode.crm.mapper;

import com.covenantcode.crm.dto.user.UserResponse;
import com.covenantcode.crm.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Mapper
public interface UserMapper {

    @Mapping(target = "role", expression = "java(user.getRole().getName().name())")
    UserResponse toResponse(User user);

    default String map(OffsetDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

}
