package com.example.ThirdProject.project.repository;

import com.example.ThirdProject.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUserId(String userId);
    List<Project> findByAlbumId(Long albumId);
    Optional<Project> findByIdAndUserId(Long id, String userId);
}
