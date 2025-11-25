package com.example.ThirdProject.project.repository;

import com.example.ThirdProject.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserId(String userId);
}
