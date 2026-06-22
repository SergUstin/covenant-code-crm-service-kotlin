package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.course.CourseCreateRequest;
import com.covenantcode.crm.dto.course.CourseResponse;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.CourseMapper;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseRepository courseRepository;

    @Override
    @Transactional
    public CourseResponse create(CourseCreateRequest request) {
        Course course = courseMapper.toEntity(request);

        if (course.getStatus() == null) {
            course.setStatus(CourseStatus.ACTIVE);
        }

        Course savedCourse = courseRepository.save(course);
        return courseMapper.toResponse(savedCourse);
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getById(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course", id));
        return courseMapper.toResponse(course);
    }
}
