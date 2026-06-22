package com.covenantcode.crm.controller

import com.covenantcode.crm.BaseIntegrationTest
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.repository.RoleRepository
import com.covenantcode.crm.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@AutoConfigureMockMvc
class AuthControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository

    @BeforeEach
    fun cleanup() {
        userRepository.deleteAll(
            userRepository.findAll().filter { it.email != "admin@covenantcode.ru" }
        )
    }

    // ─── register ─────────────────────────────────────────────────────────────

    @Test
    fun `POST register - успешная регистрация возвращает 201 с токеном и ролью MANAGER`() {
        val body = registerBody("ivan@test.com", "password123")

        mockMvc.perform(post("/api/v1/auth/register").json(body))
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.role").value("MANAGER"))
            .andExpect(jsonPath("$.email").value("ivan@test.com"))

        val saved = userRepository.findByEmail("ivan@test.com")!!
        assertThat(saved.passwordHash).isNotEqualTo("password123")
    }

    @Test
    fun `POST register - повторный email возвращает 409 Conflict`() {
        val managerRole = roleRepository.findByName(RoleName.MANAGER)!!
        userRepository.save(User().apply {
            firstName = "Существующий"; lastName = "Пользователь"
            email = "duplicate@test.com"; passwordHash = "hashed"
            role = managerRole; enabled = true
        })

        mockMvc.perform(post("/api/v1/auth/register").json(registerBody("duplicate@test.com")))
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.type").value("conflict"))
    }

    @Test
    fun `POST register - невалидные данные возвращают 400 с перечислением ошибок`() {
        val body = """{"firstName":"","lastName":"Тест","email":"not-an-email","password":"short"}"""

        mockMvc.perform(post("/api/v1/auth/register").json(body))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("validation-error"))
            .andExpect(jsonPath("$.errors").isArray)
            .andExpect(jsonPath("$.errors[*].field").isNotEmpty)
    }

    // ─── login ────────────────────────────────────────────────────────────────

    @Test
    fun `POST login - успешный вход возвращает 200 с токеном`() {
        mockMvc.perform(post("/api/v1/auth/register").json(registerBody("login_ok@test.com", "password123")))
            .andExpect(status().isCreated)

        mockMvc.perform(post("/api/v1/auth/login").json(loginBody("login_ok@test.com", "password123")))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.token").isNotEmpty)
            .andExpect(jsonPath("$.role").isNotEmpty)
            .andExpect(jsonPath("$.email").value("login_ok@test.com"))
    }

    @Test
    fun `POST login - неверный пароль возвращает 401 с общим сообщением`() {
        mockMvc.perform(post("/api/v1/auth/register").json(registerBody("wrong_pass@test.com", "correct123")))
            .andExpect(status().isCreated)

        mockMvc.perform(post("/api/v1/auth/login").json(loginBody("wrong_pass@test.com", "wrongpassword")))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.type").value("unauthorized"))
            .andExpect(jsonPath("$.detail").value("Неверный email или пароль"))
    }

    @Test
    fun `POST login - несуществующий email возвращает 401 с тем же сообщением`() {
        mockMvc.perform(post("/api/v1/auth/login").json(loginBody("nobody@test.com", "anypassword")))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.type").value("unauthorized"))
            .andExpect(jsonPath("$.detail").value("Неверный email или пароль"))
    }

    @Test
    fun `POST login - пустой email возвращает 400 validation-error`() {
        mockMvc.perform(post("/api/v1/auth/login").json("""{"email":"","password":"password123"}"""))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("validation-error"))
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private fun registerBody(email: String, password: String = "password123") = """
        {"firstName":"Иван","lastName":"Петров","email":"$email","password":"$password"}
    """.trimIndent()

    private fun loginBody(email: String, password: String) = """
        {"email":"$email","password":"$password"}
    """.trimIndent()

    private fun org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder.json(body: String) =
        contentType(MediaType.APPLICATION_JSON).content(body)
}
