package com.covenantcode.crm.mapper;

import com.covenantcode.crm.dto.course.CourseCreateRequest;

import com.covenantcode.crm.dto.course.CourseResponse;
import com.covenantcode.crm.entity.Course;
import org.mapstruct.Mapper;

@Mapper
public interface CourseMapper {

    CourseResponse toResponse(Course course);
    Course toEntity(CourseCreateRequest request);
}
