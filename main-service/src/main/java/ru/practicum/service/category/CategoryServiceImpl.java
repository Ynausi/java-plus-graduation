package ru.practicum.service.category;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.category.CategoryFilter;
import ru.practicum.dto.category.CategoryRequest;
import ru.practicum.dto.category.CategoryResponse;
import ru.practicum.exception.AlreadyExistsException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.event.EventRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryResponse save(CategoryRequest categoryRequest) {
        if (Boolean.TRUE.equals(categoryRepository.existsByName(categoryRequest.getName()))) {
            throw new AlreadyExistsException(String.format("Category with name=%s already exist", categoryRequest.getName()));
        }
        Category newCategory = categoryRepository.save(categoryMapper.toModel(categoryRequest));
        return categoryMapper.toResponse(newCategory);
    }

    @Override
    @Transactional
    public CategoryResponse update(Long catId, CategoryRequest categoryRequest) {
        Category category = categoryRepository.findById(catId).orElseThrow(
                () -> new NotFoundException(String.format("Category with id=%s was not found", catId)));
        if (Boolean.TRUE.equals(categoryRepository.existsByNameAndIdNot(categoryRequest.getName(), catId))) {
            throw new AlreadyExistsException(String.format("Category with name=%s already exist", categoryRequest.getName()));
        }
        category.setName(categoryRequest.getName());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void delete(Long catId) {
        Category category = categoryRepository.findById(catId).orElseThrow(
                () -> new NotFoundException(String.format("Category with id=%s was not found", catId)));

        if (Boolean.TRUE.equals(eventRepository.existsByCategoryId(catId))) {
            throw new ConflictException("Cannot delete category, because some events have it");
        }
        categoryRepository.delete(category);
    }

    @Override
    public List<CategoryResponse> getNeeded(CategoryFilter categoryFilter) {
        return categoryRepository.findNeededCategories(categoryFilter.getFrom(), categoryFilter.getSize())
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse findCategoryById(Long catId) {
        Category category = categoryRepository.findById(catId).orElseThrow(
                () -> new NotFoundException(String.format("Category with id=%s was not found", catId)));
        return categoryMapper.toResponse(category);
    }

}
