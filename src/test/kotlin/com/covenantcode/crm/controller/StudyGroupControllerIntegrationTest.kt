package com.covenantcode.crm.controller

import com.covenantcode.crm.BaseIntegrationTest
import com.covenantcode.crm.entity.Course
import com.covenantcode.crm.entity.Student
import com.covenantcode.crm.entity.StudyGroup
import com.covenantcode.crm.entity.User
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.repository.CourseRepository
import com.covenantcode.crm.repository.RoleRepository
import com.covenantcode.crm.repository.StudentRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.data.repository.findByIdOrNull

@AutoConfigureMockMvc
class StudyGroupControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var studyGroupRepository: StudyGroupRepository
    @Autowired private lateinit var courseRepository: CourseRepository
    @Autowired private lateinit var studentRepository: StudentRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository
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

    private fun saveCourse(title: String = "Java Backend") = courseRepository.save(Course().apply {
        this.title = title
        durationInWeeks = 12
    })

    private fun saveStudent(firstName: String) = studentRepository.save(Student().apply {
        this.firstName = firstName
        lastName = "Тестов"
    })

    private fun saveGroup(name: String, course: Course, teacher: User, status: GroupStatus = GroupStatus.DRAFT) =
        studyGroupRepository.save(StudyGroup().apply {
            this.name = name
            this.course = course
            this.teacher = teacher
            this.status = status
        })

    @Test
    fun `POST groups - успешное создание с учителем и студентами - 201 статус DRAFT`() {
        val course = saveCourse()
        val teacherToken = registerWithRole("teacher@group.test", RoleName.TEACHER)
        val teacherId = userRepository.findAll().first { it.email == "teacher@group.test" }.id!!
        val student1 = saveStudent("Алиса")
        val student2 = saveStudent("Борис")
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/groups")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Java 2026","courseId":${course.id},"teacherId":$teacherId,"startDate":"2026-09-01","studentIds":[${student1.id},${student2.id}]}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.status").value("DRAFT"))
            .andExpect(jsonPath("$.students.length()").value(2))
    }

    @Test
    fun `POST groups - курс не найден - 404`() {
        val teacherToken = registerWithRole("teacher2@group.test", RoleName.TEACHER)
        val teacherId = userRepository.findAll().first { it.email == "teacher2@group.test" }.id!!
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/groups")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Группа","courseId":9999,"teacherId":$teacherId,"startDate":"2026-09-01"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `POST groups - teacherId указывает на MANAGER - 400`() {
        val course = saveCourse()
        val managerToken = registerWithRole("manager@group.test", RoleName.MANAGER)
        val managerId = userRepository.findAll().first { it.email == "manager@group.test" }.id!!
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/groups")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Группа","courseId":${course.id},"teacherId":$managerId,"startDate":"2026-09-01"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("bad-request"))
    }

    @Test
    fun `POST groups - TEACHER не может создать группу - 403`() {
        val course = saveCourse()
        val teacherToken = registerWithRole("teacher3@group.test", RoleName.TEACHER)
        val teacherId = userRepository.findAll().first { it.email == "teacher3@group.test" }.id!!

        mockMvc.perform(
            post("/api/v1/groups")
                .header("Authorization", "Bearer $teacherToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Группа","courseId":${course.id},"teacherId":$teacherId,"startDate":"2026-09-01"}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET groups - без фильтров - 200 все группы`() {
        registerWithRole("teacher_list@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_list@group.test" }
        val course = saveCourse()
        saveGroup("Группа 1", course, teacher, GroupStatus.DRAFT)
        saveGroup("Группа 2", course, teacher, GroupStatus.ACTIVE)
        saveGroup("Группа 3", course, teacher, GroupStatus.COMPLETED)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/groups").header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(3))
    }

    @Test
    fun `GET groups - фильтр по courseId - только группы этого курса`() {
        registerWithRole("teacher_course@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_course@group.test" }
        val course1 = saveCourse("Курс A")
        val course2 = saveCourse("Курс B")
        saveGroup("Группа A1", course1, teacher)
        saveGroup("Группа A2", course1, teacher)
        saveGroup("Группа B1", course2, teacher)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/groups?courseId=${course1.id}").header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].course.id").value(course1.id))
    }

    @Test
    fun `GET groups - фильтр по status ACTIVE - только активные`() {
        registerWithRole("teacher_status@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_status@group.test" }
        val course = saveCourse()
        saveGroup("Черновик", course, teacher, GroupStatus.DRAFT)
        saveGroup("Активная", course, teacher, GroupStatus.ACTIVE)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/groups?status=ACTIVE").header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].status").value("ACTIVE"))
    }

    @Test
    fun `GET groups - пагинация - 5 записей size=2`() {
        registerWithRole("teacher_page@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_page@group.test" }
        val course = saveCourse()
        repeat(5) { saveGroup("Группа $it", course, teacher) }
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/groups?page=0&size=2").header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(5))
    }

    @Test
    fun `GET groups - TEACHER - 403`() {
        val teacherToken = registerWithRole("teacher_forbidden@group.test", RoleName.TEACHER)

        mockMvc.perform(
            get("/api/v1/groups").header("Authorization", "Bearer $teacherToken")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET groups id - ADMIN - 200`() {
        registerWithRole("teacher_gbi1@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_gbi1@group.test" }
        val group = saveGroup("Группа", saveCourse(), teacher)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/groups/${group.id}").header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(group.id))
    }

    @Test
    fun `GET groups id - TEACHER своя группа - 200`() {
        val teacherToken = registerWithRole("teacher_gbi2@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_gbi2@group.test" }
        val group = saveGroup("Группа", saveCourse(), teacher)

        mockMvc.perform(
            get("/api/v1/groups/${group.id}").header("Authorization", "Bearer $teacherToken")
        ).andExpect(status().isOk)
    }

    @Test
    fun `GET groups id - TEACHER чужая группа - 403`() {
        registerWithRole("teacher_gbi3a@group.test", RoleName.TEACHER)
        val owner = userRepository.findAll().first { it.email == "teacher_gbi3a@group.test" }
        val group = saveGroup("Группа", saveCourse(), owner)
        val otherTeacherToken = registerWithRole("teacher_gbi3b@group.test", RoleName.TEACHER)

        mockMvc.perform(
            get("/api/v1/groups/${group.id}").header("Authorization", "Bearer $otherTeacherToken")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET groups id - STUDENT в группе - 200`() {
        registerWithRole("teacher_gbi4@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_gbi4@group.test" }
        val studentToken = registerWithRole("student_gbi4@group.test", RoleName.STUDENT)
        val studentUser = userRepository.findAll().first { it.email == "student_gbi4@group.test" }
        val student = studentRepository.save(Student().apply {
            firstName = "Студент"; lastName = "Тестов"; user = studentUser
        })
        val group = saveGroup("Группа", saveCourse(), teacher).also {
            it.students = mutableSetOf(student)
            studyGroupRepository.save(it)
        }

        mockMvc.perform(
            get("/api/v1/groups/${group.id}").header("Authorization", "Bearer $studentToken")
        ).andExpect(status().isOk)
    }

    @Test
    fun `GET groups id - STUDENT не в группе - 403`() {
        registerWithRole("teacher_gbi5@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_gbi5@group.test" }
        val group = saveGroup("Группа", saveCourse(), teacher)
        val studentToken = registerWithRole("student_gbi5@group.test", RoleName.STUDENT)

        mockMvc.perform(
            get("/api/v1/groups/${group.id}").header("Authorization", "Bearer $studentToken")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET groups id - не найдена - 404`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/groups/9999").header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    // ─── PUT /api/v1/groups/{id} ──────────────────────────────────────────────

    @Test
    fun `PUT groups id - успешное обновление - 200 поля изменились`() {
        registerWithRole("teacher_put1@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_put1@group.test" }
        val course = saveCourse("Исходный курс")
        val newCourse = saveCourse("Новый курс")
        val group = saveGroup("Исходное название", course, teacher)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/groups/${group.id}")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Новое название","courseId":${newCourse.id},"teacherId":${teacher.id},"startDate":"2026-10-01"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Новое название"))
            .andExpect(jsonPath("$.course.id").value(newCourse.id))
            .andExpect(jsonPath("$.status").value("DRAFT"))
    }

    @Test
    fun `PUT groups id - группа в COMPLETED - 400 bad-request`() {
        registerWithRole("teacher_put2@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_put2@group.test" }
        val course = saveCourse()
        val group = saveGroup("Группа", course, teacher, GroupStatus.COMPLETED)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/groups/${group.id}")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Новое название","courseId":${course.id},"teacherId":${teacher.id},"startDate":"2026-10-01"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("bad-request"))
    }

    @Test
    fun `PUT groups id - новый учитель не TEACHER - 400`() {
        registerWithRole("teacher_put3@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_put3@group.test" }
        val managerToken = registerWithRole("manager_put3@group.test", RoleName.MANAGER)
        val manager = userRepository.findAll().first { it.email == "manager_put3@group.test" }
        val course = saveCourse()
        val group = saveGroup("Группа", course, teacher)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/groups/${group.id}")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Новое название","courseId":${course.id},"teacherId":${manager.id},"startDate":"2026-10-01"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("bad-request"))
    }

    @Test
    fun `PUT groups id - TEACHER не может обновить группу - 403`() {
        val teacherToken = registerWithRole("teacher_put4@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_put4@group.test" }
        val course = saveCourse()
        val group = saveGroup("Группа", course, teacher)

        mockMvc.perform(
            put("/api/v1/groups/${group.id}")
                .header("Authorization", "Bearer $teacherToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Новое название","courseId":${course.id},"teacherId":${teacher.id},"startDate":"2026-10-01"}""")
        ).andExpect(status().isForbidden)
    }

    // ─── PATCH /api/v1/groups/{id}/status ────────────────────────────────────

    @Test
    fun `PATCH groups id status - DRAFT → ACTIVE - 200 статус изменился`() {
        registerWithRole("teacher_st1@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_st1@group.test" }
        val group = saveGroup("Группа", saveCourse(), teacher, GroupStatus.DRAFT)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            patch("/api/v1/groups/${group.id}/status")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"ACTIVE"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ACTIVE"))
    }

    @Test
    fun `PATCH groups id status - DRAFT → COMPLETED - недопустимый переход - 400`() {
        registerWithRole("teacher_st2@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_st2@group.test" }
        val group = saveGroup("Группа", saveCourse(), teacher, GroupStatus.DRAFT)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            patch("/api/v1/groups/${group.id}/status")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"COMPLETED"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("bad-request"))
    }

    @Test
    fun `PATCH groups id status - COMPLETED → ACTIVE - финальный статус - 400`() {
        registerWithRole("teacher_st3@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_st3@group.test" }
        val group = saveGroup("Группа", saveCourse(), teacher, GroupStatus.COMPLETED)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            patch("/api/v1/groups/${group.id}/status")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"ACTIVE"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("bad-request"))
    }

    @Test
    fun `PATCH groups id status - TEACHER не может изменить статус - 403`() {
        val teacherToken = registerWithRole("teacher_st4@group.test", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_st4@group.test" }
        val group = saveGroup("Группа", saveCourse(), teacher, GroupStatus.DRAFT)

        mockMvc.perform(
            patch("/api/v1/groups/${group.id}/status")
                .header("Authorization", "Bearer $teacherToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"ACTIVE"}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `PATCH groups id status - группа не найдена - 404`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            patch("/api/v1/groups/9999/status")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"ACTIVE"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }
}
