package com.covenantcode.crm.service;


import com.covenantcode.crm.dto.lead.LeadCreateRequest;
import com.covenantcode.crm.dto.lead.LeadResponse;

public interface LeadService {

    LeadResponse create(LeadCreateRequest leadCreateRequest);

}
