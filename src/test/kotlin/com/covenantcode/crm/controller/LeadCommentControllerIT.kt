package com.covenantcode.crm.controller

import com.covenantcode.crm.BaseIntegrationTest
import com.covenantcode.crm.entity.Lead
import com.covenantcode.crm.entity.enums.LeadStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.repository.LeadCommentRepository
import com.covenantcode.crm.repository.LeadRepository
import com.covenantcode.crm.repository.RoleRepository
import com.covenantcode.crm.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasItem
import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.data.repository.findByIdOrNull

@AutoConfigureMockMvc
class LeadCommentControllerIT : BaseIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var leadRepository: LeadRepository
    @Autowired private lateinit var leadCommentRepository: LeadCommentRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository
    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun cleanup() {
        leadCommentRepository.deleteAll()
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
    fun `POST comments - MANAGER - 201 поля заполнены`() {
        val lead = saveLead()
        val token = registerWithRole("manager@comment.test", RoleName.MANAGER)

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"text":"Обсудили курс, клиент заинтересован"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.leadId").value(lead.id))
            .andExpect(jsonPath("$.text").value("Обсудили курс, клиент заинтересован"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty)
    }

    @Test
    fun `POST comments - ADMIN - 201`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"text":"Комментарий от администратора"}""")
        )
            .andExpect(status().isCreated)
    }

    @Test
    fun `POST comments - author - ответ содержит данные текущего пользователя`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")
        val adminUser = userRepository.findAll().first { it.email == "admin@covenantcode.ru" }

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"text":"Текст комментария"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.author.id").value(adminUser.id))
            .andExpect(jsonPath("$.author.firstName").value(adminUser.firstName))
    }

    @Test
    fun `POST comments - STUDENT - 403`() {
        val lead = saveLead()
        val token = registerWithRole("student@comment.test", RoleName.STUDENT)

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"text":"Комментарий"}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST comments - без токена - 401`() {
        val lead = saveLead()

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"text":"Комментарий"}""")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `POST comments - пустой text - 400 validation-error`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"text":""}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("validation-error"))
            .andExpect(jsonPath("$.errors[*].field", hasItem("text")))
    }

    @Test
    fun `POST comments - text из пробелов - 400`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"text":"   "}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("validation-error"))
    }

    @Test
    fun `POST 9999 comments - лид не найден - 404`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads/9999/comments")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"text":"Текст"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `POST comments - createdAt заполняется автоматически`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"text":"Текст комментария"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.createdAt", notNullValue()))
    }

    private fun addComment(leadId: Long, token: String, text: String) {
        mockMvc.perform(
            post("/api/v1/leads/$leadId/comments")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"text":"$text"}""")
        ).andExpect(status().isCreated)
    }

    @Test
    fun `GET comments - лид с комментариями - 200 список не пуст`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")
        addComment(lead.id!!, token, "Первый комментарий")
        addComment(lead.id!!, token, "Второй комментарий")

        mockMvc.perform(
            get("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].text").value("Первый комментарий"))
            .andExpect(jsonPath("$[1].text").value("Второй комментарий"))
    }

    @Test
    fun `GET comments - лид без комментариев - 200 пустой массив`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `GET comments - ADMIN - 200`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)
    }

    @Test
    fun `GET comments - MANAGER - 200`() {
        val lead = saveLead()
        val token = registerWithRole("manager@getcomments.test", RoleName.MANAGER)

        mockMvc.perform(
            get("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isOk)
    }

    @Test
    fun `GET comments - STUDENT - 403`() {
        val lead = saveLead()
        val token = registerWithRole("student@getcomments.test", RoleName.STUDENT)

        mockMvc.perform(
            get("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET comments - без токена - 401`() {
        val lead = saveLead()

        mockMvc.perform(get("/api/v1/leads/${lead.id}/comments"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET 9999 comments - лид не найден - 404`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/leads/9999/comments")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `GET comments - проверка полей ответа leadId author text createdAt`() {
        val lead = saveLead()
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")
        val adminUser = userRepository.findAll().first { it.email == "admin@covenantcode.ru" }
        addComment(lead.id!!, token, "Проверочный текст")

        mockMvc.perform(
            get("/api/v1/leads/${lead.id}/comments")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].leadId").value(lead.id))
            .andExpect(jsonPath("$[0].author.id").value(adminUser.id))
            .andExpect(jsonPath("$[0].author.firstName").value(adminUser.firstName))
            .andExpect(jsonPath("$[0].text").value("Проверочный текст"))
            .andExpect(jsonPath("$[0].createdAt", notNullValue()))
    }
}
