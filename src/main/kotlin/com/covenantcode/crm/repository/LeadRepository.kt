package com.covenantcode.crm.repository

import com.covenantcode.crm.entity.Lead
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface LeadRepository : JpaRepository<Lead, Long>, JpaSpecificationExecutor<Lead>
