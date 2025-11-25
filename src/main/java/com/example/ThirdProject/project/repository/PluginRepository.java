package com.example.ThirdProject.project.repository;

import com.example.ThirdProject.project.entity.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PluginRepository extends JpaRepository<Plugin, Long> {
}
