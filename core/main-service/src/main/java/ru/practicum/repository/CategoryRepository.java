package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Category;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Boolean existsByName(String name);

    Boolean existsByNameAndIdNot(String name, Long catId);

    @Query("SELECT c FROM Category c ORDER BY c.id LIMIT :size OFFSET :from")
    List<Category> findNeededCategories(@Param("from") Integer from,
                                        @Param("size") Integer size);
}
