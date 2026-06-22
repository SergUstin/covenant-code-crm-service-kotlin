package com.covenantcode.crm.controller

import com.covenantcode.crm.BaseIntegrationTest
import com.covenantcode.crm.entity.Course
import com.covenantcode.crm.entity.Student
import com.covenantcode.crm.entity.StudyGroup
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.repository.CourseRepository
import com.covenantcode.crm.repository.RoleRepository
import com.covenantcode.crm.repository.StudentRepository
import com.covenantcode.crm.repository.StudyGroupRepository
import com.covenantcode.crm.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import com.covenantcode.crm.entity.enums.GroupStatus
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.data.repository.findByIdOrNull

@AutoConfigureMockMvc
class StudentControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var studentRepository: StudentRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository
    @Autowired private lateinit var studyGroupRepository: StudyGroupRepository
    @Autowired private lateinit var courseRepository: CourseRepository
    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun cleanup() {
        studyGroupRepository.deleteAll()
        studentRepository.deleteAll()
        courseRepository.deleteAll()
        userRepository.deleteAll(
            userRepository.findAll().filter { it.email != "admin@covenantcode.ru" }
        )
    }

    private fun saveCourse() = courseRepository.save(Course().apply {
        title = "Тестовый курс"
        durationInWeeks = 8
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
    fun `POST students - без userId - 201 запись в БД userId null`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/students")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Алиса","lastName":"Смирнова","phone":"+79161234567","email":"alice@example.com","birthDate":"2005-03-15"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.firstName").value("Алиса"))
            .andExpect(jsonPath("$.lastName").value("Смирнова"))
            .andExpect(jsonPath("$.userId").doesNotExist())

        assert(studentRepository.count() == 1L)
    }

    @Test
    fun `POST students - несуществующий userId - 404 resource-not-found`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/students")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Борис","lastName":"Иванов","userId":9999}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `POST students - userId уже привязан - 409 conflict`() {
        val adminUser = userRepository.findAll().first { it.email == "admin@covenantcode.ru" }
        studentRepository.save(Student().apply {
            firstName = "Существующий"
            lastName = "Студент"
            user = adminUser
        })
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/students")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Новый","lastName":"Студент","userId":${adminUser.id}}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.type").value("conflict"))
    }

    @Test
    fun `POST students - TEACHER - 403`() {
        val token = registerWithRole("teacher@student.test", RoleName.TEACHER)

        mockMvc.perform(
            post("/api/v1/students")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Алиса","lastName":"Смирнова"}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `POST students - пустой firstName - 400 validation-error`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/students")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"lastName":"Смирнова"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("validation-error"))
            .andExpect(jsonPath("$.errors[*].field", hasItem("firstName")))
    }

    @Test
    fun `POST students - без токена - 401`() {
        mockMvc.perform(
            post("/api/v1/students")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Алиса","lastName":"Смирнова"}""")
        ).andExpect(status().isUnauthorized)
    }

    private fun saveStudent(firstName: String, lastName: String, phone: String? = null) =
        studentRepository.save(Student().apply {
            this.firstName = firstName
            this.lastName = lastName
            this.phone = phone
        })

    @Test
    fun `GET students - без поиска - 200 все студенты`() {
        saveStudent("Алиса", "Смирнова")
        saveStudent("Борис", "Иванов")
        saveStudent("Виктор", "Петров")
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/students")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(3))
    }

    @Test
    fun `GET students - поиск по фамилии - только совпадающие`() {
        saveStudent("Алиса", "Смирнова")
        saveStudent("Борис", "Иванов")
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/students?search=Смир")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].lastName").value("Смирнова"))
    }

    @Test
    fun `GET students - поиск по телефону - студент найден`() {
        saveStudent("Алиса", "Смирнова", "+79161234567")
        saveStudent("Борис", "Иванов", "+79990000000")
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/students?search=7916")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].firstName").value("Алиса"))
    }

    @Test
    fun `GET students - TEACHER - 403`() {
        val token = registerWithRole("teacher2@student.test", RoleName.TEACHER)

        mockMvc.perform(
            get("/api/v1/students")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET students id - ADMIN - 200`() {
        val student = saveStudent("Алиса", "Смирнова")
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/students/${student.id}")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(student.id))
    }

    @Test
    fun `GET students id - TEACHER студент в группе - 200`() {
        val teacherToken = registerWithRole("teacher3@student.test", RoleName.TEACHER)
        val teacherUser = userRepository.findAll().first { it.email == "teacher3@student.test" }
        val student = saveStudent("Борис", "Тестов")
        val course = saveCourse()
        studyGroupRepository.save(StudyGroup().apply {
            name = "Тест группа"
            this.course = course
            this.teacher = teacherUser
            students = mutableSetOf(student)
        })

        mockMvc.perform(
            get("/api/v1/students/${student.id}")
                .header("Authorization", "Bearer $teacherToken")
        ).andExpect(status().isOk)
    }

    @Test
    fun `GET students id - TEACHER студент не в группе - 403`() {
        val teacherToken = registerWithRole("teacher4@student.test", RoleName.TEACHER)
        val student = saveStudent("Виктор", "Тестов")

        mockMvc.perform(
            get("/api/v1/students/${student.id}")
                .header("Authorization", "Bearer $teacherToken")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET students id - STUDENT свой профиль - 200`() {
        val studentToken = registerWithRole("student2@student.test", RoleName.STUDENT)
        val studentUser = userRepository.findAll().first { it.email == "student2@student.test" }
        val student = studentRepository.save(Student().apply {
            firstName = "Григорий"
            lastName = "Тестов"
            user = studentUser
        })

        mockMvc.perform(
            get("/api/v1/students/${student.id}")
                .header("Authorization", "Bearer $studentToken")
        ).andExpect(status().isOk)
    }

    @Test
    fun `GET students id - STUDENT чужой профиль - 403`() {
        val studentToken = registerWithRole("student3@student.test", RoleName.STUDENT)
        val otherStudent = saveStudent("Другой", "Студент")

        mockMvc.perform(
            get("/api/v1/students/${otherStudent.id}")
                .header("Authorization", "Bearer $studentToken")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET students id - не найден - 404`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/students/9999")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `PUT students id - ADMIN - 200 поля обновлены`() {
        val student = saveStudent("Алиса", "Смирнова")
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/students/${student.id}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Мария","lastName":"Иванова","phone":"+79161112233"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.firstName").value("Мария"))
            .andExpect(jsonPath("$.lastName").value("Иванова"))
            .andExpect(jsonPath("$.phone").value("+79161112233"))
    }

    @Test
    fun `PUT students id - не найден - 404`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/students/9999")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Мария","lastName":"Иванова","phone":"+79161112233"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `PUT students id - пустой firstName - 400 validation-error`() {
        val student = saveStudent("Алиса", "Смирнова")
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/students/${student.id}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"lastName":"Иванова","phone":"+79161112233"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("validation-error"))
    }

    @Test
    fun `PUT students id - TEACHER - 403`() {
        val student = saveStudent("Алиса", "Смирнова")
        val token = registerWithRole("teacher5@student.test", RoleName.TEACHER)

        mockMvc.perform(
            put("/api/v1/students/${student.id}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Мария","lastName":"Иванова","phone":"+79161112233"}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `PUT students id - STUDENT - 403`() {
        val student = saveStudent("Алиса", "Смирнова")
        val token = registerWithRole("student4@student.test", RoleName.STUDENT)

        mockMvc.perform(
            put("/api/v1/students/${student.id}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"firstName":"Мария","lastName":"Иванова","phone":"+79161112233"}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE students id - ADMIN - 204 студент удалён из БД`() {
        val student = saveStudent("Алиса", "Смирнова")
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            delete("/api/v1/students/${student.id}")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        assert(!studentRepository.existsById(student.id!!))
    }

    @Test
    fun `DELETE students id - не найден - 404`() {
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            delete("/api/v1/students/9999")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `DELETE students id - студент в активной группе - 409`() {
        val student = saveStudent("Борис", "Тестов")
        val course = saveCourse()
        val adminUser = userRepository.findAll().first { it.email == "admin@covenantcode.ru" }
        studyGroupRepository.save(StudyGroup().apply {
            name = "Активная группа"
            this.course = course
            this.teacher = adminUser
            status = GroupStatus.ACTIVE
            students = mutableSetOf(student)
        })
        val token = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            delete("/api/v1/students/${student.id}")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.type").value("conflict"))

        assert(studentRepository.existsById(student.id!!))
    }

    @Test
    fun `DELETE students id - MANAGER - 403`() {
        val student = saveStudent("Виктор", "Тестов")
        val token = registerWithRole("manager6@student.test", RoleName.MANAGER)

        mockMvc.perform(
            delete("/api/v1/students/${student.id}")
                .header("Authorization", "Bearer $token")
        ).andExpect(status().isForbidden)
    }
}
