package ru.practicum.controller.category;


import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.category.CategoryRequest;
import ru.practicum.dto.category.CategoryResponse;
import ru.practicum.service.category.CategoryService;

import java.net.URI;

@RestController
@RequestMapping("/admin/categories")
@AllArgsConstructor
@Slf4j
public class AdminCategoryController {
    private final CategoryService categoryService;

    @PostMapping()
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest categoryRequest) {
        log.info("Create category");
        CategoryResponse created = categoryService.save(categoryRequest);
        return ResponseEntity.created(URI.create("/" + created.getId()))
                .body(created);
    }

    @DeleteMapping("/{catId}")
    public ResponseEntity<Void> delete(@PathVariable("catId") Long catId) {
        log.info("Delete category with id: {}", catId);
        categoryService.delete(catId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{catId}")
    public ResponseEntity<CategoryResponse> update(@PathVariable("catId") Long catId,
                                                   @Valid @RequestBody CategoryRequest categoryRequest) {
        log.info("Update category with id: {}", catId);
        CategoryResponse updated = categoryService.update(catId, categoryRequest);
        return ResponseEntity.ok().body(updated);
    }

}
