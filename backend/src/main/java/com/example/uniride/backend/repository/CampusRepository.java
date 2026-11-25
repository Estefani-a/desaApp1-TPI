package com.example.uniride.backend.repository;

import com.example.uniride.backend.model.Campus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusRepository extends JpaRepository<Campus, String> {
}
