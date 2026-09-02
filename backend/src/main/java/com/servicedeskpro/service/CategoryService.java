package com.servicedeskpro.service;

import com.servicedeskpro.dto.request.CategoryRequestDto;
import com.servicedeskpro.dto.response.CategoryDto;

import java.util.List;

public interface CategoryService {
    List<CategoryDto> getAllActiveCategories();
    List<CategoryDto> getAllCategories();
    CategoryDto getCategoryById(Long id);
    CategoryDto createCategory(CategoryRequestDto request);
    CategoryDto updateCategory(Long id, CategoryRequestDto request);
    void toggleCategoryStatus(Long id, boolean active);
}