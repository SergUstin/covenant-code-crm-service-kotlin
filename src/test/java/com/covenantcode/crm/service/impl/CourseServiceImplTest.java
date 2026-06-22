package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.course.CourseCreateRequest;
import com.covenantcode.crm.dto.course.CourseResponse;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.CourseMapper;
import com.covenantcode.crm.repository.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CourseServiceImplTest {

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private CourseRepository courseRepository;

    @InjectMocks
    private CourseServiceImpl courseService;

    private CourseCreateRequest request;
    private Course course;
    private Course savedCourse;
    private CourseResponse response;
    private CourseResponse courseResponse;

    @BeforeEach
    void setUp() {
        request = new CourseCreateRequest();
        request.setTitle("Java для начинающих");
        request.setDescription("Полный курс по Java");
        request.setDurationInWeeks(16);
        request.setPrice(new BigDecimal("45000.00"));
        request.setStatus(CourseStatus.ACTIVE);

        course = new Course();
        course.setId(1L);
        course.setTitle("Java Developer");
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setDurationInWeeks(request.getDurationInWeeks());
        course.setPrice(request.getPrice());
        course.setStatus(CourseStatus.ACTIVE);
        savedCourse = new Course();
        savedCourse.setId(1L);
        savedCourse.setTitle(request.getTitle());
        savedCourse.setDescription(request.getDescription());
        savedCourse.setDurationInWeeks(request.getDurationInWeeks());
        savedCourse.setPrice(request.getPrice());
        savedCourse.setStatus(CourseStatus.ACTIVE);
        savedCourse.setCreatedAt(OffsetDateTime.now());
        savedCourse.setUpdatedAt(OffsetDateTime.now());

        response = CourseResponse.builder()
                .id(1L)
                .title("Java Developer")
                .title(request.getTitle())
                .description(request.getDescription())
                .durationInWeeks(request.getDurationInWeeks())
                .price(request.getPrice())
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    public void create_success_withAllFields() {
        when(courseMapper.toEntity(request)).thenReturn(course);
        when(courseRepository.save(course)).thenReturn(savedCourse);
        when(courseMapper.toResponse(savedCourse)).thenReturn(response);
        CourseResponse result = courseService.create(request);
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("ACTIVE", result.getStatus());

        verify(courseRepository).save(course);
    }

    @Test
    public void create_statusNotProvided_setsActive() {
        request.setStatus(null);
        Course courseWithoutStatus = new Course();
        courseWithoutStatus.setTitle(request.getTitle());
        courseWithoutStatus.setDescription(request.getDescription());
        courseWithoutStatus.setDurationInWeeks(request.getDurationInWeeks());
        courseWithoutStatus.setPrice(request.getPrice());
        courseWithoutStatus.setStatus(null);

        when(courseMapper.toEntity(any(CourseCreateRequest.class))).thenReturn(courseWithoutStatus);
        when(courseRepository.save(any(Course.class))).thenReturn(savedCourse);
        when(courseMapper.toResponse(savedCourse)).thenReturn(response);

        CourseResponse result = courseService.create(request);

        ArgumentCaptor<Course> captor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(captor.capture());
        assertEquals(CourseStatus.ACTIVE, captor.getValue().getStatus());

        assertNotNull(result);
        assertEquals("ACTIVE", result.getStatus());
    }

    @Test
    void getById_WhenCourseExists_ShouldReturnResponse() {

        when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
        when(courseMapper.toResponse(course)).thenReturn(response);

        CourseResponse result = courseService.getById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Java для начинающих", result.getTitle());
        verify(courseRepository, times(1)).findById(1L);
        verify(courseMapper, times(1)).toResponse(course);
    }

    @Test
    void getById_WhenCourseDoesNotExist_ShouldThrowException() {
        when(courseRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> courseService.getById(999L));
        verify(courseRepository, times(1)).findById(999L);
        verify(courseMapper, never()).toResponse(any());
    }
}
