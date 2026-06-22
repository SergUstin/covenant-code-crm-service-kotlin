package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.lead.CourseShortResponse;
import com.covenantcode.crm.dto.lead.LeadCreateRequest;
import com.covenantcode.crm.dto.lead.LeadResponse;
import com.covenantcode.crm.dto.lead.UserShortResponse;
import com.covenantcode.crm.entity.Course;
import com.covenantcode.crm.entity.Lead;
import com.covenantcode.crm.entity.enums.LeadStatus;
import com.covenantcode.crm.entity.User;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.LeadMapper;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.LeadRepository;
import com.covenantcode.crm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeadServiceImplTest {

        @Mock
        private LeadRepository leadRepository;

        @Mock
        private CourseRepository courseRepository;

        @Mock
        private UserRepository userRepository;

        @Mock
        private LeadMapper leadMapper;

        @InjectMocks
        private LeadServiceImpl leadService;

        private LeadCreateRequest fullRequest;
        private LeadCreateRequest minimalRequest;
        private Course course;
        private User manager;
        private Lead savedLead;
        private LeadResponse expectedResponse;

        @BeforeEach
        void setUp() {
            course = new Course();
            course.setId(1L);
            course.setTitle("Test Course");

            manager = new User();
            manager.setId(2L);
            manager.setFirstName("Anna");
            manager.setLastName("Smith");

            fullRequest = new LeadCreateRequest();
            fullRequest.setFirstName("Ivan");
            fullRequest.setLastName("Petrov");
            fullRequest.setPhone("+79001234567");
            fullRequest.setEmail("ivan@example.com");
            fullRequest.setSource("website");
            fullRequest.setInterestedCourseId(1L);
            fullRequest.setAssignedManagerId(2L);
            fullRequest.setComment("Test comment");

            minimalRequest = new LeadCreateRequest();
            minimalRequest.setFirstName("John");
            minimalRequest.setPhone("+79998887766");

            savedLead = new Lead();
            savedLead.setId(1L);
            savedLead.setFirstName(fullRequest.getFirstName());
            savedLead.setLastName(fullRequest.getLastName());
            savedLead.setPhone(fullRequest.getPhone());
            savedLead.setEmail(fullRequest.getEmail());
            savedLead.setSource(fullRequest.getSource());
            savedLead.setComment(fullRequest.getComment());
            savedLead.setInterestedCourse(course);
            savedLead.setAssignedManager(manager);
            savedLead.setStatus(LeadStatus.NEW);

            expectedResponse = new LeadResponse();
            expectedResponse.setId(1L);
            expectedResponse.setFirstName(fullRequest.getFirstName());
            expectedResponse.setLastName(fullRequest.getLastName());
            expectedResponse.setPhone(fullRequest.getPhone());
            expectedResponse.setEmail(fullRequest.getEmail());
            expectedResponse.setSource(fullRequest.getSource());
            expectedResponse.setComment(fullRequest.getComment());
            expectedResponse.setStatus(LeadStatus.NEW.name());
            expectedResponse.setCreatedAt(LocalDateTime.now());
            expectedResponse.setUpdatedAt(LocalDateTime.now());

            CourseShortResponse courseShort = new CourseShortResponse();
            courseShort.setId(1L);
            courseShort.setTitle("Test Course");
            expectedResponse.setInterestedCourse(courseShort);

            UserShortResponse userShort = new UserShortResponse();
            userShort.setId(2L);
            userShort.setFirstName("Anna");
            userShort.setLastName("Smith");
            expectedResponse.setAssignedManager(userShort);
        }


@Test
@DisplayName("Создание лида с заполненными полями (курс и менеджер)")
void createLead_withAllFields_shouldReturnLeadResponse() {

    when(courseRepository.findById(1L)).thenReturn(Optional.of(course));
    when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
    when(leadRepository.save(any(Lead.class))).thenReturn(savedLead);

    when(leadMapper.toResponse(any(Lead.class))).thenReturn(expectedResponse);

    LeadResponse response = leadService.create(fullRequest);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(1L);
    assertThat(response.getStatus()).isEqualTo("NEW");
    assertThat(response.getInterestedCourse()).isNotNull();
    assertThat(response.getInterestedCourse().getId()).isEqualTo(1L);

    verify(courseRepository).findById(1L);
    verify(userRepository).findById(2L);
    verify(leadRepository).save(any(Lead.class));
    verify(leadMapper).toResponse(any(Lead.class));

    ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
    verify(leadRepository).save(leadCaptor.capture());
    Lead capturedLead = leadCaptor.getValue();
    assertThat(capturedLead.getStatus()).isEqualTo(LeadStatus.NEW);
    assertThat(capturedLead.getInterestedCourse()).isEqualTo(course);
    assertThat(capturedLead.getAssignedManager()).isEqualTo(manager);
}
@Test
@DisplayName("Создание лида только с обязательными полями (минимум)")
void createLead_withMinimalFields_shouldReturnLeadResponse() {

    Lead minimalSavedLead = new Lead();
    minimalSavedLead.setId(2L);
    minimalSavedLead.setFirstName(minimalRequest.getFirstName());
    minimalSavedLead.setPhone(minimalRequest.getPhone());
    minimalSavedLead.setStatus(LeadStatus.NEW);

    LeadResponse minimalResponse = new LeadResponse();
    minimalResponse.setId(2L);
    minimalResponse.setFirstName(minimalRequest.getFirstName());
    minimalResponse.setPhone(minimalRequest.getPhone());
    minimalResponse.setStatus("NEW");
    minimalResponse.setInterestedCourse(null);
    minimalResponse.setAssignedManager(null);

    when(leadRepository.save(any(Lead.class))).thenReturn(minimalSavedLead);
    when(leadMapper.toResponse(any(Lead.class))).thenReturn(minimalResponse);

    LeadResponse response = leadService.create(minimalRequest);

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(2L);
    assertThat(response.getStatus()).isEqualTo("NEW");
    assertThat(response.getInterestedCourse()).isNull();
    assertThat(response.getAssignedManager()).isNull();

    verify(courseRepository, never()).findById(any());
    verify(userRepository, never()).findById(any());
    verify(leadRepository).save(any(Lead.class));

    ArgumentCaptor<Lead> leadCaptor = ArgumentCaptor.forClass(Lead.class);
    verify(leadRepository).save(leadCaptor.capture());
    Lead captured = leadCaptor.getValue();
    assertThat(captured.getStatus()).isEqualTo(LeadStatus.NEW);
    assertThat(captured.getInterestedCourse()).isNull();
    assertThat(captured.getAssignedManager()).isNull();
}
@Test
@DisplayName("Передан несуществующий курс – выбрасывается ResourceNotFoundException")
void createLead_withInvalidCourseId_shouldThrowResourceNotFoundException() {

    Long invalidCourseId = 99L;
    fullRequest.setInterestedCourseId(invalidCourseId);
    fullRequest.setAssignedManagerId(2L);

    when(courseRepository.findById(invalidCourseId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> leadService.create(fullRequest))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Course")
            .hasMessageContaining(String.valueOf(invalidCourseId));

    verify(courseRepository).findById(invalidCourseId);
    verify(userRepository, never()).findById(anyLong());
    verify(leadRepository, never()).save(any(Lead.class));
}
}
