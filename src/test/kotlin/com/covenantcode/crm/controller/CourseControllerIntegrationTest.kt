package com.covenantcode.crm.controller

import com.covenantcode.crm.BaseIntegrationTest
import com.covenantcode.crm.entity.Course
import com.covenantcode.crm.entity.StudyGroup
import com.covenantcode.crm.entity.enums.CourseStatus
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.repository.CourseRepository
import com.covenantcode.crm.repository.RoleRepository
import com.covenantcode.crm.repository.StudyGroupRepository
import com.covenantcode.crm.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.containsInAnyOrder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import org.springframework.data.repository.findByIdOrNull

@AutoConfigureMockMvc
class CourseControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository
    @Autowired private lateinit var courseRepository: CourseRepository
    @Autowired private lateinit var studyGroupRepository: StudyGroupRepository
    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun cleanup() {
        studyGroupRepository.deleteAll()
        courseRepository.deleteAll()
        userRepository.deleteAll(
            userRepository.findAll().filter { it.email != "admin@covenantcode.ru" }
        )
    }

    private fun saveCourse(title: String, status: CourseStatus) = courseRepository.save(
        Course().apply {
            this.title = title
            durationInWeeks = 8
            price = BigDecimal("1000")
            this.status = status
        }
    )

    @Test
    fun `POST courses - успешное создание - 201`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/courses")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Java для начинающих","description":"Описание","durationInWeeks":16,"price":45000.00,"status":"ACTIVE"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.title").value("Java для начинающих"))
            .andExpect(jsonPath("$.durationInWeeks").value(16))
            .andExpect(jsonPath("$.price").value(45000.00))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `POST courses - статус не передан - в ответе ACTIVE`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/courses")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Вводный курс","durationInWeeks":8,"price":0}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `POST courses - отрицательный durationInWeeks - 400 validation-error`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/courses")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Курс","durationInWeeks":-5,"price":1000}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("validation-error"))
    }

    @Test
    fun `POST courses - отрицательная price - 400 validation-error`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/courses")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Курс","durationInWeeks":4,"price":-100}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("validation-error"))
    }

    @Test
    fun `POST courses - токен TEACHER - 403`() {
        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"T","lastName":"T","email":"teacher@test.com","password":"password123"}""")
        ).andReturn()
        val teacherId = objectMapper.readTree(registerResult.response.contentAsString).get("userId").asLong()
        val teacherRole = roleRepository.findByName(RoleName.TEACHER)!!
        val teacher = userRepository.findByIdOrNull(teacherId)!!
        teacher.role = teacherRole
        userRepository.save(teacher)
        val teacherToken = loginAndGetToken("teacher@test.com", "password123")

        mockMvc.perform(
            post("/api/v1/courses")
                .header("Authorization", "Bearer $teacherToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Курс","durationInWeeks":4,"price":1000}""")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET courses - без фильтра возвращает все курсы`() {
        saveCourse("Курс 1", CourseStatus.ACTIVE)
        saveCourse("Курс 2", CourseStatus.ARCHIVED)
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/courses")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content").isArray)
    }

    @Test
    fun `GET courses - фильтр ACTIVE возвращает только активные`() {
        saveCourse("Активный", CourseStatus.ACTIVE)
        saveCourse("Архивный", CourseStatus.ARCHIVED)
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/courses?status=ACTIVE")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
    }

    @Test
    fun `GET courses - фильтр ARCHIVED возвращает только архивные`() {
        saveCourse("Активный", CourseStatus.ACTIVE)
        saveCourse("Архивный", CourseStatus.ARCHIVED)
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/courses?status=ARCHIVED")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].status").value("ARCHIVED"))
    }

    @Test
    fun `GET courses - без токена возвращает 401`() {
        mockMvc.perform(get("/api/v1/courses"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `GET courses id - курс найден - 200 с полным ответом`() {
        val course = saveCourse("Java для начинающих", CourseStatus.ACTIVE)
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/courses/${course.id}")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(course.id))
            .andExpect(jsonPath("$.title").value("Java для начинающих"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `GET courses id - курс не найден - 404 resource-not-found`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/courses/99999")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
            .andExpect(jsonPath("$.status").value(404))
    }

    @Test
    fun `GET courses id - без токена - 401`() {
        mockMvc.perform(get("/api/v1/courses/1"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `DELETE courses id - успешное удаление - 204 курс не существует`() {
        val course = saveCourse("Курс для удаления", CourseStatus.ACTIVE)
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            delete("/api/v1/courses/${course.id}")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNoContent)

        assertThat(courseRepository.findById(course.id!!)).isEmpty
    }

    @Test
    fun `DELETE courses id - курс с активной группой - 409 conflict курс не удалён`() {
        val course = saveCourse("Курс с группой", CourseStatus.ACTIVE)
        val admin = userRepository.findByEmail("admin@covenantcode.ru")!!
        studyGroupRepository.save(StudyGroup().apply {
            name = "Группа A"
            this.course = course
            teacher = admin
            status = GroupStatus.ACTIVE
        })
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            delete("/api/v1/courses/${course.id}")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.type").value("conflict"))

        assertThat(courseRepository.findById(course.id!!)).isPresent
    }

    @Test
    fun `DELETE courses id - курс не найден - 404 resource-not-found`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            delete("/api/v1/courses/99999")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `DELETE courses id - токен MANAGER - 403`() {
        val course = saveCourse("Курс", CourseStatus.ACTIVE)
        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"M","lastName":"M","email":"manager@test.com","password":"password123"}""")
        ).andReturn()
        val managerId = objectMapper.readTree(registerResult.response.contentAsString).get("userId").asLong()
        val managerRole = roleRepository.findByName(RoleName.MANAGER)!!
        val manager = userRepository.findByIdOrNull(managerId)!!
        manager.role = managerRole
        userRepository.save(manager)
        val managerToken = loginAndGetToken("manager@test.com", "password123")

        mockMvc.perform(
            delete("/api/v1/courses/${course.id}")
                .header("Authorization", "Bearer $managerToken")
        )
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PUT courses id - успешное обновление - 200 данные изменились`() {
        val course = saveCourse("Исходный курс", CourseStatus.ACTIVE)
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/courses/${course.id}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Java для начинающих — обновлённая версия","description":"Описание обновлено","durationInWeeks":20,"price":49900.00,"status":"ACTIVE"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Java для начинающих — обновлённая версия"))
            .andExpect(jsonPath("$.durationInWeeks").value(20))
            .andExpect(jsonPath("$.price").value(49900.0))
            .andExpect(jsonPath("$.status").value("ACTIVE"))

        val updated = courseRepository.findByIdOrNull(course.id!!)!!
        assertThat(updated.title).isEqualTo("Java для начинающих — обновлённая версия")
        assertThat(updated.durationInWeeks).isEqualTo(20)
    }

    @Test
    fun `PUT courses id - курс не найден - 404 resource-not-found`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/courses/99999")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Несуществующий курс","durationInWeeks":10,"price":0.00,"status":"ACTIVE"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `PUT courses id - ошибка валидации - 400 validation-error с полями title и durationInWeeks`() {
        val course = saveCourse("Курс", CourseStatus.ACTIVE)
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/courses/${course.id}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"","durationInWeeks":-1,"price":1000.00,"status":"ACTIVE"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("validation-error"))
            .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("title", "durationInWeeks")))
    }

    @Test
    fun `PUT courses id - токен TEACHER - 403`() {
        val course = saveCourse("Курс", CourseStatus.ACTIVE)
        val registerResult = mockMvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"T","lastName":"T","email":"teacher2@test.com","password":"password123"}""")
        ).andReturn()
        val teacherId = objectMapper.readTree(registerResult.response.contentAsString).get("userId").asLong()
        val teacherRole = roleRepository.findByName(RoleName.TEACHER)!!
        val teacher = userRepository.findByIdOrNull(teacherId)!!
        teacher.role = teacherRole
        userRepository.save(teacher)
        val teacherToken = loginAndGetToken("teacher2@test.com", "password123")

        mockMvc.perform(
            put("/api/v1/courses/${course.id}")
                .header("Authorization", "Bearer $teacherToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"title":"Курс","durationInWeeks":4,"price":1000.00,"status":"ACTIVE"}""")
        )
            .andExpect(status().isForbidden)
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
