package com.smartcampus.placement.repository;

import com.smartcampus.placement.entity.Student;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentRepository extends JpaRepository<Student, Long> {
  Optional<Student> findByEmail(String email);

  Optional<Student> findByEmailIgnoreCase(String email);
}