package com.cookify.repository;

import com.cookify.model.Warning;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarningRepository extends JpaRepository<Warning, Long> {
    List<Warning> findByUserId(Long userId);
    long countByUserId(Long userId);
}
