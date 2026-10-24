package ru.practicum.controller.category;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.category.CategoryFilter;
import ru.practicum.dto.category.CategoryResponse;
import ru.practicum.service.category.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/categories")
@AllArgsConstructor
@Slf4j
public class PublicCategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getNeededCategories(@RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
                                                                      @RequestParam(defaultValue = "10") @Positive Integer size) {
        log.info("Get needed categories");
        CategoryFilter categoryFilter = new CategoryFilter(from, size);
        return ResponseEntity.ok()
                .body(categoryService.getNeeded(categoryFilter));
    }

    @GetMapping("/{catId}")
    public ResponseEntity<CategoryResponse> getCategory(@PathVariable("catId") Long catId) {
        log.info("Get category with id: {}", catId);
        return ResponseEntity.ok().body(categoryService.findCategoryById(catId));
    }

}
