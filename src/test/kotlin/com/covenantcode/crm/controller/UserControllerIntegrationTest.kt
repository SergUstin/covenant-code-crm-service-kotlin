package com.covenantcode.crm.controller

import com.covenantcode.crm.BaseIntegrationTest
import com.covenantcode.crm.repository.StudyGroupRepository
import com.covenantcode.crm.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@AutoConfigureMockMvc
class UserControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var studyGroupRepository: StudyGroupRepository
    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun cleanup() {
        studyGroupRepository.deleteAll()
        userRepository.deleteAll(
            userRepository.findAll().filter { it.email != "admin@covenantcode.ru" }
        )
    }

    @Test
    fun `GET users - ADMIN получает список пользователей`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/users?page=0&size=5")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
    }

    @Test
    fun `GET users - MANAGER получает 403 Forbidden`() {
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"M","lastName":"M","email":"mgr@test.com","password":"password123"}""")
        )
        val managerToken = loginAndGetToken("mgr@test.com", "password123")

        mockMvc.perform(
            get("/api/v1/users")
                .header("Authorization", "Bearer $managerToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET users - без токена возвращает 401`() {
        mockMvc.perform(get("/api/v1/users"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET users id - ADMIN получает профиль другого пользователя - 200`() {
        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"T","lastName":"T","email":"target@test.com","password":"password123"}""")
        ).andReturn()
        val targetId = objectMapper.readTree(registerResult.response.contentAsString).get("userId").asLong()
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/users/$targetId")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(targetId))
            .andExpect(jsonPath("$.email").value("target@test.com"))
    }

    @Test
    fun `GET users id - пользователь получает свой профиль - 200`() {
        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"S","lastName":"S","email":"self@test.com","password":"password123"}""")
        ).andReturn()
        val userId = objectMapper.readTree(registerResult.response.contentAsString).get("userId").asLong()
        val token = loginAndGetToken("self@test.com", "password123")

        mockMvc.perform(
            get("/api/v1/users/$userId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(userId))
    }

    @Test
    fun `GET users id - MANAGER запрашивает чужой профиль - 403`() {
        val r1 = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"A","lastName":"A","email":"user1@test.com","password":"password123"}""")
        ).andReturn()
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"B","lastName":"B","email":"user2@test.com","password":"password123"}""")
        )
        val otherId = objectMapper.readTree(r1.response.contentAsString).get("userId").asLong()
        val managerToken = loginAndGetToken("user2@test.com", "password123")

        mockMvc.perform(
            get("/api/v1/users/$otherId")
                .header("Authorization", "Bearer $managerToken")
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.type").value("forbidden"))
    }

    @Test
    fun `GET users id - несуществующий ID для ADMIN - 404`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/users/999999")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `PATCH users enabled - ADMIN блокирует пользователя - 200 enabled false`() {
        val result = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"B","lastName":"B","email":"block@test.com","password":"password123"}""")
        ).andReturn()
        val targetId = objectMapper.readTree(result.response.contentAsString).get("userId").asLong()
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            patch("/api/v1/users/$targetId/enabled")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"enabled":false}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(false))
    }

    @Test
    fun `PATCH users enabled - ADMIN разблокирует пользователя - 200 enabled true`() {
        val result = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"U","lastName":"U","email":"unblock@test.com","password":"password123"}""")
        ).andReturn()
        val targetId = objectMapper.readTree(result.response.contentAsString).get("userId").asLong()
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            patch("/api/v1/users/$targetId/enabled")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"enabled":false}""")
        )
        mockMvc.perform(
            patch("/api/v1/users/$targetId/enabled")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"enabled":true}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.enabled").value(true))
    }

    @Test
    fun `PATCH users enabled - ADMIN пытается заблокировать себя - 400`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")
        val adminId = objectMapper.readTree(
            mockMvc.perform(
                get("/api/v1/users?page=0&size=1")
                    .header("Authorization", "Bearer $adminToken")
            ).andReturn().response.contentAsString
        ).get("content")[0].get("id").asLong()

        mockMvc.perform(
            patch("/api/v1/users/$adminId/enabled")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"enabled":false}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("bad-request"))
            .andExpect(jsonPath("$.detail").value("Нельзя заблокировать собственный аккаунт"))
    }

    @Test
    fun `PATCH users enabled - MANAGER не может блокировать - 403`() {
        mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"M","lastName":"M","email":"mgr10@test.com","password":"password123"}""")
        )
        val managerToken = loginAndGetToken("mgr10@test.com", "password123")

        mockMvc.perform(
            patch("/api/v1/users/1/enabled")
                .header("Authorization", "Bearer $managerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"enabled":false}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PATCH users enabled - несуществующий ID - 404`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            patch("/api/v1/users/999999/enabled")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"enabled":false}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    private fun loginAndGetToken(email: String, password: String): String {
        val result = mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email":"$email","password":"$password"}""")
        )
            .andExpect(status().isOk)
            .andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("token").asText()
    }
}
