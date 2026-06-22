package com.covenantcode.crm.mapper

import com.covenantcode.crm.dto.group.CourseShortResponse
import com.covenantcode.crm.dto.group.StudentShortResponse
import com.covenantcode.crm.dto.group.StudyGroupResponse
import com.covenantcode.crm.dto.group.UserShortResponse
import com.covenantcode.crm.entity.StudyGroup

fun StudyGroup.toResponse() = StudyGroupResponse(
    id = id!!,
    name = name,
    course = CourseShortResponse(id = course.id!!, title = course.title),
    teacher = UserShortResponse(id = teacher.id!!, firstName = teacher.firstName, lastName = teacher.lastName),
    students = students.map { StudentShortResponse(id = it.id!!, firstName = it.firstName, lastName = it.lastName) },
    startDate = startDate,
    status = status,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
