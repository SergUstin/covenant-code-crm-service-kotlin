package com.covenantcode.crm.controller

import com.covenantcode.crm.BaseIntegrationTest
import com.covenantcode.crm.entity.Lead
import com.covenantcode.crm.entity.enums.LeadStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.repository.LeadRepository
import com.covenantcode.crm.repository.RoleRepository
import com.covenantcode.crm.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.data.repository.findByIdOrNull

@AutoConfigureMockMvc
class LeadStatusControllerIT : BaseIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var leadRepository: LeadRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository
    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun cleanup() {
        leadRepository.deleteAll()
        userRepository.deleteAll(
            userRepository.findAll().filter { it.email != "admin@covenantcode.ru" }
        )
    }

    private fun saveLead(status: LeadStatus = LeadStatus.NEW) = leadRepository.save(Lead().apply {
        firstName = "Тест"
        phone = "+70000000000"
        this.status = status
    })

    private fun loginAndGetToken(email: String, password: String): String {
        val result = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$password"}""")
        ).andExpect(status().isOk).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("token").asText()
    }

    private fun registerWithRole(email: String, role: RoleName): String {
        val result = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"U","lastName":"U","email":"$email","password":"password123"}""")
        ).andReturn()
        val userId = objectMapper.readTree(result.response.contentAsString).get("userId").asLong()
        val user = userRepository.findByIdOrNull(userId)!!
        user.role = roleRepository.findByName(role)!!
        userRepository.save(user)
        return loginAndGetToken(email, "password123")
    }

    @Test
    fun `PATCH status - MANAGER - 200 статус обновлён`() {
        val lead = saveLead()
        val token = registerWithRole("manager@status.test", RoleName.MANAGER)

        mockMvc.perform(
            patch("/api/v1/leads/${lead.id}/status")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"IN_PROGRESS"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
    }

    @Test
    fun `PATCH status - ADMIN - 200`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            patch("/api/v1/leads/${lead.id}/status")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CONTACTED"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CONTACTED"))
    }

    @Test
    fun `PATCH status - STUDENT - 403`() {
        val lead = saveLead()
        val token = registerWithRole("student@status.test", RoleName.STUDENT)

        mockMvc.perform(
            patch("/api/v1/leads/${lead.id}/status")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"IN_PROGRESS"}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PATCH status - без токена - 401`() {
        val lead = saveLead()

        mockMvc.perform(
            patch("/api/v1/leads/${lead.id}/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"IN_PROGRESS"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `PATCH status - CONVERTED_TO_STUDENT - 409 conflict`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            patch("/api/v1/leads/${lead.id}/status")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CONVERTED_TO_STUDENT"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.type").value("conflict"))
    }

    @Test
    fun `PATCH status - лид не найден - 404 resource-not-found`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            patch("/api/v1/leads/9999/status")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"NEW"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `PATCH status - пустое тело - 400 validation-error с полем status`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            patch("/api/v1/leads/${lead.id}/status")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("validation-error"))
            .andExpect(jsonPath("$.errors[*].field", hasItem("status")))
    }

    @Test
    fun `PATCH status - неизвестный статус - 400`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            patch("/api/v1/leads/${lead.id}/status")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"UNKNOWN_STATUS"}""")
        )
            .andExpect(status().isBadRequest)
    }
}
