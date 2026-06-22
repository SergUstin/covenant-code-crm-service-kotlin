package com.covenantcode.crm.controller;

import com.covenantcode.crm.BaseIntegrationTest;
import com.covenantcode.crm.dto.lead.LeadCreateRequest;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Role;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.entity.enums.CourseStatus;
import com.covenantcode.crm.entity.enums.LeadStatus;
import com.covenantcode.crm.entity.enums.RoleName;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.RoleRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser(roles = "MANAGER")
public class LeadControllerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Course testCourse;
    private User testManager;
    private final String baseUrl = "/api/v1/leads";

    @BeforeEach
    void setUp() {
        testCourse = new Course();
        testCourse.setTitle("Test Course");
        testCourse.setDescription("Integration test course");
        testCourse.setPrice(BigDecimal.valueOf(199.99));
        testCourse.setDurationInWeeks(8);
        testCourse.setStatus(CourseStatus.ACTIVE);
        testCourse = courseRepository.save(testCourse);

        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> {
                    Role newRole = new Role();
                    newRole.setName(RoleName.MANAGER);
                    return roleRepository.save(newRole);
                });

        testManager = new User();
        testManager.setFirstName("Manager");
        testManager.setLastName("Test");
        testManager.setEmail("manager@test.com");
        testManager.setPassword(passwordEncoder.encode("password"));
        testManager.setRole(managerRole);
        testManager.setEnabled(true);
        testManager = userRepository.save(testManager);
    }

    @Test
    @DisplayName("Создание лида со всеми полями -> HTTP 201, статус NEW, курс и менеджер проставлены")
    void createLead_fullFields_shouldReturn201() throws Exception {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setFirstName("Ivan");
        request.setLastName("Petrov");
        request.setPhone("+79001234567");
        request.setEmail("ivan@example.com");
        request.setSource("website");
        request.setInterestedCourseId(testCourse.getId());
        request.setAssignedManagerId(testManager.getId());
        request.setComment("Integration test");

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(LeadStatus.NEW.name()))
                .andExpect(jsonPath("$.interestedCourse.id").value(testCourse.getId()))
                .andExpect(jsonPath("$.interestedCourse.title").value(testCourse.getTitle()))
                .andExpect(jsonPath("$.assignedManager.id").value(testManager.getId()))
                .andExpect(jsonPath("$.assignedManager.firstName").value(testManager.getFirstName()))
                .andExpect(jsonPath("$.assignedManager.lastName").value(testManager.getLastName()));
    }

    @Test
    @DisplayName("Создание лида только с обязательными полями -> HTTP 201, статус NEW, курс и менеджер null")
    void createLead_minimalFields_shouldReturn201() throws Exception {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setFirstName("John");
        request.setPhone("+79998887766");

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value(LeadStatus.NEW.name()))
                .andExpect(jsonPath("$.interestedCourse").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.assignedManager").value(Matchers.nullValue()));
    }

    @Test
    @DisplayName("Передан несуществующий interestedCourseId -> HTTP 404, тип ошибки resource-not-found")
    void createLead_invalidCourseId_shouldReturn404() throws Exception {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setFirstName("Anna");
        request.setPhone("+79001112233");
        request.setInterestedCourseId(9999L);
        request.setAssignedManagerId(testManager.getId());

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("resource-not-found"))
                .andExpect(jsonPath("$.detail").value(containsString("Course")))
                .andExpect(jsonPath("$.detail").value(containsString("9999")));
    }

    @Test
    @DisplayName("Отсутствует firstName -> HTTP 400, ошибка валидации для поля firstName")
    void createLead_emptyFirstName_shouldReturn400() throws Exception {
        LeadCreateRequest request = new LeadCreateRequest();
        request.setPhone("+79001112233");

        mockMvc.perform(post(baseUrl)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("validation-error"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("firstName"))
                .andExpect(jsonPath("$.errors[0].message").value(containsString("обязательно")));
    }
}
