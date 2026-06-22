package com.covenantcode.crm.controller

import com.covenantcode.crm.BaseIntegrationTest
import com.covenantcode.crm.entity.Course
import com.covenantcode.crm.entity.Lead
import com.covenantcode.crm.entity.enums.CourseStatus
import com.covenantcode.crm.entity.enums.LeadStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.repository.CourseRepository
import com.covenantcode.crm.repository.LeadRepository
import com.covenantcode.crm.repository.RoleRepository
import com.covenantcode.crm.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import org.springframework.data.repository.findByIdOrNull

@AutoConfigureMockMvc
class LeadControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var leadRepository: LeadRepository
    @Autowired private lateinit var courseRepository: CourseRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository
    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun cleanup() {
        leadRepository.deleteAll()
        courseRepository.deleteAll()
        userRepository.deleteAll(
            userRepository.findAll().filter { it.email != "admin@covenantcode.ru" }
        )
    }

    private fun saveLead(firstName: String, phone: String, status: LeadStatus = LeadStatus.NEW) =
        leadRepository.save(Lead().apply {
            this.firstName = firstName
            this.phone = phone
            this.status = status
        })

    private fun saveCourse() = courseRepository.save(Course().apply {
        title = "Java для начинающих"
        durationInWeeks = 16
        price = BigDecimal("45000.00")
        status = CourseStatus.ACTIVE
    })

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

    @Test
    fun `PUT leads id - успешное обновление - 200 новый телефон статус не изменился`() {
        val lead = saveLead("Алексей", "+79161234567", LeadStatus.NEW)
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/leads/${lead.id}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Алексей","phone":"+79999999999"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.phone").value("+79999999999"))
            .andExpect(jsonPath("$.status").value("NEW"))
    }

    @Test
    fun `PUT leads id - конвертированный лид - 400 bad-request`() {
        val lead = saveLead("Алексей", "+79161234567", LeadStatus.CONVERTED_TO_STUDENT)
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/leads/${lead.id}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Новое","phone":"+70000000000"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("bad-request"))
    }

    @Test
    fun `PUT leads id - лид не найден - 404`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/leads/9999")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Тест","phone":"+70000000000"}""")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PUT leads id - токен TEACHER - 403`() {
        val lead = saveLead("Тест", "+70000000000")
        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"T","lastName":"T","email":"teacher@update.test","password":"password123"}""")
        ).andReturn()
        val teacherId = objectMapper.readTree(registerResult.response.contentAsString).get("userId").asLong()
        val teacher = userRepository.findByIdOrNull(teacherId)!!
        teacher.role = roleRepository.findByName(RoleName.TEACHER)!!
        userRepository.save(teacher)
        val teacherToken = loginAndGetToken("teacher@update.test", "password123")

        mockMvc.perform(
            put("/api/v1/leads/${lead.id}")
                .header("Authorization", "Bearer $teacherToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Тест","phone":"+70000000000"}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET leads id - лид найден - 200 с полным ответом`() {
        val lead = saveLead("Алексей", "+79161234567", LeadStatus.IN_PROGRESS)
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/leads/${lead.id}").header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(lead.id))
            .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
    }

    @Test
    fun `GET leads id - лид не найден - 404 resource-not-found`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/leads/9999").header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `GET leads id - токен TEACHER - 403`() {
        val lead = saveLead("Тест", "+70000000000")
        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"T","lastName":"T","email":"teacher@getbyid.test","password":"password123"}""")
        ).andReturn()
        val teacherId = objectMapper.readTree(registerResult.response.contentAsString).get("userId").asLong()
        val teacher = userRepository.findByIdOrNull(teacherId)!!
        teacher.role = roleRepository.findByName(RoleName.TEACHER)!!
        userRepository.save(teacher)
        val teacherToken = loginAndGetToken("teacher@getbyid.test", "password123")

        mockMvc.perform(
            get("/api/v1/leads/${lead.id}").header("Authorization", "Bearer $teacherToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET leads - без фильтров - 200 все лиды`() {
        saveLead("Алексей", "+79161234567", LeadStatus.NEW)
        saveLead("Мария", "+79261234567", LeadStatus.IN_PROGRESS)
        saveLead("Иван", "+79361234567", LeadStatus.CONTACTED)
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(get("/api/v1/leads").header("Authorization", "Bearer $token"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content").isArray)
    }

    @Test
    fun `GET leads - фильтр status NEW - 200 только NEW лиды`() {
        saveLead("Один", "+79161111111", LeadStatus.NEW)
        saveLead("Два", "+79162222222", LeadStatus.NEW)
        saveLead("Три", "+79163333333", LeadStatus.IN_PROGRESS)
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/leads?status=NEW").header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].status").value("NEW"))
            .andExpect(jsonPath("$.content[1].status").value("NEW"))
    }

    @Test
    fun `GET leads - поиск по части телефона - 200 нужный лид`() {
        saveLead("Алексей", "+79161234567")
        saveLead("Другой", "+70000000000")
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/leads?search=9161").header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].phone").value("+79161234567"))
    }

    @Test
    fun `GET leads - токен TEACHER - 403`() {
        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"T","lastName":"T","email":"teacher@leads.test","password":"password123"}""")
        ).andReturn()
        val teacherId = objectMapper.readTree(registerResult.response.contentAsString).get("userId").asLong()
        val teacher = userRepository.findByIdOrNull(teacherId)!!
        teacher.role = roleRepository.findByName(RoleName.TEACHER)!!
        userRepository.save(teacher)
        val teacherToken = loginAndGetToken("teacher@leads.test", "password123")

        mockMvc.perform(get("/api/v1/leads").header("Authorization", "Bearer $teacherToken"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `POST leads - полный набор полей - 201 статус NEW с вложенными объектами`() {
        val course = saveCourse()
        val admin = userRepository.findByEmail("admin@covenantcode.ru")!!
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """{"firstName":"Алексей","lastName":"Смирнов","phone":"+79161234567",
                    |"email":"a@test.com","source":"ВКонтакте",
                    |"interestedCourseId":${course.id},"assignedManagerId":${admin.id}}""".trimMargin()
                )
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.status").value("NEW"))
            .andExpect(jsonPath("$.interestedCourse.id").value(course.id))
            .andExpect(jsonPath("$.assignedManager.id").value(admin.id))
    }

    @Test
    fun `POST leads - только обязательные поля - 201 статус NEW вложенные объекты null`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Мария","phone":"+79260001122"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("NEW"))
            .andExpect(jsonPath("$.interestedCourse").doesNotExist())
            .andExpect(jsonPath("$.assignedManager").doesNotExist())
    }

    @Test
    fun `POST leads - несуществующий interestedCourseId - 404 resource-not-found`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Тест","phone":"+70000000000","interestedCourseId":9999}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `POST leads - пустое firstName - 400 validation-error с полем firstName`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/leads")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"","phone":"+79000000000"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("validation-error"))
            .andExpect(jsonPath("$.errors[*].field").value(org.hamcrest.Matchers.hasItem("firstName")))
    }
}
