package com.covenantcode.crm.repository;

import com.covenantcode.crm.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long> {
}
