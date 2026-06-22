package com.covenantcode.crm.controller

import com.covenantcode.crm.BaseIntegrationTest
import com.covenantcode.crm.entity.Lead
import com.covenantcode.crm.entity.enums.LeadStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.repository.LeadRepository
import com.covenantcode.crm.repository.RoleRepository
import com.covenantcode.crm.repository.StudentRepository
import com.covenantcode.crm.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.data.repository.findByIdOrNull

@AutoConfigureMockMvc
class LeadConvertControllerIT : BaseIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var leadRepository: LeadRepository
    @Autowired private lateinit var studentRepository: StudentRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository
    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun cleanup() {
        leadRepository.deleteAll()
        studentRepository.deleteAll()
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

    private val validBody = """
        {
          "firstName": "Алексей",
          "lastName": "Петров",
          "phone": "+79001234567",
          "email": "alexey@example.com",
          "birthDate": "1995-03-15"
        }
    """.trimIndent()

    @Test
    fun `POST convert - MANAGER - 201 поля студента корректны`() {
        val lead = saveLead()
        val token = registerWithRole("manager@convert.test", RoleName.MANAGER)

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/convert")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.firstName").value("Алексей"))
            .andExpect(jsonPath("$.lastName").value("Петров"))
            .andExpect(jsonPath("$.phone").value("+79001234567"))
            .andExpect(jsonPath("$.email").value("alexey@example.com"))
            .andExpect(jsonPath("$.birthDate").value("1995-03-15"))
            .andExpect(jsonPath("$.userId").doesNotExist())
    }

    @Test
    fun `POST convert - ADMIN - 201`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/convert")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody)
        ).andExpect(status().isCreated)
    }

    @Test
    fun `POST convert - только обязательные поля - 201 email и birthDate null`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/convert")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Мария","lastName":"Сидорова","phone":"+79007654321"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.email").doesNotExist())
            .andExpect(jsonPath("$.birthDate").doesNotExist())
    }

    @Test
    fun `POST convert повторно - уже конвертирован - 409 conflict`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")
        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/convert")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody)
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/convert")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody)
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.type").value("conflict"))
    }

    @Test
    fun `POST 9999 convert - лид не найден - 404`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads/9999/convert")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `POST convert - пустой firstName - 400 validation-error`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/convert")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"lastName":"Петров","phone":"+79001234567"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("validation-error"))
            .andExpect(jsonPath("$.errors[*].field", hasItem("firstName")))
    }

    @Test
    fun `POST convert - STUDENT - 403`() {
        val lead = saveLead()
        val token = registerWithRole("student@convert.test", RoleName.STUDENT)

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/convert")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody)
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST convert - без токена - 401`() {
        val lead = saveLead()

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/convert")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody)
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST convert - транзакционность - статус лида CONVERTED_TO_STUDENT в БД`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/convert")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody)
        ).andExpect(status().isCreated)

        val updatedLead = leadRepository.findByIdOrNull(lead.id!!)!!
        assert(updatedLead.status == LeadStatus.CONVERTED_TO_STUDENT)
    }

    @Test
    fun `POST convert - транзакционность - запись студента существует в БД`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/convert")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validBody)
        ).andExpect(status().isCreated)

        val students = studentRepository.findAll()
        assert(students.size == 1)
        assert(students[0].firstName == "Алексей")
    }
}
