package com.servicedeskpro.repository;

import com.servicedeskpro.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
    List<Category> findAllByActiveTrueOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
}