package com.covenantcode.crm.service;

import com.covenantcode.crm.dto.course.CourseCreateRequest;
import com.covenantcode.crm.dto.course.CourseResponse;

public interface CourseService {
    CourseResponse create(CourseCreateRequest request);
    CourseResponse getById(Long id);
}
