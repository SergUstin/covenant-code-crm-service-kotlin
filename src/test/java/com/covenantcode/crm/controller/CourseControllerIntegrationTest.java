package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.dto.course.CourseCreateRequest;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import com.covenantcode.crm.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
public class CourseControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCourse_success_returns201() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Java для начинающих");
        request.setDescription("Полный курс по Java");
        request.setDurationInWeeks(16);
        request.setPrice(new BigDecimal("45000.00"));
        request.setStatus(CourseStatus.ACTIVE);

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Java для начинающих"))
                .andExpect(jsonPath("$.durationInWeeks").value(16))
                .andExpect(jsonPath("$.price").value(45000.00))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCourse_statusNotProvided_statusActiveInResponse() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Бесплатный курс");
        request.setDurationInWeeks(8);
        request.setPrice(BigDecimal.ZERO);
        request.setStatus(null);

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCourse_negativeDuration_returns400() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Некорректный курс");
        request.setDurationInWeeks(-5);
        request.setPrice(new BigDecimal("10000.00"));

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCourse_negativePrice_returns400() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Курс с отрицательной ценой");
        request.setDurationInWeeks(10);
        request.setPrice(new BigDecimal("-500.00"));

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    public void createCourse_zeroDuration_returns400() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Курс с нулевой длительностью");
        request.setDurationInWeeks(0);
        request.setPrice(new BigDecimal("10000.00"));

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"));
    }

    @Test
    @DisplayName("POST /api/v1/courses - TEACHER → 403 Forbidden")
    @WithMockUser(roles = "TEACHER")
    void createCourse_withTeacherRole_returns403() throws Exception {
        CourseCreateRequest request = new CourseCreateRequest();
        request.setTitle("Тестовый курс");
        request.setDescription("Описание");
        request.setDurationInWeeks(10);
        request.setPrice(new BigDecimal("10000.00"));

        mockMvc.perform(post("/api/v1/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getById_WhenCourseExists_ShouldReturnOk() throws Exception {

        Course course = new Course();
        course.setTitle("Spring Boot Master");
        course.setDescription("Advanced course");
        course.setPrice(BigDecimal.valueOf(100.0));
        course.setDurationInWeeks(8);
        course.setStatus(CourseStatus.ACTIVE);

        Course savedCourse = courseRepository.save(course);

        mockMvc.perform(get("/api/v1/courses/{id}", savedCourse.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(savedCourse.getId()))
                .andExpect(jsonPath("$.title").value("Spring Boot Master"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getById_WhenCourseDoesNotExist_ShouldReturnNotFound() throws Exception {

        mockMvc.perform(get("/api/v1/courses/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Disabled
    @Test
    void getById_WhenUnauthorized_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/courses/1"))
                .andExpect(status().isUnauthorized());
    }
}
