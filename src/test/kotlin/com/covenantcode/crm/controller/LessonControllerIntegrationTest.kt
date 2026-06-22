package com.covenantcode.crm.controller

import com.covenantcode.crm.BaseIntegrationTest
import com.covenantcode.crm.entity.Course
import com.covenantcode.crm.entity.Lesson
import com.covenantcode.crm.entity.Student
import com.covenantcode.crm.entity.StudyGroup
import com.covenantcode.crm.entity.enums.GroupStatus
import com.covenantcode.crm.entity.enums.RoleName
import com.covenantcode.crm.repository.CourseRepository
import com.covenantcode.crm.repository.LessonRepository
import com.covenantcode.crm.repository.RoleRepository
import com.covenantcode.crm.repository.StudentRepository
import com.covenantcode.crm.repository.StudyGroupRepository
import com.covenantcode.crm.repository.UserRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalTime

@AutoConfigureMockMvc
class LessonControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var studyGroupRepository: StudyGroupRepository
    @Autowired private lateinit var courseRepository: CourseRepository
    @Autowired private lateinit var lessonRepository: LessonRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var roleRepository: RoleRepository
    @Autowired private lateinit var studentRepository: StudentRepository
    @Autowired private lateinit var objectMapper: ObjectMapper

    private fun doCleanup() {
        lessonRepository.deleteAll()
        studyGroupRepository.deleteAll()
        studentRepository.deleteAll()
        userRepository.deleteAll(
            userRepository.findAll().filter { it.email != "admin@covenantcode.ru" }
        )
        courseRepository.deleteAll()
    }

    @BeforeEach fun cleanup() = doCleanup()
    @AfterEach fun teardown() = doCleanup()

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

    private fun saveCourse() = courseRepository.save(Course().apply { title = "Курс"; durationInWeeks = 12 })

    private fun saveActiveGroup(teacherId: Long, status: GroupStatus = GroupStatus.ACTIVE): StudyGroup {
        val teacher = userRepository.findByIdOrNull(teacherId)!!
        return studyGroupRepository.save(StudyGroup().apply {
            name = "Группа"; course = saveCourse()
            this.teacher = teacher; this.status = status
            startDate = LocalDate.of(2026, 6, 1)
        })
    }

    @Test
    fun `POST lessons - успешное создание - 201`() {
        registerWithRole("teacher_lc1@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_lc1@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/lessons")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"groupId":${group.id},"teacherId":${teacher.id},
                     "topic":"Тема","lessonDate":"2026-06-10",
                     "startTime":"18:00","endTime":"19:30"}
                """.trimIndent())
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.topic").value("Тема"))
            .andExpect(jsonPath("$.studyGroup.id").value(group.id))
            .andExpect(jsonPath("$.teacher.id").value(teacher.id))
    }

    @Test
    fun `POST lessons - группа DRAFT - 400 bad-request`() {
        registerWithRole("teacher_lc2@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_lc2@test.com" }
        val group = saveActiveGroup(teacher.id!!, GroupStatus.DRAFT)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/lessons")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"groupId":${group.id},"teacherId":${teacher.id},"topic":"Тема","lessonDate":"2026-06-10","startTime":"18:00","endTime":"19:30"}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("bad-request"))
    }

    @Test
    fun `POST lessons - endTime раньше startTime - 400`() {
        registerWithRole("teacher_lc3@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_lc3@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/lessons")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"groupId":${group.id},"teacherId":${teacher.id},"topic":"Тема","lessonDate":"2026-06-10","startTime":"18:00","endTime":"17:00"}""")
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `POST lessons - группа не найдена - 404`() {
        registerWithRole("teacher_lc4@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_lc4@test.com" }
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/lessons")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"groupId":9999,"teacherId":${teacher.id},"topic":"Тема","lessonDate":"2026-06-10","startTime":"18:00","endTime":"19:30"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `POST lessons - пересечение занятий - 409 conflict`() {
        registerWithRole("teacher_lc5@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_lc5@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        // Сохранить первое занятие напрямую
        lessonRepository.save(Lesson().apply {
            studyGroup = group; this.teacher = teacher; topic = "Первое"
            lessonDate = LocalDate.of(2026, 6, 10)
            startTime = java.time.LocalTime.of(9, 0); endTime = java.time.LocalTime.of(11, 0)
        })
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            post("/api/v1/lessons")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"groupId":${group.id},"teacherId":${teacher.id},"topic":"Второе","lessonDate":"2026-06-10","startTime":"10:00","endTime":"12:00"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.type").value("conflict"))
    }

    // ─── CCCRM-0038: GET /api/v1/lessons ─────────────────────────────────────

    private fun saveLesson(teacher: com.covenantcode.crm.entity.User, group: StudyGroup, date: LocalDate, slot: Int = 0): Lesson =
        lessonRepository.save(Lesson().apply {
            studyGroup = group; this.teacher = teacher; topic = "Тема"
            lessonDate = date
            startTime = LocalTime.of(9 + slot, 0); endTime = LocalTime.of(10 + slot, 0)
        })

    private fun saveStudent(user: com.covenantcode.crm.entity.User): Student =
        studentRepository.save(Student().apply {
            this.user = user; firstName = user.firstName; lastName = user.lastName; phone = "+7"
        })

    private fun addStudentToGroup(student: Student, group: StudyGroup) {
        group.students.add(student)
        studyGroupRepository.save(group)
    }

    @Test
    fun `GET lessons - без фильтров - возвращает все занятия`() {
        registerWithRole("teacher_get1@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_get1@test.com" }
        val group1 = saveActiveGroup(teacher.id!!)
        val group2 = saveActiveGroup(teacher.id!!)
        saveLesson(teacher, group1, LocalDate.of(2026, 6, 1), 0)
        saveLesson(teacher, group1, LocalDate.of(2026, 6, 2), 1)
        saveLesson(teacher, group2, LocalDate.of(2026, 6, 3), 2)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(get("/api/v1/lessons").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(3))
    }

    @Test
    fun `GET lessons - фильтр по groupId - только занятия нужной группы`() {
        registerWithRole("teacher_get2@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_get2@test.com" }
        val group1 = saveActiveGroup(teacher.id!!)
        val group2 = saveActiveGroup(teacher.id!!)
        saveLesson(teacher, group1, LocalDate.of(2026, 6, 1), 0)
        saveLesson(teacher, group1, LocalDate.of(2026, 6, 2), 1)
        saveLesson(teacher, group2, LocalDate.of(2026, 6, 3), 2)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/lessons").param("groupId", group1.id.toString())
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `GET lessons - фильтр по диапазону дат - только занятия в диапазоне`() {
        registerWithRole("teacher_get3@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_get3@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        saveLesson(teacher, group, LocalDate.of(2026, 6, 1), 0)
        saveLesson(teacher, group, LocalDate.of(2026, 6, 15), 1)
        saveLesson(teacher, group, LocalDate.of(2026, 7, 1), 2)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            get("/api/v1/lessons")
                .param("dateFrom", "2026-06-01").param("dateTo", "2026-06-30")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `GET lessons - роль TEACHER - 403`() {
        val teacherToken = registerWithRole("teacher_get4@test.com", RoleName.TEACHER)

        mockMvc.perform(get("/api/v1/lessons").header("Authorization", "Bearer $teacherToken"))
            .andExpect(status().isForbidden)
    }

    // ─── CCCRM-0039: GET /api/v1/lessons/{id} ────────────────────────────────

    @Test
    fun `GET lessons id - ADMIN получает занятие - 200`() {
        registerWithRole("teacher_gi1@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_gi1@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        val lesson = saveLesson(teacher, group, LocalDate.of(2026, 6, 10))
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(get("/api/v1/lessons/${lesson.id}").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(lesson.id))
            .andExpect(jsonPath("$.topic").value("Тема"))
    }

    @Test
    fun `GET lessons id - TEACHER своё занятие - 200`() {
        val teacherToken = registerWithRole("teacher_gi2@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_gi2@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        val lesson = saveLesson(teacher, group, LocalDate.of(2026, 6, 10))

        mockMvc.perform(get("/api/v1/lessons/${lesson.id}").header("Authorization", "Bearer $teacherToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(lesson.id))
    }

    @Test
    fun `GET lessons id - TEACHER чужое занятие - 403`() {
        registerWithRole("teacher_gi3a@test.com", RoleName.TEACHER)
        val teacherA = userRepository.findAll().first { it.email == "teacher_gi3a@test.com" }
        val teacherBToken = registerWithRole("teacher_gi3b@test.com", RoleName.TEACHER)
        val group = saveActiveGroup(teacherA.id!!)
        val lesson = saveLesson(teacherA, group, LocalDate.of(2026, 6, 10))

        mockMvc.perform(get("/api/v1/lessons/${lesson.id}").header("Authorization", "Bearer $teacherBToken"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET lessons id - несуществующий ID - 404`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(get("/api/v1/lessons/99999").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    // ─── CCCRM-0040: PUT /api/v1/lessons/{id} ────────────────────────────────

    private fun updateBody(groupId: Long, teacherId: Long, topic: String = "Обновлённая тема") =
        """{"groupId":$groupId,"teacherId":$teacherId,"topic":"$topic","lessonDate":"2026-06-15","startTime":"10:00","endTime":"11:30"}"""

    @Test
    fun `PUT lessons id - успешное обновление - 200 поля изменились`() {
        registerWithRole("teacher_upd1@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_upd1@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        val lesson = saveLesson(teacher, group, LocalDate.of(2026, 6, 10))
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/lessons/${lesson.id}")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(group.id!!, teacher.id!!))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.topic").value("Обновлённая тема"))
            .andExpect(jsonPath("$.lessonDate").value("2026-06-15"))
    }

    @Test
    fun `PUT lessons id - группа занятия COMPLETED - 400`() {
        registerWithRole("teacher_upd2@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_upd2@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        val lesson = saveLesson(teacher, group, LocalDate.of(2026, 6, 10))
        // Переводим группу в COMPLETED после создания занятия
        group.status = GroupStatus.COMPLETED
        studyGroupRepository.save(group)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/lessons/${lesson.id}")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(group.id!!, teacher.id!!))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("bad-request"))
    }

    @Test
    fun `PUT lessons id - занятие не найдено - 404`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/lessons/99999")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"groupId":1,"teacherId":1,"topic":"T","lessonDate":"2026-06-15","startTime":"10:00","endTime":"11:30"}""")
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `PUT lessons id - пересечение занятий - 409`() {
        registerWithRole("teacher_upd4@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_upd4@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        // Первое занятие 10:00-12:00
        lessonRepository.save(Lesson().apply {
            studyGroup = group; this.teacher = teacher; topic = "Первое"
            lessonDate = LocalDate.of(2026, 6, 15)
            startTime = LocalTime.of(10, 0); endTime = LocalTime.of(12, 0)
        })
        // Второе занятие 14:00-15:00 — будем пытаться переместить на 11:00-13:00
        val lesson2 = lessonRepository.save(Lesson().apply {
            studyGroup = group; this.teacher = teacher; topic = "Второе"
            lessonDate = LocalDate.of(2026, 6, 15)
            startTime = LocalTime.of(14, 0); endTime = LocalTime.of(15, 0)
        })
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(
            put("/api/v1/lessons/${lesson2.id}")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"groupId":${group.id},"teacherId":${teacher.id},"topic":"Второе","lessonDate":"2026-06-15","startTime":"11:00","endTime":"13:00"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.type").value("conflict"))
    }

    @Test
    fun `PUT lessons id - роль TEACHER - 403`() {
        val teacherToken = registerWithRole("teacher_upd5@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_upd5@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        val lesson = saveLesson(teacher, group, LocalDate.of(2026, 6, 10))

        mockMvc.perform(
            put("/api/v1/lessons/${lesson.id}")
                .header("Authorization", "Bearer $teacherToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(updateBody(group.id!!, teacher.id!!))
        )
            .andExpect(status().isForbidden)
    }

    // ─── CCCRM-0041: DELETE /api/v1/lessons/{id} ─────────────────────────────

    @Test
    fun `DELETE lessons id - успешное удаление - 204 повторный GET возвращает 404`() {
        registerWithRole("teacher_del1@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_del1@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        val lesson = saveLesson(teacher, group, LocalDate.of(2026, 6, 10))
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(delete("/api/v1/lessons/${lesson.id}").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isNoContent)

        mockMvc.perform(get("/api/v1/lessons/${lesson.id}").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `DELETE lessons id - группа COMPLETED - 400`() {
        registerWithRole("teacher_del2@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_del2@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        val lesson = saveLesson(teacher, group, LocalDate.of(2026, 6, 10))
        group.status = GroupStatus.COMPLETED
        studyGroupRepository.save(group)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(delete("/api/v1/lessons/${lesson.id}").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.type").value("bad-request"))
    }

    @Test
    fun `DELETE lessons id - несуществующий ID - 404`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(delete("/api/v1/lessons/99999").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `DELETE lessons id - роль STUDENT - 403`() {
        val studentToken = registerWithRole("student_del4@test.com", RoleName.STUDENT)

        mockMvc.perform(delete("/api/v1/lessons/1").header("Authorization", "Bearer $studentToken"))
            .andExpect(status().isForbidden)
    }

    // ─── CCCRM-0042: GET /api/v1/groups/{groupId}/lessons ────────────────────

    @Test
    fun `GET groups groupId lessons - ADMIN получает 3 занятия в порядке сортировки`() {
        registerWithRole("teacher_gl1@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_gl1@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        saveLesson(teacher, group, LocalDate.of(2026, 6, 3), 0)
        saveLesson(teacher, group, LocalDate.of(2026, 6, 1), 1)
        saveLesson(teacher, group, LocalDate.of(2026, 6, 2), 0)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(get("/api/v1/groups/${group.id}/lessons").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].lessonDate").value("2026-06-01"))
            .andExpect(jsonPath("$[1].lessonDate").value("2026-06-02"))
            .andExpect(jsonPath("$[2].lessonDate").value("2026-06-03"))
    }

    @Test
    fun `GET groups groupId lessons - группа не найдена - 404`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(get("/api/v1/groups/99999/lessons").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `GET groups groupId lessons - TEACHER чужая группа - 403`() {
        registerWithRole("teacher_gl3a@test.com", RoleName.TEACHER)
        registerWithRole("teacher_gl3b@test.com", RoleName.TEACHER)
        val teacher1 = userRepository.findAll().first { it.email == "teacher_gl3a@test.com" }
        val teacher2 = userRepository.findAll().first { it.email == "teacher_gl3b@test.com" }
        val group = saveActiveGroup(teacher1.id!!)
        val teacher2Token = loginAndGetToken("teacher_gl3b@test.com", "password123")

        mockMvc.perform(get("/api/v1/groups/${group.id}/lessons").header("Authorization", "Bearer $teacher2Token"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET groups groupId lessons - STUDENT не в группе - 403`() {
        registerWithRole("teacher_gl4@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_gl4@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        val studentToken = registerWithRole("student_gl4@test.com", RoleName.STUDENT)

        mockMvc.perform(get("/api/v1/groups/${group.id}/lessons").header("Authorization", "Bearer $studentToken"))
            .andExpect(status().isForbidden)
    }

    // ─── CCCRM-0043: GET /api/v1/teachers/{teacherId}/lessons ────────────────

    @Test
    fun `GET teachers teacherId lessons - ADMIN получает расписание - 200`() {
        registerWithRole("teacher_tl1@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_tl1@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        saveLesson(teacher, group, LocalDate.of(2026, 6, 2), 0)
        saveLesson(teacher, group, LocalDate.of(2026, 6, 5), 1)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(get("/api/v1/teachers/${teacher.id}/lessons").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].lessonDate").value("2026-06-02"))
            .andExpect(jsonPath("$[1].lessonDate").value("2026-06-05"))
    }

    @Test
    fun `GET teachers teacherId lessons - TEACHER своё расписание с фильтром dateFrom - 200`() {
        registerWithRole("teacher_tl2@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_tl2@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        saveLesson(teacher, group, LocalDate.of(2026, 5, 20), 0)
        saveLesson(teacher, group, LocalDate.of(2026, 6, 10), 1)
        val teacherToken = loginAndGetToken("teacher_tl2@test.com", "password123")

        mockMvc.perform(
            get("/api/v1/teachers/${teacher.id}/lessons")
                .param("dateFrom", "2026-06-01")
                .header("Authorization", "Bearer $teacherToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].lessonDate").value("2026-06-10"))
    }

    @Test
    fun `GET teachers teacherId lessons - TEACHER чужое расписание - 403`() {
        registerWithRole("teacher_tl3a@test.com", RoleName.TEACHER)
        registerWithRole("teacher_tl3b@test.com", RoleName.TEACHER)
        val teacher1 = userRepository.findAll().first { it.email == "teacher_tl3a@test.com" }
        val teacher2Token = loginAndGetToken("teacher_tl3b@test.com", "password123")

        mockMvc.perform(get("/api/v1/teachers/${teacher1.id}/lessons").header("Authorization", "Bearer $teacher2Token"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET teachers teacherId lessons - несуществующий teacherId - 404`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(get("/api/v1/teachers/99999/lessons").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `GET teachers teacherId lessons - роль STUDENT - 403`() {
        val studentToken = registerWithRole("student_tl5@test.com", RoleName.STUDENT)

        mockMvc.perform(get("/api/v1/teachers/1/lessons").header("Authorization", "Bearer $studentToken"))
            .andExpect(status().isForbidden)
    }

    // ─── CCCRM-0044: GET /api/v1/students/{studentId}/lessons ────────────────

    @Test
    fun `GET students studentId lessons - студент в двух группах - 200 все занятия отсортированы`() {
        registerWithRole("teacher_sl1@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_sl1@test.com" }
        val group1 = saveActiveGroup(teacher.id!!)
        val group2 = saveActiveGroup(teacher.id!!)
        registerWithRole("student_sl1@test.com", RoleName.STUDENT)
        val studentUser = userRepository.findAll().first { it.email == "student_sl1@test.com" }
        val student = saveStudent(studentUser)
        addStudentToGroup(student, group1)
        addStudentToGroup(student, group2)
        saveLesson(teacher, group1, LocalDate.of(2026, 6, 5), 0)
        saveLesson(teacher, group2, LocalDate.of(2026, 6, 3), 1)
        saveLesson(teacher, group1, LocalDate.of(2026, 6, 7), 0)
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(get("/api/v1/students/${student.id}/lessons").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[0].lessonDate").value("2026-06-03"))
            .andExpect(jsonPath("$[1].lessonDate").value("2026-06-05"))
            .andExpect(jsonPath("$[2].lessonDate").value("2026-06-07"))
    }

    @Test
    fun `GET students studentId lessons - фильтр dateFrom dateTo - только занятия в диапазоне`() {
        registerWithRole("teacher_sl2@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_sl2@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        registerWithRole("student_sl2@test.com", RoleName.STUDENT)
        val studentUser = userRepository.findAll().first { it.email == "student_sl2@test.com" }
        val student = saveStudent(studentUser)
        addStudentToGroup(student, group)
        saveLesson(teacher, group, LocalDate.of(2026, 5, 20), 0)
        saveLesson(teacher, group, LocalDate.of(2026, 6, 10), 1)
        saveLesson(teacher, group, LocalDate.of(2026, 6, 20), 0)
        val studentToken = loginAndGetToken("student_sl2@test.com", "password123")

        mockMvc.perform(
            get("/api/v1/students/${student.id}/lessons")
                .param("dateFrom", "2026-06-01")
                .param("dateTo", "2026-06-15")
                .header("Authorization", "Bearer $studentToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].lessonDate").value("2026-06-10"))
    }

    @Test
    fun `GET students studentId lessons - STUDENT чужой studentId - 403`() {
        registerWithRole("teacher_sl3@test.com", RoleName.TEACHER)
        val teacher = userRepository.findAll().first { it.email == "teacher_sl3@test.com" }
        val group = saveActiveGroup(teacher.id!!)
        registerWithRole("student_sl3a@test.com", RoleName.STUDENT)
        val studentUser1 = userRepository.findAll().first { it.email == "student_sl3a@test.com" }
        val student1 = saveStudent(studentUser1)
        addStudentToGroup(student1, group)
        val student2Token = registerWithRole("student_sl3b@test.com", RoleName.STUDENT)

        mockMvc.perform(get("/api/v1/students/${student1.id}/lessons").header("Authorization", "Bearer $student2Token"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `GET students studentId lessons - несуществующий studentId - 404`() {
        val adminToken = loginAndGetToken("admin@covenantcode.ru", "Admin123!")

        mockMvc.perform(get("/api/v1/students/99999/lessons").header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.type").value("resource-not-found"))
    }

    @Test
    fun `GET students studentId lessons - роль TEACHER - 403`() {
        val teacherToken = registerWithRole("teacher_sl5@test.com", RoleName.TEACHER)

        mockMvc.perform(get("/api/v1/students/1/lessons").header("Authorization", "Bearer $teacherToken"))
            .andExpect(status().isForbidden)
    }
}
