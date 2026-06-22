package com.covenantcode.crm.mapper

import com.covenantcode.crm.dto.lesson.GroupShortResponse
import com.covenantcode.crm.dto.lesson.LessonResponse
import com.covenantcode.crm.dto.lesson.UserShortResponse
import com.covenantcode.crm.entity.Lesson

fun Lesson.toResponse() = LessonResponse(
    id = id!!,
    studyGroup = GroupShortResponse(id = studyGroup.id!!, name = studyGroup.name),
    teacher = UserShortResponse(id = teacher.id!!, firstName = teacher.firstName, lastName = teacher.lastName),
    topic = topic,
    description = description,
    lessonDate = lessonDate,
    startTime = startTime,
    endTime = endTime,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
