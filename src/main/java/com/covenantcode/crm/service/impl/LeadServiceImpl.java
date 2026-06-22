package com.covenantcode.crm.service.impl;

import com.covenantcode.crm.dto.lead.LeadCreateRequest;
import com.covenantcode.crm.dto.lead.LeadResponse;
import com.covenantcode.crm.entity.Lead;
import com.covenantcode.crm.entity.enums.LeadStatus;
import com.covenantcode.crm.exception.ResourceNotFoundException;
import com.covenantcode.crm.mapper.LeadMapper;
import com.covenantcode.crm.repository.CourseRepository;
import com.covenantcode.crm.repository.LeadRepository;
import com.covenantcode.crm.repository.UserRepository;
import com.covenantcode.crm.service.LeadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LeadServiceImpl implements LeadService {

    private final LeadRepository leadRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LeadMapper leadMapper;

    @Override
    @Transactional
    public LeadResponse create(LeadCreateRequest leadCreateRequest) {
        var course = leadCreateRequest.getInterestedCourseId() != null
                ? courseRepository.findById(leadCreateRequest.getInterestedCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", leadCreateRequest.getInterestedCourseId()))
                : null;

        var manager = leadCreateRequest.getAssignedManagerId() != null
                ? userRepository.findById(leadCreateRequest.getAssignedManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", leadCreateRequest.getAssignedManagerId()))
                : null;

        Lead lead = new Lead();
        lead.setFirstName(leadCreateRequest.getFirstName());
        lead.setLastName(leadCreateRequest.getLastName());
        lead.setPhone(leadCreateRequest.getPhone());
        lead.setEmail(leadCreateRequest.getEmail());
        lead.setSource(leadCreateRequest.getSource());
        lead.setComment(leadCreateRequest.getComment());
        lead.setInterestedCourse(course);
        lead.setAssignedManager(manager);
        lead.setStatus(LeadStatus.NEW);

        Lead saved = leadRepository.save(lead);
        return leadMapper.toResponse(saved);
    }
}
