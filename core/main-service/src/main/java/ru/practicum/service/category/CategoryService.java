package ru.practicum.service.category;

import ru.practicum.dto.category.CategoryFilter;
import ru.practicum.dto.category.CategoryRequest;
import ru.practicum.dto.category.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse save(CategoryRequest categoryRequest);

    CategoryResponse update(Long catId, CategoryRequest categoryRequest);

    void delete(Long catId);

    List<CategoryResponse> getNeeded(CategoryFilter categoryFilter);

    CategoryResponse findCategoryById(Long catId);
}
